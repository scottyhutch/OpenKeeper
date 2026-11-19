/*
 * Copyright (C) 2014-2018 OpenKeeper
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

import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntitySet;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureFall;
import toniarts.openkeeper.game.component.LeavingDungeon;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Handles creature falling (dropped from hand). In the future maybe all these would be handled by a physics
 * thingie?<br>
 * This only brings the creature down, the {@link CreatureLandingSystem} lands it. A thrown creature is left
 * to the {@link CreatureLandingSystem} too, it flies a turn at a time on the game logic thread.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class CreatureFallSystem implements IGameLogicUpdatable {

    private static final float GRAVITY = 2.0f;

    private final EntityData entityData;
    private final EntitySet fallEntities;
    private final EntitySet leavingEntities;

    /**
     * Creates the system
     *
     * @param entityData the entity data
     */
    public CreatureFallSystem(EntityData entityData) {
        this.entityData = entityData;

        // Position is not in the set, a set gets every change of its components, and Position changes with every step of every creature
        fallEntities = entityData.getEntities(CreatureFall.class, CreatureComponent.class);
        leavingEntities = entityData.getEntities(LeavingDungeon.class);
    }

    @Override
    public void processTick(float tpf) {
        // Add new & remove old
        fallEntities.applyChanges();
        leavingEntities.applyChanges();

        // Process ticks
        for (Entity entity : fallEntities) {
            if (entity.get(CreatureFall.class).velocity != null) {

                // Thrown, the game logic flies it
                continue;
            }

            // Like in DK II, the floor in the middle of the portal is a level lower, the creature falls through
            boolean leaving = leavingEntities.containsId(entity.getId());
            float floorHeight = leaving ? WorldUtils.UNDERFLOOR_HEIGHT : WorldUtils.FLOOR_HEIGHT;
            Position position = entityData.getComponent(entity.getId(), Position.class);
            if (position == null) {

                // In the keeper's hand, nothing to do
                continue;
            }
            if (position.position.y <= floorHeight) {

                // Down already, waiting for the game logic to land it
                continue;
            }

            // A vector of its own: the game logic lands the creature when it sees it on the floor
            Position newPosition = new Position(position.rotation, position.position.clone());
            newPosition.position.y = Math.max(newPosition.position.y - tpf * GRAVITY, floorHeight);
            entityData.setComponent(entity.getId(), newPosition);
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        fallEntities.release();
        leavingEntities.release();
    }

}
