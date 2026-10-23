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
package toniarts.openkeeper.game.logic;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureTortured;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.tools.convert.map.Creature;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A victim on the Electric Chair sits in the chair, the others are on the
 * Torture Wheel
 */
class TortureAnimationTest {

    private static final short TORTURE_WHEEL = 8;
    private static final short ELECTRIC_CHAIR = 61;

    private final EntityData entityData = new DefaultEntityData();

    @Test
    void victimOnTheWheel() {
        assertEquals(Creature.AnimationType.TORTURED_WHEEL, CreatureTorturingSystem.getTortureAnimation(entityData, victimOn(device(TORTURE_WHEEL))));
    }

    @Test
    void victimOnTheChair() {
        assertEquals(Creature.AnimationType.TORTURED_CHAIR, CreatureTorturingSystem.getTortureAnimation(entityData, victimOn(device(ELECTRIC_CHAIR))));
    }

    @Test
    void victimWithoutADevice() {
        assertEquals(Creature.AnimationType.TORTURED_WHEEL, CreatureTorturingSystem.getTortureAnimation(entityData, victimOn(null)));
    }

    private EntityId device(short objectId) {
        EntityId device = entityData.createEntity();
        entityData.setComponent(device, new ObjectComponent(objectId, null));
        return device;
    }

    private EntityId victimOn(EntityId device) {
        EntityId victim = entityData.createEntity();
        entityData.setComponent(victim, new CreatureTortured(0, 0, 0, device));
        return victim;
    }

}
