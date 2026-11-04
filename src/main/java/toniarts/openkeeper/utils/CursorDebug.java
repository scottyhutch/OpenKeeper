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
package toniarts.openkeeper.utils;

import com.jme3.input.InputManager;
import com.jme3.input.MouseInput;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Debug only: logs every change of the mouse cursor visibility, with the
 * thread and the caller. Enable with -Dok.cursorDebug=true. Grep for
 * CURSORDEBUG. A change made off the render thread is logged as a warning:
 * GLFW only captures and releases the cursor on the window's thread, so such a
 * change can leave the cursor captured.
 */
public final class CursorDebug {

    public static final boolean ENABLED = Boolean.getBoolean("ok.cursorDebug");

    private static final Logger logger = Logger.getLogger(CursorDebug.class.getName());

    private CursorDebug() {
    }

    /**
     * Wraps the mouse input of the input manager so that cursor visibility
     * changes get logged. Call on the render thread. Does nothing unless
     * enabled.
     *
     * @param inputManager the input manager
     */
    public static void install(InputManager inputManager) {
        if (!ENABLED) {
            return;
        }
        Thread renderThread = Thread.currentThread();
        try {
            Field field = InputManager.class.getDeclaredField("mouse");
            field.setAccessible(true);
            MouseInput mouse = (MouseInput) field.get(inputManager);
            Object logging = Proxy.newProxyInstance(MouseInput.class.getClassLoader(), new Class<?>[]{MouseInput.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("setCursorVisible")) {
                            log((Boolean) args[0], renderThread);
                        }
                        try {
                            return method.invoke(mouse, args);
                        } catch (InvocationTargetException e) {
                            throw e.getCause();
                        }
                    });
            field.set(inputManager, logging);
            logger.info(() -> "CURSORDEBUG installed, render thread=" + renderThread.getName()
                    + ", cursor visible=" + inputManager.isCursorVisible());
        } catch (ReflectiveOperationException | RuntimeException e) {
            logger.log(Level.WARNING, "CURSORDEBUG could not be installed", e);
        }
    }

    private static void log(boolean visible, Thread renderThread) {
        Thread thread = Thread.currentThread();
        boolean onRenderThread = thread == renderThread;
        StackTraceElement[] st = new Throwable().getStackTrace();
        StringBuilder sb = new StringBuilder("CURSORDEBUG visible=").append(visible).append(" thread=")
                .append(thread.getName());
        if (!onRenderThread) {
            sb.append(" OFF THE RENDER THREAD");
        }

        // Start from the caller of InputManager.setCursorVisible
        int start = 1;
        for (int i = 0; i < st.length; i++) {
            if (st[i].getClassName().equals(InputManager.class.getName())) {
                start = i + 1;
                break;
            }
        }
        for (int i = start; i < Math.min(st.length, start + 12); i++) {
            sb.append("\n    at ").append(st[i]);
        }
        logger.log(onRenderThread ? Level.INFO : Level.WARNING, sb.toString());
    }
}
