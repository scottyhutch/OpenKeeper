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

import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntitySet;
import com.simsilica.es.base.DefaultEntityData;
import com.simsilica.es.filter.FieldFilter;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.ManufactureProgress;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.player.PlayerManufactureControl;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.data.ResearchableType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ManufactureProgressSystemTest {

    private static final short KEEPER = 3;
    private static final short OTHER_KEEPER = 4;
    private static final short GOOD_PLAYER = 1;
    private static final short SENTRY = 7;
    private static final short BRACED_DOOR = 2;
    private static final int SENTRY_POINTS = 40;

    private EntityData entityData;
    private final Map<Short, ManufactureProgressSystem.Order> nextOrders = new HashMap<>();
    private final List<EntitySet> entitySets = new ArrayList<>();
    private PlayerManufactureControl keeperControl;
    private PlayerManufactureControl otherKeeperControl;
    private ManufactureProgressSystem system;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        keeperControl = new PlayerManufactureControl();
        otherKeeperControl = new PlayerManufactureControl();
        system = new ManufactureProgressSystem(entityData,
                List.of(player(KEEPER, keeperControl), player(OTHER_KEEPER, otherKeeperControl), player(GOOD_PLAYER, null)),
                nextOrders::get);
        system.start();
    }

    @AfterEach
    void tearDown() {
        entitySets.forEach(EntitySet::release);
        system.stop();
    }

    @Test
    void percentIsThePointsTimes100OverTheRequiredRoundedDown() {
        assertEquals(0, ManufactureProgressSystem.getPercentCompleted(0, SENTRY_POINTS));
        assertEquals(2, ManufactureProgressSystem.getPercentCompleted(1, SENTRY_POINTS));
        assertEquals(32, ManufactureProgressSystem.getPercentCompleted(13, SENTRY_POINTS));
        assertEquals(97, ManufactureProgressSystem.getPercentCompleted(39, SENTRY_POINTS));
    }

    @Test
    void percentNeverGoesOver100() {

        // The order changed to a cheaper item after the points were gathered
        assertEquals(100, ManufactureProgressSystem.getPercentCompleted(90, SENTRY_POINTS));
    }

    @Test
    void nothingIsShownWithoutAnItemOrAtZeroPercent() {
        assertNull(ManufactureProgressSystem.getProgress(null, 10));
        assertNull(ManufactureProgressSystem.getProgress(sentry(), 0));
    }

    @Test
    void theKeepersEntityShowsTheItemInProduction() {
        nextOrders.put(KEEPER, sentry());
        keeperControl.manufacture(10, SENTRY_POINTS);

        system.processTick(0.25f);

        ManufactureProgress progress = shownProgress(KEEPER);
        assertNotNull(progress);
        assertEquals(ResearchableType.TRAP, progress.type);
        assertEquals(SENTRY, progress.typeId);
        assertEquals(25, progress.percent);
        assertNull(shownProgress(OTHER_KEEPER));
    }

    @Test
    void progressFollowsTheWork() {
        nextOrders.put(KEEPER, sentry());
        keeperControl.manufacture(10, SENTRY_POINTS);
        system.processTick(0.25f);

        keeperControl.manufacture(20, SENTRY_POINTS);
        system.processTick(0.25f);

        assertEquals(75, shownProgress(KEEPER).percent);
    }

    @Test
    void theNextItemTakesOver() {
        nextOrders.put(KEEPER, sentry());
        keeperControl.manufacture(10, SENTRY_POINTS);
        system.processTick(0.25f);

        // The sentry's blueprint is sold, a door is next
        nextOrders.put(KEEPER, new ManufactureProgressSystem.Order(ResearchableType.DOOR, BRACED_DOOR, 100));
        system.processTick(0.25f);

        ManufactureProgress progress = shownProgress(KEEPER);
        assertEquals(ResearchableType.DOOR, progress.type);
        assertEquals(BRACED_DOOR, progress.typeId);
        assertEquals(10, progress.percent);
    }

    @Test
    void finishingTheItemClearsTheProgress() {
        nextOrders.put(KEEPER, sentry());
        keeperControl.manufacture(30, SENTRY_POINTS);
        system.processTick(0.25f);

        // The crate is made, the next sentry starts from nothing
        keeperControl.manufacture(10, SENTRY_POINTS);
        system.processTick(0.25f);

        assertNull(shownProgress(KEEPER));
    }

    @Test
    void noOrdersLeftClearsTheProgress() {
        nextOrders.put(KEEPER, sentry());
        keeperControl.manufacture(30, SENTRY_POINTS);
        system.processTick(0.25f);

        nextOrders.remove(KEEPER);
        system.processTick(0.25f);

        assertNull(shownProgress(KEEPER));
    }

    @Test
    void onlyKeepersThatManufactureGetAnEntity() {
        assertNotNull(progressEntity(KEEPER));
        assertNotNull(progressEntity(OTHER_KEEPER));
        assertNull(progressEntity(GOOD_PLAYER));
    }

    private ManufactureProgress shownProgress(short playerId) {
        EntitySet entities = entityData.getEntities(new FieldFilter(Owner.class, "ownerId", playerId),
                ManufactureProgress.class, Owner.class);
        entitySets.add(entities);
        for (Entity entity : entities) {
            return entity.get(ManufactureProgress.class);
        }
        return null;
    }

    private Entity progressEntity(short playerId) {
        EntitySet entities = entityData.getEntities(new FieldFilter(Owner.class, "ownerId", playerId), Owner.class);
        entitySets.add(entities);
        for (Entity entity : entities) {
            return entity;
        }
        return null;
    }

    private static ManufactureProgressSystem.Order sentry() {
        return new ManufactureProgressSystem.Order(ResearchableType.TRAP, SENTRY, SENTRY_POINTS);
    }

    private static IPlayerController player(short id, PlayerManufactureControl manufactureControl) {
        Keeper keeper = new Keeper(false, id);
        return (IPlayerController) Proxy.newProxyInstance(IPlayerController.class.getClassLoader(),
                new Class<?>[]{IPlayerController.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getKeeper" ->
                keeper;
            case "getManufactureControl" ->
                manufactureControl;
            default ->
                throw new UnsupportedOperationException(method.getName());
        });
    }

}
