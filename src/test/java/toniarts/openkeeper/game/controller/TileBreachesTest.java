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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.map.BreachTriggerFixtures;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Terrain.TerrainFlag;

/**
 * When a destroyed wall counts as a breach of a dungeon: a claimed wall of one
 * player that is brought down by another player
 */
class TileBreachesTest {

    private static final short OWNER = 1;
    private static final short ATTACKER = 3;

    private final TileBreaches breaches = new TileBreaches();
    private final Terrain claimedWall = BreachTriggerFixtures.terrain((short) 20, (short) 2, TerrainFlag.SOLID, TerrainFlag.OWNABLE);

    @Test
    void bringingDownAnEnemyWallIsABreach() {
        assertTrue(breaches.record(claimedWall, OWNER, ATTACKER));

        assertEquals(1, breaches.getDungeonBreaches(OWNER));
        assertEquals(1, breaches.getEnemyBreaches(ATTACKER));
    }

    @Test
    void theBreachIsOnlyForTheTwoPlayers() {
        breaches.record(claimedWall, OWNER, ATTACKER);

        assertEquals(0, breaches.getDungeonBreaches(ATTACKER));
        assertEquals(0, breaches.getEnemyBreaches(OWNER));
        assertEquals(0, breaches.getDungeonBreaches((short) 2));
    }

    @Test
    void everyWallIsABreach() {
        breaches.record(claimedWall, OWNER, ATTACKER);
        breaches.record(claimedWall, OWNER, ATTACKER);

        assertEquals(2, breaches.getDungeonBreaches(OWNER));
    }

    @Test
    void diggingYourOwnWallIsNoBreach() {
        assertFalse(breaches.record(claimedWall, OWNER, OWNER));

        assertEquals(0, breaches.getDungeonBreaches(OWNER));
    }

    @Test
    void aWallThatNobodyOwnsIsNoBreach() {
        assertFalse(breaches.record(claimedWall, (short) 0, ATTACKER));

        assertEquals(0, breaches.getEnemyBreaches(ATTACKER));
    }

    @Test
    void aWallThatCannotBeOwnedIsNoBreach() {
        Terrain rock = BreachTriggerFixtures.terrain((short) 4, (short) 2, TerrainFlag.SOLID);

        assertFalse(breaches.record(rock, OWNER, ATTACKER));
    }

    @Test
    void aTileThatIsNotSolidIsNoBreach() {
        Terrain claimedPath = BreachTriggerFixtures.terrain((short) 3, (short) 2, TerrainFlag.OWNABLE);

        assertFalse(breaches.record(claimedPath, OWNER, ATTACKER));
    }

    @Test
    void aTileThatStaysTheSameWhenDestroyedIsNoBreach() {
        Terrain stays = BreachTriggerFixtures.terrain((short) 20, (short) 20, TerrainFlag.SOLID, TerrainFlag.OWNABLE);

        assertFalse(breaches.record(stays, OWNER, ATTACKER));
    }
}
