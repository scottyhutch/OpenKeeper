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
package toniarts.openkeeper.view.selection;

import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;
import com.jme3.math.Vector3f;
import java.nio.FloatBuffer;
import java.util.List;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * The top rim and four uprights, drawn as narrow solid beams. Unlike
 * wide GL lines, triangles retain their width in OpenGL core profiles.
 */
final class SelectionOutline extends Mesh {

    // Visually fitted against DK II at matching tile size. Keep the thickness in
    // world units so it follows camera zoom and viewport resolution with the box.
    private static final float HALF_THICKNESS = WorldUtils.TILE_WIDTH / 64f;
    private static final int[] BOX_INDICES = {
        0, 2, 3, 0, 3, 1, 4, 5, 7, 4, 7, 6,
        0, 1, 5, 0, 5, 4, 2, 6, 7, 2, 7, 3,
        0, 4, 6, 0, 6, 2, 1, 3, 7, 1, 7, 5
    };
    private static final int EDGE_COUNT = 8;
    private final FloatBuffer positions;

    SelectionOutline(float xExtent, float yExtent, float zExtent) {
        positions = BufferUtils.createFloatBuffer(EDGE_COUNT * 8 * 3);
        short[] indices = new short[EDGE_COUNT * BOX_INDICES.length];
        for (int edge = 0; edge < EDGE_COUNT; edge++) {
            for (int i = 0; i < BOX_INDICES.length; i++) {
                indices[edge * BOX_INDICES.length + i] = (short) (edge * 8 + BOX_INDICES[i]);
            }
        }
        setBuffer(Type.Index, 3, indices);
        setBuffer(Type.Position, 3, positions);
        setDynamic();
        updatePositions(xExtent, yExtent, zExtent);
    }

    /**
     * An outline along any line segments, such as edges bent with the terrain.
     * Each segment gets a beam of the same thickness, lengthened by the
     * thickness at both ends so that neighbouring beams close their joints.
     *
     * @param segments pairs of points, one pair per segment
     */
    SelectionOutline(List<Vector3f> segments) {
        int count = segments.size() / 2;
        int[] indices = new int[count * BOX_INDICES.length];
        positions = BufferUtils.createFloatBuffer(count * 8 * 3);
        Vector3f along = new Vector3f();
        Vector3f across = new Vector3f();
        Vector3f up = new Vector3f();
        for (int segment = 0; segment < count; segment++) {
            Vector3f from = segments.get(segment * 2);
            Vector3f to = segments.get(segment * 2 + 1);
            to.subtract(from, along).normalizeLocal();
            along.cross(Math.abs(along.y) < 0.9f ? Vector3f.UNIT_Y : Vector3f.UNIT_X, across).normalizeLocal();
            along.cross(across, up);
            for (int vertex = 0; vertex < 8; vertex++) {
                Vector3f end = (vertex & 1) == 0 ? from : to;
                float alongSide = (vertex & 1) == 0 ? -1 : 1;
                float acrossSide = (vertex & 2) == 0 ? -1 : 1;
                float upSide = (vertex & 4) == 0 ? -1 : 1;
                positions.put(end.x + (along.x * alongSide + across.x * acrossSide + up.x * upSide) * HALF_THICKNESS);
                positions.put(end.y + (along.y * alongSide + across.y * acrossSide + up.y * upSide) * HALF_THICKNESS);
                positions.put(end.z + (along.z * alongSide + across.z * acrossSide + up.z * upSide) * HALF_THICKNESS);
            }
            for (int i = 0; i < BOX_INDICES.length; i++) {
                indices[segment * BOX_INDICES.length + i] = segment * 8 + BOX_INDICES[i];
            }
        }
        positions.flip();
        setBuffer(Type.Index, 3, indices);
        setBuffer(Type.Position, 3, positions);
        updateBound();
    }

    void updatePositions(float xExtent, float yExtent, float zExtent) {
        float[] extents = {xExtent, yExtent, zExtent};
        positions.clear();
        for (int axis = 0; axis < 3; axis++) {
            int across = (axis + 1) % 3;
            for (int edge = 0; edge < 4; edge++) {
                // No underground rim: liquid surfaces may blend over geometry
                // below them instead of hiding it. Uprights end at floor level.
                int yCorner = across == 1 ? edge & 1 : (edge >> 1) & 1;
                if (axis != 1 && yCorner == 0) {
                    continue;
                }
                for (int vertex = 0; vertex < 8; vertex++) {
                    for (int dimension = 0; dimension < 3; dimension++) {
                        float side = (vertex & (1 << dimension)) == 0 ? -1 : 1;
                        if (dimension == axis) {
                            positions.put(dimension == 1 && side < 0 ? 0
                                    : side * (extents[dimension] + HALF_THICKNESS));
                        } else {
                            int corner = dimension == across ? edge & 1 : (edge >> 1) & 1;
                            float center = (corner == 0 ? -1 : 1) * extents[dimension];
                            positions.put(center + side * HALF_THICKNESS);
                        }
                    }
                }
            }
        }
        positions.flip();
        getBuffer(Type.Position).updateData(positions);
        updateBound();
    }
}
