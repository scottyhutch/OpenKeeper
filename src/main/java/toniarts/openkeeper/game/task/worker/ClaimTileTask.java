/*
 * Copyright (C) 2014-2016 OpenKeeper
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

import toniarts.openkeeper.game.event.GameEventType;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.TileWork;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.AbstractTileTask;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Claim a tile task, for workers
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ClaimTileTask extends AbstractTileTask {

    /**
     * The ticks from breaking an enemy's floor to starting to claim it. DK II's
     * imp goes from attacking (state 20) to claiming (state 15, then 16,
     * 0x465CD0), and state 16 starts the claim action, each on its own tick
     */
    private static final int TICKS_TO_START_CLAIMING = 3;

    private final float tickLength;
    private final boolean leftByDigging;

    public ClaimTileTask(final INavigationService navigationService, final IMapController mapController, Point p, short playerId, int ticksPerSecond) {
        this(navigationService, mapController, p, playerId, ticksPerSecond, false);
    }

    /**
     * Claim a tile
     *
     * @param navigationService the navigation service
     * @param mapController the map controller
     * @param p the tile
     * @param playerId the claiming player
     * @param ticksPerSecond the level's game ticks per second
     * @param leftByDigging whether the tile is the floor that digging by the
     * player's workers has just left
     */
    public ClaimTileTask(final INavigationService navigationService, final IMapController mapController, Point p, short playerId, int ticksPerSecond,
            boolean leftByDigging) {
        super(navigationService, mapController, p, playerId);
        this.tickLength = 1f / ticksPerSecond;
        this.leftByDigging = leftByDigging;
    }

    /**
     * The floor that digging has left is claimed before the other usual work,
     * but not while any of the eight tiles around it is still tagged for
     * digging. Tiles tagged for mining gold or gems don't hold it up (DKII.EXE
     * 0x469600, the first job of the imp's work list at 0x698658)
     *
     * @return {@code true} if this is such floor, with no digging left around
     * it
     */
    public boolean isDugFloorToClaim() {
        if (!leftByDigging) {
            return false;
        }
        for (Point p : WorldUtils.getSurroundingTiles(mapController.getMapData(), getTaskLocation(), true)) {
            IMapTileInformation tile = mapController.getMapData().getTile(p);
            if (tile.isSelected(playerId) && tile.getGold() == 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Vector2f getTarget(ICreatureController creature) {
        return WorldUtils.pointToVector2f(getTaskLocation()); // FIXME 0.5f not needed?
    }

    @Override
    public boolean isValid(ICreatureController creature) {
        return mapController.isClaimableTile(getTaskLocation(), playerId) || getExecutionDuration(creature) < 0;
    }

    /**
     * A worker nearer to the tile than the one assigned to it takes the claim
     * over, unless that one is claiming it already. So after a fight the
     * workers go straight back to the tile (0x46AB50)
     */
    @Override
    public boolean makeRoomFor(ICreatureController creature) {
        Vector2f tile = WorldUtils.pointToVector2f(getTaskLocation());
        for (ICreatureController assignee : getAssignees()) {
            if (assignee.isAtAssignedTaskTarget() || getDistanceSquared(assignee, tile) <= getDistanceSquared(creature, tile)) {
                return false;
            }
        }
        if (!isValid(creature) || !isReachable(creature)) {
            return false;
        }
        for (ICreatureController assignee : getAssignees()) {
            unassign(assignee);
            if (this.equals(assignee.getAssignedTask())) {
                assignee.unassingCurrentTask();
            }
        }
        return true;
    }

    private static float getDistanceSquared(ICreatureController creature, Vector2f tile) {
        Vector3f position = creature.getPosition();
        return tile.distanceSquared(position.x, position.z);
    }

    @Override
    public String toString() {
        return "Claim tile at " + getTaskLocation();
    }

    /**
     * Claims the tile as DK II's claim actions do it (0x496AA0). An enemy's
     * floor is attacked first (0x465F20, 0x497670), in blows of a tick's
     * worth of the attack rate. Ground nobody owns is claimed (0x465CD0,
     * 0x4974C0) every tick from the tick after the worker starts, a tick's
     * worth of the claim rate, until the tile turns, and then the worker plays
     * the loop of its work animation out before it moves on. A broken floor
     * is claimed once the worker has gone over to claiming it. While the
     * worker waits for either, the stored execution duration is the time on
     * the task when it is done waiting, negated. A room is claimed in blows
     * too (0x466180, 0x497BF0). The work animation is the worker's eating
     * one, the imp's is called Imp_ClaimFloor.
     *
     * @param creature the worker
     * @param executionDuration the time the worker has spent on the task
     */
    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {
        float loopEnd = -getExecutionDuration(creature);
        if (loopEnd > 0) {
            if (executionDuration >= loopEnd) {
                setExecutionDuration(creature, 0);
            }
            return;
        }
        if (mapController.isClaimableTile(getTaskLocation(), playerId)) {
            boolean ground = isGround();
            if (ground ? executionDuration > 0 : isBlowDue(creature, executionDuration, Creature.AnimationType.EATING)) {
                mapController.applyClaimTile(getTaskLocation(), playerId, creature);
                if (!ground && isGround()) {
                    setExecutionDuration(creature, -(executionDuration + TICKS_TO_START_CLAIMING * tickLength));
                } else if (!isValid(creature)) {
                    creature.fireEvent(GameEventType.CREATURE_CLAIM_TILE, getTaskLocation());
                    loopEnd = getLoopEnd(creature.getCreature(), Creature.AnimationType.EATING, executionDuration);
                    if (loopEnd > executionDuration) {
                        setExecutionDuration(creature, -loopEnd);
                    }
                }
            }
            return;
        }

        // A room
        if (isBlowDue(creature, executionDuration, Creature.AnimationType.EATING)) {
            short owner = mapController.getMapData().getTile(getTaskLocation()).getOwnerId();
            mapController.applyClaimTile(getTaskLocation(), playerId, creature);

            // Heard once, when the room changes hands. Not when the worker mends its own room after an enemy's blows
            if (owner != playerId && mapController.getMapData().getTile(getTaskLocation()).getOwnerId() == playerId) {
                creature.fireEvent(GameEventType.CREATURE_CLAIM_TILE, getTaskLocation());
            }
        }
    }

    private boolean isGround() {
        Terrain terrain = mapController.getTerrain(mapController.getMapData().getTile(getTaskLocation()));
        return !terrain.getFlags().contains(Terrain.TerrainFlag.SOLID) && TileWork.convertsAtFullHealth(terrain);
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.CLAIM_TILE;
    }

}
