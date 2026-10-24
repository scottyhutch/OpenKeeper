/*
 * Copyright (C) 2014-2019 OpenKeeper
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

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.filter.FieldFilter;
import java.util.List;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.TrapComponent;
import toniarts.openkeeper.game.component.TrapViewState;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.data.ResearchableEntity;
import toniarts.openkeeper.game.data.ResearchableType;
import toniarts.openkeeper.game.listener.PlayerTrapListener;
import toniarts.openkeeper.tools.convert.map.Trap;

/**
 * Holds a list of player traps and functionality related to them
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class PlayerTrapControl extends AbstractResearchablePlayerControl<Trap, ResearchableEntity, PlayerTrapListener> /*implements RoomListener*/ {

    private final int trapCount = 0;
    private final EntityData entityData;

    public PlayerTrapControl(Keeper keeper, List<Trap> traps, EntityData entityData) {
        super(keeper, keeper.getAvailableTraps(), traps);
        this.entityData = entityData;
    }

    @Override
    protected ResearchableEntity createDataType(Trap type) {
        return new ResearchableEntity(type.getTrapId(), ResearchableType.TRAP);
    }

    /**
     * Get player door count
     *
     * @return the door count
     */
    @Override
    public int getTypeCount() {
        return trapCount;
    }

    /**
     * Count the traps this player has in the world. The doors that hold a trap
     * are doors, not traps.
     *
     * @param trapId the trap type, 0 = any
     * @param blueprints count the blueprints (placed, not built yet)
     * @param built count the built traps
     * @return the number of the traps
     */
    public int getPlacedCount(short trapId, boolean blueprints, boolean built) {
        int count = 0;
        for (EntityId entityId : entityData.findEntities(new FieldFilter<>(Owner.class, "ownerId", keeper.getId()), Owner.class, TrapComponent.class, TrapViewState.class)) {
            TrapComponent trap = entityData.getComponent(entityId, TrapComponent.class);
            TrapViewState viewState = entityData.getComponent(entityId, TrapViewState.class);
            if (trap != null && viewState != null && (trapId == 0 || trap.trapId == trapId) && (viewState.blueprint ? blueprints : built)) {
                count++;
            }
        }
        return count;
    }

}
