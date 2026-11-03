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

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Runs the game logic tasks, well, doesn't literally run them but wraps them up
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class GameLogicManager implements IGameLogicUpdatable {

    private static final Logger logger = System.getLogger(GameLogicManager.class.getName());

    protected final IGameLogicUpdatable[] updatables;
    private final BlockingQueue<Runnable> actions = new LinkedBlockingQueue<>();

    /**
     * Debug only: per updatable timing, enable with -Dok.tickTiming=true. Grep for TICKTIMING
     */
    private static final boolean TICK_TIMING = Boolean.getBoolean("ok.tickTiming");
    private static final float SUMMARY_INTERVAL = 60f;
    private String timingLabel;
    private long budgetNanos;
    private long[] tickNanos;
    private long[] sumNanos;
    private long[] maxNanos;
    private int timedTicks;
    private float windowSeconds;

    public GameLogicManager(IGameLogicUpdatable... updatables) {
        this.updatables = updatables;
    }

    @Override
    public void start() {
        for (IGameLogicUpdatable updatable : updatables) {
            updatable.start();
        }
    }

    /**
     * Debug only: times the updatables of this manager, if -Dok.tickTiming is on
     *
     * @param label name of the loop for the log lines
     * @param budgetNanos the time a tick may take
     */
    public void enableTickTiming(String label, long budgetNanos) {
        if (TICK_TIMING) {
            timingLabel = label;
            this.budgetNanos = budgetNanos;
            int n = updatables.length + 1;
            tickNanos = new long[n];
            sumNanos = new long[n];
            maxNanos = new long[n];
        }
    }

    @Override
    public void processTick(float tpf) {
        if (timingLabel != null) {
            processTimedTick(tpf);
            return;
        }
        runQueuedActions();

        // Update updatables
        for (IGameLogicUpdatable updatable : updatables) {
            try {
                updatable.processTick(tpf);
            } catch (Exception e) {
                logger.log(Level.ERROR, "Error in game logic tick on " + updatable.getClass() + "!", e);
            }
        }
    }

    private void processTimedTick(float tpf) {
        int last = updatables.length;
        long tickStart = System.nanoTime();
        long t = tickStart;
        runQueuedActions();
        long now = System.nanoTime();
        tickNanos[last] = now - t;
        t = now;
        for (int i = 0; i < last; i++) {
            try {
                updatables[i].processTick(tpf);
            } catch (Exception e) {
                logger.log(Level.ERROR, "Error in game logic tick on " + updatables[i].getClass() + "!", e);
            }
            now = System.nanoTime();
            tickNanos[i] = now - t;
            t = now;
        }
        long total = t - tickStart;
        for (int i = 0; i <= last; i++) {
            sumNanos[i] += tickNanos[i];
            maxNanos[i] = Math.max(maxNanos[i], tickNanos[i]);
        }
        timedTicks++;
        windowSeconds += tpf;
        if (total > budgetNanos) {
            Integer[] order = timingOrder(tickNanos);
            StringBuilder sb = new StringBuilder("TICKTIMING [").append(timingLabel).append("] tick=")
                    .append(total / 1_000_000).append(" ms (budget ").append(budgetNanos / 1_000_000).append(")");
            for (int i = 0; i < Math.min(6, order.length); i++) {
                sb.append(' ').append(timingName(order[i])).append('=').append(tickNanos[order[i]] / 1_000_000).append("ms");
            }
            logger.log(Level.WARNING, sb.toString());
        }
        if (windowSeconds >= SUMMARY_INTERVAL) {
            Integer[] order = timingOrder(sumNanos);
            StringBuilder sb = new StringBuilder("TICKTIMING summary [").append(timingLabel).append("] ticks=")
                    .append(timedTicks).append(" window=").append((int) windowSeconds).append("s (avg/max ms, by total)");
            for (int i : order) {
                if (sumNanos[i] > 0) {
                    sb.append("\n    ").append(timingName(i)).append(" avg=")
                            .append(String.format("%.2f", sumNanos[i] / 1e6 / timedTicks))
                            .append(" max=").append(String.format("%.1f", maxNanos[i] / 1e6));
                }
            }
            logger.log(Level.WARNING, sb.toString());
            Arrays.fill(sumNanos, 0);
            Arrays.fill(maxNanos, 0);
            timedTicks = 0;
            windowSeconds = 0;
        }
    }

    private String timingName(int index) {
        return index == updatables.length ? "QueuedActions" : updatables[index].getClass().getSimpleName();
    }

    private static Integer[] timingOrder(long[] values) {
        Integer[] order = new Integer[values.length];
        for (int i = 0; i < order.length; i++) {
            order[i] = i;
        }
        Arrays.sort(order, Comparator.comparingLong((Integer i) -> values[i]).reversed());
        return order;
    }

    /**
     * Queues an action that changes the game state from another thread, such
     * as a player action from the render thread. It runs at the start of the
     * next tick, on the logic thread. Never blocks
     *
     * @param action the action to run
     */
    public void enqueueAction(Runnable action) {
        actions.add(action);
    }

    private void runQueuedActions() {

        // Drain once, actions queued while these run wait for the next tick
        List<Runnable> queued = new ArrayList<>();
        actions.drainTo(queued);
        for (Runnable action : queued) {
            try {
                action.run();
            } catch (Exception e) {
                logger.log(Level.ERROR, "Error in queued game action!", e);
            }
        }
    }

    @Override
    public void stop() {
        for (IGameLogicUpdatable updatable : updatables) {
            updatable.stop();
        }
    }

//    private void drawCreatureVisibilities() {
//        Node node = new Node("Visibilities");
//        float elevation = 0.1f;
//        for (CreatureControl creature : worldState.getThingLoader().getCreatures()) {
//            ColorRGBA creatureColor = creatureDebugColors.get(creature);
//            if (creatureColor == null) {
//                creatureColor = ColorRGBA.randomColor();
//                creatureDebugColors.put(creature, creatureColor);
//            }
//            if (!creature.getVisibleCreatures().isEmpty()) {
//                elevation += 0.02;
//                Box box = new Box(0.1f, 0.1f, 0.1f);
//                Geometry geometry = new Geometry("Box", box);
//                Material orange = new Material(worldState.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
//                orange.setColor("Color", new ColorRGBA(creatureColor.r, creatureColor.g, creatureColor.b, 0.4f));
//                orange.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
//                orange.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
//                geometry.setCullHint(Spatial.CullHint.Never);
//                geometry.setMaterial(orange);
//                geometry.setLocalTranslation(creature.getSpatial().getWorldTranslation());
//                node.attachChild(geometry);
//                for (CreatureControl visibleCreature : creature.getVisibleCreatures()) {
//                    if (!visibleCreature.equals(creature)) {
//                        Line line = new Line(creature.getSpatial().getWorldTranslation(), visibleCreature.getSpatial().getWorldTranslation());
//
//                        orange = new Material(worldState.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
//                        orange.setColor("Color", creatureColor);
//                        orange.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
//                        orange.getAdditionalRenderState().setLineWidth(2);
//
//                        geometry = new Geometry("Bullet", line);
//                        geometry.setCullHint(Spatial.CullHint.Never);
//                        geometry.setMaterial(orange);
//                        geometry.move(0, elevation, 0);
//                        node.attachChild(geometry);
//                    }
//                }
//            }
//        }
//        app.enqueue(() -> {
//            worldState.getWorld().detachChildNamed("Visibilities");
//            worldState.getWorld().attachChild(node);
//        });
//    }
}
