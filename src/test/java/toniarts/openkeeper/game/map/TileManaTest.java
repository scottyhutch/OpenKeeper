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
package toniarts.openkeeper.game.map;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Mana;
import toniarts.openkeeper.game.component.MapTile;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static toniarts.openkeeper.tools.convert.map.ManaRulesFixtures.manaTerrain;

class TileManaTest {

    @Test
    void tileCarriesItsTerrainsManaGainAndMaximumManaGain() {
        EntityData entityData = new DefaultEntityData();
        EntityId entity = entityData.createEntity();

        MapTileController.setAttributesFromTerrain(entityData, entity, new MapTile(0, (short) 32, null, new Point(1, 1), 0), manaTerrain((short) 32, 100, 100));

        Mana mana = entityData.getComponent(entity, Mana.class);
        assertEquals(100, mana.manaGeneration);
        assertEquals(100, mana.maxManaGeneration);
    }

}
