/*
 * Copyright (C) 2014-2015 OpenKeeper
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
package toniarts.openkeeper.audio.plugins;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoadException;
import com.jme3.audio.AudioKey;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class MP2LoaderTest {

    @Test
    void truncatedFileFailsTheLoad() {
        AudioKey key = new AudioKey("blank.mp2", false);
        AssetInfo info = new AssetInfo(null, key) {
            @Override
            public InputStream openStream() {
                return new ByteArrayInputStream(new byte[141]);
            }
        };

        assertThrows(AssetLoadException.class, () -> new MP2Loader().load(info));
    }
}
