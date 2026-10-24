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
package toniarts.openkeeper.game.trigger.creature;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.state.session.PlayerService;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.StealsTriggerFixtures;
import toniarts.openkeeper.tools.convert.map.Trigger;

/**
 * The "creature steals" condition of a creature tree: the actions under it
 * run when the creature has stolen
 */
class CreatureTriggerControlStealsTest {

    private static final short PLAYER_ID = 1;
    private static final int ROOT_TRIGGER_ID = 103;
    private static final int ACTION_TRIGGER_ID = 104;
    private static final int FLAG_ID = 7;

    private final AtomicBoolean stolen = new AtomicBoolean();
    private final int[] flags = new int[16];

    @Test
    void nothingHappensBeforeTheCreatureSteals() {
        CreatureTriggerControl control = buildControl();

        control.update(0.25f);
        control.update(0.25f);

        assertEquals(0, flags[FLAG_ID]);
    }

    @Test
    void theActionsRunWhenTheCreatureSteals() {
        CreatureTriggerControl control = buildControl();
        control.update(0.25f);

        stolen.set(true);
        control.update(0.25f);

        assertEquals(1, flags[FLAG_ID]);
    }

    @Test
    void aCreatureThatHasAlreadyStolenWhenTheTreeStartsIsNotAnEvent() {
        CreatureTriggerControl control = buildControl();
        stolen.set(true);

        control.update(0.25f);

        assertEquals(0, flags[FLAG_ID]);
    }

    @Test
    void stealingIsOneEventNotAState() {
        CreatureTriggerControl control = buildControl();
        control.update(0.25f);
        stolen.set(true);
        control.update(0.25f);
        flags[FLAG_ID] = 0;

        control.update(0.25f);

        assertEquals(0, flags[FLAG_ID]);
    }

    private CreatureTriggerControl buildControl() {
        Map<Integer, Trigger> triggers = new HashMap<>();
        triggers.put(ROOT_TRIGGER_ID, StealsTriggerFixtures.steals(ROOT_TRIGGER_ID, ACTION_TRIGGER_ID));
        triggers.put(ACTION_TRIGGER_ID, StealsTriggerFixtures.setFlag(ACTION_TRIGGER_ID, FLAG_ID, 1));
        IKwdFile kwdFile = stub(IKwdFile.class, (method, args) -> method.getName().equals("getTriggers") ? triggers : null);
        ILevelInfo levelInfo = stub(ILevelInfo.class, (method, args) -> switch (method.getName()) {
            case "getLevelData" ->
                kwdFile;
            case "getFlag" ->
                flags[(Integer) args[0]];
            case "setFlag" -> {
                flags[(Integer) args[0]] = (Integer) args[1];
                yield null;
            }
            default ->
                null;
        });
        ICreatureController thief = stub(ICreatureController.class,
                (method, args) -> method.getName().equals("hasStolen") ? stolen.get() : null);

        CreatureTriggerControl control = new CreatureTriggerControl(stub(IGameController.class, (m, a) -> null), levelInfo,
                stub(IGameTimer.class, (m, a) -> null), stub(IMapController.class, (m, a) -> null),
                stub(ICreaturesController.class, (m, a) -> null), ROOT_TRIGGER_ID, PLAYER_ID,
                stub(PlayerService.class, (m, a) -> null), false);
        control.setThing(thief);
        return control;
    }

    private interface Answer {

        Object answer(Method method, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, Answer answer) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            Object result = answer.answer(method, args);
            if (result == null && method.getReturnType().isPrimitive()) {
                return method.getReturnType() == boolean.class ? (Object) false : (Object) 0;
            }
            return result;
        });
    }
}
