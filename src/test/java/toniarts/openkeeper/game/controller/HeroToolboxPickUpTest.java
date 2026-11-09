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
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.InHand;
import toniarts.openkeeper.game.component.Interaction;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.TerrainFixtures;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * My Pet Dungeon's Hero Toolbox: any keeper takes its heroes in the hand, drops
 * them in the dungeon and puts them back on the Hero Lair
 */
class HeroToolboxPickUpTest {

    private static final short GOOD_PLAYER_ID = 1;
    private static final short KEEPER_ID = 3;
    private static final short PATH_TERRAIN_ID = 13;

    private final EntityData entityData = new DefaultEntityData();
    private final Terrain heroLair = TerrainFixtures.terrain(CreaturesController.HERO_LAIR_TERRAIN_ID,
            TerrainFixtures.flags(Terrain.TerrainFlag.OWNABLE), 0, 0, 0, CreaturesController.HERO_LAIR_TERRAIN_ID);
    private final Terrain path = TerrainFixtures.terrain(PATH_TERRAIN_ID,
            TerrainFixtures.flags(Terrain.TerrainFlag.OWNABLE), 0, 0, 0, PATH_TERRAIN_ID);

    @Test
    void anyKeeperPicksUpAToolboxHero() {
        assertTrue(GameWorldController.canPickUpEntity(hero(true), KEEPER_ID, entityData, null));
    }

    @Test
    void otherHeroesStayOutOfTheHand() {
        assertFalse(GameWorldController.canPickUpEntity(hero(false), KEEPER_ID, entityData, null));
    }

    @Test
    void aToolboxHeroGoesBackOnTheHeroLair() {
        EntityId tool = inHand(hero(true));
        EntityId hero = inHand(hero(false));

        assertTrue(GameWorldController.canDropEntity(tool, KEEPER_ID, entityData, tileOwnedBy(GOOD_PLAYER_ID), heroLair));
        assertFalse(GameWorldController.canDropEntity(hero, KEEPER_ID, entityData, tileOwnedBy(GOOD_PLAYER_ID), heroLair));
    }

    @Test
    void aToolboxHeroIsDroppedOnOwnLandOnly() {
        EntityId tool = inHand(hero(true));

        assertTrue(GameWorldController.canDropEntity(tool, KEEPER_ID, entityData, tileOwnedBy(KEEPER_ID), path));
        assertFalse(GameWorldController.canDropEntity(tool, KEEPER_ID, entityData, tileOwnedBy(GOOD_PLAYER_ID), path));
    }

    private EntityId hero(boolean tool) {
        EntityId entity = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.tool = tool;
        entityData.setComponents(entity, creatureComponent,
                new Owner(GOOD_PLAYER_ID, GOOD_PLAYER_ID),
                new Interaction(true, true, true, false, false),
                new Health(100, 100),
                new Position(0, new Vector3f(2, 0, 2)));
        return entity;
    }

    private EntityId inHand(EntityId entity) {
        entityData.setComponent(entity, new InHand());
        return entity;
    }

    private static IMapTileInformation tileOwnedBy(short playerId) {
        return (IMapTileInformation) Proxy.newProxyInstance(IMapTileInformation.class.getClassLoader(),
                new Class<?>[]{IMapTileInformation.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getOwnerId" -> playerId;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

}
