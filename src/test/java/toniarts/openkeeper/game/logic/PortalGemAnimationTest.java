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
package toniarts.openkeeper.game.logic;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import toniarts.openkeeper.game.component.CreatureViewState;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.PortalGem;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.utils.Point;

class PortalGemAnimationTest {

    private final EntityData entityData = new DefaultEntityData();

    @Test
    void theReaperHoldingTheGemStopsWalking() {
        EntityId reaper = entityData.createEntity();
        entityData.setComponent(reaper, new Position(0, new Vector3f(5, 0, 5)));
        entityData.setComponent(reaper, new CreatureViewState((short) 27, 0, Creature.AnimationType.IDLE_1));
        entityData.setComponent(reaper, new Navigation(new Point(5, 5), null, null));
        CreatureViewSystem system = new CreatureViewSystem(entityData, null);
        system.processTick(0.25f);
        assertEquals(Creature.AnimationType.WALK, entityData.getComponent(reaper, CreatureViewState.class).state);

        // He reaches the gem, takes it and has nothing left to do
        entityData.removeComponent(reaper, Navigation.class);
        entityData.setComponent(reaper, new PortalGem());
        system.processTick(0.25f);

        assertEquals(Creature.AnimationType.IDLE_1, entityData.getComponent(reaper, CreatureViewState.class).state);
        system.stop();
    }

}
