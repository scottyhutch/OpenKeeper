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

import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.AttackTarget;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.FetchPortalGem;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.InHand;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.PortalGem;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.SummonedReaper;
import toniarts.openkeeper.game.map.IMapInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.PortalGemFixtures;
import toniarts.openkeeper.tools.convert.map.SummonReaperFixtures;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortalGemReaperTest {

    private static final short PLAYER_ID = 3;
    private static final short HERO_ID = 1;
    private static final short REAPER_ID = 27;
    private static final short GEM_ID = 28;
    private static final Point GEM_TILE = new Point(20, 20);
    private static final double NOW = 300;

    private final EntityData entityData = new DefaultEntityData();
    private final List<Object[]> spawns = new ArrayList<>();
    private final List<Object[]> loadedObjects = new ArrayList<>();
    private final Set<Point> unreachableTiles = new HashSet<>();
    private final Set<Point> solidTiles = new HashSet<>();
    private final List<Point> searchedTiles = new ArrayList<>();
    private final Deque<Integer> rolls = new ArrayDeque<>();
    private final List<Creature> creatures = List.of(
            SummonReaperFixtures.creature((short) 1, Creature.CreatureFlag.IS_WORKER),
            SummonReaperFixtures.creature(REAPER_ID, Creature.CreatureFlag.IS_HORNY));
    private final List<GameObject> objects = List.of(PortalGemFixtures.plainObject((short) 1),
            PortalGemFixtures.portalGem(GEM_ID));

    @Test
    void reaperAppearsAtLeastFiveTilesFromTheGemAndGoesForIt() {
        EntityId gem = addGem(GEM_TILE);

        // The first spot is right on the gem, too close
        roll(0, 0);
        roll(5, 0);
        EntityId reaper = gemReaper().callForGem(PLAYER_ID);

        assertEquals(1, spawns.size());
        Object[] spawn = spawns.get(0);
        assertEquals(REAPER_ID, spawn[0]);
        assertEquals(PLAYER_ID, spawn[1]);
        assertEquals(6, spawn[2]);
        assertEquals(WorldUtils.pointToVector2f(25, 20), spawn[3]);
        assertEquals(gem, entityData.getComponent(reaper, FetchPortalGem.class).gem);
        assertNotNull(entityData.getComponent(reaper, SummonedReaper.class));
    }

    @Test
    void reaperDoesNotAppearWhereHeCanNotReachTheGemOrOnACreature() {
        addGem(GEM_TILE);
        unreachableTiles.add(new Point(25, 20));
        addCreature((short) 1, HERO_ID, new Point(20, 26));

        roll(5, 0);
        roll(0, 6);
        roll(-6, 1);
        gemReaper().callForGem(PLAYER_ID);

        assertEquals(WorldUtils.pointToVector2f(14, 21), spawns.get(0)[3]);
    }

    @Test
    void keepersOwnReaperGoesForTheGem() {
        EntityId gem = addGem(GEM_TILE);
        EntityId ownReaper = addCreature(REAPER_ID, PLAYER_ID, new Point(3, 3));
        entityData.setComponent(ownReaper, new SummonedReaper(0, 0, 2000));
        entityData.setComponent(ownReaper, new AttackTarget(entityData.createEntity()));

        EntityId reaper = gemReaper().callForGem(PLAYER_ID);

        assertEquals(ownReaper, reaper);
        assertTrue(spawns.isEmpty());
        assertEquals(gem, entityData.getComponent(reaper, FetchPortalGem.class).gem);
        assertNull(entityData.getComponent(reaper, AttackTarget.class));
    }

    @Test
    void reaperCanAppearEightTilesAwayOnEachAxis() {
        addGem(GEM_TILE);

        // Rolls are the raw nextInt values, 16 is the top of the range
        rolls.add(16);
        rolls.add(16);
        gemReaper().callForGem(PLAYER_ID);

        assertEquals(WorldUtils.pointToVector2f(28, 28), spawns.get(0)[3]);
    }

    @Test
    void solidTilesAreNotSearchedAsEntrances() {
        addGem(GEM_TILE);
        Point solid = new Point(GEM_TILE.x + 5, GEM_TILE.y);
        solidTiles.add(solid);

        roll(5, 0);
        roll(0, 5);
        gemReaper().callForGem(PLAYER_ID);

        assertTrue(!searchedTiles.contains(solid));
        assertEquals(WorldUtils.pointToVector2f(20, 25), spawns.get(0)[3]);
    }

    @Test
    void ownReaperInHandOrWalledOffIsNotSentForTheGem() {
        addGem(GEM_TILE);
        EntityId held = addCreature(REAPER_ID, PLAYER_ID, new Point(3, 3));
        entityData.setComponent(held, new InHand());

        roll(5, 0);
        EntityId reaper = gemReaper().callForGem(PLAYER_ID);

        assertTrue(!reaper.equals(held));
        assertEquals(1, spawns.size());
        assertNull(entityData.getComponent(held, FetchPortalGem.class));
    }

    @Test
    void reaperOfTheMapGoesForTheGemToo() {
        addGem(GEM_TILE);
        EntityId mapReaper = addCreature(REAPER_ID, PLAYER_ID, new Point(3, 3));

        assertEquals(mapReaper, gemReaper().callForGem(PLAYER_ID));
        assertNotNull(entityData.getComponent(mapReaper, SummonedReaper.class));
    }

    @Test
    void gemStillCarriedIsDroppedWhereTheCarrierIs() {
        EntityId lord = addCreature((short) 1, HERO_ID, new Point(7, 9));
        entityData.setComponent(lord, new PortalGem());

        roll(5, 0);
        EntityId reaper = gemReaper().callForGem(PLAYER_ID);

        assertEquals(1, loadedObjects.size());
        assertEquals(GEM_ID, loadedObjects.get(0)[0]);
        assertEquals(HERO_ID, loadedObjects.get(0)[1]);
        assertEquals(WorldUtils.pointToVector3f(7, 9), loadedObjects.get(0)[2]);
        assertNull(entityData.getComponent(lord, PortalGem.class));
        assertNotNull(entityData.getComponent(reaper, FetchPortalGem.class));
    }

    @Test
    void reaperIsSentOnlyOnceWhenCalledOnEveryTick() {
        addGem(GEM_TILE);

        EntityId reaper = gemReaper().callForGem(PLAYER_ID);

        assertNotNull(reaper);
        assertNull(gemReaper().callForGem(PLAYER_ID));
        assertEquals(1, spawns.size());
    }

    @Test
    void nothingHappensWithoutPortalGem() {
        assertNull(gemReaper().callForGem(PLAYER_ID));
        assertTrue(spawns.isEmpty());
    }

    private void roll(int dx, int dy) {
        rolls.add(dx + 8);
        rolls.add(dy + 8);
    }

    private EntityId addGem(Point tile) {
        EntityId gem = entityData.createEntity();
        entityData.setComponent(gem, new ObjectComponent(GEM_ID, null));
        entityData.setComponent(gem, new Owner(HERO_ID, HERO_ID));
        entityData.setComponent(gem, new Position(0, WorldUtils.pointToVector3f(tile)));
        return gem;
    }

    private EntityId addCreature(short creatureId, short ownerId, Point tile) {
        EntityId entityId = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = creatureId;
        entityData.setComponent(entityId, creatureComponent);
        entityData.setComponent(entityId, new Owner(ownerId, ownerId));
        entityData.setComponent(entityId, new Position(0, WorldUtils.pointToVector3f(tile)));
        entityData.setComponent(entityId, new Health(100, 100));
        return entityId;
    }

    private PortalGemReaper gemReaper() {
        return new PortalGemReaper(kwdFile(), entityData, () -> NOW, creaturesController(), objectsController(),
                navigationService(), mapInformation(), new Random() {
            @Override
            public int nextInt(int bound) {
                return rolls.isEmpty() ? 8 : rolls.poll();
            }
        });
    }

    private IKwdFile kwdFile() {
        return (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getCreatureList" -> creatures;
                    case "getCreature" -> creatures.stream().filter(c -> c.getCreatureId() == (short) args[0]).findFirst().orElse(null);
                    case "getObjectList" -> objects;
                    case "getGameLevel" -> SummonReaperFixtures.gameLevel((short) 6);
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private ICreaturesController creaturesController() {
        return (ICreaturesController) Proxy.newProxyInstance(ICreaturesController.class.getClassLoader(),
                new Class<?>[]{ICreaturesController.class}, (proxy, method, args) -> {
                    if (!method.getName().equals("spawnCreature") || args.length != 5) {
                        throw new UnsupportedOperationException(method.getName());
                    }
                    spawns.add(args);
                    return entityData.createEntity();
                });
    }

    private IObjectsController objectsController() {
        return (IObjectsController) Proxy.newProxyInstance(IObjectsController.class.getClassLoader(),
                new Class<?>[]{IObjectsController.class}, (proxy, method, args) -> {
                    if (!method.getName().equals("loadObject") || !(args[2] instanceof Vector3f)) {
                        throw new UnsupportedOperationException(method.getName());
                    }
                    loadedObjects.add(args);
                    EntityId object = entityData.createEntity();
                    entityData.setComponent(object, new ObjectComponent((short) args[0], null));
                    entityData.setComponent(object, new Position(0, ((Vector3f) args[2]).clone()));
                    return object;
                });
    }

    private IMapInformation<?> mapInformation() {
        return (IMapInformation<?>) Proxy.newProxyInstance(IMapInformation.class.getClassLoader(),
                new Class<?>[]{IMapInformation.class}, (proxy, method, args) -> {
                    if (!method.getName().equals("isSolid")) {
                        throw new UnsupportedOperationException(method.getName());
                    }
                    return solidTiles.contains((Point) args[0]);
                });
    }

    private INavigationService navigationService() {
        return (INavigationService) Proxy.newProxyInstance(INavigationService.class.getClassLoader(),
                new Class<?>[]{INavigationService.class}, (proxy, method, args) -> {
                    if (!method.getName().equals("findPath")) {
                        throw new UnsupportedOperationException(method.getName());
                    }
                    searchedTiles.add((Point) args[1]);
                    if (unreachableTiles.contains((Point) args[1])) {
                        return null;
                    }
                    return new DefaultGraphPath<IMapTileInformation>();
                });
    }

}
