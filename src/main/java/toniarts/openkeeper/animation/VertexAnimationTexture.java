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

import com.jme3.math.FastMath;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Collections;
import java.util.List;

/**
 * The key frames of one animated geometry, decoded once to full positions and
 * packed into an RGBA32F texture for the VERTEX_TEXTURE backend. Texel
 * {@code frame * vertices + vertex} holds that vertex in that key frame, row by
 * row. Every instance of the model shares one of these; only the two frame
 * indices and the weight are per geometry. See
 * {@code Shaders/VertexAnimation.glsllib}.
 */
public final class VertexAnimationTexture {

    /**
     * Widest row used, also within the smallest texture size GL 3 guarantees
     */
    public static final int MAX_WIDTH = 1024;

    private final int vertices;
    private final int frames;
    private final int width;
    private final int height;
    private final ByteBuffer data;
    private final FloatBuffer texels;
    private Texture2D texture;
    private ByteBuffer normalData;
    private Texture2D normalTexture;

    private VertexAnimationTexture(int vertices, int frames, int width, int height, ByteBuffer data) {
        this.vertices = vertices;
        this.frames = frames;
        this.width = width;
        this.height = height;
        this.data = data;
        this.texels = data.asFloatBuffer();
    }

    /**
     * Decodes every key frame of a track
     *
     * @param track the track
     * @param maxTextureSize the largest texture side the renderer takes
     * @return the packed key frames, or {@code null} if they don't fit in one
     * texture
     */
    public static VertexAnimationTexture of(PoseTrack track, int maxTextureSize) {
        int vertices = track.getTarget().getMesh().getVertexCount();
        int frames = track.getTimes().length;
        float[] frame = new float[vertices * 3];
        float[][] decoded = new float[frames][];
        for (int i = 0; i < frames; i++) {
            track.copyFrame(i, frame);
            decoded[i] = frame.clone();
        }
        return pack(vertices, decoded, maxTextureSize);
    }

    /**
     * Packs decoded key frames
     *
     * @param vertices the vertex count
     * @param framePositions per key frame, XYZ of every vertex
     * @param maxTextureSize the largest texture side the renderer takes
     * @return the packed key frames, or {@code null} if they don't fit in one
     * texture
     */
    public static VertexAnimationTexture pack(int vertices, float[][] framePositions, int maxTextureSize) {
        int frames = framePositions.length;
        long texelCount = (long) vertices * frames;
        int width = (int) Math.min(Math.min(MAX_WIDTH, maxTextureSize), texelCount);
        if (vertices < 1 || frames < 1 || width < 1) {
            return null;
        }
        long height = (texelCount + width - 1) / width;
        if (height > maxTextureSize) {
            return null;
        }
        ByteBuffer data = BufferUtils.createByteBuffer((int) (width * height * 16));
        FloatBuffer texels = data.asFloatBuffer();
        for (int frame = 0; frame < frames; frame++) {
            float[] positions = framePositions[frame];
            if (positions.length != vertices * 3) {
                throw new IllegalArgumentException("Key frame " + frame + " has the wrong vertex count");
            }
            for (int vertex = 0; vertex < vertices; vertex++) {
                int texel = (frame * vertices + vertex) * 4;
                texels.put(texel, positions[vertex * 3]);
                texels.put(texel + 1, positions[vertex * 3 + 1]);
                texels.put(texel + 2, positions[vertex * 3 + 2]);
                texels.put(texel + 3, 1f);
            }
        }
        return new VertexAnimationTexture(vertices, frames, width, (int) height, data);
    }

    /**
     * Reads one vertex of one key frame the way the vertex shader does
     *
     * @param frame the key frame
     * @param vertex the vertex (gl_VertexID)
     * @param store where to put XYZ, at {@code offset}
     * @param offset the index of X in {@code store}
     */
    public void fetch(int frame, int vertex, float[] store, int offset) {
        int texel = frame * vertices + vertex;
        int x = texel % width;
        int y = texel / width;
        int index = (y * width + x) * 4;
        store[offset] = texels.get(index);
        store[offset + 1] = texels.get(index + 1);
        store[offset + 2] = texels.get(index + 2);
    }

    /**
     * Blends two key frames the way the vertex shader does
     *
     * @param frameA the key frame at weight 0
     * @param frameB the key frame at weight 1
     * @param weight the weight
     * @param store XYZ of every vertex
     */
    public void blend(int frameA, int frameB, float weight, float[] store) {
        float[] a = new float[3];
        float[] b = new float[3];
        for (int vertex = 0; vertex < vertices; vertex++) {
            fetch(frameA, vertex, a, 0);
            fetch(frameB, vertex, b, 0);
            for (int axis = 0; axis < 3; axis++) {
                store[vertex * 3 + axis] = a[axis] + (b[axis] - a[axis]) * weight;
            }
        }
    }

    /**
     * Get the texture, created on first use. Nearest sampling, no mipmaps, so
     * texelFetch returns the stored floats unchanged.
     *
     * @return the texture
     */
    public synchronized Texture2D getTexture() {
        if (texture == null) {
            Image image = new Image(Image.Format.RGBA32F, width, height, data, ColorSpace.Linear);
            texture = new Texture2D(image);
            texture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
            texture.setMagFilter(Texture.MagFilter.Nearest);
            texture.setWrap(Texture.WrapMode.EdgeClamp);
            texture.setName("VertexAnimation");
        }
        return texture;
    }

    /**
     * Computes the normals of every key frame for mesh smoothing (see
     * {@link VertexNormals}) and packs them like the positions, texel for
     * texel, as half floats. Done once, later calls do nothing.
     *
     * @param triangles the mesh's triangle list
     * @param baseNormals the mesh's own normals, XYZ of every vertex, may be
     * {@code null}
     */
    public void packNormals(int[] triangles, float[] baseNormals) {
        packNormals(List.of(this), List.of(triangles), Collections.singletonList(baseNormals));
    }

    /**
     * Computes the normals of the key frames of several meshes as one, e.g.
     * the meshes of one model that each have a material of their own, so the
     * vertices on the seams between them are welded too. They must have the
     * same key frames in the same space; if the frame counts differ, each
     * mesh is done on its own. Meshes that already have normals keep them.
     *
     * @param textures the meshes' key frames
     * @param triangles per mesh, its triangle list
     * @param baseNormals per mesh, its own normals, may be or hold
     * {@code null}
     */
    public static void packNormals(List<VertexAnimationTexture> textures, List<int[]> triangles,
            List<float[]> baseNormals) {
        int frames = textures.get(0).frames;
        for (VertexAnimationTexture texture : textures) {
            if (texture.frames != frames) {
                for (int i = 0; i < textures.size(); i++) {
                    packNormals(List.of(textures.get(i)), List.of(triangles.get(i)),
                            Collections.singletonList(baseNormals.get(i)));
                }
                return;
            }
        }
        int total = 0;
        int indices = 0;
        boolean anyBase = false;
        for (int i = 0; i < textures.size(); i++) {
            total += textures.get(i).vertices;
            indices += triangles.get(i).length - triangles.get(i).length % 3;
            anyBase |= baseNormals.get(i) != null;
        }
        float[][] framePositions = new float[frames][total * 3];
        int[] allTriangles = new int[indices];
        float[] allBase = anyBase ? new float[total * 3] : null;
        int offset = 0;
        int index = 0;
        for (int i = 0; i < textures.size(); i++) {
            VertexAnimationTexture texture = textures.get(i);
            for (int frame = 0; frame < frames; frame++) {
                for (int vertex = 0; vertex < texture.vertices; vertex++) {
                    texture.fetch(frame, vertex, framePositions[frame], (offset + vertex) * 3);
                }
            }
            int[] own = triangles.get(i);
            for (int j = 0; j < own.length - own.length % 3; j++) {
                allTriangles[index++] = own[j] + offset;
            }
            if (allBase != null && baseNormals.get(i) != null) {
                System.arraycopy(baseNormals.get(i), 0, allBase, offset * 3,
                        Math.min(baseNormals.get(i).length, texture.vertices * 3));
            }
            offset += texture.vertices;
        }
        float[][] normals = VertexNormals.compute(framePositions, allTriangles, allBase);
        offset = 0;
        for (VertexAnimationTexture texture : textures) {
            texture.setNormals(normals, offset);
            offset += texture.vertices;
        }
    }

    private synchronized void setNormals(float[][] normals, int offset) {
        if (normalData != null) {
            return;
        }
        ByteBuffer packed = BufferUtils.createByteBuffer(width * height * 8);
        for (int frame = 0; frame < frames; frame++) {
            for (int vertex = 0; vertex < vertices; vertex++) {
                int texel = (frame * vertices + vertex) * 8;
                int source = (offset + vertex) * 3;
                packed.putShort(texel, FastMath.convertFloatToHalf(normals[frame][source]));
                packed.putShort(texel + 2, FastMath.convertFloatToHalf(normals[frame][source + 1]));
                packed.putShort(texel + 4, FastMath.convertFloatToHalf(normals[frame][source + 2]));
                packed.putShort(texel + 6, FastMath.convertFloatToHalf(0f));
            }
        }
        normalData = packed;
    }

    /**
     * @return whether {@link #packNormals(int[], float[])} has run
     */
    public synchronized boolean hasNormals() {
        return normalData != null;
    }

    /**
     * Reads the normal of one vertex in one key frame the way the vertex
     * shader does
     *
     * @param frame the key frame
     * @param vertex the vertex (gl_VertexID)
     * @param store where to put XYZ, at {@code offset}
     * @param offset the index of X in {@code store}
     */
    public synchronized void fetchNormal(int frame, int vertex, float[] store, int offset) {
        int texel = (frame * vertices + vertex) * 8;
        for (int axis = 0; axis < 3; axis++) {
            store[offset + axis] = FastMath.convertHalfToFloat(normalData.getShort(texel + axis * 2));
        }
    }

    /**
     * Get the normal texture, created on first use, after
     * {@link #packNormals(int[], float[])}. Same layout and sampling as
     * {@link #getTexture()}.
     *
     * @return the texture, {@code null} without normals
     */
    public synchronized Texture2D getNormalTexture() {
        if (normalTexture == null && normalData != null) {
            Image image = new Image(Image.Format.RGBA16F, width, height, normalData, ColorSpace.Linear);
            normalTexture = new Texture2D(image);
            normalTexture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
            normalTexture.setMagFilter(Texture.MagFilter.Nearest);
            normalTexture.setWrap(Texture.WrapMode.EdgeClamp);
            normalTexture.setName("VertexAnimationNormals");
        }
        return normalTexture;
    }

    /**
     * @return the vertex count
     */
    public int getVertexCount() {
        return vertices;
    }

    /**
     * @return the key frame count
     */
    public int getFrameCount() {
        return frames;
    }

    /**
     * @return the texels in a row
     */
    public int getWidth() {
        return width;
    }

    /**
     * @return the rows
     */
    public int getHeight() {
        return height;
    }

    /**
     * @return the texture's size on the GPU
     */
    public long getBytes() {
        return (long) width * height * 16;
    }
}
