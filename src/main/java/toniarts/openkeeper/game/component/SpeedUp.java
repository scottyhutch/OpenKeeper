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
package toniarts.openkeeper.game.component;

import com.simsilica.es.EntityComponent;

/**
 * A creature moves faster for a while, like the Imp after its haste. DK II
 * keeps this as the creature's speed change timer (0x48CAD0)
 */
public final class SpeedUp implements EntityComponent {

    /**
     * The speed as a percentage of the creature's own speed
     */
    public int percentage;
    public double endTime;

    public SpeedUp() {
        // For serialization
    }

    public SpeedUp(int percentage, double endTime) {
        this.percentage = percentage;
        this.endTime = endTime;
    }

}
