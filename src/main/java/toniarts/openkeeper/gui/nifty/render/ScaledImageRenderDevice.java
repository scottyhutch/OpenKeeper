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

import de.lessvoid.nifty.render.batch.BatchRenderConfiguration;
import de.lessvoid.nifty.render.batch.BatchRenderDevice;
import de.lessvoid.nifty.render.batch.BatchRenderImage;
import de.lessvoid.nifty.render.batch.TextureAtlasGenerator;
import de.lessvoid.nifty.render.batch.spi.BatchRenderBackend;
import de.lessvoid.nifty.spi.render.RenderImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Nifty's batched render device, but images bigger than their converted
 * original by an integer multiple (HD textures) report the original's size.
 * Nifty then lays them out and draws them at the original size, with the
 * extra detail.<br>
 * Such an image gets a texture of its own instead of a place in the texture
 * atlas. Nifty works out the texture coordinates from the size the image
 * reports, which is right for a texture of its own but not for a place in the
 * atlas, and its own texture can be filtered smoothly.
 *
 * @author OpenKeeper
 */
public class ScaledImageRenderDevice extends BatchRenderDevice {

    /**
     * Accepts no image, so the images that get it are always uploaded as
     * textures of their own
     */
    private static final TextureAtlasGenerator NO_ATLAS = new TextureAtlasGenerator(1, 1, 0, 0f);

    private final BatchRenderBackend renderBackend;
    private final ImageScales imageScales;
    private final boolean cacheImages;
    private final Map<String, ScaledBatchRenderImage> scaledImages = new HashMap<>();

    /**
     * Creates the render device
     *
     * @param renderBackend the render backend
     * @param renderConfig the batch configuration
     * @param imageScales tells which images are bigger than their originals
     */
    public ScaledImageRenderDevice(BatchRenderBackend renderBackend, BatchRenderConfiguration renderConfig,
            ImageScales imageScales) {
        super(renderBackend, renderConfig);
        this.renderBackend = renderBackend;
        this.imageScales = imageScales;
        this.cacheImages = !renderConfig.disposeImagesBetweenScreens;
    }

    @Override
    public RenderImage createImage(String filename, boolean filterLinear) {
        ScaledBatchRenderImage cached = scaledImages.get(filename);
        if (cached != null) {
            return cached;
        }

        // Only an image with a converted original can be scaled
        if (imageScales.getOriginalSize(filename) != null) {
            BatchRenderBackend.Image image = renderBackend.loadImage(filename);
            if (image != null) {
                int scale = imageScales.getScale(filename, image.getWidth(), image.getHeight());
                if (scale > 1) {
                    ScaledBatchRenderImage scaledImage = new ScaledBatchRenderImage(image, filename, renderBackend,
                            scale, !cacheImages);
                    if (cacheImages) {
                        scaledImages.put(filename, scaledImage);
                    }
                    return scaledImage;
                }
            }
        }

        return super.createImage(filename, filterLinear);
    }

    /**
     * An image that reports its size divided by its scale, and is always
     * uploaded as a texture of its own
     */
    public static class ScaledBatchRenderImage extends BatchRenderImage {

        private final int scale;
        private final int width;
        private final int height;

        /**
         * Creates the image
         *
         * @param image the image as loaded
         * @param filename the asset path of the image
         * @param renderBackend the render backend
         * @param scale how many times bigger the image is than its original
         * @param shouldUnload whether the image is unloaded between screens
         */
        public ScaledBatchRenderImage(BatchRenderBackend.Image image, String filename,
                BatchRenderBackend renderBackend, int scale, boolean shouldUnload) {
            super(image, filename, renderBackend, NO_ATLAS, -1, shouldUnload);
            this.scale = scale;
            this.width = image.getWidth() / scale;
            this.height = image.getHeight() / scale;
        }

        /**
         * How many times bigger the image is than its original
         *
         * @return the scale
         */
        public int getScale() {
            return scale;
        }

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public int getHeight() {
            return height;
        }

    }

}
