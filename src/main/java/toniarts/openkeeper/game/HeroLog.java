/*
 * Copyright (C) 2026 OpenKeeper
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
package toniarts.openkeeper.game;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DEBUG ONLY (my-build): one-line hero timeline events, grep for HEROLOG.
 * State here is deliberately static, it is only for dedupe of log lines.
 */
public final class HeroLog {

    private static final Logger logger = System.getLogger("HeroLog");
    private static final Map<String, String> LAST = new ConcurrentHashMap<>();
    private static final Map<String, Double> LAST_TIME = new ConcurrentHashMap<>();

    /**
     * Game time in seconds, set by the trigger tree every update
     */
    public static volatile double time;

    private HeroLog() {
    }

    public static void log(String message) {
        logger.log(Level.INFO, String.format("HEROLOG t=%.1f %s", time, message));
    }

    public static String state(String key) {
        return LAST.get(key);
    }

    public static void setState(String key, String value) {
        LAST.put(key, value);
    }

    /**
     * Log only when the value for the key differs from the last logged one
     */
    public static void logChange(String key, String value, String message) {
        if (!value.equals(LAST.put(key, value))) {
            log(message);
        }
    }

    /**
     * Log at most once per the given game seconds for the key
     */
    public static void logThrottled(String key, double seconds, String message) {
        Double last = LAST_TIME.get(key);
        if (last == null || time - last >= seconds || time < last) {
            LAST_TIME.put(key, time);
            log(message);
        }
    }
}
