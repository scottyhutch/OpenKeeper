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

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.state.session.PlayerService;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The keeper hears that his dungeon has been breached, with the place of the
 * breach. DKII.EXE queues message 0xc to the owner of the wall when it is
 * brought down (0x453100), and the queue (0x4C4600) doesn't repeat it while
 * the last one is shown, 50 seconds (table at 0x6A0B00)
 */
class BreachMessagesTest {

    private static final Point TILE = new Point(4, 5);
    private static final Point OTHER_TILE = new Point(9, 2);
    private static final short KEEPER = Player.KEEPER1_ID;
    private static final short OTHER_KEEPER = Player.KEEPER2_ID;

    private final List<String> messages = new ArrayList<>();
    private double gameTime = 100;
    private final PlayerService playerService = (PlayerService) Proxy.newProxyInstance(PlayerService.class.getClassLoader(),
            new Class<?>[]{PlayerService.class}, (proxy, method, args) -> {
                if (method.getName().equals("showMentorMessage")) {
                    messages.add(args[0] + "@" + args[1] + ">p" + args[2]);
                }
                return null;
            });
    private final IGameTimer gameTimer = (IGameTimer) Proxy.newProxyInstance(IGameTimer.class.getClassLoader(),
            new Class<?>[]{IGameTimer.class}, (proxy, method, args) -> method.getName().equals("getGameTime") ? gameTime : null);
    private final BreachMessages breachMessages = new BreachMessages(playerService, gameTimer);

    @Test
    void tellsTheOwnerWhereHisDungeonWasBreached() {
        breachMessages.dungeonBreached(KEEPER, TILE);

        // "Your Dungeon has been breached!"
        assertEquals(List.of(message(KEEPER, TILE)), messages);
    }

    @Test
    void anotherBreachIsToldOnceIn50Seconds() {
        breachMessages.dungeonBreached(KEEPER, TILE);
        gameTime += 49;
        breachMessages.dungeonBreached(KEEPER, OTHER_TILE);
        gameTime += 1;
        breachMessages.dungeonBreached(KEEPER, OTHER_TILE);

        assertEquals(List.of(message(KEEPER, TILE), message(KEEPER, OTHER_TILE)), messages);
    }

    @Test
    void eachKeeperIsToldOfHisOwnBreaches() {
        breachMessages.dungeonBreached(KEEPER, TILE);
        breachMessages.dungeonBreached(OTHER_KEEPER, OTHER_TILE);

        assertEquals(List.of(message(KEEPER, TILE), message(OTHER_KEEPER, OTHER_TILE)), messages);
    }

    @Test
    void nobodyToTellIsFine() {
        new BreachMessages(null, gameTimer).dungeonBreached(KEEPER, TILE);

        assertEquals(List.of(), messages);
    }

    private static String message(short playerId, Point tile) {
        return 1150 + "@" + WorldUtils.pointToVector3f(tile) + ">p" + playerId;
    }
}
