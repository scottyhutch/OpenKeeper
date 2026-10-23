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
package toniarts.openkeeper.view.control;

import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.map.Creature.AnimationType;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Which model a creature is drawn with
 */
class CreatureViewControlTest {

    @Test
    void creatureLooksLikeItselfWhenNotMorphed() {
        for (AnimationType state : AnimationType.values()) {
            assertEquals(CreatureViewControl.Look.OWN, CreatureViewControl.getLook(false, state));
        }
    }

    @Test
    void morphedCreatureLooksLikeTheChicken() {
        assertEquals(CreatureViewControl.Look.MORPH, CreatureViewControl.getLook(true, AnimationType.WALK));
        assertEquals(CreatureViewControl.Look.MORPH, CreatureViewControl.getLook(true, AnimationType.IDLE_1));
        assertEquals(CreatureViewControl.Look.MORPH, CreatureViewControl.getLook(true, AnimationType.STAND_STILL));
    }

    @Test
    void morphedCreatureInHandDanglesAsTheChicken() {
        assertEquals(CreatureViewControl.Look.MORPH_IN_HAND, CreatureViewControl.getLook(true, AnimationType.IN_HAND));
    }

    @Test
    void morphedCreatureDiesAsItself() {
        assertEquals(CreatureViewControl.Look.OWN, CreatureViewControl.getLook(true, AnimationType.DIE));
        assertEquals(CreatureViewControl.Look.OWN, CreatureViewControl.getLook(true, AnimationType.DEATH_POSE));
    }

}
