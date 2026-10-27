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
import java.util.Random;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureMood;
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
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.TortureVictimFixtures;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What happens to the victims in a torture chamber, like in DK II
 */
@SuppressWarnings("unchecked")
class CreatureTorturingSystemTest {

    private static final short KEEPER_ID = 3;
    private static final short HERO_ID = 1;
    private static final Point CHAMBER_TILE = new Point(4, 4);
    private static final short TROLL = 10;
    private static final short MISTRESS = 38;
    private static final short IMP = 1;

    private final EntityData entityData = new DefaultEntityData();
    private final List<EntityId> leftDevice = new ArrayList<>();
    private final Map<EntityId, Short> converted = new HashMap<>();
    private final Map<EntityId, CreatureState> states = new HashMap<>();
    private final RoomInstance chamberInstance = new RoomInstance(new Room());
    private final RoomInstance heroGate = heroRoom(new Point(20, 20));
    private final RoomInstance heroLair = heroRoom(new Point(30, 30));
    private final List<List<Point>> revealed = new ArrayList<>();
    private final Map<RoomInstance, EntityId> roomEntities = new HashMap<>();
    private final Random firstChoice = new Random() {

        @Override
        public int nextInt(int bound) {
            return 0;
        }
    };
    private final Random lastChoice = new Random() {

        @Override
        public int nextInt(int bound) {
            return bound - 1;
        }
    };
    private final Map<Short, Creature> creatures = Map.of(
            TROLL, TortureVictimFixtures.victim(TROLL, (short) 1092, Creature.JobClass.ARTISAN, false),
            MISTRESS, TortureVictimFixtures.victim(MISTRESS, (short) -1000, Creature.JobClass.FIGHTER, false),
            IMP, TortureVictimFixtures.victim(IMP, (short) 1092, Creature.JobClass.ARTISAN, true));
    private CreatureTorturingSystem system = new CreatureTorturingSystem(entityData, creaturesController(), mapController(), lastChoice);

    @Test
    void enemyTorturedToDeathRevealsOneOfItsKeepersRooms() {
        torturedToDeath(HERO_ID);

        assertEquals(List.of(heroGate.getCoordinates()), revealed);
    }

    @Test
    void eachDeathRevealsAnotherRoom() {
        torturedToDeath(HERO_ID);
        torturedToDeath(HERO_ID);
        torturedToDeath(HERO_ID);

        assertEquals(List.of(heroGate.getCoordinates(), heroLair.getCoordinates()), revealed);
    }

    @Test
    void victimTellsWhatTheKeeperDoesNotKnowYet() {

        // An artisan has two things to tell about its keeper, every victim one more thing
        system = new CreatureTorturingSystem(entityData, creaturesController(), mapController(), firstChoice);
        torturedToDeath(HERO_ID);
        torturedToDeath(HERO_ID);
        torturedToDeath(HERO_ID);
        assertTrue(revealed.isEmpty());

        torturedToDeath(HERO_ID);
        assertEquals(List.of(heroGate.getCoordinates()), revealed);
    }

    @Test
    void roomsRevealedAreKeptInTheEntityData() {
        torturedToDeath(HERO_ID);

        // A loaded game starts with a new system on the same entities
        system = new CreatureTorturingSystem(entityData, creaturesController(), mapController(), lastChoice);
        torturedToDeath(HERO_ID);

        assertEquals(List.of(heroGate.getCoordinates(), heroLair.getCoordinates()), revealed);
    }

    @Test
    void ownVictimRevealsNothing() {
        torturedToDeath(KEEPER_ID);

        assertTrue(revealed.isEmpty());
    }

    @Test
    void ownVictimGetsAngrierEverySecond() {
        EntityId victim = victim(KEEPER_ID, 1000, TROLL);

        tick(3);

        assertEquals(3 * 1092, entityData.getComponent(victim, CreatureMood.class).get(CreatureMood.REASON_OTHER));
    }

    @Test
    void ownMistressEnjoysIt() {
        EntityId victim = victim(KEEPER_ID, 1000, MISTRESS);
        entityData.setComponent(victim, new CreatureMood(0, 0, 0, 0, 0, 5000));

        tick(2);

        assertEquals(3000, entityData.getComponent(victim, CreatureMood.class).get(CreatureMood.REASON_OTHER));
    }

    @Test
    void emotionlessVictimFeelsNothing() {
        EntityId victim = victim(KEEPER_ID, 1000, IMP);

        tick(3);

        assertEquals(0, entityData.getComponent(victim, CreatureMood.class).get(CreatureMood.REASON_OTHER));
    }

    @Test
    void enemyVictimsMoodIsNotChanged() {
        EntityId victim = victim(HERO_ID, 1000, TROLL);

        tick(3);

        assertEquals(0, entityData.getComponent(victim, CreatureMood.class).get(CreatureMood.REASON_OTHER));
    }

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

    private void torturedToDeath(short ownerId) {
        EntityId victim = victim(ownerId, 1000);
        tick(1);
        entityData.removeComponent(victim, CreatureTortured.class);
        entityData.setComponent(victim, new Death(1));
        tick(1);
    }

    private static RoomInstance heroRoom(Point p) {
        RoomInstance room = new RoomInstance(new Room());
        room.setOwnerId(HERO_ID);
        room.addCoordinate(p);
        room.addCoordinate(new Point(p.x + 1, p.y));
        return room;
    }

    private IRoomController roomController(RoomInstance roomInstance) {
        return (IRoomController) Proxy.newProxyInstance(IRoomController.class.getClassLoader(),
                new Class<?>[]{IRoomController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getRoomInstance" -> roomInstance;
                    case "getEntityId" -> roomEntities.computeIfAbsent(roomInstance, room -> entityData.createEntity());
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private void tick(int seconds) {
        for (int i = 0; i < seconds * 4; i++) {
            system.processTick(0.25f);
        }
    }

    private EntityId victim(short ownerId, float tortureTimeToConvert) {
        return victim(ownerId, tortureTimeToConvert, TROLL);
    }

    private EntityId victim(short ownerId, float tortureTimeToConvert, short creatureId) {
        EntityId victim = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.tortureTimeToConvert = tortureTimeToConvert;
        creatureComponent.creatureId = creatureId;
        entityData.setComponent(victim, new CreatureMood());
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
                    case "getCreature" -> creatures.get(entityData.getComponent(entityId, CreatureComponent.class).creatureId);
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
                    case "getRoomControllers" -> List.of(chamber, roomController(heroGate), roomController(heroLair));
                    case "revealTiles" -> {
                        assertEquals(KEEPER_ID, (short) (Short) args[1]);
                        revealed.add((List<Point>) args[0]);
                        yield null;
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

}
