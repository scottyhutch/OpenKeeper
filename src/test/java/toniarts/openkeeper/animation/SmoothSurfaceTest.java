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

import com.jme3.math.Vector3f;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The PN triangle of Shaders/DkLighting/Smooth.glsllib, written out again in
 * Java, over one corner whose (welded) normal points behind the triangle, as
 * at the sharp creases of the imp's sack. The surface there faces the
 * triangle's way, against that normal; shading it with the corner normal as
 * it is made black patches (lighting, SSAO and shadows all saw a surface
 * facing away from its normal).
 */
class SmoothSurfaceTest {

    private static final Vector3f[] P = {
        new Vector3f(0, 0, 0), new Vector3f(1, 0, 0), new Vector3f(0, 1, 0)
    };
    /**
     * The triangle faces +z; corner 0's normal leans behind it
     */
    private static final Vector3f[] N = {
        new Vector3f(-0.5f, -0.5f, -0.3f).normalizeLocal(), Vector3f.UNIT_Z, Vector3f.UNIT_Z
    };

    @Test
    void theSurfaceDoesNotDependOnWhichWayACornerNormalPoints() {
        Vector3f[] turned = {N[0].negate(), N[1], N[2]};
        for (float[] coord : new float[][]{{0.9f, 0.05f, 0.05f}, {0.4f, 0.3f, 0.3f}, {0.5f, 0.5f, 0f}}) {
            Vector3f a = position(N, coord);
            Vector3f b = position(turned, coord);
            assertEquals(0f, a.distance(b), 1e-6f);
            assertTrue(Vector3f.isValidVector(a));
        }
    }

    @Test
    void theNormalTurnedToTheTriangleFacesTheSurface() {
        float[] nearCorner = {0.9f, 0.05f, 0.05f};
        Vector3f surface = surfaceNormal(N, nearCorner);
        Vector3f face = P[1].subtract(P[0]).crossLocal(P[2].subtract(P[0]));
        assertTrue(surface.dot(face) > 0, "the patch isn't folded over");

        assertTrue(normal(N, nearCorner).dot(surface) < 0, "the corner normal as it is faces away");
        Vector3f[] facing = new Vector3f[3];
        for (int i = 0; i < 3; i++) {
            facing[i] = N[i].dot(face) < 0 ? N[i].negate() : N[i];
        }
        assertTrue(normal(facing, nearCorner).dot(surface) > 0.5f);
    }

    @Test
    void theShaderTurnsTheCornerNormals() throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("Shaders/DkLighting/Smooth.glsllib")) {
            assertNotNull(in);
            String source = new String(in.readAllBytes(), StandardCharsets.US_ASCII);
            String surface = source.substring(source.indexOf("vec3 smoothSurface("));
            assertTrue(surface.contains("smoothFacing(n[0], face)"));
            assertTrue(surface.contains("normal = smoothNormal(p, facing, coord)"));
        }
    }

    /**
     * The geometric normal of the patch, from its derivatives
     */
    private static Vector3f surfaceNormal(Vector3f[] n, float[] c) {
        float h = 1e-3f;
        Vector3f du = position(n, new float[]{c[0] - h, c[1] + h, c[2]}).subtractLocal(position(n, c));
        Vector3f dv = position(n, new float[]{c[0] - h, c[1], c[2] + h}).subtractLocal(position(n, c));
        return du.crossLocal(dv).normalizeLocal();
    }

    // smoothEdgePoint, smoothEdgeNormal, smoothPosition and smoothNormal of
    // Smooth.glsllib (PN triangles); corner weights w, u, v = c[0], c[1], c[2]
    private static Vector3f edgePoint(Vector3f near, Vector3f far, Vector3f nearNormal) {
        float w = far.subtract(near).dot(nearNormal);
        return near.mult(2).addLocal(far).subtractLocal(nearNormal.mult(w)).divideLocal(3);
    }

    private static Vector3f edgeNormal(Vector3f p1, Vector3f p2, Vector3f n1, Vector3f n2) {
        Vector3f edge = p2.subtract(p1);
        float v = 2 * edge.dot(n1.add(n2)) / edge.dot(edge);
        return n1.add(n2).subtractLocal(edge.mult(v)).normalizeLocal();
    }

    private static Vector3f position(Vector3f[] n, float[] c) {
        float w = c[0];
        float u = c[1];
        float v = c[2];
        Vector3f b210 = edgePoint(P[0], P[1], n[0]);
        Vector3f b120 = edgePoint(P[1], P[0], n[1]);
        Vector3f b021 = edgePoint(P[1], P[2], n[1]);
        Vector3f b012 = edgePoint(P[2], P[1], n[2]);
        Vector3f b102 = edgePoint(P[2], P[0], n[2]);
        Vector3f b201 = edgePoint(P[0], P[2], n[0]);
        Vector3f e = b210.add(b120).addLocal(b021).addLocal(b012).addLocal(b102).addLocal(b201).divideLocal(6);
        Vector3f centre = P[0].add(P[1]).addLocal(P[2]).divideLocal(3);
        Vector3f b111 = e.add(e.subtract(centre).multLocal(0.5f));
        return P[0].mult(w * w * w).addLocal(P[1].mult(u * u * u)).addLocal(P[2].mult(v * v * v))
                .addLocal(b210.mult(3 * w * w * u)).addLocal(b120.mult(3 * w * u * u))
                .addLocal(b201.mult(3 * w * w * v)).addLocal(b021.mult(3 * u * u * v))
                .addLocal(b102.mult(3 * w * v * v)).addLocal(b012.mult(3 * u * v * v))
                .addLocal(b111.mult(6 * w * u * v));
    }

    private static Vector3f normal(Vector3f[] n, float[] c) {
        float w = c[0];
        float u = c[1];
        float v = c[2];
        return n[0].mult(w * w).addLocal(n[1].mult(u * u)).addLocal(n[2].mult(v * v))
                .addLocal(edgeNormal(P[0], P[1], n[0], n[1]).multLocal(w * u))
                .addLocal(edgeNormal(P[1], P[2], n[1], n[2]).multLocal(u * v))
                .addLocal(edgeNormal(P[2], P[0], n[2], n[0]).multLocal(w * v))
                .normalizeLocal();
    }
}
