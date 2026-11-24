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

import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.AttackTarget;
import toniarts.openkeeper.game.component.ChaseBackOff;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureMeleeAttack;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.ShotFixtures;
import toniarts.openkeeper.tools.convert.map.Variable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A fighting creature drops a target it can neither reach nor walk to, like
 * DK II's fight state picks its target again whenever it is out of reach,
 * only from those it can walk to (0x45F030, 0x45CB00). A target that runs
 * away is kept and picked like any other (0x45F030, 0x45BDF0), until the
 * chase time of our side is up (0x470E30, 0x488930)
 */
class CreatureAttackTargetTest {

    private final EntityData entityData = new DefaultEntityData();
    private double time;

    @Test
    void aTargetWithNoWayToItIsDropped() {
        EntityId target = createCreature((short) 3, 14, 23);
        EntityId attacker = createCreature((short) 1, 13, 22);
        entityData.setComponent(attacker, new AttackTarget(target));

        // Diagonally past two walls: out of reach, and there is no path
        assertNull(createController(attacker, false).getAttackTarget());
        assertNull(entityData.getComponent(attacker, AttackTarget.class));
    }

    @Test
    void aTargetOutOfReachWithAWayToItIsKept() {
        EntityId target = createCreature((short) 3, 14, 23);
        EntityId attacker = createCreature((short) 1, 13, 22);
        entityData.setComponent(attacker, new AttackTarget(target));

        createController(attacker, true).getAttackTarget();

        assertEquals(target, entityData.getComponent(attacker, AttackTarget.class).entityId);
    }

    @Test
    void aTargetThatRunsAwayIsKept() {
        EntityId target = createCreature((short) 3, 14, 23);
        EntityId attacker = createCreature((short) 1, 13, 22);
        entityData.setComponent(target, new CreatureAi(0, CreatureState.FLEE, (short) 14));
        entityData.setComponent(attacker, new AttackTarget(target));

        createController(attacker, true).getAttackTarget();

        assertEquals(target, entityData.getComponent(attacker, AttackTarget.class).entityId);
    }

    @Test
    void anEnemyThatRunsAwayIsPicked() {
        EntityId target = createCreature((short) 3, 14, 23);
        EntityId attacker = createCreature((short) 1, 13, 22);
        entityData.setComponent(target, new CreatureAi(0, CreatureState.FLEE, (short) 14));

        createController(attacker, true, target).getAttackTarget();

        assertEquals(target, entityData.getComponent(attacker, AttackTarget.class).entityId);
    }

    @Test
    void aHeroGivesUpTheChaseAfterThreeSecondsAndLeavesThoseThatRunAwayAlone() {
        EntityId target = createCreature((short) 3, 14, 23);
        EntityId hero = createCreature((short) 1, 13, 22);
        entityData.setComponent(target, new CreatureAi(0, CreatureState.FLEE, (short) 14));
        entityData.setComponent(hero, new AttackTarget(target));
        CreatureController controller = createController(hero, true, target);

        controller.getAttackTarget();
        time = 2.9;
        assertNotNull(controller.getAttackTarget());

        time = 3;
        assertNull(controller.getAttackTarget());
        assertNull(controller.getAttackTarget());
        assertNotNull(entityData.getComponent(hero, ChaseBackOff.class));

        // 60 turns later at 4 turns a second
        time = 18;
        assertNotNull(controller.getAttackTarget());
    }

    private EntityId createCreature(short owner, float x, float y) {
        EntityId entity = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = 14;
        entityData.setComponents(entity, creatureComponent, new Owner(owner, owner), new Health(100, 100),
                new Position(0, new Vector3f(x, 0, y)), new CreatureMeleeAttack(0, 10, 1, 0.6f));
        return entity;
    }

    private CreatureController createController(EntityId entity, boolean pathExists, EntityId... sensed) {
        Creature creature = ShotFixtures.creature((short) 14, EnumSet.noneOf(Creature.CreatureFlag.class), EnumSet.noneOf(Creature.CreatureFlag3.class));
        INavigationService navigationService = stub(INavigationService.class,
                (name, args) -> "findPath".equals(name) && pathExists ? new DefaultGraphPath<>() : null);
        ICreaturesController creaturesController = stub(ICreaturesController.class, (name, args) -> switch (name) {
            case "isValidEntity" -> true;
            case "createController" -> stub(ICreatureController.class, (method, params) -> null);
            default -> null;
        });
        IKwdFile kwd = stub(IKwdFile.class, (name, args) -> "getGameLevel".equals(name) ? CreatureSleepFixtures.level(4) : null);
        ILevelInfo levelInfo = stub(ILevelInfo.class, (name, args) -> switch (name) {
            case "getPlayer" -> new Keeper(false, (Short) args[0]);
            case "getLevelData" -> kwd;
            default -> null;
        });
        Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> settings = Map.of(
                Variable.MiscVariable.MiscType.GOOD_CREATURES_CHASE_ENEMY_DURATION_SECONDS,
                CreatureSleepFixtures.variable(Variable.MiscVariable.MiscType.GOOD_CREATURES_CHASE_ENEMY_DURATION_SECONDS, 3),
                Variable.MiscVariable.MiscType.EVIL_CREATURES_CHASE_ENEMY_DURATION_SECONDS,
                CreatureSleepFixtures.variable(Variable.MiscVariable.MiscType.EVIL_CREATURES_CHASE_ENEMY_DURATION_SECONDS, 7));
        return new CreatureController(entity, entityData, creature, navigationService, null, () -> time, settings,
                creaturesController, stub(IEntityPositionLookup.class,
                        (name, args) -> "getSensedEntities".equals(name) ? Set.of(sensed) : null),
                null, levelInfo, null, null, null);
    }

    @FunctionalInterface
    private interface Answer {

        Object answer(String name, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, Answer answer) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            Object result = answer.answer(method.getName(), args);
            if (result == null && method.getReturnType() == boolean.class) {
                return false;
            }
            return result;
        });
    }

}
