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
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntitySet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.sound.MentorType;
import toniarts.openkeeper.game.state.session.PlayerService;

/**
 * Tells the keeper "A new species of creature has entered your Dungeon" when a
 * creature of a type he has not had before walks in through a Portal.<br>
 * In DKII.EXE the Portal arrival (0x4DC9A0) looks up a per player flag per
 * creature type (0x4C0480). If it is not set, the flag is set and message 0xd
 * is queued, with the Portal as the place the message is about. A type that is
 * already flagged gets no message. The flag is also set, silently, for the
 * creatures a player starts with (0x50B5B0) and for creatures that change
 * hands (0x48D050).<br>
 * We follow the creatures in the entity data: a creature that is new and
 * entering the dungeon is an arrival, any other creature a player has just
 * come to own is only marked as seen. The per level introduction texts that
 * the original raises as message 0xe for some creature types are not handled.
 */
public final class NewSpeciesSystem implements IGameLogicUpdatable {

    private final PlayerService playerService;
    private final EntitySet creatures;
    private final Map<Short, Set<Short>> seenTypesByPlayer = new HashMap<>();

    /**
     * Create the system
     *
     * @param entityData the entity data
     * @param playerService the players to tell
     */
    public NewSpeciesSystem(EntityData entityData, PlayerService playerService) {
        this.playerService = playerService;
        this.creatures = entityData.getEntities(CreatureAi.class, Owner.class, Position.class);
    }

    @Override
    public void start() {
        // Nothing to start
    }

    @Override
    public void stop() {
        creatures.release();
    }

    @Override
    public void processTick(float tpf) {
        creatures.applyChanges();

        // Arrivals, and the creatures a player starts with
        for (Entity entity : creatures.getAddedEntities()) {
            CreatureAi ai = entity.get(CreatureAi.class);
            short ownerId = entity.get(Owner.class).ownerId;
            if (markSeen(ownerId, ai.creatureId) && ai.getCreatureState() == CreatureState.ENTERING_DUNGEON) {
                Vector3f position = entity.get(Position.class).position;
                playerService.showMentorMessage(MentorType.SPECIES_CREATURE_ENTERED.getId(), position, ownerId);
            }
        }

        // Creatures that changed hands
        for (Entity entity : creatures.getChangedEntities()) {
            markSeen(entity.get(Owner.class).ownerId, entity.get(CreatureAi.class).creatureId);
        }
    }

    /**
     * @return {@code true} if the player had not seen the type before
     */
    private boolean markSeen(short ownerId, short creatureId) {
        return seenTypesByPlayer.computeIfAbsent(ownerId, key -> new HashSet<>()).add(creatureId);
    }

}
