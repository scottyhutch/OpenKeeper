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
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Possessed;
import toniarts.openkeeper.game.component.PossessedMovement;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.tools.convert.map.Player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A saved game may have been saved in possession. The possession goes on
 * when the game is loaded.
 */
class PossessedSystemTest {

    private final List<String> possessions = new ArrayList<>();
    private final List<EntityId> endedPossessions = new ArrayList<>();
    private double gameTime = 1000;

    private DefaultEntityData entityData;
    private EntityId creature;
    private PossessedSystem possessedSystem;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();

        // A creature that was possessed when the game was saved, 1000 seconds in
        creature = entityData.createEntity();
        entityData.setComponents(creature, new Owner(Player.KEEPER1_ID, Player.KEEPER1_ID), new Possessed(5, 1000),
                new PossessedMovement(new Vector2f(1, 0), 0, 1));

        IGameTimer gameTimer = proxy(IGameTimer.class, (proxy, method, args) -> switch (method.getName()) {
            case "getGameTime" ->
                gameTime;
            default ->
                throw new UnsupportedOperationException(method.getName());
        });
        IGameController gameController = proxy(IGameController.class, (proxy, method, args) -> switch (method.getName()) {
            case "getGameTimer" ->
                gameTimer;
            case "setPossession" -> {
                possessions.add(args[0] + " by " + args[1]);
                yield null;
            }
            default ->
                throw new UnsupportedOperationException(method.getName());
        });
        ICreaturesController creaturesController = proxy(ICreaturesController.class, (proxy, method, args) -> switch (method.getName()) {
            case "createController" ->
                proxy(ICreatureController.class, (creatureProxy, creatureMethod, creatureArgs) -> switch (creatureMethod.getName()) {
                    case "setPossession" -> {
                        endedPossessions.add((EntityId) args[0]);
                        yield null;
                    }
                    default ->
                        throw new UnsupportedOperationException(creatureMethod.getName());
                });
            default ->
                throw new UnsupportedOperationException(method.getName());
        });

        // No players, so no mana: the possession ends when the mana is checked
        possessedSystem = new PossessedSystem(List.of(), entityData, gameController, creaturesController);
    }

    @AfterEach
    void tearDown() {
        possessedSystem.stop();
        entityData.close();
    }

    @Test
    void thePlayerIsToldOfThePossessionWhenTheGameStarts() {
        assertTrue(possessions.isEmpty(), "The clients aren't listening before the game starts");

        possessedSystem.start();

        assertEquals(List.of(creature + " by " + Player.KEEPER1_ID), possessions);
        assertNull(entityData.getComponent(creature, PossessedMovement.class), "Nobody moves the creature yet");
    }

    @Test
    void theManaIsCheckedByTheGameTime() {
        possessedSystem.start();
        gameTime = 1000.5;
        possessedSystem.processTick(0.25f);

        assertTrue(endedPossessions.isEmpty());

        gameTime = 1001;
        possessedSystem.processTick(0.25f);

        assertEquals(List.of(creature), endedPossessions);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

}
