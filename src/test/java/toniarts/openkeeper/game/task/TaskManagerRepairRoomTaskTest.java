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

import com.badlogic.gdx.ai.pfa.GraphPath;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileController;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.hero.RoomTileWorld;
import toniarts.openkeeper.tools.convert.map.DestroyRoomsFixtures;
import toniarts.openkeeper.tools.convert.map.GameLevel;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A damaged room of a keeper gets repair jobs for his imps, once a second, and
 * only while there is something to repair
 */
class TaskManagerRepairRoomTaskTest {

    private static final int TICKS_PER_SEC = 4;
    private static final Point DAMAGED = new Point(4, 5);
    private static final Point WHOLE = new Point(5, 5);

    private final EntityData entityData = new DefaultEntityData();
    private final List<Point> damagedTiles = new ArrayList<>(List.of(DAMAGED));
    private final List<Task> assignedTasks = new ArrayList<>();
    private final RoomInstance roomInstance = createRoomInstance();
    private final ICreatureController imp = RoomTileWorld.stub(ICreatureController.class, (proxy, method, args) -> switch (method.getName()) {
        case "getOwnerId" ->
            Player.KEEPER1_ID;
        case "getCreatureCoordinates" ->
            new Point(0, 0);
        case "getEntityId" ->
            new EntityId(1);
        case "getPosition" ->
            new Vector3f(0, 0, 0);
        case "setAssignedTask" -> {
            assignedTasks.add((Task) args[0]);
            yield null;
        }
        default ->
            RoomTileWorld.defaultValue(proxy, method);
    });
    private final IRoomController room = RoomTileWorld.stub(IRoomController.class, (proxy, method, args) -> switch (method.getName()) {
        case "getOwnerId" ->
            Player.KEEPER1_ID;
        case "getRoomInstance" ->
            roomInstance;
        default ->
            RoomTileWorld.defaultValue(proxy, method);
    });
    private final IMapTileController tile = RoomTileWorld.stub(IMapTileController.class, (proxy, method, args) -> RoomTileWorld.defaultValue(proxy, method));
    private final IMapDataInformation<?> mapData = RoomTileWorld.stub(IMapDataInformation.class, (proxy, method, args) -> switch (method.getName()) {
        case "getWidth", "getHeight" ->
            10;
        case "getTile" ->
            tile;
        default ->
            RoomTileWorld.defaultValue(proxy, method);
    });
    private final INavigationService navigationService = RoomTileWorld.stub(INavigationService.class, (proxy, method, args) -> switch (method.getName()) {
        case "isAccessible" ->
            true;
        case "findPath" ->
            RoomTileWorld.stub(GraphPath.class, (path, call, callArgs) -> RoomTileWorld.defaultValue(path, call));
        default ->
            RoomTileWorld.defaultValue(proxy, method);
    });
    private final IMapController mapController = RoomTileWorld.stub(IMapController.class, (proxy, method, args) -> switch (method.getName()) {
        case "getMapData" ->
            mapData;
        case "getRoomControllers" ->
            List.of(room);
        case "isRepairableRoom" ->
            damagedTiles.contains((Point) args[0]) && (short) args[1] == Player.KEEPER1_ID;
        default ->
            RoomTileWorld.defaultValue(proxy, method);
    });
    private final TaskManager taskManager = createTaskManager();

    @Test
    void damagedRoomTileGetsARepairJobWithinASecond() {
        tick(TICKS_PER_SEC);

        Task task = assignRepairJob();

        assertEquals(DAMAGED, task.getTaskLocation());
        assertEquals(TaskType.CLAIM_ROOM, task.getTaskType());
    }

    @Test
    void noJobBeforeTheSecondIsOver() {
        tick(TICKS_PER_SEC - 1);

        assertFalse(workIsAvailable());
    }

    @Test
    void wholeRoomGetsNoJob() {
        damagedTiles.clear();
        tick(TICKS_PER_SEC);

        assertFalse(workIsAvailable());
    }

    @Test
    void jobsAreNotDuplicatedAndGoWhenTheRoomIsRepaired() {
        tick(TICKS_PER_SEC * 3);
        assertTrue(workIsAvailable());

        damagedTiles.clear();
        tick(TICKS_PER_SEC);

        assertFalse(workIsAvailable());
    }

    private boolean workIsAvailable() {
        boolean[] result = new boolean[1];
        taskManager.addToUnemployedWorkerQueue(imp, accepted -> result[0] = accepted);
        taskManager.processUnemployedWorkerQueue();
        return result[0];
    }

    private Task assignRepairJob() {
        assertTrue(workIsAvailable());
        return assignedTasks.getFirst();
    }

    private void tick(int ticks) {
        for (int i = 0; i < ticks; i++) {
            taskManager.processTick(0.25f);
        }
    }

    private static RoomInstance createRoomInstance() {
        RoomInstance instance = new RoomInstance(DestroyRoomsFixtures.destroyRoomsRoom("Lair", RoomTileWorld.LAIR_TERRAIN_ID));
        instance.addCoordinate(DAMAGED);
        instance.addCoordinate(WHOLE);
        return instance;
    }

    private TaskManager createTaskManager() {
        GameLevel gameLevel = DestroyRoomsFixtures.destroyRoomsGameLevel(TICKS_PER_SEC);
        IKwdFile kwdFile = RoomTileWorld.stub(IKwdFile.class, (proxy, method, args) -> method.getName().equals("getGameLevel") ? gameLevel : RoomTileWorld.defaultValue(proxy, method));
        Keeper keeper = new Keeper(false, Player.KEEPER1_ID);
        IPlayerController player = RoomTileWorld.stub(IPlayerController.class, (proxy, method, args) -> method.getName().equals("getKeeper") ? keeper : RoomTileWorld.defaultValue(proxy, method));
        return new TaskManager(entityData, RoomTileWorld.stub(IGameWorldController.class, (proxy, method, args) -> RoomTileWorld.defaultValue(proxy, method)), mapController,
                RoomTileWorld.stub(IObjectsController.class, (proxy, method, args) -> RoomTileWorld.defaultValue(proxy, method)),
                RoomTileWorld.stub(ICreaturesController.class, (proxy, method, args) -> RoomTileWorld.defaultValue(proxy, method)),
                navigationService,
                List.of(player),
                RoomTileWorld.stub(ILevelInfo.class, (proxy, method, args) -> method.getName().equals("getLevelData") ? kwdFile : RoomTileWorld.defaultValue(proxy, method)),
                RoomTileWorld.stub(IEntityPositionLookup.class, (proxy, method, args) -> RoomTileWorld.defaultValue(proxy, method)),
                new EnumMap<>(MiscType.class));
    }

}
