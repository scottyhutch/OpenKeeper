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
package toniarts.openkeeper.tools.convert.map;

/**
 * Builds the level data the mana tests need. Lives in the data classes' own
 * package because their setters are {@code protected} (parser-only).
 */
public final class ManaRulesFixtures {

    private ManaRulesFixtures() {
    }

    public static Variable.MiscVariable manaVariable(Variable.MiscVariable.MiscType type, int value) {
        Variable.MiscVariable variable = new Variable.MiscVariable();
        variable.setVariableId(type);
        variable.setValue(value);
        return variable;
    }

    public static Terrain manaTerrain(short terrainId, int manaGain, int maxManaGain) {
        Terrain terrain = new Terrain();
        terrain.setTerrainId(terrainId);
        terrain.setFlags(TerrainFixtures.flags(Terrain.TerrainFlag.OWNABLE));
        terrain.setManaGain(manaGain);
        terrain.setMaxManaGain(maxManaGain);
        return terrain;
    }

    public static KeeperSpell manaSpell(int manaCost) {
        KeeperSpell keeperSpell = new KeeperSpell();
        keeperSpell.setManaCost(manaCost);
        return keeperSpell;
    }

}
