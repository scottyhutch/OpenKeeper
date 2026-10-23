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
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureMood;
import toniarts.openkeeper.game.component.CreatureTortured;
import toniarts.openkeeper.game.component.Death;
import toniarts.openkeeper.game.component.InHand;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.room.AbstractRoomController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.utils.GameTimeCounter;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Handles creatures torturing. When enough... persuasion has been received... join the player army
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class CreatureTorturingSystem extends GameTimeCounter {

    private final EntityData entityData;
    private final IKwdFile kwdFile;
    private final ICreaturesController creaturesController;
    private final IMapController mapController;
    private final EntitySet torturedEntities;
    private final Map<EntityId, IRoomController> chambersByVictim = new HashMap<>();

    public CreatureTorturingSystem(EntityData entityData, IKwdFile kwdFile, ICreaturesController creaturesController, IMapController mapController) {
        this.entityData = entityData;
        this.kwdFile = kwdFile;
        this.creaturesController = creaturesController;
        this.mapController = mapController;

        // Have the position also here, since the player may move tortured entities between torture rooms, kinda still tortured but not counting towards death at the time
        torturedEntities = entityData.getEntities(CreatureTortured.class, CreatureComponent.class, Position.class, Owner.class);
    }

    @Override
    public void processTick(float tpf) {
        super.processTick(tpf);
        // Add new & remove old
        if (torturedEntities.applyChanges()) {
            processRemovedEntities(torturedEntities.getRemovedEntities());
        }

        // Process ticks
        for (Entity entity : torturedEntities) {
            CreatureTortured creatureTortured = entity.get(CreatureTortured.class);
            if (creatureTortured.tortureCheckTime >= timeElapsed) {
                continue;
            }

            IRoomController chamber = getChamber(entity);
            if (chamber == null) {
                continue;
            }

            CreatureComponent creatureComponent = entity.get(CreatureComponent.class);
            Owner owner = entity.get(Owner.class);
            short playerId = chamber.getRoomInstance().getOwnerId();

            // Like in DK II, the torture works once a second
            if ((int) (creatureTortured.timeTortured + tpf) > (int) creatureTortured.timeTortured) {
                tortureForASecond(entity.getId(), creatureComponent, owner.ownerId == playerId);
            }

            if (creatureTortured.timeTortured + tpf >= creatureComponent.tortureTimeToConvert) {
                if (owner.ownerId != playerId) {

                    // Convert!
                    entityData.removeComponent(entity.getId(), CreatureTortured.class);
                    leaveDevice(entity.getId());
                    creaturesController.createController(entity.getId()).convertCreature(playerId);
                    continue;
                }
            }

            entity.set(new CreatureTortured(creatureTortured.timeTortured + tpf, timeElapsed, creatureTortured.healthCheckTime, creatureTortured.device));
        }
    }

    /**
     * A second of torture. Like in DK II, the keeper's own victim gets its
     * torture mood change, which a Mistress enjoys. The health change is done
     * by the health system.
     */
    private void tortureForASecond(EntityId victim, CreatureComponent creatureComponent, boolean ownCreature) {
        if (!ownCreature) {
            return;
        }
        Creature creature = kwdFile.getCreature(creatureComponent.creatureId);
        CreatureMood mood = entityData.getComponent(victim, CreatureMood.class);
        if (creature == null || mood == null || creature.getAttributes().getTortureMoodChange() == 0
                || (creature.getFlags2() != null && creature.getFlags2().contains(Creature.CreatureFlag2.IS_EMOTIONLESS))) {
            return;
        }
        entityData.setComponent(victim, mood.add(CreatureMood.REASON_OTHER, creature.getAttributes().getTortureMoodChange()));
    }

    private IRoomController getChamber(Entity entity) {
        IRoomController chamber = chambersByVictim.get(entity.getId());
        if (chamber == null) {
            chamber = mapController.getRoomControllerByCoordinates(WorldUtils.vectorToPoint(entity.get(Position.class).position));
            if (chamber == null || !chamber.hasObjectControl(AbstractRoomController.ObjectType.TORTUREE)) {
                return null;
            }
            chambersByVictim.put(entity.getId(), chamber);
        }
        return chamber;
    }

    /**
     * The victim is no longer tortured: it died, was picked up, or the chamber
     * was sold or destroyed, which sets its victims free
     */
    private void processRemovedEntities(Set<Entity> entities) {
        for (Entity entity : entities) {
            EntityId victim = entity.getId();
            IRoomController chamber = chambersByVictim.get(victim);
            if (chamber == null) {
                continue;
            }
            leaveDevice(victim);

            if (!chamber.getRoomInstance().isDestroyed()
                    || entityData.getComponent(victim, Death.class) != null
                    || entityData.getComponent(victim, InHand.class) != null
                    || entityData.getComponent(victim, CreatureComponent.class) == null) {
                continue;
            }

            // Set free
            Owner owner = entityData.getComponent(victim, Owner.class);
            entityData.setComponent(victim, new Owner(owner.ownerId, owner.ownerId));
            creaturesController.createController(victim).getStateMachine().changeState(CreatureState.IDLE);
        }
    }

    private void leaveDevice(EntityId victim) {
        IRoomController chamber = chambersByVictim.remove(victim);
        if (chamber != null) {
            chamber.getObjectControl(AbstractRoomController.ObjectType.TORTUREE).removeItem(victim);
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        torturedEntities.release();
        chambersByVictim.clear();
    }

}
