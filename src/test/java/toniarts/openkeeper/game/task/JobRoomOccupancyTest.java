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
package toniarts.openkeeper.game.task;

import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.CasinoController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.TrainingRoomController;
import toniarts.openkeeper.game.controller.room.storage.IRoomObjectControl;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.creature.Gamble;
import toniarts.openkeeper.game.task.creature.Train;
import toniarts.openkeeper.tools.convert.map.CasinoVariableFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.RoomFixtures;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static toniarts.openkeeper.tools.convert.map.RoomFixtures.roomWithObjects;

/**
 * The capacity used of the Casino, the Training Room and the like is the number
 * of creatures that have a job in them, so the room tooltip counts them
 */
class JobRoomOccupancyTest {

    private static final short KEEPER = 3;

    private final EntityData entityData = new DefaultEntityData();
    private final Map<MiscType, MiscVariable> gameSettings = new EnumMap<>(MiscType.class);

    @Test
    void casinoCountsTheGamblersThatHaveATask() {
        CasinoController casino = casino();
        ICreatureController first = creature();
        ICreatureController second = creature();
        assertEquals(0, casino.getUsedCapacity(ObjectType.GAMBLER));

        Gamble firstTask = gamble(casino);
        firstTask.assign(first, true);
        gamble(casino).assign(second, true);
        assertEquals(2, casino.getUsedCapacity(ObjectType.GAMBLER));
        assertEquals(2, casino.getMaxCapacity(ObjectType.GAMBLER));

        // The creature leaves, once is enough
        firstTask.unassign(first);
        firstTask.unassign(first);
        assertEquals(1, casino.getUsedCapacity(ObjectType.GAMBLER));
    }

    @Test
    void gamblerInAFullCasinoKeepsItsTaskButANewcomerHasNone() {
        CasinoController casino = casino();
        ICreatureController first = creature();
        Gamble firstTask = gamble(casino);
        firstTask.assign(first, true);
        gamble(casino).assign(creature(), true);

        assertTrue(firstTask.isValid(first));
        assertFalse(gamble(casino).isValid(creature()));
    }

    @Test
    void trainingRoomCountsTheTraineesThatHaveATask() {
        TrainingRoomController room = new TrainingRoomController(entityData.createEntity(), entityData, null,
                roomInstance(7, 14), objectsController(), null);
        room.construct();
        gameSettings.put(MiscType.TRAINING_ROOM_MAX_EXPERIENCE_LEVEL, CasinoVariableFixtures.miscVariable(MiscType.TRAINING_ROOM_MAX_EXPERIENCE_LEVEL, 10));
        gameSettings.put(MiscType.MODIFY_PLAYER_GOLD_WHILE_TRAINING_PER_SECOND, CasinoVariableFixtures.miscVariable(MiscType.MODIFY_PLAYER_GOLD_WHILE_TRAINING_PER_SECOND, -1));
        ICreatureController trainee = creature();

        Train train = new Train(null, null, new Point(1, 1), KEEPER, room, taskManager(room), null, gameSettings, null);
        train.assign(trainee, true);
        assertEquals(1, room.getUsedCapacity(ObjectType.TRAINEE));

        train.unassign(trainee);
        assertEquals(0, room.getUsedCapacity(ObjectType.TRAINEE));
    }

    @Test
    void creaturesWithTasksAreNotCountedTwiceWhenHandingOutTasks() {
        List<EntityId> occupants = new ArrayList<>();
        Point point = new Point(1, 1);
        RoomInstance roomInstance = new RoomInstance(RoomFixtures.room((short) 15, 0));
        roomInstance.addCoordinate(point);
        IRoomObjectControl control = proxy(IRoomObjectControl.class, (proxy, method, args) -> switch (method.getName()) {
            case "getAvailableCoordinates" ->
                List.of(point);
            case "getMaxCapacity" ->
                2;
            case "getCurrentCapacity" ->
                occupants.size();
            case "isFullCapacity" ->
                occupants.size() >= 2;
            case "contains" ->
                occupants.contains(args[0]);
            case "addItem" -> {
                occupants.add((EntityId) args[0]);
                yield args[0];
            }
            case "removeItem" -> {
                occupants.remove(args[0]);
                yield null;
            }
            default ->
                null;
        });
        IRoomController room = proxy(IRoomController.class, (proxy, method, args) -> switch (method.getName()) {
            case "getRoomInstance" ->
                roomInstance;
            case "getObjectControl" ->
                control;
            case "isTileAccessible" ->
                true;
            case "isDestroyed", "isRemoved" ->
                false;
            case "hashCode" ->
                System.identityHashCode(proxy);
            case "equals" ->
                proxy == args[0];
            default ->
                null;
        });
        setCasinoSettings();
        TaskManager taskManager = taskManager(room);

        // The one point is shared by the two gamblers the room takes, not by the one only
        assertTrue(taskManager.assignClosestRoomTask(creature(), ObjectType.GAMBLER, null));
        assertTrue(taskManager.assignClosestRoomTask(creature(), ObjectType.GAMBLER, null));
        assertFalse(taskManager.assignClosestRoomTask(creature(), ObjectType.GAMBLER, null));
        assertEquals(2, occupants.size());
    }

    private CasinoController casino() {
        CasinoController casino = new CasinoController(entityData.createEntity(), entityData, null,
                roomInstance(15, 87), objectsController(), null);
        casino.construct();
        return casino;
    }

    private Gamble gamble(CasinoController casino) {
        setCasinoSettings();
        return new Gamble(null, null, new Point(1, 1), KEEPER, casino, taskManager(casino), entityData, null, null, gameSettings,
                () -> CasinoController.PayOut.SMILES, null);
    }

    private void setCasinoSettings() {
        for (MiscType type : new MiscType[]{MiscType.GAMBLING_MODIFY_CREATURE_MONEY_SMILES_PER_SECOND,
            MiscType.GAMBLING_MODIFY_CREATURE_MONEY_MONEY_PER_SECOND, MiscType.GAMBLING_MODIFY_CREATURE_ANGER_SMILES_PER_SECOND,
            MiscType.GAMBLING_MODIFY_CREATURE_ANGER_MONEY_PER_SECOND, MiscType.GAMBLING_CREATURE_JACKPOT_CHANCE_SMILES_PER_SECOND,
            MiscType.GAMBLING_CREATURE_JACKPOT_CHANCE_MONEY_PER_SECOND, MiscType.GAMBLING_JACKPOT_BASIC_AMOUNT,
            MiscType.GAMBLING_JACKPOT_PAY_TIME_LIMIT, MiscType.MAX_GOLD_PILE_OUTSIDE_TREASURY}) {
            gameSettings.put(type, CasinoVariableFixtures.miscVariable(type, 1));
        }
    }

    /**
     * A 3x3 room with the one piece of furniture it gets, so it takes two
     * creatures
     */
    private static RoomInstance roomInstance(int roomId, int furnitureId) {
        RoomInstance roomInstance = new RoomInstance(roomWithObjects((short) roomId, (short) furnitureId, (short) 0, (short) 0, (short) 0, (short) 0, (short) 0));
        roomInstance.setOwnerId(KEEPER);
        for (int x = 1; x <= 3; x++) {
            for (int y = 1; y <= 3; y++) {
                roomInstance.addCoordinate(new Point(x, y));
            }
        }
        return roomInstance;
    }

    private IObjectsController objectsController() {
        return proxy(IObjectsController.class, (proxy, method, args) -> method.getName().equals("loadObject") ? entityData.createEntity() : null);
    }

    private ICreatureController creature() {
        EntityId creatureId = entityData.createEntity();
        return proxy(ICreatureController.class, (proxy, method, args) -> switch (method.getName()) {
            case "getEntityId" ->
                creatureId;
            case "getOwnerId" ->
                KEEPER;
            case "getGold" ->
                100;
            case "hashCode" ->
                System.identityHashCode(proxy);
            case "equals" ->
                proxy == args[0];
            default ->
                null;
        });
    }

    private TaskManager taskManager(IRoomController room) {
        IMapDataInformation<?> mapData = proxy(IMapDataInformation.class, (proxy, method, args) -> switch (method.getName()) {
            case "getWidth", "getHeight" ->
                0;
            default ->
                null;
        });
        IMapController mapController = proxy(IMapController.class, (proxy, method, args) -> switch (method.getName()) {
            case "getMapData" ->
                mapData;
            case "getRoomsByFunction" ->
                List.of(room);
            default ->
                null;
        });
        INavigationService navigationService = proxy(INavigationService.class, (proxy, method, args) -> new DefaultGraphPath<>());

        return new TaskManager(entityData, proxy(IGameWorldController.class, (proxy, method, args) -> null), mapController, null, null,
                navigationService, List.of(),
                proxy(ILevelInfo.class, (proxy, method, args) -> method.getName().equals("getLevelData") ? proxy(IKwdFile.class, (p, m, a) -> null) : null), null,
                gameSettings);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }
}
