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
package toniarts.openkeeper.game.controller.player;

/**
 * The keeper's manufacturing progress. All the creatures working in the
 * keeper's workshops add their manufacture points to the item currently in
 * production, like research points add to the current research.
 */
public final class PlayerManufactureControl {

    private int progress = 0;
    private int required = 0;

    /**
     * Adds manufacture points to the item in production. The original checks
     * the points against the cost of the first slot of its manufacture list,
     * not of the item being made (0x506B50). The list doesn't go back to its
     * first slot when an item is done, so every item costs as much as the
     * first one ordered until the list is emptied by selling.
     *
     * @param points the points to add
     * @param itemRequired the points the item in production needs
     * ({@code manufToBuild})
     * @return true if the item is finished, the progress starts over then
     */
    public boolean manufacture(int points, int itemRequired) {
        if (required == 0) {
            required = itemRequired;
        }
        progress += points;
        if (progress >= required) {
            progress = 0;
            return true;
        }
        return false;
    }

    /**
     * Starts the manufacture list over, as the original does when selling
     * blueprints leaves nothing to manufacture (0x5069E0). The next item
     * ordered sets the cost again.
     */
    public void clearOrders() {
        progress = 0;
        required = 0;
    }

    /**
     * @return the manufacture points gathered for the item in production
     */
    public int getProgress() {
        return progress;
    }

}
