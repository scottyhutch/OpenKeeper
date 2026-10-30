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
package toniarts.openkeeper.game.controller.player;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureExperience;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureCountTriggerFixtures;

/**
 * The counts of the creatures of a player by level and in the dying state
 */
class PlayerCreatureControlCountTest {

    private static final short IMP_ID = 1;
    private static final short WARLOCK_ID = 2;
    private static final short TROLL_ID = 3;

    private final EntityData entityData = new DefaultEntityData();
    private final Creature imp = CreatureCountTriggerFixtures.creature(IMP_ID);
    private final Creature warlock = CreatureCountTriggerFixtures.creature(WARLOCK_ID);
    private final Creature troll = CreatureCountTriggerFixtures.creature(TROLL_ID);
    private final PlayerCreatureControl control = new PlayerCreatureControl(new Keeper(false, (short) 3), imp,
            List.of(imp, warlock, troll), entityData);

    @Test
    void creaturesAtLeastAtTheLevelAreCounted() {
        add(warlock, 1, false);
        add(warlock, 2, false);
        add(troll, 5, false);

        assertEquals(3, control.getCountAtLevel(1));
        assertEquals(2, control.getCountAtLevel(2));
        assertEquals(1, control.getCountAtLevel(3));
        assertEquals(0, control.getCountAtLevel(6));
    }

    @Test
    void workersAreNotCountedByLevel() {
        add(imp, 5, true);
        add(warlock, 2, false);

        assertEquals(1, control.getCountAtLevel(2));
    }

    @Test
    void creaturesThatAreRemovedAreNotCounted() {
        EntityId removed = add(warlock, 2, false);
        control.onCreatureRemoved(removed, warlock);

        assertEquals(0, control.getCountAtLevel(1));
    }

    @Test
    void onlyTheUnconsciousAreDying() {
        add(warlock, 1, false);
        EntityId dying = add(warlock, 1, false);
        entityData.setComponent(dying, new Unconscious(0));

        assertEquals(1, control.getDyingCount((short) 0));
    }

    @Test
    void workersCanBeDyingToo() {
        EntityId dyingImp = add(imp, 1, true);
        entityData.setComponent(dyingImp, new Unconscious(0));

        assertEquals(1, control.getDyingCount((short) 0));
    }

    @Test
    void theDyingCanBeLimitedToACreatureType() {
        EntityId dyingWarlock = add(warlock, 1, false);
        entityData.setComponent(dyingWarlock, new Unconscious(0));
        EntityId dyingTroll = add(troll, 1, false);
        entityData.setComponent(dyingTroll, new Unconscious(0));

        assertEquals(2, control.getDyingCount((short) 0));
        assertEquals(1, control.getDyingCount(TROLL_ID));
    }

    private EntityId add(Creature creature, int level, boolean worker) {
        EntityId entity = entityData.createEntity();
        CreatureComponent component = new CreatureComponent();
        component.creatureId = creature.getCreatureId();
        component.worker = worker;
        entityData.setComponent(entity, component);
        entityData.setComponent(entity, new CreatureExperience(level, 0, 0, 0, 0));
        control.onCreatureAdded(entity, creature);
        return entity;
    }
}
