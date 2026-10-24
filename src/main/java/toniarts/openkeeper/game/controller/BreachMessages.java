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

import java.util.HashMap;
import java.util.Map;
import toniarts.openkeeper.game.state.session.PlayerService;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Tells the keeper that his dungeon has been breached, i.e. another player has
 * brought down one of his walls. DKII.EXE queues message 0xc to the owner of
 * the wall when it falls (0x453100). The message carries the tile, so the
 * keeper can zoom there from the message box, and the queue (0x4C4600) does
 * not repeat it while the last one is still shown (table at 0x6A0B00)
 */
public final class BreachMessages {

    /**
     * "Your Dungeon has been breached!"
     */
    static final int BREACHED_SPEECH_ID = 1150;

    /**
     * How long, in seconds, the message stays and is not repeated
     */
    private static final double DURATION = 50;

    private final PlayerService playerService;
    private final IGameTimer gameTimer;
    private final Map<Short, Double> shownAt = new HashMap<>();

    /**
     * Create the breach messages
     *
     * @param playerService the players to tell, may be {@code null} when there
     * is nobody to tell
     * @param gameTimer the game time
     */
    public BreachMessages(PlayerService playerService, IGameTimer gameTimer) {
        this.playerService = playerService;
        this.gameTimer = gameTimer;
    }

    /**
     * A wall of the keeper was brought down by another player
     *
     * @param ownerId the owner of the wall
     * @param tile the tile of the wall
     */
    public void dungeonBreached(short ownerId, Point tile) {
        if (playerService == null) {
            return;
        }
        double now = gameTimer.getGameTime();
        Double last = shownAt.get(ownerId);
        if (last != null && now - last < DURATION) {
            return;
        }
        shownAt.put(ownerId, now);
        playerService.showMentorMessage(BREACHED_SPEECH_ID, WorldUtils.pointToVector3f(tile), ownerId);
    }

}
