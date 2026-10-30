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
package toniarts.openkeeper.game.controller;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.util.Random;
import toniarts.openkeeper.game.component.AttackTarget;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureImprisoned;
import toniarts.openkeeper.game.component.CreatureTortured;
import toniarts.openkeeper.game.component.Death;
import toniarts.openkeeper.game.component.FetchPortalGem;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.InHand;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.PortalGem;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.SummonedReaper;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.map.IMapInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.navigation.pathfinding.INavigable;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * When a keeper wins a level that has the Portal Gem, the Horned Reaper comes
 * to take it, as in DK II. The keeper's own Reaper goes for it if he has one.
 * Otherwise a Reaper appears on a free tile five to eight tiles from the gem,
 * from where he can walk to it. A gem still carried by a creature is dropped
 * where the creature is.
 */
public final class PortalGemReaper {

    private static final int ENTRANCE_TRIES = 500;
    private static final int TRIES_BEFORE_ANY_DISTANCE = 128;
    private static final int ENTRANCE_OFFSET = 8;
    private static final int MIN_DISTANCE_SQUARED = 25;

    private final IKwdFile kwdFile;
    private final EntityData entityData;
    private final IGameTimer gameTimer;
    private final ICreaturesController creaturesController;
    private final IObjectsController objectsController;
    private final INavigationService navigationService;
    private final IMapInformation<?> mapInformation;
    private final Random random;

    /**
     * @param kwdFile the level
     * @param entityData the entities
     * @param gameTimer the game clock
     * @param creaturesController the creatures
     * @param objectsController the objects
     * @param navigationService finds the way to the gem
     * @param mapInformation tells the solid tiles
     */
    public PortalGemReaper(IKwdFile kwdFile, EntityData entityData, IGameTimer gameTimer, ICreaturesController creaturesController,
            IObjectsController objectsController, INavigationService navigationService, IMapInformation<?> mapInformation) {
        this(kwdFile, entityData, gameTimer, creaturesController, objectsController, navigationService, mapInformation, new Random());
    }

    PortalGemReaper(IKwdFile kwdFile, EntityData entityData, IGameTimer gameTimer, ICreaturesController creaturesController,
            IObjectsController objectsController, INavigationService navigationService, IMapInformation<?> mapInformation, Random random) {
        this.kwdFile = kwdFile;
        this.entityData = entityData;
        this.gameTimer = gameTimer;
        this.creaturesController = creaturesController;
        this.objectsController = objectsController;
        this.navigationService = navigationService;
        this.mapInformation = mapInformation;
        this.random = random;
    }

    /**
     * Sends the Horned Reaper for the Portal Gem, if the level has one
     *
     * @param playerId the keeper who won
     * @return the Reaper, {@code null} if there is no gem or no Reaper
     */
    public EntityId callForGem(short playerId) {
        Short gemObjectId = getPortalGemObjectId(kwdFile);
        Creature reaperCreature = getReaper();
        if (gemObjectId == null || reaperCreature == null || !entityData.findEntities(null, FetchPortalGem.class).isEmpty()) {
            return null;
        }

        EntityId gem = findGem(gemObjectId);
        if (gem == null) {
            return null;
        }

        Point gemTile = WorldUtils.vectorToPoint(entityData.getComponent(gem, Position.class).position);
        EntityId reaper = findOwnReaper(playerId, gemTile, new ReaperNavigable(playerId, reaperCreature));
        if (reaper == null) {
            Point entrance = findEntrance(gemTile, new ReaperNavigable(playerId, reaperCreature));
            int level = Math.max(1, kwdFile.getGameLevel().getOneShotHornyLev());
            reaper = creaturesController.spawnCreature(reaperCreature.getId(), playerId, level,
                    WorldUtils.pointToVector2f(entrance), ICreaturesController.SpawnType.CONJURE);
            entityData.setComponent(reaper, new SummonedReaper(gameTimer.getGameTime(), 0, 0));
        } else {

            // A Reaper of the map is looked after with the summoned ones from now on
            if (entityData.getComponent(reaper, SummonedReaper.class) == null) {
                entityData.setComponent(reaper, new SummonedReaper(gameTimer.getGameTime(), 0, 0));
            }
            entityData.removeComponent(reaper, AttackTarget.class);
            entityData.removeComponent(reaper, Navigation.class);
        }
        entityData.setComponent(reaper, new FetchPortalGem(gem));

        return reaper;
    }

    /**
     * The Portal Gem object of the level
     *
     * @param kwdFile the level
     * @return the object ID, {@code null} if the level has no Portal Gem
     */
    public static Short getPortalGemObjectId(IKwdFile kwdFile) {
        for (GameObject object : kwdFile.getObjectList()) {
            if (object.getFlags().contains(GameObject.ObjectFlag.OBJECT_TYPE_LEVEL_GEM)) {
                return object.getObjectId();
            }
        }
        return null;
    }

    /**
     * Drops the Portal Gem a creature carries where the creature is
     *
     * @param entityData the entities
     * @param objectsController the objects
     * @param gemObjectId the Portal Gem object
     * @param carrier the creature carrying the gem
     * @return the gem object
     */
    public static EntityId dropPortalGem(EntityData entityData, IObjectsController objectsController, short gemObjectId, EntityId carrier) {
        Owner owner = entityData.getComponent(carrier, Owner.class);
        Position position = entityData.getComponent(carrier, Position.class);
        entityData.removeComponent(carrier, PortalGem.class);

        return objectsController.loadObject(gemObjectId, owner != null ? owner.ownerId : 0, position.position.clone(), 0);
    }

    private Creature getReaper() {
        for (Creature creature : kwdFile.getCreatureList()) {
            if (creature.getFlags().contains(Creature.CreatureFlag.IS_HORNY)) {
                return creature;
            }
        }
        return null;
    }

    private EntityId findGem(short gemObjectId) {
        for (EntityId entityId : entityData.findEntities(null, ObjectComponent.class, Position.class)) {
            if (entityData.getComponent(entityId, ObjectComponent.class).objectId == gemObjectId) {
                return entityId;
            }
        }
        for (EntityId carrier : entityData.findEntities(null, PortalGem.class, Position.class)) {
            return dropPortalGem(entityData, objectsController, gemObjectId, carrier);
        }
        return null;
    }

    private EntityId findOwnReaper(short playerId, Point gemTile, INavigable navigable) {
        for (EntityId entityId : entityData.findEntities(null, CreatureComponent.class, Owner.class, Health.class)) {
            if (entityData.getComponent(entityId, Owner.class).ownerId != playerId
                    || entityData.getComponent(entityId, Death.class) != null
                    || entityData.getComponent(entityId, InHand.class) != null
                    || entityData.getComponent(entityId, Unconscious.class) != null
                    || entityData.getComponent(entityId, CreatureImprisoned.class) != null
                    || entityData.getComponent(entityId, CreatureTortured.class) != null) {
                continue;
            }
            Creature creature = kwdFile.getCreature(entityData.getComponent(entityId, CreatureComponent.class).creatureId);
            SummonedReaper summonedReaper = entityData.getComponent(entityId, SummonedReaper.class);
            if (creature != null && creature.getFlags().contains(Creature.CreatureFlag.IS_HORNY)
                    && (summonedReaper == null || !summonedReaper.isLeaving())
                    && navigationService.findPath(WorldUtils.vectorToPoint(entityData.getComponent(entityId, Position.class).position),
                            gemTile, navigable) != null) {
                return entityId;
            }
        }
        return null;
    }

    /**
     * A random free tile around the gem. Close tiles are taken only when
     * nothing farther has turned up for a while, and the gem tile itself if
     * nothing is found at all.
     */
    private Point findEntrance(Point gemTile, INavigable reaper) {
        for (int i = 0; i < ENTRANCE_TRIES; i++) {
            int dx = random.nextInt(ENTRANCE_OFFSET * 2 + 1) - ENTRANCE_OFFSET;
            int dy = random.nextInt(ENTRANCE_OFFSET * 2 + 1) - ENTRANCE_OFFSET;
            if (i < TRIES_BEFORE_ANY_DISTANCE && dx * dx + dy * dy < MIN_DISTANCE_SQUARED) {
                continue;
            }
            Point tile = new Point(gemTile.x + dx, gemTile.y + dy);
            // The cheap checks first, the search is the expensive part
            if (!mapInformation.isSolid(tile) && !hasCreature(tile) && navigationService.findPath(gemTile, tile, reaper) != null) {
                return tile;
            }
        }
        return gemTile;
    }

    private boolean hasCreature(Point tile) {
        for (EntityId entityId : entityData.findEntities(null, CreatureComponent.class, Position.class)) {
            if (tile.equals(WorldUtils.vectorToPoint(entityData.getComponent(entityId, Position.class).position))) {
                return true;
            }
        }
        return false;
    }

    /**
     * How the Reaper moves, before there is a Reaper
     */
    private record ReaperNavigable(short ownerId, Creature creature) implements INavigable {

        @Override
        public short getOwnerId() {
            return ownerId;
        }

        @Override
        public boolean canFly() {
            return creature.getFlags().contains(Creature.CreatureFlag.CAN_FLY);
        }

        @Override
        public boolean canWalkOnWater() {
            return creature.getFlags().contains(Creature.CreatureFlag.CAN_WALK_ON_WATER);
        }

        @Override
        public boolean canWalkOnLava() {
            return creature.getFlags().contains(Creature.CreatureFlag.CAN_WALK_ON_LAVA);
        }

        @Override
        public boolean canMoveDiagonally() {
            return true;
        }

    }

}
