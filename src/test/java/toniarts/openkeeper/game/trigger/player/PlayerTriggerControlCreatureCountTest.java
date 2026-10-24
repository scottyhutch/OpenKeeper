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
package toniarts.openkeeper.game.trigger.player;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureExperience;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.player.PlayerCreatureControl;
import toniarts.openkeeper.game.controller.player.PlayerStatsControl;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.state.session.PlayerService;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureCountTriggerFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Trigger;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric.ComparisonType;

/**
 * The "player creatures at level" and "player creatures dying" conditions of a
 * player tree
 */
class PlayerTriggerControlCreatureCountTest {

    private static final short PLAYER_ID = 3;
    private static final int ROOT_TRIGGER_ID = 205;
    private static final int ACTION_TRIGGER_ID = 206;
    private static final int FLAG_ID = 7;

    private final EntityData entityData = new DefaultEntityData();
    private final Creature imp = CreatureCountTriggerFixtures.creature((short) 1);
    private final Creature warlock = CreatureCountTriggerFixtures.creature((short) 2);
    private final PlayerCreatureControl creatures = new PlayerCreatureControl(new Keeper(false, PLAYER_ID), imp,
            List.of(imp, warlock), entityData);
    private final int[] flags = new int[16];

    @Test
    void aCreatureOfTheLevelSatisfiesAtLevel() {
        PlayerTriggerControl control = buildControl(CreatureCountTriggerFixtures.creaturesAtLevel(ROOT_TRIGGER_ID,
                ACTION_TRIGGER_ID, ComparisonType.GREATER_OR_EQUAL_TO, (short) 2, 1));
        add(warlock, 2, false);

        control.update(0.25f);

        assertEquals(1, flags[FLAG_ID]);
    }

    @Test
    void aCreatureBelowTheLevelDoesNot() {
        PlayerTriggerControl control = buildControl(CreatureCountTriggerFixtures.creaturesAtLevel(ROOT_TRIGGER_ID,
                ACTION_TRIGGER_ID, ComparisonType.GREATER_OR_EQUAL_TO, (short) 2, 1));
        add(warlock, 1, false);

        control.update(0.25f);

        assertEquals(0, flags[FLAG_ID]);
    }

    @Test
    void aHighLevelWorkerDoesNot() {
        PlayerTriggerControl control = buildControl(CreatureCountTriggerFixtures.creaturesAtLevel(ROOT_TRIGGER_ID,
                ACTION_TRIGGER_ID, ComparisonType.GREATER_OR_EQUAL_TO, (short) 2, 1));
        add(imp, 5, true);

        control.update(0.25f);

        assertEquals(0, flags[FLAG_ID]);
    }

    @Test
    void anUnconsciousCreatureIsDying() {
        PlayerTriggerControl control = buildControl(CreatureCountTriggerFixtures.creaturesDying(ROOT_TRIGGER_ID,
                ACTION_TRIGGER_ID, ComparisonType.GREATER_OR_EQUAL_TO, 1));
        EntityId dying = add(warlock, 1, false);
        entityData.setComponent(dying, new Unconscious(0));

        control.update(0.25f);

        assertEquals(1, flags[FLAG_ID]);
    }

    @Test
    void conciousCreaturesAreNotDying() {
        PlayerTriggerControl control = buildControl(CreatureCountTriggerFixtures.creaturesDying(ROOT_TRIGGER_ID,
                ACTION_TRIGGER_ID, ComparisonType.GREATER_OR_EQUAL_TO, 1));
        add(warlock, 1, false);

        control.update(0.25f);

        assertEquals(0, flags[FLAG_ID]);
    }

    private EntityId add(Creature creature, int level, boolean worker) {
        EntityId entity = entityData.createEntity();
        CreatureComponent component = new CreatureComponent();
        component.creatureId = creature.getCreatureId();
        component.worker = worker;
        entityData.setComponent(entity, component);
        entityData.setComponent(entity, new CreatureExperience(level, 0, 0, 0, 0));
        creatures.onCreatureAdded(entity, creature);
        return entity;
    }

    private PlayerTriggerControl buildControl(Trigger root) {
        Map<Integer, Trigger> triggers = new HashMap<>();
        triggers.put(ROOT_TRIGGER_ID, root);
        triggers.put(ACTION_TRIGGER_ID, CreatureCountTriggerFixtures.setFlag(ACTION_TRIGGER_ID, FLAG_ID, 1));
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
        IPlayerController playerController = stub(IPlayerController.class, (method, args) -> switch (method.getName()) {
            case "getCreatureControl" ->
                creatures;
            case "getStatsControl" ->
                new PlayerStatsControl();
            default ->
                null;
        });
        IGameController gameController = stub(IGameController.class,
                (method, args) -> method.getName().equals("getPlayerController") ? playerController : null);

        return new PlayerTriggerControl(gameController, levelInfo, stub(IGameTimer.class, (m, a) -> null),
                stub(IMapController.class, (m, a) -> null), stub(ICreaturesController.class, (m, a) -> null),
                ROOT_TRIGGER_ID, PLAYER_ID, stub(PlayerService.class, (m, a) -> null));
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
