/*
 * Copyright (C) 2014-2025 OpenKeeper
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
package toniarts.openkeeper.game.state.loop;

import com.simsilica.es.EntityData;
import toniarts.openkeeper.game.controller.GameController;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.WorkshopOrders;
import toniarts.openkeeper.game.controller.trap.TrapEffects;
import toniarts.openkeeper.game.controller.room.FiveByFiveRotatedController;
import toniarts.openkeeper.game.data.CampaignLevel;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.fogofwar.IBlocksSight;
import toniarts.openkeeper.game.logic.*;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.state.session.GameSessionServerService;
import toniarts.openkeeper.game.state.session.SaveGame;
import toniarts.openkeeper.game.task.ITaskManager;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.GameLoop;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import javax.annotation.Nullable;

/**
 *
 * @author ArchDemon
 */
public final class GameLoopManager {

    private static final Logger logger = System.getLogger(GameLoopManager.class.getName());

    /**
     * How long stopping waits for the saves that were asked for, in seconds
     */
    private static final long SAVE_TIMEOUT = 30;

    private final List<GameLoop> loops = new ArrayList<>();

    // Saves that are asked for but not yet collected or written. The files are written on a thread
    // that is not a daemon, so the JVM doesn't exit in the middle of it.
    private final Set<Runnable> pendingCollects = ConcurrentHashMap.newKeySet();
    private final Set<CompletableFuture<Void>> pendingWrites = ConcurrentHashMap.newKeySet();
    private final ThreadPoolExecutor saveWriter = new ThreadPoolExecutor(1, 1, 5, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(), runnable -> new Thread(runnable, "Save game"));

    {
        // An idle writer thread must not keep the JVM alive
        saveWriter.allowCoreThreadTimeOut(true);
    }

    private final GameSessionServerService gameService;

    private final IGameController gameController;

    private final GameLogicManager gameLogicThread;

    private final PayDaySystem payDaySystem;

    /**
     * Single use game states
     *
     * @param level the level to load
     * @param gameService
     * @param players player participating in this game, can be {@code null}
     * @param campaign whether this is a campaign level
     */
    public GameLoopManager(IKwdFile level, GameSessionServerService gameService, List<Keeper> players, boolean campaign) {
        this(level, gameService, players, campaign, null);
    }

    /**
     * Loads a saved game
     *
     * @param level the level of the saved game
     * @param gameService the game service, its server entities are the saved
     * ones
     * @param saveGame the saved game
     */
    public GameLoopManager(IKwdFile level, GameSessionServerService gameService, SaveGame saveGame) {
        this(level, gameService, null, false, saveGame);
    }

    private GameLoopManager(IKwdFile level, GameSessionServerService gameService, List<Keeper> players, boolean campaign, SaveGame saveGame) {
        this.gameService = gameService;
        final EntityData entityData = gameService.getServerEntityData();
        final Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings = level.getVariables();

        gameController = saveGame != null ? new GameController(level, saveGame, entityData, gameSettings, gameService)
                : new GameController(level, players, campaign, entityData, gameSettings, gameService);
        final ILevelInfo levelInfo = gameController.getLevelInfo();
        final IGameWorldController gameWorldController = gameController.getGameWorldController();
        final IEntityPositionLookup positionSystem = gameController.getEntityLookupService();
        final Map<Short, IPlayerController> playerControllers = gameController.getPlayerControllers();
        final ITaskManager taskManager = gameController.getTaskManager();
        final GameEventService gameEventService = gameController.getGameEventService();

        payDaySystem = new PayDaySystem(entityData, levelInfo.getLevelData(), gameController.getGameTimer(),
                playerControllers.values(), gameSettings);

        // Game logic
        gameLogicThread = new GameLogicManager(
                gameWorldController.getMapController(),
                new DecaySystem(entityData),
                new JailBreakSystem(entityData, levelInfo, gameWorldController.getMapController()),
                new WoodenBridgeDecaySystem(entityData, gameController.getGameTimer(), gameWorldController,
                        level, gameWorldController.getMapController(),
                        gameController.getLevelVariable(Variable.MiscVariable.MiscType.WOOD_BRIDGE_LIFE_ON_LAVA_SECONDS)),
                new TremorSystem(entityData, gameController.getGameTimer(), gameWorldController.getMapController(),
                        (int) gameController.getLevelVariable(Variable.MiscVariable.MiscType.TREMOR_DAMAGE_PER_TERRAIN_TILE_PER_SECOND)),
                new WallDeteriorationSystem(gameWorldController.getMapController(), playerControllers.values(),
                        (int) gameController.getLevelVariable(Variable.MiscVariable.MiscType.NUMBER_OF_RANDOM_WALL_DETERIORATIONS_PER_SECOND),
                        (int) gameController.getLevelVariable(Variable.MiscVariable.MiscType.WALL_DETERIORATION_DAMAGE)),
                new CreatureExperienceSystem(entityData, levelInfo.getLevelData(), gameSettings,
                        gameWorldController.getCreaturesController()),
                new CreatureSleepSystem(entityData, levelInfo.getLevelData(),
                        gameController.getGameTimer()),
                new CreatureMoodSystem(entityData, levelInfo.getLevelData(), playerControllers.values(),
                        gameController.getGameTimer(), gameSettings),
                payDaySystem,
                new SlapSystem(entityData, levelInfo.getLevelData(), playerControllers.values(), gameSettings,
                        gameWorldController.getObjectsController()),
                new TurncoatSystem(entityData, gameController.getGameTimer()),
                new HealthSystem(entityData, positionSystem, gameSettings, gameWorldController.getCreaturesController(),
                        gameWorldController.getRoomAttackMessages(),
                        levelInfo, playerControllers.values(), gameWorldController.getMapController(),
                        gameController.getGameTimer(), gameEventService),
                new InfernoSystem(entityData, positionSystem, levelInfo.getLevelData()),
                new CreatureTorturingSystem(entityData, gameWorldController.getCreaturesController(),
                        gameWorldController.getMapController(), playerControllers, gameEventService,
                        (int) gameController.getLevelVariable(Variable.MiscVariable.MiscType.TORTURE_CHANCE_OF_DYING_WHEN_CONVERTED)),
                new KeeperDefeatSystem(entityData, levelInfo, gameController, gameWorldController.getMapController(),
                        gameWorldController.getObjectsController(), players != null && !players.isEmpty()),
                new DeathSystem(entityData, gameSettings, positionSystem),
                new GraveyardSystem(entityData, levelInfo.getLevelData(), gameSettings,
                        gameWorldController.getMapController(), gameWorldController.getCreaturesController(), gameEventService),
                new PlayerCreatureSystem(entityData, levelInfo.getLevelData(), playerControllers.values()),
                new ManufactureProgressSystem(entityData, playerControllers.values(),
                        ManufactureProgressSystem.workshopOrders(new WorkshopOrders(entityData, levelInfo.getLevelData()))),
                new PlayerSpellbookSystem(entityData, levelInfo.getLevelData(), playerControllers.values()),
                new CreatureMoodSwitchSystem(entityData, levelInfo::isCreatureMoodsEnabled),
                (IGameLogicUpdatable) gameController,
                new CreatureSpawnSystem(entityData, gameWorldController.getCreaturesController(), playerControllers.values(),
                        gameSettings, levelInfo, gameWorldController.getMapController(), gameEventService),
                new ChickenSpawnSystem(entityData, gameWorldController.getObjectsController(),
                        playerControllers.values(), gameSettings, gameWorldController.getMapController()),
                new ManaCalculatorLogic(playerControllers.values(), entityData),
                new LowManaSystem(entityData, level, playerControllers.values(), gameSettings, gameService),
                new ManufacturePointLossSystem(entityData, playerControllers,
                        (int) gameController.getLevelVariable(Variable.MiscVariable.MiscType.MANUFACTURE_POINT_LOSS_WHILE_NO_WORKERS_PER_SECOND)),
                new PossessedSystem(playerControllers.values(), entityData, gameController,
                        gameWorldController.getCreaturesController()),
                new CreatureLeaveSystem(entityData, gameWorldController.getCreaturesController()),
                new SummonedReaperSystem(entityData, playerControllers.values(), gameWorldController.getCreaturesController(),
                        gameController.getGameTimer(), gameController.getNavigationService()),
                new PortalGemDropSystem(entityData, levelInfo.getLevelData(), gameWorldController.getObjectsController()),
                new CallToArmsSystem(entityData, level, playerControllers.values(),
                        gameWorldController.getCreaturesController(), gameController.getGameTimer(), gameEventService),
                new CreatureAiSystem(entityData, gameWorldController.getCreaturesController(), taskManager),
                new NewSpeciesSystem(entityData, gameService,
                        levelInfo.getLevelData().getGameLevel().getIntroductionOverrideTextIds()),
                new CasinoSystem(entityData, gameWorldController.getCreaturesController(),
                        gameWorldController.getMapController(), positionSystem, gameController.getGameTimer(), gameEventService, gameSettings),
                new ChickenAiSystem(entityData, gameWorldController.getObjectsController()),
                new SeparationSystem(entityData, level, gameWorldController.getMapController(), positionSystem),
                new TrainingDummySystem(entityData, levelInfo.getLevelData(), gameWorldController.getMapController()),
                new CreatureViewSystem(entityData, levelInfo.getLevelData()),
                new DoorSystem(entityData, level, gameWorldController.getMapController(), levelInfo),
                new DoorViewSystem(entityData, positionSystem),
                new BattleSystem(entityData, gameController.getGameTimer()),
                new LooseObjectSystem(entityData, gameWorldController.getMapController(), playerControllers, positionSystem),
                new TrapSystem(entityData, level, gameController.getGameTimer(),
                        solidTilesBlockSight(gameWorldController.getMapController()), playerControllers,
                        new TrapEffects(entityData, level, gameController.getGameTimer(), gameWorldController.getMapController(),
                                gameService, gameWorldController.getShotsController()),
                        gameController.getLevelVariable(Variable.MiscVariable.MiscType.TRIGGER_TRAP_TRIGGER_SPEED_TILES_PER_SECOND)),
                new FreezeSystem(entityData, gameWorldController.getCreaturesController(), gameController.getGameTimer()),
                new ShotSystem(entityData, level, gameController.getGameTimer(), gameWorldController.getShotsController(), positionSystem,
                        gameWorldController.getMapController(), playerControllers),
                new HaulingSystem(entityData),
                (IGameLogicUpdatable) taskManager);

        final long logicTickNanos = 1_000_000_000L / levelInfo.getLevelData().getGameLevel().getTicksPerSec();
        gameLogicThread.enableTickTiming("Game Logic", logicTickNanos);
        loops.add(new GameLoop(gameLogicThread, logicTickNanos, "Logic"));

        // Animation systems
        final GameLogicManager gameAnimationThread = new GameLogicManager(
                new DungeonHeartConstruction(
                        entityData,
                        gameController.getLevelVariable(Variable.MiscVariable.MiscType.TIME_BEFORE_DUNGEON_HEART_CONSTRUCTION_BEGINS),
                        levelInfo.getLevelData().getObject(FiveByFiveRotatedController.OBJECT_PLUG_ID).getMiscEffectId(),
                        levelInfo.getLevelData().getGameLevel().getTicksPerSec()),
                new CreatureFallSystem(entityData, gameController.getGameTimer(), gameEventService,
                        gameWorldController.getMapController(), levelInfo.getLevelData().getGameLevel().getTicksPerSec(),
                        gameController.getLevelVariable(Variable.MiscVariable.MiscType.GRAVITY_CONSTANT)));
        loops.add(new GameLoop(gameAnimationThread, GameLoop.INTERVAL_FPS_60, "Animation"));

        // Steering
        loops.add(new GameLoop(new GameLogicManager(new MovementSystem(entityData, level, gameWorldController.getMapController()),
                new PossessedMovementSystem(entityData, gameWorldController.getMapController(),
                        positionSystem, gameWorldController.getCreaturesController(), gameController.getGameTimer())), GameLoop.INTERVAL_FPS_60, "Steering"));

        // A loaded game goes on from the time it was saved at, the times in its components are from that clock
        if (saveGame != null) {
            gameLogicThread.setTimeElapsed(saveGame.getGameTime());
            gameAnimationThread.setTimeElapsed(saveGame.getGameTime());
        }
    }

    private static IBlocksSight solidTilesBlockSight(IMapController mapController) {
        return (x, y) -> {
            IMapTileInformation tile = mapController.getMapData().getTile(x, y);
            return tile == null || mapController.getTerrain(tile).getFlags().contains(Terrain.TerrainFlag.SOLID);
        };
    }

    public void pause() {
        loops.stream().forEach(GameLoop::pause);
        gameService.setGamePaused(true);
    }

    public void resume() {
        loops.stream().forEach(GameLoop::resume);
        gameService.setGamePaused(false);
    }

    public void start() {
        ThreadCheck.setRunning(true);
        loops.stream().forEach(GameLoop::start);
    }

    /**
     * Stops the game. A save that was asked for but hasn't been collected yet
     * is collected first, and the saves that are being written are waited for,
     * so a game saved just before quitting is not lost.
     */
    public void stop() {
        ThreadCheck.setRunning(false);
        finishSaves();

        // The game logic last: stopping it tears down what the animation and steering loops still use
        loops.reversed().forEach(GameLoop::stop);
        loops.clear();
        saveWriter.shutdown();
    }

    private void finishSaves() {
        // The loops are only held between frames here, the logic loop may not run its queue any more
        pendingCollects.forEach(Runnable::run);
        try {
            CompletableFuture.allOf(pendingWrites.toArray(CompletableFuture[]::new)).get(SAVE_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException ex) {
            logger.log(Level.ERROR, "Failed to finish saving the game!", ex);
        }
    }

    /**
     * Saves the game. The game is collected at the start of the next logic
     * tick, queued like the player actions, so no player action runs in the
     * middle of it. The animation and steering loops are held between frames
     * meanwhile. The file is then written in the background. This doesn't
     * wait for the save, so it can be called from the render thread.
     *
     * @param file the save file
     * @param campaignLevel the campaign level, or {@code null} for
     * non-campaign
     * @param client the client's own part of the save, see
     * {@link SaveGame#setClient(Object)}, or {@code null} for none
     * @return completes when the file is written
     */
    public CompletableFuture<Void> save(Path file, @Nullable CampaignLevel campaignLevel, @Nullable Object client) {
        if (loops.isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalStateException("The game has stopped!"));
        }
        CompletableFuture<SaveGame> saveGame = new CompletableFuture<>();
        Runnable collect = new Runnable() {
            @Override
            public void run() {
                pendingCollects.remove(this);
                hold(0, () -> {
                    if (!saveGame.isDone()) {
                        long start = System.nanoTime();
                        try {
                            saveGame.complete(new SaveGame(
                                    gameController.getLevelInfo().getLevelData().getGameLevel().getName(),
                                    campaignLevel,
                                    gameController.getGameTimer().getGameTime(),
                                    gameController.getLevelInfo(),
                                    gameController.getPlayerControllers().values(),
                                    gameController.getTriggerControls(),
                                    gameController.getGameWorldController().getCreaturesController().getCreatedParties(),
                                    gameController.getGameWorldController().getMapController().getRoomControllers(),
                                    gameService.getServerEntityData()));
                        } catch (RuntimeException ex) {
                            saveGame.completeExceptionally(ex);
                        }
                        logger.log(Level.INFO, "Collected the save in {0} ms, the game was held for that long", (System.nanoTime() - start) / 1_000_000);
                    }
                    return null;
                });
            }
        };
        pendingCollects.add(collect);
        gameLogicThread.enqueueAction(collect);

        // A paused logic loop doesn't run the queue, but then no player action runs either
        if (loops.get(0).isPaused()) {
            collect.run();
        }

        CompletableFuture<Void> write = saveGame.thenAcceptAsync(save -> {
            try {
                if (client != null) {
                    save.setClient(client);
                }
                save.write(file);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }, saveWriter);
        pendingWrites.add(write);
        write.whenComplete((result, ex) -> pendingWrites.remove(write));
        return write;
    }

    private <T> T hold(int loop, Supplier<T> action) {
        if (loop == loops.size()) {
            return action.get();
        }
        return loops.get(loop).hold(() -> hold(loop + 1, action));
    }

    public IGameController getGameController() {
        return gameController;
    }

    /**
     * Queues a player action to run at the start of the next game logic tick.
     * Player actions come from the render thread (or the network thread) and
     * change the same state as the game logic does
     *
     * @param action the player action
     */
    public void runPlayerAction(Runnable action) {
        gameLogicThread.enqueueAction(action);
    }

    public PayDaySystem getPayDaySystem() {
        return payDaySystem;
    }

}
