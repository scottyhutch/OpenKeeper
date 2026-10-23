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
package toniarts.openkeeper.game.logic;

import com.badlogic.gdx.ai.fsm.StateMachine;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureTortured;
import toniarts.openkeeper.game.component.Death;
import toniarts.openkeeper.game.component.InHand;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.storage.IRoomObjectControl;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The victims leave their torture devices when they are converted, die, are
 * picked up or their chamber is gone
 */
class CreatureTorturingSystemTest {

    private static final short KEEPER_ID = 3;
    private static final short HERO_ID = 1;
    private static final Point CHAMBER_TILE = new Point(4, 4);

    private final EntityData entityData = new DefaultEntityData();
    private final List<EntityId> leftDevice = new ArrayList<>();
    private final Map<EntityId, Short> converted = new HashMap<>();
    private final Map<EntityId, CreatureState> states = new HashMap<>();
    private final RoomInstance chamberInstance = new RoomInstance(new Room());
    private final CreatureTorturingSystem system = new CreatureTorturingSystem(entityData, creaturesController(), mapController());

    @Test
    void convertedEnemyLeavesTheDevice() {
        EntityId victim = victim(HERO_ID, 2);

        tick(3);

        assertEquals(KEEPER_ID, converted.get(victim));
        assertEquals(List.of(victim), leftDevice);
    }

    @Test
    void deadVictimLeavesTheDevice() {
        EntityId victim = victim(HERO_ID, 100);
        tick(1);

        entityData.removeComponent(victim, CreatureTortured.class);
        entityData.setComponent(victim, new Death(1));
        tick(1);

        assertEquals(List.of(victim), leftDevice);
        assertTrue(states.isEmpty());
    }

    @Test
    void victimMovedToAnotherRoomIsNotSetFree() {
        EntityId victim = victim(HERO_ID, 100);
        tick(1);

        // Picked up and dropped somewhere else between two ticks
        entityData.removeComponent(victim, CreatureTortured.class);
        tick(1);

        assertEquals(List.of(victim), leftDevice);
        assertTrue(states.isEmpty());
    }

    @Test
    void victimOfADestroyedChamberIsSetFree() {
        EntityId victim = victim(HERO_ID, 100);
        tick(1);

        // The chamber sets its victims free when it is sold or destroyed
        chamberInstance.setDestroyed(true);
        entityData.removeComponent(victim, CreatureTortured.class);
        tick(1);

        assertEquals(CreatureState.IDLE, states.get(victim));
        assertEquals(HERO_ID, entityData.getComponent(victim, Owner.class).controlId);
    }

    @Test
    void pickedUpVictimIsNotSetFree() {
        EntityId victim = victim(HERO_ID, 100);
        tick(1);

        entityData.removeComponent(victim, CreatureTortured.class);
        entityData.setComponent(victim, new InHand());
        tick(1);

        assertTrue(states.isEmpty());
    }

    @Test
    void ownCreatureIsNotConverted() {
        EntityId victim = victim(KEEPER_ID, 2);

        tick(3);

        assertTrue(converted.isEmpty());
        assertTrue(leftDevice.isEmpty());
    }

    private void tick(int seconds) {
        for (int i = 0; i < seconds * 4; i++) {
            system.processTick(0.25f);
        }
    }

    private EntityId victim(short ownerId, float tortureTimeToConvert) {
        EntityId victim = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.tortureTimeToConvert = tortureTimeToConvert;
        entityData.setComponent(victim, creatureComponent);
        entityData.setComponent(victim, new Owner(ownerId, KEEPER_ID));
        entityData.setComponent(victim, new Position(0, WorldUtils.pointToVector3f(CHAMBER_TILE)));
        entityData.setComponent(victim, new CreatureTortured(0, -1, 0));
        return victim;
    }

    private ICreaturesController creaturesController() {
        return (ICreaturesController) Proxy.newProxyInstance(ICreaturesController.class.getClassLoader(),
                new Class<?>[]{ICreaturesController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "createController" -> creature((EntityId) args[0]);
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private ICreatureController creature(EntityId entityId) {
        return (ICreatureController) Proxy.newProxyInstance(ICreatureController.class.getClassLoader(),
                new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "convertCreature" -> {
                        converted.put(entityId, (Short) args[0]);
                        yield null;
                    }
                    case "getStateMachine" -> stateMachine(entityId);
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private StateMachine<?, ?> stateMachine(EntityId entityId) {
        return (StateMachine<?, ?>) Proxy.newProxyInstance(StateMachine.class.getClassLoader(),
                new Class<?>[]{StateMachine.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "changeState" -> {
                        states.put(entityId, (CreatureState) args[0]);
                        yield null;
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private IMapController mapController() {
        chamberInstance.setOwnerId(KEEPER_ID);
        IRoomObjectControl<?> torturees = (IRoomObjectControl<?>) Proxy.newProxyInstance(IRoomObjectControl.class.getClassLoader(),
                new Class<?>[]{IRoomObjectControl.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "removeItem" -> {
                        leftDevice.add((EntityId) args[0]);
                        yield null;
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        IRoomController chamber = (IRoomController) Proxy.newProxyInstance(IRoomController.class.getClassLoader(),
                new Class<?>[]{IRoomController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getRoomInstance" -> chamberInstance;
                    case "hasObjectControl" -> args[0] == ObjectType.TORTUREE;
                    case "getObjectControl" -> torturees;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        return (IMapController) Proxy.newProxyInstance(IMapController.class.getClassLoader(),
                new Class<?>[]{IMapController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getRoomControllerByCoordinates" -> CHAMBER_TILE.equals(args[0]) ? chamber : null;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

}
