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
package toniarts.openkeeper.game.controller.room;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import com.simsilica.es.filter.FieldFilter;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.tools.convert.map.RoomFixtures;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static toniarts.openkeeper.tools.convert.map.RoomFixtures.roomWithObjects;

/**
 * The roulette tables of the Casino get their wheels as in the original
 */
class CasinoControllerTest {

    private final EntityData entityData = new DefaultEntityData();

    @Test
    void everyRouletteTableGetsTwoWheelsThatGoWithTheRoom() {
        RoomInstance roomInstance = new RoomInstance(RoomFixtures.roomWithObjects((short) 15,
                (short) 87, (short) 59, (short) 67, (short) 68, (short) 0, (short) 0));
        // The pay out adjuster takes the place of the first piece of furniture with none above it, so the room is
        // big enough to keep a table
        for (int x = 5; x <= 9; x++) {
            for (int y = 5; y <= 9; y++) {
                roomInstance.addCoordinate(new Point(x, y));
            }
        }
        CasinoController casino = new CasinoController(entityData.createEntity(), entityData, null, roomInstance,
                objectsController(), null);

        casino.construct();
        Set<Vector3f> tables = tables();
        assertTrue(!tables.isEmpty());
        Set<Vector3f> expected = new HashSet<>();
        for (Vector3f table : tables) {
            expected.add(new Vector3f(table.x, 1, table.z + 2000f / 4096));
            expected.add(new Vector3f(table.x, 1, table.z - 2000f / 4096));
        }
        assertEquals(expected, wheels());

        // Built again, as when the room grows
        casino.construct();
        assertEquals(2 * tables().size(), wheels().size());

        casino.remove();
        assertTrue(wheels().isEmpty());
    }

    private Set<Vector3f> tables() {
        Set<Vector3f> tables = new HashSet<>();
        for (EntityId table : entityData.findEntities(new FieldFilter<>(ObjectComponent.class, "objectId",
                CasinoController.ROULETTE_TABLE_OBJECT_ID), ObjectComponent.class, Position.class)) {
            tables.add(entityData.getComponent(table, Position.class).position);
        }
        return tables;
    }

    private Set<Vector3f> wheels() {
        Set<Vector3f> wheels = new HashSet<>();
        for (EntityId wheel : entityData.findEntities(new FieldFilter<>(ObjectComponent.class, "objectId",
                CasinoController.ROULETTE_WHEEL_OBJECT_ID), ObjectComponent.class, Position.class)) {
            wheels.add(entityData.getComponent(wheel, Position.class).position);
        }
        return wheels;
    }

    private IObjectsController objectsController() {
        return (IObjectsController) Proxy.newProxyInstance(IObjectsController.class.getClassLoader(), new Class<?>[]{IObjectsController.class}, (proxy, method, args) -> {
            if (!"loadObject".equals(method.getName())) {
                throw new UnsupportedOperationException(method.getName());
            }
            Vector3f position = args[2] instanceof Vector3f vector
                    ? new Vector3f(vector.x, 1, vector.z)
                    : new Vector3f((int) args[2], 1, (int) args[3]);
            EntityId object = entityData.createEntity();
            entityData.setComponents(object, new ObjectComponent((short) args[0], null), new Position(0, position));
            return object;
        });
    }


    @Test
    void roomWithOneTableTakesTwoGamblers() {
        CasinoController room = buildCasino(3);

        assertEquals(1, room.getFurnitureCount());
        assertEquals(2, room.getMaxCapacity(ObjectType.GAMBLER));
    }

    @Test
    void roomWithoutFurnitureTakesNoGamblers() {
        CasinoController room = buildCasino(2);

        assertEquals(0, room.getFurnitureCount());
        assertEquals(0, room.getMaxCapacity(ObjectType.GAMBLER));
    }

    private static CasinoController buildCasino(int size) {
        EntityData entityData = new DefaultEntityData();
        IObjectsController objectsController = (IObjectsController) Proxy.newProxyInstance(
                IObjectsController.class.getClassLoader(),
                new Class<?>[]{IObjectsController.class},
                (proxy, method, args) -> method.getName().equals("loadObject") ? entityData.createEntity() : null);

        // Casino objects in DK II: Roulette Table, Barrel, Bar Table and Alcove Barrels
        RoomInstance roomInstance = new RoomInstance(roomWithObjects((short) 15, (short) 87, (short) 59, (short) 67, (short) 68, (short) 0, (short) 0));
        for (int x = 1; x <= size; x++) {
            for (int y = 1; y <= size; y++) {
                roomInstance.addCoordinate(new Point(x, y));
            }
        }

        CasinoController room = new CasinoController(entityData.createEntity(), entityData, null, roomInstance, objectsController, null);
        room.construct();
        return room;
    }
}
