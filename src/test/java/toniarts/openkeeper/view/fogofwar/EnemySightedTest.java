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
package toniarts.openkeeper.view.fogofwar;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.TerrainFixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * An enemy is sighted when the keeper's creatures first see it, not when it
 * is on the keeper's land already as the fog is seeded (the prisoners of a
 * loaded game)
 */
class EnemySightedTest {

    private static final short FLOOR_ID = 1;
    private static final int MAP_SIZE = 5;

    @Test
    void enemiesOnTheSeededLandAreNotSighted() throws ReflectiveOperationException {
        DefaultEntityData entityData = new DefaultEntityData();

        // The keeper's creature perceives the tiles round it, also the ones explored already
        Creature creature = CreatureSleepFixtures.sleeper(1);
        Field perceptionRange = Creature.Attributes.class.getDeclaredField("perceptionRange");
        perceptionRange.setAccessible(true);
        perceptionRange.set(creature.getAttributes(), 3f);
        Terrain floor = TerrainFixtures.terrain(FLOOR_ID, TerrainFixtures.flags(), 0, 0, 0, FLOOR_ID);
        IKwdFile kwdFile = proxy(IKwdFile.class, (proxy, method, args) -> switch (method.getName()) {
            case "getTerrain" ->
                floor;
            case "getCreature" ->
                creature;
            case "getThings" ->
                List.of();
            default ->
                throw new UnsupportedOperationException(method.getName());
        });
        List<EntityId> sighted = new ArrayList<>();
        FogOfWarController controller = new FogOfWarController(entityData, kwdFile, map(), Player.KEEPER1_ID,
                points -> {
                }, sighted::add);

        // The keeper owns the two left columns, the seeding explores the third too
        creature(entityData, Player.KEEPER1_ID, 2, 2);
        creature(entityData, Player.GOOD_PLAYER_ID, 1, 1);
        EntityId newEnemy = creature(entityData, Player.GOOD_PLAYER_ID, 3, 3);
        controller.seedLevelStart();
        controller.start();
        controller.update(1);

        assertEquals(List.of(newEnemy), sighted);
        entityData.close();
    }

    private static EntityId creature(DefaultEntityData entityData, short ownerId, int x, int y) {
        EntityId entityId = entityData.createEntity();
        entityData.setComponents(entityId, new CreatureComponent(), new Owner(ownerId, ownerId),
                new Position(0, new Vector3f(x + 0.5f, 0, y + 0.5f)));
        return entityId;
    }

    @SuppressWarnings("unchecked")
    private static IMapDataInformation<IMapTileInformation> map() {
        IMapTileInformation[][] tiles = new IMapTileInformation[MAP_SIZE][MAP_SIZE];
        for (int x = 0; x < MAP_SIZE; x++) {
            short ownerId = x < 2 ? Player.KEEPER1_ID : 0;
            for (int y = 0; y < MAP_SIZE; y++) {
                tiles[x][y] = proxy(IMapTileInformation.class, (proxy, method, args) -> switch (method.getName()) {
                    case "getTerrainId" ->
                        FLOOR_ID;
                    case "getOwnerId" ->
                        ownerId;
                    default ->
                        throw new UnsupportedOperationException(method.getName());
                });
            }
        }
        return proxy(IMapDataInformation.class, (proxy, method, args) -> {
            if (method.isDefault()) {
                return InvocationHandler.invokeDefault(proxy, method, args);
            }
            return switch (method.getName()) {
                case "getWidth", "getHeight" ->
                    MAP_SIZE;
                case "getTile" -> {
                    int x = (int) args[0];
                    int y = (int) args[1];
                    yield x >= 0 && y >= 0 && x < MAP_SIZE && y < MAP_SIZE ? tiles[x][y] : null;
                }
                default ->
                    throw new UnsupportedOperationException(method.getName());
            };
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

}
