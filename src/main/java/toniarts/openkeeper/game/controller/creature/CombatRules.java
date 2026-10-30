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
package toniarts.openkeeper.game.controller.creature;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import java.util.Set;
import toniarts.openkeeper.tools.convert.map.Creature;

/**
 * The rules of a melee fight between creatures, as the original game has them
 */
public final class CombatRules {

    /**
     * How many creatures can attack the same creature at once in melee
     */
    public static final int MAX_MELEE_ATTACKERS = 8;

    /**
     * The original measures angles in 2048 steps per full circle
     */
    private static final int HALF_CIRCLE_STEPS = 1024;
    private static final int STEPS_PER_BONUS_PERCENT = 20;
    private static final int MIN_REAR_ATTACK_BONUS_PERCENT = 16;
    private static final int POSSESSED_DAMAGE_TAKEN_PERCENT = 50;

    private CombatRules() {
        // Nope
    }

    /**
     * Adds the bonus for a melee hit on a creature. The original doesn't look
     * at the way the target faces, it takes the direction from the target to
     * the attacker on the map: from the north there is no bonus, from the east
     * or the west 25 % and from the south 51 %. Below 16 % (about 56 degrees
     * off north) there is no bonus (0x498E30).
     *
     * @param damage the damage of the hit
     * @param attackerPosition where the attacker stands
     * @param targetPosition where the target stands
     * @return the damage with the bonus added
     */
    public static int addRearAttackBonus(int damage, Vector3f attackerPosition, Vector3f targetPosition) {
        float dx = attackerPosition.x - targetPosition.x;
        float dz = attackerPosition.z - targetPosition.z;
        if (dx == 0 && dz == 0) {
            return damage;
        }

        // Angle steps clockwise from north (-z), then folded to the steps away from north as the original
        // does it, where the very last step before north counts as south
        int steps = Math.floorMod((int) (FastMath.atan2(dx, -dz) / FastMath.PI * HALF_CIRCLE_STEPS), 2 * HALF_CIRCLE_STEPS);
        if (steps >= HALF_CIRCLE_STEPS) {
            steps = (2 * HALF_CIRCLE_STEPS - 2 - steps) & (HALF_CIRCLE_STEPS - 1);
        }
        int bonusPercent = steps / STEPS_PER_BONUS_PERCENT;
        if (bonusPercent < MIN_REAR_ATTACK_BONUS_PERCENT) {
            return damage;
        }

        return damage + damage * bonusPercent / 100;
    }

    /**
     * Scales the damage by how much of it the target takes. A possessed
     * creature takes only half of the damage.
     *
     * @param damage the damage of the hit
     * @param targetPossessed whether the target is possessed by its keeper
     * @return the damage the target takes
     */
    public static int getDamageTaken(int damage, boolean targetPossessed) {
        if (targetPossessed) {
            return damage * POSSESSED_DAMAGE_TAKEN_PERCENT / 100;
        }

        return damage;
    }

    /**
     * Can the attacker hurt the target at all. Horned Reapers take no damage,
     * and creatures only attackable by the Horned Reaper take damage only from
     * it.
     *
     * @param attackerFlags the attacker's creature flags
     * @param targetFlags the target's creature flags
     * @return true if the target takes damage from the attacker
     */
    public static boolean canBeHurtBy(Set<Creature.CreatureFlag> attackerFlags, Set<Creature.CreatureFlag> targetFlags) {
        if (targetFlags.contains(Creature.CreatureFlag.IS_HORNY)) {
            return false;
        }

        return !targetFlags.contains(Creature.CreatureFlag.ONLY_ATTACKABLE_BY_HORNY)
                || attackerFlags.contains(Creature.CreatureFlag.IS_HORNY);
    }

}
