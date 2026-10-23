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

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.EnumSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureImprisoned;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.ThunderboltFixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A creature struck by the keeper's Thunderbolt falls over and gets up again,
 * as after being dropped from the hand
 */
class CreatureKnockDownTest {

    private static final double NOW = 100;

    private EntityData entityData;
    private EntityId entity;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        entity = entityData.createEntity();
        entityData.setComponents(entity, new Owner((short) 4, (short) 4), new Health(1000, 1000),
                new Position(0, new Vector3f()), new CreatureAi(50, CreatureState.WORK, (short) 10));
    }

    @Test
    void workingCreatureFallsOverAndStopsWhatItWasDoing() {
        CreatureController controller = controller(EnumSet.noneOf(Creature.CreatureFlag.class));
        controller.getStateMachine().setInitialState(CreatureState.WORK);
        entityData.setComponent(entity, new Navigation(null, null, null));

        controller.knockDown();

        assertEquals(CreatureState.FALLEN, controller.getStateMachine().getCurrentState());
        assertNull(entityData.getComponent(entity, Navigation.class));
        CreatureAi creatureAi = entityData.getComponent(entity, CreatureAi.class);
        assertEquals(CreatureState.FALLEN, creatureAi.getCreatureState());
        assertEquals(NOW, creatureAi.stateStartTime, 0.0001);
    }

    @Test
    void creatureAlreadyDownStaysDownLonger() {
        CreatureController controller = controller(EnumSet.noneOf(Creature.CreatureFlag.class));
        controller.getStateMachine().setInitialState(CreatureState.FALLEN);
        entityData.setComponent(entity, new CreatureAi(50, CreatureState.FALLEN, (short) 10));

        controller.knockDown();

        assertEquals(CreatureState.FALLEN, controller.getStateMachine().getCurrentState());
        assertEquals(NOW, entityData.getComponent(entity, CreatureAi.class).stateStartTime, 0.0001);
    }

    @Test
    void hornedReaperStaysOnItsFeet() {
        CreatureController controller = controller(EnumSet.of(Creature.CreatureFlag.IS_HORNY));
        controller.getStateMachine().setInitialState(CreatureState.WORK);

        controller.knockDown();

        assertEquals(CreatureState.WORK, controller.getStateMachine().getCurrentState());
    }

    @Test
    void prisonerStaysInPrison() {
        CreatureController controller = controller(EnumSet.noneOf(Creature.CreatureFlag.class));
        controller.getStateMachine().setInitialState(CreatureState.IMPRISONED);
        entityData.setComponent(entity, new CreatureImprisoned(0, 0));

        controller.knockDown();

        assertEquals(CreatureState.IMPRISONED, controller.getStateMachine().getCurrentState());
    }

    @Test
    void possessedCreatureIsNotKnockedDown() {
        CreatureController controller = controller(EnumSet.noneOf(Creature.CreatureFlag.class));
        controller.getStateMachine().setInitialState(CreatureState.IDLE);

        // A possessed creature has no AI of its own
        entityData.removeComponent(entity, CreatureAi.class);

        controller.knockDown();

        assertEquals(CreatureState.IDLE, controller.getStateMachine().getCurrentState());
        assertNull(entityData.getComponent(entity, CreatureAi.class));
    }

    private CreatureController controller(EnumSet<Creature.CreatureFlag> flags) {
        Creature creature = ThunderboltFixtures.creature((short) 10, flags, EnumSet.noneOf(Creature.CreatureFlag3.class));
        ILevelInfo levelInfo = (ILevelInfo) Proxy.newProxyInstance(ILevelInfo.class.getClassLoader(),
                new Class<?>[]{ILevelInfo.class}, (proxy, method, args) -> null);
        return new CreatureController(entity, entityData, creature, null, null, () -> NOW, null, null, null, null, levelInfo, null, null);
    }

}
