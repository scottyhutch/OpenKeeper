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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tells how many times bigger an image is than the converted original it
 * replaces. HD copies of the converted textures (tools/hd-textures) have the
 * same asset names as the originals and an integer multiple of their size.
 * The interface lays images out by their size in pixels, so it should use the
 * original's size for them.<br>
 * The original's size is read from its PNG header and cached. An image with no
 * original, or not an exact integer multiple of it, has the scale 1.
 *
 * @author OpenKeeper
 */
public final class ImageScales {

    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n'};
    private static final int IHDR = 0x49484452;

    private final Path originalsFolder;
    private final Map<String, Optional<ImageSize>> originalSizes = new ConcurrentHashMap<>();

    /**
     * The size of an image in pixels
     *
     * @param width the width
     * @param height the height
     */
    public record ImageSize(int width, int height) {

    }

    /**
     * Creates the scales for images that override the originals in a folder
     *
     * @param originalsFolder the folder of the converted originals, the asset
     * paths are relative to it
     */
    public ImageScales(Path originalsFolder) {
        this.originalsFolder = originalsFolder;
    }

    /**
     * Gets how many times bigger an image is than its original
     *
     * @param assetPath the asset path of the image
     * @param width the width of the image that was loaded
     * @param height the height of the image that was loaded
     * @return the scale, 1 if the image has no original or isn't an exact
     * integer multiple of it
     */
    public int getScale(String assetPath, int width, int height) {
        ImageSize original = getOriginalSize(assetPath);
        if (original == null) {
            return 1;
        }
        return getScale(original, width, height);
    }

    /**
     * Gets the size of the converted original of an image
     *
     * @param assetPath the asset path of the image
     * @return the size of the original, or null if there is no original PNG
     * or no path
     */
    public ImageSize getOriginalSize(String assetPath) {

        // Some Nifty effects create their image before its file name is set
        if (assetPath == null) {
            return null;
        }
        return originalSizes.computeIfAbsent(assetPath, this::readOriginalSize).orElse(null);
    }

    private Optional<ImageSize> readOriginalSize(String assetPath) {
        Path original;
        try {
            original = originalsFolder.resolve(assetPath);
        } catch (InvalidPathException e) {
            return Optional.empty();
        }
        if (!Files.isRegularFile(original)) {
            return Optional.empty();
        }
        try (InputStream in = Files.newInputStream(original)) {
            return Optional.ofNullable(readPngSize(in));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    /**
     * Gets how many times bigger an image is than its original. Both sides
     * must be scaled by the same integer.
     *
     * @param original the size of the original
     * @param width the width of the image
     * @param height the height of the image
     * @return the scale, 1 if the image isn't an exact integer multiple of the
     * original
     */
    public static int getScale(ImageSize original, int width, int height) {
        if (original.width() <= 0 || original.height() <= 0
                || width % original.width() != 0 || height % original.height() != 0) {
            return 1;
        }
        int scale = width / original.width();
        if (scale < 1 || height / original.height() != scale) {
            return 1;
        }
        return scale;
    }

    /**
     * Reads the size of a PNG image from its header
     *
     * @param in the PNG data, read only up to the end of the header
     * @return the size, or null if the data isn't a PNG
     * @throws IOException if reading fails
     */
    public static ImageSize readPngSize(InputStream in) throws IOException {
        // The IHDR chunk always comes first: length, type, width, height
        byte[] header = in.readNBytes(PNG_SIGNATURE.length + 16);
        if (header.length < PNG_SIGNATURE.length + 16
                || !Arrays.equals(header, 0, PNG_SIGNATURE.length, PNG_SIGNATURE, 0, PNG_SIGNATURE.length)) {
            return null;
        }
        ByteBuffer chunk = ByteBuffer.wrap(header, PNG_SIGNATURE.length, 16);
        chunk.getInt();
        if (chunk.getInt() != IHDR) {
            return null;
        }
        int width = chunk.getInt();
        int height = chunk.getInt();
        if (width <= 0 || height <= 0) {
            return null;
        }
        return new ImageSize(width, height);
    }

}
