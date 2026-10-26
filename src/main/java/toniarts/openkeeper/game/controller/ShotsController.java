/*
 * Copyright (C) 2014-2024 OpenKeeper
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
package toniarts.openkeeper.game.controller;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.lang.System.Logger;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.ShotFlight;
import toniarts.openkeeper.game.component.ShotViewState;
import toniarts.openkeeper.game.component.Tremor;
import toniarts.openkeeper.game.controller.entity.EntityController;
import toniarts.openkeeper.game.controller.shot.IShotProcessor;
import toniarts.openkeeper.game.controller.shot.InfernoShotProcessor;
import toniarts.openkeeper.game.controller.shot.LightningShotProcessor;
import toniarts.openkeeper.game.controller.shot.ShotFlightRules;
import toniarts.openkeeper.game.controller.shot.ShotRequest;
import toniarts.openkeeper.game.sound.CombatSounds;
import toniarts.openkeeper.game.sound.ThingSounds;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Shot;
import static toniarts.openkeeper.tools.convert.map.Shot.ProcessType.CHICKEN;
import static toniarts.openkeeper.tools.convert.map.Shot.ProcessType.CREATE_CREATURE;
import static toniarts.openkeeper.tools.convert.map.Shot.ProcessType.CREATE_OBJECT;
import static toniarts.openkeeper.tools.convert.map.Shot.ProcessType.CREATE_SINGLE_CREATURE;
import static toniarts.openkeeper.tools.convert.map.Shot.ProcessType.INFERNO;
import static toniarts.openkeeper.tools.convert.map.Shot.ProcessType.LIGHTNING;
import static toniarts.openkeeper.tools.convert.map.Shot.ProcessType.MODIFY_HEALTH;
import static toniarts.openkeeper.tools.convert.map.Shot.ProcessType.POSSESS_CREATURE;
import static toniarts.openkeeper.tools.convert.map.Shot.ProcessType.SIGHT_OF_EVIL;
import static toniarts.openkeeper.tools.convert.map.Shot.ProcessType.SUMMON_REAPER;
import static toniarts.openkeeper.tools.convert.map.Shot.ProcessType.TREMOR;
import static toniarts.openkeeper.tools.convert.map.Shot.ProcessType.TURNCOAT;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Fires shots. What a shot does is decided by its process type, each
 * implemented process type has an {@link IShotProcessor}.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class ShotsController implements IShotsController {

    private static final Logger logger = System.getLogger(ShotsController.class.getName());

    private final IKwdFile kwdFile;
    private final EntityData entityData;
    private final Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings;
    private final IGameTimer gameTimer;
    private final IGameController gameController;
    private final IMapController mapController;
    private final ILevelInfo levelInfo;
    private final IObjectsController objectsController;
    private final ICreaturesController creaturesController;
    private final CombatSounds combatSounds;
    private final Map<Shot.ProcessType, IShotProcessor> processors;
    private final Set<Shot.ProcessType> unprocessedTypes = EnumSet.noneOf(Shot.ProcessType.class);

    public ShotsController(IKwdFile kwdFile, EntityData entityData, Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings, IGameTimer gameTimer,
            IGameController gameController, IMapController mapController, ILevelInfo levelInfo, IObjectsController objectsController, ICreaturesController creaturesController) {
        this.kwdFile = kwdFile;
        this.entityData = entityData;
        this.gameSettings = gameSettings;
        this.gameTimer = gameTimer;
        this.gameController = gameController;
        this.mapController = mapController;
        this.levelInfo = levelInfo;
        this.objectsController = objectsController;
        this.creaturesController = creaturesController;
        this.combatSounds = new CombatSounds(entityData, kwdFile);
        this.processors = createProcessors();
    }

    private Map<Shot.ProcessType, IShotProcessor> createProcessors() {
        Map<Shot.ProcessType, IShotProcessor> result = new EnumMap<>(Shot.ProcessType.class);

        // The data is the creature and its level, for the single creature variant that is the upgraded spell's level 4
        IShotProcessor createCreature = request -> creaturesController.spawnCreature((short) request.shotData1(), request.playerId(),
                request.shotData2(), WorldUtils.vector3fToVector2f(request.position()), ICreaturesController.SpawnType.CONJURE);
        result.put(CREATE_CREATURE, createCreature);
        result.put(CREATE_SINGLE_CREATURE, createCreature);
        result.put(CREATE_OBJECT, this::createObject);
        result.put(POSSESS_CREATURE, request -> creaturesController.createController(request.target()).setPossession(true));
        result.put(MODIFY_HEALTH, request -> {
            EntityController.setDamage(entityData, request.target(), -request.shotData1());
            playShotHit(request.shot(), request.target());
        });
        result.put(TURNCOAT, request -> {
            if (request.target() != null && creaturesController.isValidEntity(request.target())) {
                creaturesController.createController(request.target()).turncoat(request.playerId(), request.shotData1());
            }
        });
        result.put(CHICKEN, request -> {
            if (request.target() != null && creaturesController.isValidEntity(request.target())) {
                creaturesController.createController(request.target()).turnIntoChicken(request.shotData1());
            }
        });
        result.put(SUMMON_REAPER, request -> new ReaperSummoner(kwdFile, entityData, gameTimer, creaturesController).summon(request.shot().getShotId(),
                request.shotData1(), request.playerId(), WorldUtils.vector3fToVector2f(request.position())));
        result.put(TREMOR, request -> {

            // Shot data 1 is the duration in seconds, shot data 2 the radius in tiles
            double now = gameTimer.getGameTime();
            entityData.setComponent(entityData.createEntity(), new Tremor(WorldUtils.vectorToPoint(request.position()), request.shotData2(), now, now + request.shotData1()));
        });
        result.put(SIGHT_OF_EVIL, request -> {

            // Shot data 1 is the radius, the shot lifetime (in game ticks) the duration
            if (request.shotData1() > 0) {
                mapController.revealSightOfEvil(WorldUtils.vectorToPoint(request.position()), request.shotData1(), request.shot().getHealth(), request.playerId());
            }
        });
        result.put(LIGHTNING, new LightningShotProcessor(entityData, gameTimer, kwdFile, mapController));
        result.put(INFERNO, new InfernoShotProcessor(entityData, gameTimer, () -> kwdFile.getGameLevel().getTicksPerSec()));

        return result;
    }

    @Override
    public void createShot(short shotTypeId, int shotData1, int shotData2, short playerId, Vector3f position, EntityId target, EntityId shooter) {

        // Creature attacks still apply their damage themselves, see CreatureController#executeAttack. Processing
        // their shots here too would hit twice (Mistress and Fairy lightning share the keeper's process type)
        if (shooter != null) {
            return;
        }

        Shot shot = kwdFile.getShotById(shotTypeId);
        ThingSounds.play(shot.getSoundCategory(), ThingSounds.SHOT_LAUNCH, position);
        IShotProcessor processor = processors.get(shot.getProcessType());
        if (processor == null) {
            logger.log(Logger.Level.WARNING, "Shot type {0} not implemented", shot.getProcessType());
            return;
        }

        processor.process(new ShotRequest(shot, shotData1, shotData2, playerId, position, target));
    }

    @Override
    public boolean fireShot(short shotTypeId, int shotData1, int shotData2, short playerId, Vector3f position, EntityId target,
            EntityId shooter, float maxRange) {
        Shot shot = kwdFile.getShotById(shotTypeId);
        Vector3f targetPosition = getAimPoint(target);
        if (!ShotFlightRules.isFlying(shot) || targetPosition == null) {
            return false;
        }

        Vector3f velocity = ShotFlightRules.aim(position, targetPosition, shot.getSpeed());
        int ticksPerSecond = kwdFile.getGameLevel().getTicksPerSec();
        EntityId entity = entityData.createEntity();
        entityData.setComponents(entity,
                new Position(FastMath.atan2(velocity.x, velocity.z), new Vector3f(position)),
                new ShotViewState(shotTypeId, velocity.mult(ticksPerSecond)),
                new ShotFlight(shotTypeId, playerId, shooter, shotData1, shotData2, velocity, new Vector3f(position), maxRange,
                        shot.getHealth()));

        return true;
    }

    /**
     * Shots are aimed at the middle of a creature, half its height above its
     * feet (0x4A7FD0)
     *
     * @param target the target
     * @return the spot to aim at, {@code null} if the target isn't in the
     * world
     */
    private Vector3f getAimPoint(EntityId target) {
        Vector3f position = EntityController.getPosition(entityData, target);
        CreatureComponent creatureComponent = entityData.getComponent(target, CreatureComponent.class);
        if (position == null || creatureComponent == null) {
            return position;
        }

        return position.add(0, creatureComponent.height / 2, 0);
    }

    @Override
    public void hit(EntityId shotEntity, EntityId thing) {
        ShotFlight flight = entityData.getComponent(shotEntity, ShotFlight.class);
        Shot shot = kwdFile.getShotById(flight.shotId);

        // The shot's data 1 is its damage (0x4AC420)
        if (shot.getDamageType() == Shot.DamageType.HIT_THING && flight.shotData1 > 0) {
            EntityController.setDamage(entityData, thing, flight.shotData1);
        }

        IShotProcessor processor = processors.get(shot.getProcessType());
        if (processor != null) {
            processor.process(new ShotRequest(shot, flight.shotData1, flight.shotData2, flight.ownerId,
                    EntityController.getPosition(entityData, shotEntity), thing));
        } else if (shot.getProcessType() != Shot.ProcessType.NONE && unprocessedTypes.add(shot.getProcessType())) {
            logger.log(Logger.Level.WARNING, "Shot type {0} not implemented", shot.getProcessType());
        }
    }

    private void createObject(ShotRequest request) {
        GameObject object = kwdFile.getObject((short) request.shotData1());
        if (object != null && object.getFlags().contains(GameObject.ObjectFlag.OBJECT_TYPE_GOLD)) {

            // Create Gold: the shot's second value is the amount of gold
            Point tile = WorldUtils.vectorToPoint(request.position());
            objectsController.loadObject((short) request.shotData1(), request.playerId(), tile.x, tile.y, request.shotData2(), null, null);
        } else {
            objectsController.loadObject((short) request.shotData1(), request.playerId(), request.position(), 0);
        }
    }

    private void playShotHit(Shot shot, EntityId target) {
        Position targetPosition = target != null ? entityData.getComponent(target, Position.class) : null;
        if (targetPosition != null) {
            combatSounds.playShotHit(shot, target, targetPosition.position);
        }
    }

}
