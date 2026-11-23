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

import com.jme3.material.MatParamOverride;
import com.jme3.renderer.Caps;
import com.jme3.renderer.Limits;
import com.jme3.scene.Geometry;
import com.jme3.shader.VarType;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Collection;
import java.util.Map;
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
    private static final String[] NAMES = {MAP, WIDTH, VERTICES, FRAME_A, FRAME_B, WEIGHT};

    private static volatile boolean supported;
    private static volatile boolean active;
    private static volatile int generation;
    private static volatile int maxTextureSize = VertexAnimationTexture.MAX_WIDTH;

    private static final LongAdder SAMPLE_NANOS = new LongAdder();
    private static final LongAdder UPLOADED_BYTES = new LongAdder();
    private static final LongAdder SAMPLES = new LongAdder();
    private static final LongAdder TEXTURE_BYTES = new LongAdder();

    private VertexTextureBackend() {
    }

    /**
     * Chooses the backend. Call once on the render thread, before models are
     * shown.
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
        LOGGER.log(Level.INFO, "Animation backend: " + (on ? "VERTEX_TEXTURE" : "CPU") + " (requested " + requested + ")");
        return on;
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
