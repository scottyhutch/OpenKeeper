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
 * Builds the {@link Shot}, {@link KeeperSpell} and {@link Creature} fixtures
 * that the Thunderbolt tests need. Lives in the data classes' own package
 * because their setters are {@code protected} (parser-only).
 */
public final class ThunderboltFixtures {

    private ThunderboltFixtures() {
    }

    /**
     * A lightning shot
     *
     * @param shotId the shot ID
     * @param name the shot name
     * @return the shot
     */
    public static Shot lightningShot(short shotId, String name) {
        Shot shot = new Shot();
        shot.setShotId(shotId);
        shot.setName(name);
        shot.setProcessType(Shot.ProcessType.LIGHTNING);
        return shot;
    }

    /**
     * A keeper spell firing the given shot, upgraded or not
     *
     * @param shotTypeId the shot the spell fires
     * @return the keeper spell
     */
    public static KeeperSpell keeperSpell(short shotTypeId) {
        KeeperSpell keeperSpell = new KeeperSpell();
        keeperSpell.setName("Thunderbolt");
        keeperSpell.setShotTypeId(shotTypeId);
        keeperSpell.setBonusShotTypeId(shotTypeId);
        return keeperSpell;
    }

    /**
     * A creature with the given flags
     *
     * @param creatureId the creature ID
     * @param flags the creature flags
     * @param flags3 the third set of creature flags
     * @return the creature
     */
    public static Creature creature(short creatureId, EnumSet<Creature.CreatureFlag> flags, EnumSet<Creature.CreatureFlag3> flags3) {
        Creature creature = new Creature();
        creature.setCreatureId(creatureId);
        creature.setName("Test creature " + creatureId);
        creature.setFlags(flags);
        creature.setFlags3(flags3);
        return creature;
    }

}
