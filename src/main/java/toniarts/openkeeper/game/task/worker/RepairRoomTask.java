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
package toniarts.openkeeper.game.task.worker;

import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.utils.Point;

/**
 * Repair a room of our own that has been damaged. The imp works the same way
 * as when claiming a room, it looks the same too.
 */
public final class RepairRoomTask extends ClaimTileTask {

    public RepairRoomTask(final INavigationService navigationService, final IMapController mapController, Point p, short playerId) {
        super(navigationService, mapController, p, playerId);
    }

    @Override
    public boolean isValid(ICreatureController creature) {
        return mapController.isRepairableRoom(getTaskLocation(), playerId);
    }

    @Override
    public String toString() {
        return "Repair room at " + getTaskLocation();
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.CLAIM_ROOM;
    }

}
