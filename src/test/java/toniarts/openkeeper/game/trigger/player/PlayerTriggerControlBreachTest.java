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
package toniarts.openkeeper.game.trigger.player;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.TileBreaches;
import toniarts.openkeeper.game.controller.player.PlayerStatsControl;
import toniarts.openkeeper.game.state.session.PlayerService;
import toniarts.openkeeper.tools.convert.map.BreachTriggerFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Terrain.TerrainFlag;
import toniarts.openkeeper.tools.convert.map.Trigger;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric.TargetType;

/**
 * The "dungeon breached" and "enemy breached" conditions of a player tree: the
 * actions under them run when a wall of the player is brought down, or when the
 * player brings down a wall of another
 */
class PlayerTriggerControlBreachTest {

    private static final short PLAYER_ID = 3;
    private static final short OTHER_PLAYER_ID = 1;
    private static final int ROOT_TRIGGER_ID = 205;
    private static final int ACTION_TRIGGER_ID = 206;
    private static final int FLAG_ID = 7;

    private final TileBreaches breaches = new TileBreaches();
    private final Terrain claimedWall = BreachTriggerFixtures.terrain((short) 20, (short) 2, TerrainFlag.SOLID, TerrainFlag.OWNABLE);
    private final int[] flags = new int[16];

    @Test
    void nothingHappensBeforeAWallIsBrokenThrough() {
        PlayerTriggerControl control = buildControl(TargetType.PLAYER_DUNGEON_BREACHED);

        control.update(0.25f);

        assertEquals(0, flags[FLAG_ID]);
    }

    @Test
    void theOwnerOfTheWallGetsTheDungeonBreached() {
        PlayerTriggerControl control = buildControl(TargetType.PLAYER_DUNGEON_BREACHED);

        breaches.record(claimedWall, PLAYER_ID, OTHER_PLAYER_ID);
        control.update(0.25f);

        assertEquals(1, flags[FLAG_ID]);
    }

    @Test
    void theOwnerDoesNotGetTheEnemyBreached() {
        PlayerTriggerControl control = buildControl(TargetType.PLAYER_ENEMY_BREACHED);

        breaches.record(claimedWall, PLAYER_ID, OTHER_PLAYER_ID);
        control.update(0.25f);

        assertEquals(0, flags[FLAG_ID]);
    }

    @Test
    void theOneWhoBrokeThroughGetsTheEnemyBreached() {
        PlayerTriggerControl control = buildControl(TargetType.PLAYER_ENEMY_BREACHED);

        breaches.record(claimedWall, OTHER_PLAYER_ID, PLAYER_ID);
        control.update(0.25f);

        assertEquals(1, flags[FLAG_ID]);
    }

    @Test
    void theOneWhoBrokeThroughDoesNotGetTheDungeonBreached() {
        PlayerTriggerControl control = buildControl(TargetType.PLAYER_DUNGEON_BREACHED);

        breaches.record(claimedWall, OTHER_PLAYER_ID, PLAYER_ID);
        control.update(0.25f);

        assertEquals(0, flags[FLAG_ID]);
    }

    @Test
    void aBreachIsOneEventNotAState() {
        PlayerTriggerControl control = buildControl(TargetType.PLAYER_DUNGEON_BREACHED);

        breaches.record(claimedWall, PLAYER_ID, OTHER_PLAYER_ID);
        control.update(0.25f);
        flags[FLAG_ID] = 0;
        control.update(0.25f);

        assertEquals(0, flags[FLAG_ID]);
    }

    private PlayerTriggerControl buildControl(TargetType type) {
        Map<Integer, Trigger> triggers = new HashMap<>();
        triggers.put(ROOT_TRIGGER_ID, BreachTriggerFixtures.breach(ROOT_TRIGGER_ID, ACTION_TRIGGER_ID, type));
        triggers.put(ACTION_TRIGGER_ID, BreachTriggerFixtures.setFlag(ACTION_TRIGGER_ID, FLAG_ID, 1));
        IKwdFile kwdFile = stub(IKwdFile.class, (method, args) -> method.getName().equals("getTriggers") ? triggers : null);
        ILevelInfo levelInfo = stub(ILevelInfo.class, (method, args) -> switch (method.getName()) {
            case "getLevelData" ->
                kwdFile;
            case "getFlag" ->
                flags[(Integer) args[0]];
            case "setFlag" -> {
                flags[(Integer) args[0]] = (Integer) args[1];
                yield null;
            }
            default ->
                null;
        });
        IPlayerController playerController = stub(IPlayerController.class,
                (method, args) -> method.getName().equals("getStatsControl") ? new PlayerStatsControl(new toniarts.openkeeper.game.data.Keeper(false, (short) 0)) : null);
        IGameController gameController = stub(IGameController.class,
                (method, args) -> method.getName().equals("getPlayerController") ? playerController : null);
        IMapController mapController = stub(IMapController.class, (method, args) -> switch (method.getName()) {
            case "getDungeonBreaches" ->
                breaches.getDungeonBreaches((Short) args[0]);
            case "getEnemyBreaches" ->
                breaches.getEnemyBreaches((Short) args[0]);
            default ->
                null;
        });

        return new PlayerTriggerControl(gameController, levelInfo, stub(IGameTimer.class, (m, a) -> null),
                mapController, stub(ICreaturesController.class, (m, a) -> null),
                ROOT_TRIGGER_ID, PLAYER_ID, stub(PlayerService.class, (m, a) -> null));
    }

    private interface Answer {

        Object answer(Method method, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, Answer answer) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            Object result = answer.answer(method, args);
            if (result == null && method.getReturnType().isPrimitive()) {
                return method.getReturnType() == boolean.class ? (Object) false : (Object) 0;
            }
            return result;
        });
    }
}
