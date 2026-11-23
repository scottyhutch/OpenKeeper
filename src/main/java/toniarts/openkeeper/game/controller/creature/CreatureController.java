/*
 * Copyright (C) 2014-2018 OpenKeeper
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
package toniarts.openkeeper.game.controller.creature;

import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.fsm.StateMachine;
import com.badlogic.gdx.ai.pfa.GraphPath;
import com.badlogic.gdx.math.Vector2;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.filter.FieldFilter;
import toniarts.openkeeper.game.component.Armour;
import toniarts.openkeeper.game.component.Attack;
import toniarts.openkeeper.game.HeroLog;
import toniarts.openkeeper.game.component.AttackTarget;
import toniarts.openkeeper.game.component.BackOff;
import toniarts.openkeeper.game.component.CallToArms;
import toniarts.openkeeper.game.component.CasinoBigWinner;
import toniarts.openkeeper.game.component.CasinoDisco;
import toniarts.openkeeper.game.component.CastShot;
import toniarts.openkeeper.game.component.ChaseBackOff;
import toniarts.openkeeper.game.component.CombatPitFighter;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureExperience;
import toniarts.openkeeper.game.component.CreatureFall;
import toniarts.openkeeper.game.component.CreatureHunger;
import toniarts.openkeeper.game.component.CreatureImprisoned;
import toniarts.openkeeper.game.component.CreatureMeleeAttack;
import toniarts.openkeeper.game.component.CreatureMood;
import toniarts.openkeeper.game.component.CreatureMoodJob;
import toniarts.openkeeper.game.component.CreaturePay;
import toniarts.openkeeper.game.component.CreatureRecuperating;
import toniarts.openkeeper.game.component.CreatureSleep;
import toniarts.openkeeper.game.component.CreatureSpell;
import toniarts.openkeeper.game.component.CreatureSpells;
import toniarts.openkeeper.game.component.CreatureTortured;
import toniarts.openkeeper.game.component.DroppedJob;
import toniarts.openkeeper.game.component.CreatureViewState;
import toniarts.openkeeper.game.component.Death;
import toniarts.openkeeper.game.component.Disguised;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.DoorTarget;
import toniarts.openkeeper.game.component.Fearless;
import toniarts.openkeeper.game.component.FollowTarget;
import toniarts.openkeeper.game.component.Food;
import toniarts.openkeeper.game.component.FoodHunt;
import toniarts.openkeeper.game.component.Frightened;
import toniarts.openkeeper.game.component.GamblingAfterPay;
import toniarts.openkeeper.game.component.Frozen;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.component.GuardedRoom;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Hypnotised;
import toniarts.openkeeper.game.component.IdleAnimation;
import toniarts.openkeeper.game.component.InHand;
import toniarts.openkeeper.game.component.Invisible;
import toniarts.openkeeper.game.component.Invulnerable;
import toniarts.openkeeper.game.component.JobPicked;
import toniarts.openkeeper.game.component.Mana;
import toniarts.openkeeper.game.component.MeleeHit;
import toniarts.openkeeper.game.component.Mobile;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Objective;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Party;
import toniarts.openkeeper.game.component.PathRetry;
import toniarts.openkeeper.game.component.Peaceful;
import toniarts.openkeeper.game.component.PlayerObjective;
import toniarts.openkeeper.game.component.PortalGem;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.PrisonEscape;
import toniarts.openkeeper.game.component.Possessed;
import toniarts.openkeeper.game.component.PossessedAbility;
import toniarts.openkeeper.game.component.PossessedMovement;
import toniarts.openkeeper.game.component.ReturnToHeroLair;
import toniarts.openkeeper.game.component.Senses;
import toniarts.openkeeper.game.component.Slapped;
import toniarts.openkeeper.game.component.SpeedBlur;
import toniarts.openkeeper.game.component.SpeedChange;
import toniarts.openkeeper.game.component.SpeedUp;
import toniarts.openkeeper.game.component.StolenLoot;
import toniarts.openkeeper.game.component.Stored;
import toniarts.openkeeper.game.component.SummonedReaper;
import toniarts.openkeeper.game.component.TaskComponent;
import toniarts.openkeeper.game.component.Teleport;
import toniarts.openkeeper.game.component.Threat;
import toniarts.openkeeper.game.component.Turncoat;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.component.WithdrawnFrom;
import toniarts.openkeeper.game.controller.GameWorldController;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.IShotsController;
import toniarts.openkeeper.game.controller.entity.EntityController;
import toniarts.openkeeper.game.controller.entity.IEntityController;
import toniarts.openkeeper.game.controller.object.IObjectController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController;
import toniarts.openkeeper.game.controller.room.BridgeController;
import toniarts.openkeeper.game.controller.room.CombatPitController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.PrisonController;
import toniarts.openkeeper.game.controller.room.storage.RoomPrisonerControl;
import toniarts.openkeeper.game.data.ActionPoint;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.data.ObjectiveType;
import toniarts.openkeeper.game.event.GameEvent.ThingFlag;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.event.GameEventType;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.navigation.steering.CreatureSizing;
import toniarts.openkeeper.game.navigation.steering.SteeringUtils;
import toniarts.openkeeper.game.task.ITaskManager;
import toniarts.openkeeper.game.task.Task;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureSpell.CreatureSpellFlag;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;
import toniarts.openkeeper.tools.convert.map.Door;
import toniarts.openkeeper.tools.convert.map.Material;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Shot;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.utils.WorldUtils;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * Controls an entity with {@link CreatureAi} component. Basically supports the
 * AI state machine.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
// Controllers inherit entity-ID equality and hashing; mutable sleep state is not identity.
// skipcq: JAVA-W0100
public final class CreatureController extends EntityController implements ICreatureController {

    /**
     * Fight turns in a row with the threat over our fear that scare us
     */
    private static final int SCARED_OVER_THREAT_CHECKS = 3;
    /**
     * Within this many tiles of our own Dungeon Heart, the threat to us is
     * taken lighter the nearer we are
     */
    private static final int HEART_THREAT_DISTANCE = 10;
    /**
     * The least distance, in tiles, a creature backs off from its target
     */
    private static final float BACK_OFF_RANGE = 2f;

    private static final Logger logger = System.getLogger(CreatureController.class.getName());

    /**
     * How far from the threat, in tiles, a fleeing creature aims. The distance
     * DKII.EXE adds to the effect radius is 2, the radius is guessed at 5
     */
    private static final int FLEE_DISTANCE = 7;
    /**
     * How far, in tiles, a recuperating hero wanders on its own land (DK II
     * 0x461B50 asks 0x461BB0 for a tile this far)
     */
    private static final int OWN_LAND_WANDER_DISTANCE = 10;

    private static final int FACE_DISTANCE = 32; // Tiles
    /**
     * Possessed movement speed relative to walking forward
     */
    private static final int IMP_TELEPORT_OUT_EFFECT = 321;
    private static final int IMP_TELEPORT_IN_EFFECT = 322;
    private static final float FACING_TOLERANCE = 0.2f; // Radians
    private static final float POSSESSED_BACKWARD_SPEED_FACTOR = 0.5f;
    private static final float POSSESSED_STRAFE_SPEED_FACTOR = 0.667f;
    private static final float SPEED_MULTIPLIER = 2f;
    // DK II's 0xD000, under 13 square tiles from the heart's centre
    private static final float NEAR_OWN_HEART_DISTANCE_SQUARED = 13 * WorldUtils.TILE_WIDTH * WorldUtils.TILE_WIDTH;
    /**
     * An enemy of the other alignment counts as this much nearer when we look
     * for one to fight. DK II takes 7/8 of the squared distance (0x45B140).
     */
    private static final float OTHER_ALIGNMENT_DISTANCE_FACTOR = (float) Math.sqrt(7 / 8f);
    /**
     * How long the big winner of the Casino celebrates its jackpot, as in DK II
     */
    private static final float BIG_WINNER_CELEBRATION_SECONDS = 10f;

    /**
     * The game ticks a summoned Horned Reaper stands before it roars: DK II
     * waits 12 ticks and fades it in over 8 more (0x474740, 0x4747E0)
     */
    private static final int REAPER_ARRIVE_TICKS = 20;
    /**
     * Out of 100 each tick an idle prisoner rolls: below the first it plays
     * an animation, below the second it moves on (DKII.EXE 0x473350)
     */
    private static final int PRISONER_ANIMATION_ROLL = 10;
    private static final int PRISONER_MOVE_ROLL = 20;
    /**
     * The Prison objects a prisoner kicks, on every tenth tick: the Skull, the Bone and the Lavatory (DKII.EXE 0x473350)
     */
    private static final short PRISONER_KICKS_FIRST_OBJECT = 49;
    private static final short PRISONER_KICKS_LAST_OBJECT = 51;
    private static final int PRISONER_KICK_INTERVAL_TICKS = 10;
    /**
     * A kicked object gets a push of 0x1000, which moves it 1/16 tile a tick with no friction, and its state stops it
     * on the fifth tick, turned 448/2048 of a circle to a random side (0x4A1A60, 0x4B2030)
     */
    private static final float PRISONER_KICK_DISTANCE = 0.25f;
    private static final float PRISONER_KICK_TURN = 448 / 2048f * FastMath.TWO_PI;
    private static final Point[] NEIGHBOUR_TILES = {new Point(0, -1), new Point(1, 0), new Point(0, 1), new Point(-1, 0)};
    /**
     * How long a creature that got a job from its job list doesn't pick from the list again, as in DK II (DKII.EXE
     * 0x4780E0)
     */
    private static final float JOB_PICK_INTERVAL_SECONDS = 5f;
    /**
     * The states the Chicken spell interrupts, the creature is going about its own business in these
     */
    private static final Set<CreatureState> CHICKEN_INTERRUPTIBLE_STATES = EnumSet.of(CreatureState.IDLE,
            CreatureState.WANDER, CreatureState.WORK, CreatureState.FIGHT, CreatureState.FOLLOW, CreatureState.FLEE,
            CreatureState.SLEEPING, CreatureState.MELEE_ATTACK, CreatureState.CAST_SPELL, CreatureState.EATING,
            CreatureState.DANCING, CreatureState.CHICKEN, CreatureState.REAPER_HUNT, CreatureState.REAPER_WANDER);
    private static final int MOOD_JOB_INTERVAL_SECONDS = 5;

    /**
     * An Imp teleports to a job more than this many tiles away on either axis
     */
    private static final int IMP_TELEPORT_DISTANCE = 2;
    private static final int TELEPORT_OUT_TICKS = 4;
    private static final int TELEPORT_IN_TICKS = 2;
    /**
     * The jobs that take something somewhere, an Imp walks these (0x4651A0)
     */
    private static final Set<TaskType> CARRY_TASKS = EnumSet.of(TaskType.CARRY_CREATURE_TO_LAIR, TaskType.CARRY_CREATURE_TO_JAIL,
            TaskType.CARRY_OBJECT_TO_STORAGE, TaskType.CARRY_CORPSE_TO_GRAVEYARD, TaskType.DELIVER_TRAP_CRATE, TaskType.DELIVER_DOOR_CRATE);
    /**
     * How long after a withdraw a creature still goes back to where it fought, as in DK II (DKII.EXE 0x4773D0)
     */
    private static final int RETURN_TO_FIGHT_TICKS = 500;

    /**
     * The possessed melee attack hits what is at most 341/2048 of a full
     * circle (about 60 degrees) off the creature's facing, as the cosine of
     * that angle
     */
    private static final float POSSESSED_ATTACK_MIN_FACING = FastMath.cos(341 / 2048f * FastMath.TWO_PI);

    /**
     * A creature joins the group of the possessed creature from closer than
     * this, in tiles
     */
    private static final int POSSESSED_GROUP_RANGE = 7;

    /**
     * The effects a melee hit plays on its target, "Physical Hit Effect" and
     * "Spark shower effect"
     */
    private static final int PHYSICAL_HIT_EFFECT_ID = 237;
    private static final int SPARK_SHOWER_EFFECT_ID = 313;

    /**
     * A possessed creature's spell aims at creatures closer than this, 91
     * square tiles (0x5B000 in DK II's fixed point)
     */
    private static final float POSSESSED_SPELL_REACH_SQUARED = 91;

    /**
     * A possessed Bile Demon picks up a worker closer than a tile (0x516EC0)
     * and holds it half a tile in front of it, at its eye height (0x4831D0).
     * It throws the worker forward two tiles a turn and up a quarter of a tile
     * a turn (0x4831D0).
     */
    private static final float POSSESSED_PICK_UP_REACH = WorldUtils.TILE_WIDTH;
    private static final float THROWN_WORKER_SPEED = 2 * WorldUtils.TILE_WIDTH;
    private static final float THROWN_WORKER_LIFT = WorldUtils.TILE_WIDTH / 4;

    /**
     * The possessed Maiden's fear attack frightens its target for 20 game
     * ticks with this severity (0x40FA90, 0x514470)
     */
    private static final int FEAR_ATTACK_TICKS = 20;
    private static final int FEAR_ATTACK_SEVERITY = 2000;

    private static final int EAT_ATTEMPT_INTERVAL_TICKS = 4;

    /**
     * Every creature's work modifier, which the attack recharges are divided
     * by. DK II starts it at 0.8 (0x4872A0) and the creature data of the
     * original creatures never raises it.
     */
    private static final float ATTACK_WORK_MODIFIER = 0.8f;

    /**
     * A creature picks its target again this often, in game turns, an Imp
     * more often (0x45E9F0)
     */
    private static final int TARGET_PICK_TURNS = 10;
    private static final int IMP_TARGET_PICK_TURNS = 4;

    /**
     * How long a creature that gave up a chase leaves those that run away
     * alone, in game turns (0x488930)
     */
    private static final int CHASE_BACK_OFF_TURNS = 60;

    /**
     * The steps of a creature under the Chicken spell that has no Hatchery to
     * go to, in DK II's order (0x463BE0)
     */
    private static final int[][] CHICKEN_STEPS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    /**
     * The states a meal interrupts. Like in DK II, a creature busy with
     * something else, say fighting, sleeping or held in prison, gets the food
     * of a chicken it is given without stopping to eat it
     */
    private static final Set<CreatureState> EAT_INTERRUPTIBLE_STATES = EnumSet.of(CreatureState.IDLE,
            CreatureState.WANDER, CreatureState.WORK, CreatureState.DANCING);

    /**
     * Of the attack spells, the one that was ready first: the smallest last
     * cast plus recharge, and the earlier slot on a tie (DK II 0x45CB00,
     * 0x48C800). A spell never cast counts as cast at 0. The recharge is
     * divided by the work modifier, as in isAttackRecharged.
     */
    private static final Comparator<CreatureSpell> READY_FIRST = Comparator.comparingDouble(
            creatureSpell -> (creatureSpell.attactStartTime != null ? creatureSpell.attactStartTime : 0)
            + creatureSpell.rechargeTime / ATTACK_WORK_MODIFIER);

    private final INavigationService navigationService;
    private final ITaskManager taskManager;
    private final IGameTimer gameTimer;
    private final Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings;
    private final ICreaturesController creaturesController;
    private final IEntityPositionLookup entityPositionLookup;
    private final ILevelInfo levelInfo;
    private final RoomDestructionTrigger roomDestructionTrigger = new RoomDestructionTrigger();
    private final IShotsController shotsController;
    private final GameEventService gameEventService;
    // TODO: All the data is not supposed to be on entities as they become too big, but I don't want these here either
    private final Creature creature;
    private final StateMachine<ICreatureController, CreatureState> stateMachine;
    private float motionless = 0;

    public CreatureController(EntityId entityId, EntityData entityData, Creature creature, INavigationService navigationService,
            ITaskManager taskManager, IGameTimer gameTimer, Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings,
            ICreaturesController creaturesController, IEntityPositionLookup entityPositionLookup, IMapController mapController,
            ILevelInfo levelInfo, IObjectsController objectsController, IShotsController shotsController, GameEventService gameEventService) {
        super(entityId, entityData, objectsController, mapController);
        this.navigationService = navigationService;
        this.taskManager = taskManager;
        this.creature = creature;
        this.gameTimer = gameTimer;
        this.gameSettings = Collections.unmodifiableMap(gameSettings);
        this.creaturesController = creaturesController;
        this.entityPositionLookup = entityPositionLookup;
        this.levelInfo = levelInfo;
        this.shotsController = shotsController;
        this.gameEventService = gameEventService;
        this.stateMachine = new DefaultStateMachine<>(this) {
            @Override
            public void changeState(CreatureState newState) {
                toniarts.openkeeper.game.state.loop.ThreadCheck.logic("CreatureController.changeState");
                CreatureAi ai = entityData.getComponent(entityId, CreatureAi.class);
                CreatureState target = ai == null ? null : ai.getTargetState();
                if (newState == CreatureState.IDLE && target != null) {
                    newState = target;
                }
                if (newState != null && newState == target) {
                    // Consume before enter(): a failed target may immediately fall back to IDLE.
                    entityData.setComponent(entityId, new CreatureAi(ai.stateStartTime,
                            ai.getCreatureState(), ai.creatureId));
                }
                super.changeState(newState);
            }
        };
    }

    @Override
    public void completeDungeonEntrance() {
        Position position = entityData.getComponent(entityId, Position.class);
        var offset = creature.getAnimationOffsets(Creature.OffsetType.PORTAL_ENTRANCE);
        if (position == null || offset == null) {
            return;
        }
        // DKII 0x459c91..0x459d2d adds offset slot 0 in world XY (no rotation),
        // then replaces Z with floor height before switching to idle.
        entityData.setComponent(entityId, new Position(position.rotation,
                new Vector3f(position.position.x + offset.x, WorldUtils.FLOOR_HEIGHT,
                        position.position.z + offset.y)));
    }

    @Override
    public boolean shouldFleeOrAttack() {
        checkSurroundings();

        // A non-fighter (imp) only hits back at an enemy in reach and otherwise keeps working
        boolean fighter = creature.getFightStyle() != Creature.FightStyle.NON_FIGHTER;
        IEntityController enemyInReach = fighter ? null : getAttackTarget();
        // DK II gives it a reach of one tile (0x45BDF0)
        if (enemyInReach != null && !isInRange(WorldUtils.TILE_WIDTH, enemyInReach.getEntityId())) {
            setAttackTarget(null);
            enemyInReach = null;
        }

        // Check fleeing, TODO: Always flee? A non-fighter withdraws as soon as an enemy fighter picks it as
        // its target, wherever it is (0x45EC80)
        if (entityData.getComponent(entityId, Fearless.class) == null && !isCombatPitFighter()
                && ((fighter ? isThreatTooHigh() : isTargetedByEnemyFighter() && !isCornered())
                || isFrightened(entityData.getComponent(entityId, CreatureComponent.class).fear))) {

            // Fighters no longer flee from DHeart, our own one that still stands
            IRoomController roomController = fighter ? mapController.getRoomControllerByCoordinates(getCreatureCoordinates()) : null;
            if (roomController == null || !roomController.isDungeonHeart()
                    || roomController.getOwnerId() != getOwnerId() || roomController.isDestroyed()) {
                // With nowhere to withdraw to, it carries on as if nothing was after it (0x45BDF0, 0x45CB00)
                if (stateMachine.isInState(CreatureState.FLEE) || flee()) {
                    return true;
                }
            }
        }

        // Should we attack, try to avoid i.e. imps engaging in a fight. Near their own heart they
        // hit back at an enemy in reach
        IEntityController attackTarget = fighter ? getAttackTarget() : isNearOwnHeart() ? enemyInReach : null;
        if (attackTarget != null && (stateMachine.isInState(CreatureState.FIGHT)
                || stateMachine.isInState(CreatureState.MELEE_ATTACK) || isNearCallToArms())) {
            if (!stateMachine.isInState(CreatureState.FIGHT) && !stateMachine.isInState(CreatureState.MELEE_ATTACK)) {
                stateMachine.changeState(CreatureState.FIGHT);
            }
            return true;
        }

        // With nobody to fight, a dead body is worth a spell
        return castCorpseSpell();
    }

    /**
     * Casts our spell for dead bodies, Raise Dead, on the nearest dead body we
     * sense within the spell's range (0x45CB00)
     *
     * @return true if we cast the spell
     */
    private boolean castCorpseSpell() {
        CreatureSpells creatureSpells = entityData.getComponent(entityId, CreatureSpells.class);
        if (creatureSpells == null) {
            return false;
        }

        for (EntityId creatureSpellEntity : creatureSpells.creatureSpells) {
            CreatureSpell spell = entityData.getComponent(creatureSpellEntity, CreatureSpell.class);
            toniarts.openkeeper.tools.convert.map.CreatureSpell creatureSpell = levelInfo.getLevelData().getCreatureSpellById(spell.creatureSpellId);
            if (!isAttackRecharged(spell) || !creatureSpell.getFlags().contains(CreatureSpellFlag.IS_DEFENSIVE)) {
                continue;
            }

            // The Imp's Teleport is defensive too, but it has no shot
            Shot shot = levelInfo.getLevelData().getShotById(creatureSpell.getShotTypeId());
            if (shot == null || shot.getCollideType() != Shot.CollideType.DEAD_BODIES) {
                continue;
            }

            EntityId deadBody = getNearestDeadBody(spell.range);
            if (deadBody != null) {
                entityData.setComponent(creatureSpellEntity, new CreatureSpell(spell, gameTimer.getGameTime()));
                stopCreature();
                stateMachine.changeState(CreatureState.CAST_SPELL);
                entityData.setComponent(entityId, new CastShot(spell.creatureSpellId, deadBody, spell.range,
                        gameTimer.getGameTime() + getShotDelay(spell.creatureSpellId)));

                return true;
            }
        }

        return false;
    }

    private EntityId getNearestDeadBody(float range) {
        EntityId nearestDeadBody = null;
        float nearestDistance = range;
        for (EntityId entity : entityPositionLookup.getSensedEntities(entityId)) {
            if (entityData.getComponent(entity, Death.class) != null && entityData.getComponent(entity, CreatureComponent.class) != null
                    && getDistanceToCreature(entity) <= nearestDistance) {
                nearestDistance = getDistanceToCreature(entity);
                nearestDeadBody = entity;
            }
        }

        return nearestDeadBody;
    }

    @Override
    public void checkSurroundings() {

        // A neutral creature looks for a creature of a keeper, not the heroes, within its sight
        // range on both axes and with nothing in between: walls and every door but a Barricade
        // block the sight. It joins the first one's keeper. An imprisoned one joins nobody
        // (DKII.EXE 0x4790A0, line of sight 0x4512F0 and 0x4511A0)
        Senses senses = entityData.getComponent(entityId, Senses.class);
        if (getOwnerId() != Player.NEUTRAL_PLAYER_ID || senses == null || isImprisoned()) {
            return;
        }

        Point tile = getCreatureCoordinates();
        int range = (int) senses.distanceCanSee;
        for (int x = tile.x - range + 1; x < tile.x + range; x++) {
            for (int y = tile.y - range + 1; y < tile.y + range; y++) {
                for (EntityId entity : entityPositionLookup.getEntitiesInLocation(x, y)) {
                    Owner owner = entityData.getComponent(entity, Owner.class);
                    Position position = entityData.getComponent(entity, Position.class);
                    if (owner != null && owner.ownerId != Player.NEUTRAL_PLAYER_ID && owner.ownerId != Player.GOOD_PLAYER_ID
                            && position != null && entityData.getComponent(entity, CreatureComponent.class) != null
                            && entityPositionLookup.hasLineOfSight(getPosition(), position.position,
                                    door -> !levelInfo.getLevelData().getDoorById(door.doorId).getFlags().contains(Door.DoorFlag.IS_BARRICADE))) {
                        convertCreature(owner.ownerId);
                        return;
                    }
                }
            }
        }
    }

    /**
     * DK II's non-fighters fight an enemy in reach within 13 square tiles of the
     * centre of their own Dungeon Heart (0xD000 in 0x45BDF0), as long as it stands
     */
    private boolean isNearOwnHeart() {
        Keeper keeper = levelInfo.getPlayer(getOwnerId());
        Point heart = keeper != null ? keeper.getDungeonHeartLocation() : null;
        if (heart == null) {
            return false;
        }
        IRoomController room = mapController.getRoomControllerByCoordinates(heart);
        if (room == null || room.isDestroyed()) {
            return false;
        }
        Vector3f position = getPosition();
        return WorldUtils.pointToVector2f(heart).distanceSquared(position.x, position.z) < NEAR_OWN_HEART_DISTANCE_SQUARED;
    }

    /**
     * Tells whether the threat to us, less the threat we cause, is over our
     * fear while no fellow fighters are with us. A creature that always flees
     * doesn't count on fellow fighters
     *
     * @return true if we should flee
     */
    private boolean isThreatTooHigh() {
        Threat threatComponent = entityData.getComponent(entityId, Threat.class);
        int threat = threatComponent != null ? threatComponent.threat : 0;
        int fear = entityData.getComponent(entityId, CreatureComponent.class).fear;
        int threatToUs = getEnemyThreat();
        int threatCaused = creature.getFlags().contains(Creature.CreatureFlag.ALWAYS_FLEE)
                || isHealthAtCriticalLevel() ? threat : getOurThreat();
        return threatToUs - threatCaused > fear && (getFellowFighters() == 0
                || creature.getFlags().contains(Creature.CreatureFlag.ALWAYS_FLEE));
    }

    private boolean isTargetedByEnemyFighter() {

        // Whoever is after us counts, an archer shoots from farther than a worker senses
        for (EntityId entity : entityData.findEntities(new FieldFilter<>(AttackTarget.class, "entityId", entityId), AttackTarget.class)) {
            if (isEnemy(entity) && getEntityFightingStyle(entity) != Creature.FightStyle.NON_FIGHTER) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if we should fear death
     *
     * @return true if we have critically low health level
     */
    private boolean isHealthAtCriticalLevel() {
        return gameSettings.get(Variable.MiscVariable.MiscType.CREATURE_CRITICAL_HEALTH_PERCENTAGE_OF_MAX).getValue() > getHealthPercentage();
    }

    private int getFellowFighters() {
        int fellowFighters = 0;
        for (EntityId entity : entityPositionLookup.getSensedEntities(entityId)) {
            if (isAlly(entity) && !isIncapacitated(entity) && getEntityFightingStyle(entity) != Creature.FightStyle.NON_FIGHTER) {
                fellowFighters++;
            }
        }

        return fellowFighters;
    }

    /**
     * Gets the total threat caused by the enemies visible
     *
     * @return total enemy threat
     */
    private int getEnemyThreat() {
        int enemyThreat = 0;
        for (EntityId entity : entityPositionLookup.getSensedEntities(entityId)) {
            if (isEnemy(entity) && isThreat(entity) && ThreatSource.isThreateningUs(entityData, entity, entityId)) {
                enemyThreat += getThreat(entity);
            }
        }

        return enemyThreat;
    }

    /**
     * Checks if the given entity posseses a threat to anyone at all
     *
     * @param entity the entity to check
     * @return is the entity a threat to anyone
     */
    private boolean isThreat(EntityId entity) {
        return !isIncapacitated(entity) && !isCaptive(entity) && !isChicken(entity);
    }

    /**
     * Checks if the given entity is a captive. Unable to join a fight and also
     * posses no threat to anyone
     *
     * @param entity the entity
     * @return is entity a captive
     */
    private boolean isCaptive(EntityId entity) {
        // Fighting in the Combat Pit is kind of captivity too
        return isImprisoned(entityData, entity) || isTortured(entityData, entity)
                || entityData.getComponent(entity, CombatPitFighter.class) != null;
    }

    /**
     * Gets the total threat caused by us. Meaning the band of brothers visible
     * to us
     *
     * @return total threat caused by us
     */
    private int getOurThreat() {
        int ourThreat = 0;
        for (EntityId entity : entityPositionLookup.getSensedEntities(entityId)) {
            if (isAlly(entity) && isThreat(entity)) {
                ourThreat += getThreat(entity);
            }
        }

        return ourThreat;
    }

    /**
     * Has a possessed Maiden frightened us more than we can take. DK II adds
     * the severity to our fear on each tick of the fear (0x488930), the fear
     * is gone when its time is up.
     *
     * @param fear how much fear we can take
     * @return true if we are frightened and should run away
     */
    private boolean isFrightened(int fear) {
        Frightened frightened = entityData.getComponent(entityId, Frightened.class);
        if (frightened != null && frightened.endTime <= gameTimer.getGameTime()) {
            entityData.removeComponent(entityId, Frightened.class);
            return false;
        }
        return frightened != null && frightened.severity > fear;
    }

    @Override
    public void unassingCurrentTask() {
        Task assignedTask = getAssignedTask();
        if (assignedTask != null) {
            assignedTask.unassign(this);
            entityData.removeComponent(entityId, TaskComponent.class);
        }
    }

    @Override
    public void navigateToRandomPoint() {
        final Position position = entityData.getComponent(entityId, Position.class);
        final Mobile mobile = entityData.getComponent(entityId, Mobile.class);
        final Owner owner = entityData.getComponent(entityId, Owner.class);
        if (position != null && mobile != null && owner != null) {
            Point start = WorldUtils.vectorToPoint(position.position);

            // An idle creature walks about on its owner's land, like in DK II, a neutral one on neutral land. Others
            // anywhere near
            Point destination = stateMachine.isInState(CreatureState.IDLE)
                    ? navigationService.findIdleWalkTile(start, this)
                    : navigationService.findRandomAccessibleTile(start, 10, this);
            if (start.equals(destination)) {
                walkInsideTile(position.position);
            } else if (destination != null) {
                createNavigation(start, destination, null);
            }
        }
    }

    /**
     * Walks to a spot inside the tile the creature stands on. DK II does this
     * when an idle walk finds no tile to go to (0x4808E0): it takes the
     * creature's own tile and then one of nine spots in it.
     *
     * @param position where the creature is now
     */
    private void walkInsideTile(Vector3f position) {
        Point tile = WorldUtils.vectorToPoint(position);
        Vector2 spot = WorldUtils.pointToVector2(tile);
        spot.add((FastMath.rand.nextInt(3) - 1) * 0.25f, (FastMath.rand.nextInt(3) - 1) * 0.25f);
        Vector2 here = new Vector2(position.x, position.z);
        if (here.dst(spot) > 0.1f) {
            entityData.setComponent(entityId, new Navigation(tile, null, List.of(here, spot)));
        }
    }

    @Override
    public boolean isSummonedReaper() {

        // One sent for the Portal Gem only goes for the gem, SummonedReaperSystem walks him there
        SummonedReaper summonedReaper = entityData.getComponent(entityId, SummonedReaper.class);
        return summonedReaper != null && !summonedReaper.isLeaving() && summonedReaper.gem == null;
    }

    @Override
    public boolean huntNearestEnemy() {
        Vector3f ourPosition = getPosition();
        Optional<EntityId> enemy = entityData.findEntities(null, CreatureComponent.class, Owner.class, Position.class, Health.class).stream()
                .filter(this::isReaperPrey)
                .sorted(Comparator.comparingDouble(entity -> getPosition(entityData, entity).distanceSquared(ourPosition)))
                .filter(this::hasPathToEntity)
                .findFirst();
        enemy.ifPresent(this::setAttackTarget);
        return enemy.isPresent();
    }

    /**
     * Can the Reaper go after the creature. Not one that is out of the fight
     * or held captive, nor another Reaper on its way out.
     */
    private boolean isReaperPrey(EntityId entity) {
        SummonedReaper otherReaper = entityData.getComponent(entity, SummonedReaper.class);
        return isEnemy(entity) && entityData.getComponent(entity, Health.class).health > 0
                && !isIncapacitated(entity) && !isCaptive(entity)
                && (otherReaper == null || !otherReaper.isLeaving());
    }

    @Override
    public boolean attackNearestEnemyHeart() {
        Point from = getCreatureCoordinates();
        Keeper keeper = levelInfo.getPlayer(getOwnerId());
        CreatureMeleeAttack meleeAttack = entityData.getComponent(entityId, CreatureMeleeAttack.class);
        if (keeper == null || meleeAttack == null) {
            return false;
        }

        // DK II measures the way to a heart as dx * dx + 2 * dy in tiles, compared unsigned,
        // so a heart at a smaller y can count as far away (0x45E1B0)
        List<EntityId> hearts = new ArrayList<>();
        for (EntityId entity : entityData.findEntities(null, ObjectComponent.class, Owner.class, Position.class, Health.class)) {
            if (DungeonHeartTarget.isDungeonHeart(entityData, entity) && entityData.getComponent(entity, Health.class).health > 0
                    && keeper.isEnemy(entityData.getComponent(entity, Owner.class).ownerId)) {
                hearts.add(entity);
            }
        }
        hearts.sort(Comparator.comparing(heart -> {
            Point tile = WorldUtils.vectorToPoint(getPosition(entityData, heart));
            return (tile.x - from.x) * (tile.x - from.x) + 2 * (tile.y - from.y);
        }, Integer::compareUnsigned));
        for (EntityId heart : hearts) {
            Point heartTile = WorldUtils.vectorToPoint(getPosition(entityData, heart));
            Point attackTile = DungeonHeartTarget.getAttackTile(from, heartTile, meleeAttack.range,
                    tile -> tile.equals(from) || navigationService.findPath(from, tile, this) != null);
            if (attackTile != null) {
                entityData.setComponent(entityId, new AttackTarget(heart));
                return true;
            }
        }

        return false;
    }

    @Override
    public void navigateAsChicken() {
        if (entityData.getComponent(entityId, Position.class) == null) {
            return;
        }
        Point start = getCreatureCoordinates();

        // The nearest Hatchery of our keeper that we can get to, measured to its middle as in DK II (0x4C6750)
        List<IRoomController> hatcheries = new ArrayList<>(mapController.getRoomsByFunction(AbstractRoomController.ObjectType.FOOD, getOwnerId()));
        hatcheries.sort(Comparator.comparingInt(hatchery -> {
            Point middle = hatchery.getRoomInstance().getCenter();
            return (middle.x - start.x) * (middle.x - start.x) + (middle.y - start.y) * (middle.y - start.y);
        }));
        for (IRoomController hatchery : hatcheries) {
            List<Point> coordinates = new ArrayList<>(hatchery.getRoomInstance().getCoordinates());
            coordinates.removeIf(p -> !hatchery.isTileAccessible(null, p));
            Point destination = Utils.getRandomItem(coordinates);
            if (destination != null && !createNavigation(start, destination, null)) {
                return;
            }
        }

        // No Hatchery, a step east, west, south or north, starting from a random one of them
        int first = Utils.getRandom().nextInt(CHICKEN_STEPS.length);
        for (int i = 0; i < CHICKEN_STEPS.length; i++) {
            int[] step = CHICKEN_STEPS[(first + i) % CHICKEN_STEPS.length];
            Point p = new Point(start.x + step[0], start.y + step[1]);
            if (mapController.getMapData().getTile(p) != null && !mapController.isSolid(p) && !mapController.isWater(p) && !mapController.isLava(p)) {
                createNavigation(start, p, null);
                return;
            }
        }
    }

    @Override
    public void navigateToRandomPointInCombatPit() {
        CombatPitFighter pitFighter = entityData.getComponent(entityId, CombatPitFighter.class);
        if (pitFighter != null && mapController.getRoomController(pitFighter.pit) instanceof CombatPitController pit) {

            // Waiting in the pit is no work, DK II adds the no work anger each time too (0x478B80)
            addNoWorkAnger();
            createNavigation(getCreatureCoordinates(), pit.getRandomFightingTile(), null);
        }
    }

    /**
     * Like in DK II (DKII.EXE 0x473350, 0x4731F0, 0x4E50F0), an idle prisoner
     * rolls 100 every tick. One in ten it moves on: starting from a random
     * one, it takes the first of the four tiles next to it that is in the cage
     * with it and walks there. Another one in ten, if it has the animation, it
     * is angry instead, two to four times over, and rolls for nothing until
     * that is done. A creature without the animation moves on then too.
     * <br>
     * A hungry prisoner goes after food instead of moving on, no more often
     * than a free creature tries to eat (0x4731F0), and so does every
     * prisoner when food is dropped into the cage. A prisoner after food
     * walks to the nearest food in the Prison (0x473550) and eats it when it
     * stands on its tile: one chicken fills a prisoner, gives it the health a
     * chicken gives and takes its anger about food away (0x473750). Then it
     * goes for the next one, and when no food is left it gets its anger for
     * having no food and is idle again
     */
    @Override
    public void idleInPrison() {
        Point tile = getCreatureCoordinates();
        IRoomController prison = mapController.getRoomControllerByCoordinates(tile);
        if (prison == null) {
            return;
        }
        if (entityData.getComponent(entityId, FoodHunt.class) != null) {
            huntFoodInPrison(prison, tile);
            return;
        }

        // The state lasts as long as the animation plays
        CreatureAi ai = entityData.getComponent(entityId, CreatureAi.class);
        double gameTime = gameTimer.getGameTime();
        if (ai != null && gameTime < ai.stateStartTime + ai.stateDuration) {
            return;
        }
        if (kickPrisonObject(prison, tile, ai == null ? 0 : gameTime - ai.stateStartTime)) {
            return;
        }
        int roll = Utils.getRandom().nextInt(100);
        ArtResource angry = creature.getAnimation(Creature.AnimationType.ANGRY);
        if (roll < PRISONER_ANIMATION_ROLL && ai != null && angry != null && angry.getType() != ArtResource.ArtResourceType.NONE) {
            entityData.setComponent(entityId, new CreatureAi(gameTime, CreatureState.IMPRISONED, ai.creatureId, ai.getTargetState(),
                    (2 + Utils.getRandom().nextInt(3)) * getAnimationTime(creature, Creature.AnimationType.ANGRY)));
            return;
        }
        if (roll >= PRISONER_MOVE_ROLL) {
            return;
        }
        if (isHungry() && startEatAttempt()) {
            entityData.setComponent(entityId, new FoodHunt());
            huntFoodInPrison(prison, tile);
            return;
        }
        int first = Utils.getRandom().nextInt(NEIGHBOUR_TILES.length);
        for (int i = 0; i < NEIGHBOUR_TILES.length; i++) {
            Point offset = NEIGHBOUR_TILES[(first + i) % NEIGHBOUR_TILES.length];
            Point next = new Point(tile.x + offset.x, tile.y + offset.y);
            if (mapController.getRoomControllerByCoordinates(next) == prison && prison.isTileAccessible(tile, next)) {
                createNavigation(tile, next, null);
                return;
            }
        }
    }

    /**
     * Like in DK II (DKII.EXE 0x473350), on every tenth tick of its idling a
     * prisoner that stands on the tile of a Skull, a Bone or the Lavatory of
     * its Prison turns to it and kicks it away, the way the prisoner then
     * faces. DK II slides the object there over four ticks, here it is moved
     * in one step
     *
     * @param prison the Prison the prisoner is in
     * @param tile the tile the prisoner stands on
     * @param idleTime how long the prisoner has been idle, in seconds
     * @return true if the prisoner kicked something
     */
    private boolean kickPrisonObject(IRoomController prison, Point tile, double idleTime) {
        for (EntityId thing : entityPositionLookup.getEntitiesInLocation(tile)) {
            ObjectComponent object = entityData.getComponent(thing, ObjectComponent.class);
            if (object == null || object.objectId < PRISONER_KICKS_FIRST_OBJECT || object.objectId > PRISONER_KICKS_LAST_OBJECT
                    || !prison.getFloorFurniture().contains(thing)) {
                continue;
            }
            if (Math.round(idleTime * levelInfo.getLevelData().getGameLevel().getTicksPerSec()) % PRISONER_KICK_INTERVAL_TICKS != 0) {
                return false;
            }
            Position position = entityData.getComponent(entityId, Position.class);
            Position objectPosition = entityData.getComponent(thing, Position.class);
            float dx = objectPosition.position.x - position.position.x;
            float dz = objectPosition.position.z - position.position.z;
            float rotation = dx == 0 && dz == 0 ? position.rotation : FastMath.atan2(dx, dz);
            entityData.setComponent(entityId, new Position(rotation, position.position));
            entityData.setComponent(thing, new Position(
                    objectPosition.rotation + (Utils.getRandom().nextBoolean() ? PRISONER_KICK_TURN : -PRISONER_KICK_TURN),
                    objectPosition.position.add(FastMath.sin(rotation) * PRISONER_KICK_DISTANCE, 0, FastMath.cos(rotation) * PRISONER_KICK_DISTANCE)));
            return true;
        }
        return false;
    }

    private void huntFoodInPrison(IRoomController prison, Point tile) {
        CreatureMood mood = entityData.getComponent(entityId, CreatureMood.class);
        EntityId food = getFood(tile);
        if (food != null) {
            entityPositionLookup.getEntityController(food).remove();
            entityData.setComponent(entityId, new CreatureHunger(gameTimer.getGameTime(), 0));
            setDamage(-creature.getAttributes().getHpFromChicken());
            if (mood != null) {
                entityData.setComponent(entityId, mood.clear(CreatureMood.REASON_NO_FOOD));
            }
            return;
        }

        Point nearest = null;
        int nearestDistance = Integer.MAX_VALUE;
        for (Point p : prison.getRoomInstance().getCoordinates()) {
            int distance = (p.x - tile.x) * (p.x - tile.x) + (p.y - tile.y) * (p.y - tile.y);
            if (distance < nearestDistance && getFood(p) != null) {
                nearest = p;
                nearestDistance = distance;
            }
        }
        if (nearest != null && !createNavigation(tile, nearest, null)) {
            return;
        }

        // No food left, or no way to it
        entityData.removeComponent(entityId, FoodHunt.class);
        if (nearest == null && mood != null) {
            entityData.setComponent(entityId, mood.add(CreatureMood.REASON_NO_FOOD,
                    CreatureMood.toRuntimeValue(creature.getAttributes().getAngerNoFood())));
        }
    }

    private EntityId getFood(Point tile) {
        for (EntityId thing : entityPositionLookup.getEntitiesInLocation(tile)) {
            if (entityData.getComponent(thing, Food.class) != null) {
                return thing;
            }
        }
        return null;
    }

    @Override
    public IPartyController getParty() {
        Party party = entityData.getComponent(entityId, Party.class);
        if (party != null) {
            return creaturesController.getPartyById(party.partyId);
        }
        return null;
    }

    @Override
    public StateMachine<ICreatureController, CreatureState> getStateMachine() {
        return stateMachine;
    }

    @Override
    public boolean hasObjective() {
        Objective creatureObjective = entityData.getComponent(entityId, Objective.class);
        return (creatureObjective != null && creatureObjective.objective != null);
    }

    @Override
    public boolean followObjective() {
        return taskManager.assignObjectiveTask(this, entityData.getComponent(entityId, Objective.class).objective);
    }

    @Override
    public boolean needsLair() {
        // Creatures without a lair object (the princes, King Reginald...) never make a home
        return getOwnerId() >= Player.KEEPER1_ID && creature.getLairObjectId() != 0
                && entityData.getComponent(entityId, CreatureSleep.class) != null;
    }

    @Override
    public boolean hasLair() {
        CreatureSleep creatureSleep = entityData.getComponent(entityId, CreatureSleep.class);
        return creatureSleep != null && creatureSleep.lairObjectId != null && entityData.getComponent(creatureSleep.lairObjectId, Position.class) != null;
    }

    @Override
    public boolean findLair() {
        return taskManager.assignClosestRoomTask(this, AbstractRoomController.ObjectType.LAIR, null);
    }

    @Override
    public boolean isNeedForSleep() {
        CreatureSleep creatureSleep = entityData.getComponent(entityId, CreatureSleep.class);
        // CREATURE_SLEEPS_WHEN_BELOW_PERCENT_HEALTH triggers the native attempt path,
        // but its assigned-bed gate (0x46f47b) still requires positive debt. The
        // 250/1000 HP, zero-debt probe stayed awake; manual Lair placement bypasses it.
        return needsLair() && isNeedForSleep(creatureSleep,
                creature.getAttributes().getTimeSleep());
    }

    static boolean isNeedForSleep(CreatureSleep sleep, int timeSleep) {
        return sleep != null && timeSleep > 0 && sleep.sleepNeed > 0;
    }

    @Override
    public boolean goToSleepAfterLairDrop() {
        if (shouldFleeOrAttack()) {
            return false;
        }
        if (!needsLair() || creature.getAttributes().getTimeSleep() <= 0 || !hasLair()) {
            return false;
        }
        IRoomController room = mapController.getRoomControllerByCoordinates(getCreatureCoordinates());
        CreatureSleep sleep = entityData.getComponent(entityId, CreatureSleep.class);
        Stored bed = entityData.getComponent(sleep.lairObjectId, Stored.class);
        if (room == null || room.isDestroyed() || room.isRemoved()
                || room.getOwnerId() != getOwnerId()
                || !room.hasObjectControl(AbstractRoomController.ObjectType.LAIR)
                || bed == null || bed.objectType != AbstractRoomController.ObjectType.LAIR
                || !room.getEntityId().equals(bed.room)) {
            return false;
        }
        // Native manual placement bypasses the autonomous debt/retry gates.
        return taskManager.assignSleepTask(this);
    }

    @Override
    public boolean goToSleep() {
        double gameTime = gameTimer.getGameTime();
        CreatureSleep sleep = entityData.getComponent(entityId, CreatureSleep.class);
        if (sleep == null || gameTime - sleep.lastSleepAttemptTime < 1) {
            return false;
        }
        // Native autonomous attempts are throttled to one per four native ticks.
        CreatureSleep updated = new CreatureSleep(sleep);
        updated.lastSleepAttemptTime = gameTime;
        entityData.setComponent(entityId, updated);
        return taskManager.assignSleepTask(this);
    }

    @Override
    public boolean doDroppedJob() {
        DroppedJob droppedJob = entityData.getComponent(entityId, DroppedJob.class);
        if (!DroppedJobs.isDue(droppedJob, droppedJob != null && !droppedJob.justDropped && hasNeedBeforeDroppedJob())) {
            return false;
        }
        if (droppedJob.justDropped) {
            entityData.setComponent(entityId, new DroppedJob(droppedJob.jobType, false));
        }

        // Like in DK II, a job that can't be done any more is forgotten
        if (!taskManager.isTaskAvailable(this, droppedJob.jobType)) {
            entityData.removeComponent(entityId, DroppedJob.class);
            return false;
        }
        return taskManager.assignTask(this, droppedJob.jobType);
    }

    private boolean hasNeedBeforeDroppedJob() {
        return (needsLair() && !hasLair()) || (hasLair() && isNeedForSleep()) || isHungry();
    }

    @Override
    public void findWork(Consumer<Boolean> workResult) {

        // See if we have some available work
        if (isWorker()) {
            taskManager.addToUnemployedWorkerQueue(this, (foundWork) -> {

                // DK II's worker that finds no work stays in its idle state for 240 to 479 ticks (0x463DF0). Only an
                // idle walk that ends sooner takes it back to its job search, so after an idle animation it walks on
                IdleAnimation idle = entityData.getComponent(entityId, IdleAnimation.class);
                if (!foundWork && idle != null) {
                    int ticks = 240 + FastMath.rand.nextInt(240);
                    entityData.setComponent(entityId, new IdleAnimation(idle.animation, idle.endTime,
                            gameTimer.getGameTime() + ticks / (double) levelInfo.getLevelData().getGameLevel().getTicksPerSec()));
                }
                workResult.accept(foundWork);
            });
            return;
        }

        // DKII sends a creature that has stood guard back to guard duty before it picks from its jobs. Guard duty is the
        // only job it returns to like that (0x4778E0 tries the job 0x462850 has set)
        if (entityData.getComponent(entityId, GuardedRoom.class) != null && taskManager.assignTask(this, Creature.JobType.GUARD)) {
            workResult.accept(true);
            return;
        }

        short owner = getOwnerId();
        if (owner == Player.NEUTRAL_PLAYER_ID || owner == Player.GOOD_PLAYER_ID) {

            // Currently no creature specific jobs offered for neutral or good players
            workResult.accept(false);
            return;
        }

        // Like in DK II, discontent comes before the jobs the creature likes
        if (findMoodJob()) {
            workResult.accept(true);
            return;
        }

        // DKII doesn't let a creature pick from its jobs again for 5 seconds after a pick that got it one. Until
        // then it goes on to its other choices, and the lack of work doesn't anger it (0x4780E0)
        JobPicked jobPicked = entityData.getComponent(entityId, JobPicked.class);
        boolean pickedLately = jobPicked != null && gameTimer.getGameTime() - jobPicked.time < JOB_PICK_INTERVAL_SECONDS;

        // A job of its own: like DK II, roll 0 to 99 against the chances of all of its jobs, and when the job rolled
        // can't be started, take the first one of the list that can (0x4780E0)
        // FIXME: moods
        Creature.JobPreference[] jobs = !pickedLately && creature.getHappyJobs() != null ? creature.getHappyJobs() : new Creature.JobPreference[0];
        Creature.JobPreference rolled = rollJob(jobs);
        if (rolled != null && taskManager.isTaskAvailable(this, rolled.getJobType()) && taskManager.assignTask(this, rolled.getJobType())) {
            entityData.setComponent(entityId, new JobPicked(gameTimer.getGameTime()));
            workResult.accept(true);
            return;
        }
        for (Creature.JobPreference job : jobs) {
            if (job != rolled && taskManager.isTaskAvailable(this, job.getJobType()) && taskManager.assignTask(this, job.getJobType())) {
                entityData.setComponent(entityId, new JobPicked(gameTimer.getGameTime()));
                workResult.accept(true);
                return;
            }
        }

        // DKII angers a creature each time it looks for a job and finds none, before it goes on to its other
        // choices (0x4780E0, then 0x478680)
        if (!pickedLately) {
            addNoWorkAnger();
        }

        // With none of its own jobs to start, the creature trains, like in DK II (0x478680)
        if (!creature.getFlags().contains(Creature.CreatureFlag.NOT_TRAIN_WHEN_IDLE)
                && taskManager.isTaskAvailable(this, Creature.JobType.TRAIN)
                && taskManager.assignTask(this, Creature.JobType.TRAIN)) {
            workResult.accept(true);
            return;
        }

        // Then it watches a fight in the Combat Pit, and then it gambles in the Casino (job 9)
        if (taskManager.isTaskAvailable(this, Creature.JobType.COMBAT_PIT_SPECTATE)
                && taskManager.assignTask(this, Creature.JobType.COMBAT_PIT_SPECTATE)) {
            workResult.accept(true);
            return;
        }
        if (taskManager.isTaskAvailable(this, Creature.JobType.DRINK) && taskManager.assignTask(this, Creature.JobType.DRINK)) {
            workResult.accept(true);
            return;
        }

        workResult.accept(false);
    }

    private void addNoWorkAnger() {
        CreatureMood mood = entityData.getComponent(entityId, CreatureMood.class);
        if (mood != null) {
            entityData.setComponent(entityId, mood.add(CreatureMood.REASON_NO_WORK,
                    CreatureMood.toRuntimeValue(creature.getAttributes().getAngerNoWork())));
        }
    }

    /**
     * Like in DK II, an angry creature takes a job from its angry list, and an
     * unhappy creature, or an angry one that got none, from its unhappy list.
     * Once a list gave a job, it rests for a while
     *
     * @return true if the creature got a job
     */
    private boolean findMoodJob() {
        CreatureMood mood = entityData.getComponent(entityId, CreatureMood.class);
        int state = mood == null ? 0 : mood.getState(
                CreatureMood.toRuntimeThreshold(creature.getAttributes().getUnhappyThreshold()));
        if (state == 0) {
            return false;
        }
        double gameTime = gameTimer.getGameTime();
        CreatureMoodJob moodJob = entityData.getComponent(entityId, CreatureMoodJob.class);
        if (moodJob == null) {
            moodJob = new CreatureMoodJob(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
        }
        if (state == 2 && gameTime - moodJob.angryJobTime >= MOOD_JOB_INTERVAL_SECONDS
                && assignMoodJob(creature.getAngryJobs())) {
            entityData.setComponent(entityId, new CreatureMoodJob(moodJob.unhappyJobTime, gameTime));
            return true;
        }
        if (gameTime - moodJob.unhappyJobTime >= MOOD_JOB_INTERVAL_SECONDS
                && assignMoodJob(creature.getUnhappyJobs())) {
            entityData.setComponent(entityId, new CreatureMoodJob(gameTime, moodJob.angryJobTime));
            return true;
        }
        return false;
    }

    /**
     * Starts the job a roll of 100 lands on, or else the first job on the list
     * that the creature can start
     */
    private boolean assignMoodJob(Creature.JobPreference[] moodJobs) {
        if (moodJobs == null) {
            return false;
        }
        List<Creature.JobPreference> jobs = new ArrayList<>(List.of(moodJobs));
        int roll = FastMath.rand.nextInt(100);
        int chance = 0;
        for (Creature.JobPreference job : moodJobs) {
            chance += job.getChance();
            if (roll <= chance) {
                jobs.addFirst(job);
                break;
            }
        }
        for (Creature.JobPreference job : jobs) {
            if (taskManager.assignTask(this, job.getJobType())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Rolls 0 to 99 against the jobs' chances laid end to end. A range takes
     * both of its ends, so a roll on the border goes to the earlier job. With
     * chances adding up to less than 100 the roll can miss them all
     *
     * @param jobs the creature's jobs
     * @return the job rolled, {@code null} for none
     */
    private static Creature.JobPreference rollJob(Creature.JobPreference[] jobs) {
        int roll = FastMath.rand.nextInt(100);
        int end = 0;
        for (Creature.JobPreference job : jobs) {
            end += job.getChance();
            if (roll <= end) {
                return job;
            }
        }
        return null;
    }

    @Override
    public boolean isWorker() {
        return entityData.getComponent(entityId, CreatureComponent.class).worker;
    }

    @Override
    public void executeAssignedTask() {
        TaskComponent taskComponent = entityData.getComponent(entityId, TaskComponent.class);
        if (!taskComponent.taskStarted) {
            entityData.setComponent(entityId, new TaskComponent(taskComponent.taskId, taskComponent.targetEntity, taskComponent.targetLocation, taskComponent.taskType, taskComponent.taskDuration, true));
        }
        if (isAssignedTaskValid()) {
            Task task = getAssignedTask();
            if (!task.moveToOtherSpot(this, taskComponent.taskDuration)) {
                task.executeTask(this, taskComponent.taskDuration);
            }
        }
    }

    @Override
    public boolean isTooMuchGold() {
        return getGold() >= getMaxGold() && isWorker();
    }

    @Override
    public boolean dropGoldToTreasury() {
        if (getGold() > 0 && getOwnerId() >= Player.KEEPER1_ID && isWorker()) {
            if (taskManager.assignGoldToTreasuryTask(this)) {
                navigateToAssignedTask();
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean claimDugFloorOnTheWay() {
        if (taskManager.assignDugFloorOnTheWay(this)) {

            // It is still on its way to its old work
            stopCreature();
            navigateToAssignedTask();
            return true;
        }

        return false;
    }

    @Override
    public boolean isStopped() {
        return entityData.getComponent(entityId, Navigation.class) == null;
    }

    @Override
    public void walkToHeartToDie() {
        EntityController.setDamage(entityData, entityId, 1);
        Keeper keeper = levelInfo.getPlayer(getOwnerId());
        if (keeper == null || keeper.getDungeonHeartLocation() == null) {
            return;
        }

        Point deathbed = new Point(keeper.getDungeonHeartLocation().x + 1, keeper.getDungeonHeartLocation().y + 1);
        if (deathbed.equals(getCreatureCoordinates())) {
            EntityController.setDamage(entityData, entityId, 50);
        } else if (isStopped()) {
            createNavigation(getCreatureCoordinates(), deathbed, null);
        }
    }

    @Override
    public void navigateToAssignedTask() {
        if (isWaitingToRetryPath()) {
            return;
        }
        Task assignedTask = getAssignedTask();
        if (assignedTask != null) {
            castImpHaste();

            Vector2f loc = assignedTask.getTarget(this);
            Vector2f workPosition = assignedTask.getWorkPosition(this);
            if (workPosition != null && workPosition.equals(loc)) {
                workPosition = null;
            }

            // The middle of the target tile is near enough, unless the task wants us somewhere else on the tile
            boolean there = workPosition != null ? isAt(workPosition) : loc != null && isNear(loc);
            if (loc != null && !there) {
                //workNavigationRequired = false;

                Point destination = WorldUtils.vectorToPoint(loc);
                if (!teleport(assignedTask.getTaskType(), destination, workPosition != null ? workPosition : loc)) {
                    createNavigation(getCreatureCoordinates(), destination, assignedTask.isFaceTarget() ? assignedTask.getTaskLocation() : null, workPosition, false, true);
                }
            }
        }
    }

    /**
     * An Imp setting off to a job speeds itself up with its own speed spell
     * whenever the spell is recharged, without a cast (0x4651A0). The spell
     * does what its shot does, so a speed change that is still running is left
     * as it is, but the spell recharges all the same. A speed-up shows the
     * speed blur for as many game ticks as the spell's first value, the
     * percentage
     */
    private void castImpHaste() {
        EntityId spellEntity = getRechargedSpell(CreatureSpellFlag.IMP_HASTE);
        if (spellEntity != null) {
            CreatureSpell spell = entityData.getComponent(spellEntity, CreatureSpell.class);
            toniarts.openkeeper.tools.convert.map.CreatureSpell creatureSpell = levelInfo.getLevelData().getCreatureSpellById(spell.creatureSpellId);
            entityData.setComponent(spellEntity, new CreatureSpell(spell, gameTimer.getGameTime()));
            shotsController.createShot(creatureSpell.getShotTypeId(), creatureSpell.getShotData1(), creatureSpell.getShotData2(), getOwnerId(),
                    getPosition(), entityId, entityId);
            if (creatureSpell.getShotData1() > 100) {
                entityData.setComponent(entityId, new SpeedBlur(gameTimer.getGameTime()
                        + creatureSpell.getShotData1() / (double) levelInfo.getLevelData().getGameLevel().getTicksPerSec()));
            }
        }
    }

    /**
     * An Imp with its teleport spell recharged jumps to a job more than 2 tiles
     * away on either axis instead of walking there, if it can walk there. Not
     * when it takes something somewhere, but gold to the Treasury is no such
     * trip (0x4651A0). The spell has no shot and costs no mana
     *
     * @param taskType the job
     * @param destination the tile the Imp would walk to
     * @param target the spot it lands on, the one it would walk to
     * @return true if the Imp teleports
     */
    private boolean teleport(TaskType taskType, Point destination, Vector2f target) {
        Point location = getCreatureCoordinates();
        if (CARRY_TASKS.contains(taskType)
                || (Math.abs(destination.x - location.x) <= IMP_TELEPORT_DISTANCE && Math.abs(destination.y - location.y) <= IMP_TELEPORT_DISTANCE)) {
            return false;
        }
        EntityId spellEntity = getRechargedSpell(CreatureSpellFlag.IMP_TELEPORT);
        if (spellEntity == null || navigationService.findPath(location, destination, this) == null) {
            return false;
        }

        double now = gameTimer.getGameTime();
        Position position = entityData.getComponent(entityId, Position.class);
        entityData.setComponent(spellEntity, new CreatureSpell(entityData.getComponent(spellEntity, CreatureSpell.class), now));
        entityData.removeComponent(entityId, Navigation.class);
        entityData.setComponent(entityId, new Teleport(now, new Vector3f(target.x, position.position.y, target.y)));
        gameEventService.emitEffect(IMP_TELEPORT_OUT_EFFECT, position.position, position.rotation);
        gameEventService.emit(GameEventType.CREATURE_TELEPORT_OUT, ThingFlag.CREATURE, creature.getCreatureId(), position.position);
        return true;
    }

    /**
     * The Imp jumps out, then lands on the job and jumps in. Each half lasts as
     * long as the jump animation, but at least 4 ticks out and 2 ticks in
     * (0x499B80 and 0x499BE0)
     */
    private void continueTeleport(Teleport teleport) {
        double now = gameTimer.getGameTime();
        int ticks = teleport.destination != null ? TELEPORT_OUT_TICKS : TELEPORT_IN_TICKS;
        if (now - teleport.startTime < Math.max(ticks / (double) levelInfo.getLevelData().getGameLevel().getTicksPerSec(),
                getAnimationTime(creature, Creature.AnimationType.SPECIAL_1))) {
            return;
        }
        if (teleport.destination == null) {
            entityData.removeComponent(entityId, Teleport.class);
            return;
        }

        Position position = entityData.getComponent(entityId, Position.class);
        entityData.setComponents(entityId, new Position(position.rotation, teleport.destination), new Teleport(now, null));
        gameEventService.emitEffect(IMP_TELEPORT_IN_EFFECT, teleport.destination, position.rotation);
        gameEventService.emit(GameEventType.CREATURE_TELEPORT_IN, ThingFlag.CREATURE, creature.getCreatureId(), teleport.destination);
    }

    /**
     * @param flag the flag of the spell
     * @return our spell with the flag if it is recharged, null if there is none
     */
    private EntityId getRechargedSpell(CreatureSpellFlag flag) {
        CreatureSpells creatureSpells = entityData.getComponent(entityId, CreatureSpells.class);
        if (creatureSpells != null) {
            for (EntityId spellEntity : creatureSpells.creatureSpells) {
                CreatureSpell spell = entityData.getComponent(spellEntity, CreatureSpell.class);
                if (levelInfo.getLevelData().getCreatureSpellById(spell.creatureSpellId).getFlags().contains(flag) && isAttackRecharged(spell)) {
                    return spellEntity;
                }
            }
        }
        return null;
    }

    private boolean createNavigation(Point currentLocation, Point destination, Point faceTarget) {
        return createNavigation(currentLocation, destination, faceTarget, null);
    }

    private boolean createNavigation(Point currentLocation, Point destination, Point faceTarget, Vector2f workPosition) {
        return createNavigation(currentLocation, destination, faceTarget, workPosition, false);
    }

    private boolean createNavigation(Point currentLocation, Point destination, Point faceTarget, Vector2f workPosition,
            boolean pursuingAttackTarget) {
        return createNavigation(currentLocation, destination, faceTarget, workPosition, pursuingAttackTarget, false);
    }

    private boolean createNavigation(Point currentLocation, Point destination, Point faceTarget, Vector2f workPosition,
            boolean pursuingAttackTarget, boolean endInObstacle) {
        GraphPath<IMapTileInformation> path = navigationService.findPath(currentLocation, destination, this);
        if (path == null) {
            entityData.setComponent(entityId, new PathRetry(destination, gameTimer.getGameTime()));
            logger.log(Level.WARNING, "No path from {0} to {1}", getCreatureCoordinates(), destination);
            return true;
        }

        // An enemy door in the way is not walked through, it is broken down
        List<Point> tiles = new ArrayList<>(path.getCount());
        path.forEach(tile -> tiles.add(tile.getLocation()));
        int doorIndex = DoorAttackTarget.getDoorIndex(tiles, this::isDoorToBreak);
        EntityId door = doorIndex >= DoorAttackTarget.ADJACENT_DOOR_INDEX ? getBlockingDoor(tiles.get(doorIndex)) : null;
        if (doorIndex == DoorAttackTarget.ADJACENT_DOOR_INDEX
                || door != null && isDoorInReach(door, WorldUtils.vector3fToVector2f(getPosition()))) {
            breakDoor(door);
            return false;
        } else if (door != null) {

            // The walk ends on the first tile from where the door is in reach, at the latest on the one next to it
            int stop = DoorAttackTarget.ADJACENT_DOOR_INDEX;
            while (stop < doorIndex - 1 && !isDoorInReach(door, WorldUtils.pointToVector2f(tiles.get(stop)))) {
                stop++;
            }
            path = DoorAttackTarget.stopBefore(path, stop + 1);
            destination = tiles.get(stop);
            faceTarget = tiles.get(doorIndex);
            workPosition = null;
            endInObstacle = false;
        } else {
            entityData.removeComponent(entityId, DoorTarget.class);
        }

        List<Vector2> navigationPath = SteeringUtils.pathToList(path);
        Vector3f position = getPosition();
        if (workPosition != null) {
            SteeringUtils.pathToPosition(navigationPath, new Vector2(position.x, position.z), new Vector2(workPosition.x, workPosition.y));
        } else if (navigationPath.size() > 1) {
            navigationPath.addFirst(new Vector2(position.x, position.z));
        }
        if (navigationPath.size() > 1) {
            navigationPath = navigationService.smoothPath(navigationPath, CreatureSizing.getBodyRadius(creature), this);
        }
        // Task destinations, including beds, can lie inside furniture. Ordinary walks
        // keep contact through their final leg; native job flags remain to be mapped.
        entityData.setComponent(entityId, new Navigation(destination, faceTarget, navigationPath, pursuingAttackTarget, endInObstacle));
        return false;
    }

    /**
     * Tells if the assigned task is where a search for a path found nothing
     * lately. Searching again at once, on every tick, would only fail again
     */
    private boolean isWaitingToRetryPath() {
        Task assignedTask = getAssignedTask();
        Vector2f target = assignedTask != null ? assignedTask.getTarget(this) : null;
        PathRetry pathRetry = entityData.getComponent(entityId, PathRetry.class);
        return target != null && pathRetry != null && pathRetry.isWaiting(WorldUtils.vectorToPoint(target), gameTimer.getGameTime());
    }

    @Override
    public boolean attackAdjacentDoor() {
        // The native idle door scan checks willingness before looking at adjacent tiles (0x481400).
        if (entityData.getComponent(entityId, Peaceful.class) != null
                || !DoorAttackTarget.attacksAdjacentWhenIdle(getOwnerId())) {
            return false;
        }
        Position position = entityData.getComponent(entityId, Position.class);
        Point tile = position != null ? WorldUtils.vectorToPoint(position.position) : null;

        // The door we break stays our target while it is in our reach, also after we backed off from it (0x45F030)
        DoorTarget doorTarget = entityData.getComponent(entityId, DoorTarget.class);
        EntityId door = doorTarget != null && tile != null && canBreakDoor(doorTarget.door)
                && isDoorInReach(doorTarget.door, WorldUtils.vector3fToVector2f(position.position))
                ? doorTarget.door : DoorAttackTarget.findAdjacent(tile, this::getBlockingDoor, this::canBreakDoor);
        if (door == null) {
            entityData.removeComponent(entityId, DoorTarget.class);
            return false;
        }
        breakDoor(door);
        return true;
    }

    @Override
    public boolean canBreakDoor(EntityId door) {
        // A follower keeps behind its leader, and the leader breaks the door (DKII.EXE 0x462130 walks with
        // the own-doors-only flag, see 0x4C8BD0)
        IPartyController party = getParty();
        if (party != null && !party.isPartyLeader(this)) {
            return false;
        }
        return DoorAttackTarget.canBreak(entityData, door, creature.getFightStyle(), isEnemy(door));
    }

    private boolean isDoorToBreak(Point tile) {
        EntityId door = getBlockingDoor(tile);
        return door != null && canBreakDoor(door);
    }

    private EntityId getBlockingDoor(Point tile) {
        return entityPositionLookup.getBlockingDoor(tile.x, tile.y, getOwnerId());
    }

    @Override
    public boolean attackVisibleTrap() {
        // Ordinary trap selection uses the same willingness gate as creature combat (DK II 0x45A6D0).
        if (entityData.getComponent(entityId, Peaceful.class) != null
                || !TrapAttackTarget.attacksTraps(creature.getFightStyle(), getOwnerId())) {
            return false;
        }
        EntityId trap = TrapAttackTarget.findNearest(entityPositionLookup.getSensedEntities(entityId), this::isTrapToAttack,
                this::getDistanceToCreature, creature.getAttributes().getDistanceCanSee(), isWorker(),
                target -> isWithinAttackDistance(target) || hasPathToEntity(target));
        if (trap == null) {
            return false;
        }

        if (isWithinAttackDistance(trap)) {
            stopCreature();
            executeAttack(trap);
        } else {
            navigateToAttackTarget(trap);
        }
        return true;
    }

    private boolean isTrapToAttack(EntityId entity) {
        return TrapAttackTarget.isAttackable(entityData, entity) && isEnemy(entity);
    }

    /**
     * Breaks a door in our way: attacks it when it is in reach, and otherwise
     * steps up to it first, the door then being next to us. The door is a
     * target of the original's fight state like any other (0x481400), and we
     * keep it with the attack we go for it with.
     *
     * @param door the door
     */
    private void breakDoor(EntityId door) {
        DoorTarget doorTarget = entityData.getComponent(entityId, DoorTarget.class);
        boolean melee = getDoorAttackSpell(door).isEmpty();
        if (doorTarget == null || !door.equals(doorTarget.door) || doorTarget.melee != melee) {
            entityData.setComponent(entityId, new DoorTarget(door, melee));
        }

        Vector3f position = getPosition();
        if (isDoorInReach(door, WorldUtils.vector3fToVector2f(position))) {
            attackDoor(door);
            return;
        }

        Point tile = getCreatureCoordinates();
        Point doorTile = getCreatureCoordinates(entityData, door);
        Vector2f spot = DoorAttackTarget.getApproachPoint(doorTile, tile, getDoorReach(door));
        entityData.setComponent(entityId, new Navigation(tile, doorTile, new ArrayList<>(List.of(
                new Vector2(position.x, position.z), new Vector2(spot.x, spot.y)))));
    }

    /**
     * Is a door in reach from a spot. Like DK II (0x45E8F0), the door also
     * needs to be in sight.
     *
     * @param door the door
     * @param from the spot
     * @return true if we can attack the door from the spot
     */
    private boolean isDoorInReach(EntityId door, Vector2f from) {
        Vector3f doorPosition = getPosition(entityData, door);
        return WorldUtils.vector3fToVector2f(doorPosition).distance(from) < getDoorReach(door)
                && entityPositionLookup.hasLineOfSight(new Vector3f(from.x, doorPosition.y, from.y), doorPosition);
    }

    /**
     * How far from a door's centre we attack it: the range of the attack spell
     * we cast at it, or else our melee reach for a door (0x45CB00)
     *
     * @param door the door
     * @return the reach
     */
    private float getDoorReach(EntityId door) {
        Optional<CreatureSpell> spell = getDoorAttackSpell(door);
        return spell.isPresent() ? spell.get().range : DoorAttackTarget.getMeleeReach(levelInfo.getLevelData().getDoorById(
                entityData.getComponent(door, DoorComponent.class).doorId), getMeleeRange());
    }

    /**
     * The attack spell we cast at a door. DK II picks it as against other
     * targets, the one that was ready first, but leaves out the spells that
     * only work on creatures, and a support creature also takes one that
     * still recharges and waits for it (0x45CB00, 0x45F030). A door is a
     * target given to the creature, so DK II doesn't look at its distance
     * here: the spell is cast even from right next to the door. Once the
     * creature goes for the door in melee it doesn't pick again (0x45F030).
     *
     * @param door the door
     * @return the spell, empty to hit the door in melee
     */
    private Optional<CreatureSpell> getDoorAttackSpell(EntityId door) {
        DoorTarget doorTarget = entityData.getComponent(entityId, DoorTarget.class);
        if (doorTarget != null && doorTarget.melee && door.equals(doorTarget.door)) {
            return Optional.empty();
        }

        boolean support = creature.getFightStyle() == Creature.FightStyle.SUPPORT;
        return getCreatureSpells()
                .stream()
                .filter(creatureSpell -> {
                    EnumSet<CreatureSpellFlag> flags = levelInfo.getLevelData().getCreatureSpellById(creatureSpell.creatureSpellId).getFlags();
                    return (support || isAttackRecharged(creatureSpell)) && flags.contains(CreatureSpellFlag.IS_ATTACKING)
                            && !flags.contains(CreatureSpellFlag.CAST_ONLY_ON_CREATURES);
                })
                .min(READY_FIRST);
    }

    /**
     * Attacks a door in reach as often as the attack recharges, with the
     * attack spell or else in melee. The creature turns to it before every
     * attack, and the hit is a normal melee hit (0x45F030, 0x498E30).
     *
     * @param door the door
     */
    private void attackDoor(EntityId door) {
        Optional<CreatureSpell> spell = getDoorAttackSpell(door);
        boolean waitingForSpell = spell.isPresent() && !isAttackRecharged(spell.get());
        if (waitingForSpell && !isStopped() || !faceAttackTarget(door)) {
            return; // Waiting for its spell, a support creature first finishes its step back
        }

        CreatureMeleeAttack creatureMeleeAttack = entityData.getComponent(entityId, CreatureMeleeAttack.class);
        if (spell.isPresent()) {
            if (waitingForSpell) {
                backOffFromDoor(door, spell.get().range);
            } else {
                castAttackSpell(spell.get(), door);
            }
        } else if (creatureMeleeAttack != null && isAttackRecharged(creatureMeleeAttack)) {
            swingMelee(door, creatureMeleeAttack);
        }
    }

    /**
     * A support creature that waits for its spell nearer to the door than half
     * the spell's range and half the distance it can see backs away from it,
     * facing it. DK II shuffles it straight back, or else along the wall
     * behind it (0x45F030, 0x460DA0). Here it steps back a tile.
     *
     * @param door the door
     * @param range the range of the spell
     */
    private void backOffFromDoor(EntityId door, float range) {
        Vector3f position = getPosition();
        Vector2f away = WorldUtils.vector3fToVector2f(position).subtractLocal(WorldUtils.vector3fToVector2f(getPosition(entityData, door)));
        float distance = away.length();
        if (distance >= range / 2 || distance >= creature.getAttributes().getDistanceCanSee() / 2 || distance == 0) {
            return;
        }

        Point tile = getCreatureCoordinates();
        away.divideLocal(distance);
        int x = Math.round(away.x);
        int y = Math.round(away.y);
        for (Point step : List.of(new Point(tile.x + x, tile.y + y), new Point(tile.x, tile.y + y), new Point(tile.x + x, tile.y))) {
            if (!step.equals(tile) && navigationService.findPath(tile, step, this) != null) {
                entityData.setComponent(entityId, new Navigation(step, getCreatureCoordinates(entityData, door), new ArrayList<>(List.of(
                        new Vector2(position.x, position.z), WorldUtils.pointToVector2(step)))));
                return;
            }
        }
    }

    @Override
    public boolean isAtAssignedTaskTarget() {
        Task assignedTask = getAssignedTask();

        // Stopped comes first and the target is asked once, a task may search a path for its target
        if (assignedTask == null || !isStopped()) {
            return false;
        }
        Vector2f target = assignedTask.getTarget(this);
        if (target == null) {
            return false;
        }

        // A task may want us somewhere else on the tile, such as a trainee standing around its dummy
        if (isNear(target)) {
            return true;
        }
        Vector2f workPosition = assignedTask.getWorkPosition(this);
        return workPosition != null && isAt(workPosition);
    }

    private boolean isNear(Vector2f target) {
        Vector3f currentPos = getPosition();
        return (target.distanceSquared(currentPos.x, currentPos.z) < 0.5f);
    }

    /**
     * Standing on the spot, not just on the tile. Well above the arrival
     * tolerance of the steering (0.1), or the creature keeps stepping around
     * the spot
     */
    private boolean isAt(Vector2f target) {
        Vector3f currentPos = getPosition();
        return (target.distanceSquared(currentPos.x, currentPos.z) < 0.25f * 0.25f);
    }

    @Override
    public void dropGold() {
        Gold gold = entityData.getComponent(entityId, Gold.class);
        if (gold == null || gold.gold <= 0) {
            return;
        }

        // Nowhere to take the gold, leave it on the floor so we can carry on working
        entityData.setComponent(entityId, new Gold(0, gold.maxGold));
        addLooseGold(gold.gold);
    }

    @Override
    public boolean isWorkNavigationRequired() {
        // TODO:
        return false;
    }

    @Override
    public boolean isAssignedTaskValid() {
        Task assignedTask = getAssignedTask();
        return (assignedTask != null && assignedTask.isValid(this));
    }

    @Override
    public IEntityController getAttackTarget() {
        CombatPitFighter pitFighter = entityData.getComponent(entityId, CombatPitFighter.class);
        if (pitFighter != null) {
            return getCombatPitOpponent(pitFighter.pit);
        }

        // One that won't fight neither picks a target nor keeps the one it has (DK II 0x45A6D0, 0x45F030)
        if (entityData.getComponent(entityId, Peaceful.class) != null) {
            setAttackTarget(null);
            return null;
        }
        AttackTarget attackTarget = entityData.getComponent(entityId, AttackTarget.class);
        EntityId target = attackTarget != null && isTargetToKeep(attackTarget.entityId) ? attackTarget.entityId : null;

        // A Horned Reaper picks his prey over the whole map and hunts it down even when it runs (DKII.EXE 0x45E1B0)
        if (target != null && creature.getFlags().contains(Creature.CreatureFlag.IS_HORNY)) {
            return DungeonHeartTarget.isDungeonHeart(entityData, target) ? objectsController.createController(target)
                    : creaturesController.createController(target);
        }

        // The original picks its target again when it steps onto another tile or every 10 turns, and keeps the one it
        // has when it finds none (0x45E9F0). Chasing a creature, it drops it and picks again every turn, so it only
        // chases on while it picks the same one (0x470E30), except a Horned Reaper, who hunts his prey over the
        // whole map (0x45E1B0). Fighting a creature or a heart's pillar in reach, it keeps
        // it: 0x45E9F0 does not pick in the fight states, and the fight state picks only when it has no target, between
        // swings it just waits (0x45F030, 0x48C9B0). Hitting a heart with a spell from further away, it picks again on
        // every turn its spell is still recharging (0x45F030 calls 0x45CB00 while 0x48C800 says the spell isn't ready).
        // A support creature picks again on every turn of the fight state that isn't in the middle of a swing or a cast
        // (0x45F030 with fight style 4)
        boolean inReach = target != null && isWithinAttackDistance(target);
        boolean chasing = target != null && !inReach && !DungeonHeartTarget.isDungeonHeart(entityData, target);
        boolean fighting = inReach && (stateMachine.isInState(CreatureState.FIGHT) || stateMachine.isInState(CreatureState.MELEE_ATTACK));
        boolean waitingToCast = target != null && !inReach && stateMachine.isInState(CreatureState.FIGHT) && isWaitingToCast(target);
        boolean supporting = target != null && stateMachine.isInState(CreatureState.FIGHT)
                && creature.getFightStyle() == Creature.FightStyle.SUPPORT;
        boolean picked = target == null || chasing || waitingToCast || supporting || !fighting && isTimeToPickTarget(attackTarget);
        if (picked) {
            EntityId enemy = pickAttackTarget();
            if (enemy != null || chasing && !creature.getFlags().contains(Creature.CreatureFlag.IS_HORNY)) {
                target = enemy;
            }
        }
        if (target == null) {
            setAttackTarget(null);
            return null;
        }

        // It counts how long it chases one that runs away, and gives up on it after the chase duration of its side
        // (0x470E30). Then it leaves those that run away alone for a while. A Horned Reaper hunts his prey down even when
        // it runs (0x45E1B0), so he never gives up
        double chaseStartTime = Double.NaN;
        if (isFleeing(target)) {
            chaseStartTime = attackTarget != null && !Double.isNaN(attackTarget.chaseStartTime) ? attackTarget.chaseStartTime
                    : isWithinAttackDistance(target) ? Double.NaN : gameTimer.getGameTime();
            if (gameTimer.getGameTime() - chaseStartTime >= getChaseDuration() && !isWithinAttackDistance(target)
                    && !creature.getFlags().contains(Creature.CreatureFlag.IS_HORNY)) {
                entityData.setComponent(entityId, new ChaseBackOff(gameTimer.getGameTime()));
                setAttackTarget(null);
                return null;
            }
        }
        if (picked) {
            try {
                heroLogTarget(target);
            } catch (RuntimeException e) {
                // Debug logging must never break the game
            }
            entityData.setComponent(entityId, new AttackTarget(target, gameTimer.getGameTime(), getCreatureCoordinates(), chaseStartTime));
        } else if (Double.compare(chaseStartTime, attackTarget.chaseStartTime) != 0) {
            entityData.setComponent(entityId, new AttackTarget(target, attackTarget.time, attackTarget.tile, chaseStartTime));
        }

        return DungeonHeartTarget.isDungeonHeart(entityData, target) ? objectsController.createController(target)
                : creaturesController.createController(target);
    }

    /**
     * DEBUG: a hero picks a keeper owned target, logged on the first one and
     * whenever the target's owner changes
     */
    private void heroLogTarget(EntityId target) {
        if (getOwnerId() != Player.GOOD_PLAYER_ID) {
            return;
        }
        Owner owner = entityData.getComponent(target, Owner.class);
        if (owner == null || owner.ownerId < Player.KEEPER1_ID) {
            return;
        }
        boolean heart = DungeonHeartTarget.isDungeonHeart(entityData, target);
        CreatureComponent cc = entityData.getComponent(target, CreatureComponent.class);
        Vector3f from = getPosition();
        Vector3f to = EntityController.getPosition(entityData, target);
        float tiles = from != null && to != null ? from.distance(to) / WorldUtils.TILE_WIDTH : -1;
        toniarts.openkeeper.game.HeroLog.logChange("target" + entityId.getId(), String.valueOf(owner.ownerId),
                String.format("HERO_TARGET hero=%d creatureId=%d target=%s id=%d owner=%d heroPos=%s targetPos=%s dist=%.1f tiles",
                        entityId.getId(), entityData.getComponent(entityId, CreatureComponent.class).creatureId,
                        heart ? "DUNGEON_HEART" : cc != null ? "creature" + cc.creatureId : "object", target.getId(), owner.ownerId,
                        from, to, tiles));
    }

    private boolean isTargetToKeep(EntityId target) {
        if (DungeonHeartTarget.isDungeonHeart(entityData, target)) {
            return isHeartToAttack(target);
        }
        return isEnemy(target) && !(isIncapacitated(target) || isCaptive(target) || isUnreachable(target) || isInvisible(target));
    }

    private boolean isTimeToPickTarget(AttackTarget attackTarget) {
        int turns = creature.getFightStyle() == Creature.FightStyle.NON_FIGHTER ? IMP_TARGET_PICK_TURNS : TARGET_PICK_TURNS;
        return !getCreatureCoordinates().equals(attackTarget.tile) || gameTimer.getGameTime() - attackTarget.time >= getTurns(turns);
    }

    /**
     * How long we chase an enemy that runs away, 3 seconds for a good creature
     * and 7 for an evil one
     */
    private double getChaseDuration() {
        Variable.MiscVariable chaseDuration = gameSettings.get(getOwnerId() == Player.GOOD_PLAYER_ID
                ? Variable.MiscVariable.MiscType.GOOD_CREATURES_CHASE_ENEMY_DURATION_SECONDS
                : Variable.MiscVariable.MiscType.EVIL_CREATURES_CHASE_ENEMY_DURATION_SECONDS);
        return chaseDuration != null ? chaseDuration.getValue() : 0;
    }

    private boolean isBackingOff() {
        ChaseBackOff backOff = entityData.getComponent(entityId, ChaseBackOff.class);
        return backOff != null && gameTimer.getGameTime() - backOff.time < getTurns(CHASE_BACK_OFF_TURNS);
    }

    private double getTurns(int turns) {
        return (double) turns / levelInfo.getLevelData().getGameLevel().getTicksPerSec();
    }

    /**
     * Picks the enemy to attack. A fighter always comes before an enemy heart,
     * a worker only while the heart's nearest pillar isn't strictly nearer, so
     * on a tie the worker is kept (0x45CB00).
     *
     * @return the enemy to attack, {@code null} if there is none
     */
    private EntityId pickAttackTarget() {
        EntityId enemy = getNearestEnemy();
        if (enemy != null && !creaturesController.createController(enemy).isWorker()) {
            return enemy;
        }
        EntityId heart = getNearestEnemyHeart(enemy != null ? getDistanceToCreature(enemy) : Float.MAX_VALUE);
        return heart != null ? heart : enemy;
    }

    private boolean isAttacking(EntityId attacker, EntityId target) {
        AttackTarget attackTarget = entityData.getComponent(attacker, AttackTarget.class);
        return attackTarget != null && target.equals(attackTarget.entityId);
    }

    /**
     * Checks that the target can take one more attacker, we may already be one
     * of them. Like DK II (0x45A9A0), the melee attackers and the ones with
     * attack spells have room for 8 each, and one with spells that finds no
     * room among them fights in melee (0x45A550)
     *
     * @param target the creature we would like to attack
     * @return true if we can join the fight against the target
     */
    private boolean hasRoomForAttacker(EntityId target) {
        int meleeAttackers = 0;
        int rangedAttackers = 0;
        for (EntityId attacker : entityData.findEntities(new FieldFilter<>(AttackTarget.class, "entityId", target), AttackTarget.class)) {
            if (attacker.equals(entityId)) {
                return true;
            }
            if (hasAttackSpell(attacker)) {
                rangedAttackers++;
            } else {
                meleeAttackers++;
            }
        }
        meleeAttackers += Math.max(0, rangedAttackers - CombatRules.MAX_RANGED_ATTACKERS);

        return meleeAttackers < CombatRules.MAX_MELEE_ATTACKERS
                || rangedAttackers < CombatRules.MAX_RANGED_ATTACKERS && hasAttackSpell(entityId);
    }

    private boolean hasAttackSpell(EntityId creature) {
        CreatureSpells creatureSpells = entityData.getComponent(creature, CreatureSpells.class);
        if (creatureSpells != null) {
            for (EntityId creatureSpellEntity : creatureSpells.creatureSpells) {
                CreatureSpell spell = entityData.getComponent(creatureSpellEntity, CreatureSpell.class);
                if (spell != null && levelInfo.getLevelData().getCreatureSpellById(spell.creatureSpellId).getFlags().contains(CreatureSpellFlag.IS_ATTACKING)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Gets the creature we fight in the Combat Pit. An enemy fighter comes
     * first, then the nearest one of our own side.
     *
     * @param pit the Combat Pit we fight in
     * @return the opponent, or {@code null} if there is none
     */
    private ICreatureController getCombatPitOpponent(EntityId pit) {
        AttackTarget attackTarget = entityData.getComponent(entityId, AttackTarget.class);
        if (attackTarget != null && isCombatPitOpponent(attackTarget.entityId, pit)) {
            return creaturesController.createController(attackTarget.entityId);
        }

        // We beat our opponent, winning cheers us up. Picking the next opponent clears the beaten one, so this is once a win
        CombatPitFighter beaten = attackTarget != null ? entityData.getComponent(attackTarget.entityId, CombatPitFighter.class) : null;
        CreatureMood mood = entityData.getComponent(entityId, CreatureMood.class);
        if (beaten != null && pit.equals(beaten.pit) && isIncapacitated(attackTarget.entityId) && mood != null) {
            entityData.setComponent(entityId, mood.add(CreatureMood.REASON_OTHER,
                    (int) gameSettings.get(Variable.MiscVariable.MiscType.MODIFY_ANGER_OF_COMBAT_PIT_VICTOR).getValue()));
        }

        EntityId nearestEnemy = null;
        float nearestEnemyDistance = Float.MAX_VALUE;
        EntityId nearestOther = null;
        float nearestOtherDistance = Float.MAX_VALUE;
        if (!isAtTopCombatPitLevel(entityId)) {
            for (EntityId entity : entityData.findEntities(new FieldFilter<>(CombatPitFighter.class, "pit", pit), CombatPitFighter.class)) {
                if (!isCombatPitOpponent(entity, pit)) {
                    continue;
                }
                float distance = getDistanceToCreature(entity);
                if (isEnemy(entity)) {
                    if (distance < nearestEnemyDistance) {
                        nearestEnemyDistance = distance;
                        nearestEnemy = entity;
                    }
                } else if (distance < nearestOtherDistance) {
                    nearestOtherDistance = distance;
                    nearestOther = entity;
                }
            }
        }

        EntityId opponent = nearestEnemy != null ? nearestEnemy : nearestOther;
        setAttackTarget(opponent);
        return opponent != null ? creaturesController.createController(opponent) : null;
    }

    private boolean isCombatPitOpponent(EntityId entity, EntityId pit) {
        CombatPitFighter pitFighter = entityData.getComponent(entity, CombatPitFighter.class);
        return !entity.equals(entityId) && pitFighter != null && pit.equals(pitFighter.pit)
                && !isIncapacitated(entity) && !isAtTopCombatPitLevel(entity);
    }

    /**
     * Creatures at the top level the Combat Pit teaches neither fight nor are
     * fought there
     */
    private boolean isAtTopCombatPitLevel(EntityId entity) {
        CreatureExperience creatureExperience = entityData.getComponent(entity, CreatureExperience.class);
        return creatureExperience != null && creatureExperience.level >= (int) gameSettings.get(Variable.MiscVariable.MiscType.COMBAT_PIT_MAX_EXPERIENCE_LEVEL).getValue();
    }

    /**
     * Finds the enemy creature we sense and can fight, the nearest enemy
     * already attacking us first
     *
     * @return the creature, or {@code null} if there is none
     */
    private EntityId getNearestEnemy() {
        EntityId nearestEnemy = null;
        float bestScore = Float.MAX_VALUE;
        EntityId nearestAttacker = null;
        float nearestAttackerDistance = Float.MAX_VALUE;
        for (EntityId entity : getEntitiesInNoticeRange()) {
            if (creaturesController.isValidEntity(entity)
                    && isEnemy(entity)
                    && !(isIncapacitated(entity) || isCaptive(entity) || isInvisible(entity))
                    && !(isFleeing(entity) && isBackingOff() && !isWithinAttackDistance(entity))
                    && hasRoomForAttacker(entity)
                    && isNoticed(entity)
                    && (hasPathToEntity(entity) || isWithinAttackDistance(entity))) {
                float distance = getTargetDistance(entity);
                float score = distance + getFightStyleScore(entity);
                if (score < bestScore) {
                    bestScore = score;
                    nearestEnemy = entity;
                }
                if (distance < nearestAttackerDistance && isAttacking(entity, entityId)) {
                    nearestAttackerDistance = distance;
                    nearestAttacker = entity;
                }
            }
        }

        return nearestAttacker != null ? nearestAttacker : nearestEnemy;
    }

    /**
     * The squared distance to a target, 7/8 of it for a natural enemy: a good
     * creature against an evil one and the other way round (0x45C640)
     */
    private float getTargetDistance(EntityId target) {
        float distance = getDistanceToCreature(target);
        CreatureComponent other = entityData.getComponent(target, CreatureComponent.class);
        Creature.CreatureFlag naturalEnemy = creature.getFlags().contains(Creature.CreatureFlag.IS_GOOD)
                ? Creature.CreatureFlag.IS_EVIL : Creature.CreatureFlag.IS_GOOD;
        boolean isNaturalEnemy = other != null
                && levelInfo.getLevelData().getCreature(other.creatureId).getFlags().contains(naturalEnemy);
        return distance * distance * (isNaturalEnemy ? 7f / 8 : 1);
    }

    /**
     * What our fight style adds to a target's squared distance, in our sight
     * squared. A blocker takes the nearest (0x45B140), a blitzer goes for
     * the support creatures (0x45B800), and flankers and support creatures go
     * for an enemy a blocker or a blitzer already holds (0x45BAD0, 0x45B500)
     */
    private float getFightStyleScore(EntityId target) {
        Creature.FightStyle fightStyle = creature.getFightStyle();
        int holders = 0;
        int supporters = 0;
        if (fightStyle == Creature.FightStyle.FLANKER || fightStyle == Creature.FightStyle.SUPPORT) {
            for (EntityId attacker : entityData.findEntities(new FieldFilter<>(AttackTarget.class, "entityId", target), AttackTarget.class)) {
                Creature.FightStyle attackerStyle = getEntityFightingStyle(attacker);
                if (attackerStyle == Creature.FightStyle.BLITZER || attackerStyle == Creature.FightStyle.BLOCKER) {
                    holders++;
                } else if (attackerStyle == Creature.FightStyle.SUPPORT) {
                    supporters++;
                }
            }
        }

        int sightSquares = 0;
        if (fightStyle == Creature.FightStyle.BLITZER) {
            sightSquares = getEntityFightingStyle(target) == Creature.FightStyle.SUPPORT ? 0 : 1;
        } else if (fightStyle == Creature.FightStyle.FLANKER) {
            sightSquares = holders > 0 ? holders : supporters > 0 ? supporters + 8 : 17;
        } else if (fightStyle == Creature.FightStyle.SUPPORT) {
            sightSquares = holders > 0 ? holders : 9;
        }
        float sight = creature.getAttributes().getDistanceCanSee();
        return sightSquares * sight * sight;
    }

    /**
     * Finds the nearest enemy Dungeon Heart we sense and can get next to
     *
     * @param maxDistance the heart needs to be nearer than this
     * @return the heart, or {@code null} if there is none
     */
    private EntityId getNearestEnemyHeart(float maxDistance) {
        EntityId nearestHeart = null;
        float nearestDistance = maxDistance;
        for (EntityId entity : entityPositionLookup.getSensedEntities(entityId)) {
            if (DungeonHeartTarget.isDungeonHeart(entityData, entity) && isHeartToAttack(entity)
                    && getHeartAttackTile(entity) != null) {
                float distance = getDistanceToHeart(entity);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestHeart = entity;
                }
            }
        }

        return nearestHeart;
    }

    private float getMeleeRange() {
        return entityData.getComponent(entityId, CreatureMeleeAttack.class).range;
    }

    private boolean isHeartToAttack(EntityId heart) {
        Health health = entityData.getComponent(heart, Health.class);
        return health != null && health.health > 0 && isEnemy(heart)
                && creature.getFightStyle() != Creature.FightStyle.NON_FIGHTER;
    }

    /**
     * How far the heart's nearest pillar is, the original goes for the pillars
     */
    private float getDistanceToHeart(EntityId heart) {
        Vector3f position = getPosition();
        Vector2f ourPosition = new Vector2f(position.x, position.z);
        return DungeonHeartTarget.getNearestPillar(ourPosition, getHeartTile(heart)).distance(ourPosition);
    }

    private Point getHeartTile(EntityId heart) {
        return WorldUtils.vectorToPoint(getPosition(entityData, heart));
    }

    private Point getHeartAttackTile(EntityId heart) {
        Point ourPosition = getCreatureCoordinates();
        return DungeonHeartTarget.getAttackTile(ourPosition, getHeartTile(heart), getMeleeRange(),
                tile -> tile.equals(ourPosition) || isWalkable(tile) && navigationService.findPath(ourPosition, tile, this) != null);
    }

    /**
     * A cheap check to save a path search to a wall
     */
    private boolean isWalkable(Point tile) {
        IMapTileInformation mapTile = mapController.getMapData().getTile(tile);
        return mapTile != null && !mapController.getTerrain(mapTile).getFlags().contains(Terrain.TerrainFlag.SOLID);
    }

    /**
     * Whether we notice the enemy. DK II sees an enemy nearer than the sight
     * distance with no solid tile in between, and hears one nearer than the
     * hearing distance when the walk there is shorter than that too
     * (0x45B140, 0x45BAD0, 0x45B500). An enemy further away is left alone,
     * even one we could walk to.
     */
    private boolean isNoticed(EntityId entity) {
        Senses senses = entityData.getComponent(entityId, Senses.class);
        if (senses == null) {
            return true; // Only the position lookup limits what it notices
        }
        Vector3f ourPosition = getPosition();
        Vector3f theirPosition = getPosition(entityData, entity);
        if (ourPosition == null || theirPosition == null) {
            return false;
        }
        float distance = ourPosition.distance(theirPosition) / WorldUtils.TILE_WIDTH;
        if (isOfOtherAlignment(entity)) {
            distance *= OTHER_ALIGNMENT_DISTANCE_FACTOR;
        }
        if (distance < senses.distanceCanSee && entityPositionLookup.hasLineOfSight(ourPosition, theirPosition)) {
            return true;
        }
        if (distance >= senses.distanceCanHear) {
            return false;
        }

        // The walk is measured like DK II does it, a diagonal step counts one and a half
        Point ourPos = WorldUtils.vectorToPoint(ourPosition);
        Point theirPos = WorldUtils.vectorToPoint(theirPosition);
        GraphPath<IMapTileInformation> path = ourPos.equals(theirPos) ? null : navigationService.findPath(ourPos, theirPos, this);
        if (path == null) {
            return ourPos.equals(theirPos);
        }
        float walk = 0;
        Point previous = ourPos;
        for (IMapTileInformation tile : path) {
            int dx = Math.abs(tile.getX() - previous.x);
            int dy = Math.abs(tile.getY() - previous.y);
            walk += Math.max(dx, dy) + Math.min(dx, dy) / 2f;
            previous = tile.getLocation();
        }
        return walk < senses.distanceCanHear;
    }

    /**
     * Everything on the tiles within our sight or hearing distance. DK II goes
     * through every enemy creature and measures the straight distance
     * (0x45B140), so this is a square of tiles rather than the sensed area,
     * which only reaches that far in steps along the map axes and misses the
     * diagonals.
     */
    private Collection<EntityId> getEntitiesInNoticeRange() {
        Senses senses = entityData.getComponent(entityId, Senses.class);
        if (senses == null) {
            return entityPositionLookup.getSensedEntities(entityId);
        }
        IMapTileInformation ourTile = entityPositionLookup.getEntityLocation(entityId);
        if (ourTile == null) {
            return Collections.emptyList();
        }
        int range = (int) FastMath.ceil(Math.max(senses.distanceCanSee, senses.distanceCanHear) / OTHER_ALIGNMENT_DISTANCE_FACTOR);
        List<EntityId> entities = new ArrayList<>();
        for (int x = ourTile.getX() - range; x <= ourTile.getX() + range; x++) {
            for (int y = ourTile.getY() - range; y <= ourTile.getY() + range; y++) {
                IMapTileInformation tile = mapController.getMapData().getTile(x, y);
                if (tile != null) {
                    entities.addAll(entityPositionLookup.getEntitiesInLocation(tile));
                }
            }
        }
        entities.remove(entityId);

        return entities;
    }

    /**
     * Whether the other creature is of the alignment we fight. DK II checks
     * the evil flag of the enemy when we are good, otherwise its good flag
     * (0x45B140).
     */
    private boolean isOfOtherAlignment(EntityId entity) {
        CreatureComponent otherCreature = entityData.getComponent(entity, CreatureComponent.class);
        if (otherCreature == null) {
            return false;
        }
        Creature.CreatureFlag otherAlignment = creature.getFlags().contains(Creature.CreatureFlag.IS_GOOD)
                ? Creature.CreatureFlag.IS_EVIL : Creature.CreatureFlag.IS_GOOD;

        return levelInfo.getLevelData().getCreature(otherCreature.creatureId).getFlags().contains(otherAlignment);
    }

    /**
     * Tells if a target is out of reach with no way to it. DK II's fight
     * state picks a target again whenever its target is out of reach, and only
     * one it can walk to (0x45F030, 0x45CB00), so such a target is dropped.
     * One already walked to has a way to it.
     *
     * @param entity the target
     * @return true if it can't be hit nor walked to
     */
    private boolean isUnreachable(EntityId entity) {
        return entityData.getComponent(entityId, Navigation.class) == null && !isWithinAttackDistance(entity)
                && !hasPathToEntity(entity);
    }

    private boolean hasPathToEntity(EntityId entity) {
        Point ourPos = WorldUtils.vectorToPoint(getPosition());
        Point theirPos = WorldUtils.vectorToPoint(getPosition(entityData, entity));

        return ourPos.equals(theirPos) || navigationService.findPath(ourPos, theirPos, this) != null;
    }

    private void setAttackTarget(EntityId entity) {
        if (entity == null) {
            entityData.removeComponent(entityId, AttackTarget.class);
        } else {
            entityData.setComponent(entityId, new AttackTarget(entity, gameTimer.getGameTime(), getCreatureCoordinates(), Double.NaN));
        }
    }

    @Override
    public boolean isWithinAttackDistance(EntityId attackTarget) {
        if (DungeonHeartTarget.isDungeonHeart(entityData, attackTarget)) {
            return DungeonHeartTarget.isInReach(WorldUtils.vector3fToVector2f(getPosition()), getHeartTile(attackTarget), getMeleeRange());
        }
        float distanceNeeded = Math.max(getPreferredAttackDistance(), getPossibleAttackDistance());

        // TODO: currently we move only with a tile precision, so accept attack if on the same tile already
        return isInRange(distanceNeeded, attackTarget);
    }

    /**
     * Gets currently possible maximum attack distance for this creature,
     * includes the cooldowns
     *
     * @return maximum distance can attack at the moment
     */
    private float getPossibleAttackDistance() {
        float distanceNeeded = entityData.getComponent(entityId, CreatureMeleeAttack.class).range;
        for (CreatureSpell attack : getCreatureSpells()) {
            toniarts.openkeeper.tools.convert.map.CreatureSpell spell = levelInfo.getLevelData().getCreatureSpellById(attack.creatureSpellId);
            if (spell.getFlags().contains(CreatureSpellFlag.IS_ATTACKING) && isAttackRecharged(attack)) {
                distanceNeeded = Math.max(distanceNeeded, attack.range);
            }
        }

        return distanceNeeded;
    }

    /**
     * Gets the preferred attack distance from target for this creature's
     * figting style
     *
     * @return preferred attack distance
     */
    private float getPreferredAttackDistance() {
        float distanceNeeded = entityData.getComponent(entityId, CreatureMeleeAttack.class).range; // The melee range, the shortest range
        if (creature.getFightStyle() == Creature.FightStyle.SUPPORT) {

            // Get max distance we can cast all spells, and hopefully stay safe
            Optional<Float> shortestDistance = getShortestAttackSpellDistanceNeeded();
            if (shortestDistance.isPresent()) {
                distanceNeeded = shortestDistance.get();
            }
        }

        return distanceNeeded;
    }

    /**
     * Is the target in the range of one of our attack spells, with none of
     * those that reach it recharged yet
     */
    private boolean isWaitingToCast(EntityId target) {
        boolean inSpellRange = false;
        for (CreatureSpell attack : getCreatureSpells()) {
            if (levelInfo.getLevelData().getCreatureSpellById(attack.creatureSpellId).getFlags().contains(CreatureSpellFlag.IS_ATTACKING)
                    && isInRange(attack.range, target)) {
                if (isAttackRecharged(attack)) {
                    return false;
                }
                inSpellRange = true;
            }
        }
        return inSpellRange;
    }

    private Optional<Float> getShortestAttackSpellDistanceNeeded() {
        Float shortestDistance = null;
        for (CreatureSpell attack : getCreatureSpells()) {
            toniarts.openkeeper.tools.convert.map.CreatureSpell spell = levelInfo.getLevelData().getCreatureSpellById(attack.creatureSpellId);
            if (spell.getFlags().contains(CreatureSpellFlag.IS_ATTACKING)) {
                if (shortestDistance == null) {
                    shortestDistance = attack.range;
                } else {
                    shortestDistance = Math.min(shortestDistance, attack.range);
                }
            }
        }

        return Optional.ofNullable(shortestDistance);
    }

    private boolean isInRange(float range, EntityId target) {
        return range >= getDistanceToCreature(target) || (isStopped() && isAtSameTile(target));
    }

    private List<CreatureSpell> getCreatureSpells() {
        CreatureSpells creatureSpells = entityData.getComponent(entityId, CreatureSpells.class);
        if (creatureSpells == null) {
            return Collections.emptyList();
        }

        List<CreatureSpell> spells = new ArrayList<>(creatureSpells.creatureSpells.size());
        for (EntityId entity : creatureSpells.creatureSpells) {
            spells.add(entityData.getComponent(entity, CreatureSpell.class));
        }

        return spells;
    }

    private boolean isAtSameTile(EntityId attackTarget) {
        return getCreatureCoordinates().equals(getCreatureCoordinates(entityData, attackTarget));
    }

    @Override
    public void stop() {
        // Note that this is the updatable stop, not the creature stop...
    }

    @Override
    public boolean turnToAttackTarget(EntityId attackTarget) {
        if (DungeonHeartTarget.isDungeonHeart(entityData, attackTarget)) {
            return true;
        }
        Position position = entityData.getComponent(entityId, Position.class);
        Vector3f targetPosition = getPosition(entityData, attackTarget);
        if (position == null || targetPosition == null) {
            return true;
        }

        // The original turns a creature by its turn speed every turn and starts an attack only once it faces the target
        // (0x45F030, 0x4B2AE0). The turn speed is in radians per second
        float turn = (float) Math.IEEEremainder(-position.rotation - SteeringUtils.calculateVectorToAngle(
                new Vector2(targetPosition.x - position.position.x, targetPosition.z - position.position.z)), FastMath.TWO_PI);
        float maxTurn = creature.getTurnSpeed() / levelInfo.getLevelData().getGameLevel().getTicksPerSec();
        if (turn != 0) {
            entityData.setComponent(entityId, new Position(position.rotation + FastMath.clamp(turn, -maxTurn, maxTurn),
                    position.position.clone()));
        }
        return Math.abs(turn) <= maxTurn;
    }

    @Override
    public void executeAttack(EntityId attackTarget) {

        // Like the original, turn to the target first and attack only when there is no turn left (0x45F030)
        if (!faceAttackTarget(attackTarget)) {
            return;
        }

        // TODO: how to apply the damage? Create a component for THIS creature that adds the damage to enemy after the countdown is finished?
        // Always prefer melee if in range
        CreatureMeleeAttack creatureMeleeAttack = entityData.getComponent(entityId, CreatureMeleeAttack.class);
        if (DungeonHeartTarget.isDungeonHeart(entityData, attackTarget)
                ? isWithinAttackDistance(attackTarget) : isInRange(creatureMeleeAttack.range, attackTarget)) {
            if (isAttackRecharged(creatureMeleeAttack)) {
                swingMelee(attackTarget, creatureMeleeAttack);
            }
        } else {
            getPreferredAttackSpell(attackTarget).ifPresent(attack -> castAttackSpell(attack, attackTarget));
        }
    }

    /**
     * Swings our melee attack at the target, a creature or a door, which then
     * needs to recharge. The hit lands the creature's shot delay into the
     * swing, on a door too (0x498E30)
     *
     * @param attackTarget the target
     * @param creatureMeleeAttack our melee attack
     */
    private void swingMelee(EntityId attackTarget, CreatureMeleeAttack creatureMeleeAttack) {

        // Half of the swings play the second melee animation, if the first one is flagged for it (0x4B37D0)
        CreatureMeleeAttack swing = new CreatureMeleeAttack(creatureMeleeAttack, gameTimer.getGameTime());
        ArtResource melee = creature.getAnimation(Creature.AnimationType.MELEE_ATTACK);
        swing.swipe = melee != null && melee.getFlags().contains(ArtResource.ArtResourceFlag.UNKNOWN_1)
                && creature.getAnimation(Creature.AnimationType.SWIPE) != null && FastMath.rand.nextBoolean();
        entityData.setComponent(entityId, swing);
        stateMachine.changeState(CreatureState.MELEE_ATTACK);

        // The hit lands the creature's shot delay into the swing (0x498E30), counted in whole turns rounded
        // down (0x492320)
        int ticksPerSec = levelInfo.getLevelData().getGameLevel().getTicksPerSec();
        double hitDelay = (int) (creature.getShotDelay() * ticksPerSec) / (double) ticksPerSec;
        entityData.setComponent(entityId, new MeleeHit(attackTarget, gameTimer.getGameTime() + hitDelay));
        turnOnUs(attackTarget);
        releaseMeleeHit();
    }

    /**
     * Casts an attack spell at the target, the spell then needs to recharge
     *
     * @param attack the spell
     * @param attackTarget the target
     */
    private void castAttackSpell(CreatureSpell attack, EntityId attackTarget) {
        CreatureSpells creatureSpells = entityData.getComponent(entityId, CreatureSpells.class);
        for (EntityId creatureSpellEntity : creatureSpells.creatureSpells) {
            CreatureSpell spell = entityData.getComponent(creatureSpellEntity, CreatureSpell.class);
            if (spell.creatureSpellId == attack.creatureSpellId) {
                entityData.setComponent(creatureSpellEntity, new CreatureSpell(attack, gameTimer.getGameTime()));
                break;
            }
        }
        stateMachine.changeState(CreatureState.CAST_SPELL);

        // The shot leaves during the cast, after the spell's shot delay (0x4984D0)
        entityData.setComponent(entityId, new CastShot(attack.creatureSpellId, attackTarget, attack.range,
                gameTimer.getGameTime() + getShotDelay(attack.creatureSpellId)));
        turnOnUs(attackTarget);
    }

    /**
     * A fighting creature we start a swing or a cast at turns on us, unless
     * its own target is a creature no one else attacks. A heart is not a
     * creature, so one hitting a heart turns on us (0x45F030 calls 0x45AA20)
     *
     * @param target the creature we swing or cast at
     */
    private void turnOnUs(EntityId target) {
        CreatureAi ai = entityData.getComponent(target, CreatureAi.class);
        if (ai == null || !(ai.getCreatureState() == CreatureState.FIGHT || ai.getCreatureState() == CreatureState.MELEE_ATTACK
                || ai.getCreatureState() == CreatureState.CAST_SPELL)) {
            return;
        }
        AttackTarget targetsTarget = entityData.getComponent(target, AttackTarget.class);
        if (targetsTarget != null && (entityId.equals(targetsTarget.entityId)
                || !DungeonHeartTarget.isDungeonHeart(entityData, targetsTarget.entityId)
                && entityData.findEntities(new FieldFilter<>(AttackTarget.class, "entityId", targetsTarget.entityId), AttackTarget.class).size() < 2)) {
            return;
        }
        entityData.setComponent(target, new AttackTarget(entityId, gameTimer.getGameTime(),
                WorldUtils.vectorToPoint(getPosition(entityData, target)), Double.NaN));
    }

    @Override
    public void releaseMeleeHit() {
        MeleeHit meleeHit = entityData.getComponent(entityId, MeleeHit.class);
        if (meleeHit == null || gameTimer.getGameTime() < meleeHit.hitTime) {
            return;
        }

        entityData.removeComponent(entityId, MeleeHit.class);

        // The hit lands even if the target has stepped away, as long as it is still on the map
        if (entityData.getComponent(meleeHit.target, Position.class) != null) {
            CreatureMeleeAttack creatureMeleeAttack = entityData.getComponent(entityId, CreatureMeleeAttack.class);
            hitWithMelee(meleeHit.target, creatureMeleeAttack, creatureMeleeAttack.damage);
        }
    }

    @Override
    public boolean hasPendingMeleeHit() {
        return entityData.getComponent(entityId, MeleeHit.class) != null;
    }

    @Override
    public void cancelMeleeHit() {
        entityData.removeComponent(entityId, MeleeHit.class);
    }

    @Override
    public void releaseCastShot() {
        CastShot castShot = entityData.getComponent(entityId, CastShot.class);
        if (castShot == null || gameTimer.getGameTime() < castShot.releaseTime) {
            return;
        }

        entityData.removeComponent(entityId, CastShot.class);

        // TODO: What is the alternative shot all about?
        toniarts.openkeeper.tools.convert.map.CreatureSpell creatureSpell = levelInfo.getLevelData().getCreatureSpellById(castShot.creatureSpellId);
        int shotData1 = getShotData1(creatureSpell);

        // A shot that flies, cast on its own caster, hits it at once: it starts on the caster, the only thing such a
        // shot hits (0x451570). An instant shot, like Heal, is fired like any other
        boolean flyingOnCaster = entityId.equals(castShot.target)
                && levelInfo.getLevelData().getShotById(creatureSpell.getShotTypeId()).getSpeed() > 0;
        if (flyingOnCaster || !shotsController.fireShot(creatureSpell.getShotTypeId(), shotData1, creatureSpell.getShotData2(),
                getOwnerId(), getShotPosition(castShot.creatureSpellId, getPosition(entityData, castShot.target)), castShot.target, entityId, castShot.range)) {
            shotsController.createShot(creatureSpell.getShotTypeId(), shotData1, creatureSpell.getShotData2(), getOwnerId(), getPosition(), castShot.target, entityId);

            // TODO: Of course not like that, the shot is created and it does what it does
            // Set the damage, a shot that makes things has the thing in its value
            if (creatureSpell.getFlags().contains(CreatureSpellFlag.IS_ATTACKING)
                    && levelInfo.getLevelData().getShotById(creatureSpell.getShotTypeId()).getAttackCategory() != Shot.AttackCategory.GENERATION) {
                setDamage(castShot.target, shotData1);
            }
        }
    }

    @Override
    public void cancelCastShot() {
        entityData.removeComponent(entityId, CastShot.class);
    }

    @Override
    public boolean castSupportSpell() {
        CreatureSpells creatureSpells = entityData.getComponent(entityId, CreatureSpells.class);
        if (creatureSpells == null) {
            return false;
        }

        // Of the spells for our own creatures, the last one that is ready gets its go
        EntityId spellEntity = null;
        for (EntityId creatureSpellEntity : creatureSpells.creatureSpells) {
            CreatureSpell spell = entityData.getComponent(creatureSpellEntity, CreatureSpell.class);
            if (isAttackRecharged(spell) && levelInfo.getLevelData().getCreatureSpellById(spell.creatureSpellId).getFlags().contains(CreatureSpellFlag.CREATURE)) {
                spellEntity = creatureSpellEntity;
            }
        }
        if (spellEntity == null) {
            return false;
        }

        CreatureSpell spell = entityData.getComponent(spellEntity, CreatureSpell.class);
        toniarts.openkeeper.tools.convert.map.CreatureSpell creatureSpell = levelInfo.getLevelData().getCreatureSpellById(spell.creatureSpellId);
        EntityId target = getSupportSpellTarget(spell, creatureSpell);
        if (target == null) {
            return false;
        }

        entityData.setComponent(spellEntity, new CreatureSpell(spell, gameTimer.getGameTime()));
        stateMachine.changeState(CreatureState.CAST_SPELL);

        // Cast like an attack spell, the shot leaves after the spell's shot delay (0x45F030, 0x498870)
        entityData.setComponent(entityId, new CastShot(spell.creatureSpellId, target, spell.range,
                gameTimer.getGameTime() + getShotDelay(spell.creatureSpellId)));

        return true;
    }

    /**
     * Gets the creature to cast a spell for our own creatures on. A spell for
     * the caster itself needs the caster to fight. The others go to the nearest
     * of our fighters in the spell's range that has less than a quarter of its
     * health left, and that can be the caster too
     *
     * @param spell the spell we have
     * @param creatureSpell the spell's data
     * @return the creature to cast on, or {@code null} if nobody needs it
     */
    private EntityId getSupportSpellTarget(CreatureSpell spell, toniarts.openkeeper.tools.convert.map.CreatureSpell creatureSpell) {
        if (creatureSpell.getFlags().contains(CreatureSpellFlag.CAST_ON_SELF)) {
            return isFighting(entityId) && isSupportSpellNeeded(entityId, creatureSpell) ? entityId : null;
        }
        if (isSupportSpellTarget(entityId, creatureSpell)) {
            return entityId;
        }

        EntityId nearestTarget = null;
        float nearestDistance = Float.MAX_VALUE;
        for (EntityId entity : entityPositionLookup.getSensedEntities(entityId)) {
            float distance = getDistanceToCreature(entity);
            if (distance < nearestDistance && distance <= spell.range && isSupportSpellTarget(entity, creatureSpell)) {
                nearestDistance = distance;
                nearestTarget = entity;
            }
        }

        return nearestTarget;
    }

    private boolean isSupportSpellTarget(EntityId entity, toniarts.openkeeper.tools.convert.map.CreatureSpell creatureSpell) {
        Health health = entityData.getComponent(entity, Health.class);
        Owner owner = entityData.getComponent(entity, Owner.class);

        return health != null && owner != null && owner.ownerId == getOwnerId() && health.health < health.maxHealth / 4
                && isFighting(entity) && !isIncapacitated(entity) && !isCaptive(entity) && isSupportSpellNeeded(entity, creatureSpell);
    }

    private boolean isFighting(EntityId entity) {
        CreatureAi creatureAi = entityData.getComponent(entity, CreatureAi.class);
        if (creatureAi == null) {
            return false;
        }
        CreatureState state = creatureAi.getCreatureState();

        return state == CreatureState.FIGHT || state == CreatureState.MELEE_ATTACK || state == CreatureState.CAST_SPELL;
    }

    private boolean isInvisible(EntityId entity) {
        return entityData.getComponent(entity, Invisible.class) != null;
    }

    /**
     * The spell's own condition on the creature it would be cast on
     *
     * @param entity the creature to cast on
     * @param creatureSpell the spell's data
     * @return true if the spell is of use to the creature
     */
    private boolean isSupportSpellNeeded(EntityId entity, toniarts.openkeeper.tools.convert.map.CreatureSpell creatureSpell) {
        Health health = entityData.getComponent(entity, Health.class);

        return switch (creatureSpell.getAlternativeShot()) {
            case HEAL ->
                health != null && health.health < health.maxHealth / 2;
            case ARMOUR ->
                isFighting(entity) && entityData.getComponent(entity, Armour.class) == null;
            case INVULNERABLE ->
                isFighting(entity) && entityData.getComponent(entity, Invulnerable.class) == null;
            case INVISIBLE ->
                isFighting(entity) && !isInvisible(entity);
            case HASTE ->
                entityData.getComponent(entity, SpeedChange.class) == null;
            default ->
                false;
        };
    }

    /**
     * How long into the cast the shot leaves. It leaves at the end of the cast
     * at the latest, so that a cast always fires. A possessed creature's shot
     * leaves at once (0x4984D0).
     *
     * @param creatureSpellId the spell cast
     * @return the time from the start of the cast
     */
    private double getShotDelay(short creatureSpellId) {
        if (entityData.getComponent(entityId, Possessed.class) != null) {
            return 0;
        }

        double castTime = getAnimationTime(creature, Creature.AnimationType.CAST_SPELL);
        for (Creature.Spell spell : creature.getSpells()) {
            if (spell.getCreatureSpellId() == creatureSpellId) {
                return Math.min(spell.getShotDelay(), castTime);
            }
        }

        return 0;
    }

    /**
     * Hits the target with our melee attack
     *
     * @param attackTarget the target we hit
     * @param creatureMeleeAttack our melee attack
     * @param damage the damage to deal
     */
    private void hitWithMelee(EntityId attackTarget, CreatureMeleeAttack creatureMeleeAttack, int damage) {
        int damageTaken = getMeleeDamageTaken(attackTarget, getCombatPitDamage(attackTarget, damage));
        if (damageTaken > 0) {
            setDamage(attackTarget, damageTaken);
        }
        gameEventService.emitAt(GameEventType.MELEE_HIT, ThingFlag.CREATURE, creature.getCreatureId(), attackTarget, getPosition());
        showMeleeHit(attackTarget, creatureMeleeAttack.getAttackType());
    }

    /**
     * Plays the hit effects of a melee hit on the target (0x4998A0). Every
     * blade, blunt, body and scythe hit makes the physical hit effect, a blade
     * or scythe hitting metal also makes a spark shower
     *
     * @param attackTarget the target we hit
     * @param attackType our melee attack type
     */
    private void showMeleeHit(EntityId attackTarget, Creature.AttackType attackType) {
        Vector3f position = getPosition(entityData, attackTarget);
        if (position == null || attackType != Creature.AttackType.MELEE_BLADE && attackType != Creature.AttackType.MELEE_BLUNT
                && attackType != Creature.AttackType.MELEE_BODY && attackType != Creature.AttackType.MELEE_SCYTHE) {
            return;
        }

        gameEventService.emitEffect(PHYSICAL_HIT_EFFECT_ID, position, 0);
        Material material = getMaterial(attackTarget);
        if ((attackType == Creature.AttackType.MELEE_BLADE || attackType == Creature.AttackType.MELEE_SCYTHE)
                && (material == Material.METAL1 || material == Material.METAL2)) {
            gameEventService.emitEffect(SPARK_SHOWER_EFFECT_ID, position, 0);
        }
    }

    private Material getMaterial(EntityId entity) {
        CreatureComponent creatureComponent = entityData.getComponent(entity, CreatureComponent.class);
        if (creatureComponent != null) {
            return levelInfo.getLevelData().getCreature(creatureComponent.creatureId).getMaterial();
        }
        DoorComponent doorComponent = entityData.getComponent(entity, DoorComponent.class);
        if (doorComponent != null) {
            return levelInfo.getLevelData().getDoorById(doorComponent.doorId).getMaterial();
        }
        ObjectComponent objectComponent = entityData.getComponent(entity, ObjectComponent.class);
        if (objectComponent != null) {
            return levelInfo.getLevelData().getObject(objectComponent.objectId).getMaterial();
        }
        return Material.NONE;
    }

    private float getFirstPersonRechargeModifyPercentage() {
        Variable.MiscVariable modify = gameSettings.get(Variable.MiscVariable.MiscType.FIRST_PERSON_RECHARGE_MODIFY_PERCENTAGE);
        return modify != null ? modify.getValue() : 100;
    }

    /**
     * Gets what a stat is in first person, as a percentage of the normal
     *
     * @param statType the stat
     * @return 100 plus the stat increase, 100 if there is none
     */
    private int getFirstPersonPercentage(Variable.CreatureStats.StatType statType) {
        Variable.CreatureFirstPerson stat = levelInfo.getLevelData().getCreatureFirstPersonStats().get(statType);
        return 100 + (stat != null ? stat.getValue() : 0);
    }

    /**
     * Gets the damage our melee hit does to the target
     *
     * @param attackTarget the target we hit
     * @param damage the damage of our melee attack
     * @return the damage the target takes
     */
    private int getMeleeDamageTaken(EntityId attackTarget, int damage) {
        CreatureComponent targetCreature = entityData.getComponent(attackTarget, CreatureComponent.class);
        if (targetCreature != null && !CombatRules.canBeHurtBy(creature.getFlags(),
                levelInfo.getLevelData().getCreature(targetCreature.creatureId).getFlags())) {
            return 0;
        }

        // Only creatures take the bonus for a hit from behind, doors and objects are hit as they are (0x498E30)
        Position targetPosition = entityData.getComponent(attackTarget, Position.class);
        Vector3f ourPosition = getPosition();
        if (targetCreature != null && targetPosition != null && ourPosition != null) {
            damage = CombatRules.addRearAttackBonus(damage, ourPosition, targetPosition.position, targetPosition.rotation);
        }

        // A possessed target takes less, EntityController.setDamage scales every hit
        return damage;
    }

    /**
     * Where the creature's shot starts. The spell's shot offset is in front of
     * the creature and above its feet (0x4984D0), the creature faces the
     * target when it fires.
     *
     * @param creatureSpellId the spell fired
     * @param targetPosition where the shot is aimed at
     * @return the start of the shot
     */
    private Vector3f getShotPosition(short creatureSpellId, Vector3f targetPosition) {
        Vector3f position = getPosition();
        Vector3f result = new Vector3f(position);
        for (Creature.Spell spell : creature.getSpells()) {
            if (spell.getCreatureSpellId() != creatureSpellId) {
                continue;
            }

            Vector3f forward = targetPosition != null ? targetPosition.subtract(position).setY(0) : Vector3f.ZERO;
            if (forward.lengthSquared() > 0) {
                result.addLocal(forward.normalizeLocal().multLocal(-spell.getShotOffset().y));
            }
            result.y += spell.getShotOffset().z;
            break;
        }

        return result;
    }

    /**
     * Gets preferred attack creature spell that is available to cast to the
     * target
     *
     * @param attackTarget target to attack
     * @return creature spell to cast on the unsuspecting victim
     */
    private Optional<CreatureSpell> getPreferredAttackSpell(EntityId attackTarget) {
        List<CreatureSpell> creatureSpells = getCreatureSpells();
        if (creatureSpells.isEmpty()) {
            return Optional.empty();
        }

        // Of the attack spells that are ready, the one that was ready first
        return creatureSpells
                .stream()
                .filter(creatureSpell -> isAttackRecharged(creatureSpell) && isInRange(creatureSpell.range, attackTarget) && levelInfo.getLevelData().getCreatureSpellById(creatureSpell.creatureSpellId).getFlags().contains(CreatureSpellFlag.IS_ATTACKING))
                .min(READY_FIRST);
    }

    /**
     * Fighters in the Combat Pit hit by the melee damage modifier percentage
     * (normal damage at 100) and take only a part of the damage
     *
     * @param attackTarget the target we hit
     * @param damage the damage of our hit
     * @return the damage the target takes
     */
    private int getCombatPitDamage(EntityId attackTarget, int damage) {
        if (isCombatPitFighter()) {
            damage = damage * (int) gameSettings.get(Variable.MiscVariable.MiscType.COMBAT_PIT_MELEE_DAMAGE_MODIFIER_PERCENTAGE).getValue() / 100;
        }
        if (entityData.getComponent(attackTarget, CombatPitFighter.class) != null) {
            damage = damage * (int) gameSettings.get(Variable.MiscVariable.MiscType.PIT_PERCENTAGE_DAMAGE_TAKEN_OF_NORMAL_COMBAT).getValue() / 100;
        }

        return damage;
    }

    @Override
    public boolean hasRangedAttackReady() {
        return getCreatureSpells().stream().anyMatch(creatureSpell -> isAttackRecharged(creatureSpell)
                && levelInfo.getLevelData().getCreatureSpellById(creatureSpell.creatureSpellId).getFlags().contains(CreatureSpellFlag.IS_ATTACKING));
    }

    @Override
    public void castTrainingShot(EntityId target) {
        CreatureSpells creatureSpells = entityData.getComponent(entityId, CreatureSpells.class);
        if (creatureSpells == null) {
            return;
        }
        for (EntityId creatureSpellEntity : creatureSpells.creatureSpells) {
            CreatureSpell spell = entityData.getComponent(creatureSpellEntity, CreatureSpell.class);
            toniarts.openkeeper.tools.convert.map.CreatureSpell creatureSpell = levelInfo.getLevelData().getCreatureSpellById(spell.creatureSpellId);
            if (isAttackRecharged(spell) && creatureSpell.getFlags().contains(CreatureSpellFlag.IS_ATTACKING)) {
                entityData.setComponent(creatureSpellEntity, new CreatureSpell(spell, gameTimer.getGameTime()));

                // The shot flies at the target and bursts on the wall behind it
                int shotData1 = getShotData1(creatureSpell);
                if (!shotsController.fireShot(creatureSpell.getShotTypeId(), shotData1, creatureSpell.getShotData2(), getOwnerId(),
                        getShotPosition(spell.creatureSpellId, getPosition(entityData, target)), target, entityId, spell.range)) {
                    shotsController.createShot(creatureSpell.getShotTypeId(), shotData1, creatureSpell.getShotData2(), getOwnerId(), getPosition(), target, entityId);
                }
                return;
            }
        }
    }

    private boolean isAttackRecharged(Attack attack) {

        // The recharge is divided by the creature's work modifier, which is 0.8 for every creature, and the attack is
        // ready only once that much time has fully passed (0x48C9B0 melee, 0x48C800 spells)
        return attack.attactStartTime == null || attack.attactStartTime + attack.rechargeTime / ATTACK_WORK_MODIFIER < gameTimer.getGameTime();
    }

    @Override
    public void navigateToAttackTarget(EntityId attackTarget) {
        if (DungeonHeartTarget.isDungeonHeart(entityData, attackTarget)) {
            navigateToHeart(attackTarget);
            return;
        }
        Vector3f targetPosition = getPosition(entityData, attackTarget);
        if (targetPosition != null) {

            // Check if we are already going there
            Point destination = WorldUtils.vectorToPoint(targetPosition);
            Navigation currentNavigation = entityData.getComponent(entityId, Navigation.class);
            boolean continuingPursuit = currentNavigation != null && currentNavigation.pursuingAttackTarget;
            if (continuingPursuit && currentNavigation.target.equals(destination)) {
                return;
            }

            // Just now simply go where the target currently is
            Point ourPosition = getCreatureCoordinates();
            if (destination != null && !destination.equals(ourPosition)) {
                boolean failed = createNavigation(ourPosition, destination, null, null, true);
                if (!failed && !continuingPursuit) {
                    gameEventService.emit(GameEventType.CREATURE_CHARGE, entityId, getPosition());
                }
            }
        }
    }

    /**
     * Turns the creature on the spot to face the target it attacks, for a heart
     * the pillar it hits, the nearest one, as the original does (0x45CB00,
     * 0x4B2AE0). Like the original, it faces from where it stands to where the
     * target stands. Facing is steered through a navigation that only has the
     * tile to face from, and it is only made when the creature isn't already
     * facing the target.
     *
     * @param attackTarget the target being attacked
     * @return true if the creature already faces the target
     */
    private boolean faceAttackTarget(EntityId attackTarget) {
        Position position = entityData.getComponent(entityId, Position.class);
        Vector3f targetPosition = getPosition(entityData, attackTarget);
        if (targetPosition == null) {
            return true;
        }
        Vector2f standing = WorldUtils.vector3fToVector2f(position.position);
        Vector2f toTarget = (DungeonHeartTarget.isDungeonHeart(entityData, attackTarget)
                ? DungeonHeartTarget.getNearestPillar(standing, getHeartTile(attackTarget))
                : WorldUtils.vector3fToVector2f(targetPosition)).subtractLocal(standing);
        if (toTarget.lengthSquared() < FastMath.ZERO_TOLERANCE) {
            return true; // Right on top of it, there is no way to turn to
        }
        double turn = Math.IEEEremainder(-position.rotation - SteeringUtils.calculateVectorToAngle(
                new Vector2(toTarget.x, toTarget.y)), 2 * Math.PI);
        if (Math.abs(turn) <= FACING_TOLERANCE) {
            return true;
        }

        // The steering faces from the centre of a tile to the centre of another, the creature is often on the
        // target's own tile. A tile far along the way to the target gives that way to within a degree or so.
        Point tile = getCreatureCoordinates();
        toTarget.normalizeLocal().multLocal(FACE_DISTANCE);
        entityData.setComponent(entityId, new Navigation(tile,
                new Point(tile.x + Math.round(toTarget.x), tile.y + Math.round(toTarget.y)), new ArrayList<>()));
        return false;
    }

    private void navigateToHeart(EntityId heart) {
        Point heartTile = getHeartTile(heart);
        Navigation currentNavigation = entityData.getComponent(entityId, Navigation.class);
        if (currentNavigation != null
                && DungeonHeartTarget.isInReach(WorldUtils.pointToVector2f(currentNavigation.target), heartTile, getMeleeRange())) {
            return;
        }

        Point attackTile = getHeartAttackTile(heart);
        if (attackTile == null) {

            // Walled off, pick something else to do
            setAttackTarget(null);
        } else {

            // Pushed out of reach on the attack tile, back to its middle
            createNavigation(getCreatureCoordinates(), attackTile, heartTile,
                    attackTile.equals(getCreatureCoordinates()) ? WorldUtils.pointToVector2f(attackTile) : null, true);
        }
    }

    @Override
    public ICreatureController getFollowTarget() {
        FollowTarget followTarget = entityData.getComponent(entityId, FollowTarget.class);
        if (followTarget != null && entityData.getComponent(followTarget.entityId, Position.class) != null) {
            return creaturesController.createController(followTarget.entityId);
        }
        return null;
    }

    @Override
    public boolean shouldNavigateToFollowTarget() {
        return (isStopped() && getDistanceToCreature(getFollowTarget().getEntityId()) > 1.5f) || (getDistanceToCreature(getFollowTarget().getEntityId()) > 2.5f);
    }

    @Override
    public boolean answerCallToArms(EntityId banner) {
        Position bannerPosition = entityData.getComponent(banner, Position.class);
        if (bannerPosition == null) {
            return false;
        }
        Point destination = WorldUtils.vectorToPoint(bannerPosition.position);
        GraphPath<IMapTileInformation> path = navigationService.findPath(getCreatureCoordinates(), destination, this);
        if (path == null) {
            return false;
        }

        stateMachine.changeState(CreatureState.CALL_TO_ARMS);
        setFollowTarget(banner);
        entityData.setComponent(entityId, new Navigation(destination, null, SteeringUtils.pathToList(path)));
        return true;
    }

    @Override
    public boolean answerHeartCall(Point enemyTile) {
        Health health = entityData.getComponent(entityId, Health.class);
        if (entityData.getComponent(entityId, CreatureAi.class) == null
                || !(stateMachine.isInState(CreatureState.IDLE) || stateMachine.isInState(CreatureState.WORK))
                || isIncapacitated()
                || creature.getFlags().contains(Creature.CreatureFlag.IS_HORNY)
                || entityData.getComponent(entityId, Peaceful.class) != null
                || health.health <= health.maxHealth / 4) {
            return false;
        }
        GraphPath<IMapTileInformation> path = navigationService.findPath(getCreatureCoordinates(), enemyTile, this);
        if (path == null) {
            return false;
        }

        stateMachine.changeState(CreatureState.HEART_CALL);
        entityData.setComponent(entityId, new Navigation(enemyTile, null, SteeringUtils.pathToList(path)));
        return true;
    }

    @Override
    public boolean heedCallToArms() {
        FollowTarget followTarget = entityData.getComponent(entityId, FollowTarget.class);
        if (followTarget == null || entityData.getComponent(followTarget.entityId, CallToArms.class) == null) {
            return false;
        }

        // Wait while the keeper carries the banner
        Position bannerPosition = entityData.getComponent(followTarget.entityId, Position.class);
        if (bannerPosition == null || shouldFleeOrAttack()) {
            return true;
        }
        if (!isStopped()) {
            return true;
        }

        // Go to the banner, or mill about it when there
        Point banner = WorldUtils.vectorToPoint(bannerPosition.position);
        Point ourPosition = getCreatureCoordinates();
        if (Math.max(Math.abs(ourPosition.x - banner.x), Math.abs(ourPosition.y - banner.y)) > 2) {
            if (createNavigation(ourPosition, banner, null)) {
                gameEventService.emitToPlayer(GameEventType.CALL_TO_ARMS_UNREACHABLE, getOwnerId());
                return false;
            }
        } else {
            navigateToRandomPointAroundTarget(followTarget.entityId, 2);
        }
        return true;
    }

    /**
     * While its keeper has a Call to Arms banner up, a creature only starts a
     * fight near the banner
     *
     * @return true if we may start a fight here
     */
    private boolean isNearCallToArms() {
        Vector3f ourPosition = getPosition();
        if (ourPosition == null) {
            return true;
        }
        // Not filtered by the owner field: that would go through the owner of every entity, map tiles included
        for (EntityId banner : entityData.findEntities(null, CallToArms.class)) {
            CallToArms callToArms = entityData.getComponent(banner, CallToArms.class);
            Position bannerPosition = entityData.getComponent(banner, Position.class);
            Owner owner = entityData.getComponent(banner, Owner.class);
            if (callToArms != null && callToArms.isUp() && bannerPosition != null && owner != null && owner.ownerId == getOwnerId()
                    && bannerPosition.position.distance(ourPosition) > gameSettings.get(Variable.MiscVariable.MiscType.CALL_TO_ARMS_FIGHT_DISTANCE_TILES).getValue() * WorldUtils.TILE_WIDTH) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Task getAssignedTask() {
        TaskComponent taskComponent = entityData.getComponent(entityId, TaskComponent.class);
        if (taskComponent != null) {
            return taskManager.getTaskById(taskComponent.taskId);
        }
        return null;
    }

    @Override
    public float getDistanceToCreature(EntityId target) {

        // FIXME: now just direct distance, should be perhaps real distance that the creature needs to traverse to reach the target
        Position targetPosition = entityData.getComponent(target, Position.class);
        Vector3f ourPosition = getPosition();
        if (targetPosition == null || ourPosition == null) {
            return Float.MAX_VALUE;
        }
        return ourPosition.distance(targetPosition.position);
    }

    @Override
    public void navigateToRandomPointAroundTarget(EntityId target, int radius) {
        Position targetPosition = entityData.getComponent(target, Position.class);
        if (targetPosition != null) {

            // To keep up with the target, see if it has a target it is navigating to
            Point destination;
            Navigation targetNavigation = entityData.getComponent(target, Navigation.class);
            if (targetNavigation != null) {
                destination = targetNavigation.target;
            } else {
                destination = WorldUtils.vectorToPoint(targetPosition.position);
            }

            Point p = navigationService.findRandomAccessibleTile(destination, radius, this);
            Point ourPosition = getCreatureCoordinates();
            if (p != null && p != ourPosition) {
                createNavigation(ourPosition, p, null);
            }
        }
    }

    @Override
    public void setFollowTarget(EntityId target) {
        entityData.setComponent(entityId, new FollowTarget(target));
    }

    @Override
    public void resetFollowTarget() {
        entityData.removeComponent(entityId, FollowTarget.class);
    }

    @Override
    public boolean flee() {

        // A creature with a lair withdraws to it and nowhere else, failing when it can't get there (0x4611D0 goes to
        // the object in creature +0x3B7)
        Point lair = getLairLocation();
        if (lair != null) {
            if (navigationService.findPath(getCreatureCoordinates(), lair, this) == null) {
                return false;
            }
            changeToFlee();
            createNavigation(getCreatureCoordinates(), lair, null);
            rememberWhereWeWithdrewFrom();
            return true;
        }
        Keeper keeper = levelInfo.getPlayer(getOwnerId());

        // A frightened creature, or one without a heart, runs away from its threat
        Point threat = keeper == null || keeper.getDungeonHeartLocation() == null
                || entityData.getComponent(entityId, Frightened.class) != null ? findThreatLocation() : null;
        if (threat != null) {
            changeToFlee();
            fleeFrom(threat);
            return true;
        }

        // A keeper's creature withdraws to a random point 2 tiles either way of its heart room's position (0x4611D0).
        // In DK II that position is a tile down and right of the heart's centre (room +0x27), so on level 5 the Imps
        // end up on the corners of the centre 3x3. Finding nowhere, it doesn't withdraw at all
        if (keeper != null && keeper.getDungeonHeartLocation() != null) {
            Point heart = keeper.getDungeonHeartLocation();
            Point destination = new Point(heart.x + 1 + (FastMath.rand.nextBoolean() ? 2 : -2), heart.y + 1 + (FastMath.rand.nextBoolean() ? 2 : -2));
            if (navigationService.findPath(getCreatureCoordinates(), destination, this) == null) {
                destination = findWithdrawFallback();
            }
            if (destination == null) {
                return false;
            }
            changeToFlee();

            // The walk may end inside the heart's obstacle (0x4611D0 asks for it with 0x20 / 0x40)
            createNavigation(getCreatureCoordinates(), destination, null, null, false, true);
        } else {
            changeToFlee();
            navigateToRandomPoint();
        }
        rememberWhereWeWithdrewFrom();
        return true;
    }

    /**
     * Remembers the tile we withdraw from and when, to go back there once we
     * are well again. DK II does it for every creature but the Imp (0x4611D0).
     * DK II stores the tile as (x, x), a bug; we keep the intended (x, y)
     */
    private void rememberWhereWeWithdrewFrom() {
        if (creature.getId() != levelInfo.getLevelData().getImp().getId()) {
            entityData.setComponent(entityId, new WithdrawnFrom(getCreatureCoordinates(), gameTimer.getGameTime()));
        }
    }

    @Override
    public boolean goBackToWhereWeFought() {
        WithdrawnFrom withdrawnFrom = entityData.getComponent(entityId, WithdrawnFrom.class);
        if (withdrawnFrom == null) {
            return false;
        }

        // Within 500 turns of the withdraw, once we have over three quarters of our health (DK II 0x4773D0)
        if (gameTimer.getGameTime() >= withdrawnFrom.time + RETURN_TO_FIGHT_TICKS / (double) levelInfo.getLevelData().getGameLevel().getTicksPerSec()) {
            entityData.removeComponent(entityId, WithdrawnFrom.class);
            return false;
        }
        Health health = entityData.getComponent(entityId, Health.class);
        if (health == null || health.health <= health.maxHealth * 3 / 4
                || createNavigation(getCreatureCoordinates(), withdrawnFrom.tile, null)) {
            return false;
        }
        entityData.removeComponent(entityId, WithdrawnFrom.class);
        return true;
    }

    private void changeToFlee() {
        if (!stateMachine.isInState(CreatureState.FLEE)) {
            stateMachine.changeState(CreatureState.FLEE);
        }
    }

    /**
     * Where a creature withdraws to when it can't get to its heart, as DK II's
     * 0x4611D0 does: the position of one of its keeper's rooms, bridges left
     * out, picked at random up to 3 times (0x4C63D0). Failing that, a nearby
     * tile away from the creature it fights (5 tries, then 2 next to it), or
     * one next to it (5 tries) when it fights nobody.
     *
     * @return the destination, null if there is none
     */
    private Point findWithdrawFallback() {
        Point ourLocation = getCreatureCoordinates();
        List<IRoomController> rooms = mapController.getRoomControllers().stream()
                .filter(room -> room.getOwnerId() == getOwnerId() && !(room instanceof BridgeController))
                .toList();
        for (int i = 0; i < 3 && !rooms.isEmpty(); i++) {
            IRoomController room = rooms.get(FastMath.rand.nextInt(rooms.size()));
            Point position = room.getRoomInstance().getCenter();
            if (room.isDungeonHeart()) {
                position = new Point(position.x + 1, position.y + 1);
            }
            if (navigationService.findPath(ourLocation, position, this) != null) {
                return position;
            }
        }

        FleeDestinationFinder finder = createFleeDestinationFinder(ourLocation);
        AttackTarget target = entityData.getComponent(entityId, AttackTarget.class);
        Position targetPosition = target != null ? entityData.getComponent(target.entityId, Position.class) : null;
        Optional<Point> destination = targetPosition != null
                ? finder.findRunningFrom(ourLocation, WorldUtils.vectorToPoint(targetPosition.position), 5)
                        .or(() -> finder.findAround(ourLocation, 1, 2))
                : finder.findAround(ourLocation, 1, 5);
        return destination.orElse(null);
    }

    /**
     * Finds what we are running from. When we are frightened it is the
     * creature that frightened us, otherwise the nearest enemy we sense that
     * threatens us. A trap is one after it has fired, see {@link Threat}.
     *
     * @return the tile of the threat, null if there is none
     */
    private Point findThreatLocation() {
        Frightened frightened = entityData.getComponent(entityId, Frightened.class);
        if (frightened != null && frightened.endTime > gameTimer.getGameTime()) {
            IMapTileInformation tile = entityPositionLookup.getEntityLocation(frightened.entity);
            if (tile != null) {
                return tile.getLocation();
            }
        }

        Point ourLocation = getCreatureCoordinates();
        Point nearest = null;
        for (EntityId entity : entityPositionLookup.getSensedEntities(entityId)) {
            IMapTileInformation tile = isEnemy(entity) && isThreat(entity) && getThreat(entity) > 0
                    && ThreatSource.isThreateningUs(entityData, entity, entityId)
                    ? entityPositionLookup.getEntityLocation(entity) : null;
            if (tile != null && (nearest == null || tile.getLocation().distance(ourLocation) < nearest.distance(ourLocation))) {
                nearest = tile.getLocation();
            }
        }
        return nearest;
    }

    /**
     * Runs away from the threat the way DKII.EXE does, see
     * {@link FleeDestinationFinder}
     */
    private void fleeFrom(Point threat) {
        Point ourLocation = getCreatureCoordinates();
        FleeDestinationFinder finder = createFleeDestinationFinder(ourLocation);
        finder.findAwayFrom(ourLocation, threat, FLEE_DISTANCE)
                .or(() -> finder.findAround(ourLocation))
                .ifPresent(destination -> createNavigation(ourLocation, destination, null));
    }

    private FleeDestinationFinder createFleeDestinationFinder(Point ourLocation) {
        return new FleeDestinationFinder(mapController.getMapData().getWidth(),
                mapController.getMapData().getHeight(),
                (x, y) -> navigationService.findPath(ourLocation, new Point(x, y), this) != null,
                (x, y) -> mapController.getMapData().getTile(x, y).getOwnerId() == getOwnerId() && !mapController.isSolid(new Point(x, y)),
                ThreadLocalRandom.current());
    }

    @Override
    public boolean isAttacked() {
        // TODO:
        return false;
    }

    @Override
    public boolean isEnoughSleep() {
        CreatureSleep creatureSleep = entityData.getComponent(entityId, CreatureSleep.class);
        return creatureSleep.sleepNeed == 0 && isFullHealth();
    }

    @Override
    public boolean isCombatPitFighter() {
        return entityData.getComponent(entityId, CombatPitFighter.class) != null;
    }

    @Override
    public boolean isIncapacitated() {
        return isIncapacitated(entityData, entityId);
    }

    private boolean isIncapacitated(EntityId entityId) {
        return isIncapacitated(entityData, entityId);
    }

    private static boolean isIncapacitated(EntityData entityData, EntityId entityId) {
        Health health = entityData.getComponent(entityId, Health.class);
        if (health == null || entityData.getComponent(entityId, Unconscious.class) != null) {
            return true;
        }

        CreatureRecuperating recuperating = entityData.getComponent(entityId, CreatureRecuperating.class);
        if (recuperating != null) {
            return true;
        }

        CreatureFall creatureFall = entityData.getComponent(entityId, CreatureFall.class);
        if (creatureFall != null) {
            return true;
        }

        InHand inHand = entityData.getComponent(entityId, InHand.class);
        if (inHand != null) {
            return true;
        }

        return false;
    }

    private void initState() {
        stateMachine.changeState(entityData.getComponent(entityId, CreatureAi.class).getCreatureState());
    }

    @Override
    public boolean canFly() {
        Mobile mobile = entityData.getComponent(entityId, Mobile.class);
        return mobile.canFly;
    }

    @Override
    public boolean canWalkOnWater() {
        Mobile mobile = entityData.getComponent(entityId, Mobile.class);
        if (!mobile.canWalkOnWater) {

            // We need anyway to get out from water if we are tossed in such
            Vector3f pos = getPosition();
            if (pos != null) {
                return mapController.isWater(WorldUtils.vectorToPoint(pos));
            }
        }
        return mobile.canWalkOnWater;
    }

    @Override
    public boolean canWalkOnLava() {
        Mobile mobile = entityData.getComponent(entityId, Mobile.class);
        if (!mobile.canWalkOnLava) {

            // We need anyway to get out from lava if we are tossed in such
            Vector3f pos = getPosition();
            if (pos != null) {
                return mapController.isLava(WorldUtils.vectorToPoint(pos));
            }
        }
        return mobile.canWalkOnLava;
    }

    @Override
    public boolean canMoveDiagonally() {
        return true;
    }

    @Override
    public void start() {

    }

    @Override
    public void processTick(float tpf) {
        Armour armour = entityData.getComponent(entityId, Armour.class);
        if (armour != null && armour.endTime <= gameTimer.getGameTime()) {
            entityData.removeComponent(entityId, Armour.class);
        }
        Invulnerable invulnerable = entityData.getComponent(entityId, Invulnerable.class);
        if (invulnerable != null && invulnerable.endTime <= gameTimer.getGameTime()) {
            entityData.removeComponent(entityId, Invulnerable.class);
        }
        Invisible invisible = entityData.getComponent(entityId, Invisible.class);
        if (invisible != null && invisible.endTime <= gameTimer.getGameTime()) {
            entityData.removeComponent(entityId, Invisible.class);
        }
        SpeedBlur speedBlur = entityData.getComponent(entityId, SpeedBlur.class);
        if (speedBlur != null && speedBlur.endTime <= gameTimer.getGameTime()) {
            entityData.removeComponent(entityId, SpeedBlur.class);
        }

        // A manual drop can already carry its target in AI, but no state may
        // enter or advance until the fall system has finished landing.
        if (entityData.getComponent(entityId, CreatureFall.class) != null) {
            return;
        }

        // A creature picked up into the keeper's hand has no position, and no AI while it's there. The pick up
        // runs on the render thread, so the creature can still be in this tick's list (#670)
        if (entityData.getComponent(entityId, Position.class) == null) {
            return;
        }

        // A teleporting Imp does nothing else until it has landed (states 51 and 52)
        Teleport teleport = entityData.getComponent(entityId, Teleport.class);
        if (teleport != null) {
            continueTeleport(teleport);
            return;
        }

        /**
         * Hmm, I'm not sure how to do this, this is not ideal either, how to
         * control the state machine outside the controller. Should it be
         * allowed and should we just check that the current state matches the
         * state in the entity component
         */
        //CreatureAi creatureAi = entityData.getComponent(entityId, CreatureAi.class);
        if (stateMachine.getCurrentState() == null) {
            initState();
        }

        /**
         * The creatures have these time motionless stuff in different states,
         * they seem to equal to kind of re-evaluate what to do. We should
         * figure out a proper way to utilize these. We could also use the
         * delayed telegram stuff. The hard part is just to kind of figure out
         * the motionless part and not re-send messages always etc. We could
         * probably go with pretty much event driven AI.
         */
        if (isStopped()) {
            motionless += tpf;
        } else {
            motionless = 0;
        }

        destroyRoomOnTheWay();

        // Task timer
        TaskComponent taskComponent = entityData.getComponent(entityId, TaskComponent.class);
        if (taskComponent != null && taskComponent.taskStarted) {
            entityData.setComponent(entityId, new TaskComponent(taskComponent.taskId, taskComponent.targetEntity, taskComponent.targetLocation, taskComponent.taskType, taskComponent.taskDuration + tpf, taskComponent.taskStarted));
        }

        // A slap interrupts the work, once per slap
        Slapped slapped = entityData.getComponent(entityId, Slapped.class);
        if (slapped != null && !slapped.reacted) {
            entityData.setComponent(entityId, new Slapped(slapped.startTime, true));
            interruptWork();
        }

        // Hypnotised on the render thread, we start following here. Outside FOLLOW only a creature that was
        // just hypnotised has its hypnotiser as the follow target, FOLLOW drops it on the way out
        FollowTarget followTarget = entityData.getComponent(entityId, FollowTarget.class);
        if (followTarget != null && followTarget.entityId.equals(getHypnotiser())
                && stateMachine.getCurrentState() != CreatureState.FOLLOW) {
            unassingCurrentTask();
            stateMachine.changeState(CreatureState.FOLLOW);
        }

        stateMachine.update();

        // Also change our state component
        CreatureAi creatureAi = entityData.getComponent(entityId, CreatureAi.class);
        if (creatureAi == null || stateMachine.getCurrentState() != creatureAi.getCreatureState()) {
            entityData.setComponent(entityId, new CreatureAi(gameTimer.getGameTime(), stateMachine.getCurrentState(), creature.getId(),
                    creatureAi == null ? null : creatureAi.getTargetState()));
        }
    }

    /**
     * A hero with the DESTROY_ROOMS flag wrecks the keeper's room tiles he
     * steps on, on his way to wherever he is going (DKII.EXE 0x488930)
     */
    private void destroyRoomOnTheWay() {
        CreatureComponent creatureComponent = entityData.getComponent(entityId, CreatureComponent.class);
        Position position = entityData.getComponent(entityId, Position.class);
        Point tile = roomDestructionTrigger.onTheWay(position != null ? position.position : null, stateMachine.getCurrentState(),
                creatureComponent != null && creatureComponent.destroysRooms, getOwnerId());
        if (tile == null || entityData.getComponent(entityId, Possessed.class) != null
                || !taskManager.assignDestroyRoomTask(this, tile)) {
            return;
        }

        // Stop on the tile and swing at it
        if (stateMachine.isInState(CreatureState.WORK)) {
            stopCreature();
            navigateToAssignedTask();
        } else {
            stateMachine.changeState(CreatureState.WORK);
        }
    }

    @Override
    public boolean isTimeToReEvaluate() {

        // See that we have been motionless for enough time, per state
        // TODO: now just 5 seconds, it is the default for imps
        // DK II's pit fighter waits four seconds' worth of ticks on each spot (0x457CF0)
        return motionless >= (stateMachine.getCurrentState() == CreatureState.COMBAT_PIT ? 4f : 5f);
    }

    @Override
    public void resetReEvaluationTimer() {
        motionless = 0;
    }

    @Override
    public boolean playIdleAnimation() {

        // DK II only lets an idle animation follow an idle walk (the flag at CState +0xA7), so the creature walks the
        // first time and after each animation. A creature without the animation walks too. A worker makes the same
        // roll inside its own idle state (0x464240), and a neutral creature in its own (0x4776B0)
        IdleAnimation last = entityData.getComponent(entityId, IdleAnimation.class);
        int roll = FastMath.rand.nextInt(100);
        if (last != null && last.animation == null && roll >= 25) {
            Creature.AnimationType animation = roll < 75 ? Creature.AnimationType.IDLE_1 : Creature.AnimationType.IDLE_2;
            double time = getAnimationTime(creature, animation);
            if (time > 0) {
                entityData.setComponent(entityId, new IdleAnimation(animation, gameTimer.getGameTime() + time, last.lookForWorkTime));
                return true;
            }
        }
        if (last == null || last.animation != null) {
            entityData.setComponent(entityId, new IdleAnimation(null, 0, last != null ? last.lookForWorkTime : 0));
        }
        return false;
    }

    @Override
    public boolean isWaitingToLookForWork() {
        IdleAnimation idle = entityData.getComponent(entityId, IdleAnimation.class);
        return isWorker() && idle != null && idle.animation != null && gameTimer.getGameTime() < idle.lookForWorkTime;
    }

    @Override
    public boolean isPlayingIdleAnimation() {
        IdleAnimation idle = entityData.getComponent(entityId, IdleAnimation.class);
        return idle != null && idle.animation != null && gameTimer.getGameTime() < idle.endTime;
    }

    @Override
    public void addGold(int amount) {
        Gold gold = entityData.getComponent(entityId, Gold.class);

        // Drop excess gold, we can only carry so much
        int maxGoldCanAdd = gold.maxGold - gold.gold;
        int goldToAdd = Math.min(amount, maxGoldCanAdd);
        int looseGold = amount - goldToAdd;

        if (goldToAdd > 0) {
            entityData.setComponent(entityId, new Gold(gold.gold + goldToAdd, gold.maxGold));
        }

        if (looseGold > 0) {
            Point coordinates = getCreatureCoordinates();

            // Like in DKII (0x4510C0), the gold goes to the pile at our feet if there is one
            EntityId goldPile = GameWorldController.findLooseGoldPile(entityData, entityPositionLookup, coordinates);
            if (goldPile != null) {
                Gold pile = entityData.getComponent(goldPile, Gold.class);
                entityData.setComponent(goldPile, new Gold(pile.gold + looseGold, pile.maxGold));
                return;
            }
            addLooseGold(looseGold);
        }
    }

    private void addLooseGold(int amount) {
        Point coordinates = getCreatureCoordinates();
        objectsController.addLooseGold(coordinates.x, coordinates.y, amount, (int) gameSettings.get(Variable.MiscVariable.MiscType.MAX_GOLD_PILE_OUTSIDE_TREASURY).getValue());
    }

    @Override
    public void takePay(int amount) {
        Gold gold = entityData.getComponent(entityId, Gold.class);
        entityData.setComponent(entityId, new Gold(Math.min(gold.gold + amount / 2, gold.maxGold), gold.maxGold));

        // Whatever the amount, the creature is owed nothing more and is no longer angry about its pay
        CreaturePay pay = entityData.getComponent(entityId, CreaturePay.class);
        if (pay != null && pay.unpaid > 0) {
            entityData.removeComponent(entityId, CreaturePay.class);
        }
        CreatureMood mood = entityData.getComponent(entityId, CreatureMood.class);
        if (mood != null) {
            entityData.setComponent(entityId, mood.clear(CreatureMood.REASON_NO_PAY));
        }
    }

    @Override
    public int getGold() {
        return entityData.getComponent(entityId, Gold.class).gold;
    }

    @Override
    public int getMaxGold() {
        return entityData.getComponent(entityId, Gold.class).maxGold;
    }

    @Override
    public void substractGold(int amount) {
        Gold gold = entityData.getComponent(entityId, Gold.class);
        entityData.setComponent(entityId, new Gold(gold.gold - amount, gold.maxGold));
    }

    @Override
    public void removeFromMap() {
        entityData.removeComponent(entityId, Gold.class); // Carried gold vanishes with the creature
        remove();
    }

    @Override
    public Point getLairLocation() {
        CreatureSleep creatureSleep = entityData.getComponent(entityId, CreatureSleep.class);
        if (creatureSleep != null && creatureSleep.lairObjectId != null) {
            Position position = entityData.getComponent(creatureSleep.lairObjectId, Position.class);
            if (position != null) {
                return WorldUtils.vectorToPoint(position.position);
            }
        }
        return null;
    }

    @Override
    public boolean isUnconscious() {
        return entityData.getComponent(entityId, Unconscious.class) != null;
    }

    @Override
    public void wakeUp() {
        Unconscious unconscious = entityData.getComponent(entityId, Unconscious.class);
        Health health = entityData.getComponent(entityId, Health.class);
        if (unconscious == null || health == null) {
            return;
        }

        // In DK II the health of a knocked out creature is its dying time in ticks, one is lost every tick
        // (DKII.EXE 0x4B2CE0, 0x4599D0). It wakes up with what is left (0x468420)
        double timeLeft = gameSettings.get(Variable.MiscVariable.MiscType.CREATURE_DYING_STATE_DURATION_SECONDS).getValue()
                - (gameTimer.getGameTime() - unconscious.startTime);
        int healthLeft = (int) (timeLeft * levelInfo.getLevelData().getGameLevel().getTicksPerSec());
        entityData.setComponent(entityId, new Health(Math.clamp(healthLeft, 1, health.maxHealth), health.maxHealth));
        entityData.removeComponent(entityId, Unconscious.class);
        stateMachine.changeState(CreatureState.IDLE);
    }

    @Override
    public Point getCreatureCoordinates() {
        return getCreatureCoordinates(entityData, entityId);
    }

    private static Point getCreatureCoordinates(EntityData entityData, EntityId entity) {
        return WorldUtils.vectorToPoint(getPosition(entityData, entity));
    }

    @Override
    public void setAssignedTask(Task task) {

        // Unassign previous task
        unassingCurrentTask();

        //workNavigationRequired = true;
        entityData.setComponent(entityId, new TaskComponent(task.getId(), task.getTaskTarget(), task.getTaskLocation(), task.getTaskType(), 0.0f, false));
    }

    @Override
    public Creature getCreature() {
        return creature;
    }

    @Override
    public void fireEvent(GameEventType type, Point location) {
        gameEventService.emit(type, ThingFlag.CREATURE, creature.getCreatureId(), location);
    }

    @Override
    public void stopCreature() {
        entityData.removeComponent(entityId, Navigation.class);
    }

    @Override
    public int getObjectiveTargetActionPointId() {
        Objective creatureObjective = entityData.getComponent(entityId, Objective.class);
        if (creatureObjective != null) {
            return creatureObjective.actionPointId;
        }
        return 0;
    }

    @Override
    public void setObjectiveTargetActionPointId(int actionPointId) {
        Objective creatureObjective = entityData.getComponent(entityId, Objective.class);
        entityData.setComponent(entityId, new Objective((creatureObjective != null ? creatureObjective.objective : null), (creatureObjective != null ? creatureObjective.objectiveTargetPlayerId : 0), actionPointId));
    }

    @Override
    public Thing.HeroParty.Objective getObjective() {
        Objective creatureObjective = entityData.getComponent(entityId, Objective.class);
        if (creatureObjective != null) {
            return creatureObjective.objective;
        }
        return null;
    }

    @Override
    public void setObjective(Thing.HeroParty.Objective objective) {
        Objective creatureObjective = entityData.getComponent(entityId, Objective.class);
        entityData.setComponent(entityId, new Objective(objective, (creatureObjective != null ? creatureObjective.objectiveTargetPlayerId : 0), (creatureObjective != null ? creatureObjective.actionPointId : 0)));
    }

    @Override
    public boolean isDead() {
        Health health = entityData.getComponent(entityId, Health.class);
        return health == null;
    }

    @Override
    public boolean isAfraid() {
        return stateMachine.isInState(CreatureState.FLEE);
    }

    @Override
    public void setRunning(boolean running) {
        CreatureComponent creatureComponent = entityData.getComponent(entityId, CreatureComponent.class);
        Mobile mobile = entityData.getComponent(entityId, Mobile.class);
        if (mobile != null) {
            entityData.setComponent(entityId, new Mobile(mobile.canFly, mobile.canWalkOnWater, mobile.canWalkOnLava,
                    running ? creatureComponent.runSpeed : creatureComponent.speed));
        }
    }

    @Override
    public boolean isImprisoned() {
        return isImprisoned(entityData, entityId);
    }

    @Override
    public boolean isTortured() {
        return isTortured(entityData, entityId);
    }

    private static boolean isImprisoned(EntityData entityData, EntityId entity) {
        CreatureImprisoned imprisoned = entityData.getComponent(entity, CreatureImprisoned.class);
        return imprisoned != null;
    }

    private static boolean isTortured(EntityData entityData, EntityId entity) {
        CreatureTortured tortured = entityData.getComponent(entity, CreatureTortured.class);
        return tortured != null;
    }

    @Override
    public boolean isStunned() {
        return stateMachine.isInState(CreatureState.STUNNED);
    }

    @Override
    public int getLevel() {
        CreatureExperience creatureExperience = entityData.getComponent(entityId, CreatureExperience.class);
        return creatureExperience.level;
    }

    @Override
    public boolean isSlapped() {
        Slapped creatureSlapped = entityData.getComponent(entityId, Slapped.class);
        return creatureSlapped != null;
    }

    /**
     * A slapped worker drops whatever it is working on and looks for work
     * anew
     */
    private void interruptWork() {
        if (!isWorker() || stateMachine.getCurrentState() != CreatureState.WORK) {
            return;
        }

        stateMachine.changeState(CreatureState.IDLE);
    }

    @Override
    public boolean isPortalGemInPosession() {
        PortalGem portalGem = entityData.getComponent(entityId, PortalGem.class);
        return portalGem != null;
    }
    @Override
    public void markStolen() {
        entityData.setComponent(entityId, new StolenLoot());
    }

    @Override
    public boolean hasStolen() {
        return entityData.getComponent(entityId, StolenLoot.class) != null;
    }

    @Override
    public void attachPortalGem() {
        entityData.setComponent(entityId, new PortalGem());
    }

    @Override
    public void setObjectiveTargetPlayerId(short playerId) {
        Objective creatureObjective = entityData.getComponent(entityId, Objective.class);
        entityData.setComponent(entityId, new Objective((creatureObjective != null ? creatureObjective.objective : null), playerId, (creatureObjective != null ? creatureObjective.actionPointId : 0)));
    }

    @Override
    public short getObjectiveTargetPlayerId() {
        Objective creatureObjective = entityData.getComponent(entityId, Objective.class);
        if (creatureObjective != null) {
            return creatureObjective.objectiveTargetPlayerId;
        }

        return -1;
    }

    @Override
    public void setPlayerObjective(ObjectiveType objective) {
        if (objective == null) {
            entityData.removeComponent(entityId, PlayerObjective.class);
        } else {
            entityData.setComponent(entityId, new PlayerObjective(objective));
        }
    }

    @Override
    public void setCreatureLair(EntityId lairId) {
        CreatureSleep creatureSleep = entityData.getComponent(entityId, CreatureSleep.class);
        CreatureSleep updated = new CreatureSleep(creatureSleep);
        updated.lairObjectId = lairId;
        entityData.setComponent(entityId, updated);
    }

    @Override
    public void sleep() {
        double gameTime = gameTimer.getGameTime();
        // Native lair recovery uses global second boundaries, not a full second after entry.
        double recoveryTime = Math.floor(gameTime);
        entityData.setComponent(entityId, new CreatureRecuperating(gameTime, recoveryTime, recoveryTime));
        CreatureSleep creatureSleep = entityData.getComponent(entityId, CreatureSleep.class);
        CreatureSleep updated = new CreatureSleep(creatureSleep);
        updated.lastSleepTime = gameTime;
        updated.sleepStartTime = gameTime;
        entityData.setComponent(entityId, updated);
        stateMachine.changeState(CreatureState.SLEEPING);
    }

    @Override
    public void knockDown() {

        // Without the AI component the creature is in hand, falling or possessed, nothing for us to knock down
        if (entityData.getComponent(entityId, CreatureAi.class) == null || isIncapacitated()) {
            return;
        }

        unassingCurrentTask();
        stopCreature();
        stateMachine.changeState(CreatureState.FALLEN);

        // Written here and not at the end of the tick, so that the state time restarts also when the creature
        // was down already. That keeps a creature that is struck over and over down
        entityData.setComponent(entityId, new CreatureAi(gameTimer.getGameTime(), CreatureState.FALLEN, creature.getId(),
                entityData.getComponent(entityId, CreatureAi.class).getTargetState()));
    }

    private boolean isAlly(EntityId entity) {
        Owner otherOwner = entityData.getComponent(entity, Owner.class);
        if (otherOwner != null) {
            Keeper keeper = levelInfo.getPlayer(getSideId(entityId, getOwnerId()));
            if (keeper != null) {
                return keeper.isAlly(getSideId(entity, otherOwner.ownerId));
            }
        }

        return false;
    }

    /**
     * Gets the player whose side the creature fights on. It is the owner,
     * unless the creature is under the Turncoat spell
     *
     * @param entity the creature
     * @param ownerId the creature's owner
     * @return the player the creature fights for
     */
    private short getSideId(EntityId entity, short ownerId) {
        Turncoat turncoat = entityData.getComponent(entity, Turncoat.class);
        if (turncoat != null) {
            return turncoat.playerId;
        }

        return ownerId;
    }

    private Creature.FightStyle getEntityFightingStyle(EntityId entity) {
        CreatureComponent otherCreature = entityData.getComponent(entity, CreatureComponent.class);
        if (otherCreature != null) {
            return levelInfo.getLevelData().getCreature(otherCreature.creatureId).getFightStyle();
        }

        return null;
    }

    private boolean isEnemy(EntityId entity) {
        Owner otherOwner = entityData.getComponent(entity, Owner.class);
        if (otherOwner != null) {

            // We take a creature disguised as one of ours for one of ours (0x45B140)
            short sideId = getSideId(entityId, getOwnerId());
            Disguised disguised = entityData.getComponent(entity, Disguised.class);
            if (disguised != null && disguised.ownerId == sideId) {
                return false;
            }
            Keeper keeper = levelInfo.getPlayer(sideId);
            if (keeper != null) {
                return keeper.isEnemy(getSideId(entity, otherOwner.ownerId));
            }
        }

        return false;
    }

    @Override
    public boolean isEnemyAtOurGuardRoom() {
        short ownerId = getOwnerId();
        AttackTarget attackTarget = entityData.getComponent(entityId, AttackTarget.class);
        if (ownerId == Player.NEUTRAL_PLAYER_ID || ownerId == Player.GOOD_PLAYER_ID || attackTarget == null
                || entityData.getComponent(attackTarget.entityId, CreatureComponent.class) == null) {
            return false;
        }
        Vector3f targetPosition = getPosition(entityData, attackTarget.entityId);
        if (targetPosition == null) {
            return false;
        }

        Point target = WorldUtils.vectorToPoint(targetPosition);
        int distance = (int) gameSettings.get(Variable.MiscVariable.MiscType.GUARD_ROOM_REPORTING_DISTANCE_TILES).getValue();
        for (IRoomController guardRoom : mapController.getRoomsByFunction(AbstractRoomController.ObjectType.GUARD, ownerId)) {
            int minX = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int minY = Integer.MAX_VALUE;
            int maxY = Integer.MIN_VALUE;
            for (Point tile : guardRoom.getRoomInstance().getCoordinates()) {
                minX = Math.min(minX, tile.x);
                maxX = Math.max(maxX, tile.x);
                minY = Math.min(minY, tile.y);
                maxY = Math.max(maxY, tile.y);
            }
            if (minX - distance < target.x && target.x < maxX + distance && minY - distance < target.y && target.y < maxY + distance) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isCornered() {
        Point location = getCreatureCoordinates();
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                Point tile = new Point(location.x + x, location.y + y);
                if ((x != 0 || y != 0) && navigationService.findPath(location, tile, this) != null && !isHeldAgainstUs(tile)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Whether an enemy that fights us stands on the tile
     */
    private boolean isHeldAgainstUs(Point tile) {
        for (EntityId entity : entityPositionLookup.getEntitiesInLocation(tile)) {
            AttackTarget target = entityData.getComponent(entity, AttackTarget.class);
            if (target != null && entityId.equals(target.entityId) && isEnemy(entity)) {
                return true;
            }
        }

        return false;
    }

    private int getThreat(EntityId entity) {
        Threat threat = entityData.getComponent(entity, Threat.class);
        if (threat != null) {
            return threat.threat;
        }

        return 0;
    }

    @Override
    public boolean isScared() {

        // It takes three fight turns in a row with the threat over our fear (DK II 0x45EC80), a non-fighter is
        // scared at once when an enemy fighter turns on it (0x45EC10)
        BackOff backOff = entityData.getComponent(entityId, BackOff.class);
        int overThreatChecks = 0;
        if (entityData.getComponent(entityId, Fearless.class) == null && !isCombatPitFighter()) {
            if (creature.getFightStyle() == Creature.FightStyle.NON_FIGHTER && isTargetedByEnemyFighter()) {
                overThreatChecks = SCARED_OVER_THREAT_CHECKS;
            } else if (getFightThreat() > entityData.getComponent(entityId, CreatureComponent.class).fear) {
                overThreatChecks = Math.min(backOff != null ? backOff.overThreatChecks + 1 : 1, SCARED_OVER_THREAT_CHECKS);
            }
        }
        if (backOff == null || backOff.overThreatChecks != overThreatChecks) {
            entityData.setComponent(entityId, new BackOff(overThreatChecks, backOff != null ? backOff.endTime : 0));
        }

        return overThreatChecks >= SCARED_OVER_THREAT_CHECKS;
    }

    /**
     * Gets the threat of the enemies we fight. An enemy shares its threat
     * evenly between its target and the creatures attacking it. Near our own
     * Dungeon Heart we take it more lightly (DK II 0x45E4F0)
     *
     * @return the threat to us
     */
    private int getFightThreat() {
        AttackTarget ourTarget = entityData.getComponent(entityId, AttackTarget.class);
        int threat = 0;

        // The enemies we fight count however far away they are, not only the ones we sense, so a spell caster
        // fighting from a distance feels their threat too
        Set<EntityId> enemies = new HashSet<>(entityData.findEntities(new FieldFilter<>(AttackTarget.class, "entityId", entityId), AttackTarget.class));
        if (ourTarget != null) {
            enemies.add(ourTarget.entityId);
        }
        for (EntityId enemy : enemies) {
            AttackTarget enemyTarget = entityData.getComponent(enemy, AttackTarget.class);
            int shares = (enemyTarget != null && entityId.equals(enemyTarget.entityId) ? 1 : 0)
                    + (ourTarget != null && enemy.equals(ourTarget.entityId) ? 1 : 0);
            if (shares > 0 && isEnemy(enemy) && isThreat(enemy) && !isFleeing(enemy)) {

                // A creature that walked away has left the fight, it takes no share (DK II 0x4602E0)
                int opponents = (int) entityData.findEntities(new FieldFilter<>(AttackTarget.class, "entityId", enemy), AttackTarget.class)
                        .stream().filter(attacker -> !isFleeing(attacker)).count()
                        + (enemyTarget != null ? 1 : 0);
                threat += shares * (getThreat(enemy) / opponents);
            }
        }

        Keeper keeper = levelInfo.getPlayer(getOwnerId());
        if (keeper != null && keeper.getDungeonHeartLocation() != null) {
            Point heart = keeper.getDungeonHeartLocation();
            Point tile = getCreatureCoordinates();
            int distance = Math.max(Math.abs(tile.x - heart.x), Math.abs(tile.y - heart.y));
            threat = threat * Math.min(distance, HEART_THREAT_DISTANCE) / HEART_THREAT_DISTANCE;
        }

        return threat;
    }

    @Override
    public void startBackOff() {
        BackOff backOff = entityData.getComponent(entityId, BackOff.class);
        entityData.setComponent(entityId, new BackOff(backOff != null ? backOff.overThreatChecks : 0,
                gameTimer.getGameTime() + getBackOffTurns() / (double) levelInfo.getLevelData().getGameLevel().getTicksPerSec()));
    }

    @Override
    public boolean canStepBack() {
        return getBackOffTurns() > 0;
    }

    /**
     * Gets how many turns we step back from our target before we walk away.
     * The more we are scared, the sooner we walk away. We don't step back from
     * one that would catch us (DK II 0x45F030)
     *
     * @return the turns, the durations are in turns despite their names
     */
    private int getBackOffTurns() {
        int fear = entityData.getComponent(entityId, CreatureComponent.class).fear;
        AttackTarget attackTarget = entityData.getComponent(entityId, AttackTarget.class);
        CreatureComponent targetCreature = attackTarget != null ? entityData.getComponent(attackTarget.entityId, CreatureComponent.class) : null;
        if (fear > 0 && attackTarget != null && creature.getFightStyle() != Creature.FightStyle.NON_FIGHTER
                && (targetCreature == null || targetCreature.runSpeed <= entityData.getComponent(entityId, CreatureComponent.class).runSpeed)) {
            int minimum = (int) gameSettings.get(Variable.MiscVariable.MiscType.BACK_OFF_MINIMUM_DURATION_SECONDS).getValue();
            int maximum = (int) gameSettings.get(Variable.MiscVariable.MiscType.BACK_OFF_MAXIMUM_DURATION_SECONDS).getValue();
            return minimum + (maximum - minimum) * (4 - Math.min(getFightThreat() / fear, 4));
        }

        return 0;
    }

    @Override
    public boolean isBackOffOver() {
        BackOff backOff = entityData.getComponent(entityId, BackOff.class);
        return backOff == null || backOff.endTime <= gameTimer.getGameTime();
    }

    @Override
    public boolean backOff() {
        AttackTarget attackTarget = entityData.getComponent(entityId, AttackTarget.class);
        Vector3f targetPosition = attackTarget != null ? getPosition(entityData, attackTarget.entityId) : null;
        Vector3f position = getPosition();
        if (targetPosition == null || position == null) {
            return false;
        }

        // Far enough from the target's reach, we just stand (DK II 0x4602E0)
        CreatureMeleeAttack targetMeleeAttack = entityData.getComponent(attackTarget.entityId, CreatureMeleeAttack.class);
        float range = Math.max(BACK_OFF_RANGE, targetMeleeAttack != null ? targetMeleeAttack.range : 0);
        float dx = position.x - targetPosition.x;
        float dz = position.z - targetPosition.z;
        if (dx * dx + dz * dz > range * range * 5 / 4) {
            stopCreature();
            return true;
        }
        if (!isStopped()) {
            return true;
        }

        // Step back a tile and face the target again. Straight away from it, or else along the wall behind us.
        // DK II backs off a little at a time, so it gets stuck only against a wall right behind it
        Point tile = getCreatureCoordinates();
        int direction = Math.round(FastMath.atan2(dz, dx) / FastMath.QUARTER_PI);
        for (int turn : new int[]{0, 1, -1, 2, -2}) {
            float angle = (direction + turn) * FastMath.QUARTER_PI;
            Point step = new Point(tile.x + Math.round(FastMath.cos(angle)), tile.y + Math.round(FastMath.sin(angle)));
            GraphPath<IMapTileInformation> path = navigationService.findPath(tile, step, this);
            if (path != null) {
                entityData.setComponent(entityId, new Navigation(step, WorldUtils.vectorToPoint(targetPosition), SteeringUtils.pathToList(path)));
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean withdrawToHeroLair() {
        if (getOwnerId() != Player.GOOD_PLAYER_ID || entityData.getComponent(entityId, ReturnToHeroLair.class) == null
                || !isBadlyHurt()) {
            return false;
        }

        // To the first hero lair action point we can get to, else we fight on (DK II 0x4611D0)
        for (ActionPoint actionPoint : levelInfo.getActionPoints()) {
            if (actionPoint.getFlags().contains(Thing.ActionPoint.ActionPointFlag.HERO_LAIR) && !createNavigation(getCreatureCoordinates(), actionPoint.getStart(), null)) {
                rememberWhereWeWithdrewFrom();
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean shouldWithdrawToHeart() {

        // More than 5 tiles from our heart, along either axis (DK II 0x45EDA0, the heroes' check is the lair one)
        Keeper keeper = levelInfo.getPlayer(getOwnerId());
        Point heart = keeper != null ? keeper.getDungeonHeartLocation() : null;
        if (getOwnerId() == Player.GOOD_PLAYER_ID || heart == null || !isBadlyHurt()) {
            return false;
        }
        Point tile = getCreatureCoordinates();
        return Math.max(Math.abs(tile.x - heart.x), Math.abs(tile.y - heart.y)) > 5;
    }

    /**
     * Below a sixth of its health, unless it is fearless or fighting in the
     * Combat Pit (DK II 0x45EDA0, 0x48F250)
     */
    private boolean isBadlyHurt() {
        Health health = entityData.getComponent(entityId, Health.class);
        return entityData.getComponent(entityId, Fearless.class) == null && !isCombatPitFighter()
                && health != null && health.health < health.maxHealth / 6;
    }

    @Override
    public void wanderOnOwnLand() {

        // A tile of our own on the square 10 tiles round us. DK II goes round the square from a random start
        // to its end only, so it may find nothing this time (0x461BB0)
        Point tile = getCreatureCoordinates();
        int side = OWN_LAND_WANDER_DISTANCE * 2;
        for (int i = ThreadLocalRandom.current().nextInt(side * 4); i < side * 4; i++) {
            int offset = i % side - OWN_LAND_WANDER_DISTANCE;
            Point destination = switch (i / side) {
                case 0 -> new Point(tile.x + offset, tile.y - OWN_LAND_WANDER_DISTANCE);
                case 1 -> new Point(tile.x + OWN_LAND_WANDER_DISTANCE, tile.y + offset);
                case 2 -> new Point(tile.x + offset, tile.y + OWN_LAND_WANDER_DISTANCE);
                default -> new Point(tile.x - OWN_LAND_WANDER_DISTANCE, tile.y + offset);
            };
            IMapTileInformation mapTile = mapController.getMapData().getTile(destination);
            if (mapTile != null && mapTile.getOwnerId() == getOwnerId() && !mapController.isSolid(destination)) {
                createNavigation(tile, destination, null);
                return;
            }
        }
    }

    @Override
    public void freePrisoners() {

        // Only from a tile of a Prison that has a door, and the break is done from the door (DK II 0x473C60)
        Point tile = getCreatureCoordinates();
        if (!(mapController.getRoomControllerByCoordinates(tile) instanceof PrisonController prison)
                || prison.getDoor() == null) {
            stateMachine.changeState(CreatureState.IDLE);
            return;
        }
        if (!tile.equals(prison.getDoor())) {
            if (isStopped() && createNavigation(tile, prison.getDoor(), prison.getRoomInstance().getCenter())) {
                stateMachine.changeState(CreatureState.IDLE);
            }
            return;
        }

        // The friends still held inside set off for the door, all on the same tick (0x4E2E70)
        prison.<RoomPrisonerControl>getObjectControl(AbstractRoomController.ObjectType.PRISONER).breakOut(this::isAlly);
        stateMachine.changeState(CreatureState.IDLE);
    }

    @Override
    public boolean escapePrison() {
        if (entityData.getComponent(entityId, PrisonEscape.class) == null) {
            return false;
        }

        // To the tile of the cage below the door, from there onto the door, and free once on it (DK II 0x4739E0)
        Point tile = getCreatureCoordinates();
        if (!(mapController.getRoomControllerByCoordinates(tile) instanceof PrisonController prison)) {
            return true;
        }
        Point door = prison.getDoor();
        if (door == null || tile.equals(door)) {
            prison.<RoomPrisonerControl>getObjectControl(AbstractRoomController.ObjectType.PRISONER).free(entityId);
            stateMachine.changeState(CreatureState.IDLE);
        } else if (isStopped()) {
            Point belowDoor = new Point(door.x, door.y + 1);
            createNavigation(tile, tile.equals(belowDoor) ? door : belowDoor, null);
        }
        return true;
    }

    private boolean isFleeing(EntityId entity) {
        CreatureAi creatureAi = entityData.getComponent(entity, CreatureAi.class);
        if (creatureAi != null) {
            return creatureAi.getCreatureState() == CreatureState.FLEE;
        }

        return false;
    }

    @Override
    public void imprison(short playerId, EntityId roomId) {
        Health health = entityData.getComponent(entityId, Health.class);
        if (health == null) {
            logger.log(Level.INFO, "Tried to imprison me but I don't have any health left!");
            return;
        }

        // In DK II the health of a knocked out creature is its dying time in ticks
        // (DKII.EXE 0x4B2CE0, 0x4599D0). The Prison takes it in with a tenth of its
        // maximum health on top (0x4E2C70)
        int healthLeft = health.health;
        Unconscious unconscious = entityData.getComponent(entityId, Unconscious.class);
        if (unconscious != null) {
            double timeLeft = gameSettings.get(Variable.MiscVariable.MiscType.CREATURE_DYING_STATE_DURATION_SECONDS).getValue()
                    - (gameTimer.getGameTime() - unconscious.startTime);
            healthLeft = (int) (timeLeft * levelInfo.getLevelData().getGameLevel().getTicksPerSec());
        }
        entityData.setComponent(entityId, new Health(Math.clamp(healthLeft + health.maxHealth / 10, 1, health.maxHealth), health.maxHealth));
        entityData.removeComponent(entityId, Unconscious.class);
        entityData.setComponent(entityId, new CreatureImprisoned(gameTimer.getGameTime(), gameTimer.getGameTime()));
        entityData.setComponent(entityId, new Stored(roomId, AbstractRoomController.ObjectType.PRISONER));

        // Switch the control to the imprisoning player
        Owner owner = entityData.getComponent(entityId, Owner.class);
        entityData.setComponent(entityId, new Owner(owner.ownerId, playerId));

        stateMachine.changeState(CreatureState.IMPRISONED);
    }

    @Override
    public boolean isStateTimeExceeded() {
        double timeSpent = gameTimer.getGameTime() - entityData.getComponent(entityId, CreatureAi.class).stateStartTime;
        double stateTargetTime;

        switch (stateMachine.getCurrentState()) {
            case STUNNED -> {

                // A frozen creature stands stunned until it thaws. Otherwise the creature lies there for its stun
                // time, an Imp has none, and then gets up, cutting its stunned animation short (DKII.EXE 0x481020,
                // 0x4810E0: the stunned instance 0x34 can be interrupted)
                if (entityData.getComponent(entityId, Frozen.class) != null) {
                    return false;
                }
                stateTargetTime = entityData.getComponent(entityId, CreatureComponent.class).stunDuration;
            }
            case FALLEN -> {

                // Never shorter than the fall itself, imps for example have no stun duration at all
                stateTargetTime = Math.max(entityData.getComponent(entityId, CreatureComponent.class).stunDuration,
                        getAnimationTime(creature, Creature.AnimationType.FALLBACK));
            }
            case GETTING_UP -> {
                stateTargetTime = getAnimationTime(creature, Creature.AnimationType.GET_UP);
            }
            case ENTERING_DUNGEON -> {
                stateTargetTime = getAnimationTime(creature, Creature.AnimationType.ENTRANCE);
            }
            case TELLING_OFF -> {
                stateTargetTime = getAnimationTime(creature, Creature.AnimationType.MELEE_ATTACK);
            }
            case MELEE_ATTACK -> {

                // The swing plays its start part, the swing and its end part one after another at the animation's
                // frame rate, a part carrying the time left over from the one before it into the next (0x4B3C10,
                // 0x4B41E0). The swing ends on the turn after the turn its last frame is reached (0x496AA0). A
                // Knight's 9 + 30 + 11 frames at 18 fps take 11.1 turns at 4 turns a second, so it swings every 13
                // turns
                ArtResource swing = creature.getAnimation(entityData.getComponent(entityId, CreatureMeleeAttack.class).swipe
                        ? Creature.AnimationType.SWIPE : Creature.AnimationType.MELEE_ATTACK);
                stateTargetTime = 0;
                if (swing != null) {
                    int ticksPerSec = levelInfo.getLevelData().getGameLevel().getTicksPerSec();
                    int fps = swing.<Number>getData(ArtResource.KEY_FPS).intValue();
                    int frames = swing.<Number>getData(ArtResource.KEY_FRAMES).intValue();
                    if (swing.getFlags().contains(ArtResource.ArtResourceFlag.HAS_START_ANIMATION)) {
                        frames += swing.<Number>getData(ArtResource.KEY_START_AF).intValue();
                    }
                    if (swing.getFlags().contains(ArtResource.ArtResourceFlag.HAS_END_ANIMATION)) {
                        frames += swing.<Number>getData(ArtResource.KEY_END_AF).intValue();
                    }
                    int turns = (frames * ticksPerSec + fps - 1) / fps + 1;

                    // The state ends on the turn after this time, and the FIGHT turn after it is the last turn of the
                    // swing, where the next swing can start
                    stateTargetTime = (turns - 2) / (double) ticksPerSec;
                }
            }
            case CAST_SPELL -> {
                stateTargetTime = getAnimationTime(creature, Creature.AnimationType.CAST_SPELL);
            }
            case EATING -> {
                stateTargetTime = getAnimationTime(creature, Creature.AnimationType.EATING);
            }
            case CELEBRATING -> {
                stateTargetTime = BIG_WINNER_CELEBRATION_SECONDS;
            }
            case REAPER_ARRIVE -> {
                stateTargetTime = (double) REAPER_ARRIVE_TICKS / levelInfo.getLevelData().getGameLevel().getTicksPerSec();
            }
            case REAPER_ROAR -> {
                stateTargetTime = getAnimationTime(creature, Creature.AnimationType.ROAR);
            }
            case CHICKEN -> {
                stateTargetTime = entityData.getComponent(entityId, CreatureAi.class).stateDuration;
            }
            case FLEE -> {
                // I couldn't find a variable for this, so lets use this for now
                stateTargetTime = gameSettings.get(Variable.MiscVariable.MiscType.IMP_IDLE_DELAY_BEFORE_REEVALUATION_SECONDS).getValue();
            }
            default -> {
                stateTargetTime = Double.MAX_VALUE;
            }
        }

        return stateTargetTime < timeSpent;
    }

    /**
     * How long one round of a creature's animation takes
     *
     * @param creature the creature
     * @param animation the animation
     * @return the time in seconds, 0 if the creature doesn't have the animation
     */
    public static double getAnimationTime(Creature creature, Creature.AnimationType animation) {
        // TODO: we could cache and calculate these for all centrally, also include the starting and ending animation
        ArtResource animationResource = creature.getAnimation(animation);
        if (animationResource == null) {
            return 0;
        }

        int frames = animationResource.getData("frames");
        int fps = animationResource.getData("fps");

        return frames / (double) fps;
    }

    @Override
    public boolean isClaimed() {
        return Player.NEUTRAL_PLAYER_ID != getOwnerId();
    }

    @Override
    public boolean isPossessed() {
        return entityData.getComponent(entityId, Possessed.class) != null;
    }

    @Override
    public boolean isHungry() {
        CreatureHunger creatureHunger = entityData.getComponent(entityId, CreatureHunger.class);
        if (creatureHunger == null) {
            return false;
        }

        // Every hunger rate seconds without a meal adds another meal to the need, and the clock starts again
        float hungerRate = creature.getAttributes().getHungerRate();
        int mealsMissed = hungerRate > 0 ? (int) ((gameTimer.getGameTime() - creatureHunger.lastEatTime) / hungerRate) : 0;
        TaskComponent task = entityData.getComponent(entityId, TaskComponent.class);
        if (mealsMissed > 0 && task != null && task.taskType == TaskType.GAMBLE && task.taskStarted) {

            // No hunger builds up while gambling: DK II's Casino states have flag 8 in the state table (0x47BBE0),
            // and 0x477C20 adds no meal while it is set. Its clock runs on, so we hold it at one meal due, which
            // the creature owes as soon as it stops
            CreatureHunger held = new CreatureHunger(gameTimer.getGameTime() - hungerRate, creatureHunger.amountNeeded);
            held.lastEatAttemptTime = creatureHunger.lastEatAttemptTime;
            entityData.setComponent(entityId, held);

            return creatureHunger.amountNeeded > 0;
        }
        if (mealsMissed > 0) {
            CreatureComponent creatureComponent = entityData.getComponent(entityId, CreatureComponent.class);
            CreatureHunger updated = new CreatureHunger(creatureHunger.lastEatTime + mealsMissed * hungerRate,
                    creatureHunger.amountNeeded + mealsMissed * creatureComponent.hungerFill);
            updated.lastEatAttemptTime = creatureHunger.lastEatAttemptTime;
            entityData.setComponent(entityId, updated);

            return true;
        }

        return creatureHunger.amountNeeded > 0;
    }

    @Override
    public boolean goToEat() {
        return startEatAttempt() && taskManager.assignEatTask(this);
    }

    private boolean startEatAttempt() {
        double gameTime = gameTimer.getGameTime();
        CreatureHunger hunger = entityData.getComponent(entityId, CreatureHunger.class);
        if (hunger == null || gameTime - hunger.lastEatAttemptTime
                < EAT_ATTEMPT_INTERVAL_TICKS / (double) levelInfo.getLevelData().getGameLevel().getTicksPerSec()) {
            return false;
        }
        // Native attempts are throttled to one per four ticks
        CreatureHunger updated = new CreatureHunger(hunger.lastEatTime, hunger.amountNeeded);
        updated.lastEatAttemptTime = gameTime;
        entityData.setComponent(entityId, updated);
        return true;
    }

    @Override
    public boolean goGambling() {

        // As in the original (0x4560F0), the wish only depends on the gold held, the stake is the money mode one
        // The big winner has stopped gambling (the Gamble task is not valid for it), so it doesn't wish for it
        return !isWorker() && getOwnerId() >= Player.KEEPER1_ID
                && entityData.getComponent(entityId, CasinoBigWinner.class) == null && getGold() > Math.abs(
                gameSettings.get(Variable.MiscVariable.MiscType.GAMBLING_MODIFY_CREATURE_MONEY_MONEY_PER_SECOND).getValue())
                && taskManager.assignTask(this, Creature.JobType.DRINK);
    }

    @Override
    public boolean goGamblingAfterPay() {
        if (entityData.getComponent(entityId, GamblingAfterPay.class) == null) {
            return false;
        }
        entityData.removeComponent(entityId, GamblingAfterPay.class);

        // Only the duties that hunger and fatigue interrupt too, not what the creature has to see through
        Task task = getAssignedTask();
        return task != null && (task.getTaskType() == TaskType.TRAIN || task.getTaskType() == TaskType.RESEARCH
                || task.getTaskType() == TaskType.GUARD || task.getTaskType() == TaskType.COLLECT_PAY) && goGambling();
    }

    @Override
    public boolean goToCollectPay() {
        CreaturePay pay = entityData.getComponent(entityId, CreaturePay.class);
        double gameTime = gameTimer.getGameTime();
        if (pay == null || pay.unpaid <= 0 || pay.attempts >= CreaturePay.MAX_COLLECTION_ATTEMPTS || gameTime < pay.nextAttemptTime) {
            return false;
        }
        if (taskManager.assignCollectPayTask(this)) {
            return true;
        }

        // No gold room with gold in it that we can reach, that is a try that got nothing
        entityData.setComponent(entityId, new CreaturePay(pay.unpaid, pay.attempts + 1, gameTime
                + gameSettings.get(Variable.MiscVariable.MiscType.DELAY_BEFORE_PAY_REEVALUATION_SECONDS).getValue(), false));
        return false;
    }

    @Override
    public void eat(IEntityController target) {

        // Ok, first we destroy the entity we are eating so that nobody else gets it
        Health health = entityData.getComponent(target.getEntityId(), Health.class);
        if (health != null) {
            target.remove();

            if (EAT_INTERRUPTIBLE_STATES.contains(stateMachine.getCurrentState())) {
                stateMachine.changeState(CreatureState.EATING);
            } else if (!isIncapacitated()) {
                sate();
            }
        }
    }

    @Override
    public void sate() {

        // Like in DK II, a creature that isn't hungry gets nothing from a chicken
        if (!isHungry()) {
            return;
        }
        CreatureHunger creatureHunger = entityData.getComponent(entityId, CreatureHunger.class);
        if (creatureHunger.amountNeeded > 0) {

            // Every chicken starts the hunger clock again
            entityData.setComponent(entityId, new CreatureHunger(gameTimer.getGameTime(), creatureHunger.amountNeeded - 1));
        }

        // Every chicken eaten clears the no-food anger
        CreatureMood mood = entityData.getComponent(entityId, CreatureMood.class);
        if (mood != null && mood.noFood != 0) {
            entityData.setComponent(entityId, mood.clear(CreatureMood.REASON_NO_FOOD));
        }

        // Increase health
        setDamage(-creature.getAttributes().getHpFromChicken());
    }

    @Override
    public boolean collectWinnings() {
        return entityData.getComponent(entityId, CasinoBigWinner.class) != null && taskManager.assignCollectWinningsTask(this);
    }

    @Override
    public boolean goToDisco() {
        if (isWorker() || getOwnerId() < Player.KEEPER1_ID || creature.getAnimation(Creature.AnimationType.DANCE) == null) {
            return false;
        }

        // Dance where we stand if we are in the disco already
        if (isAtDisco()) {
            return true;
        }
        for (EntityId casino : entityData.findEntities(new FieldFilter<>(Owner.class, "ownerId", getOwnerId()), CasinoDisco.class, Owner.class)) {
            IRoomController room = mapController.getRoomController(casino);
            if (room != null) {
                List<Point> coordinates = new ArrayList<>(room.getRoomInstance().getCoordinates());
                coordinates.removeIf(p -> !room.isTileAccessible(null, p));
                Point destination = Utils.getRandomItem(coordinates);
                return destination != null && !createNavigation(getCreatureCoordinates(), destination, null);
            }
        }

        return false;
    }

    @Override
    public boolean isAtDisco() {
        IRoomController room = mapController.getRoomControllerByCoordinates(getCreatureCoordinates());
        return room != null && room.getOwnerId() == getOwnerId()
                && entityData.getComponent(room.getEntityId(), CasinoDisco.class) != null;
    }

    @Override
    public void makeHungry() {
        CreatureHunger creatureHunger = entityData.getComponent(entityId, CreatureHunger.class);
        makeHungry(creatureHunger);
    }

    private void makeHungry(CreatureHunger creatureHunger) {
        CreatureComponent creatureComponent = entityData.getComponent(entityId, CreatureComponent.class);
        entityData.setComponent(entityId, new CreatureHunger(creatureHunger.lastEatTime, creatureComponent.hungerFill));
    }

    @Override
    public void setWillFight(boolean willFight) {
        if (willFight) {
            entityData.removeComponent(entityId, Peaceful.class);
        } else {
            entityData.setComponent(entityId, new Peaceful());
        }
    }

    @Override
    public int getResearchPerTick() {
        CreatureComponent creatureComponent = entityData.getComponent(entityId, CreatureComponent.class);
        int pointsPerTick = creatureComponent.researchPerTick;
        int ticksPerSecond = levelInfo.getLevelData().getGameLevel().getTicksPerSec();

        // A slapped creature researches faster, DK II adds the bonus on the
        // first tick of every second (DKII.EXE 0x4C2DC0)
        if (isSlapped() && Math.round(gameTimer.getGameTime() * ticksPerSecond) % ticksPerSecond == 0) {
            int bonusPercentage = (int) gameSettings.get(Variable.MiscVariable.MiscType.INCREASED_WORK_RATE_PECENTAGE_FROM_SLAPPING).getValue();
            pointsPerTick += ticksPerSecond * ticksPerSecond * bonusPercentage * pointsPerTick / 100;
        }
        return pointsPerTick;
    }

    @Override
    public int getTorturedTypeResearchBonus() {
        Variable.MiscVariable percentage = gameSettings.get(Variable.MiscVariable.MiscType.INCREASED_WORK_RATE_PERCENTAGE_OF_PLAYERS_TORTURED_CREATURE_TYPES);
        if (percentage == null || !isTypeTortured(entityData, creature.getCreatureId(), getOwnerId())) {
            return 0;
        }
        return getTorturedTypeBonus(getResearchPerTick(), levelInfo.getLevelData().getGameLevel().getTicksPerSec(), (int) percentage.getValue());
    }

    /**
     * Get the work rate bonus a creature gets once a second while its keeper
     * tortures one of his own creatures of the same type. This is DK II's
     * reckoning: the sum of the ticks per second and the percentage, times the
     * ticks per second and the points per tick, in hundredths
     *
     * @param pointsPerTick the points the creature makes on each tick
     * @param ticksPerSecond the level's ticks per second
     * @param percentage the work rate increase percentage of tortured creature
     * types
     * @return the bonus points
     */
    static int getTorturedTypeBonus(int pointsPerTick, int ticksPerSecond, int percentage) {
        return (ticksPerSecond + percentage) * ticksPerSecond * pointsPerTick / 100;
    }

    /**
     * Is the keeper torturing one of his own creatures of the type, in his own
     * torture chamber
     *
     * @param entityData the entity data
     * @param creatureId the creature type
     * @param playerId the keeper
     * @return true if such a creature is tortured
     */
    static boolean isTypeTortured(EntityData entityData, short creatureId, short playerId) {
        for (EntityId victim : entityData.findEntities(null, CreatureTortured.class, CreatureComponent.class, Owner.class)) {
            Owner owner = entityData.getComponent(victim, Owner.class);
            if (owner.ownerId == playerId && owner.controlId == playerId
                    && entityData.getComponent(victim, CreatureComponent.class).creatureId == creatureId) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getManufacturePerTick() {
        CreatureComponent creatureComponent = entityData.getComponent(entityId, CreatureComponent.class);
        return creatureComponent.manufacturePerTick;
    }

    @Override
    public void giveObject(IObjectController object) {
        if (object.getType() == AbstractRoomController.ObjectType.GOLD) {

            // Take only what we can carry, the rest stays in the pile
            Gold pile = entityData.getComponent(object.getEntityId(), Gold.class);
            int amount = Math.min(pile.gold, getMaxGold() - getGold());
            addGold(amount);
            if (amount < pile.gold) {
                entityData.setComponent(object.getEntityId(), new Gold(pile.gold - amount, pile.maxGold));
            } else {
                entityData.removeComponent(object.getEntityId(), Gold.class);
                object.remove();
            }
        } else {
            logger.log(Level.WARNING, "Object {0} receiving not specified!", object.getType());
        }
    }

    @Override
    public void stopRecuperating() {
        entityData.removeComponent(entityId, CreatureRecuperating.class);
    }

    @Override
    public boolean isRecuperating() {
        return entityData.getComponent(entityId, CreatureRecuperating.class) != null;
    }

    @Override
    public void playAnimation(Creature.AnimationType animation) {
        entityData.setComponent(entityId, new CreatureViewState(creature.getCreatureId(), gameTimer.getGameTime(), animation));
    }

    @Override
    public void turnIntoChicken(double duration) {
        if (creature.getFlags().contains(Creature.CreatureFlag.IS_IMMUNE_TO_CHICKEN)) {
            return;
        }

        // Drop whatever we were doing, unless we are not in control of ourselves
        if (entityData.getComponent(entityId, Possessed.class) == null
                && CHICKEN_INTERRUPTIBLE_STATES.contains(stateMachine.getCurrentState())) {
            stateMachine.changeState(CreatureState.CHICKEN);
            entityData.setComponent(entityId, new CreatureAi(gameTimer.getGameTime(), CreatureState.CHICKEN,
                    creature.getId(), null, duration));
        }
    }

    private boolean isChicken(EntityId entity) {
        CreatureAi creatureAi = entityData.getComponent(entity, CreatureAi.class);
        return creatureAi != null && creatureAi.getCreatureState() == CreatureState.CHICKEN;
    }

    @Override
    public void setPossession(boolean possessed) {
        if (possessed) {
            startPossession();
        } else {
            endPossession();
        }
    }

    @Override
    public void setPossessedMovement(Vector2f direction, float rotation, float pitch, byte speedMode) {
        // Use the level scaled speeds, same as the AI movement does
        Creature.Attributes attributes = creature.getAttributes();
        CreatureComponent creatureComponent = entityData.getComponent(entityId, CreatureComponent.class);
        float walkSpeed = creatureComponent != null ? creatureComponent.speed : attributes.getSpeed();
        float runSpeed = creatureComponent != null ? creatureComponent.runSpeed : attributes.getRunSpeed();
        float speed = switch (speedMode) {
            case PossessedMovement.SPEED_RUN -> runSpeed;
            case PossessedMovement.SPEED_CREEP -> attributes.getShuffleSpeed();
            default -> walkSpeed;
        };
        // A praying creature doesn't move, but still turns
        PossessedAbility possessedAbility = entityData.getComponent(entityId, PossessedAbility.class);
        boolean praying = possessedAbility != null && possessedAbility.ability == Creature.SpecialAbility.PRAY;
        Vector2f normalizedDirection = direction.lengthSquared() > 0 && !praying ? direction.normalize() : new Vector2f();
        if (normalizedDirection.lengthSquared() > 0) {

            // Backing up and strafing are slower than walking forward, relative to the facing
            Vector2f facing = new Vector2f(FastMath.sin(rotation), FastMath.cos(rotation));
            float forward = normalizedDirection.dot(facing);
            float sideways = normalizedDirection.x * facing.y - normalizedDirection.y * facing.x;
            speed *= new Vector2f(forward * (forward < 0 ? POSSESSED_BACKWARD_SPEED_FACTOR : 1f),
                    sideways * POSSESSED_STRAFE_SPEED_FACTOR).length();
        }
        entityData.setComponent(entityId, new PossessedMovement(normalizedDirection, rotation, pitch, speed * SPEED_MULTIPLIER));
    }

    @Override
    public void setPossessedAttack(boolean attacking, int spell) {
        Possessed possessed = entityData.getComponent(entityId, Possessed.class);
        if (possessed == null) {
            return;
        }
        if (attacking) {

            entityData.setComponent(entityId, new Possessed(possessed.manaDrain, possessed.manaCheckTime, true, gameTimer.getGameTime(), spell));
        } else if (possessed.isAttackPending() && !possessed.attackGroup) {

            // A click shorter than a logic tick still gets its one swing
            double pressTime = hasSwungSince(possessed) ? Double.NaN : possessed.attackPressTime;
            entityData.setComponent(entityId, new Possessed(possessed.manaDrain, possessed.manaCheckTime, false, pressTime, possessed.attackSpell));
        }
    }

    private boolean hasSwungSince(Possessed possessed) {
        Attack attack;
        if (possessed.attackSpell > 0) {
            EntityId spellEntity = getPossessedSpell(possessed.attackSpell);
            attack = spellEntity != null ? entityData.getComponent(spellEntity, CreatureSpell.class) : null;
        } else {
            attack = entityData.getComponent(entityId, CreatureMeleeAttack.class);
        }
        return attack == null || (attack.attactStartTime != null && attack.attactStartTime >= possessed.attackPressTime);
    }

    @Override
    public void addPossessedFollower() {
        Possessed possessed = entityData.getComponent(entityId, Possessed.class);
        if (possessed != null) {
            entityData.setComponent(entityId, new Possessed(possessed.manaDrain, possessed.manaCheckTime, false, gameTimer.getGameTime(), true));
        }
    }

    @Override
    public void executePossessedAttack() {

        // A bat can't attack or cast (DK II stops it at the keys, 0x4100F0)
        PossessedAbility possessedAbility = entityData.getComponent(entityId, PossessedAbility.class);
        if (possessedAbility != null && possessedAbility.ability == Creature.SpecialAbility.TURN_TO_BAT) {
            return;
        }


        Possessed possessed = entityData.getComponent(entityId, Possessed.class);
        if (possessed != null && possessed.attackSpell > 0) {
            castPossessedSpell(possessed.attackSpell);
            return;
        }

        CreatureMeleeAttack meleeAttack = entityData.getComponent(entityId, CreatureMeleeAttack.class);
        Position position = entityData.getComponent(entityId, Position.class);
        if (position != null && possessed != null && possessed.attackGroup) {
            addFollowerInView(position);
            return;
        }
        if (meleeAttack == null || position == null) {
            return;
        }

        // The creature fights differently in first person, see the first person stats
        CreatureMeleeAttack firstPersonAttack = new CreatureMeleeAttack(meleeAttack);
        firstPersonAttack.damage = firstPersonAttack.damage * getFirstPersonPercentage(Variable.CreatureStats.StatType.MELEE_DAMAGE) / 100;
        firstPersonAttack.rechargeTime = firstPersonAttack.rechargeTime * getFirstPersonPercentage(Variable.CreatureStats.StatType.MELEE_RECHARGE_TIME_SECONDS) / 100
                * getFirstPersonRechargeModifyPercentage() / 100;
        if (!isAttackRecharged(firstPersonAttack)) {
            return;
        }

        // Nothing to hit, a worker digs but others don't swing at all
        EntityId target = getPossessedAttackTarget(position);
        if (target != null) {

            // A possessed creature's hit lands at once, without the shot delay (0x498E30)
            entityData.setComponent(entityId, new CreatureMeleeAttack(meleeAttack, gameTimer.getGameTime()));
            hitWithMelee(target, meleeAttack, firstPersonAttack.damage);
            revealTo(target);

            // The creature we have hypnotised attacks what we hit (0x514830)
            EntityId hypnotised = getHypnotised();
            if (hypnotised != null) {
                entityData.setComponent(hypnotised, new AttackTarget(target));
            }

            // So do the creatures of our group, a follower takes its leader's target over its own (0x45CB00)
            for (EntityId follower : getPossessedFollowers()) {
                entityData.setComponent(follower, new AttackTarget(target));
            }
        } else if (isWorker() && digTileInFront(position)) {
            entityData.setComponent(entityId, new CreatureMeleeAttack(meleeAttack, gameTimer.getGameTime()));
        } else {
            return;
        }
        entityData.setComponent(entityId, new CreatureViewState(creature.getCreatureId(), gameTimer.getGameTime(), Creature.AnimationType.MELEE_ATTACK));
    }

    @Override
    public void usePossessedAbility(int ability) {
        Creature.SpecialAbility specialAbility = switch (ability) {
            case 1 -> creature.getFirstPersonSpecialAbility1();
            case 2 -> creature.getFirstPersonSpecialAbility2();
            case POSSESSED_SNIPER_MODE_KEY -> hasFirstPersonAbility(Creature.SpecialAbility.SNIPER_MODE)
                    ? Creature.SpecialAbility.SNIPER_MODE : Creature.SpecialAbility.NONE;
            default -> Creature.SpecialAbility.NONE;
        };
        if (specialAbility == Creature.SpecialAbility.TURN_TO_BAT || specialAbility == Creature.SpecialAbility.PRAY
                || specialAbility == Creature.SpecialAbility.SNIPER_MODE) {
            togglePossessedAbility(specialAbility);
        } else if (specialAbility == Creature.SpecialAbility.PICK_UP_WORKERS) {
            pickUpOrThrowWorker();
        } else if (specialAbility == Creature.SpecialAbility.HYPNOTISE) {
            hypnotiseOrRelease();
        } else if (specialAbility == Creature.SpecialAbility.DISGUISE_N_STEAL) {
            disguiseOrReveal();
        } else if (specialAbility == Creature.SpecialAbility.FEAR_ATTACK) {
            frightenTarget();
        }
    }

    private boolean hasFirstPersonAbility(Creature.SpecialAbility ability) {
        return creature.getFirstPersonSpecialAbility1() == ability || creature.getFirstPersonSpecialAbility2() == ability;
    }

    /**
     * The damage of a shot the creature fires. While the possessed creature is
     * in sniper mode its shots do double damage (0x4AC420).
     *
     * @param creatureSpell the spell that makes the shot
     * @return the shot's data 1, its damage if it hits things
     */
    private int getShotData1(toniarts.openkeeper.tools.convert.map.CreatureSpell creatureSpell) {
        PossessedAbility possessedAbility = entityData.getComponent(entityId, PossessedAbility.class);
        if (possessedAbility != null && possessedAbility.ability == Creature.SpecialAbility.SNIPER_MODE
                && levelInfo.getLevelData().getShotById(creatureSpell.getShotTypeId()).getDamageType() == Shot.DamageType.HIT_THING) {
            return creatureSpell.getShotData1() * 2;
        }

        return creatureSpell.getShotData1();
    }

    /**
     * Frightens what our melee attack would hit for 20 game ticks (0x40FA90,
     * 0x514470). Using it again starts the time over.
     */
    private void frightenTarget() {
        EntityId target = getPossessedAttackTarget(entityData.getComponent(entityId, Position.class));
        if (target != null) {
            double duration = FEAR_ATTACK_TICKS / (double) levelInfo.getLevelData().getGameLevel().getTicksPerSec();
            entityData.setComponent(target, new Frightened(entityId, FEAR_ATTACK_SEVERITY, gameTimer.getGameTime() + duration));
        }
    }

    /**
     * Disguises us as a creature of the keeper whose land we stand on, or
     * drops the disguise (0x517270, 0x48EE90). It can't be done on our own or
     * neutral land, or while a creature of that keeper or of their allies sees
     * us: we are within its sight distance and no solid tile is in between.
     */
    private void disguiseOrReveal() {
        if (entityData.getComponent(entityId, Disguised.class) != null) {
            entityData.removeComponent(entityId, Disguised.class);
            return;
        }

        Vector3f position = getPosition();
        IMapTileInformation tile = mapController.getMapData().getTile(WorldUtils.vectorToPoint(position));
        Keeper keeper = tile != null ? levelInfo.getPlayer(tile.getOwnerId()) : null;
        if (keeper == null || keeper.getId() == getOwnerId() || keeper.getId() == Player.NEUTRAL_PLAYER_ID) {
            return;
        }
        for (EntityId entity : entityData.findEntities(null, CreatureComponent.class, Owner.class, Senses.class)) {
            if (keeper.isAlly(entityData.getComponent(entity, Owner.class).ownerId)
                    && getDistanceToCreature(entity) < entityData.getComponent(entity, Senses.class).distanceCanSee
                    && isInSight(position, entity)) {
                return;
            }
        }

        entityData.setComponent(entityId, new Disguised(keeper.getId()));
    }

    /**
     * Drops our disguise when we hit or cast at a creature of the keeper we
     * look like, or of their allies (0x514830)
     *
     * @param target what we hit or cast at
     */
    private void revealTo(EntityId target) {
        Disguised disguised = entityData.getComponent(entityId, Disguised.class);
        Owner owner = entityData.getComponent(target, Owner.class);
        Keeper keeper = owner != null ? levelInfo.getPlayer(owner.ownerId) : null;
        if (disguised != null && keeper != null && keeper.isAlly(disguised.ownerId)) {
            entityData.removeComponent(entityId, Disguised.class);
        }
    }

    /**
     * Hypnotises the nearest enemy creature in sight within 60 degrees of the
     * view, or lets go of the one we have hypnotised (0x5163C0). It joins our
     * keeper and follows us (0x461980), our keeper pays
     * HYPNOTISM_MANA_DRAIN_PER_SECOND for it (0x4C10E0). If the nearest
     * creature is immune to turncoat, nothing happens.
     */
    private void hypnotiseOrRelease() {
        EntityId hypnotised = getHypnotised();
        if (hypnotised != null) {
            releaseHypnotised(hypnotised);
            return;
        }

        Position position = entityData.getComponent(entityId, Position.class);
        EntityId nearestCreature = null;
        float nearestDistance = Float.MAX_VALUE;
        for (EntityId entity : entityData.findEntities(null, CreatureComponent.class, Position.class)) {
            float distance = getDistanceToCreature(entity);
            if (distance < nearestDistance && canBeHypnotised(entity) && isInFront(position, entity) && isInSight(position.position, entity)) {
                nearestDistance = distance;
                nearestCreature = entity;
            }
        }
        if (nearestCreature == null || getCreature(nearestCreature).getFlags().contains(Creature.CreatureFlag.IS_IMMUNE_TO_TURNCOAT)) {
            return;
        }

        entityData.setComponent(nearestCreature, new Hypnotised(entityId, entityData.getComponent(nearestCreature, Owner.class).ownerId));
        entityData.setComponent(nearestCreature, new Owner(getOwnerId(), getOwnerId()));
        entityData.removeComponent(nearestCreature, AttackTarget.class);
        Variable.MiscVariable manaDrain = gameSettings.get(Variable.MiscVariable.MiscType.HYPNOTISM_MANA_DRAIN_PER_SECOND);
        if (manaDrain != null && manaDrain.getValue() > 0) {
            entityData.setComponent(nearestCreature, new Mana(-(int) manaDrain.getValue()));
        }

        // This runs on the render thread, the creature starts following us on its own tick
        entityData.setComponent(nearestCreature, new FollowTarget(entityId));
    }

    private boolean canBeHypnotised(EntityId entity) {
        return !entityId.equals(entity) && creaturesController.isValidEntity(entity) && isEnemy(entity) && !isIncapacitated(entity)
                && entityData.getComponent(entity, Possessed.class) == null
                && getCreature(entity).getFlags().contains(Creature.CreatureFlag.CAN_BE_HYPNOTIZED);
    }

    private Creature getCreature(EntityId entity) {
        return levelInfo.getLevelData().getCreature(entityData.getComponent(entity, CreatureComponent.class).creatureId);
    }

    /**
     * Is there no solid tile on the line from us to the entity (0x4512F0)
     */
    private boolean isInSight(Vector3f position, EntityId entity) {
        Vector3f target = getPosition(entityData, entity);
        int steps = (int) FastMath.ceil(position.distance(target) / (WorldUtils.TILE_WIDTH / 8));
        for (int i = 1; i <= steps; i++) {
            Point tile = WorldUtils.vectorToPoint(position.clone().interpolateLocal(target, i / (float) steps));
            if (mapController.getMapData().getTile(tile) == null || mapController.isSolid(tile)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Gets the creature we have hypnotised
     *
     * @return the creature, {@code null} if there is none
     */
    private EntityId getHypnotised() {
        return entityData.findEntity(new FieldFilter<>(Hypnotised.class, "entity", entityId), Hypnotised.class);
    }

    /**
     * Lets go of the creature we have hypnotised (0x5163C0, 0x514590). It goes
     * back to its owner and stops fighting (0x45EF50).
     *
     * @param hypnotised the creature we have hypnotised
     */
    private void releaseHypnotised(EntityId hypnotised) {
        short ownerId = entityData.getComponent(hypnotised, Hypnotised.class).ownerId;
        entityData.removeComponent(hypnotised, Hypnotised.class);
        entityData.removeComponent(hypnotised, Mana.class);
        entityData.removeComponent(hypnotised, AttackTarget.class);
        entityData.setComponent(hypnotised, new Owner(ownerId, ownerId));

        // Without the follow target the creature goes back to idle on its own tick
        FollowTarget followTarget = entityData.getComponent(hypnotised, FollowTarget.class);
        if (followTarget != null && followTarget.entityId.equals(entityId)) {
            entityData.removeComponent(hypnotised, FollowTarget.class);
        }
    }

    @Override
    public EntityId getHypnotiser() {
        Hypnotised hypnotised = entityData.getComponent(entityId, Hypnotised.class);
        return hypnotised != null ? hypnotised.entity : null;
    }

    /**
     * Picks up the nearest worker of our keeper in front of us, or throws the
     * one we carry (0x516EC0). The worker has to be within a tile and 60
     * degrees of the view. It leaves its work and its AI while it is carried.
     * DK II's throw only marks the worker thrown, along the view (flag 0x200
     * and +0x48D), and the worker's own turn throws it (0x4831D0). Here the
     * HaulingSystem throws it on the game logic tick.
     */
    private void pickUpOrThrowWorker() {
        PossessedAbility possessedAbility = entityData.getComponent(entityId, PossessedAbility.class);
        if (possessedAbility != null && possessedAbility.entity != null) {
            Position position = entityData.getComponent(entityId, Position.class);
            if (possessedAbility.releaseVelocity == null && position != null) {
                Vector3f velocity = new Vector3f(FastMath.sin(position.rotation) * THROWN_WORKER_SPEED, THROWN_WORKER_LIFT,
                        FastMath.cos(position.rotation) * THROWN_WORKER_SPEED);
                entityData.setComponent(entityId, new PossessedAbility(possessedAbility.ability, possessedAbility.checkTime,
                        possessedAbility.entity, velocity));
            }
            return;
        }

        Position position = entityData.getComponent(entityId, Position.class);
        EntityId nearestWorker = null;
        float nearestDistance = POSSESSED_PICK_UP_REACH;
        for (Point tile : WorldUtils.getSurroundingTiles(mapController.getMapData(), WorldUtils.vectorToPoint(position.position), true)) {
            for (EntityId entity : entityPositionLookup.getEntitiesInLocation(tile)) {
                float distance = getDistanceToCreature(entity);
                if (distance < nearestDistance && isOwnWorker(entity) && isInFront(position, entity)) {
                    nearestDistance = distance;
                    nearestWorker = entity;
                }
            }
        }
        if (nearestWorker == null) {
            return;
        }

        creaturesController.createController(nearestWorker).unassingCurrentTask();
        entityData.removeComponent(nearestWorker, CreatureAi.class);
        entityData.removeComponent(nearestWorker, Navigation.class);
        CreatureViewState viewState = entityData.getComponent(nearestWorker, CreatureViewState.class);
        if (viewState != null) {
            entityData.setComponent(nearestWorker, new CreatureViewState(viewState.creatureId, gameTimer.getGameTime(), Creature.AnimationType.IN_HAND));
        }
        entityData.setComponent(entityId, new PossessedAbility(Creature.SpecialAbility.PICK_UP_WORKERS, gameTimer.getGameTime(), nearestWorker));
    }

    private boolean isOwnWorker(EntityId entity) {
        Owner owner = entityData.getComponent(entity, Owner.class);
        return !entityId.equals(entity) && creaturesController.isValidEntity(entity) && owner != null
                && owner.ownerId == getOwnerId() && !isIncapacitated(entity) && creaturesController.createController(entity).isWorker();
    }

    /**
     * Turns an ability that stays on, such as the Vampire's bat form, on or
     * off. A bat flies (0x517190, 0x48ED40). A praying creature stays still
     * and heals (0x517220). Sniper mode is a flag on the creature (0x5171D0).
     *
     * @param ability the ability to turn on or off
     */
    private void togglePossessedAbility(Creature.SpecialAbility ability) {
        PossessedAbility possessedAbility = entityData.getComponent(entityId, PossessedAbility.class);
        boolean on = possessedAbility == null || possessedAbility.ability != ability;
        if (on) {
            entityData.setComponent(entityId, new PossessedAbility(ability, gameTimer.getGameTime()));
        } else {
            entityData.removeComponent(entityId, PossessedAbility.class);
        }

        Mobile mobile = entityData.getComponent(entityId, Mobile.class);
        if (ability == Creature.SpecialAbility.TURN_TO_BAT && mobile != null) {
            entityData.setComponent(entityId, new Mobile(on || creature.getFlags().contains(Creature.CreatureFlag.CAN_FLY),
                    mobile.canWalkOnWater, mobile.canWalkOnLava, mobile.maxSpeed));
        }
    }

    /**
     * Casts a spell of the possessed creature, if it has recharged
     * (0x514830). It is aimed at the creature closest to where the possessed
     * creature looks, or straight ahead if there is none.
     *
     * @param spell 1 to 3, the creature's spell in the order the creature has
     * them
     */
    private void castPossessedSpell(int spell) {
        EntityId spellEntity = getPossessedSpell(spell);
        Position position = entityData.getComponent(entityId, Position.class);
        if (spellEntity == null || position == null) {
            return;
        }

        // In first person the recharge time is scaled as the melee attack's is (0x4F7AB0)
        CreatureSpell attack = entityData.getComponent(spellEntity, CreatureSpell.class);
        CreatureSpell firstPersonAttack = new CreatureSpell(attack.creatureSpellId, entityId, attack.rechargeTime
                * getFirstPersonPercentage(Variable.CreatureStats.StatType.MELEE_RECHARGE_TIME_SECONDS) / 100
                * getFirstPersonRechargeModifyPercentage() / 100, attack.range);
        firstPersonAttack.attactStartTime = attack.attactStartTime;
        toniarts.openkeeper.tools.convert.map.CreatureSpell creatureSpell = levelInfo.getLevelData().getCreatureSpellById(attack.creatureSpellId);
        Set<CreatureSpellFlag> flags = creatureSpell.getFlags();
        if (!isAttackRecharged(firstPersonAttack)) {
            return;
        }

        // The Imp's teleport and haste are no casts, and they recharge even when they do nothing
        if (flags.contains(CreatureSpellFlag.IMP_TELEPORT)) {
            teleportToDungeonHeart(position);
            entityData.setComponent(spellEntity, new CreatureSpell(attack, gameTimer.getGameTime()));
            return;
        }

        // Nor is the Imp's haste. It changes the speed with the spell's values and the same speed timer as a slap,
        // so a speed change that is still running is left as it is, but the spell recharges all the same (0x48CAD0).
        // A speed-up shows the speed blur for as many game ticks as the percentage (0x514830)
        if (flags.contains(CreatureSpellFlag.IMP_HASTE)) {
            shotsController.createShot(creatureSpell.getShotTypeId(), creatureSpell.getShotData1(), creatureSpell.getShotData2(), getOwnerId(),
                    getPosition(), entityId, entityId);
            entityData.setComponent(spellEntity, new CreatureSpell(attack, gameTimer.getGameTime()));
            if (creatureSpell.getShotData1() > 100) {
                entityData.setComponent(entityId, new SpeedBlur(gameTimer.getGameTime()
                        + creatureSpell.getShotData1() / (double) levelInfo.getLevelData().getGameLevel().getTicksPerSec()));
            }
            return;
        }

        EntityId target = flags.contains(CreatureSpellFlag.CAST_ON_SELF) ? entityId
                : getPossessedSpellTarget(position, flags.contains(CreatureSpellFlag.CREATURE));
        if (target != null) {
            entityData.setComponent(entityId, new CastShot(attack.creatureSpellId, target, attack.range,
                    gameTimer.getGameTime() + getShotDelay(attack.creatureSpellId)));
            releaseCastShot();
            revealTo(target);
        } else {
            PossessedMovement movement = entityData.getComponent(entityId, PossessedMovement.class);
            float pitch = movement != null ? movement.pitch : 0;
            Vector3f ahead = getViewDirection(position.rotation, pitch).multLocal(attack.range);
            Vector3f start = getShotPosition(attack.creatureSpellId, position.position.add(ahead));
            if (!shotsController.fireShot(creatureSpell.getShotTypeId(), getShotData1(creatureSpell), creatureSpell.getShotData2(),
                    getOwnerId(), start, start.add(ahead), entityId, attack.range)) {
                return;
            }
        }
        entityData.setComponent(spellEntity, new CreatureSpell(attack, gameTimer.getGameTime()));
        entityData.setComponent(entityId, new CreatureViewState(creature.getCreatureId(), gameTimer.getGameTime(), Creature.AnimationType.CAST_SPELL));
    }

    /**
     * Teleports the possessed Imp to the middle of its keeper's dungeon heart,
     * the one closest to it in DK II (0x514830, 0x4C61C0). Where it looks
     * doesn't matter.
     *
     * @param position the position of the Imp
     */
    private void teleportToDungeonHeart(Position position) {
        Keeper keeper = levelInfo.getPlayer(getOwnerId());
        Point heart = keeper != null ? keeper.getDungeonHeartLocation() : null;
        if (heart != null) {
            Vector3f destination = WorldUtils.pointToVector3f(heart);
            gameEventService.emitEffect(IMP_TELEPORT_OUT_EFFECT, position.position, 0);
            gameEventService.emitEffect(IMP_TELEPORT_IN_EFFECT, destination, 0);
            entityData.setComponent(entityId, new Position(position.rotation, destination));
        }
    }

    /**
     * The direction a possessed creature looks in
     *
     * @param rotation the facing of the creature
     * @param pitch the pitch of the view, positive looking up, in radians
     * @return the unit vector of the view
     */
    private static Vector3f getViewDirection(float rotation, float pitch) {
        return new Vector3f(FastMath.sin(rotation) * FastMath.cos(pitch), FastMath.sin(pitch),
                FastMath.cos(rotation) * FastMath.cos(pitch));
    }

    /**
     * Finds the possessed creature's spell, the creature only has the spells
     * of its level
     *
     * @param spell 1 to 3, the creature's spell in the order the creature has
     * them
     * @return the creature spell entity, {@code null} if the creature doesn't
     * have the spell
     */
    private EntityId getPossessedSpell(int spell) {
        if (spell < 1 || spell > creature.getSpells().size()) {
            return null;
        }

        short creatureSpellId = creature.getSpells().get(spell - 1).getCreatureSpellId();
        CreatureSpells creatureSpells = entityData.getComponent(entityId, CreatureSpells.class);
        if (creatureSpells == null) {
            return null;
        }
        for (EntityId spellEntity : creatureSpells.creatureSpells) {
            if (entityData.getComponent(spellEntity, CreatureSpell.class).creatureSpellId == creatureSpellId) {
                return spellEntity;
            }
        }

        return null;
    }

    /**
     * Finds what a possessed creature's spell aims at (0x515520): the creature
     * closest to where the possessed creature looks. It has to be within the
     * first person accuracy, which DK II compares to one minus the cosine of
     * the angle off the view as FIRST_PERSON_ACCURACY_ANGLE_DEGREES / 720. So
     * the 45 of the levels lets through about 20 degrees. The view includes
     * its pitch, and DK II also wants a line of sight to the creature
     * (0x4512F0), here that is no solid tile in between. Doors are not
     * checked.
     *
     * @param position the position of the possessed creature
     * @param ownCreatures whether the spell is for our own creatures rather
     * than the enemy's
     * @return the target, {@code null} if there is nothing to aim at
     */
    private EntityId getPossessedSpellTarget(Position position, boolean ownCreatures) {
        Variable.MiscVariable accuracy = gameSettings.get(Variable.MiscVariable.MiscType.FIRST_PERSON_ACCURACY_ANGLE_DEGREES);
        float bestOffView = (accuracy != null ? accuracy.getValue() : 45) / 720f;
        EntityId bestTarget = null;
        PossessedMovement movement = entityData.getComponent(entityId, PossessedMovement.class);
        Vector3f view = getViewDirection(position.rotation, movement != null ? movement.pitch : 0);
        Point center = WorldUtils.vectorToPoint(position.position);
        int reach = (int) FastMath.ceil(FastMath.sqrt(POSSESSED_SPELL_REACH_SQUARED));
        for (int x = center.x - reach; x <= center.x + reach; x++) {
            for (int y = center.y - reach; y <= center.y + reach; y++) {
                IMapTileInformation tile = mapController.getMapData().getTile(x, y);
                if (tile == null) {
                    continue;
                }

                for (EntityId entity : entityPositionLookup.getEntitiesInLocation(tile)) {
                    Owner owner = entityData.getComponent(entity, Owner.class);
                    Vector3f targetPosition = getPosition(entityData, entity);
                    if (owner == null || targetPosition == null || entityId.equals(entity) || !creaturesController.isValidEntity(entity)
                            || isIncapacitated(entity) || (owner.ownerId == getOwnerId()) != ownCreatures) {
                        continue;
                    }

                    float dx = targetPosition.x - position.position.x;
                    float dz = targetPosition.z - position.position.z;
                    float distanceSquared = dx * dx + dz * dz;
                    if (distanceSquared >= POSSESSED_SPELL_REACH_SQUARED || distanceSquared == 0) {
                        continue;
                    }

                    float dy = targetPosition.y - position.position.y;
                    float offView = 1 - (view.x * dx + view.y * dy + view.z * dz) / FastMath.sqrt(distanceSquared + dy * dy);
                    if (offView < bestOffView && entityPositionLookup.hasLineOfSight(position.position, targetPosition)) {
                        bestOffView = offView;
                        bestTarget = entity;
                    }
                }
            }
        }

        return bestTarget;
    }

    /**
     * Finds what the possessed creature hits: the nearest target on the tiles
     * around it that is within the cone in front of it. The melee range of the
     * creature doesn't matter here.
     *
     * @param position the position of the possessed creature
     * @return the target to hit, or {@code null} if there is nothing to hit
     */
    private EntityId getPossessedAttackTarget(Position position) {
        EntityId nearestTarget = null;
        float nearestDistance = Float.MAX_VALUE;
        for (Point tile : WorldUtils.getSurroundingTiles(mapController.getMapData(), WorldUtils.vectorToPoint(position.position), true)) {
            for (EntityId entity : entityPositionLookup.getEntitiesInLocation(tile)) {
                float distance = getDistanceToCreature(entity);
                if (distance < nearestDistance && isPossessedAttackTarget(entity) && isInFront(position, entity)) {
                    nearestDistance = distance;
                    nearestTarget = entity;
                }
            }
        }

        return nearestTarget;
    }

    /**
     * Can the possessed creature hit the entity with its melee attack. Any
     * creature but our own, also neutral and allied ones, and the doors and
     * the traps that are not ours. A hidden trap is not hit until it shows.
     * The Dungeon Heart can't be hit, it is not among the objects the original
     * game lets a possessed creature hit.
     *
     * @param entity the entity in reach
     * @return true if the entity is something to hit
     */
    private boolean isPossessedAttackTarget(EntityId entity) {
        Owner owner = entityData.getComponent(entity, Owner.class);
        if (entityId.equals(entity) || owner == null || owner.ownerId == getOwnerId()) {
            return false;
        }
        return DoorAttackTarget.isBreakable(entityData, entity)
                || TrapAttackTarget.isAttackable(entityData, entity)
                || (creaturesController.isValidEntity(entity) && !isIncapacitated(entity));
    }

    private boolean isInFront(Position position, EntityId entity) {
        return getFacing(position, entity) >= POSSESSED_ATTACK_MIN_FACING;
    }

    /**
     * How straight the creature looks at the entity
     *
     * @return the cosine of the angle between where the creature looks and
     * where the entity is, 1 if it is right on the entity
     */
    private float getFacing(Position position, EntityId entity) {
        Vector3f targetPosition = getPosition(entityData, entity);
        float dx = targetPosition.x - position.position.x;
        float dz = targetPosition.z - position.position.z;
        float length = FastMath.sqrt(dx * dx + dz * dz);
        if (length == 0) {
            return 1;
        }

        return (FastMath.sin(position.rotation) * dx + FastMath.cos(position.rotation) * dz) / length;
    }

    /**
     * A possessed worker digs the tile next to it in the direction it is
     * facing. Dirt, gold, gems and walls can be dug, also our own walls.
     *
     * @param position the position of the possessed worker
     * @return true if there was something to dig
     */
    private boolean digTileInFront(Position position) {
        float x = FastMath.sin(position.rotation);
        float z = FastMath.cos(position.rotation);
        Point tile = WorldUtils.vectorToPoint(position.position);
        if (Math.abs(x) >= Math.abs(z)) {
            tile = new Point(tile.x + (x > 0 ? 1 : -1), tile.y);
        } else {
            tile = new Point(tile.x, tile.y + (z > 0 ? 1 : -1));
        }

        IMapTileInformation mapTile = mapController.getMapData().getTile(tile);
        if (mapTile == null) {
            return false;
        }
        Set<Terrain.TerrainFlag> flags = mapController.getTerrain(mapTile).getFlags();
        if (!flags.contains(Terrain.TerrainFlag.SOLID) || (flags.contains(Terrain.TerrainFlag.IMPENETRABLE) && mapTile.getGold() <= 0)) {
            return false;
        }

        addGold(mapController.damageTile(tile, getOwnerId(), this));
        return true;
    }

    /**
     * Our own creature that the possessed creature looks at joins its group
     * and follows it. It is the one closest to where the creature looks, at
     * most FIRST_PERSON_ACCURACY_ANGLE_DEGREES off in the way the original game
     * measures it, and closer than 7 tiles. The group has room for
     * MAXIMUM_GROUP_NUMBER_OF_FOLLOWERS creatures.
     *
     * @param position the position of the possessed creature
     */
    private void addFollowerInView(Position position) {
        if (getPossessedFollowers().size() >= getMiscVariable(Variable.MiscVariable.MiscType.MAXIMUM_GROUP_NUMBER_OF_FOLLOWERS, 8)) {
            return;
        }

        // The original compares the angle, in 2048ths of a circle, to one minus the cosine, in 4096ths
        float bestFacing = 1 - getMiscVariable(Variable.MiscVariable.MiscType.FIRST_PERSON_ACCURACY_ANGLE_DEGREES, 45) / 720f;
        EntityId follower = null;
        Point tile = WorldUtils.vectorToPoint(position.position);
        for (int x = tile.x - POSSESSED_GROUP_RANGE; x <= tile.x + POSSESSED_GROUP_RANGE; x++) {
            for (int y = tile.y - POSSESSED_GROUP_RANGE; y <= tile.y + POSSESSED_GROUP_RANGE; y++) {
                if (mapController.getMapData().getTile(x, y) == null) {
                    continue;
                }
                for (EntityId entity : entityPositionLookup.getEntitiesInLocation(new Point(x, y))) {
                    float facing = getFacing(position, entity);
                    if (facing > bestFacing && canJoinPossessedGroup(entity) && getDistanceToCreature(entity) < POSSESSED_GROUP_RANGE) {
                        bestFacing = facing;
                        follower = entity;
                    }
                }
            }
        }
        if (follower == null) {
            return;
        }

        ICreatureController creatureController = creaturesController.createController(follower);
        creatureController.unassingCurrentTask();
        creatureController.setFollowTarget(entityId);
        creatureController.getStateMachine().changeState(CreatureState.FOLLOW);
    }

    /**
     * Can the creature join the group of the possessed creature. Our own
     * creatures can, but not the ones already in a group, the imprisoned or
     * tortured ones, the incapacitated ones or the Horny Reaper.
     */
    private boolean canJoinPossessedGroup(EntityId entity) {
        Owner owner = entityData.getComponent(entity, Owner.class);
        CreatureComponent creatureComponent = entityData.getComponent(entity, CreatureComponent.class);
        return !entityId.equals(entity) && owner != null && owner.ownerId == getOwnerId() && creatureComponent != null
                && creaturesController.isValidEntity(entity) && !isIncapacitated(entity)
                && entityData.getComponent(entity, FollowTarget.class) == null
                && entityData.getComponent(entity, CreatureImprisoned.class) == null
                && entityData.getComponent(entity, CreatureTortured.class) == null
                && !levelInfo.getLevelData().getCreature(creatureComponent.creatureId).getFlags().contains(Creature.CreatureFlag.IS_HORNY);
    }

    /**
     * The group of the possessed creature. The creature it has hypnotised is
     * not in it, that one has its own slot (0x5163C0, 0x461980).
     */
    private Set<EntityId> getPossessedFollowers() {
        Set<EntityId> followers = new HashSet<>(entityData.findEntities(new FieldFilter<>(FollowTarget.class, "entityId", entityId), FollowTarget.class));
        followers.removeIf(follower -> entityData.getComponent(follower, Hypnotised.class) != null);
        return followers;
    }

    private int getMiscVariable(Variable.MiscVariable.MiscType type, int defaultValue) {
        Variable.MiscVariable variable = gameSettings.get(type);
        return variable != null ? (int) variable.getValue() : defaultValue;
    }

    @Override
    public boolean isInPossessedGroup() {
        FollowTarget followTarget = entityData.getComponent(entityId, FollowTarget.class);
        return followTarget != null && entityData.getComponent(followTarget.entityId, Possessed.class) != null
                && entityData.getComponent(entityId, Hypnotised.class) == null;
    }

    private void startPossession() {
        if (entityData.getComponent(entityId, Possessed.class) != null) {
            return;
        }

        CreatureComponent creatureComponent = entityData.getComponent(entityId, CreatureComponent.class);
        int manaDrain = creatureComponent != null ? creatureComponent.posessionManaCost : 0;
        if (isPossessionUpgraded()) {
            manaDrain = manaDrain * (int) gameSettings.get(Variable.MiscVariable.MiscType.UPGRADED_POSSESSION_COST_PERCENTAGE).getValue() / 100;
        }
        Mana mana = entityData.getComponent(entityId, Mana.class);
        entityData.setComponent(entityId, new Mana(mana != null ? mana.manaGeneration - manaDrain : -manaDrain));
        entityData.setComponent(entityId, new Possessed(manaDrain, gameTimer.getGameTime()));
        entityData.removeComponent(entityId, CreatureAi.class);
        entityData.removeComponent(entityId, Navigation.class);
        unassingCurrentTask();
    }

    /**
     * DK II charges less for possessing once the keeper has upgraded the
     * Possession spell (DKII.EXE 0x4C17A0 and 0x4C10E0)
     *
     * @return whether the owner has upgraded the Possession spell
     */
    private boolean isPossessionUpgraded() {
        Keeper keeper = levelInfo.getPlayer(getOwnerId());
        return keeper != null && keeper.getAvailableSpells().stream()
                .anyMatch(spell -> spell.isUpgraded()
                && levelInfo.getLevelData().getKeeperSpellById(spell.getId()).getTargetRule() == KeeperSpell.TargetRule.POSESSION);
    }

    private void endPossession() {
        Possessed possessed = entityData.getComponent(entityId, Possessed.class);
        if (possessed == null) {
            return;
        }

        // Return the mana flow
        Mana mana = entityData.getComponent(entityId, Mana.class);
        int manaGeneration = (mana != null ? mana.manaGeneration : 0) + possessed.manaDrain;
        if (manaGeneration == 0) {
            entityData.removeComponent(entityId, Mana.class);
        } else {
            entityData.setComponent(entityId, new Mana(manaGeneration));
        }

        // The abilities only last the possession. A carried worker falls down on its own turn once its carrier is
        // no longer possessed (0x4831D0), here on the HaulingSystem's tick
        PossessedAbility possessedAbility = entityData.getComponent(entityId, PossessedAbility.class);
        if (possessedAbility != null && possessedAbility.entity != null) {
            entityData.setComponent(entityId, new PossessedAbility(possessedAbility.ability, possessedAbility.checkTime,
                    possessedAbility.entity, new Vector3f()));
        } else if (possessedAbility != null) {
            togglePossessedAbility(possessedAbility.ability);
        }

        // The hypnotised creature is let go (0x514590)
        EntityId hypnotised = getHypnotised();
        if (hypnotised != null) {
            releaseHypnotised(hypnotised);
        }

        // The disguise drops (0x4BE090)
        entityData.removeComponent(entityId, Disguised.class);
        entityData.removeComponent(entityId, PossessedMovement.class);
        entityData.removeComponent(entityId, Possessed.class);

        // The group follows us only while we are possessed
        for (EntityId follower : getPossessedFollowers()) {
            entityData.removeComponent(follower, FollowTarget.class);
        }
        entityData.setComponent(entityId, new CreatureAi(gameTimer.getGameTime(), CreatureState.IDLE, getCreature().getCreatureId()));
    }

    @Override
    public void convertCreature(short playerId) {
        removeFromRoomStrorage();

        // Remove our lair from the last player
        removeLair();

        // Set new owner
        entityData.setComponent(entityId, new Owner(playerId, playerId));

        // Remove good player stuff
        entityData.removeComponent(entityId, Objective.class);

        // Reset AI
        getStateMachine().changeState(CreatureState.IDLE);
    }

    @Override
    public void turncoat(short playerId, double duration) {
        if (creature.getFlags().contains(Creature.CreatureFlag.IS_IMMUNE_TO_TURNCOAT)) {
            return;
        }

        entityData.setComponent(entityId, new Turncoat(playerId, gameTimer.getGameTime() + duration));
    }

    @Override
    public void rebel() {
        short keeperId = getOwnerId();
        removeFromRoomStrorage();
        removeLair();
        entityData.setComponent(entityId, new Owner(Player.GOOD_PLAYER_ID, Player.GOOD_PLAYER_ID));
        entityData.setComponent(entityId, new Objective(Thing.HeroParty.Objective.KILL_PLAYER, keeperId, 0));

        // The mentor tells the keeper, like DK II (0x4750B0, message 0x13)
        gameEventService.emitToPlayer(GameEventType.CREATURE_REBELLED, keeperId);

        // Start on the objective
        getStateMachine().changeState(CreatureState.IDLE);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CreatureController that)) {
            return false;
        }

        return Objects.equals(entityId, that.entityId);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(entityId);
    }
}
