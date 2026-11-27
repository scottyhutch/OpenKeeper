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
package toniarts.openkeeper.game.navigation;

import com.badlogic.gdx.ai.pfa.GraphPath;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.pathfinding.INavigable;

/**
 * Provides access to navigation inside the game world. Pathfinding and such.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface INavigationService {

    /**
     * Get a random tile, that is not a starting tile
     *
     * @param start starting coordinates
     * @param radius radius, in tiles
     * @param navigable the navigable entity
     * @return a random tile if one is found
     */
    Point findRandomAccessibleTile(Point start, int radius, INavigable navigable);

    /**
     * Get a random tile from current room, that is not a starting tile. The
     * starting tile must be a room.
     *
     * @param start starting coordinates
     * @param radius radius, in tiles
     * @param navigable the navigable entity
     * @return a random tile if one is found
     */
    Point findRandomTileInRoom(Point start, int radius, INavigable navigable);

    /**
     * Finds a path between the given points if there is one
     *
     * @param start start point
     * @param end end point
     * @param navigable the entity to find path for
     * @return output path, null if path not found
     */
    GraphPath<IMapTileInformation> findPath(Point start, Point end, INavigable navigable);

    /**
     * Can the entity get from start to the target. Use this rather than
     * {@link #findPath(Point, Point, INavigable)} to just test that there is a
     * way: the area the entity can get to is flood filled once a tick and
     * shared by every start in it
     *
     * @param start start point
     * @param target the point to get to
     * @param navigable the entity to test with
     * @return true if there is a way from start to the target
     */
    boolean isReachable(Point start, Point target, INavigable navigable);

    /**
     * The first gate on the way from start to the end. The area the entity
     * can get to is split into regions, rectangles of one kind of ground, and
     * a gate is where the way goes from one region to the next. The gates
     * that can be seen through from the start are passed over, as DK II does
     * with its navigation regions (DKII.EXE 0x4D0150, 0x4CF490)
     *
     * @param start start point
     * @param end end point
     * @param navigable the entity to find the way for
     * @return the tile past the middle of the first gate that hides the way
     * on, the end if no gate does, or null if there is no way
     */
    Point findGate(Point start, Point end, INavigable navigable);

    /**
     * Check if given tile is accessible by the given creature
     *
     * @param from from where
     * @param to to where
     * @param navigable the entity to test with
     * @return is accessible
     */
    boolean isAccessible(IMapTileInformation from, IMapTileInformation to, INavigable navigable);

}
