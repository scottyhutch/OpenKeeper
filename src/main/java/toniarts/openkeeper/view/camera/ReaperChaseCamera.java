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

/**
 * Follows the Horned Reaper when he comes for the Portal Gem at the end of a
 * won level, as the chase camera does in DK II. The camera keeps its angle,
 * glides over to look at the Reaper and zooms in close to him.
 */
public final class ReaperChaseCamera {

    /**
     * How high the camera ends up over the floor
     */
    static final float ZOOM_HEIGHT = 5f;

    /**
     * How much of the way to the Reaper the camera goes in a second
     */
    private static final float FOLLOW_SPEED = 2f;

    private final Camera camera;

    /**
     * Creates the chase camera
     *
     * @param camera the camera to move
     */
    public ReaperChaseCamera(Camera camera) {
        this.camera = camera;
    }

    /**
     * Moves the camera towards the Reaper
     *
     * @param target where the Reaper is
     * @param tpf the time since the last frame
     */
    public void update(Vector3f target, float tpf) {
        float amount = Math.min(1f, FOLLOW_SPEED * tpf);
        Vector2f lookAt = getLookAtPoint(camera).interpolateLocal(new Vector2f(target.x, target.z), amount);
        float height = camera.getLocation().y;
        if (height > ZOOM_HEIGHT) {
            height += (ZOOM_HEIGHT - height) * amount;
        }
        setLookAt(lookAt, height);
    }

    /**
     * Where on the floor the camera looks at
     *
     * @param camera the camera
     * @return the point on the floor
     */
    static Vector2f getLookAtPoint(Camera camera) {
        Vector3f direction = camera.getDirection();
        Vector3f location = camera.getLocation();
        Vector3f result = direction.mult(-location.y / direction.y).addLocal(location);
        return new Vector2f(result.x, result.z);
    }

    /**
     * Places the camera at the height so that it looks at the point, with its
     * angle unchanged
     */
    private void setLookAt(Vector2f lookAt, float height) {
        Vector3f direction = camera.getDirection();
        Vector3f location = new Vector3f(lookAt.x, 0, lookAt.y).subtractLocal(direction.mult(height / -direction.y));
        camera.setLocation(location);
    }

}
