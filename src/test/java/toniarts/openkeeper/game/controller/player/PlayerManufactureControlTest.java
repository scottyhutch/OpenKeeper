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
package toniarts.openkeeper.game.controller.player;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Values from the shipped data: a Troll manufactures 60 points a second, a
 * Bile Demon 50, a Sentry Trap needs 1500
 */
class PlayerManufactureControlTest {

    private static final int TROLL = 60;
    private static final int BILE_DEMON = 50;
    private static final int SENTRY_TRAP = 1500;

    @Test
    void trollManufacturesASentryTrapIn25Seconds() {
        PlayerManufactureControl control = new PlayerManufactureControl();

        for (int second = 1; second < 25; second++) {
            assertFalse(control.manufacture(TROLL, SENTRY_TRAP), "Done after " + second + " seconds");
        }

        assertTrue(control.manufacture(TROLL, SENTRY_TRAP));
        assertEquals(0, control.getProgress());
    }

    @Test
    void allWorkersAddToTheSameItem() {
        PlayerManufactureControl control = new PlayerManufactureControl();

        // A Troll and a Bile Demon together, 110 points a second
        int seconds = 0;
        boolean done = false;
        while (!done) {
            seconds++;
            done = control.manufacture(TROLL, SENTRY_TRAP) | control.manufacture(BILE_DEMON, SENTRY_TRAP);
        }

        assertEquals(14, seconds);
    }

    @Test
    void laterItemsAreBuiltAgainstTheFirstItemsCost() {
        PlayerManufactureControl control = new PlayerManufactureControl();
        int cheapDoor = 600;

        // The Sentry Trap was ordered first. The door after it still needs the Sentry Trap's 1500 points
        // (0x506B50 compares with the first slot of the list, which stays put)
        manufacture(control, SENTRY_TRAP, 1500);
        for (int points = TROLL; points < SENTRY_TRAP; points += TROLL) {
            assertFalse(control.manufacture(TROLL, cheapDoor), "Done at " + points + " points");
        }
        assertTrue(control.manufacture(TROLL, cheapDoor));
    }

    @Test
    void sellingEveryOrderStartsTheListOver() {
        PlayerManufactureControl control = new PlayerManufactureControl();
        int cheapDoor = 600;
        manufacture(control, SENTRY_TRAP, 1500);
        control.manufacture(TROLL, SENTRY_TRAP);

        control.clearOrders();

        assertEquals(0, control.getProgress());
        manufacture(control, cheapDoor, 600);
    }

    private static void manufacture(PlayerManufactureControl control, int required, int expectedPoints) {
        for (int points = TROLL; points < expectedPoints; points += TROLL) {
            assertFalse(control.manufacture(TROLL, required), "Done at " + points + " points");
        }
        assertTrue(control.manufacture(TROLL, required));
    }

}
