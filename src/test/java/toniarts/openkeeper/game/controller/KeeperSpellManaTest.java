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

import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.data.Keeper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static toniarts.openkeeper.tools.convert.map.ManaRulesFixtures.manaSpell;

class KeeperSpellManaTest {

    @Test
    void spellCanBeCastWithAllTheManaTheKeeperHasDespiteUpkeep() {
        Keeper keeper = new Keeper(false, (short) 3);
        keeper.setMana(1000);
        keeper.setManaLoose(100);

        assertTrue(KeeperSpellCastValidator.checkPlayerMana(manaSpell(1000), keeper));
    }

    @Test
    void spellCostingMoreThanTheKeepersManaCantBeCast() {
        Keeper keeper = new Keeper(false, (short) 3);
        keeper.setMana(999);

        assertFalse(KeeperSpellCastValidator.checkPlayerMana(manaSpell(1000), keeper));
    }

}
