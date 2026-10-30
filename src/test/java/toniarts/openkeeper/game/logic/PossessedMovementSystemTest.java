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

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Possessed;
import toniarts.openkeeper.game.component.PossessedMovement;
import toniarts.openkeeper.game.component.SpeedUp;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A possessed creature moves as its keeper steers it
 *
 * @author OpenKeeper
 */
class PossessedMovementSystemTest {

    private EntityData entityData;
    private PossessedMovementSystem system;
    private EntityId creature;
    private double gameTime;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        gameTime = 10;

        // Walking along +z at a tile a second, on open ground
        creature = entityData.createEntity();
        entityData.setComponents(creature, new Possessed(0, 0), new Position(0, new Vector3f(10, 0, 2)),
                new PossessedMovement(new Vector2f(0, 1), 0, 0, 1));

        IMapTileInformation tile = stub(IMapTileInformation.class);
        IMapDataInformation<?> mapData = stub(IMapDataInformation.class, "getTile", tile);
        IMapController mapController = stub(IMapController.class, "getMapData", mapData);
        ICreatureController creatureController = stub(ICreatureController.class, "getCost", 1f);
        ICreaturesController creaturesController = stub(ICreaturesController.class, "createController", creatureController);
        IGameTimer gameTimer = (IGameTimer) Proxy.newProxyInstance(IGameTimer.class.getClassLoader(), new Class<?>[]{IGameTimer.class},
                (proxy, method, args) -> "getGameTime".equals(method.getName()) ? gameTime : null);
        system = new PossessedMovementSystem(entityData, mapController, null, creaturesController, gameTimer);
    }

    @Test
    void speedUpMovesTheCreatureFaster() {
        entityData.setComponent(creature, new SpeedUp(150, gameTime + 1));

        system.processTick(0.1f);

        assertEquals(2.15f, entityData.getComponent(creature, Position.class).position.z, 0.0001f);
    }

    @Test
    void speedUpThatHasRunOutDoesNothing() {
        entityData.setComponent(creature, new SpeedUp(150, gameTime));

        system.processTick(0.1f);

        assertEquals(2.1f, entityData.getComponent(creature, Position.class).position.z, 0.0001f);
    }

    private static <T> T stub(Class<T> type) {
        return stub(type, null, null);
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, String methodName, Object result) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> method.getName().equals(methodName) ? result : null);
    }

}
