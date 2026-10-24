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

import java.util.Comparator;
import java.util.List;
import toniarts.openkeeper.game.map.IMapTileController;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;

/**
 * The arithmetic of the work imps do on a whole room: claiming one that is not
 * ours and repairing one that is. DKII.EXE keeps one health pool for the whole
 * room (0x4E5300), so the work does not depend on the size of the room; the
 * bigger the room, the longer it takes. We keep the health in the tiles and
 * spread the work over them.
 */
public final class RoomWork {

    private RoomWork() {
        // Nope
    }

    /**
     * The level variable that sets how fast a room is claimed. Rooms of nobody
     * have a variable of their own (DKII.EXE 0x497BF0), the rooms of any other
     * player use the convert room health.
     *
     * @param ownerId the current owner of the room
     * @return the variable
     */
    public static MiscType claimVariable(short ownerId) {
        return ownerId == Player.NEUTRAL_PLAYER_ID ? MiscType.CONVERT_NEUTRAL_ROOM_HEALTH : MiscType.CONVERT_ROOM_HEALTH;
    }

    /**
     * Damage the room. The damage is shared equally by the tiles, but what a
     * tile cannot take (it has no health left) goes to the other tiles, so the
     * room as a whole loses exactly the damage, like the one health pool of
     * the original.
     *
     * @param tiles the tiles of the room
     * @param damage the damage to the room as a whole, positive
     * @return true if the room ran out of health and is captured
     */
    public static boolean damage(List<IMapTileController> tiles, int damage) {
        List<IMapTileController> weakestFirst = tiles.stream()
                .sorted(Comparator.comparingInt(IMapTileController::getHealth))
                .toList();
        int remaining = damage;
        for (int i = 0; i < weakestFirst.size(); i++) {
            IMapTileController tile = weakestFirst.get(i);
            int taken = Math.min(tile.getHealth(), remaining / (weakestFirst.size() - i));
            tile.setHealth(tile.getHealth() - taken);
            remaining -= taken;
        }
        return tiles.stream().allMatch(tile -> tile.getHealth() == 0);
    }

    /**
     * Repair the room. The healing is shared equally, but what a tile cannot
     * take (it is whole already) goes to the tiles that still need it.
     *
     * @param tiles the tiles of the room
     * @param healing the healing of the room as a whole, positive
     * @return true if the room is whole now
     */
    public static boolean heal(List<IMapTileController> tiles, int healing) {
        List<IMapTileController> neediestLast = tiles.stream()
                .sorted(Comparator.comparingInt(tile -> tile.getMaxHealth() - tile.getHealth()))
                .toList();
        int remaining = healing;
        for (int i = 0; i < neediestLast.size(); i++) {
            IMapTileController tile = neediestLast.get(i);
            int given = Math.min(tile.getMaxHealth() - tile.getHealth(), remaining / (neediestLast.size() - i));
            tile.setHealth(tile.getHealth() + given);
            remaining -= given;
        }
        return isIntact(tiles);
    }

    /**
     * Is every tile of the room at full health
     *
     * @param tiles the tiles of the room
     * @return true if there is nothing to repair
     */
    public static boolean isIntact(List<IMapTileController> tiles) {
        return tiles.stream().allMatch(IMapTileController::isAtFullHealth);
    }

}
