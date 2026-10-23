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

import com.simsilica.es.EntityId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Room;

/**
 * Chooses the creature a Portal attracts, as the original does. Each creature
 * type that can come gets a weight and the choice is random by the weights:
 * <ul>
 * <li>a creature whose first attraction is the Lair weighs 1, or 3 if it
 * sleeps in a Goblin bed</li>
 * <li>any other creature weighs the capacity of the keeper's rooms of its
 * first attraction, less one for each creature of its type the keeper already
 * has</li>
 * </ul>
 * A type the keeper can attract for the first time during the level is
 * favoured: 200 times on the next generation, then 9 times, 8 times and so on.
 * Nothing comes while the keeper lacks Lair space for the creatures it has.
 * <br>
 * The favour is runtime state per keeper, it is not saved.
 */
public final class PortalCreatureChoice {

    private static final int LAIR_ROOM_ID = 2;
    private static final int PORTAL_ROOM_ID = 3;
    private static final int LIBRARY_ROOM_ID = 6;
    private static final int TRAINING_ROOM_ID = 7;
    private static final int WORKSHOP_ROOM_ID = 10;
    private static final int TORTURE_CHAMBER_ROOM_ID = 12;
    private static final int TEMPLE_ROOM_ID = 13;
    private static final int CASINO_ROOM_ID = 15;
    private static final int HERO_PORTAL_ROOM_ID = 25;
    private static final short GOBLIN_BED_OBJECT_ID = 22;
    private static final int FAVOURED_GENERATIONS = 10;
    private static final double FIRST_GENERATION_FAVOUR = 200;

    private final RandomGenerator random;
    private final Map<Short, Set<Short>> attractableTypesByPlayer = new HashMap<>();
    private final Map<Short, Map<Short, Integer>> favourByPlayer = new HashMap<>();

    public PortalCreatureChoice(RandomGenerator random) {
        this.random = random;
    }

    /**
     * Note the creature types the keeper can attract at the start of the
     * level, they are never favoured
     *
     * @param player the keeper
     * @param creaturesController the creatures controller
     * @param available whether the creature type is in the pool and meets
     * its attraction requirements
     */
    public void start(IPlayerController player, ICreaturesController creaturesController, Predicate<Creature> available) {
        if (getFreeLairSpace(player, creaturesController) >= 0) {
            updateAttractableTypes(player, available, false);
        }
    }

    /**
     * Choose the creature the Portal attracts
     *
     * @param player the keeper
     * @param entrance the Portal
     * @param creaturesController the creatures controller
     * @param available whether the creature type is in the pool and meets
     * its attraction requirements
     * @return the creature type, or {@code null} if none can come
     */
    public Creature chooseCreature(IPlayerController player, IRoomController entrance, ICreaturesController creaturesController,
            Predicate<Creature> available) {
        if (getFreeLairSpace(player, creaturesController) < 0) {
            return null;
        }

        Map<Creature, Double> weights = getCandidateWeights(player, entrance, creaturesController, available);
        if (weights.isEmpty()) {
            return null;
        }

        // Every generation lessens the favour
        Map<Short, Integer> favour = getFavour(player);
        favour.replaceAll((creatureId, generations) -> Math.max(0, generations - 1));

        double total = 0;
        for (double weight : weights.values()) {
            total += weight;
        }
        return pick(weights, random.nextDouble() * total);
    }

    /**
     * Get the creature types the Portal can attract, with their weights
     *
     * @param player the keeper
     * @param entrance the Portal
     * @param creaturesController the creatures controller
     * @param available whether the creature type is in the pool and meets
     * its attraction requirements
     * @return the creature types and their weights
     */
    Map<Creature, Double> getCandidateWeights(IPlayerController player, IRoomController entrance,
            ICreaturesController creaturesController, Predicate<Creature> available) {
        updateAttractableTypes(player, available, true);

        Map<Short, Integer> favour = getFavour(player);
        Map<Creature, Double> weights = new LinkedHashMap<>();
        for (Creature creature : player.getCreatureControl().getTypesAvailable()) {
            if (!isAvailableVia(creature, entrance.getRoom()) || !isAttractable(creature, player, available)) {
                continue;
            }
            int generations = favour.getOrDefault(creature.getCreatureId(), 0);
            double multiplier = generations == FAVOURED_GENERATIONS ? FIRST_GENERATION_FAVOUR : Math.max(1, generations);
            weights.put(creature, getWeight(creature, player) * multiplier);
        }
        return weights;
    }

    /**
     * Mark the creature types the keeper can attract now, the ones that
     * become attractable after the start of the level are favoured
     */
    private void updateAttractableTypes(IPlayerController player, Predicate<Creature> available, boolean favourNew) {
        Set<Short> attractableTypes = attractableTypesByPlayer.computeIfAbsent(player.getKeeper().getId(), id -> new HashSet<>());
        Map<Short, Integer> favour = getFavour(player);
        for (Creature creature : player.getCreatureControl().getTypesAvailable()) {
            if (attractableTypes.contains(creature.getCreatureId()) || !isAttractable(creature, player, available)) {
                continue;
            }
            attractableTypes.add(creature.getCreatureId());
            if (favourNew && getFirstAttractionRoomId(creature) != LAIR_ROOM_ID) {
                favour.put(creature.getCreatureId(), FAVOURED_GENERATIONS);
            }
        }
    }

    private Map<Short, Integer> getFavour(IPlayerController player) {
        return favourByPlayer.computeIfAbsent(player.getKeeper().getId(), id -> new HashMap<>());
    }

    private static boolean isAttractable(Creature creature, IPlayerController player, Predicate<Creature> available) {
        return !creature.getFlags().contains(Creature.CreatureFlag.IS_UNIQUE)
                && available.test(creature)
                && getWeight(creature, player) > 0;
    }

    private static boolean isAvailableVia(Creature creature, Room entrance) {
        return switch (entrance.getId()) {
            case PORTAL_ROOM_ID ->
                creature.getFlags().contains(Creature.CreatureFlag.AVAILABLE_VIA_PORTAL);
            case HERO_PORTAL_ROOM_ID ->
                creature.getFlags2() != null && creature.getFlags2().contains(Creature.CreatureFlag2.AVAILABLE_VIA_HERO_PORTAL);
            default ->
                false;
        };
    }

    /**
     * Get the weight of a creature type for the Portal's choice, before any
     * favour
     *
     * @param creature the creature type
     * @param player the keeper
     * @return the weight, 0 if the creature has no room to come to
     */
    static int getWeight(Creature creature, IPlayerController player) {
        int roomId = getFirstAttractionRoomId(creature);
        if (roomId == LAIR_ROOM_ID) {
            return creature.getLairObjectId() == GOBLIN_BED_OBJECT_ID ? 3 : 1;
        }

        int capacity = 0;
        for (Entry<Room, Set<IRoomController>> rooms : player.getRoomControl().getRoomControllers().entrySet()) {
            if (rooms.getKey().getId() != roomId) {
                continue;
            }
            for (IRoomController room : new ArrayList<>(rooms.getValue())) {
                capacity += getAttractionCapacity(room);
            }
        }
        return Math.max(0, capacity - player.getCreatureControl().getTypeCount(creature));
    }

    /**
     * Get how many creatures a room supports when the Portal chooses its
     * creature. The furnished rooms count their furniture, one more if they
     * have any, except the Torture Chamber. The Temple supports two if it has
     * its hand and a Portal none. The others count their tiles.
     *
     * @param room the room
     * @return the capacity
     */
    static int getAttractionCapacity(IRoomController room) {
        int furniture = room.getFloorFurnitureCount() + room.getWallFurnitureCount();
        return switch (room.getRoom().getId()) {
            case PORTAL_ROOM_ID ->
                0;
            case LIBRARY_ROOM_ID, TRAINING_ROOM_ID, WORKSHOP_ROOM_ID, CASINO_ROOM_ID ->
                furniture > 0 ? furniture + 1 : 0;
            case TORTURE_CHAMBER_ROOM_ID ->
                furniture;
            case TEMPLE_ROOM_ID ->
                room.getFloorFurnitureCount() > 0 ? 2 : 0;
            default ->
                room.getRoomInstance().getCoordinates().size();
        };
    }

    /**
     * Get the free space in the keeper's Lairs, less the space the keeper's
     * creatures without a bed still need. Creatures can come while this is not
     * negative.
     *
     * @param player the keeper
     * @param creaturesController the creatures controller
     * @return the free Lair space
     */
    static int getFreeLairSpace(IPlayerController player, ICreaturesController creaturesController) {
        int space = 0;
        for (Entry<Room, Set<IRoomController>> rooms : player.getRoomControl().getRoomControllers().entrySet()) {
            if (rooms.getKey().getId() != LAIR_ROOM_ID) {
                continue;
            }
            for (IRoomController room : new ArrayList<>(rooms.getValue())) {
                space += room.getMaxCapacity() - room.getUsedCapacity();
            }
        }
        for (Entry<Creature, Set<EntityId>> creatures : player.getCreatureControl().getAllCreatures().entrySet()) {
            if (creatures.getKey().getLairObjectId() == 0) {
                continue;
            }
            for (EntityId entityId : new ArrayList<>(creatures.getValue())) {
                if (!creaturesController.createController(entityId).hasLair()) {
                    space--;
                }
            }
        }
        return space;
    }

    /**
     * Pick a creature type by its weight
     *
     * @param weights the creature types and their weights
     * @param roll a random number from 0 to the total weight
     * @return the creature type
     */
    static Creature pick(Map<Creature, Double> weights, double roll) {
        double cumulative = 0;
        Creature last = null;
        for (Entry<Creature, Double> weight : weights.entrySet()) {
            cumulative += weight.getValue();
            last = weight.getKey();
            if (roll < cumulative) {
                return last;
            }
        }
        return last;
    }

    private static int getFirstAttractionRoomId(Creature creature) {
        Creature.Attraction[] attractions = creature.getAttractions();
        if (attractions == null || attractions.length == 0) {
            return 0;
        }

        // The original takes the room of the first attraction whatever its type
        return attractions[0].getRoomId() & 0xFF;
    }

}
