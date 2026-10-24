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

/**
 * Builds the creature count conditions and a creature type for the tests. Lives
 * in the trigger package because the setters are {@code protected}.
 */
public final class CreatureCountTriggerFixtures {

    private CreatureCountTriggerFixtures() {
    }

    /**
     * A creature type with the given id
     *
     * @param creatureId the creature id
     * @return the creature
     */
    public static Creature creature(short creatureId) {
        Creature creature = new Creature();
        creature.setCreatureId(creatureId);
        return creature;
    }

    /**
     * A "player creatures at level" condition, like the one in level 2. The
     * level is in the player byte of the data.
     *
     * @param id the trigger id
     * @param child the id of the first child
     * @param comparison how the count is compared to the value
     * @param level the level the creatures have to have at least
     * @param value the value to compare to
     * @return the condition
     */
    public static TriggerGeneric creaturesAtLevel(int id, int child, TriggerGeneric.ComparisonType comparison,
            short level, int value) {
        TriggerGeneric trigger = condition(id, child, TriggerGeneric.TargetType.PLAYER_CREATURES_AT_LEVEL, comparison, value);
        trigger.setUserData("targetId", (short) 0);
        trigger.setUserData("playerId", level);
        return trigger;
    }

    /**
     * A "player creatures dying" condition
     *
     * @param id the trigger id
     * @param child the id of the first child
     * @param comparison how the count is compared to the value
     * @param value the value to compare to
     * @return the condition
     */
    public static TriggerGeneric creaturesDying(int id, int child, TriggerGeneric.ComparisonType comparison, int value) {
        TriggerGeneric trigger = condition(id, child, TriggerGeneric.TargetType.PLAYER_CREATURES_DYING, comparison, value);
        trigger.setUserData("playerId", (short) 0);
        return trigger;
    }

    private static TriggerGeneric condition(int id, int child, TriggerGeneric.TargetType type,
            TriggerGeneric.ComparisonType comparison, int value) {
        TriggerGeneric trigger = new TriggerGeneric(null);
        trigger.setId(id);
        trigger.setIdChild(child);
        trigger.setRepeatTimes((short) 1);
        trigger.setType(type);
        trigger.setTargetValueComparison(comparison);
        trigger.setUserData("flag", (short) 1); // Constant value
        trigger.setUserData("value", value);
        return trigger;
    }

    /**
     * An action that sets a flag to a constant value
     *
     * @param id the trigger id
     * @param flagId the flag
     * @param value the value
     * @return the action
     */
    public static TriggerAction setFlag(int id, int flagId, int value) {
        TriggerAction action = new TriggerAction(null);
        action.setId(id);
        action.setType(TriggerAction.ActionType.FLAG);
        action.setUserData("flagId", (short) flagId);
        action.setUserData("flag", (short) (0x02 | 0x08)); // Constant value, "="
        action.setUserData("value", value);
        return action;
    }
}
