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

import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntitySet;
import com.simsilica.es.filter.AndFilter;
import com.simsilica.es.filter.FieldFilter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.TaskComponent;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.player.PlayerManufactureControl;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.utils.GameTimeCounter;

/**
 * The manufacture points of a keeper drop while none of its creatures works in
 * the workshops. Once a second the points gathered for the item in production
 * go down by MANUFACTURE_POINT_LOSS_WHILE_NO_WORKERS_PER_SECOND, never below
 * zero, as in the original.
 */
public final class ManufacturePointLossSystem extends GameTimeCounter {

    private final Map<Short, IPlayerController> playerControllers;
    private final EntitySet manufacturers;
    private final int pointLossPerSecond;

    public ManufacturePointLossSystem(EntityData entityData, Map<Short, IPlayerController> playerControllers, int pointLossPerSecond) {
        this.playerControllers = playerControllers;
        this.pointLossPerSecond = pointLossPerSecond;
        manufacturers = entityData.getEntities(new AndFilter(TaskComponent.class,
                new FieldFilter(TaskComponent.class, "taskStarted", true),
                new FieldFilter(TaskComponent.class, "taskType", TaskType.MANUFACTURE)), TaskComponent.class, Owner.class);
    }

    @Override
    public void processTick(float tpf) {
        double second = Math.floor(timeElapsed);
        super.processTick(tpf);
        if (Math.floor(timeElapsed) == second) {
            return;
        }

        manufacturers.applyChanges();
        Set<Short> manufacturing = new HashSet<>();
        for (Entity entity : manufacturers) {
            manufacturing.add(entity.get(Owner.class).ownerId);
        }
        for (Map.Entry<Short, IPlayerController> entry : playerControllers.entrySet()) {
            PlayerManufactureControl manufactureControl = entry.getValue().getManufactureControl();
            if (manufactureControl != null && !manufacturing.contains(entry.getKey())) {
                manufactureControl.losePoints(pointLossPerSecond);
            }
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        manufacturers.release();
    }

}
