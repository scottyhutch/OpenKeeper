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

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.map.IMapTileController;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The room work of the imps: claiming a room that is not ours, and repairing
 * one that is. DKII.EXE keeps one health pool for the whole room (0x4E5300),
 * so the work of an imp is the same whatever the size of the room
 */
class RoomWorkTest {

    @Test
    void neutralRoomsAreClaimedWithTheNeutralRoomVariable() {
        assertEquals(MiscType.CONVERT_NEUTRAL_ROOM_HEALTH, RoomWork.claimVariable(Player.NEUTRAL_PLAYER_ID));
    }

    @Test
    void roomsOfAnotherKeeperAreClaimedWithTheConvertRoomVariable() {
        assertEquals(MiscType.CONVERT_ROOM_HEALTH, RoomWork.claimVariable(Player.KEEPER2_ID));
        assertEquals(MiscType.CONVERT_ROOM_HEALTH, RoomWork.claimVariable(Player.GOOD_PLAYER_ID));
    }

    @Test
    void damageOfTheWholeRoomIsTheSameForBigAndSmallRooms() {
        List<IMapTileController> small = tiles(10000, 10000, 2);
        List<IMapTileController> big = tiles(10000, 10000, 8);

        RoomWork.damage(small, 4000);
        RoomWork.damage(big, 4000);

        assertEquals(4000, 20000 - total(small));
        assertEquals(4000, 80000 - total(big));
    }

    @Test
    void damageKeepsTheRemainder() {
        List<IMapTileController> room = tiles(100, 100, 3);

        RoomWork.damage(room, 10);

        assertEquals(290, total(room));
    }

    @Test
    void damageThatOneTileCannotTakeGoesToTheOthers() {
        List<IMapTileController> room = List.of(tile(100, 10000), tile(10000, 10000));

        RoomWork.damage(room, 9000);

        assertEquals(0, room.get(0).getHealth());
        assertEquals(1100, room.get(1).getHealth());
    }

    @Test
    void roomIsCapturedWhenTheHealthRunsOut() {
        List<IMapTileController> room = tiles(1000, 10000, 4);

        assertFalse(RoomWork.damage(room, 3999));
        assertTrue(RoomWork.damage(room, 1));
        assertEquals(0, total(room));
    }

    @Test
    void healingGoesToTheTilesThatNeedIt() {
        List<IMapTileController> room = List.of(tile(10000, 10000), tile(2000, 10000), tile(10000, 10000));

        assertFalse(RoomWork.heal(room, 5000));

        assertEquals(7000, room.get(1).getHealth());
        assertEquals(10000, room.get(0).getHealth());
    }

    @Test
    void healingNeverGoesOverTheMaximumAndTellsWhenTheRoomIsWhole() {
        List<IMapTileController> room = List.of(tile(9900, 10000), tile(5000, 10000));

        assertTrue(RoomWork.heal(room, 20000));

        assertEquals(10000, room.get(0).getHealth());
        assertEquals(10000, room.get(1).getHealth());
    }

    @Test
    void healingOfAnUnevenRoomSpendsTheWholeShareOnTheTileThatNeedsIt() {
        List<IMapTileController> room = List.of(tile(10000, 10000), tile(10000, 10000), tile(0, 10000));

        RoomWork.heal(room, 6000);

        assertEquals(6000, room.get(2).getHealth());
    }

    @Test
    void roomIsIntactOnlyWhenEveryTileIsAtFullHealth() {
        assertTrue(RoomWork.isIntact(tiles(10000, 10000, 3)));
        assertFalse(RoomWork.isIntact(List.of(tile(10000, 10000), tile(9999, 10000))));
    }

    private static List<IMapTileController> tiles(int health, int maxHealth, int count) {
        return IntStream.range(0, count).mapToObj(i -> tile(health, maxHealth)).toList();
    }

    private static int total(List<IMapTileController> tiles) {
        return tiles.stream().mapToInt(IMapTileController::getHealth).sum();
    }

    private static IMapTileController tile(int health, int maxHealth) {
        int[] values = {health};
        return (IMapTileController) Proxy.newProxyInstance(IMapTileController.class.getClassLoader(),
                new Class<?>[]{IMapTileController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getHealth" ->
                        values[0];
                    case "getMaxHealth" ->
                        maxHealth;
                    case "setHealth" -> {
                        values[0] = (Integer) args[0];
                        yield null;
                    }
                    case "isAtFullHealth" ->
                        values[0] == maxHealth;
                    default ->
                        throw new UnsupportedOperationException(method.getName());
                });
    }

}
