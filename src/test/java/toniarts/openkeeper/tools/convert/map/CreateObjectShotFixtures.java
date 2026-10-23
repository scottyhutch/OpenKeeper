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
package toniarts.openkeeper.tools.convert.map;

/**
 * Builds the shots and objects for the object creating shot tests. Lives in
 * the data classes' own package because their setters are {@code protected}
 * (parser-only).
 */
public final class CreateObjectShotFixtures {

    private CreateObjectShotFixtures() {
    }

    public static Shot createObjectShot(short shotId) {
        Shot shot = new Shot();
        shot.setShotId(shotId);
        shot.setProcessType(Shot.ProcessType.CREATE_OBJECT);
        return shot;
    }

    public static GameObject object(short objectId, GameObject.ObjectFlag... flags) {
        GameObject object = GameObjectFixtures.gameObject("Object " + objectId, flags);
        object.setObjectId(objectId);
        return object;
    }

}
