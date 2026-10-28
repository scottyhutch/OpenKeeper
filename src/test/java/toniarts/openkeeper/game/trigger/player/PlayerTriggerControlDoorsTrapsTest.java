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

import com.simsilica.es.EntityData;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.TrapComponent;
import toniarts.openkeeper.game.component.TrapViewState;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.player.PlayerDoorControl;
import toniarts.openkeeper.game.controller.player.PlayerStatsControl;
import toniarts.openkeeper.game.controller.player.PlayerTrapControl;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.state.session.PlayerService;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.PlacedCountTriggerFixtures;
import toniarts.openkeeper.tools.convert.map.Trigger;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric.ComparisonType;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric.TargetType;

/**
 * The "player doors" and "player traps" conditions of a player tree: the count
 * of the player's doors or traps, by type, or the blueprints or the built ones
 */
class PlayerTriggerControlDoorsTrapsTest {

    private static final short PLAYER_ID = 3;
    private static final short OTHER_PLAYER_ID = 4;
    private static final int ROOT_TRIGGER_ID = 205;
    private static final int ACTION_TRIGGER_ID = 206;
    private static final int FLAG_ID = 7;

    // The flag byte of the level data
    private static final short COUNT_TYPE = 0x01;
    private static final short COUNT_BLUEPRINTS = 0x01 | 0x08;
    private static final short COUNT_BUILT = 0x01 | 0x10;

    private final EntityData entityData = new DefaultEntityData();
    private final PlayerDoorControl doors = new PlayerDoorControl(new Keeper(false, PLAYER_ID), List.of(), entityData);
    private final PlayerTrapControl traps = new PlayerTrapControl(new Keeper(false, PLAYER_ID), List.of(), entityData);
    private final int[] flags = new int[16];

    @Test
    void noTrapsAtTheStartIsZero() {
        PlayerTriggerControl control = buildControl(TargetType.PLAYER_TRAPS, ComparisonType.EQUAL_TO, 0, COUNT_TYPE, 0);

        control.update(0.25f);

        assertEquals(1, flags[FLAG_ID]);
    }

    @Test
    void placingATrapStopsTheZeroCount() {
        PlayerTriggerControl control = buildControl(TargetType.PLAYER_TRAPS, ComparisonType.EQUAL_TO, 0, COUNT_TYPE, 0);
        addTrap(PLAYER_ID, 3, false);

        control.update(0.25f);

        assertEquals(0, flags[FLAG_ID]);
    }

    @Test
    void aTrapBlueprintIsNotABuiltTrap() {
        PlayerTriggerControl built = buildControl(TargetType.PLAYER_TRAPS, ComparisonType.GREATER_OR_EQUAL_TO, 0, COUNT_BUILT, 1);
        addTrap(PLAYER_ID, 3, true);

        built.update(0.25f);

        assertEquals(0, flags[FLAG_ID]);
    }

    @Test
    void aTrapBlueprintCountsAsABlueprint() {
        PlayerTriggerControl blueprints = buildControl(TargetType.PLAYER_TRAPS, ComparisonType.GREATER_OR_EQUAL_TO, 3, COUNT_BLUEPRINTS, 1);
        addTrap(PLAYER_ID, 3, true);

        blueprints.update(0.25f);

        assertEquals(1, flags[FLAG_ID]);
    }

    @Test
    void aBuiltTrapCountsAsBuilt() {
        PlayerTriggerControl built = buildControl(TargetType.PLAYER_TRAPS, ComparisonType.GREATER_OR_EQUAL_TO, 3, COUNT_BUILT, 1);
        addTrap(PLAYER_ID, 3, false);

        built.update(0.25f);

        assertEquals(1, flags[FLAG_ID]);
    }

    @Test
    void theTrapTypeIsLookedAtWhenCountingAll() {
        PlayerTriggerControl byType = buildControl(TargetType.PLAYER_TRAPS, ComparisonType.GREATER_OR_EQUAL_TO, 2, COUNT_TYPE, 1);
        addTrap(PLAYER_ID, 3, false);

        byType.update(0.25f);

        assertEquals(0, flags[FLAG_ID]);
    }

    @Test
    void theBuiltCountIgnoresTheTypeLikeTheOriginal() {
        PlayerTriggerControl built = buildControl(TargetType.PLAYER_TRAPS, ComparisonType.GREATER_OR_EQUAL_TO, 2, COUNT_BUILT, 1);
        addTrap(PLAYER_ID, 3, false);

        built.update(0.25f);

        assertEquals(1, flags[FLAG_ID]);
    }

    @Test
    void anotherPlayersTrapsDoNotCount() {
        PlayerTriggerControl control = buildControl(TargetType.PLAYER_TRAPS, ComparisonType.GREATER_OR_EQUAL_TO, 0, COUNT_TYPE, 1);
        addTrap(OTHER_PLAYER_ID, 3, false);

        control.update(0.25f);

        assertEquals(0, flags[FLAG_ID]);
    }

    @Test
    void aBuiltDoorCountsAsBuilt() {
        PlayerTriggerControl built = buildControl(TargetType.PLAYER_DOORS, ComparisonType.GREATER_OR_EQUAL_TO, 1, COUNT_BUILT, 1);
        addDoor(PLAYER_ID, 1, false);

        built.update(0.25f);

        assertEquals(1, flags[FLAG_ID]);
    }

    @Test
    void aDoorBlueprintIsNotABuiltDoor() {
        PlayerTriggerControl built = buildControl(TargetType.PLAYER_DOORS, ComparisonType.GREATER_OR_EQUAL_TO, 1, COUNT_BUILT, 1);
        addDoor(PLAYER_ID, 1, true);

        built.update(0.25f);

        assertEquals(0, flags[FLAG_ID]);
    }

    @Test
    void theDoorsOfATypeAreCounted() {
        PlayerTriggerControl control = buildControl(TargetType.PLAYER_DOORS, ComparisonType.EQUAL_TO, 1, COUNT_TYPE, 1);
        addDoor(PLAYER_ID, 1, false);

        control.update(0.25f);

        assertEquals(1, flags[FLAG_ID]);
    }

    private void addDoor(short ownerId, int doorId, boolean blueprint) {
        var entity = entityData.createEntity();
        entityData.setComponent(entity, new DoorComponent((short) doorId, false, blueprint));
        entityData.setComponent(entity, new Owner(ownerId, ownerId));
    }

    private void addTrap(short ownerId, int trapId, boolean blueprint) {
        var entity = entityData.createEntity();
        entityData.setComponent(entity, new TrapComponent((short) trapId));
        entityData.setComponent(entity, new Owner(ownerId, ownerId));
        entityData.setComponent(entity, new TrapViewState((short) trapId, blueprint));
    }

    private PlayerTriggerControl buildControl(TargetType type, ComparisonType comparison, int targetId, short flag, int value) {
        Map<Integer, Trigger> triggers = new HashMap<>();
        triggers.put(ROOT_TRIGGER_ID, PlacedCountTriggerFixtures.placedCount(ROOT_TRIGGER_ID, ACTION_TRIGGER_ID, type,
                comparison, (short) targetId, flag, value));
        triggers.put(ACTION_TRIGGER_ID, PlacedCountTriggerFixtures.setFlag(ACTION_TRIGGER_ID, FLAG_ID, 1));
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
        IPlayerController playerController = stub(IPlayerController.class, (method, args) -> switch (method.getName()) {
            case "getDoorControl" ->
                doors;
            case "getTrapControl" ->
                traps;
            case "getStatsControl" ->
                new PlayerStatsControl(new toniarts.openkeeper.game.data.Keeper(false, (short) 0));
            default ->
                null;
        });
        IGameController gameController = stub(IGameController.class,
                (method, args) -> method.getName().equals("getPlayerController") ? playerController : null);

        return new PlayerTriggerControl(gameController, levelInfo, stub(IGameTimer.class, (m, a) -> null),
                stub(IMapController.class, (m, a) -> null), stub(ICreaturesController.class, (m, a) -> null),
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
