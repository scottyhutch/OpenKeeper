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
package toniarts.openkeeper.game.task.objective;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.utils.Point;

/**
 * The thief is marked when he steals, the "creature steals" trigger reads it
 */
class StealGoldMarksThiefTest {

    @Test
    void theThiefIsMarkedAsHavingStolen() {
        AtomicInteger marked = new AtomicInteger();
        StealGold task = new StealGold(null, null, treasury(), new Point(6, 2), Player.KEEPER1_ID, Player.GOOD_PLAYER_ID, thief -> {
        });

        task.executeTask(thief(marked), 0);

        assertEquals(1, marked.get());
    }

    @Test
    void theThiefIsMarkedBeforeTheLootListenerHearsOfIt() {
        AtomicInteger markedWhenReported = new AtomicInteger(-1);
        AtomicInteger marked = new AtomicInteger();
        StealGold task = new StealGold(null, null, treasury(), new Point(6, 2), Player.KEEPER1_ID, Player.GOOD_PLAYER_ID,
                looter -> markedWhenReported.set(marked.get()));

        task.executeTask(thief(marked), 0);

        assertEquals(1, markedWhenReported.get());
    }

    private static IGameWorldController treasury() {
        return (IGameWorldController) Proxy.newProxyInstance(IGameWorldController.class.getClassLoader(),
                new Class<?>[]{IGameWorldController.class}, (proxy, method, args) -> method.getName().equals("substractGold") ? 0 : null);
    }

    private static ICreatureController thief(AtomicInteger marked) {
        return (ICreatureController) Proxy.newProxyInstance(ICreatureController.class.getClassLoader(),
                new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getMaxGold" ->
                7500;
            case "markStolen" -> {
                marked.incrementAndGet();
                yield null;
            }
            default ->
                method.getReturnType() == int.class ? (Object) 0 : method.getReturnType() == boolean.class ? (Object) false : null;
        });
    }
}
