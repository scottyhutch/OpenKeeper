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

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Trigger;
import toniarts.openkeeper.tools.convert.map.TriggerAction;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric;

/**
 * The creature scope: the Creature Afraid event and the Alter Speed action,
 * with the trees built by hand.
 */
class CreatureTriggerControlTest {

    private static final short VALUE_EQUAL = 0x02 | 0x08; // Constant value, "="

    private final Map<Integer, Trigger> triggers = new HashMap<>();
    private final int[] flags = new int[128];
    private boolean afraid = false;
    private Boolean running = null;

    private CreatureTriggerControl control(int rootId) {
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> method.getName().equals("getTriggers") ? triggers : null);
        ILevelInfo levelInfo = (ILevelInfo) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ILevelInfo.class},
                (proxy, method, args) -> switch (method.getName()) {
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
        IGameController gameController = (IGameController) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IGameController.class},
                (proxy, method, args) -> null);
        IGameTimer gameTimer = () -> 0.0;
        ICreatureController creature = (ICreatureController) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ICreatureController.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "isAfraid" ->
                        afraid;
                    case "setRunning" -> {
                        running = (Boolean) args[0];
                        yield null;
                    }
                    case "isDead", "isHungry", "isSlapped", "isAttacked", "isImprisoned", "isTortured", "isClaimed",
                            "isStunned", "isUnconscious", "isPortalGemInPosession", "isPickedUp" ->
                        false;
                    default ->
                        null;
                });

        CreatureTriggerControl control = new CreatureTriggerControl(gameController, levelInfo, gameTimer, null, null, rootId, (short) 4, null, null, false);
        control.setThing(creature);
        return control;
    }

    private void event(int id, int child, TriggerGeneric.TargetType type) {
        TriggerGeneric trigger = new TriggerGeneric(null);
        base(trigger, id, 0, child, 1);
        invoke(TriggerGeneric.class, trigger, "setType", TriggerGeneric.TargetType.class, type);
    }

    private void flagIs(int id, int child, int flagId, int value) {
        TriggerGeneric trigger = new TriggerGeneric(null);
        base(trigger, id, 0, child, 1);
        invoke(TriggerGeneric.class, trigger, "setType", TriggerGeneric.TargetType.class, TriggerGeneric.TargetType.FLAG);
        invoke(TriggerGeneric.class, trigger, "setTargetValueComparison", TriggerGeneric.ComparisonType.class, TriggerGeneric.ComparisonType.EQUAL_TO);
        data(trigger, "targetId", (short) flagId);
        data(trigger, "flag", (short) 1);
        data(trigger, "value", value);
    }

    private void setFlag(int id, int flagId, int value) {
        TriggerAction trigger = new TriggerAction(null);
        base(trigger, id, 0, 0, 0);
        invoke(TriggerAction.class, trigger, "setType", TriggerAction.ActionType.class, TriggerAction.ActionType.FLAG);
        data(trigger, "flagId", (short) flagId);
        data(trigger, "flag", VALUE_EQUAL);
        data(trigger, "value", value);
    }

    private void alterSpeed(int id, int available) {
        TriggerAction trigger = new TriggerAction(null);
        base(trigger, id, 0, 0, 0);
        invoke(TriggerAction.class, trigger, "setType", TriggerAction.ActionType.class, TriggerAction.ActionType.ALTER_SPEED);
        data(trigger, "available", (short) available);
    }

    private void base(Trigger trigger, int id, int next, int child, int repeat) {
        invoke(Trigger.class, trigger, "setId", int.class, id);
        invoke(Trigger.class, trigger, "setIdNext", int.class, next);
        invoke(Trigger.class, trigger, "setIdChild", int.class, child);
        invoke(Trigger.class, trigger, "setRepeatTimes", short.class, (short) repeat);
        triggers.put(id, trigger);
    }

    private void data(Trigger trigger, String key, Number value) {
        invoke(Trigger.class, trigger, "setUserData", String.class, key, Number.class, value);
    }

    private static void invoke(Class<?> clazz, Object target, String name, Object... typesAndArgs) {
        try {
            Class<?>[] types = new Class<?>[typesAndArgs.length / 2];
            Object[] args = new Object[types.length];
            for (int i = 0; i < types.length; i++) {
                types[i] = (Class<?>) typesAndArgs[i * 2];
                args[i] = typesAndArgs[i * 2 + 1];
            }
            Method method = clazz.getDeclaredMethod(name, types);
            method.setAccessible(true);
            method.invoke(target, args);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void afraidEventFiresWhenTheCreatureTurnsAfraid() {

        // [Creature Afraid -> flag 1 = 1]
        event(1, 2, TriggerGeneric.TargetType.CREATURE_AFRAID);
        setFlag(2, 1, 1);
        CreatureTriggerControl control = control(1);

        control.update(0.25f);
        assertEquals(0, flags[1]);

        afraid = true;
        control.update(0.25f);
        assertEquals(1, flags[1]);
    }

    @Test
    void afraidEventStaysQuietWhileTheCreatureIsNotAfraid() {
        event(1, 2, TriggerGeneric.TargetType.CREATURE_AFRAID);
        setFlag(2, 1, 1);
        CreatureTriggerControl control = control(1);

        control.update(0.25f);
        control.update(0.25f);

        assertEquals(0, flags[1]);
    }

    @Test
    void alterSpeedOneMakesTheCreatureRun() {

        // Flag 0 == 0 -> [Alter speed: run]
        flagIs(1, 2, 0, 0);
        alterSpeed(2, 1);

        control(1).update(0.25f);

        assertTrue(running);
    }

    @Test
    void alterSpeedZeroMakesTheCreatureWalk() {
        flagIs(1, 2, 0, 0);
        alterSpeed(2, 0);

        control(1).update(0.25f);

        assertFalse(running);
    }
}
