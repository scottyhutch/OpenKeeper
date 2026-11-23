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
package toniarts.openkeeper.animation;

import java.util.Locale;

/**
 * Where creature vertex animation is evaluated. {@link #CPU} is the reference:
 * {@link PoseTrack} blends the key frames into the mesh's position buffer. {@link #VERTEX_TEXTURE} keeps the decoded key frames in a
 * float texture and blends them in the vertex shader. {@link #AUTO} takes the
 * vertex texture when the GPU supports it, and is the default.
 */
public enum AnimationBackend {

    AUTO("Auto"), CPU("CPU"), VERTEX_TEXTURE("GPU");

    private final String label;

    AnimationBackend(String label) {
        this.label = label;
    }

    /**
     * Reads a setting value, case-insensitively
     *
     * @param value the setting value, may be {@code null}
     * @return the backend, {@link #CPU} for a missing or unknown value
     */
    public static AnimationBackend parse(String value) {
        if (value == null) {
            return CPU;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return CPU;
        }
    }

    /**
     * @return the name shown in the graphics options
     */
    @Override
    public String toString() {
        return label;
    }
}
