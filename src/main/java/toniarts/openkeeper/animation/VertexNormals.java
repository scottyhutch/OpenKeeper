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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Per key frame vertex normals of an animated mesh, for mesh smoothing. The
 * KMF meshes repeat a vertex wherever its texture coordinates change (UV
 * seams), so the normal of a vertex is the area weighted average of the faces
 * around every vertex at the same place. Area, not angle, weighting: the tiny
 * end caps of the thin shafts and spikes these models are full of then barely
 * tilt the normals at the shaft's ends, which would blow the smoothed shaft up
 * into a lemon. Vertices are welded when they share
 * the position in every key frame, so they move as one and the smoothed
 * surface can't open a crack between them.
 * <p>
 * Where the faces around a welded position cancel out, as on a double-sided
 * sheet whose two sides share their vertex positions, each vertex keeps the
 * normal of its own faces.
 */
public final class VertexNormals {

    /**
     * Below this length (relative to the summed face weights) the faces
     * around a position have cancelled out
     */
    private static final float CANCELLED = 0.05f;

    private VertexNormals() {
    }

    /**
     * Groups the vertices that sit at exactly the same position in every key
     * frame
     *
     * @param framePositions per key frame, XYZ of every vertex
     * @param vertices the vertex count
     * @return per vertex, the lowest vertex index of its group
     */
    public static int[] weld(float[][] framePositions, int vertices) {
        int[] groups = new int[vertices];
        Map<Long, List<Integer>> byHash = new HashMap<>();
        for (int vertex = 0; vertex < vertices; vertex++) {
            groups[vertex] = vertex;
            long hash = 17;
            for (float[] positions : framePositions) {
                for (int axis = 0; axis < 3; axis++) {
                    hash = hash * 31 + bits(positions[vertex * 3 + axis]);
                }
            }
            List<Integer> candidates = byHash.computeIfAbsent(hash, h -> new ArrayList<>(2));
            for (int candidate : candidates) {
                if (samePlace(framePositions, candidate, vertex)) {
                    groups[vertex] = candidate;
                    break;
                }
            }
            if (groups[vertex] == vertex) {
                candidates.add(vertex);
            }
        }
        return groups;
    }

    /**
     * Computes the normals of every key frame
     *
     * @param framePositions per key frame, XYZ of every vertex
     * @param triangles the triangle list, three vertex indices a triangle
     * @param baseNormals the mesh's own normals, XYZ of every vertex, used
     * for vertices without faces and to keep the faces' winding the right way
     * out; may be {@code null}
     * @return per key frame, the unit normal XYZ of every vertex
     */
    public static float[][] compute(float[][] framePositions, int[] triangles, float[] baseNormals) {
        int vertices = framePositions.length == 0 ? 0 : framePositions[0].length / 3;
        int[] groups = weld(framePositions, vertices);
        float[][] result = new float[framePositions.length][];
        for (int frame = 0; frame < framePositions.length; frame++) {
            result[frame] = computeFrame(framePositions[frame], triangles, groups, baseNormals);
        }
        if (baseNormals != null && result.length > 0 && windingIsInverted(result[0], baseNormals)) {
            for (float[] normals : result) {
                for (int i = 0; i < normals.length; i++) {
                    normals[i] = -normals[i];
                }
            }
        }
        return result;
    }

    private static float[] computeFrame(float[] positions, int[] triangles, int[] groups, float[] baseNormals) {
        int vertices = groups.length;
        float[] own = new float[vertices * 3];
        float[] ownWeight = new float[vertices];
        float[] edgeA = new float[3];
        float[] edgeB = new float[3];
        float[] face = new float[3];
        for (int i = 0; i + 2 < triangles.length; i += 3) {
            int[] corners = {triangles[i], triangles[i + 1], triangles[i + 2]};
            difference(positions, corners[1], corners[0], edgeA);
            difference(positions, corners[2], corners[0], edgeB);
            cross(edgeA, edgeB, face);
            float length = length(face); // Twice the area
            if (length <= 0f || !Float.isFinite(length)) {
                continue; // Degenerate, no direction
            }
            for (int vertex : corners) {
                for (int axis = 0; axis < 3; axis++) {
                    own[vertex * 3 + axis] += face[axis];
                }
                ownWeight[vertex] += length;
            }
        }

        // Sum the faces of every welded group
        float[] welded = new float[vertices * 3];
        float[] weldedWeight = new float[vertices];
        for (int vertex = 0; vertex < vertices; vertex++) {
            int group = groups[vertex];
            for (int axis = 0; axis < 3; axis++) {
                welded[group * 3 + axis] += own[vertex * 3 + axis];
            }
            weldedWeight[group] += ownWeight[vertex];
        }

        float[] normals = new float[vertices * 3];
        for (int vertex = 0; vertex < vertices; vertex++) {
            int group = groups[vertex];
            if (!normalize(welded, group, weldedWeight[group], normals, vertex)
                    && !normalize(own, vertex, ownWeight[vertex], normals, vertex)) {
                fallback(baseNormals, vertex, normals);
            }
        }
        return normals;
    }

    /**
     * Writes the unit normal of a sum, if the faces in it don't cancel out
     *
     * @return whether it had a direction
     */
    private static boolean normalize(float[] sums, int index, float weight, float[] store, int vertex) {
        float x = sums[index * 3];
        float y = sums[index * 3 + 1];
        float z = sums[index * 3 + 2];
        float length = (float) Math.sqrt(x * x + y * y + z * z);
        if (weight <= 0f || length <= CANCELLED * weight) {
            return false;
        }
        store[vertex * 3] = x / length;
        store[vertex * 3 + 1] = y / length;
        store[vertex * 3 + 2] = z / length;
        return true;
    }

    private static void fallback(float[] baseNormals, int vertex, float[] store) {
        float x = 0f;
        float y = 1f;
        float z = 0f;
        if (baseNormals != null) {
            float length = (float) Math.sqrt(baseNormals[vertex * 3] * baseNormals[vertex * 3]
                    + baseNormals[vertex * 3 + 1] * baseNormals[vertex * 3 + 1]
                    + baseNormals[vertex * 3 + 2] * baseNormals[vertex * 3 + 2]);
            if (length > 0f) {
                x = baseNormals[vertex * 3] / length;
                y = baseNormals[vertex * 3 + 1] / length;
                z = baseNormals[vertex * 3 + 2] / length;
            }
        }
        store[vertex * 3] = x;
        store[vertex * 3 + 1] = y;
        store[vertex * 3 + 2] = z;
    }

    /**
     * Whether the faces wind the other way round than the mesh's own normals
     * point, then the computed normals point inwards
     */
    private static boolean windingIsInverted(float[] normals, float[] baseNormals) {
        double agreement = 0;
        for (int i = 0; i + 2 < normals.length && i + 2 < baseNormals.length; i += 3) {
            agreement += normals[i] * baseNormals[i] + normals[i + 1] * baseNormals[i + 1]
                    + normals[i + 2] * baseNormals[i + 2];
        }
        return agreement < 0;
    }

    private static void difference(float[] positions, int to, int from, float[] store) {
        for (int axis = 0; axis < 3; axis++) {
            store[axis] = positions[to * 3 + axis] - positions[from * 3 + axis];
        }
    }

    private static void cross(float[] a, float[] b, float[] store) {
        store[0] = a[1] * b[2] - a[2] * b[1];
        store[1] = a[2] * b[0] - a[0] * b[2];
        store[2] = a[0] * b[1] - a[1] * b[0];
    }

    private static float length(float[] v) {
        return (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
    }

    private static boolean samePlace(float[][] framePositions, int a, int b) {
        for (float[] positions : framePositions) {
            for (int axis = 0; axis < 3; axis++) {
                if (bits(positions[a * 3 + axis]) != bits(positions[b * 3 + axis])) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * The bits of a coordinate, with -0 and 0 the same
     */
    private static int bits(float value) {
        return Float.floatToIntBits(value == 0f ? 0f : value);
    }
}
