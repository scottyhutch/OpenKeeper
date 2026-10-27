/*
 * Copyright (C) 2014-2016 OpenKeeper
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

import com.jme3.math.Vector2f;
import com.jme3.util.SafeArrayList;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.filter.FieldFilter;
import toniarts.openkeeper.utils.Point;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Death;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.PortalAttracted;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.room.ICreatureEntrance;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.listener.RoomListener;
import toniarts.openkeeper.game.sound.ThingSounds;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.tools.convert.map.Variable.CreaturePool;
import toniarts.openkeeper.utils.GameTimeCounter;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Handles creatures spawning, from Portals, Dungeon Hearts...<br>
 * It is confirmed behavior in the original game that multiple portals that have the same counter value will
 * spawn creatures at the same time. Even one could think that since the creature count goes up, we wouldn't
 * be able to spawn more than one creature at the time as the cooldown requirement increases. We replicate
 * this behavior 100% since we actually count the creatures once per tick.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class CreatureSpawnSystem extends GameTimeCounter {

    /**
     * A hero portal the keeper has captured, the "Mercenary Portal", counts as a portal for the creature limit
     */
    public static final short HERO_PORTAL_ROOM_ID = 25;

    private final EntityData entityData;
    private final ICreaturesController creaturesController;
    private final int minimumImpCount;
    private final int entranceCooldownTime;
    private final int initialPortalCapacity;
    private final int additionalPortalCapacity;
    private final int freeImpCoolDownTime;
    private final Map<Short, IPlayerController> playerControllersById;
    private final SafeArrayList<ICreatureEntrance> entrances = new SafeArrayList<>(ICreatureEntrance.class);
    private final IKwdFile kwdFile;
    private final PortalCreatureChoice creatureChoice;
    private boolean uniqueLayouts = false;

    public CreatureSpawnSystem(EntityData entityData, ICreaturesController creaturesController, Collection<IPlayerController> playerControllers,
            Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings, ILevelInfo levelInfo,
            IMapController mapController) {
        this.entityData = entityData;
        this.creaturesController = creaturesController;
        this.creatureChoice = new PortalCreatureChoice(entityData, Utils.getRandom());

        // We need the game state just for the variables
        entranceCooldownTime = (int) gameSettings.get(Variable.MiscVariable.MiscType.ENTRANCE_GENERATION_SPEED_SECONDS).getValue();
        minimumImpCount = (int) gameSettings.get(Variable.MiscVariable.MiscType.MINIMUM_IMP_THRESHOLD).getValue();
        initialPortalCapacity = (int) gameSettings.get(Variable.MiscVariable.MiscType.CREATURES_SUPPORTED_BY_FIRST_PORTAL).getValue();
        additionalPortalCapacity = (int) gameSettings.get(Variable.MiscVariable.MiscType.CREATURES_SUPPORTED_PER_ADDITIONAL_PORTAL).getValue();
        freeImpCoolDownTime = (int) gameSettings.get(Variable.MiscVariable.MiscType.TIME_BEFORE_FREE_IMP_GENERATED_SECONDS).getValue();
        kwdFile = levelInfo.getLevelData();

        // Populate entrance list
        playerControllersById = HashMap.newHashMap(playerControllers.size());
        for (IPlayerController player : playerControllers) {
            playerControllersById.put(player.getKeeper().getId(), player);

            // Add initial rooms
            for (Entry<Room, Set<IRoomController>> keeperRooms : player.getRoomControl().getRoomControllers().entrySet()) {

                // See that should we add
                for (IRoomController genericRoom : keeperRooms.getValue()) {

                    // A bit clumsy to check like this
                    if (!(genericRoom instanceof ICreatureEntrance)) {
                        break;
                    }
                    entrances.add((ICreatureEntrance) genericRoom);
                }
            }

            // Add room listener to get notified of the changes
            // They should be quite rare vs the rate in which we iterate on each tick
            mapController.addListener(player.getKeeper().getId(), new EntranceListener(player.getKeeper().getId()));
        }
    }

    @Override
    public void processTick(float tpf) {
        super.processTick(tpf);

        for (ICreatureEntrance entrance : entrances.getArray()) {
            evaluateAndSpawnCreature(entrance);
        }
    }

    private void evaluateAndSpawnCreature(ICreatureEntrance entrance) {

        // TODO: we should have a listener for destroy that we can remove the room
        if (entrance.isDestroyed()) {
            return;
        }

        double timeSinceLastSpawn = timeElapsed - entrance.getLastSpawnTime();
        IPlayerController player = playerControllersById.get(entrance.getOwnerId());
        boolean spawned = false;
        EntityId entityId = null;
        if (timeSinceLastSpawn >= freeImpCoolDownTime && entrance.isDungeonHeart()) {
            if (player.getCreatureControl().getImpCount() < minimumImpCount) {

                // Spawn imp
                Point entranceCoordinate = entrance.getEntranceCoordinate();
                Vector2f spot = new Vector2f(entranceCoordinate.x, entranceCoordinate.y);
                entityId = creaturesController.spawnCreature(kwdFile.getImp().getCreatureId(), player.getKeeper().getId(), 1, spot, ICreaturesController.SpawnType.PLACE);
                spawned = true;

                // The cast sound is not gated by the free flag
                ThingSounds.playCreateImp(kwdFile, WorldUtils.vector2fToVector3(spot));
            }
        } else if (timeSinceLastSpawn >= getGenerationInterval(entrance)
                && player.getRoomControl().isPortalsOpen() && !isCreatureLimitReached(player)) {

            // Evaluate what creature can we spawn
            // A unique creature called in by a secret room layout comes first
            Map<Integer, CreaturePool> pool = kwdFile.getCreaturePool(player.getKeeper().getId());
            Creature creature = uniqueLayouts
                    ? creatureChoice.chooseUniqueCreature(player, playerControllersById.values(), creaturesController, kwdFile)
                    : null;
            if (creature == null) {
                creature = creatureChoice.chooseCreature(player, entrance, creaturesController,
                        type -> isCreatureAvailableFromPool(type, player, pool) && isCreatureRequirementsSatisfied(type, player));
            }

            // Spawn by the weights, as the original does
            // TODO: also get notified about the evicted and newly introduced creatures
            // Maybe store these under the keeper etc.? For saving purposes
            // The logic classes shouldn't have anything to save
            if (creature != null) {
                short creatureId = creature.getCreatureId();
                Point entranceCoordinate = entrance.getEntranceCoordinate();
                entityId = creaturesController.spawnCreature(creatureId, player.getKeeper().getId(), 1, new Vector2f(entranceCoordinate.x, entranceCoordinate.y), ICreaturesController.SpawnType.ENTRANCE);
                if (entityId != null) {

                    // Only the attracted creatures count towards the limit
                    entityData.setComponent(entityId, new PortalAttracted());
                }
                spawned = true;
            }
        }

        if (spawned) {

            // Reset spawn time
            entrance.onSpawn(timeElapsed, entityId);
        } else if (!entrance.isDungeonHeart() && timeSinceLastSpawn >= getGenerationInterval(entrance)) {

            // As in the original, the Portal's clock restarts on every check, also when no creature came in
            entrance.onGenerationSkipped(timeElapsed);
        }
    }

    /**
     * Get the time between the entrance's creature generation checks. In the original it is the level's
     * entrance generation speed plus the square of the creatures that came in through the Portal since it was
     * claimed, so each Portal slows down on its own.
     *
     * @param entrance the creature entrance
     * @return the generation interval, in seconds
     */
    private double getGenerationInterval(ICreatureEntrance entrance) {
        int creaturesEntered = entrance.getCreaturesEntered();
        return entranceCooldownTime + creaturesEntered * creaturesEntered;
    }

    private boolean isCreatureLimitReached(IPlayerController player) {
        int portals = player.getRoomControl().getTypeCount(kwdFile.getPortal());
        Room heroPortal = kwdFile.getRoomById(HERO_PORTAL_ROOM_ID);
        if (heroPortal != null) {
            portals += player.getRoomControl().getTypeCount(heroPortal);
        }
        return getAttractedCreatureCount(entityData, player.getKeeper().getId())
                >= getPortalCapacity(portals, initialPortalCapacity, additionalPortalCapacity);
    }

    /**
     * Get the number of creatures the portals can attract. As in the original, the first portal supports
     * CREATURES_SUPPORTED_BY_FIRST_PORTAL creatures and each further portal
     * CREATURES_SUPPORTED_PER_ADDITIONAL_PORTAL more.
     *
     * @param portals the number of portals the keeper has, captured hero portals included
     * @param firstPortal the creatures the first portal supports
     * @param perAdditionalPortal the creatures each further portal supports
     * @return the number of creatures the portals can attract
     */
    public static int getPortalCapacity(int portals, int firstPortal, int perAdditionalPortal) {
        if (portals <= 0) {
            return 0;
        }
        return firstPortal + (portals - 1) * perAdditionalPortal;
    }

    /**
     * Get the number of the keeper's living creatures that came in through a portal. As in the original, the
     * creatures the keeper got otherwise, like the ones in the level from the start, don't count towards the
     * portal limit.
     *
     * @param entityData the entities
     * @param playerId the keeper
     * @return the number of attracted creatures
     */
    public static int getAttractedCreatureCount(EntityData entityData, short playerId) {
        int count = 0;
        for (EntityId creature : entityData.findEntities(new FieldFilter(Owner.class, "ownerId", playerId),
                CreatureComponent.class, Owner.class, PortalAttracted.class)) {
            if (entityData.getComponent(creature, Death.class) == null) {
                count++;
            }
        }
        return count;
    }

    private boolean isCreatureRequirementsSatisfied(Creature creature, IPlayerController player) {
        return CreatureAttraction.isSatisfied(creature, player, kwdFile);
    }

    private static boolean isCreatureAvailableFromPool(Creature creature, IPlayerController player, Map<Integer, CreaturePool> pool) {
        CreaturePool creaturePool = pool.get(Short.valueOf(creature.getCreatureId()).intValue());
        if (creaturePool != null) {
            int creatures = player.getCreatureControl().getTypeCount(creature);
            return creaturePool.getValue() > creatures;
        }
        return false;
    }

    @Override
    public void start() {
        uniqueLayouts = PortalCreatureChoice.isUniqueLayoutLevel(kwdFile.getGameLevel().getLvlFlags());

        // The creature types the keepers can attract from the start are not favoured later
        for (IPlayerController player : playerControllersById.values()) {
            if (!isCreatureLimitReached(player)) {
                Map<Integer, CreaturePool> pool = kwdFile.getCreaturePool(player.getKeeper().getId());
                creatureChoice.start(player, creaturesController,
                        type -> isCreatureAvailableFromPool(type, player, pool) && isCreatureRequirementsSatisfied(type, player));
            }
        }
    }

    @Override
    public void stop() {

    }

    /**
     * In DK 2 it is not possible to place spawn points in game, but but we don't know that
     */
    private final class EntranceListener implements RoomListener {

        private final short playerId;

        public EntranceListener(short playerId) {
            this.playerId = playerId;
        }

        @Override
        public void onBuild(IRoomController room) {
            addRoom(room);
        }

        @Override
        public void onCaptured(IRoomController room) {
            //addRoom(room);
        }

        @Override
        public void onCapturedByEnemy(IRoomController room) {
            //removeRoom(room);
        }

        @Override
        public void onSold(IRoomController room) {
            removeRoom(room);
        }

        private void addRoom(IRoomController room) {
            if (room instanceof ICreatureEntrance iCreatureEntrance) {
                entrances.add(iCreatureEntrance);
            }
        }

        private void removeRoom(IRoomController room) {
            if (room instanceof ICreatureEntrance iCreatureEntrance) {
                entrances.remove(iCreatureEntrance);
            }
        }

    }

}
