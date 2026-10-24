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
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Regeneration;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.RegenerationVariableFixtures;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Things on their own land regain health once every ticks per second plus one
 * ticks, like the Dungeon Heart does in DKII.EXE (25 health, 20 a second at
 * four ticks per second), not on every logic tick
 */
class HealthRegenerationTest {

    private static final short OWNER = 3;
    private static final int TICKS_PER_SECOND = 4;
    private static final int START_HEALTH = 1000;
    private static final int MAX_HEALTH = 100000;

    @Test
    void regainsOnceEveryTicksPerSecondPlusOneTicks() {
        assertEquals(List.of(5, 5, 5, 5, 5, 5), regenerationGaps(4));
        assertEquals(List.of(11, 11, 11, 11, 11, 11), regenerationGaps(10));
    }

    @Test
    void regainsTwentyHealthASecondAtFourTicksPerSecond() {
        EntityData entityData = new DefaultEntityData();
        HealthSystem system = healthSystem(entityData, OWNER);
        EntityId entity = entity(entityData);

        tick(system, 20 * TICKS_PER_SECOND, TICKS_PER_SECOND);

        assertEquals(20 * 20, health(entityData, entity) - START_HEALTH);
    }

    /**
     * Ticks between the regeneration steps, each step being the 25 health
     */
    private static List<Integer> regenerationGaps(int ticksPerSecond) {
        EntityData entityData = new DefaultEntityData();
        HealthSystem system = healthSystem(entityData, OWNER);
        EntityId entity = entity(entityData);

        List<Integer> gaps = new ArrayList<>();
        int previous = health(entityData, entity);
        int lastStepTick = -1;
        for (int tick = 0; tick < 12 * ticksPerSecond; tick++) {
            tick(system, 1, ticksPerSecond);
            int current = health(entityData, entity);
            if (current != previous) {
                assertEquals(25, current - previous);
                if (lastStepTick >= 0) {
                    gaps.add(tick - lastStepTick);
                }
                lastStepTick = tick;
            }
            previous = current;
        }
        return gaps.subList(0, 6);
    }

    @Test
    void doesNotRegainOnForeignLand() {
        EntityData entityData = new DefaultEntityData();
        HealthSystem system = healthSystem(entityData, (short) 4);
        EntityId entity = entity(entityData);

        tick(system, 10 * TICKS_PER_SECOND, TICKS_PER_SECOND);

        assertEquals(START_HEALTH, health(entityData, entity));
    }

    private static void tick(HealthSystem system, int ticks, int ticksPerSecond) {
        for (int i = 0; i < ticks; i++) {
            system.processTick(1.0f / ticksPerSecond);
        }
    }

    private static int health(EntityData entityData, EntityId entity) {
        return entityData.getComponent(entity, Health.class).health;
    }

    private static EntityId entity(EntityData entityData) {
        EntityId entity = entityData.createEntity();
        entityData.setComponents(entity, new Health(START_HEALTH, MAX_HEALTH), new Regeneration(25, null),
                new Owner(OWNER, OWNER));
        return entity;
    }

    private static HealthSystem healthSystem(EntityData entityData, short tileOwner) {
        IMapTileInformation tile = (IMapTileInformation) Proxy.newProxyInstance(IMapTileInformation.class.getClassLoader(),
                new Class<?>[]{IMapTileInformation.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getOwnerId" -> tileOwner;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        IEntityPositionLookup lookup = (IEntityPositionLookup) Proxy.newProxyInstance(IEntityPositionLookup.class.getClassLoader(),
                new Class<?>[]{IEntityPositionLookup.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getEntityLocation" -> tile;
                    default -> throw new UnsupportedOperationException(method.getName());
                });

        Map<MiscType, MiscVariable> settings = new EnumMap<>(MiscType.class);
        settings.put(MiscType.CREATURE_DYING_STATE_DURATION_SECONDS, variable(MiscType.CREATURE_DYING_STATE_DURATION_SECONDS, 60));
        settings.put(MiscType.PRISON_MODIFY_CREATURE_HEALTH_PER_SECOND, variable(MiscType.PRISON_MODIFY_CREATURE_HEALTH_PER_SECOND, -3));
        settings.put(MiscType.MODIFY_HEALTH_OF_CREATURE_IN_LAIR_PER_SECOND, variable(MiscType.MODIFY_HEALTH_OF_CREATURE_IN_LAIR_PER_SECOND, 100));
        ILevelInfo levelInfo = (ILevelInfo) Proxy.newProxyInstance(ILevelInfo.class.getClassLoader(),
                new Class<?>[]{ILevelInfo.class}, (proxy, method, args) -> null);
        return new HealthSystem(entityData, lookup, settings, null, levelInfo, List.of(), null);
    }

    private static MiscVariable variable(MiscType type, int value) {
        return RegenerationVariableFixtures.miscVariable(type, value);
    }

}
