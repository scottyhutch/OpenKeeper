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

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Mesh smoothing needs one normal per place: a vertex repeated at a UV seam
 * must get the same normal as its twin, or the smoothed surfaces on both sides
 * of the seam bulge differently and open a crack.
 */
class VertexNormalsTest {

    private static final float EPSILON = 1e-5f;

    /**
     * A roof: two faces meeting at the ridge 0-1 (z = 0, y = 1), sloping down
     * to y = 0 at z = -1 and z = 1. The ridge is repeated (vertices 4 and 5)
     * for the second face, like a UV seam.
     */
    private static final float[] ROOF = {
        0, 1, 0, // 0 ridge, first face
        1, 1, 0, // 1 ridge, first face
        0, 0, -1, // 2
        1, 0, -1, // 3
        0, 1, 0, // 4 = 0, second face
        1, 1, 0, // 5 = 1, second face
        0, 0, 1, // 6
        1, 0, 1 // 7
    };
    /**
     * Counter-clockwise seen from above
     */
    private static final int[] ROOF_TRIANGLES = {
        0, 1, 2, 1, 3, 2, // the face towards -z
        4, 6, 5, 5, 6, 7 // the face towards +z
    };

    @Test
    void verticesAtTheSamePlaceInEveryFrameAreWelded() {
        int[] groups = VertexNormals.weld(new float[][]{ROOF}, 8);

        assertEquals(0, groups[4]);
        assertEquals(1, groups[5]);
        assertArrayEquals(new int[]{0, 1, 2, 3}, java.util.Arrays.copyOf(groups, 4));
        assertEquals(6, groups[6]);
        assertEquals(7, groups[7]);
    }

    @Test
    void verticesThatPartInAnyFrameAreNotWelded() {
        float[] open = ROOF.clone();
        open[4 * 3 + 1] = 1.5f; // The second face's ridge lifts away, a mouth opening
        int[] groups = VertexNormals.weld(new float[][]{ROOF, open}, 8);

        assertEquals(4, groups[4], "together in the first frame only");
        assertEquals(1, groups[5], "together in both");
    }

    @Test
    void negativeZeroIsTheSamePlace() {
        float[] a = {0f, 1f, 0f, -0f, 1f, -0f};
        assertEquals(0, VertexNormals.weld(new float[][]{a}, 2)[1]);
    }

    @Test
    void theSeamTwinsGetTheSameAveragedNormal() {
        float[] normals = VertexNormals.compute(new float[][]{ROOF}, ROOF_TRIANGLES, null)[0];

        // The ridge averages both faces: straight up
        for (int vertex : new int[]{0, 1, 4, 5}) {
            assertNormal(normals, vertex, 0, 1, 0);
        }
        // The eaves only touch their own face
        float s = (float) Math.sqrt(0.5);
        assertNormal(normals, 2, 0, s, -s);
        assertNormal(normals, 6, 0, s, s);
    }

    @Test
    void withoutWeldingTheSeamWouldSplit() {
        float[] split = ROOF.clone();
        split[4 * 3 + 2] = 1e-3f; // Not quite the same place
        float[] normals = VertexNormals.compute(new float[][]{split}, ROOF_TRIANGLES, null)[0];

        assertNotEquals(normals[2], normals[4 * 3 + 2], 0.5f, "the twins differ, smoothing would crack here");
    }

    @Test
    void everyFrameHasItsOwnNormals() {
        // The second frame lays the roof flat
        float[] flat = ROOF.clone();
        for (int vertex = 0; vertex < 8; vertex++) {
            flat[vertex * 3 + 1] = 0;
        }
        float[][] normals = VertexNormals.compute(new float[][]{ROOF, flat}, ROOF_TRIANGLES, null);

        float s = (float) Math.sqrt(0.5);
        assertNormal(normals[0], 2, 0, s, -s);
        for (int vertex = 0; vertex < 8; vertex++) {
            assertNormal(normals[1], vertex, 0, 1, 0);
        }
    }

    @Test
    void bothSidesOfADoubleSidedSheetKeepTheirOwnNormals() {
        // One triangle, and the same triangle wound the other way with vertices of its own at the same places
        float[] sheet = {0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 1};
        int[] triangles = {0, 2, 1, 3, 4, 5};
        float[] normals = VertexNormals.compute(new float[][]{sheet}, triangles, null)[0];

        for (int vertex = 0; vertex < 3; vertex++) {
            assertNormal(normals, vertex, 0, 1, 0);
            assertNormal(normals, vertex + 3, 0, -1, 0);
        }
    }

    @Test
    void anOutsideInWindingFollowsTheMeshNormals() {
        int[] inverted = new int[ROOF_TRIANGLES.length];
        for (int i = 0; i < inverted.length; i += 3) {
            inverted[i] = ROOF_TRIANGLES[i];
            inverted[i + 1] = ROOF_TRIANGLES[i + 2];
            inverted[i + 2] = ROOF_TRIANGLES[i + 1];
        }
        float[] up = new float[8 * 3];
        for (int vertex = 0; vertex < 8; vertex++) {
            up[vertex * 3 + 1] = 1;
        }
        float[] normals = VertexNormals.compute(new float[][]{ROOF}, inverted, up)[0];

        assertNormal(normals, 0, 0, 1, 0);
        float s = (float) Math.sqrt(0.5);
        assertNormal(normals, 6, 0, s, s);
    }

    @Test
    void verticesWithoutFacesKeepTheMeshNormal() {
        float[] positions = {0, 0, 0, 1, 0, 0, 0, 0, 1, 5, 5, 5};
        float[] base = {0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 0, 2};
        float[] normals = VertexNormals.compute(new float[][]{positions}, new int[]{0, 2, 1}, base)[0];

        assertNormal(normals, 3, 0, 0, 1);
    }

    @Test
    void degenerateTrianglesAreIgnored() {
        float[] positions = {0, 0, 0, 1, 0, 0, 0, 0, 1, 2, 0, 0};
        float[] normals = VertexNormals.compute(new float[][]{positions}, new int[]{0, 2, 1, 0, 1, 3}, null)[0];

        assertNormal(normals, 0, 0, 1, 0);
        for (float value : normals) {
            assertFalse(Float.isNaN(value));
        }
    }

    @Test
    void theTexturePacksTheNormalsLikeThePositions() {
        float[] flat = ROOF.clone();
        for (int vertex = 0; vertex < 8; vertex++) {
            flat[vertex * 3 + 1] = 0;
        }
        VertexAnimationTexture texture = VertexAnimationTexture.pack(8, new float[][]{ROOF, flat}, 5);
        assertFalse(texture.hasNormals());

        texture.packNormals(ROOF_TRIANGLES, null);

        assertTrue(texture.hasNormals());
        assertEquals(texture.getWidth(), texture.getNormalTexture().getImage().getWidth());
        assertEquals(texture.getHeight(), texture.getNormalTexture().getImage().getHeight());
        float[] normal = new float[3];
        texture.fetchNormal(0, 5, normal, 0);
        assertArrayEquals(new float[]{0, 1, 0}, normal, 1e-3f);
        texture.fetchNormal(0, 7, normal, 0);
        float s = (float) Math.sqrt(0.5);
        assertArrayEquals(new float[]{0, s, s}, normal, 1e-3f, "half floats");
        texture.fetchNormal(1, 7, normal, 0);
        assertArrayEquals(new float[]{0, 1, 0}, normal, 1e-3f);
    }

    @Test
    void theMeshesOfOneModelAreWeldedAcrossTheirSeams() {
        // The roof's two faces in two meshes, as two materials would make them
        float[] first = java.util.Arrays.copyOfRange(ROOF, 0, 12);
        float[] second = java.util.Arrays.copyOfRange(ROOF, 12, 24);
        VertexAnimationTexture a = VertexAnimationTexture.pack(4, new float[][]{first}, 64);
        VertexAnimationTexture b = VertexAnimationTexture.pack(4, new float[][]{second}, 64);

        VertexAnimationTexture.packNormals(java.util.List.of(a, b), java.util.List.of(new int[]{0, 1, 2, 1, 3, 2},
                new int[]{0, 2, 1, 1, 2, 3}), java.util.Arrays.asList(null, null));

        float[] normal = new float[3];
        a.fetchNormal(0, 0, normal, 0);
        assertArrayEquals(new float[]{0, 1, 0}, normal, 1e-3f, "the ridge of the first mesh");
        b.fetchNormal(0, 1, normal, 0);
        assertArrayEquals(new float[]{0, 1, 0}, normal, 1e-3f, "the ridge of the second mesh");
        float s = (float) Math.sqrt(0.5);
        b.fetchNormal(0, 3, normal, 0);
        assertArrayEquals(new float[]{0, s, s}, normal, 1e-3f);
    }

    @Test
    void smoothingIsSwitchedOnByTheUsualWords() {
        for (String on : new String[]{"ON", "on", " true ", "Yes", "1"}) {
            assertTrue(VertexTextureBackend.parseSmoothing(on), on);
        }
        for (String off : new String[]{"OFF", "false", "", "2"}) {
            assertFalse(VertexTextureBackend.parseSmoothing(off), off);
        }
        assertFalse(VertexTextureBackend.parseSmoothing(null));
    }

    private static void assertNormal(float[] normals, int vertex, float x, float y, float z) {
        assertArrayEquals(new float[]{x, y, z}, java.util.Arrays.copyOfRange(normals, vertex * 3, vertex * 3 + 3), EPSILON,
                "normal of vertex " + vertex);
    }
}
