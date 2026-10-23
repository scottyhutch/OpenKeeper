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
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import toniarts.openkeeper.game.component.CreatureMeleeAttack;
import toniarts.openkeeper.game.component.CreatureViewState;
import toniarts.openkeeper.game.component.ObjectViewState;
import toniarts.openkeeper.game.component.ObjectViewState.GameObjectAnimState;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.TaskComponent;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.utils.GameTimeCounter;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Makes the training dummy recoil from the trainees' blows. In DK II a trainee
 * swings at the dummy one blow at a time, and the dummy plays its recoil, its
 * first additional resource, when the blow lands at the end of the swing. A
 * swing takes the creature's melee recharge time
 */
public final class TrainingDummySystem extends GameTimeCounter {

    private final EntityData entityData;
    private final IKwdFile kwdFile;
    private final IMapController mapController;
    private final EntitySet trainees;
    private final Map<EntityId, Double> nextBlows = new HashMap<>();
    private final Map<EntityId, Double> recoilEnds = new HashMap<>();

    public TrainingDummySystem(EntityData entityData, IKwdFile kwdFile, IMapController mapController) {
        this.entityData = entityData;
        this.kwdFile = kwdFile;
        this.mapController = mapController;

        trainees = entityData.getEntities(TaskComponent.class, CreatureViewState.class, CreatureMeleeAttack.class);
    }

    @Override
    public void processTick(float tpf) {
        super.processTick(tpf);

        if (trainees.applyChanges()) {
            for (Entity entity : trainees.getRemovedEntities()) {
                nextBlows.remove(entity.getId());
            }
        }

        for (Entity entity : trainees) {
            swing(entity);
        }

        endRecoils();
    }

    private void swing(Entity trainee) {
        TaskComponent task = trainee.get(TaskComponent.class);
        CreatureViewState view = trainee.get(CreatureViewState.class);

        // Swinging at the dummy, not on the way there
        if (task.taskType != TaskType.TRAIN || task.targetLocation == null || view.state != Creature.AnimationType.MELEE_ATTACK) {
            nextBlows.remove(trainee.getId());
            return;
        }

        double swingTime = Math.max(trainee.get(CreatureMeleeAttack.class).rechargeTime, 0.1f);
        Double nextBlow = nextBlows.get(trainee.getId());
        if (nextBlow == null) {
            nextBlows.put(trainee.getId(), timeElapsed + swingTime);
        } else if (timeElapsed >= nextBlow) {
            nextBlows.put(trainee.getId(), nextBlow + swingTime);
            hitDummy(task.targetLocation);
        }
    }

    private void hitDummy(Point dummyTile) {
        IRoomController room = mapController.getRoomControllerByCoordinates(dummyTile);
        if (room == null) {
            return;
        }

        for (EntityId furniture : room.getFloorFurniture()) {
            Position position = entityData.getComponent(furniture, Position.class);
            ObjectViewState view = entityData.getComponent(furniture, ObjectViewState.class);
            if (position == null || view == null || !dummyTile.equals(WorldUtils.vectorToPoint(position.position))) {
                continue;
            }

            ArtResource recoil = getRecoil(kwdFile.getObject(view.objectId));
            if (recoil == null) {
                continue;
            }

            // A new state each blow, so the recoil starts over
            entityData.setComponent(furniture, new ObjectViewState(view.objectId, view.state,
                    GameObjectAnimState.ADDITIONAL_RESOURCE_1, view.visible));
            recoilEnds.put(furniture, timeElapsed + getAnimationTime(recoil));
        }
    }

    private void endRecoils() {
        Iterator<Map.Entry<EntityId, Double>> iterator = recoilEnds.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<EntityId, Double> recoilEnd = iterator.next();
            if (timeElapsed < recoilEnd.getValue()) {
                continue;
            }

            iterator.remove();
            ObjectViewState view = entityData.getComponent(recoilEnd.getKey(), ObjectViewState.class);
            if (view != null) {
                entityData.setComponent(recoilEnd.getKey(), new ObjectViewState(view.objectId, view.state,
                        GameObjectAnimState.MESH_RESOURCE, view.visible));
            }
        }
    }

    /**
     * The training dummy's recoil, its first additional resource
     *
     * @param object the object hit
     * @return the recoil animation, {@code null} if the object has none
     */
    static ArtResource getRecoil(GameObject object) {
        if (object == null) {
            return null;
        }
        List<ArtResource> resources = object.getAdditionalResources();
        if (resources == null || resources.isEmpty() || resources.get(0) == null
                || resources.get(0).getType() != ArtResource.ArtResourceType.ANIMATING_MESH) {
            return null;
        }
        return resources.get(0);
    }

    private static double getAnimationTime(ArtResource animation) {
        Number frames = animation.getData(ArtResource.KEY_FRAMES);
        Number fps = animation.getData(ArtResource.KEY_FPS);
        if (frames == null || fps == null || fps.intValue() <= 0) {
            return 0;
        }
        return frames.doubleValue() / fps.doubleValue();
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {
        trainees.release();
        nextBlows.clear();
        recoilEnds.clear();
    }

}
