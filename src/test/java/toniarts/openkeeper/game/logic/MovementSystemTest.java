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

import com.badlogic.gdx.math.Vector2;
import com.jme3.math.Transform;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityComponent;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureMotion;
import toniarts.openkeeper.game.component.CreatureViewState;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Mobile;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.SeparationPush;
import toniarts.openkeeper.game.data.Settings;
import toniarts.openkeeper.game.map.IMapInformation;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.view.animation.MotionHistory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MovementSystemTest {

    @Test void publishedSnapshotsRemainUnchangedWhileSteeringAndFalling() {
        EntityData data = new DefaultEntityData(); EntityId id = data.createEntity();
        Vector3f input = new Vector3f(10,3,23); Position retained = new Position(0,input);
        input.x = 99;
        assertEquals(10,retained.position.x);
        data.setComponents(id,retained,new Mobile(false,false,false,1),new toniarts.openkeeper.game.component.CreatureComponent(),
                new Navigation(new Point(20,23),null,List.of(new Vector2(10,23),new Vector2(20,23))));
        MovementSystem movement = new MovementSystem(data,map(false,false));
        for(int i=0;i<60;i++) movement.processTick(1f/60);
        assertEquals(new Vector3f(10,3,23),retained.position);
        Position beforeFall = data.getComponent(id,Position.class);
        data.setComponent(id,new toniarts.openkeeper.game.component.CreatureFall());
        new CreatureFallSystem(data).processTick(.1f);
        movement.processTick(.1f);
        assertEquals(3,beforeFall.position.y);
        assertEquals(2.8f,data.getComponent(id,Position.class).position.y,1e-6);
        assertEquals(beforeFall.position.x,data.getComponent(id,Position.class).position.x);
        assertNull(data.getComponent(id,Navigation.class));
        movement.stop();
    }

    @Test
    void movingCreatureTurnsAtItsAuthoredRateInsteadOfSnapping() throws Exception {
        EntityData entities = new DefaultEntityData();
        var definition = new toniarts.openkeeper.tools.convert.map.Creature();
        var field = definition.getClass().getDeclaredField("turnSpeed");
        field.setAccessible(true);
        field.setFloat(definition, .125f); // Synthetic 90 degrees per second.
        var level = (toniarts.openkeeper.tools.convert.map.IKwdFile) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{toniarts.openkeeper.tools.convert.map.IKwdFile.class},
                (proxy, method, args) -> method.getName().equals("getCreature") ? definition : null);
        EntityId id = entities.createEntity();
        var creature = new toniarts.openkeeper.game.component.CreatureComponent();
        creature.creatureId = 7;
        entities.setComponents(id, creature, new Position(0, new Vector3f(10, 0, 10)),
                new Mobile(false, false, false, 1), new Navigation(new Point(18, 10), null,
                        List.of(new Vector2(10, 10), new Vector2(18, 10))));
        MovementSystem movement = new MovementSystem(entities, map(false, false), level);
        float previous = 0;
        for (int frame = 0; frame < 30; frame++) {
            movement.processTick(1f / 60);
            float rotation = entities.getComponent(id, Position.class).rotation;
            assertTrue(Math.abs(rotation - previous) <= Math.PI / 120 + 1e-5,
                    "Facing snapped beyond the authored turn limit");
            previous = rotation;
        }
        assertTrue(previous > .70f && previous < .80f, "Expected about 45 degrees after half a second");
        for (int frame = 0; frame < 60; frame++) {
            movement.processTick(1f / 60);
        }
        assertEquals((float) Math.PI / 2, entities.getComponent(id, Position.class).rotation, 1e-4f);
        movement.stop();
    }

    @Test
    void anUnfinishedStationaryWalkKeepsItsNavigation() {
        EntityData data = new DefaultEntityData();
        EntityId id = data.createEntity();
        data.setComponents(id, new Position(0, new Vector3f(3, 0, 3)), new Mobile(false, false, false, 0),
                new Navigation(new Point(8, 3), null, List.of(new Vector2(3, 3), new Vector2(8, 3))));
        MovementSystem movement = new MovementSystem(data, map(false, false));
        try {
            for (int tick = 0; tick < 10; tick++) {
                movement.processTick(.25f);
            }
            assertTrue(data.getComponent(id, Navigation.class) != null, "a stalled walk is not arrival");
        } finally {
            movement.stop();
        }
    }

    @ParameterizedTest
    @ValueSource(floats = {.25f, .125f})
    void onePointFacingRequestCompletesAwayFromThePathPoint(float tick) {
        EntityData data = new DefaultEntityData();
        EntityId id = data.createEntity();
        Vector3f start = new Vector3f(3.3f, 0, 3);
        data.setComponents(id, new Position(1, start.clone()), new Mobile(false, false, false, 1),
                new Navigation(new Point(3, 3), new Point(3, 4), List.of(new Vector2(3, 3))));
        MovementSystem movement = new MovementSystem(data, map(false, false));
        try {
            for (int i = 0; i < 100 && data.getComponent(id, Navigation.class) != null; i++) {
                movement.processTick(tick);
            }
            assertNull(data.getComponent(id, Navigation.class), "orientation-only request never completed");
            assertEquals(start, data.getComponent(id, Position.class).position, "facing must not move the creature");
            assertTrue(Math.cos(data.getComponent(id, Position.class).rotation) > .98);
        } finally {
            movement.stop();
        }
    }

    @Test
    void residualSpinSettlesAtTheSameRateOnLogicTicks() {
        EntityData data = new DefaultEntityData();
        EntityId id = data.createEntity();
        data.setComponents(id, new Position(0, new Vector3f(3, 0, 3)), new Mobile(false, false, false, 1),
                new Navigation(new Point(3, 3), new Point(3, 4), new ArrayList<>()));
        MovementSystem movement = new MovementSystem(data, map(false, false));
        try {
            var field = MovementSystem.class.getDeclaredField("steerableEntitiesByEntityId");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            var steerables = (java.util.Map<EntityId, toniarts.openkeeper.game.navigation.steering.ISteerableEntity>) field.get(movement);
            // Synthetic residual yaw after arriving, already facing the target.
            steerables.get(id).setAngularVelocity(.1f);
            for (int tick = 0; tick < 4 && data.getComponent(id, Navigation.class) != null; tick++) {
                movement.processTick(.25f);
            }
            assertNull(data.getComponent(id, Navigation.class), "residual spin kept an arrived route alive");
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        } finally {
            movement.stop();
        }
    }

    /**
     * A creature turning on the spot to face a heart pillar is sent to a spot
     * inside its own tile, as an idle walk does (DKII.EXE 0x4808E0). The new
     * walk has the same target tile, but it needs path following that the
     * turning had none of.
     */
    @Test
    void aWalkReplacingATurnOnTheSameTileFollowsItsPath() throws Exception {
        EntityData entities = new DefaultEntityData();
        var definition = new toniarts.openkeeper.tools.convert.map.Creature();
        var level = (toniarts.openkeeper.tools.convert.map.IKwdFile) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{toniarts.openkeeper.tools.convert.map.IKwdFile.class},
                (proxy, method, args) -> method.getName().equals("getCreature") ? definition : null);
        EntityId id = entities.createEntity();
        entities.setComponents(id, new toniarts.openkeeper.game.component.CreatureComponent(),
                new Position((float) Math.PI, new Vector3f(3, 0, 3)), new Mobile(false, false, false, 1),
                new Navigation(new Point(3, 3), new Point(3, 4), new ArrayList<>()));
        MovementSystem movement = new MovementSystem(entities, map(false, false), level);
        try {
            movement.processTick(.25f);
            entities.setComponent(id, new Navigation(new Point(3, 3), null, List.of(new Vector2(3, 3), new Vector2(3.25f, 3.25f))));
            for (int tick = 0; tick < 20 && entities.getComponent(id, Navigation.class) != null; tick++) {
                movement.processTick(.25f);
            }
            assertNull(entities.getComponent(id, Navigation.class), "the walk never finished");
            Vector3f position = entities.getComponent(id, Position.class).position;
            assertTrue(position.x > 3.15f && position.z > 3.15f, "it never walked to the spot: " + position);
        } finally {
            movement.stop();
        }
    }

    /**
     * A creature standing on 15,23 turns on the spot to face 16,25. Facing is
     * (sin r, cos r) on x, z for the Position rotation r.
     */
    @ParameterizedTest
    @ValueSource(floats = {0f, -0.46f, 1f, -1f, 3f, -3f})
    void turnsOnTheSpotToFaceTheTarget(float rotation) {
        EntityData entityData = new DefaultEntityData();
        EntityId creature = entityData.createEntity();
        entityData.setComponents(creature, new Position(rotation, new Vector3f(15, 0, 23)), new Mobile(false, false, false, 1),
                new Navigation(new Point(15, 23), new Point(16, 25), new ArrayList<>()));
        MovementSystem movementSystem = new MovementSystem(entityData, map(false, false));

        for (int frame = 0; frame < 180 && entityData.getComponent(creature, Navigation.class) != null; frame++) {
            movementSystem.processTick(1 / 60f);
        }

        assertNull(entityData.getComponent(creature, Navigation.class), "still turning after 3 seconds");
        float facing = entityData.getComponent(creature, Position.class).rotation;
        double cos = (Math.sin(facing) * 1 + Math.cos(facing) * 2) / Math.sqrt(5);
        assertTrue(cos > Math.cos(0.2), "facing " + Math.toDegrees(Math.acos(cos)) + " degrees away");
    }

    /**
     * Walking through water halves the speed and lava speeds it up by half
     * (DKII.EXE 0x488930), flying is not slowed
     */
    @Test
    void waterHalvesAndLavaRaisesTheWalkingSpeed() {
        assertEquals(1f, walkedInASecond(map(false, false), false), 0.1f);
        assertEquals(0.5f, walkedInASecond(map(true, false), false), 0.1f);
        assertEquals(1.5f, walkedInASecond(map(false, true), false), 0.1f);
        assertEquals(1f, walkedInASecond(map(true, false), true), 0.1f);
    }

    /**
     * A creature other than a worker walks at half speed below a fifth of its
     * health (DKII.EXE 0x488930), on top of the water and lava change
     */
    @Test
    void lowHealthHalvesTheWalkingSpeed() {
        assertEquals(1f, walkedInASecond(map(false, false), false, new Health(20, 100), creature(false)), 0.1f);
        assertEquals(0.5f, walkedInASecond(map(false, false), false, new Health(19, 100), creature(false)), 0.1f);
        assertEquals(0.25f, walkedInASecond(map(true, false), false, new Health(19, 100), creature(false)), 0.05f);
        assertEquals(1f, walkedInASecond(map(false, false), false, new Health(19, 100), creature(true)), 0.1f);
    }

    private static CreatureComponent creature(boolean worker) {
        CreatureComponent creature = new CreatureComponent();
        creature.worker = worker;
        return creature;
    }

    /**
     * Distance walked east at 1 tile a second, in the second after getting up
     * to speed, on logic ticks of a quarter of a second
     */
    private static float walkedInASecond(IMapInformation map, boolean canFly, EntityComponent... components) {
        EntityData entityData = new DefaultEntityData();
        EntityId creature = entityData.createEntity();
        entityData.setComponents(creature, new Position(0, new Vector3f(10, 0, 23)), new Mobile(canFly, true, true, 1),
                new Navigation(new Point(20, 23), null, List.of(new Vector2(10, 23), new Vector2(20, 23))));
        entityData.setComponents(creature, components);
        MovementSystem movementSystem = new MovementSystem(entityData, map);
        for (int tick = 0; tick < 8; tick++) {
            movementSystem.processTick(1 / 4f);
        }
        float start = entityData.getComponent(creature, Position.class).position.x;
        for (int tick = 0; tick < 4; tick++) {
            movementSystem.processTick(1 / 4f);
        }
        return entityData.getComponent(creature, Position.class).position.x - start;
    }

    private static IMapInformation map(boolean water, boolean lava) {
        return (IMapInformation) Proxy.newProxyInstance(IMapInformation.class.getClassLoader(), new Class<?>[]{IMapInformation.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "isWater" -> water;
                    case "isLava" -> lava;
                    default -> throw new AssertionError(method.getName());
                });
    }

    /**
     * The other threads still read the Position the creature had before the
     * step, walking on must not move that one under them.
     */
    @Test
    void walkingLeavesThePreviousPositionAlone() {
        EntityData entityData = new DefaultEntityData();
        EntityId creature = entityData.createEntity();
        entityData.setComponents(creature, new Position(0, new Vector3f(15, 1, 23)), new Mobile(false, false, false, 1),
                new Navigation(new Point(17, 23), null, List.of(new Vector2(15, 23), new Vector2(17, 23))));
        MovementSystem movementSystem = new MovementSystem(entityData, map(false, false));
        Position previous = entityData.getComponent(creature, Position.class);

        for (int tick = 0; tick < 2; tick++) {
            movementSystem.processTick(1 / 4f);
        }

        assertEquals(new Vector3f(15, 1, 23), previous.position);
        Vector3f position = entityData.getComponent(creature, Position.class).position;
        assertTrue(position.x > 15, "not walking");
        assertEquals(1, position.y);
    }

    /**
     * The creatures move on the logic tick, four times a second. The view
     * shows a walking creature between the ticks only from the published
     * motion, so the motion is published unless it is switched off. It is
     * published after the move, the view then lags a tick less behind.
     */
    @Test
    void aWalkOnTheLogicTickIsShownBetweenTheTicks() {
        EntityData entityData = new DefaultEntityData();
        EntityId creature = entityData.createEntity();
        entityData.setComponents(creature, new Position(0, new Vector3f(15, 1, 23)), new Mobile(false, false, false, 1),
                new CreatureViewState((short) 1, 0, Creature.AnimationType.STAND_STILL),
                new Navigation(new Point(19, 23), null, List.of(new Vector2(15, 23), new Vector2(19, 23))));
        MovementSystem movementSystem = new MovementSystem(entityData, map(false, false));
        CreatureViewSystem creatureViewSystem = new CreatureViewSystem(entityData, null, null,
                (Boolean) Settings.Setting.ANIMATION_INTERPOLATION.getDefaultValue());
        IGameLogicUpdatable motionPublication = creatureViewSystem.getMotionPublication();
        MotionHistory history = new MotionHistory(8);
        List<CreatureMotion> published = new ArrayList<>();
        try {

            // In the order of the logic loop
            for (int tick = 0; tick < 4; tick++) {
                creatureViewSystem.processTick(1 / 4f);
                movementSystem.processTick(1 / 4f);
                motionPublication.processTick(1 / 4f);
                CreatureMotion motion = entityData.getComponent(creature, CreatureMotion.class);
                assertNotNull(motion, "no motion published for the view");
                assertEquals(entityData.getComponent(creature, Position.class).position.x, motion.x, "not where the tick moved it");
                history.add(motion);
                published.add(motion);
            }
        } finally {
            creatureViewSystem.stop();
            movementSystem.stop();
        }

        CreatureMotion before = published.get(2);
        CreatureMotion after = published.get(3);
        assertTrue(after.x > before.x, "not walking");
        Transform shown = new Transform();
        assertTrue(history.sample((before.time + after.time) / 2, shown));
        float x = shown.getTranslation().x;
        assertTrue(x > before.x && x < after.x, "shown at " + x + ", not between the ticks at " + before.x + " and " + after.x);
    }

    /**
     * A push moves a standing creature on like walking does: the view keeps
     * interpolating it, and the position it had before is left alone.
     */
    @Test
    void aPushedCreatureMovesOnInTheSameMotion() {
        EntityData entityData = new DefaultEntityData();
        EntityId creature = entityData.createEntity();
        entityData.setComponents(creature, new CreatureComponent(), new Position(0, new Vector3f(15, 1, 23)),
                new Mobile(false, false, false, 1), new SeparationPush(new Vector2f(0.1f, 0)));
        MovementSystem movementSystem = new MovementSystem(entityData, map(false, false));
        Position previous = entityData.getComponent(creature, Position.class);
        try {
            movementSystem.processTick(1 / 4f);
        } finally {
            movementSystem.stop();
        }

        Position pushed = entityData.getComponent(creature, Position.class);
        assertTrue(pushed.position.x > 15, "not pushed");
        assertEquals(new Vector3f(15, 1, 23), previous.position);
        assertEquals(previous.motionEpoch, pushed.motionEpoch);
    }
}
