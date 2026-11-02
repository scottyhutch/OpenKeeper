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
package toniarts.openkeeper.game.state.loop;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Debug only: logs server code that runs off the game logic thread. Enable with
 * -Dok.threadCheck=true. Each distinct call site is logged once. Grep for
 * THREADCHECK.
 */
public final class ThreadCheck {

    public static final boolean ENABLED = Boolean.getBoolean("ok.threadCheck");

    private static final Logger logger = Logger.getLogger(ThreadCheck.class.getName());
    private static final Set<String> SEEN = ConcurrentHashMap.newKeySet();
    private static volatile boolean running;

    private ThreadCheck() {
    }

    public static void setRunning(boolean running) {
        ThreadCheck.running = running;
    }

    /**
     * Logic thread only
     */
    public static void logic(String what) {
        if (ENABLED && running) {
            check(what, false);
        }
    }

    /**
     * Logic, animation or steering thread
     */
    public static void loops(String what) {
        if (ENABLED && running) {
            check(what, true);
        }
    }

    private static void check(String what, boolean anyLoop) {
        String name = Thread.currentThread().getName();
        if (name.equals("Game Logic") || (anyLoop && (name.equals("Game Steering") || name.equals("Game Animation")))) {
            return;
        }
        StackTraceElement[] st = new Throwable().getStackTrace();
        // st[0] is check, st[1] logic/loops, st[2] the guarded method, st[3] its caller
        String site = what + "<-" + (st.length > 3 ? st[3] : "?");
        if (SEEN.add(site)) {
            logger.warning(() -> {
                StringBuilder sb = new StringBuilder("THREADCHECK thread=").append(name).append(" in ").append(what)
                        .append(" called from ").append(site);
                for (int i = 2; i < Math.min(st.length, 14); i++) {
                    sb.append("\n    at ").append(st[i]);
                }
                return sb.toString();
            });
        }
    }
}
