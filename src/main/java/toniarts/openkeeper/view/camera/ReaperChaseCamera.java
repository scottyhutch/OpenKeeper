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

import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;

/**
 * Follows the Horned Reaper when he comes for the Portal Gem at the end of a
 * won level, as the chase camera does in DK II. The camera keeps its angle,
 * glides over to look at the Reaper's chest and keeps far enough to show all of
 * him, from his feet to his horns.
 */
public final class ReaperChaseCamera {

    /**
     * How many times the Reaper's height the picture shows, so that there is
     * room around him
     */
    static final float FRAMING = 2f;

    /**
     * The part of the picture the wide screen bars leave visible
     */
    static final float VISIBLE_PART = 0.75f;

    /**
     * Where the camera looks at, as a part of the Reaper's height from his
     * feet. The chest, not the floor under his feet
     */
    static final float AIM_HEIGHT = 0.5f;

    /**
     * How much of the way to the Reaper the camera goes in a second
     */
    private static final float FOLLOW_SPEED = 2f;

    private final Camera camera;
    private final float reaperHeight;

    /**
     * Creates the chase camera
     *
     * @param camera the camera to move
     * @param reaperHeight the height of the Reaper, in tiles
     */
    public ReaperChaseCamera(Camera camera, float reaperHeight) {
        this.camera = camera;
        this.reaperHeight = reaperHeight;
    }

    /**
     * Moves the camera towards the Reaper
     *
     * @param target where the Reaper's feet are
     * @param tpf the time since the last frame
     */
    public void update(Vector3f target, float tpf) {
        float amount = Math.min(1f, FOLLOW_SPEED * tpf);
        float aimY = target.y + reaperHeight * AIM_HEIGHT;
        Vector2f lookAt = getLookAtPoint(camera, aimY).interpolateLocal(new Vector2f(target.x, target.z), amount);
        float height = camera.getLocation().y - aimY;
        height += (getFramingHeight() - height) * amount;
        setLookAt(lookAt, aimY, height);
    }

    /**
     * How high the camera needs to be over the point it looks at to show the
     * whole Reaper, with its angle unchanged
     */
    float getFramingHeight() {
        float halfView = FastMath.tan(camera.getFov() * FastMath.DEG_TO_RAD / 2f) * VISIBLE_PART;
        float distance = reaperHeight * FRAMING / (2f * halfView);
        return distance * -camera.getDirection().y;
    }

    /**
     * Where the camera looks at, on a level
     *
     * @param camera the camera
     * @param level the height of the level
     * @return the point on the level
     */
    static Vector2f getLookAtPoint(Camera camera, float level) {
        Vector3f direction = camera.getDirection();
        Vector3f location = camera.getLocation();
        Vector3f result = direction.mult((level - location.y) / direction.y).addLocal(location);
        return new Vector2f(result.x, result.z);
    }

    /**
     * Places the camera at the height over the point it looks at, with its
     * angle unchanged
     */
    private void setLookAt(Vector2f lookAt, float level, float height) {
        Vector3f direction = camera.getDirection();
        Vector3f location = new Vector3f(lookAt.x, level, lookAt.y).subtractLocal(direction.mult(height / -direction.y));
        camera.setLocation(location);
    }

}
