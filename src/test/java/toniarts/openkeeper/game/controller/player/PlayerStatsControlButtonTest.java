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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.player.PlayerStatsControl.PressedButton;
import toniarts.openkeeper.tools.convert.map.TriggerAction.MakeType;

/**
 * The HUD button presses a level script can wait for
 */
class PlayerStatsControlButtonTest {

    @Test
    void noButtonIsPressedAtTheStart() {
        assertTrue(new PlayerStatsControl(new toniarts.openkeeper.game.data.Keeper(false, (short) 0)).getButtonPresses().isEmpty());
    }

    @Test
    void countsThePressesOfEachButton() {
        PlayerStatsControl stats = new PlayerStatsControl(new toniarts.openkeeper.game.data.Keeper(false, (short) 0));

        stats.buttonPressed(MakeType.ROOM, (short) 2);
        stats.buttonPressed(MakeType.ROOM, (short) 2);
        stats.buttonPressed(MakeType.TRAP, (short) 2);

        assertEquals(2, stats.getButtonPresses().get(new PressedButton(MakeType.ROOM, (short) 2)));
        assertEquals(1, stats.getButtonPresses().get(new PressedButton(MakeType.TRAP, (short) 2)));
        assertEquals(2, stats.getButtonPresses().size());
    }
}
