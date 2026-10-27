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

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.sound.MentorType;
import toniarts.openkeeper.game.state.session.PlayerService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * "A new species of creature has entered your Dungeon". DKII.EXE (0x4DC9A0)
 * says it only for the first creature of a type that arrives through a Portal;
 * the types a player starts with, or has got otherwise, are already seen
 */
class NewSpeciesSystemTest {

    private static final short KEEPER = 1;
    private static final short WARLOCK = 3;
    private static final short DARK_ELF = 6;
    private static final short INTRODUCED = 5; // Has its own introduction on the level
    private static final int SPEECH = MentorType.SPECIES_CREATURE_ENTERED.getId();

    private final List<String> messages = new ArrayList<>();
    private final EntityData entityData = new DefaultEntityData();
    private final PlayerService playerService = (PlayerService) Proxy.newProxyInstance(PlayerService.class.getClassLoader(),
            new Class<?>[]{PlayerService.class}, (proxy, method, args) -> {
                if (method.getName().equals("showMentorMessage")) {
                    messages.add(args[0] + "@" + args[1] + ">p" + args[2]);
                } else if (method.getName().equals("playSpeech")) {
                    messages.add("level speech " + args[0] + ">p" + args[4]);
                }
                return null;
            });
    private final NewSpeciesSystem system = new NewSpeciesSystem(entityData, playerService, Map.of(INTRODUCED, 9));

    @Test
    void firstArrivalOfATypeIsAnnouncedAtTheCreature() {
        creature(KEEPER, DARK_ELF, CreatureState.ENTERING_DUNGEON, new Vector3f(3, 0, 4));
        system.processTick(0.25f);

        assertEquals(List.of(SPEECH + "@" + new Vector3f(3, 0, 4) + ">p1"), messages);
    }

    @Test
    void typeWithAnIntroductionPlaysTheLevelSpeechOncePerType() {
        creature(KEEPER, INTRODUCED, CreatureState.ENTERING_DUNGEON, new Vector3f(3, 0, 4));
        system.processTick(0.25f);
        assertEquals(List.of("level speech 9>p1"), messages);
        messages.clear();

        creature(KEEPER, INTRODUCED, CreatureState.ENTERING_DUNGEON, new Vector3f(3, 0, 4));
        system.processTick(0.25f);
        assertTrue(messages.isEmpty());
    }

    @Test
    void laterArrivalsOfTheSameTypeAreNotAnnounced() {
        creature(KEEPER, DARK_ELF, CreatureState.ENTERING_DUNGEON, new Vector3f(3, 0, 4));
        system.processTick(0.25f);
        messages.clear();

        creature(KEEPER, DARK_ELF, CreatureState.ENTERING_DUNGEON, new Vector3f(3, 0, 4));
        system.processTick(0.25f);

        assertTrue(messages.isEmpty());
    }

    @Test
    void anotherTypeIsAnnouncedAgain() {
        creature(KEEPER, DARK_ELF, CreatureState.ENTERING_DUNGEON, new Vector3f(3, 0, 4));
        system.processTick(0.25f);
        messages.clear();

        creature(KEEPER, WARLOCK, CreatureState.ENTERING_DUNGEON, new Vector3f(3, 0, 4));
        system.processTick(0.25f);

        assertEquals(1, messages.size());
    }

    @Test
    void typesTheKeeperStartsWithAreAlreadySeen() {
        creature(KEEPER, DARK_ELF, CreatureState.IDLE, new Vector3f(1, 0, 1));
        system.processTick(0.25f);

        creature(KEEPER, DARK_ELF, CreatureState.ENTERING_DUNGEON, new Vector3f(3, 0, 4));
        system.processTick(0.25f);

        assertTrue(messages.isEmpty());
    }

    @Test
    void typesSeenAreKeptPerKeeper() {
        creature(KEEPER, DARK_ELF, CreatureState.ENTERING_DUNGEON, new Vector3f(3, 0, 4));
        system.processTick(0.25f);
        messages.clear();

        creature((short) 2, DARK_ELF, CreatureState.ENTERING_DUNGEON, new Vector3f(8, 0, 8));
        system.processTick(0.25f);

        assertEquals(List.of(SPEECH + "@" + new Vector3f(8, 0, 8) + ">p2"), messages);
    }

    @Test
    void creatureTakenOverCountsAsSeenWithoutAMessage() {
        EntityId stolen = creature((short) 2, DARK_ELF, CreatureState.IDLE, new Vector3f(1, 0, 1));
        system.processTick(0.25f);

        entityData.setComponent(stolen, new Owner(KEEPER, KEEPER));
        system.processTick(0.25f);
        assertTrue(messages.isEmpty());

        creature(KEEPER, DARK_ELF, CreatureState.ENTERING_DUNGEON, new Vector3f(3, 0, 4));
        system.processTick(0.25f);
        assertTrue(messages.isEmpty());
    }

    private EntityId creature(short owner, short creatureId, CreatureState state, Vector3f position) {
        EntityId id = entityData.createEntity();
        entityData.setComponents(id, new CreatureAi(0, state, creatureId), new Owner(owner, owner),
                new Position(0, position));
        return id;
    }

}
