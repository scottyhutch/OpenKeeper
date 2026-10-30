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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ClockSweepMeshTest {

    private static final float DELTA = 0.0001f;

    @Test
    void nothingSweptCoversTheWholeButton() {

        // From 12 o'clock round every corner back to 12 o'clock
        assertArrayEquals(new float[]{
            0.5f, 1, 1, 1, 1, 0, 0, 0, 0, 1, 0.5f, 1
        }, ClockSweepMesh.outline(0), DELTA);
    }

    @Test
    void theHandGoesClockwiseFromTwelve() {

        // A quarter done: the top right is swept, from 3 o'clock on is left
        assertArrayEquals(new float[]{
            1, 0.5f, 1, 0, 0, 0, 0, 1, 0.5f, 1
        }, ClockSweepMesh.outline(0.25f), DELTA);
    }

    @Test
    void theHandFollowsTheEdgeOfTheButton() {

        // An eighth past 12 is the top right corner, and so on
        assertArrayEquals(new float[]{
            1, 1, 1, 0, 0, 0, 0, 1, 0.5f, 1
        }, ClockSweepMesh.outline(0.125f), DELTA);
        assertArrayEquals(new float[]{
            0, 0.5f, 0, 1, 0.5f, 1
        }, ClockSweepMesh.outline(0.75f), DELTA);
    }

    @Test
    void allSweptLeavesNothing() {
        assertEquals(0, ClockSweepMesh.outline(1).length);
    }

    @Test
    void theMeshIsAFanAroundTheMiddle() {
        ClockSweepMesh mesh = new ClockSweepMesh();

        // Five outline points make four triangles
        mesh.setSwept(0.25f);

        assertEquals(6, mesh.getVertexCount());
        assertEquals(4, mesh.getTriangleCount());
    }

}
