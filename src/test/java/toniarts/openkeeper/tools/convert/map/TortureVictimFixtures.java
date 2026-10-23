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
package toniarts.openkeeper.tools.convert.map;

import java.util.EnumSet;

/**
 * Builds {@link Creature} fixtures for the torture tests. Lives in the data
 * classes' own package because their setters are {@code protected}
 * (parser-only).
 */
public final class TortureVictimFixtures {

    private TortureVictimFixtures() {
    }

    public static Creature victim(short creatureId, short tortureMoodChange, Creature.JobClass jobClass, boolean emotionless) {
        Creature creature = new Creature();
        creature.setCreatureId(creatureId);
        creature.setName("Creature " + creatureId);
        creature.setJobClass(jobClass);
        creature.setFlags2(emotionless ? EnumSet.of(Creature.CreatureFlag2.IS_EMOTIONLESS) : EnumSet.noneOf(Creature.CreatureFlag2.class));
        creature.getAttributes().setTortureMoodChange(tortureMoodChange);
        return creature;
    }

}
