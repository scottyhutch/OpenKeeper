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
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureTortured;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.player.PlayerStatsControl;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A converted victim counts for the keeper whose torture chamber it is in
 */
class CreatureTorturingSystemTest {

    private static final short KEEPER_ID = 3;
    private static final short HERO_ID = 1;
    private static final Point CHAMBER_TILE = new Point(4, 4);

    private final EntityData entityData = new DefaultEntityData();
    private final Map<EntityId, Short> converted = new HashMap<>();
    private final PlayerStatsControl stats = new PlayerStatsControl(new Keeper());
    private final Map<Short, IPlayerController> players = Map.of(KEEPER_ID, (IPlayerController) Proxy.newProxyInstance(
            IPlayerController.class.getClassLoader(), new Class<?>[]{IPlayerController.class}, (proxy, method, args) -> switch (method.getName()) {
                case "getStatsControl" -> stats;
                default -> throw new UnsupportedOperationException(method.getName());
            }));
    private final CreatureTorturingSystem system = new CreatureTorturingSystem(entityData, creaturesController(), mapInformation(), players);

    @Test
    void conversionCountsOnceForTheTorturersKeeper() {
        EntityId victim = victim(HERO_ID);

        system.processTick(1f);

        assertEquals(Map.of(victim, KEEPER_ID), converted);
        assertEquals(1, stats.getCreaturesConverted());
    }

    @Test
    void ownCreatureIsNotConverted() {
        victim(KEEPER_ID);

        system.processTick(1f);

        assertEquals(0, converted.size());
        assertEquals(0, stats.getCreaturesConverted());
    }

    private EntityId victim(short ownerId) {
        EntityId victim = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.tortureTimeToConvert = 2;
        entityData.setComponent(victim, creatureComponent);
        entityData.setComponent(victim, new Owner(ownerId, ownerId));
        entityData.setComponent(victim, new Position(0, WorldUtils.pointToVector3f(CHAMBER_TILE)));
        entityData.setComponent(victim, new CreatureTortured(5, -1, 0));
        return victim;
    }

    private ICreaturesController creaturesController() {
        return (ICreaturesController) Proxy.newProxyInstance(ICreaturesController.class.getClassLoader(),
                new Class<?>[]{ICreaturesController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "createController" -> creature((EntityId) args[0]);
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private ICreatureController creature(EntityId entityId) {
        return (ICreatureController) Proxy.newProxyInstance(ICreatureController.class.getClassLoader(),
                new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "convertCreature" -> {
                        converted.put(entityId, (Short) args[0]);
                        yield null;
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    @SuppressWarnings("rawtypes")
    private IMapInformation mapInformation() {
        IMapTileInformation tile = (IMapTileInformation) Proxy.newProxyInstance(IMapTileInformation.class.getClassLoader(),
                new Class<?>[]{IMapTileInformation.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getOwnerId" -> KEEPER_ID;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        IMapDataInformation data = (IMapDataInformation) Proxy.newProxyInstance(IMapDataInformation.class.getClassLoader(),
                new Class<?>[]{IMapDataInformation.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getTile" -> tile;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        return (IMapInformation) Proxy.newProxyInstance(IMapInformation.class.getClassLoader(),
                new Class<?>[]{IMapInformation.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getMapData" -> data;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

}
