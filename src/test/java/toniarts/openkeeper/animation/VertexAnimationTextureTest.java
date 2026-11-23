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
import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.shader.VarType;
import java.nio.FloatBuffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The VERTEX_TEXTURE backend has to draw exactly what the CPU backend writes
 * into the position buffer. The shader's texel lookup and blend are repeated
 * here on the packed texture data, for synthetic sparse poses.
 */
class VertexAnimationTextureTest {

    private static final int VERTICES = 7;
    private static final float[] TIMES = {1f / 30, 0.2f, 0.45f, 0.5f, 0.9f};

    @BeforeEach
    void enableBackend() {
        VertexTextureBackend.setSupported(true);
    }

    @AfterEach
    void restoreBackend() {
        VertexTextureBackend.setActive(false);
        VertexTextureBackend.setSupported(false);
    }

    @Test
    void shaderBlendOfThePackedFramesMatchesTheCpuPoseAtEveryTime() {
        PoseTrack.PoseFrame[] frames = frames();
        PoseTrack cpu = track(frames);
        PoseTrack gpu = track(frames);
        float[] shaded = new float[VERTICES * 3];
        for (int i = -10; i <= 120; i++) {
            double time = i / 97.0;
            cpu.getDataAtTime(time, null);
            VertexTextureBackend.setActive(true);
            gpu.getDataAtTime(time, null);
            VertexTextureBackend.setActive(false);
            VertexAnimationTexture texture = gpu.getVertexAnimationTexture();
            texture.blend(intOverride(gpu, VertexTextureBackend.FRAME_A), intOverride(gpu, VertexTextureBackend.FRAME_B),
                    (Float) override(gpu, VertexTextureBackend.WEIGHT).getValue(), shaded);
            assertArrayEquals(positions(cpu), shaded, 0f, "time " + time);
        }
    }

    @Test
    void framesWrapOntoMoreRowsWithoutChangingTheLookup() {
        PoseTrack track = track(frames());
        VertexAnimationTexture wide = VertexAnimationTexture.of(track, 4096);
        VertexAnimationTexture narrow = VertexAnimationTexture.of(track, 8);
        assertEquals(1, wide.getHeight());
        assertEquals(8, narrow.getWidth());
        assertEquals((VERTICES * TIMES.length + 7) / 8, narrow.getHeight());
        float[] a = new float[VERTICES * 3];
        float[] b = new float[VERTICES * 3];
        float[] decoded = new float[VERTICES * 3];
        for (int frame = 0; frame < TIMES.length; frame++) {
            wide.blend(frame, frame, 0, a);
            narrow.blend(frame, frame, 0, b);
            track.copyFrame(frame, decoded);
            assertArrayEquals(decoded, a, 0f);
            assertArrayEquals(decoded, b, 0f);
        }
        assertNull(VertexAnimationTexture.of(track, 2), "doesn't fit, stays on the CPU");
    }

    @Test
    void instancesShareTheTextureAndMeshAndPickingGetsTheShownPose() {
        PoseTrack.PoseFrame[] frames = frames();
        PoseTrack first = track(frames);
        Geometry clone = first.getTarget().clone();
        PoseTrack second = new PoseTrack(clone, TIMES.clone(), frames);
        PoseTrack cpu = track(frames);
        cpu.getDataAtTime(0.3, null);
        VertexTextureBackend.setActive(true);
        first.getDataAtTime(0.3, null);
        second.getDataAtTime(0.3, null);
        assertSame(first.getVertexAnimationTexture(), second.getVertexAnimationTexture());
        assertSame(first.getTarget().getMesh(), second.getTarget().getMesh());
        assertSame(first.getVertexAnimationTexture().getTexture(), override(first, VertexTextureBackend.MAP).getValue());

        // A pick writes the pose into a mesh of its own, the shared one is left alone
        Mesh shared = second.getTarget().getMesh();
        float[] before = positions(second);
        second.materializePose();
        assertArrayEquals(positions(cpu), positions(second), 0f);
        assertTrue(shared != second.getTarget().getMesh());
        assertArrayEquals(before, floats(shared), 0f);
    }

    @Test
    void switchingBackToTheCpuRemovesTheShaderInputs() {
        PoseTrack track = track(frames());
        PoseTrack cpu = track(frames());
        VertexTextureBackend.setActive(true);
        track.getDataAtTime(0.6, null);
        assertNotNull(override(track, VertexTextureBackend.FRAME_A));
        VertexTextureBackend.setActive(false);
        track.getDataAtTime(0.6, null);
        cpu.getDataAtTime(0.6, null);
        assertNull(override(track, VertexTextureBackend.FRAME_A));
        assertArrayEquals(positions(cpu), positions(track), 0f);
    }

    @Test
    void materialsWithoutTheShaderInputsStayOnTheCpu() {
        PoseTrack track = track(frames());
        track.getTarget().setMaterial(new Material(new MaterialDef(null, "plain")));
        PoseTrack cpu = track(frames());
        cpu.getDataAtTime(0.6, null);
        VertexTextureBackend.setActive(true);
        track.getDataAtTime(0.6, null);
        assertNull(override(track, VertexTextureBackend.FRAME_A));
        assertArrayEquals(positions(cpu), positions(track), 0f);
    }

    /**
     * Sparse pose pairs over a 7 vertex mesh. Like the converted KMF data,
     * adjacent frames address different vertex sets, and some pairs blend
     * their two poses by a weight of their own.
     */
    private static PoseTrack.PoseFrame[] frames() {
        PoseTrack.PoseFrame[] frames = new PoseTrack.PoseFrame[TIMES.length];
        for (int f = 0; f < frames.length; f++) {
            int[] first = f % 2 == 0 ? new int[]{0, 2, 4, 6} : new int[]{1, 2, 3};
            int[] second = f % 3 == 0 ? new int[]{5} : new int[]{0, 5};
            frames[f] = new PoseTrack.PoseFrame(new Pose[]{
                pose(first, f, 0.1f), pose(first, f, 1.3f), pose(second, f, -0.7f), pose(second, f, 2.1f)},
                    new float[]{f / 4f, f % 2 == 0 ? 0.35f : 1f});
        }
        return frames;
    }

    private static Pose pose(int[] indices, int frame, float scale) {
        Vector3f[] offsets = new Vector3f[indices.length];
        for (int i = 0; i < indices.length; i++) {
            float v = indices[i] + frame * scale;
            offsets[i] = new Vector3f(v * 0.37f, (float) Math.sin(v) * 3.1f, v * v * 0.013f - 1f);
        }
        return new Pose("p" + frame, offsets, indices);
    }

    private static PoseTrack track(PoseTrack.PoseFrame[] frames) {
        float[] bind = new float[VERTICES * 3];
        for (int i = 0; i < bind.length; i++) {
            bind[i] = (i % 5) * 0.25f - 0.4f;
        }
        Mesh mesh = new Mesh();
        mesh.setBuffer(VertexBuffer.Type.Position, 3, bind.clone());
        mesh.setBuffer(VertexBuffer.Type.BindPosePosition, 3, bind.clone());
        Geometry target = new Geometry("animated", mesh);
        MaterialDef def = new MaterialDef(null, "vertex-texture");
        def.addMaterialParam(VarType.Texture2D, VertexTextureBackend.MAP, null);
        target.setMaterial(new Material(def));
        return new PoseTrack(target, TIMES.clone(), frames);
    }

    private static MatParamOverride override(PoseTrack track, String name) {
        for (MatParamOverride override : track.getTarget().getLocalMatParamOverrides()) {
            if (override.getName().equals(name)) {
                return override;
            }
        }
        return null;
    }

    private static int intOverride(PoseTrack track, String name) {
        return (Integer) override(track, name).getValue();
    }

    private static float[] positions(PoseTrack track) {
        return floats(track.getTarget().getMesh());
    }

    private static float[] floats(Mesh mesh) {
        FloatBuffer buffer = (FloatBuffer) mesh.getBuffer(VertexBuffer.Type.Position).getData();
        float[] result = new float[buffer.limit()];
        buffer.get(0, result);
        return result;
    }
}
