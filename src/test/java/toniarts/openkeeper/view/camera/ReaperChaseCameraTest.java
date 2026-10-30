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

    private static final float REAPER_HEIGHT = 1f;
    private static final float FLOOR = 1f;

    /**
     * Like the one of the game: the lens of the levels, and the angle of level 3
     */
    private static Camera camera() {
        Camera camera = new Camera(800, 600);
        camera.setFrustumPerspective(28.6f, 800f / 600f, 0.1f, 100f);
        camera.setLocation(new Vector3f(10, 10, 0));
        camera.lookAt(new Vector3f(10, 0, 8), Vector3f.UNIT_Y);
        return camera;
    }

    private static void chase(Camera camera, Vector3f feet, int frames) {
        ReaperChaseCamera chase = new ReaperChaseCamera(camera, REAPER_HEIGHT);
        for (int i = 0; i < frames; i++) {
            chase.update(feet, 1 / 60f);
        }
    }

    /**
     * Fails if the point is not in the part of the picture that the wide screen
     * bars leave
     */
    private static void assertShown(Camera camera, Vector3f point, String what) {
        Vector3f screen = camera.getScreenCoordinates(point);
        float margin = (1f - ReaperChaseCamera.VISIBLE_PART) / 2f * camera.getHeight();
        assertTrue(screen.y > margin && screen.y < camera.getHeight() - margin,
                what + " is out of the picture, at " + screen);
        assertTrue(screen.x > 0 && screen.x < camera.getWidth(), what + " is out of the picture, at " + screen);
    }

    @Test
    void theWholeReaperIsShownFromHisFeetToHisHorns() {
        Camera camera = camera();
        Vector3f feet = new Vector3f(30, FLOOR, 40);

        chase(camera, feet, 600);

        assertShown(camera, feet, "The feet");
        assertShown(camera, feet.add(0, REAPER_HEIGHT, 0), "The horns");
    }

    @Test
    void theCameraLooksAtTheChestNotTheFloor() {
        Camera camera = camera();
        Vector3f direction = camera.getDirection().clone();
        Vector3f feet = new Vector3f(30, FLOOR, 40);

        chase(camera, feet, 600);

        Vector2f lookAt = ReaperChaseCamera.getLookAtPoint(camera, FLOOR + REAPER_HEIGHT / 2);
        assertEquals(30f, lookAt.x, 0.01f);
        assertEquals(40f, lookAt.y, 0.01f);
        assertTrue(direction.distance(camera.getDirection()) < 1e-4f, "The camera keeps its angle");
    }

    @Test
    void aTallerReaperIsShownFromFurtherAway() {
        Camera small = camera();
        Camera tall = camera();
        Vector3f feet = new Vector3f(30, FLOOR, 40);

        chase(small, feet, 600);
        new ReaperChaseCamera(tall, 2 * REAPER_HEIGHT).update(feet, 100f);

        assertTrue(tall.getLocation().y > small.getLocation().y);
    }

    @Test
    void theCameraMovesSmoothly() {
        Camera camera = camera();
        Vector3f feet = new Vector3f(30, FLOOR, 40);
        float startHeight = camera.getLocation().y;

        chase(camera, feet, 1);

        Vector2f lookAt = ReaperChaseCamera.getLookAtPoint(camera, FLOOR + REAPER_HEIGHT / 2);
        assertTrue(lookAt.x > 10f && lookAt.x < 30f, "Moved part of the way: " + lookAt);
        assertTrue(camera.getLocation().y != startHeight && camera.getLocation().y > FLOOR);
    }

    @Test
    void aCameraThatIsTooCloseIsPulledBack() {
        Camera camera = new Camera(800, 600);
        camera.setFrustumPerspective(28.6f, 800f / 600f, 0.1f, 100f);
        camera.setLocation(new Vector3f(10, 2, 0));
        camera.lookAt(new Vector3f(10, 0, 2), Vector3f.UNIT_Y);
        Vector3f feet = new Vector3f(10, FLOOR, 1.5f);

        chase(camera, feet, 600);

        assertShown(camera, feet, "The feet");
        assertShown(camera, feet.add(0, REAPER_HEIGHT, 0), "The horns");
    }

}
