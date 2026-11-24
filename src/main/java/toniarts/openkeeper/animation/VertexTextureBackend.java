/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper.  If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.animation;

import com.jme3.asset.AssetManager;
import com.jme3.material.MatParam;
import com.jme3.material.MatParamOverride;
import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import com.jme3.renderer.Caps;
import com.jme3.renderer.Limits;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.shader.VarType;
import java.lang.System.Logger;
import java.lang.ref.WeakReference;
import java.lang.System.Logger.Level;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * The VERTEX_TEXTURE animation backend's switch and its per-geometry shader
 * inputs. {@link PoseTrack} asks {@link #isActive()} on every sample; when it
 * is on, the track hands its two key frames and weight to the geometry as
 * material parameter overrides instead of writing the position buffer, and
 * the DkLighting vertex shaders blend them from the shared
 * {@link VertexAnimationTexture}.
 * <p>
 * The backend is chosen once at start-up from the {@code AnimationBackend}
 * setting and the renderer's capabilities; without vertex texture fetch or
 * float textures it stays on the CPU and says so in the log.
 */
public final class VertexTextureBackend {

    private static final Logger LOGGER = System.getLogger(VertexTextureBackend.class.getName());

    public static final String MAP = "VertexAnimationMap";
    public static final String WIDTH = "VertexAnimationWidth";
    public static final String VERTICES = "VertexAnimationVertices";
    public static final String FRAME_A = "VertexAnimationFrameA";
    public static final String FRAME_B = "VertexAnimationFrameB";
    public static final String WEIGHT = "VertexAnimationWeight";
    public static final String NORMAL_MAP = "VertexAnimationNormalMap";
    private static final String[] NAMES = {MAP, WIDTH, VERTICES, FRAME_A, FRAME_B, WEIGHT, NORMAL_MAP};

    /**
     * The material of the drawn geometries that mesh smoothing tessellates,
     * {@code DkLighting/Vertex} with tessellation stages
     */
    public static final String SMOOTH_MATDEF = "MatDefs/DkLighting/Smooth.j3md";
    private static final String VERTEX_MATDEF = "MatDefs/DkLighting/Vertex.j3md";
    /**
     * Most tessellation segments per edge, by smoothing level 1 to 4
     */
    private static final int[] SEGMENTS = {2, 4, 8, 16};

    private static volatile boolean supported;
    private static volatile boolean active;
    private static volatile int generation;
    private static volatile int maxTextureSize = VertexAnimationTexture.MAX_WIDTH;
    private static volatile MaterialDef smoothDef;
    private static volatile MaterialDef vertexDef;
    private static volatile boolean smoothing;
    private static volatile int smoothingLevel = 3;
    private static volatile boolean phongTessellation;
    /**
     * Smoothing materials by the material they stand in for, and back, so
     * shared materials stay shared. Weak both ways: a material goes with the
     * last geometry that uses it.
     */
    private static final Map<Material, WeakReference<Material>> SMOOTH_MATERIALS
            = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Material, WeakReference<Material>> ORIGINAL_MATERIALS
            = Collections.synchronizedMap(new WeakHashMap<>());

    private static final LongAdder SAMPLE_NANOS = new LongAdder();
    private static final LongAdder UPLOADED_BYTES = new LongAdder();
    private static final LongAdder SAMPLES = new LongAdder();
    private static final LongAdder TEXTURE_BYTES = new LongAdder();

    private VertexTextureBackend() {
    }

    /**
     * Chooses the backend. Call on the render thread before models are shown,
     * or again when the settings change: the tracks resample on their next
     * update.
     *
     * @param requested the requested backend
     * @param caps the renderer's capabilities
     * @param limits the renderer's limits
     * @return whether the vertex texture backend is on
     */
    public static boolean configure(AnimationBackend requested, Collection<Caps> caps, Map<Limits, Integer> limits) {
        Integer vertexUnits = limits.get(Limits.VertexTextureUnits);
        Integer textureSize = limits.get(Limits.TextureSize);
        String missing = null;
        if (!caps.contains(Caps.GLSL150)) {
            missing = "GLSL 1.50";
        } else if (!caps.contains(Caps.FloatTexture)) {
            missing = "float textures";
        } else if (vertexUnits == null || vertexUnits < 1) {
            missing = "vertex texture fetch";
        }
        supported = missing == null;
        if (textureSize != null && textureSize > 0) {
            maxTextureSize = textureSize;
        }
        boolean on = supported && requested != AnimationBackend.CPU;
        if (requested == AnimationBackend.VERTEX_TEXTURE && !supported) {
            LOGGER.log(Level.WARNING, "Animation backend VERTEX_TEXTURE needs " + missing + ", falling back to CPU");
        }
        setActive(on);
        LOGGER.log(Level.INFO, "Animation backend: " + (on ? "VERTEX_TEXTURE" : "CPU") + " (requested " + requested.name() + ")");
        return on;
    }

    /**
     * Chooses whether to smooth the animated models by tessellation. Call on
     * the render thread after {@link #configure}, at start-up or again when the
     * settings change. Needs the vertex texture backend, for the per key
     * frame normals, and tessellation shaders; without them it stays off and
     * says so in the log.
     *
     * @param requested whether smoothing was asked for
     * @param level 1 to 4, the most segments per edge are 2, 4, 8 or 16
     * @param method "PN" for curved PN triangles (the default) or "PHONG" for
     * Phong tessellation
     * @param caps the renderer's capabilities
     * @param assetManager the asset manager, to load the material
     * @return whether mesh smoothing is on
     */
    public static boolean configureSmoothing(boolean requested, int level, String method, Collection<Caps> caps,
            AssetManager assetManager) {
        int newLevel = Math.clamp(level, 1, SEGMENTS.length);
        boolean newPhong = method != null && method.trim().toUpperCase(Locale.ROOT).startsWith("PHONG");
        if (newLevel != smoothingLevel || newPhong != phongTessellation) {
            // Changed in the options, the smoothed geometries take it on their next sample
            smoothingLevel = newLevel;
            phongTessellation = newPhong;
            generation++;
        }
        String missing = null;
        if (!caps.contains(Caps.TesselationShader) || !caps.contains(Caps.GLSL400)) {
            missing = "tessellation shaders (GLSL 4.00)";
        } else if (!active) {
            missing = "the VERTEX_TEXTURE animation backend";
        }
        if (missing == null && smoothDef == null) {
            vertexDef = (MaterialDef) assetManager.loadAsset(VERTEX_MATDEF);
            smoothDef = (MaterialDef) assetManager.loadAsset(SMOOTH_MATDEF);
        }
        if (requested && missing != null) {
            LOGGER.log(Level.WARNING, "Mesh smoothing needs " + missing + ", it stays off");
        }
        boolean on = requested && missing == null;
        setSmoothing(on);
        LOGGER.log(Level.INFO, "Mesh smoothing: " + (on ? "ON, level " + smoothingLevel + ", "
                + (phongTessellation ? "Phong tessellation" : "PN triangles") : "OFF"));
        return on;
    }

    /**
     * Reads the mesh smoothing setting, case-insensitively: ON, TRUE, YES or 1
     * switch it on
     *
     * @param value the setting value, may be {@code null}
     * @return whether it asks for smoothing
     */
    public static boolean parseSmoothing(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim().toUpperCase(Locale.ROOT);
        return trimmed.equals("ON") || trimmed.equals("TRUE") || trimmed.equals("YES") || trimmed.equals("1");
    }

    /**
     * @return whether the animated models are smoothed by tessellation
     */
    public static boolean isSmoothing() {
        return smoothing && active;
    }

    /**
     * Switches mesh smoothing at run time, e.g. to compare. Tracks resample on
     * their next update. Ignored unless {@link #configureSmoothing} found the
     * GPU able to.
     *
     * @param on whether to smooth
     */
    public static void setSmoothing(boolean on) {
        boolean value = on && smoothDef != null;
        if (value != smoothing) {
            smoothing = value;
            generation++;
        }
    }

    /**
     * Whether smoothing can draw this geometry: a {@code DkLighting/Vertex}
     * material (or its smoothing stand-in) and a triangle list
     *
     * @param geometry the geometry
     * @return whether it can be tessellated
     */
    static boolean canSmooth(Geometry geometry) {
        Material material = geometry.getMaterial();
        if (material == null || geometry.getMesh() == null) {
            return false;
        }
        String def = material.getMaterialDef().getAssetName();
        Mesh.Mode mode = geometry.getMesh().getMode();
        return (VERTEX_MATDEF.equals(def) || SMOOTH_MATDEF.equals(def))
                && (mode == Mesh.Mode.Triangles || mode == Mesh.Mode.Patch);
    }

    /**
     * Gives the geometry its smoothing material, or its own back. Every
     * parameter is copied over, the smoothing one is shared by every geometry
     * that shared the original. The mesh must be a patch list when on, a
     * triangle list when off.
     *
     * @param geometry the geometry
     * @param on whether to smooth it
     */
    static void useSmoothMaterial(Geometry geometry, boolean on) {
        Material current = geometry.getMaterial();
        boolean smooth = SMOOTH_MATDEF.equals(current.getMaterialDef().getAssetName());
        if (on && !smooth) {
            Material material;
            synchronized (SMOOTH_MATERIALS) {
                WeakReference<Material> reference = SMOOTH_MATERIALS.get(current);
                material = reference == null ? null : reference.get();
                if (material == null) {
                    material = copy(current, smoothDef);
                    material.setFloat("TessellationSegments", SEGMENTS[smoothingLevel - 1]);
                    material.setBoolean("PhongTessellation", phongTessellation);
                    SMOOTH_MATERIALS.put(current, new WeakReference<>(material));
                    ORIGINAL_MATERIALS.put(material, new WeakReference<>(current));
                }
            }
            // A material kept from earlier may predate the geometry's current highlight
            for (String name : new String[]{"Tint", "TintOverlay"}) {
                MatParam highlight = current.getParam(name);
                if (highlight != null) {
                    material.setParam(name, highlight.getVarType(), highlight.getValue());
                } else {
                    material.clearParam(name);
                }
            }
            geometry.setMaterial(material);
        } else if (on) {
            // The level or the method may have changed since it was made
            float segments = SEGMENTS[smoothingLevel - 1];
            if (!Float.valueOf(segments).equals(current.getParamValue("TessellationSegments"))
                    || !Boolean.valueOf(phongTessellation).equals(current.getParamValue("PhongTessellation"))) {
                current.setFloat("TessellationSegments", segments);
                current.setBoolean("PhongTessellation", phongTessellation);
            }
        } else if (smooth) {
            WeakReference<Material> reference = ORIGINAL_MATERIALS.get(current);
            Material original = reference == null ? null : reference.get();
            if (original != null) {
                copyParameters(current, original);
            } else {
                // Nothing else used it any more, a new one does
                original = copy(current, vertexDef);
            }
            geometry.setMaterial(original);
        }
    }

    private static Material copy(Material from, MaterialDef def) {
        Material material = new Material(def);
        copyParameters(from, material);
        material.setName(from.getName());
        material.getAdditionalRenderState().set(from.getAdditionalRenderState());
        material.setTransparent(from.isTransparent());
        material.setReceivesShadows(from.isReceivesShadows());
        return material;
    }

    private static void copyParameters(Material from, Material to) {
        for (MatParam parameter : from.getParams()) {
            if (to.getMaterialDef().getMaterialParam(parameter.getName()) != null) {
                to.setParam(parameter.getName(), parameter.getVarType(), parameter.getValue());
            }
        }
    }

    /**
     * @return whether creature vertex animation runs in the vertex shader
     */
    public static boolean isActive() {
        return active;
    }

    /**
     * Switches the backend at run time, e.g. to compare the two. Tracks
     * resample on their next update. Ignored when the GPU can't do it.
     *
     * @param on whether to use the vertex texture backend
     */
    public static void setActive(boolean on) {
        boolean value = on && supported;
        if (value != active) {
            active = value;
            generation++;
        }
    }

    /**
     * @return whether the GPU can run the vertex texture backend, known once
     * {@link #configure} ran
     */
    public static boolean isSupported() {
        return supported;
    }

    /**
     * Marks the GPU as able to run the backend, for tests without a renderer
     *
     * @param value whether it is supported
     */
    static void setSupported(boolean value) {
        supported = value;
    }

    static int getGeneration() {
        return generation;
    }

    static int getMaxTextureSize() {
        return maxTextureSize;
    }

    /**
     * Whether the geometry's material can draw vertex textures. Others, if
     * any, stay on the CPU.
     *
     * @param geometry the geometry
     * @return whether its material has the vertex texture inputs
     */
    static boolean supports(Geometry geometry) {
        return geometry.getMaterial() != null && geometry.getMaterial().getMaterialDef().getMaterialParam(MAP) != null;
    }

    /**
     * Sets the shader inputs of one geometry
     *
     * @param geometry the animated geometry
     * @param texture its key frames
     * @param frameA the key frame at weight 0
     * @param frameB the key frame at weight 1
     * @param weight the weight
     */
    static void apply(Geometry geometry, VertexAnimationTexture texture, int frameA, int frameB, float weight) {
        set(geometry, MAP, VarType.Texture2D, texture.getTexture());
        set(geometry, WIDTH, VarType.Int, texture.getWidth());
        set(geometry, VERTICES, VarType.Int, texture.getVertexCount());
        set(geometry, FRAME_A, VarType.Int, frameA);
        set(geometry, FRAME_B, VarType.Int, frameB);
        set(geometry, WEIGHT, VarType.Float, weight);
        Object normals = isSmoothing() ? texture.getNormalTexture() : null;
        if (normals != null) {
            set(geometry, NORMAL_MAP, VarType.Texture2D, normals);
        } else {
            remove(geometry, NORMAL_MAP);
        }
    }

    private static void remove(Geometry geometry, String name) {
        for (MatParamOverride override : geometry.getLocalMatParamOverrides().getArray()) {
            if (name.equals(override.getName())) {
                geometry.removeMatParamOverride(override);
            }
        }
    }

    /**
     * Removes the shader inputs, the geometry draws its position buffer again
     *
     * @param geometry the geometry
     */
    static void clear(Geometry geometry) {
        for (MatParamOverride override : geometry.getLocalMatParamOverrides().getArray()) {
            for (String name : NAMES) {
                if (name.equals(override.getName())) {
                    geometry.removeMatParamOverride(override);
                }
            }
        }
    }

    private static void set(Geometry geometry, String name, VarType type, Object value) {
        for (MatParamOverride override : geometry.getLocalMatParamOverrides().getArray()) {
            if (name.equals(override.getName())) {
                if (!value.equals(override.getValue())) {
                    override.setValue(value);
                }
                return;
            }
        }
        geometry.addMatParamOverride(new MatParamOverride(type, name, value));
    }

    static void recordSample(long nanos, long uploadedBytes) {
        SAMPLE_NANOS.add(nanos);
        UPLOADED_BYTES.add(uploadedBytes);
        SAMPLES.increment();
    }

    static void recordTexture(long bytes) {
        TEXTURE_BYTES.add(bytes);
    }

    /**
     * Animation cost since the last call, for measurements: the time spent in
     * {@link PoseTrack} sampling, the bytes it sent to the GPU per frame
     * (position buffers, or the per-geometry uniforms), the samples taken and
     * the bytes of key frame textures created
     *
     * @return {nanoseconds, uploaded bytes, samples, texture bytes}
     */
    public static long[] drainStatistics() {
        return new long[]{SAMPLE_NANOS.sumThenReset(), UPLOADED_BYTES.sumThenReset(), SAMPLES.sumThenReset(),
            TEXTURE_BYTES.sumThenReset()};
    }
}
