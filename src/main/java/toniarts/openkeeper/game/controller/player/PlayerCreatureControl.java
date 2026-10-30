/*
 * Copyright (C) 2014-2016 OpenKeeper
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
package toniarts.openkeeper.game.controller.player;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureExperience;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.tools.convert.map.Creature;

import java.util.*;

/**
 * Holds a list of player creatures and functionality related to them
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class PlayerCreatureControl extends AbstractPlayerControl<Creature, Set<EntityId>, Short> {

    private final Creature imp;
    private final EntityData entityData;
    private int creatureCount = 0;

    /**
     * A creature control that does not know the creature entities, the counts
     * by level and by state are always 0
     *
     * @param keeper the player
     * @param imp the worker creature type
     * @param creatures the creature types
     */
    public PlayerCreatureControl(Keeper keeper, Creature imp, Collection<Creature> creatures) {
        this(keeper, imp, creatures, new DefaultEntityData());
    }

    /**
     * @param keeper the player
     * @param imp the worker creature type
     * @param creatures the creature types
     * @param entityData the entities of the creatures, for the counts by level
     * and by state
     */
    public PlayerCreatureControl(Keeper keeper, Creature imp, Collection<Creature> creatures, EntityData entityData) {
        super(keeper, keeper.getAvailableCreatures(), creatures);
        this.imp = imp;
        this.entityData = entityData;
    }

    @Override
    protected short getDataTypeId(Short type) {
        return type;
    }

    @Override
    protected Short getDataType(Creature type) {
        return type.getCreatureId();
    }

    public void onCreatureAdded(EntityId entityId, Creature creature) {

        // Add to the list
        Set<EntityId> creatureSet = get(creature);
        if (creatureSet == null) {
            creatureSet = new LinkedHashSet<>();
            put(creature, creatureSet);
        }
        creatureSet.add(entityId);

        // Listeners
        if (!isImp(creature)) {
            creatureCount++;
        }
    }

    public void onCreatureRemoved(EntityId entityId, Creature creature) {

        // Delete
        Set<EntityId> creatureSet = get(creature);
        if (creatureSet != null) {
            creatureSet.remove(entityId);
        }

        // Listeners
        if (!isImp(creature)) {
            creatureCount--;
        }
    }

    /**
     * Get all creatures
     *
     * @return the creatures
     */
    public Map<Creature, Set<EntityId>> getAllCreatures() {
        return new LinkedHashMap<>(types);
    }

    /**
     * Get creatures, minus the imps
     *
     * @return the creatures
     */
    public Map<Creature, Set<EntityId>> getCreatures() {
        Map<Creature, Set<EntityId>> map = getAllCreatures();
        map.remove(imp);
        return map;
    }

    private boolean isImp(Creature creature) {
        return creature.equals(imp);
    }

    public Creature getImp() {
        return imp;
    }

    /**
     * Get player creature count. Excluding imps.
     *
     * @return the creature count
     */
    @Override
    public int getTypeCount() {
        return creatureCount;
    }

    /**
     * Count the creatures that have at least the given level. The workers are
     * not counted, like in the original.
     *
     * @param level the lowest level that is counted
     * @return the number of the creatures
     */
    public int getCountAtLevel(int level) {
        int count = 0;
        for (Set<EntityId> creatureSet : List.copyOf(types.values())) {
            for (EntityId entityId : List.copyOf(creatureSet)) {
                CreatureComponent creature = entityData.getComponent(entityId, CreatureComponent.class);
                CreatureExperience experience = entityData.getComponent(entityId, CreatureExperience.class);
                if (creature != null && !creature.worker && experience != null && experience.level >= level) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Count the creatures that are in the dying state (unconscious, waiting to
     * be rescued or captured). The workers are counted too.
     *
     * @param creatureId the creature type, 0 = any
     * @return the number of the creatures
     */
    public int getDyingCount(short creatureId) {
        int count = 0;
        for (Map.Entry<Creature, Set<EntityId>> entry : List.copyOf(types.entrySet())) {
            if (creatureId != 0 && entry.getKey().getCreatureId() != creatureId) {
                continue;
            }
            for (EntityId entityId : List.copyOf(entry.getValue())) {
                if (entityData.getComponent(entityId, Unconscious.class) != null) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Get the imp count
     *
     * @return the number of imps
     */
    public int getImpCount() {
        if (imp == null) {
            return 0;
        }
        Set<EntityId> imps = get(imp);
        return (imps != null ? imps.size() : 0);
    }

}
