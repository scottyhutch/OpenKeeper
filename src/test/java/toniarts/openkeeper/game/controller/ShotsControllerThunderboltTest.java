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
package toniarts.openkeeper.game.controller;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Damage;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Shot;
import toniarts.openkeeper.tools.convert.map.ThunderboltFixtures;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Thunderbolt keeper spell: a lightning strike on the target
 */
class ShotsControllerThunderboltTest {

    private static final short THUNDERBOLT_SHOT_ID = 21;
    private static final short CREATURE_LIGHTNING_SHOT_ID = 30;
    private static final short TROLL_ID = 10;
    private static final short KING_ID = 21;
    private static final short CASTER = 3;
    private static final short ENEMY = 4;
    private static final Point WATER_TILE = new Point(5, 5);

    private EntityData entityData;
    private ShotsController shotsController;
    private List<EntityId> knockedDown;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        knockedDown = new ArrayList<>();
        Map<Short, Shot> shots = Map.of(
                THUNDERBOLT_SHOT_ID, ThunderboltFixtures.lightningShot(THUNDERBOLT_SHOT_ID, "Thunderbolt"),
                CREATURE_LIGHTNING_SHOT_ID, ThunderboltFixtures.lightningShot(CREATURE_LIGHTNING_SHOT_ID, "Lightning Shot"));
        Map<Short, Creature> creatures = Map.of(
                TROLL_ID, ThunderboltFixtures.creature(TROLL_ID, EnumSet.noneOf(Creature.CreatureFlag.class), EnumSet.noneOf(Creature.CreatureFlag3.class)),
                KING_ID, ThunderboltFixtures.creature(KING_ID, EnumSet.noneOf(Creature.CreatureFlag.class), EnumSet.of(Creature.CreatureFlag3.IS_IMMUNE_TO_LIGHTNING)));
        IKwdFile kwdFile = proxy(IKwdFile.class, (method, args) -> switch (method) {
            case "getShotById" -> shots.get((short) args[0]);
            case "getCreature" -> creatures.get((short) args[0]);
            case "getKeeperSpells" -> List.of(ThunderboltFixtures.keeperSpell(THUNDERBOLT_SHOT_ID));
            default -> null;
        });
        IMapController mapController = proxy(IMapController.class, (method, args) -> switch (method) {
            case "isWater" -> WATER_TILE.equals(args[0]);
            default -> null;
        });
        ICreaturesController creaturesController = proxy(ICreaturesController.class, (method, args) -> switch (method) {
            case "isValidEntity" -> entityData.getComponent((EntityId) args[0], CreatureComponent.class) != null;
            case "createController" -> {
                EntityId entity = (EntityId) args[0];
                yield proxy(ICreatureController.class, (creatureMethod, creatureArgs) -> {
                    if (creatureMethod.equals("knockDown")) {
                        knockedDown.add(entity);
                    }
                    return null;
                });
            }
            default -> null;
        });
        shotsController = new ShotsController(kwdFile, entityData, null, () -> 100, null, mapController, null, null, creaturesController);
    }

    @Test
    void thunderboltDealsShotData1AsDamage() {
        EntityId troll = creature(TROLL_ID, new Point(2, 2));

        shotsController.createShot(THUNDERBOLT_SHOT_ID, 150, 0, CASTER, new Vector3f(), troll);

        assertEquals(150, entityData.getComponent(troll, Damage.class).damage);
    }

    @Test
    void creatureStandingInWaterTakesDoubleDamage() {
        EntityId troll = creature(TROLL_ID, WATER_TILE);

        shotsController.createShot(THUNDERBOLT_SHOT_ID, 200, 0, CASTER, new Vector3f(), troll);

        assertEquals(400, entityData.getComponent(troll, Damage.class).damage);
    }

    @Test
    void thunderboltKnocksTheCreatureDown() {
        EntityId troll = creature(TROLL_ID, new Point(2, 2));

        shotsController.createShot(THUNDERBOLT_SHOT_ID, 150, 0, CASTER, new Vector3f(), troll);

        assertEquals(List.of(troll), knockedDown);
    }

    @Test
    void creatureImmuneToLightningIsLeftAlone() {
        EntityId king = creature(KING_ID, new Point(2, 2));

        shotsController.createShot(THUNDERBOLT_SHOT_ID, 150, 0, CASTER, new Vector3f(), king);

        assertNull(entityData.getComponent(king, Damage.class));
        assertTrue(knockedDown.isEmpty());
    }

    @Test
    void thingThatIsNotACreatureTakesTheDamageButIsNotKnockedDown() {
        EntityId door = entityData.createEntity();
        entityData.setComponents(door, new Owner(ENEMY, ENEMY), new Health(1000, 1000), position(new Point(2, 2)));

        shotsController.createShot(THUNDERBOLT_SHOT_ID, 150, 0, CASTER, new Vector3f(), door);

        assertEquals(150, entityData.getComponent(door, Damage.class).damage);
        assertTrue(knockedDown.isEmpty());
    }

    @Test
    void nothingToStrikeWithoutATargetWithHealth() {
        EntityId deadBody = entityData.createEntity();
        entityData.setComponents(deadBody, position(new Point(2, 2)));

        shotsController.createShot(THUNDERBOLT_SHOT_ID, 150, 0, CASTER, new Vector3f(), deadBody);
        shotsController.createShot(THUNDERBOLT_SHOT_ID, 150, 0, CASTER, new Vector3f(), null);

        assertNull(entityData.getComponent(deadBody, Damage.class));
        assertTrue(knockedDown.isEmpty());
    }

    @Test
    void creatureLightningIsLeftToTheAttack() {
        EntityId troll = creature(TROLL_ID, new Point(2, 2));

        // The attacking creature deals the damage of its own spell
        shotsController.createShot(CREATURE_LIGHTNING_SHOT_ID, 300, 0, ENEMY, new Vector3f(), troll);

        assertNull(entityData.getComponent(troll, Damage.class));
        assertTrue(knockedDown.isEmpty());
    }

    private EntityId creature(short creatureId, Point tile) {
        EntityId entity = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = creatureId;
        entityData.setComponents(entity, creatureComponent, new Owner(ENEMY, ENEMY), new Health(1000, 1000), position(tile));
        return entity;
    }

    private static Position position(Point tile) {
        return new Position(0, new Vector3f(tile.x, 0, tile.y));
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Handler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> handler.invoke(method.getName(), args));
    }

    @FunctionalInterface
    private interface Handler {

        Object invoke(String method, Object[] args);
    }

}
