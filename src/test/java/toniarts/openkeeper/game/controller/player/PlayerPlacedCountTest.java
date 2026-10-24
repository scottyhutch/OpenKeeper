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
package toniarts.openkeeper.game.controller.player;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.TrapComponent;
import toniarts.openkeeper.game.component.TrapViewState;
import toniarts.openkeeper.game.data.Keeper;

/**
 * The counts of the doors and traps a player has, by type, and as blueprints or
 * built ones
 */
class PlayerPlacedCountTest {

    private static final short PLAYER_ID = 3;
    private static final short OTHER_PLAYER_ID = 4;

    private final EntityData entityData = new DefaultEntityData();
    private final PlayerDoorControl doors = new PlayerDoorControl(new Keeper(false, PLAYER_ID), List.of(), entityData);
    private final PlayerTrapControl traps = new PlayerTrapControl(new Keeper(false, PLAYER_ID), List.of(), entityData);

    @Test
    void noDoorsMeansZero() {
        assertEquals(0, doors.getPlacedCount((short) 0, true, true));
    }

    @Test
    void doorsAreCountedByTypeAndOwner() {
        addDoor(PLAYER_ID, 1, false);
        addDoor(PLAYER_ID, 1, false);
        addDoor(PLAYER_ID, 2, false);
        addDoor(OTHER_PLAYER_ID, 1, false);

        assertEquals(3, doors.getPlacedCount((short) 0, true, true));
        assertEquals(2, doors.getPlacedCount((short) 1, true, true));
        assertEquals(1, doors.getPlacedCount((short) 2, true, true));
    }

    @Test
    void doorBlueprintsAreToldFromBuiltDoors() {
        addDoor(PLAYER_ID, 1, true);
        addDoor(PLAYER_ID, 1, false);
        addDoor(PLAYER_ID, 2, false);

        assertEquals(1, doors.getPlacedCount((short) 0, true, false));
        assertEquals(2, doors.getPlacedCount((short) 0, false, true));
        assertEquals(3, doors.getPlacedCount((short) 0, true, true));
    }

    @Test
    void trapsAreCountedByTypeOwnerAndState() {
        addTrap(PLAYER_ID, 3, true);
        addTrap(PLAYER_ID, 3, false);
        addTrap(PLAYER_ID, 5, false);
        addTrap(OTHER_PLAYER_ID, 3, false);

        assertEquals(3, traps.getPlacedCount((short) 0, true, true));
        assertEquals(2, traps.getPlacedCount((short) 3, true, true));
        assertEquals(1, traps.getPlacedCount((short) 0, true, false));
        assertEquals(2, traps.getPlacedCount((short) 0, false, true));
    }

    @Test
    void aTrapDoorIsNotATrap() {
        EntityId doorEntity = addDoor(PLAYER_ID, 4, false);
        entityData.setComponent(doorEntity, new TrapComponent((short) 3));

        assertEquals(0, traps.getPlacedCount((short) 0, true, true));
        assertEquals(1, doors.getPlacedCount((short) 0, true, true));
    }

    private EntityId addDoor(short ownerId, int doorId, boolean blueprint) {
        EntityId entity = entityData.createEntity();
        entityData.setComponent(entity, new DoorComponent((short) doorId, false, blueprint));
        entityData.setComponent(entity, new Owner(ownerId, ownerId));
        return entity;
    }

    private EntityId addTrap(short ownerId, int trapId, boolean blueprint) {
        EntityId entity = entityData.createEntity();
        entityData.setComponent(entity, new TrapComponent((short) trapId));
        entityData.setComponent(entity, new Owner(ownerId, ownerId));
        entityData.setComponent(entity, new TrapViewState((short) trapId, blueprint));
        return entity;
    }
}
