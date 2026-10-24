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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import toniarts.openkeeper.tools.convert.map.Terrain;

/**
 * Keeps count of the dungeon breaches. A breach is when a wall that a player
 * owns (a claimed wall) is brought down by another player. The owner of the wall
 * has had its dungeon breached, the other player has breached an enemy dungeon.
 * The counts are read by the player triggers, which turn their growth into
 * events.
 */
public final class TileBreaches {

    private final Map<Short, AtomicInteger> dungeonBreaches = new ConcurrentHashMap<>();
    private final Map<Short, AtomicInteger> enemyBreaches = new ConcurrentHashMap<>();

    /**
     * Tell that a tile was destroyed. Counts it, if it was a breach.
     *
     * @param terrain the terrain of the tile before it was destroyed
     * @param tileOwnerId the owner of the tile
     * @param attackerId the player who destroyed the tile
     * @return true if the tile was a breach
     */
    public boolean record(Terrain terrain, short tileOwnerId, short attackerId) {
        if (tileOwnerId == 0 || tileOwnerId == attackerId || !isOwnableWall(terrain)
                || terrain.getDestroyedTypeTerrainId() == terrain.getTerrainId()) {
            return false;
        }
        dungeonBreaches.computeIfAbsent(tileOwnerId, key -> new AtomicInteger()).incrementAndGet();
        enemyBreaches.computeIfAbsent(attackerId, key -> new AtomicInteger()).incrementAndGet();
        return true;
    }

    private static boolean isOwnableWall(Terrain terrain) {
        return terrain.getFlags().contains(Terrain.TerrainFlag.SOLID) && terrain.getFlags().contains(Terrain.TerrainFlag.OWNABLE);
    }

    /**
     * How many times the dungeon of the player has been breached
     *
     * @param playerId the player
     * @return the number of breaches
     */
    public int getDungeonBreaches(short playerId) {
        AtomicInteger count = dungeonBreaches.get(playerId);
        return count != null ? count.get() : 0;
    }

    /**
     * How many times the player has breached an enemy dungeon
     *
     * @param playerId the player
     * @return the number of breaches
     */
    public int getEnemyBreaches(short playerId) {
        AtomicInteger count = enemyBreaches.get(playerId);
        return count != null ? count.get() : 0;
    }
}
