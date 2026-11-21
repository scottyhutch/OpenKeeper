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

import com.badlogic.gdx.ai.pfa.GraphPath;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import toniarts.openkeeper.game.component.AttackTarget;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.Damage;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.DoorViewState;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.PortalGem;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.TrapComponent;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.navigation.steering.SteeringUtils;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Sends a summoned Horned Reaper for the Portal Gem at the end of the level:
 * he walks to it and picks it up, and that is the end of his job. The Reaper's
 * hunt is in his own creature states (REAPER_HUNT, REAPER_WANDER and
 * REAPER_LEAVE), the gem isn't part of it. On his walk to the gem he destroys
 * the traps and unlocks his keeper's doors on his tile and the four next to it
 * (0x474F80).
 */
final class ReaperAi implements SummonedReaperSystem.ReaperDecisions {

    private static final int GEM_RADIUS = 1;
    private static final float REACH_SQUARED = 2.25f;

    private final EntityData entityData;
    private final ICreaturesController creaturesController;
    private final INavigationService navigationService;
    private final IEntityPositionLookup entityPositionLookup;

    ReaperAi(EntityData entityData, ICreaturesController creaturesController, INavigationService navigationService,
            IEntityPositionLookup entityPositionLookup) {
        this.entityData = entityData;
        this.creaturesController = creaturesController;
        this.navigationService = navigationService;
        this.entityPositionLookup = entityPositionLookup;
    }

    @Override
    public void fetchPortalGem(EntityId reaper, EntityId gem) {
        Position gemPosition = entityData.getComponent(gem, Position.class);
        if (gemPosition == null || entityData.getComponent(reaper, PortalGem.class) != null) {
            return;
        }

        Vector3f reaperPosition = entityData.getComponent(reaper, Position.class).position;
        Point from = WorldUtils.vectorToPoint(reaperPosition);
        Point gemTile = WorldUtils.vectorToPoint(gemPosition.position);
        ICreatureController reaperController = creaturesController.createController(reaper);
        Navigation navigation = entityData.getComponent(reaper, Navigation.class);
        if (from.equals(gemTile) || (navigation == null && isNextTo(reaperPosition, gemPosition.position))) {
            pickUpPortalGem(reaper, reaperController, gem);
            return;
        }

        clearTheWay(reaper, from);

        // Keep him on his way, whatever else he might think of
        if (navigation != null && Math.abs(navigation.target.x - gemTile.x) <= GEM_RADIUS
                && Math.abs(navigation.target.y - gemTile.y) <= GEM_RADIUS) {
            return;
        }
        walkToPortalGem(reaper, reaperController, from, gemTile);
    }

    /**
     * On each tick of his walk to the gem the Reaper clears his tile and the
     * four next to it, like DK II's state for that walk (0x474F80): a built
     * trap is destroyed, whoever owns it, and a locked door of his own keeper
     * is unlocked. A blueprint is left alone.
     */
    private void clearTheWay(EntityId reaper, Point from) {
        short ownerId = entityData.getComponent(reaper, Owner.class).ownerId;
        for (Point tile : new Point[]{from, new Point(from.x - 1, from.y), new Point(from.x + 1, from.y),
            new Point(from.x, from.y - 1), new Point(from.x, from.y + 1)}) {
            for (EntityId entityId : entityPositionLookup.getEntitiesInLocation(tile)) {
                Health health = entityData.getComponent(entityId, Health.class);
                if (entityData.getComponent(entityId, TrapComponent.class) != null && health != null) {
                    entityData.setComponent(entityId, new Damage(health.health));
                    continue;
                }
                DoorComponent door = entityData.getComponent(entityId, DoorComponent.class);
                if (door != null && door.locked && !door.blueprint && entityData.getComponent(entityId, Owner.class).ownerId == ownerId) {
                    entityData.setComponent(entityId, new DoorComponent(door.doorId, false, false, door.secret));
                    DoorViewState viewState = entityData.getComponent(entityId, DoorViewState.class);
                    if (viewState != null) {
                        entityData.setComponent(entityId, new DoorViewState(viewState.doorId, false, viewState.blueprint, viewState.open));
                    }
                }
            }
        }
    }

    /**
     * Has he stopped so close to the gem that he can reach out for it. He may
     * not be able to stand on its tile, or the steering may stop short of it.
     */
    private static boolean isNextTo(Vector3f reaperPosition, Vector3f gemPosition) {
        float dx = reaperPosition.x - gemPosition.x;
        float dz = reaperPosition.z - gemPosition.z;
        return dx * dx + dz * dz <= REACH_SQUARED;
    }

    /**
     * Walks to the gem, or next to it if its tile can't be entered
     */
    private void walkToPortalGem(EntityId reaper, ICreatureController reaperController, Point from, Point gemTile) {
        Point destination = gemTile;
        GraphPath<IMapTileInformation> path = navigationService.findPath(from, destination, reaperController);
        if (path == null) {
            destination = navigationService.findRandomAccessibleTile(gemTile, GEM_RADIUS, reaperController);
            path = destination != null ? navigationService.findPath(from, destination, reaperController) : null;
        }
        if (path != null) {
            reaperController.getStateMachine().changeState(CreatureState.WANDER);
            entityData.setComponent(reaper, new Navigation(destination, null, SteeringUtils.pathToList(path)));
        }
    }

    /**
     * The gem is gone from the floor, the Reaper holds it and stands still
     */
    private void pickUpPortalGem(EntityId reaper, ICreatureController reaperController, EntityId gem) {
        entityData.removeEntity(gem);
        entityData.setComponent(reaper, new PortalGem());
        reaperController.unassingCurrentTask();
        entityData.removeComponent(reaper, AttackTarget.class);
        entityData.removeComponent(reaper, CreatureAi.class);
        entityData.removeComponent(reaper, Navigation.class);
    }

}
