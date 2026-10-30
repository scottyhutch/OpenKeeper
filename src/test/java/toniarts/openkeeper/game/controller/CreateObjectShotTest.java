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
package toniarts.openkeeper.game.controller;

import com.jme3.math.Vector3f;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.map.CreateObjectShotFixtures;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Shots that create an object, like the Create Gold and Call to Arms spells
 */
class CreateObjectShotTest {

    private static final short SHOT_ID = 5;
    private static final short GOLD_ID = 1;
    private static final short BANNER_ID = 37;
    private static final short PLAYER_ID = 3;

    private final List<Object[]> loadedObjects = new ArrayList<>();

    @Test
    void createGoldMakesAPileOfTheShotsAmount() {

        // Create Gold: object 1 (Gold), 1000 gold, 2000 when upgraded
        shotsController(CreateObjectShotFixtures.object(GOLD_ID, GameObject.ObjectFlag.OBJECT_TYPE_GOLD))
                .createShot(SHOT_ID, GOLD_ID, 1000, PLAYER_ID, new Vector3f(4.2f, 0, 7.4f), null, null);

        assertEquals(1, loadedObjects.size());
        Object[] args = loadedObjects.get(0);
        assertEquals(GOLD_ID, args[0]);
        assertEquals(PLAYER_ID, args[1]);
        assertEquals(4, args[2]);
        assertEquals(7, args[3]);
        assertEquals(1000, args[4]);
    }

    @Test
    void otherObjectsAreCreatedWhereTheShotLands() {
        Vector3f position = new Vector3f(4.2f, 0, 7.6f);

        shotsController(CreateObjectShotFixtures.object(BANNER_ID))
                .createShot(SHOT_ID, BANNER_ID, 0, PLAYER_ID, position, null, null);

        assertEquals(1, loadedObjects.size());
        Object[] args = loadedObjects.get(0);
        assertEquals(BANNER_ID, args[0]);
        assertEquals(position, args[2]);
    }

    private ShotsController shotsController(GameObject object) {
        IKwdFile kwdFile = stub(IKwdFile.class, (name, args) -> switch (name) {
            case "getShotById" -> CreateObjectShotFixtures.createObjectShot(SHOT_ID);
            case "getObject" -> object;
            default -> throw new UnsupportedOperationException(name);
        });
        IObjectsController objectsController = stub(IObjectsController.class, (name, args) -> {
            if (name.equals("loadObject")) {
                loadedObjects.add(args);
                return null;
            }
            throw new UnsupportedOperationException(name);
        });
        return new ShotsController(kwdFile, null, null, null, null, null, null, objectsController, null, null);
    }

    private interface Answer {

        Object answer(String methodName, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, Answer answer) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> answer.answer(method.getName(), args));
    }

}
