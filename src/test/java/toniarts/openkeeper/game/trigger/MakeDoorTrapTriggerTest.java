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
package toniarts.openkeeper.game.trigger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.player.PlayerDoorControl;
import toniarts.openkeeper.game.controller.player.PlayerTrapControl;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.tools.convert.map.Door;
import toniarts.openkeeper.tools.convert.map.DoorFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Trap;
import toniarts.openkeeper.tools.convert.map.TrapFixtures;
import toniarts.openkeeper.tools.convert.map.TriggerAction;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The MAKE trigger action makes a door or a trap available or unavailable to
 * the player it names, like it does for rooms (DKII.EXE 0x518AD0 case 6)
 */
class MakeDoorTrapTriggerTest {

    private static final short PLAYER = 3;
    private static final short OTHER_PLAYER = 4;

    private final Door door = DoorFixtures.door((short) 2, 0);
    private final Trap trap = TrapFixtures.trap((short) 5, 0);
    private final Map<Short, PlayerDoorControl> doorControls = Map.of(
            PLAYER, new PlayerDoorControl(new Keeper(false, PLAYER), List.of(door)),
            OTHER_PLAYER, new PlayerDoorControl(new Keeper(false, OTHER_PLAYER), List.of(door)));
    private final Map<Short, PlayerTrapControl> trapControls = Map.of(
            PLAYER, new PlayerTrapControl(new Keeper(false, PLAYER), List.of(trap)),
            OTHER_PLAYER, new PlayerTrapControl(new Keeper(false, OTHER_PLAYER), List.of(trap)));
    private final TriggerControl control = createControl();

    @Test
    void makesTheDoorAvailableAndUnavailableToTheNamedPlayer() {
        make(TriggerAction.MakeType.DOOR, door.getDoorId(), true);

        assertEquals(List.of(door), doorControls.get(PLAYER).getTypesAvailable());
        assertEquals(List.of(), doorControls.get(OTHER_PLAYER).getTypesAvailable());

        make(TriggerAction.MakeType.DOOR, door.getDoorId(), false);

        assertEquals(List.of(), doorControls.get(PLAYER).getTypesAvailable());
    }

    @Test
    void makesTheTrapAvailableAndUnavailableToTheNamedPlayer() {
        make(TriggerAction.MakeType.TRAP, trap.getTrapId(), true);

        assertEquals(List.of(trap), trapControls.get(PLAYER).getTypesAvailable());
        assertEquals(List.of(), trapControls.get(OTHER_PLAYER).getTypesAvailable());

        make(TriggerAction.MakeType.TRAP, trap.getTrapId(), false);

        assertEquals(List.of(), trapControls.get(PLAYER).getTypesAvailable());
    }

    private void make(TriggerAction.MakeType type, short targetId, boolean available) {
        TriggerActionData action = new TriggerActionData(2, null, TriggerAction.ActionType.MAKE);
        action.setUserData("type", (short) type.getValue());
        action.setUserData("available", (short) (available ? 1 : 0));
        action.setUserData("playerId", PLAYER);
        action.setUserData("targetId", targetId);
        control.doAction(action);
    }

    private TriggerControl createControl() {
        IKwdFile kwdFile = proxy(IKwdFile.class, (p, method, args) -> switch (method.getName()) {
            case "getTriggers" ->
                Map.of(1, new TriggerGeneric(null));
            case "getDoorById" ->
                door;
            case "getTrapById" ->
                trap;
            default ->
                throw new UnsupportedOperationException(method.getName());
        });
        ILevelInfo levelInfo = proxy(ILevelInfo.class, (p, method, args) -> kwdFile);
        IGameController gameController = proxy(IGameController.class, (p, method, args) -> {
            short playerId = (short) args[0];
            return proxy(IPlayerController.class, (p2, playerMethod, playerArgs) -> switch (playerMethod.getName()) {
                case "getDoorControl" ->
                    doorControls.get(playerId);
                case "getTrapControl" ->
                    trapControls.get(playerId);
                default ->
                    throw new UnsupportedOperationException(playerMethod.getName());
            });
        });

        return new TriggerControl(gameController, levelInfo, null, null, null, 1);
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }
}
