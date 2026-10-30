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
package toniarts.openkeeper.view;

import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.map.Creature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static toniarts.openkeeper.tools.convert.map.CreatureEntranceFixtures.creature;

/**
 * A creature coming in through a Portal shows its own entrance effect, as in
 * the original.
 */
class CreatureArrivalEffectTest {

    private static final int GOBLIN_PORTAL_ENTRANCE = 181;

    @Test
    void creatureEnteringTheDungeonShowsItsEntranceEffect() {
        Creature goblin = creature((short) 7, GOBLIN_PORTAL_ENTRANCE);

        assertEquals(GOBLIN_PORTAL_ENTRANCE, PlayerEntityViewState.getArrivalEffectId(goblin, Creature.AnimationType.ENTRANCE));
    }

    @Test
    void creatureAppearingOtherwiseShowsNoEntranceEffect() {
        Creature goblin = creature((short) 7, GOBLIN_PORTAL_ENTRANCE);

        assertEquals(0, PlayerEntityViewState.getArrivalEffectId(goblin, Creature.AnimationType.IDLE_1));
        assertEquals(0, PlayerEntityViewState.getArrivalEffectId(goblin, Creature.AnimationType.DEATH_POSE));
    }

    @Test
    void creatureWithoutEntranceEffectShowsNothing() {
        Creature imp = creature((short) 1, 0);

        assertEquals(0, PlayerEntityViewState.getArrivalEffectId(imp, Creature.AnimationType.ENTRANCE));
    }

}
