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
package toniarts.openkeeper.view.camera;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReaperChaseCameraTest {

    private static Camera camera() {
        Camera camera = new Camera(800, 600);
        camera.setLocation(new Vector3f(10, 12, 0));
        camera.lookAt(new Vector3f(10, 0, 8), Vector3f.UNIT_Y);
        return camera;
    }

    @Test
    void theCameraEndsUpLookingAtTheReaperFromClose() {
        Camera camera = camera();
        Vector3f direction = camera.getDirection().clone();
        ReaperChaseCamera chase = new ReaperChaseCamera(camera);

        for (int i = 0; i < 600; i++) {
            chase.update(new Vector3f(30, 0, 40), 1 / 60f);
        }

        Vector2f lookAt = ReaperChaseCamera.getLookAtPoint(camera);
        assertEquals(30f, lookAt.x, 0.01f);
        assertEquals(40f, lookAt.y, 0.01f);
        assertEquals(ReaperChaseCamera.ZOOM_HEIGHT, camera.getLocation().y, 0.01f);
        assertTrue(direction.distance(camera.getDirection()) < 1e-4f, "The camera keeps its angle");
    }

    @Test
    void theCameraMovesSmoothly() {
        Camera camera = camera();
        ReaperChaseCamera chase = new ReaperChaseCamera(camera);

        chase.update(new Vector3f(30, 0, 40), 1 / 60f);

        Vector2f lookAt = ReaperChaseCamera.getLookAtPoint(camera);
        assertTrue(lookAt.x > 10f && lookAt.x < 30f, "Moved part of the way: " + lookAt);
        assertTrue(camera.getLocation().y < 12f && camera.getLocation().y > ReaperChaseCamera.ZOOM_HEIGHT);
    }

    @Test
    void aCloserCameraIsNotZoomedOut() {
        Camera camera = new Camera(800, 600);
        camera.setLocation(new Vector3f(10, 2, 0));
        camera.lookAt(new Vector3f(10, 0, 2), Vector3f.UNIT_Y);
        ReaperChaseCamera chase = new ReaperChaseCamera(camera);

        chase.update(new Vector3f(10, 0, 2), 1f);

        assertEquals(2f, camera.getLocation().y, 0.01f);
    }

}
