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

import com.simsilica.es.EntityData;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.PrisonController;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.RoomFixtures;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * What happens to a creature the keeper drops into a prison or a torture
 * chamber. Like in DK II, the torture chamber takes any creature, also the
 * keeper's own, except the keeper's Imps
 */
class CaptiveDropTest {

    private static final short KEEPER_ID = 3;
    private static final short HERO_ID = 1;
    private static final Point CAGE = new Point(2, 2);

    @Test
    void enemyIsTortured() {
        assertEquals(CreatureState.TORTURED, GameWorldController.getCaptiveState(room(ObjectType.TORTUREE), CAGE, HERO_ID, false));
    }

    @Test
    void ownCreatureIsTortured() {
        assertEquals(CreatureState.TORTURED, GameWorldController.getCaptiveState(room(ObjectType.TORTUREE), CAGE, KEEPER_ID, false));
    }

    @Test
    void ownImpIsNotTortured() {
        assertNull(GameWorldController.getCaptiveState(room(ObjectType.TORTUREE), CAGE, KEEPER_ID, true));
    }

    @Test
    void enemyImpIsTortured() {
        assertEquals(CreatureState.TORTURED, GameWorldController.getCaptiveState(room(ObjectType.TORTUREE), CAGE, HERO_ID, true));
    }

    @Test
    void enemyIsImprisoned() {
        assertEquals(CreatureState.IMPRISONED, GameWorldController.getCaptiveState(prison(), CAGE, HERO_ID, false));
    }

    @Test
    void enemyAroundTheCageIsNotImprisoned() {
        assertNull(GameWorldController.getCaptiveState(prison(), new Point(1, 2), HERO_ID, false));
        assertNull(GameWorldController.getCaptiveState(prison(), new Point(3, 3), HERO_ID, false));
    }

    @Test
    void ownCreatureIsNotImprisoned() {
        assertNull(GameWorldController.getCaptiveState(prison(), CAGE, KEEPER_ID, false));
    }

    @Test
    void ownCreatureIsNotImprisonedInACapturedPrison() {
        PrisonController prison = prison();
        prison.getRoomInstance().setOwnerId(HERO_ID);

        prison.captured(KEEPER_ID);

        assertNull(GameWorldController.getCaptiveState(prison, CAGE, KEEPER_ID, false));
        assertEquals(CreatureState.IMPRISONED, GameWorldController.getCaptiveState(prison, CAGE, HERO_ID, false));
    }

    @Test
    void otherRoomsHoldNoCaptives() {
        assertNull(GameWorldController.getCaptiveState(room(ObjectType.LAIR), CAGE, HERO_ID, false));
        assertNull(GameWorldController.getCaptiveState(null, CAGE, HERO_ID, false));
    }

    /**
     * A 3 by 3 Prison of the keeper, the cage is its middle tile
     */
    private static PrisonController prison() {
        EntityData entityData = new DefaultEntityData();
        IObjectsController objectsController = (IObjectsController) Proxy.newProxyInstance(IObjectsController.class.getClassLoader(),
                new Class<?>[]{IObjectsController.class}, (proxy, method, args) -> method.getName().equals("loadObject") ? entityData.createEntity() : null);

        // Prison objects in DK II: Skull, Bone and Lavatory
        RoomInstance roomInstance = new RoomInstance(RoomFixtures.roomWithObjects((short) 11, (short) 49, (short) 50, (short) 51, (short) 0, (short) 0, (short) 0));
        roomInstance.setOwnerId(KEEPER_ID);
        for (int x = 1; x <= 3; x++) {
            for (int y = 1; y <= 3; y++) {
                roomInstance.addCoordinate(new Point(x, y));
            }
        }
        PrisonController prison = new PrisonController(entityData.createEntity(), entityData, null, roomInstance, objectsController, null);
        prison.construct();
        return prison;
    }

    private static IRoomController room(ObjectType objectType) {
        RoomInstance roomInstance = new RoomInstance(new Room());
        roomInstance.setOwnerId(KEEPER_ID);
        return (IRoomController) Proxy.newProxyInstance(IRoomController.class.getClassLoader(),
                new Class<?>[]{IRoomController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getRoomInstance" -> roomInstance;
                    case "hasObjectControl" -> args[0] == objectType;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

}
