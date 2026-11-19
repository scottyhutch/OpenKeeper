/*
 * Copyright (C) 2014-2015 OpenKeeper
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
package toniarts.openkeeper.game.trigger.actionpoint;


import java.util.function.Predicate;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.object.IObjectController;
import toniarts.openkeeper.game.data.ActionPoint;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.state.session.PlayerService;
import toniarts.openkeeper.game.trigger.TriggerControl;
import toniarts.openkeeper.game.trigger.TriggerGenericData;
import toniarts.openkeeper.tools.convert.map.Terrain;

/**
 *
 * @author ArchDemon
 */
public final class ActionPointTriggerControl extends TriggerControl {

    private final ActionPoint ap;
    private final IEntityPositionLookup entityPositionLookup;

    public ActionPointTriggerControl(final IGameController gameController, final ILevelInfo levelInfo, final IGameTimer gameTimer,
            final IMapController mapController, final ICreaturesController creaturesController, int triggerId, ActionPoint ap,
            final PlayerService playerService, final IEntityPositionLookup entityPositionLookup) {
        super(gameController, levelInfo, gameTimer, mapController, creaturesController, playerService, triggerId);
        this.ap = ap;
        this.entityPositionLookup = entityPositionLookup;
    }

    @Override
    protected boolean isActive(TriggerGenericData trigger) {
        switch (trigger.getType()) {
            case AP_CONGREGATE_IN:
                return isCongregateActive(trigger);

            case AP_POSESSED_CREATURE_ENTERS:
                return isPossessedCreatureActive(trigger);

            case AP_CLAIM_PART_OF: {
                short playerId = trigger.getUserData("playerId", short.class);
                return compare(trigger, countTiles(playerId, (short) 0), trigger.getUserData("value", int.class));
            }

            case AP_CLAIM_ALL_OF: {

                // DK II ignores the comparison and the value, the count has to equal the area (DKII.EXE 0x4B0D70)
                short playerId = trigger.getUserData("playerId", short.class);
                return countTiles(playerId, (short) 0) == countTiles((short) 0, (short) 0);
            }

            case AP_SLAB_TYPES: {
                short playerId = trigger.getUserData("playerId", short.class);
                short terrainId = trigger.getUserData("terrainId", short.class);
                return compare(trigger, countTiles(playerId, terrainId), trigger.getUserData("value", int.class));
            }

            case AP_TAG_PART_OF: {
                short playerId = trigger.getUserData("playerId", short.class);
                // TODO check who tagged tile
                return compare(trigger, countTiles(tile -> tile.isSelected(playerId)),
                        trigger.getUserData("value", int.class));
            }

            case AP_TAG_ALL_OF: {

                // DK II counts the player's dig tasks and the taggable tiles of the point apart, and ignores the
                // comparison and the value: the tasks have to be at least as many (#1157)
                short playerId = trigger.getUserData("playerId", short.class);
                return countTiles(tile -> tile.isSelected(playerId))
                        >= countTiles(tile -> mapController.getTerrain(tile).getFlags().contains(Terrain.TerrainFlag.TAGGABLE));
            }

            default:
                return super.isActive(trigger);
        }
    }

    /**
     * The possessed creature condition holds while a creature that is
     * possessed, of the player (0 = any) and of the creature type (0 = any),
     * is on the action point. The original looks at the creatures on the
     * tiles of the point and ignores the comparison and the value.
     */
    private boolean isPossessedCreatureActive(TriggerGenericData trigger) {
        short playerId = trigger.getUserData("playerId", short.class);
        short targetId = trigger.getUserData("targetId", short.class);
        short type = trigger.getUserData("targetType", short.class);
        if (type != 0 && type != 3) {
            // STUB: the possessed objects (kind 6), nothing but creatures can be possessed here
            logStub(trigger, "Only creatures can be possessed, targetType " + type + " is not supported");
            return false;
        }
        return countCreatures(creature -> creature.isPossessed() && isWanted(creature, playerId, targetId)) > 0;
    }

    private static boolean isWanted(ICreatureController creature, short playerId, short targetId) {
        return (playerId == 0 || creature.getOwnerId() == playerId) && (targetId == 0 || creature.getCreature().getCreatureId() == targetId);
    }

    private int countCreatures(Predicate<ICreatureController> filter) {
        int count = 0;
        for (int x = (int) ap.getStart().x; x <= (int) ap.getEnd().x; x++) {
            for (int y = (int) ap.getStart().y; y <= (int) ap.getEnd().y; y++) {
                for (ICreatureController creature : entityPositionLookup.getEntityTypesInLocation(x, y, ICreatureController.class)) {
                    if (filter.test(creature)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private boolean isCongregateActive(TriggerGenericData trigger) {
        short playerId = trigger.getUserData("playerId", short.class);
        short targetId = trigger.getUserData("targetId", short.class);
        int value = trigger.getUserData("value", int.class);
        short type = trigger.getUserData("targetType", short.class);

        // DK II counts objects for target type 6 and creatures for every other type (DKII.EXE 0x4B0D70)
        if (type == 6) {
            return compare(trigger, countObjects(playerId, targetId), value);
        }
        return compare(trigger, countCreatures(creature -> isWanted(creature, playerId, targetId)), value);
    }

    private int countObjects(short playerId, short targetId) {
        int count = 0;
        for (int x = (int) ap.getStart().x; x <= (int) ap.getEnd().x; x++) {
            for (int y = (int) ap.getStart().y; y <= (int) ap.getEnd().y; y++) {
                for (IObjectController object : entityPositionLookup.getEntityTypesInLocation(x, y, IObjectController.class)) {
                    if ((playerId == 0 || object.getOwnerId() == playerId) && (targetId == 0 || object.getObjectId() == targetId)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /**
     * Counts the action point's tiles the way DK II does for the claim and
     * slab type conditions (DKII.EXE 0x4B07E0). With a terrain the tile has to
     * be of that terrain. Without one, a player's tiles only count on ownable
     * terrain. With neither, every tile counts.
     *
     * @param playerId the tile owner, 0 for any
     * @param terrainId the terrain, 0 for none
     * @return the number of matching tiles
     */
    private int countTiles(short playerId, short terrainId) {
        return countTiles(tile -> (playerId == 0 || playerId == tile.getOwnerId())
                && (terrainId != 0 ? terrainId == tile.getTerrainId()
                        : playerId == 0 || mapController.getTerrain(tile).getFlags().contains(Terrain.TerrainFlag.OWNABLE)));
    }

    private int countTiles(Predicate<IMapTileInformation> filter) {
        IMapDataInformation map = mapController.getMapData();
        int count = 0;
        for (int x = (int) ap.getStart().x; x <= (int) ap.getEnd().x; x++) {
            for (int y = (int) ap.getStart().y; y <= (int) ap.getEnd().y; y++) {
                if (filter.test(map.getTile(x, y))) {
                    count++;
                }
            }
        }
        return count;
    }
}
