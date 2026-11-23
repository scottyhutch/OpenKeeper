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
package toniarts.openkeeper.game.data;

import com.jme3.input.KeyInput;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.animation.AnimationBackend;
import toniarts.openkeeper.animation.VertexTextureBackend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The default keys
 */
class SettingsTest {

    @Test
    void testQuickKeysAreDungeonKeeperKeysAndUsedOnce() {
        assertEquals(KeyInput.KEY_S, Settings.Setting.QUICK_SAVE.getDefaultValue());
        assertEquals(KeyInput.KEY_L, Settings.Setting.QUICK_LOAD.getDefaultValue());
        assertEquals(KeyInput.KEY_END, Settings.Setting.CAMERA_ZOOM_OUT.getDefaultValue());
        assertNull(Settings.Setting.CAMERA_ZOOM_OUT.getSpecialKey(), "DK II zooms with Home / End");

        for (Settings.Setting quickKey : new Settings.Setting[]{Settings.Setting.QUICK_SAVE, Settings.Setting.QUICK_LOAD}) {
            for (Settings.Setting setting : Settings.Setting.getSettings(Settings.SettingCategory.CONTROLS)) {
                assertFalse(setting != quickKey && setting.getDefaultValue().equals(quickKey.getDefaultValue())
                        && Objects.equals(setting.getSpecialKey(), quickKey.getSpecialKey()), setting + " has the keys of " + quickKey);
            }
        }
    }

    @Test
    void testAnimationTakesTheGpuWhenItCanAndSmoothingIsOff() {
        assertEquals(AnimationBackend.AUTO, AnimationBackend.parse(String.valueOf(Settings.Setting.ANIMATION_BACKEND.getDefaultValue())));
        assertFalse(VertexTextureBackend.parseSmoothing(String.valueOf(Settings.Setting.MESH_SMOOTHING.getDefaultValue())));
        assertEquals(3, Settings.Setting.MESH_SMOOTHING_LEVEL.getDefaultValue());
        assertEquals("PN", Settings.Setting.MESH_SMOOTHING_METHOD.getDefaultValue());

        // What the graphics options save
        for (AnimationBackend backend : AnimationBackend.values()) {
            assertEquals(backend, AnimationBackend.parse(backend.name()));
        }
        assertTrue(VertexTextureBackend.parseSmoothing("ON"));
        assertFalse(VertexTextureBackend.parseSmoothing("OFF"));
        assertEquals(AnimationBackend.CPU, AnimationBackend.parse("unknown"));
    }

}
