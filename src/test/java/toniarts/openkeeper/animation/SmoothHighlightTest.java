/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.animation;

import com.jme3.asset.DesktopAssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.Caps;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import java.util.EnumSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.utils.AssetUtils;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * The hover highlight must work, and survive switching mesh smoothing, on the
 * smoothing material just like on DkLighting/Vertex.
 */
class SmoothHighlightTest {

    private DesktopAssetManager assets;

    @BeforeEach
    void enableSmoothing() {
        assets = new DesktopAssetManager(true);
        VertexTextureBackend.setSupported(true);
        VertexTextureBackend.setActive(true);
        VertexTextureBackend.configureSmoothing(true, 2, "PN",
                EnumSet.of(Caps.TesselationShader, Caps.GLSL400), assets);
    }

    @AfterEach
    void restore() {
        VertexTextureBackend.setSmoothing(false);
        VertexTextureBackend.setActive(false);
        VertexTextureBackend.setSupported(false);
    }

    @Test
    void smoothMaterialTakesTheHighlightAndKeepsItWhenSmoothingToggles() {
        Geometry geometry = new Geometry("creature", new Box(1, 1, 1));
        geometry.setMaterial(new Material(assets, "MatDefs/DkLighting/Vertex.j3md"));
        Node root = new Node();
        Node child = new Node();
        root.attachChild(child);
        child.attachChild(geometry);

        VertexTextureBackend.useSmoothMaterial(geometry, true);
        assertEquals(VertexTextureBackend.SMOOTH_MATDEF, geometry.getMaterial().getMaterialDef().getAssetName());
        AssetUtils.setModelHighlight(root, ColorRGBA.White, true);
        ColorRGBA smoothOverlay = (ColorRGBA) geometry.getMaterial().getParam("TintOverlay").getValue();
        assertNotEquals(ColorRGBA.BlackNoAlpha, smoothOverlay);

        // Back to the original: it takes the highlight over
        VertexTextureBackend.useSmoothMaterial(geometry, false);
        assertEquals(smoothOverlay, geometry.getMaterial().getParam("TintOverlay").getValue());

        // Highlight ended while it is not smoothed, smoothing comes back on a kept material
        AssetUtils.setModelHighlight(root, ColorRGBA.White, false);
        VertexTextureBackend.useSmoothMaterial(geometry, true);
        assertEquals(ColorRGBA.BlackNoAlpha, geometry.getMaterial().getParam("TintOverlay").getValue());

        // And the other way round
        VertexTextureBackend.useSmoothMaterial(geometry, false);
        AssetUtils.setModelHighlight(root, ColorRGBA.White, true);
        VertexTextureBackend.useSmoothMaterial(geometry, true);
        assertEquals(smoothOverlay, geometry.getMaterial().getParam("TintOverlay").getValue());
    }
}
