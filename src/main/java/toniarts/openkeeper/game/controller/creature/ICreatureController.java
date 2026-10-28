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

import com.badlogic.gdx.ai.fsm.StateMachine;
import com.jme3.math.Vector2f;
import com.simsilica.es.EntityId;
import toniarts.openkeeper.game.event.GameEventType;
import toniarts.openkeeper.utils.Point;
import java.util.function.Consumer;
import toniarts.openkeeper.game.controller.entity.IEntityController;
import toniarts.openkeeper.game.controller.object.IObjectController;
import toniarts.openkeeper.game.data.ObjectiveType;
import toniarts.openkeeper.game.logic.IGameLogicUpdatable;
import toniarts.openkeeper.game.navigation.pathfinding.INavigable;
import toniarts.openkeeper.game.task.Task;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Thing;

/**
 * Controls creature entities
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface ICreatureController extends IGameLogicUpdatable, INavigable, IEntityController {

    /** Move to the authored end position after a completed Portal entrance animation. */
    public void completeDungeonEntrance();

    public boolean shouldFleeOrAttack();

    /**
     * An idle fighter hits an enemy door that is next to it, as DKII.EXE does
     *
     * @return true if there is such a door and we are on it
     */
    public boolean attackAdjacentDoor();

    /**
     * A fighter with no enemy creature to fight goes for the nearest enemy
     * trap it sees, as DKII.EXE does
     *
     * @return true if there is such a trap and we are on it
     */
    public boolean attackVisibleTrap();

    /**
     * Checks what the creature sees and hears
     */
    public void checkSurroundings();

    public void unassingCurrentTask();

    public void navigateToRandomPoint();

    public IPartyController getParty();

    public StateMachine<ICreatureController, CreatureState> getStateMachine();

    public boolean hasObjective();

    public boolean followObjective();

    public boolean needsLair();

    public boolean hasLair();

    public boolean findLair();

    public boolean isNeedForSleep();

    public boolean goToSleep();

    /**
     * Takes on the job of the room the keeper dropped the creature into, as
     * in DK II. Right after the drop the job comes first, later only when
     * the creature has no need to see to. A job that can't be done any more
     * is forgotten
     *
     * @return {@code true} if the creature took on the job
     */
    public boolean doDroppedJob();

    /**
     * Handles the manual rest target after landing and getting up. Rest can be
     * assigned without fatigue debt, provided the creature's bed is in that Lair.
     *
     * @return whether a sleep task was assigned
     */
    public boolean goToSleepAfterLairDrop();

    public void findWork(Consumer<Boolean> workResult);

    public boolean isWorker();

    public boolean isTooMuchGold();

    public boolean dropGoldToTreasury();

    public boolean isStopped();

    public void navigateToAssignedTask();

    public boolean isAtAssignedTaskTarget();

    /**
     * Drops all the gold the creature carries on the floor where it stands
     */
    public void dropGold();

    /**
     * Takes the creature off the map for good, as the level script action
     * Remove From Map does (DKII.EXE 0x49AC80, type 0x23). The creature is
     * neither killed nor does it drop its gold, it just ceases to exist.
     */
    public void removeFromMap();

    public boolean isWorkNavigationRequired();

    public boolean isAssignedTaskValid();

    /**
     * Gets the creature, or the enemy Dungeon Heart, we are fighting. Picks a
     * new target if we don't have a valid one.
     *
     * @return the target to attack, or {@code null} if there is none
     */
    public IEntityController getAttackTarget();

    public boolean isWithinAttackDistance(EntityId attackTarget);

    public void stopCreature();

    public void executeAttack(EntityId attackTarget);

    /**
     * Lets the shot of the spell being cast leave once the cast has reached
     * the spell's shot delay
     */
    public void releaseCastShot();

    /**
     * Drops the shot of an interrupted cast
     */
    public void cancelCastShot();

    public void navigateToAttackTarget(EntityId attackTarget);

    public ICreatureController getFollowTarget();

    public Task getAssignedTask();

    public float getDistanceToCreature(EntityId target);

    public void navigateToRandomPointAroundTarget(EntityId target, int radius);

    public void resetFollowTarget();

    public void flee();

    public boolean isAttacked();

    public boolean isEnoughSleep();

    public boolean isIncapacitated();

    /**
     * Is the creature fighting in a Combat Pit
     *
     * @return true if dropped into a Combat Pit to fight
     */
    public boolean isCombatPitFighter();

    public boolean isTimeToReEvaluate();

    public void resetReEvaluationTimer();

    public int getGold();

    public int getMaxGold();

    public void substractGold(int amount);

    public Point getLairLocation();

    public boolean isUnconscious();

    public Point getCreatureCoordinates();

    public void setAssignedTask(Task task);

    /**
     * The assigned task moved on to its next step, with a new type and
     * location. The creature keeps the task and heads for the new location.
     *
     * @param task the assigned task
     */
    public void updateAssignedTask(Task task);

    public void executeAssignedTask();

    public Creature getCreature();

    /**
     * Tells that something happened to the creature type, e.g. it claimed a tile
     *
     * @param type the event
     * @param location the tile where it happened
     */
    public void fireEvent(GameEventType type, Point location);

    public void addGold(int amount);

    public int getObjectiveTargetActionPointId();

    public void setObjectiveTargetActionPointId(int actionPointId);

    public Thing.HeroParty.Objective getObjective();

    public void setObjective(Thing.HeroParty.Objective objective);

    public boolean isDead();

    public boolean isImprisoned();

    public boolean isTortured();

    /**
     * Is the creature possessed, controlled by the player from the inside
     *
     * @return true if possessed
     */
    public boolean isPossessed();

    public boolean isStunned();

    /**
     * Is the creature afraid, i.e. so scared of the enemies that it is running
     * away
     *
     * @return {@code true} if afraid
     */
    public boolean isAfraid();

    /**
     * Make the creature run or walk, the speed comes from the creature's run
     * or walk speed attribute
     *
     * @param running {@code true} to run, {@code false} to walk
     */
    public void setRunning(boolean running);

    public int getLevel();

    public void attachPortalGem();

    public void setObjectiveTargetPlayerId(short playerId);

    public short getObjectiveTargetPlayerId();

    public void setPlayerObjective(ObjectiveType objective);

    public void setCreatureLair(EntityId lairId);

    /**
     * Evaluates the time spent in current state and compares it to the
     * creatures target time in a state. The target time would be set by either
     * animation or level variable
     *
     * @return {@code true} if state should be changed
     */
    public boolean isStateTimeExceeded();

    public void sleep();

    /**
     * Knocks the creature off its feet, it falls back and gets up again like
     * after being dropped from the hand. Whatever it was doing is dropped. A
     * creature already down stays down longer. Does nothing to a creature
     * that is not on its own feet to begin with, i.e. in hand, falling,
     * possessed or out cold.
     */
    public void knockDown();

    /**
     * Set a target for us to follow
     *
     * @param target target to follow
     */
    public void setFollowTarget(EntityId target);

    /**
     * Gets the possessed creature that has hypnotised us, we are on its side
     * and follow it
     *
     * @return the creature, {@code null} if we are not hypnotised
     */
    public EntityId getHypnotiser();

    public boolean shouldNavigateToFollowTarget();

    public boolean isSlapped();

    public boolean isPortalGemInPosession();

    /**
     * Mark the creature as one that has stolen something
     */
    public void markStolen();

    /**
     * Has the creature stolen something
     *
     * @return true if it has
     */
    public boolean hasStolen();

    /**
     * When a creature is hauled to prison, call this to properly seal the enemy
     * to the prison
     *
     * @param playerId the imprisoner player
     * @param roomId the room in which to imprison
     */
    public void imprison(short playerId, EntityId roomId);

    /**
     * Is the (neutral) creature claimed
     *
     * @return returns {@code true} if the creature is owned by a keeper
     */
    public boolean isClaimed();

    public boolean isHungry();

    public boolean goToEat();

    /**
     * Makes the creature eat the target
     *
     * @param target the devouree
     */
    public void eat(IEntityController target);

    /**
     * Marks that we have eaten a single ration of food
     */
    public void sate();

    /**
     * Sends the creature to collect its jackpot, if it is the big winner of a
     * Casino
     *
     * @return true if the creature was assigned to collect its winnings
     */
    public boolean collectWinnings();

    /**
     * Sends the creature to dance in its keeper's Casino, if one is in disco
     * mode
     *
     * @return true if the creature is in the disco or on its way there
     */
    public boolean goToDisco();

    /**
     * Is the creature standing in its keeper's Casino while it is in disco mode
     *
     * @return true if the creature is at the disco
     */
    public boolean isAtDisco();

    /**
     * Makes the creature hungry
     */
    public void makeHungry();

    /**
     * Get the research points the creature makes on each game tick in the
     * library, its research per second turned into points per tick the way
     * DK II does it
     *
     * @return research points per tick
     */
    public int getResearchPerTick();

    /**
     * Get the research points the creature makes once a second on top of its
     * research per tick while its keeper tortures one of his own creatures of
     * the same type
     *
     * @return the research points, 0 if no such creature is tortured
     */
    public int getTorturedTypeResearchBonus();

    /**
     * Get research per second attribute
     *
     * @return research per second
     */
    public int getResearchPerSecond();

    /**
     * Get manufacture per second attribute
     *
     * @return manufacture per second
     */
    public int getManufacturePerSecond();

    /**
     * Gives object to creature
     *
     * @param object the object, might be food, gold, or what ever
     */
    public void giveObject(IObjectController object);

    public void stopRecuperating();

    /**
     * Is the creature recuperating safely in its own bed
     *
     * @return true if recuperating
     */
    public boolean isRecuperating();

    /**
     * Turns the creature into a chicken for a while, as by the Chicken spell.
     * Creatures immune to the spell stay as they are
     *
     * @param duration how long, in seconds
     */
    public void turnIntoChicken(double duration);

    /**
     * Is the creature a chicken, by the Chicken spell
     *
     * @return true if the creature is a chicken
     */
    public boolean isChicken();

    /**
     * Sets creature possession on/off
     *
     * @param possessed should the creature be posessed or AI driven
     */
    public void setPossession(boolean possessed);

    /**
     * Sets the movement intent for a possessed creature, as steered by its keeper
     *
     * @param direction movement direction on the map plane, may be a zero vector
     * @param rotation the wanted rotation
     * @param pitch the pitch of the view, positive looking up, in radians
     * @param speedMode one of {@link toniarts.openkeeper.game.component.PossessedMovement}'s speed constants
     */
    public void setPossessedMovement(Vector2f direction, float rotation, float pitch, byte speedMode);

    /**
     * Sets the attack intent for a possessed creature, as given by its keeper.
     * The swings happen on the game logic thread, see
     * {@link #executePossessedAttack()}.
     *
     * @param attacking whether the attack button is held down
     * @param spell 0 for the melee attack, 1 to 3 for the creature's spells
     */
    public void setPossessedAttack(boolean attacking, int spell);

    /**
     * The keeper wants the creature the possessed creature looks at to join
     * its group. It joins on the game logic thread, see
     * {@link #executePossessedAttack()}.
     */
    public void addPossessedFollower();

    /**
     * Swings the melee attack of a possessed creature, if it has recharged.
     * Hits the nearest creature in front of it on the tiles around it, or if
     * there is none, a worker digs the tile it is facing. When the keeper asked
     * for a follower instead, our own creature it looks at joins its group. Or
     * casts the chosen spell, if the creature has it at its level and it has
     * recharged.
     */
    public void executePossessedAttack();

    /**
     * Does the creature follow a creature its keeper possesses
     *
     * @return true if the creature is in the group of a possessed creature
     */
    public boolean isInPossessedGroup();

    /**
     * Uses a first person ability of a possessed creature, as its keeper
     * presses the ability's key or uses the selected ability. The Vampire
     * turns into a bat or back, the Monk and the Fairy start or stop praying,
     * the Bile Demon picks up or throws a worker, the Vampire hypnotises a
     * creature or lets it go, the Maiden of the Nest frightens what she would
     * hit. A sniper turns sniper mode on or off with its own key.
     *
     * @param ability 1 or 2, the creature's first or second ability, or
     * {@link #POSSESSED_SNIPER_MODE_KEY}
     */
    public void usePossessedAbility(int ability);

    /**
     * The key of sniper mode, as the ability to use. It has a key of its own
     * rather than one of the two ability keys.
     */
    public static final int POSSESSED_SNIPER_MODE_KEY = 3;

    /**
     * Converts creature to another player's bidding
     *
     * @param playerId the new player ID
     */
    public void convertCreature(short playerId);

    /**
     * Makes the creature fight for another player for a while, as by the
     * Turncoat spell. It stays owned by its keeper. Creatures immune to the
     * spell stay as they are
     *
     * @param playerId the player the creature fights for
     * @param duration how long, in seconds
     */
    public void turncoat(short playerId, double duration);

}
