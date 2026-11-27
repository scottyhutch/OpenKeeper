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

import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.badlogic.gdx.ai.pfa.GraphPath;
import com.simsilica.es.EntityId;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.logic.IGameLogicUpdatable;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.pathfinding.INavigable;
import toniarts.openkeeper.game.navigation.pathfinding.MapDistance;
import toniarts.openkeeper.game.navigation.pathfinding.MapIndexedGraph;
import toniarts.openkeeper.game.navigation.pathfinding.MapPathFinder;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.utils.WorldUtils;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Offers navigation related services
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class NavigationService implements INavigationService, IGameLogicUpdatable {

    private static final Logger logger = System.getLogger(NavigationService.class.getName());

    private final IMapController mapController;
    private final IEntityPositionLookup entityPositionLookup;
    private final MapIndexedGraph pathFindingMap;
    private final MapPathFinder pathFinder;
    private final MapDistance heuristic;
    private final Map<NavigableKey, Map<Point, ReachableArea>> reachableAreas = new ConcurrentHashMap<>();

    public NavigationService(IMapController mapController, IEntityPositionLookup entityPositionLookup) {
        this.mapController = mapController;
        this.entityPositionLookup = entityPositionLookup;

        pathFindingMap = new MapIndexedGraph(mapController, entityPositionLookup);
        pathFinder = new MapPathFinder(pathFindingMap, false);
        heuristic = new MapDistance();
    }


    @Override
    public Point findRandomAccessibleTile(Point start, int radius, INavigable navigable) {
        return findRandomAccessibleTile(start, radius, navigable, null);
    }

    @Override
    public Point findRandomTileInRoom(Point start, int radius, INavigable navigable) {
        IRoomController roomInstance = mapController.getRoomControllerByCoordinates(start);
        if (roomInstance == null) {
            logger.log(Level.WARNING, () -> "Starting point " + start + " is not in a room!");
            return null;
        }
        Set<Point> allowedTiles = new HashSet<>(roomInstance.getRoomInstance().getCoordinates());

        return findRandomAccessibleTile(start, radius, navigable, allowedTiles);
    }

    private Point findRandomAccessibleTile(Point start, int radius, INavigable navigable, Set<Point> allowedTiles) {
        Set<Point> tiles = HashSet.newHashSet(radius * radius - 1);

        // Start growing the circle, always testing the tile
        getAccessibleNeighbours(mapController.getMapData().getTile(start.x, start.y), radius, navigable, tiles, allowedTiles);
        tiles.remove(start);

        // Take a random point
        if (!tiles.isEmpty()) {
            return Utils.getRandomItem(new ArrayList<>(tiles));
        }
        return null;
    }

    private void getAccessibleNeighbours(IMapTileInformation startTile, int radius, INavigable navigable, Set<Point> tiles, Set<Point> allowedTiles) {
        if (radius > 0) {
            for (int y = startTile.getY() - 1; y <= startTile.getY() + 1; y++) {
                for (int x = startTile.getX() - 1; x <= startTile.getX() + 1; x++) {

                    // If this is good, add and get neighbours
                    IMapTileInformation tile = mapController.getMapData().getTile(x, y);
                    if (tile != null
                            && !tiles.contains(tile.getLocation())
                            && isAccessible(startTile, tile, navigable)
                            && (allowedTiles == null || allowedTiles.contains(tile.getLocation()))) {
                        tiles.add(tile.getLocation());
                        getAccessibleNeighbours(tile, radius - 1, navigable, tiles, allowedTiles);
                    }
                }
            }
        }
    }

    /**
     * Note that this is not thread safe!!
     *
     * @param start
     * @param end
     * @param navigable
     * @return
     */
    @Override
    public GraphPath<IMapTileInformation> findPath(Point start, Point end, INavigable navigable) {
        pathFindingMap.setPathFindable(navigable);
        GraphPath<IMapTileInformation> outPath = new DefaultGraphPath<>();
        IMapTileInformation startTile = mapController.getMapData().getTile(start.x, start.y);
        IMapTileInformation endTile = mapController.getMapData().getTile(end.x, end.y);
        if (startTile != null && endTile != null && pathFinder.searchNodePath(startTile, endTile, heuristic, outPath)) {
            return outPath;
        }
        return null;
    }

    @Override
    public boolean isReachable(Point start, Point target, INavigable navigable) {
        return getCachedArea(start, navigable).tiles().contains(target);
    }

    @Override
    public Point findGate(Point start, Point end, INavigable navigable) {
        GraphPath<IMapTileInformation> path = findPath(start, end, navigable);
        if (path == null) {
            return null;
        }

        // The regions on the way
        Map<Point, Region> regions = getRegions(getCachedArea(start, navigable), navigable);
        List<Region> route = new ArrayList<>();
        for (IMapTileInformation tile : path) {
            Region region = regions.get(tile.getLocation());
            if (region != null && (route.isEmpty() || !route.getLast().equals(region))) {
                route.add(region);
            }
        }

        // A gate is passed over when the next one, or the end, is in sight through it from the start (0x4CF490)
        int[] eye = {start.x * 2 + 1, start.y * 2 + 1};
        int[] last = {end.x * 2 + 1, end.y * 2 + 1, end.x * 2 + 1, end.y * 2 + 1};
        for (int i = 1; i < route.size(); i++) {
            int[] gate = route.get(i - 1).getGate(route.get(i));
            int[] next = i + 1 < route.size() ? route.get(i).getGate(route.get(i + 1)) : last;
            if (!isInSight(eye, gate, next[0], next[1]) || !isInSight(eye, gate, next[2], next[3])) {
                return route.get(i).getTile((gate[0] + gate[2]) / 2, (gate[1] + gate[3]) / 2);
            }
        }
        return end;
    }

    /**
     * Is the point between the lines from the eye through the ends of the
     * gate. On a line counts, as in DK II (0x4CF490). All in half tiles
     */
    private static boolean isInSight(int[] eye, int[] gate, int x, int y) {
        long side = cross(eye, gate[0], gate[1], gate[2], gate[3]);
        long first = cross(eye, gate[0], gate[1], x, y);
        if (side == 0) {

            // A gate of a corner only, the point has to be straight on past it
            return first == 0 && (long) (gate[0] - eye[0]) * (x - eye[0]) + (long) (gate[1] - eye[1]) * (y - eye[1]) > 0;
        }
        return first * side >= 0 && cross(eye, x, y, gate[2], gate[3]) * side >= 0;
    }

    private static long cross(int[] eye, int ax, int ay, int bx, int by) {
        return (long) (ax - eye[0]) * (by - eye[1]) - (long) (ay - eye[1]) * (bx - eye[0]);
    }

    /**
     * The area the entity can get to from the start, filled once a tick
     */
    private ReachableArea getCachedArea(Point start, INavigable navigable) {
        Map<Point, ReachableArea> areas = reachableAreas.computeIfAbsent(new NavigableKey(navigable), key -> new ConcurrentHashMap<>());
        ReachableArea area = areas.get(start);
        if (area != null && !area.isSameFor(navigable)) {

            // It breaks other doors than the one the area was filled for, so it has an area of its own. Not kept
            return getReachableArea(start, navigable);
        }
        if (area == null) {
            area = getReachableArea(start, navigable);

            // Every tile in the area has the same area. Unless the start can't be stepped back on, a locked door say
            IMapDataInformation<?> mapData = mapController.getMapData();
            ReachableArea startArea = area;
            if (Arrays.stream(WorldUtils.getSurroundingTiles(mapData, start, false)).anyMatch(p -> !p.equals(start)
                    && startArea.tiles().contains(p) && isAccessible(mapData.getTile(p), mapData.getTile(start), navigable))) {
                area.tiles().forEach(p -> areas.putIfAbsent(p, startArea));
            } else {
                areas.put(start, area);
            }
        }
        return area;
    }

    /**
     * The tiles the entity can get to from the start, the same steps the path
     * finding takes. DK II keeps the connected areas of the map, so a path to
     * somewhere out of reach fails at once (DKII.EXE 0x4C9400, 0x4D0150)
     *
     * @param start the start point
     * @param navigable the entity
     * @return the reachable tiles, the start included, and the doors met on the
     * way
     */
    private ReachableArea getReachableArea(Point start, INavigable navigable) {
        IMapDataInformation<?> mapData = mapController.getMapData();
        Set<Point> area = new HashSet<>();
        Map<EntityId, Boolean> doors = new HashMap<>();
        area.add(start);
        addDoor(start, navigable, doors);
        Deque<Point> open = new ArrayDeque<>(area);
        while (!open.isEmpty()) {
            Point p = open.poll();
            for (Point next : WorldUtils.getSurroundingTiles(mapData, p, false)) {
                if (!area.contains(next)) {
                    addDoor(next, navigable, doors);
                    if (isAccessible(mapData.getTile(p), mapData.getTile(next), navigable)) {
                        area.add(next);
                        open.add(next);
                    }
                }
            }
        }
        return new ReachableArea(area, doors, new HashMap<>());
    }

    /**
     * Splits the area into regions, rectangles of one kind of ground, the
     * first time it is asked in a tick. DK II's navigation map tells water,
     * lava and doors from the rest (0x44FC40, 0x4A66F0) and the triangles of
     * its navigation mesh, its regions, are of one kind each. It builds them
     * again round a tile each time the tile changes (0x4D1930, 0x4CE770)
     */
    private Map<Point, Region> getRegions(ReachableArea area, INavigable navigable) {
        if (area.regions().isEmpty()) {
            List<Point> tiles = new ArrayList<>(area.tiles());
            tiles.sort(Comparator.comparingInt((Point p) -> p.y).thenComparingInt(p -> p.x));
            for (Point p : tiles) {
                if (!area.regions().containsKey(p)) {
                    Object kind = getGroundKind(p, navigable);
                    int x1 = p.x;
                    while (isFree(area, x1 + 1, x1 + 1, p.y, kind, navigable)) {
                        x1++;
                    }
                    int y1 = p.y;
                    while (isFree(area, p.x, x1, y1 + 1, kind, navigable)) {
                        y1++;
                    }
                    Region region = new Region(p.x, p.y, x1, y1);
                    for (int y = p.y; y <= y1; y++) {
                        for (int x = p.x; x <= x1; x++) {
                            area.regions().put(new Point(x, y), region);
                        }
                    }
                }
            }
        }
        return area.regions();
    }

    /**
     * Are the tiles of the row in the area, of the kind and in no region yet
     */
    private boolean isFree(ReachableArea area, int x0, int x1, int y, Object kind, INavigable navigable) {
        for (int x = x0; x <= x1; x++) {
            Point p = new Point(x, y);
            if (!area.tiles().contains(p) || area.regions().containsKey(p) || !Objects.equals(kind, getGroundKind(p, navigable))) {
                return false;
            }
        }
        return true;
    }

    /**
     * The kind of ground: the door on it, water, lava or null for the rest
     */
    private Object getGroundKind(Point p, INavigable navigable) {
        EntityId door = entityPositionLookup.getBlockingDoor(p.x, p.y, navigable.getOwnerId());
        if (door != null) {
            return door;
        }
        Set<Terrain.TerrainFlag> flags = mapController.getTerrain(mapController.getMapData().getTile(p)).getFlags();
        return flags.contains(Terrain.TerrainFlag.WATER) ? Terrain.TerrainFlag.WATER
                : flags.contains(Terrain.TerrainFlag.LAVA) ? Terrain.TerrainFlag.LAVA : null;
    }

    /**
     * Notes the door that blocks the tile and whether the entity breaks it.
     * The area depends on it, as an entity that breaks the door gets through
     */
    private void addDoor(Point tile, INavigable navigable, Map<EntityId, Boolean> doors) {
        EntityId door = entityPositionLookup.getBlockingDoor(tile.x, tile.y, navigable.getOwnerId());
        if (door != null) {
            doors.computeIfAbsent(door, navigable::canBreakDoor);
        }
    }

    @Override
    public boolean isAccessible(IMapTileInformation from, IMapTileInformation to, INavigable navigable) {
        Float cost = navigable.getCost(from, to, mapController, entityPositionLookup);
        return cost != null;
    }

    @Override
    public void processTick(float tpf) {

        // The reachable areas are a cache for a tick, the map and the doors change
        reachableAreas.clear();
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    /**
     * What the reachable area depends on. A navigable with costs of its own,
     * one that digs through rock say, is kept apart by its type
     */
    private record NavigableKey(Class<?> type, short ownerId, boolean canFly, boolean canWalkOnWater, boolean canWalkOnLava,
            boolean canMoveDiagonally) {

        NavigableKey(INavigable navigable) {
            this(navigable.getClass(), navigable.getOwnerId(), navigable.canFly(), navigable.canWalkOnWater(),
                    navigable.canWalkOnLava(), navigable.canMoveDiagonally());
        }

    }

    /**
     * The tiles of a reachable area and the doors met while filling it, with
     * whether the entity it was filled for breaks them. And its regions by
     * tile, once asked for
     */
    private record ReachableArea(Set<Point> tiles, Map<EntityId, Boolean> doors, Map<Point, Region> regions) {

        /**
         * Does the area hold for this entity too. It does unless the entity
         * breaks a door met on the way that the other didn't, or the other way
         * round, like a party follower that leaves the doors to its leader
         */
        boolean isSameFor(INavigable navigable) {
            return doors.entrySet().stream().allMatch(door -> navigable.canBreakDoor(door.getKey()) == door.getValue());
        }

    }

    /**
     * A rectangle of a reachable area, all of one kind of ground
     */
    private record Region(int x0, int y0, int x1, int y1) {

        /**
         * Where it meets the other region, in half tiles: the two ends of the
         * line between them, or twice the corner they share
         */
        int[] getGate(Region other) {
            return new int[]{Math.max(x0, other.x0) * 2, Math.max(y0, other.y0) * 2,
                (Math.min(x1, other.x1) + 1) * 2, (Math.min(y1, other.y1) + 1) * 2};
        }

        /**
         * The tile of the region nearest the point, in half tiles
         */
        Point getTile(int x, int y) {
            return new Point(Math.clamp(x / 2, x0, x1), Math.clamp(y / 2, y0, y1));
        }

    }

}
