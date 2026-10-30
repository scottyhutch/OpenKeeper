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

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.TaskComponent;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.player.PlayerManufactureControl;
import toniarts.openkeeper.game.task.TaskType;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Manufacture points drop once a second while no creature of the keeper is
 * manufacturing. The shipped levels lose 1 point a second, the logic runs 4
 * ticks a second.
 */
class ManufacturePointLossSystemTest {

    private static final short KEEPER = 3;
    private static final short OTHER_KEEPER = 4;
    private static final float TICK = 0.25f;
    private static final int POINT_LOSS = 1;

    private EntityData entityData;
    private PlayerManufactureControl manufactureControl;
    private ManufacturePointLossSystem system;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        manufactureControl = new PlayerManufactureControl();
        manufactureControl.manufacture(10, 1500);
        system = new ManufacturePointLossSystem(entityData, Map.of(KEEPER, player(manufactureControl)), POINT_LOSS);
    }

    @Test
    void pointsDropOnceASecondWithoutWorkers() {
        runSeconds(3);

        assertEquals(7, manufactureControl.getProgress());
    }

    @Test
    void pointsDoNotDropBelowZero() {
        runSeconds(20);

        assertEquals(0, manufactureControl.getProgress());
    }

    @Test
    void pointsStayWhileACreatureManufactures() {
        addCreature(KEEPER, TaskType.MANUFACTURE, true);

        runSeconds(3);

        assertEquals(10, manufactureControl.getProgress());
    }

    @Test
    void creaturesOnTheWayOrOfOtherKeepersDoNotCount() {
        addCreature(KEEPER, TaskType.MANUFACTURE, false);
        addCreature(KEEPER, TaskType.RESEARCH, true);
        addCreature(OTHER_KEEPER, TaskType.MANUFACTURE, true);

        runSeconds(3);

        assertEquals(7, manufactureControl.getProgress());
    }

    @Test
    void pointsDropAgainWhenTheWorkerLeaves() {
        EntityId worker = addCreature(KEEPER, TaskType.MANUFACTURE, true);
        runSeconds(2);
        entityData.removeComponent(worker, TaskComponent.class);

        runSeconds(2);

        assertEquals(8, manufactureControl.getProgress());
    }

    private void runSeconds(int seconds) {
        for (int i = 0; i < seconds * 4; i++) {
            system.processTick(TICK);
        }
    }

    private EntityId addCreature(short owner, TaskType taskType, boolean taskStarted) {
        EntityId entityId = entityData.createEntity();
        entityData.setComponent(entityId, new Owner(owner, owner));
        entityData.setComponent(entityId, new TaskComponent(1, null, null, taskType, 0, taskStarted));
        return entityId;
    }

    private static IPlayerController player(PlayerManufactureControl manufactureControl) {
        return (IPlayerController) Proxy.newProxyInstance(IPlayerController.class.getClassLoader(),
                new Class<?>[]{IPlayerController.class}, (proxy, method, args) -> {
                    if (!method.getName().equals("getManufactureControl")) {
                        throw new UnsupportedOperationException(method.getName());
                    }
                    return manufactureControl;
                });
    }

}
