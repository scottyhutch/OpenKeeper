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
package toniarts.openkeeper.game.state.loop;

import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.TaskComponent;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.task.Task;
import toniarts.openkeeper.game.task.TaskType;

/**
 * Debug only: logs what the keepers' workers are doing and why they might be
 * idle. Enable with -Dok.impDebug=true. Reads game state only, the timers
 * live here. Grep for IMPDEBUG.
 */
public final class ImpDebug {

    public static final boolean ENABLED = Boolean.getBoolean("ok.impDebug");

    private static final Logger logger = Logger.getLogger(ImpDebug.class.getName());
    private static final float INTERVAL = 5f;
    private static final float IDLE_AFTER = 5f;

    private final EntitySet creatures;
    private final EntityData entityData;
    private final ICreaturesController creaturesController;
    private final Map<EntityId, Long> lastTask = new HashMap<>();
    private final Map<EntityId, Float> lastChange = new HashMap<>();
    private final Map<EntityId, String> rejections = new HashMap<>();
    private final Map<EntityId, Float> lastNote = new HashMap<>();
    private float time;
    private float nextReport = INTERVAL;

    public ImpDebug(EntityData entityData, ICreaturesController creaturesController) {
        this.entityData = entityData;
        this.creaturesController = creaturesController;
        creatures = entityData.getEntities(CreatureAi.class, Owner.class);
    }

    /**
     * The unemployed worker search found no task for this worker, remember why
     *
     * @param worker the worker
     * @param tasks the open tasks of its keeper
     */
    public void noteUnemployed(ICreatureController worker, Set<Task> tasks) {
        // The path checks are costly, look at each worker only now and then
        Float last = lastNote.get(worker.getEntityId());
        if (last != null && time - last < INTERVAL) {
            return;
        }
        lastNote.put(worker.getEntityId(), time);
        int full = 0;
        int invalid = 0;
        int unreachable = 0;
        for (Task task : tasks) {
            if (task.isFull()) {
                full++;
            } else if (!task.isValid(worker)) {
                invalid++;
            } else if (!task.isReachable(worker)) {
                unreachable++;
            }
        }
        rejections.put(worker.getEntityId(), tasks.isEmpty() ? "no open tasks"
                : "candidates=" + tasks.size() + " full=" + full + " invalid=" + invalid + " unreachable=" + unreachable);
    }

    /**
     * Call once per logic tick
     *
     * @param tpf time since the last tick
     * @param taskQueues the open tasks by keeper
     */
    public void update(float tpf, Map<Short, Set<Task>> taskQueues) {
        time += tpf;
        creatures.applyChanges();
        for (Entity entity : creatures) {
            TaskComponent tc = entityData.getComponent(entity.getId(), TaskComponent.class);
            Long id = tc != null ? tc.taskId : Long.valueOf(-1);
            if (!id.equals(lastTask.put(entity.getId(), id))) {
                lastChange.put(entity.getId(), time);
            }
        }
        if (time < nextReport) {
            return;
        }
        nextReport = time + INTERVAL;
        Map<Short, Boolean> reported = new HashMap<>();
        for (Entity entity : creatures) {
            short owner = entity.get(Owner.class).ownerId;
            if (!taskQueues.containsKey(owner)) {
                continue;
            }
            ICreatureController creature = creaturesController.createController(entity.getId());
            if (!creature.isWorker()) {
                continue;
            }
            Set<Task> open = taskQueues.get(owner);
            if (reported.put(owner, true) == null) {
                Map<TaskType, Integer> counts = new EnumMap<>(TaskType.class);
                for (Task task : open) {
                    counts.merge(task.getTaskType(), 1, Integer::sum);
                }
                logger.warning(() -> "IMPDEBUG t=" + (int) time + " keeper=" + owner + " openTasks=" + open.size() + " " + counts);
            }
            TaskComponent tc = entityData.getComponent(entity.getId(), TaskComponent.class);
            float since = time - lastChange.getOrDefault(entity.getId(), 0f);
            logger.warning(() -> "IMPDEBUG t=" + (int) time + " keeper=" + owner + " worker=" + entity.getId().getId()
                    + " state=" + creature.getStateMachine().getCurrentState()
                    + " task=" + (tc != null ? tc.taskType + "@" + (tc.targetLocation != null ? tc.targetLocation : tc.targetEntity) : "none")
                    + " since=" + String.format("%.1f", since) + "s");
            if (tc == null && since > IDLE_AFTER && !open.isEmpty()) {
                logger.warning(() -> "IMPDEBUG idle t=" + (int) time + " keeper=" + owner + " worker=" + entity.getId().getId()
                        + " idleFor=" + String.format("%.1f", since) + "s open=" + open.size()
                        + " lastSearch=" + rejections.getOrDefault(entity.getId(), "none yet"));
            }
        }
    }
}
