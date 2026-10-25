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
import toniarts.openkeeper.game.data.ResearchableType;

/**
 * The trap or door the keeper's workshops are manufacturing and how far along
 * it is, for the HUD. The entity also has the keeper as its {@link Owner}.
 * There is no component while nothing is being made.
 */
public final class ManufactureProgress implements EntityComponent {

    public ResearchableType type;
    public short typeId;

    /**
     * Percent completed, 1 to 100
     */
    public int percent;

    public ManufactureProgress() {
        // For serialization
    }

    public ManufactureProgress(ResearchableType type, short typeId, int percent) {
        this.type = type;
        this.typeId = typeId;
        this.percent = percent;
    }

}
