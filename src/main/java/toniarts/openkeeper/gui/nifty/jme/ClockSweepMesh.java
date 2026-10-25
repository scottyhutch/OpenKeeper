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
package toniarts.openkeeper.gui.nifty.jme;

import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;

/**
 * The part of a unit square a clock hand hasn't swept over yet. The hand
 * starts at 12 o'clock and goes clockwise, as on the original's manufacture
 * buttons. The edge follows the square, not a circle, so at 0 the whole square
 * is covered. Coordinates are 0 to 1 with y up, as in the GUI node; scale the
 * geometry to the button.
 */
public final class ClockSweepMesh extends Mesh {

    /**
     * The corners of the square as fractions of a full turn from 12 o'clock
     */
    private static final float[] CORNERS = {1 / 8f, 3 / 8f, 5 / 8f, 7 / 8f};

    public ClockSweepMesh() {
        setSwept(0);
    }

    /**
     * Sets how far the hand has gone
     *
     * @param swept fraction of a full turn, 0 to 1
     */
    public void setSwept(float swept) {
        float[] outline = outline(swept);
        int points = outline.length / 2;
        float[] positions = new float[(points + 1) * 3];
        positions[0] = 0.5f;
        positions[1] = 0.5f;
        for (int i = 0; i < points; i++) {
            positions[(i + 1) * 3] = outline[i * 2];
            positions[(i + 1) * 3 + 1] = outline[i * 2 + 1];
        }

        // A fan around the middle
        short[] indexes = new short[Math.max(0, points - 1) * 3];
        for (int i = 0; i < points - 1; i++) {
            indexes[i * 3] = 0;
            indexes[i * 3 + 1] = (short) (i + 1);
            indexes[i * 3 + 2] = (short) (i + 2);
        }

        setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(positions));
        setBuffer(VertexBuffer.Type.Index, 3, BufferUtils.createShortBuffer(indexes));
        updateBound();
        updateCounts();
    }

    /**
     * The edge of the part not swept over yet, clockwise from the hand back to
     * 12 o'clock
     *
     * @param swept fraction of a full turn, 0 to 1
     * @return x, y pairs on the edge of the unit square, empty if the hand has
     * gone all the way around
     */
    public static float[] outline(float swept) {
        float start = Math.max(0, swept);
        if (start >= 1) {
            return new float[0];
        }

        float[] outline = new float[(CORNERS.length + 2) * 2];
        int count = addPoint(outline, 0, start);
        for (float corner : CORNERS) {
            if (corner > start) {
                count = addPoint(outline, count, corner);
            }
        }
        count = addPoint(outline, count, 1);

        float[] result = new float[count * 2];
        System.arraycopy(outline, 0, result, 0, result.length);
        return result;
    }

    private static int addPoint(float[] outline, int count, float fraction) {
        double angle = fraction * 2 * Math.PI;
        double x = Math.sin(angle);
        double y = Math.cos(angle);

        // Out to the edge of the square
        double scale = 0.5 / Math.max(Math.abs(x), Math.abs(y));
        outline[count * 2] = (float) (0.5 + x * scale);
        outline[count * 2 + 1] = (float) (0.5 + y * scale);
        return count + 1;
    }

}
