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
 * Builds the terrains and the breach conditions for the tests. Lives in the
 * trigger package because the setters are {@code protected}.
 */
public final class BreachTriggerFixtures {

    private BreachTriggerFixtures() {
    }

    /**
     * A terrain that turns into another terrain when its health is gone
     *
     * @param terrainId the terrain id
     * @param destroyedTerrainId the terrain it becomes when destroyed
     * @param flags the terrain flags
     * @return the terrain
     */
    public static Terrain terrain(short terrainId, short destroyedTerrainId, Terrain.TerrainFlag... flags) {
        Terrain terrain = new Terrain();
        terrain.setTerrainId(terrainId);
        terrain.setDestroyedTypeTerrainId(destroyedTerrainId);
        EnumSet<Terrain.TerrainFlag> set = EnumSet.noneOf(Terrain.TerrainFlag.class);
        for (Terrain.TerrainFlag flag : flags) {
            set.add(flag);
        }
        terrain.setFlags(set);
        return terrain;
    }

    /**
     * A "dungeon breached" or "enemy breached" condition, like the one in
     * level 1
     *
     * @param id the trigger id
     * @param child the id of the first child
     * @param type {@code PLAYER_DUNGEON_BREACHED} or {@code PLAYER_ENEMY_BREACHED}
     * @return the condition
     */
    public static TriggerGeneric breach(int id, int child, TriggerGeneric.TargetType type) {
        TriggerGeneric trigger = new TriggerGeneric(null);
        trigger.setId(id);
        trigger.setIdChild(child);
        trigger.setRepeatTimes((short) 1);
        trigger.setType(type);
        trigger.setUserData("playerId", (short) 0); // Any
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
