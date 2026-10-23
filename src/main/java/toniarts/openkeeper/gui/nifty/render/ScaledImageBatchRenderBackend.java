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
package toniarts.openkeeper.gui.nifty.render;

import com.jme3.niftygui.JmeBatchRenderBackend;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * jME's batch render backend, but the images that are bigger than their
 * converted original (see {@link ScaledImageRenderDevice}) get smoothly
 * filtered, mipmapped textures. They are drawn smaller than they are, and the
 * backend's own textures are filtered with the nearest pixel, which would
 * leave them grainy.
 *
 * @author OpenKeeper
 */
public class ScaledImageBatchRenderBackend extends JmeBatchRenderBackend {

    private static final Logger logger = System.getLogger(ScaledImageBatchRenderBackend.class.getName());

    /**
     * jME keeps the textures it creates for Nifty to itself
     */
    private static final Field TEXTURES_FIELD = getTexturesField();

    private final ImageScales imageScales;
    private final Set<Image> scaledImages = Collections.newSetFromMap(new WeakHashMap<>());

    /**
     * Creates the render backend
     *
     * @param display the Nifty display
     * @param imageScales tells which images are bigger than their originals
     */
    public ScaledImageBatchRenderBackend(NiftyJmeDisplay display, ImageScales imageScales) {
        super(display);
        this.imageScales = imageScales;
    }

    @Override
    public Image loadImage(String filename) {
        Image image = super.loadImage(filename);
        if (image != null && imageScales.getScale(filename, image.getWidth(), image.getHeight()) > 1) {
            scaledImages.add(image);
        }
        return image;
    }

    @Override
    public int createNonAtlasTexture(Image image) {
        int textureId = super.createNonAtlasTexture(image);
        if (scaledImages.contains(image)) {
            Texture2D texture = getTexture(textureId);
            if (texture != null) {
                texture.setMinFilter(Texture.MinFilter.Trilinear);
                texture.setMagFilter(Texture.MagFilter.Bilinear);
            }
        }
        return textureId;
    }

    @SuppressWarnings("unchecked")
    private Texture2D getTexture(int textureId) {
        if (TEXTURES_FIELD == null) {
            return null;
        }
        try {
            return ((Map<Integer, Texture2D>) TEXTURES_FIELD.get(this)).get(textureId);
        } catch (IllegalAccessException | ClassCastException e) {
            logger.log(Level.WARNING, "Can't filter the texture of an HD interface image!", e);
            return null;
        }
    }

    private static Field getTexturesField() {
        try {
            Field field = JmeBatchRenderBackend.class.getDeclaredField("textures");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException | RuntimeException e) {
            logger.log(Level.WARNING, "HD interface images will be drawn without smooth filtering!", e);
            return null;
        }
    }

}
