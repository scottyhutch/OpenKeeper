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

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import toniarts.openkeeper.gui.nifty.render.ImageScales.ImageSize;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ImageScalesTest {

    private static final String ICON = "Textures/GUI/Icons/icon.png";
    private static final ImageSize ORIGINAL = new ImageSize(16, 8);

    @TempDir
    Path originals;

    @Test
    void readsTheSizeFromThePngHeader() throws IOException {
        assertEquals(new ImageSize(37, 11), ImageScales.readPngSize(new ByteArrayInputStream(png(37, 11))));
    }

    @Test
    void readsNoSizeFromOtherData() throws IOException {
        byte[] gif = "GIF89a\u0010\u0000\u0008\u0000 and some more bytes".getBytes(StandardCharsets.ISO_8859_1);

        assertNull(ImageScales.readPngSize(new ByteArrayInputStream(gif)));
    }

    @Test
    void readsNoSizeFromATruncatedPng() throws IOException {
        byte[] truncated = Arrays.copyOf(png(37, 11), 20);

        assertNull(ImageScales.readPngSize(new ByteArrayInputStream(truncated)));
    }

    @Test
    void scaleIsTheIntegerMultiple() {
        assertEquals(4, ImageScales.getScale(ORIGINAL, 64, 32));
        assertEquals(2, ImageScales.getScale(ORIGINAL, 32, 16));
    }

    @Test
    void scaleIsOneForTheOriginalSize() {
        assertEquals(1, ImageScales.getScale(ORIGINAL, 16, 8));
    }

    @Test
    void scaleIsOneWhenNotAnIntegerMultiple() {
        assertEquals(1, ImageScales.getScale(ORIGINAL, 24, 12));
        assertEquals(1, ImageScales.getScale(ORIGINAL, 65, 32));
    }

    @Test
    void scaleIsOneWhenTheSidesAreScaledDifferently() {
        assertEquals(1, ImageScales.getScale(ORIGINAL, 64, 16));
    }

    @Test
    void scaleIsOneForASmallerImage() {
        assertEquals(1, ImageScales.getScale(ORIGINAL, 8, 4));
        assertEquals(1, ImageScales.getScale(ORIGINAL, 0, 0));
    }

    @Test
    void comparesWithTheConvertedOriginal() throws IOException {
        writeOriginal(ICON, png(16, 8));
        ImageScales imageScales = new ImageScales(originals);

        assertEquals(ORIGINAL, imageScales.getOriginalSize(ICON));
        assertEquals(4, imageScales.getScale(ICON, 64, 32));
        assertEquals(1, imageScales.getScale(ICON, 16, 8));
        assertEquals(1, imageScales.getScale(ICON, 60, 30));
    }

    @Test
    void scaleIsOneWithoutAnOriginal() {
        ImageScales imageScales = new ImageScales(originals);

        assertNull(imageScales.getOriginalSize(ICON));
        assertEquals(1, imageScales.getScale(ICON, 64, 32));
    }

    @Test
    void scaleIsOneWhenTheOriginalIsNotAPng() throws IOException {
        writeOriginal(ICON, "not a PNG".getBytes(StandardCharsets.US_ASCII));
        ImageScales imageScales = new ImageScales(originals);

        assertNull(imageScales.getOriginalSize(ICON));
        assertEquals(1, imageScales.getScale(ICON, 64, 32));
    }

    @Test
    void scaleIsOneForAPathThatIsNoFile() {
        ImageScales imageScales = new ImageScales(originals);

        assertEquals(1, imageScales.getScale("HUDBackground", 64, 32));
        assertEquals(1, imageScales.getScale("Textures/GUI", 64, 32));
    }

    @Test
    void readsEachOriginalOnce() throws IOException {
        Path original = writeOriginal(ICON, png(16, 8));
        ImageScales imageScales = new ImageScales(originals);
        imageScales.getOriginalSize(ICON);
        Files.delete(original);

        assertEquals(4, imageScales.getScale(ICON, 64, 32));
    }

    private Path writeOriginal(String assetPath, byte[] data) throws IOException {
        Path file = originals.resolve(assetPath);
        Files.createDirectories(file.getParent());
        return Files.write(file, data);
    }

    static byte[] png(int width, int height) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB), "png", out);
        return out.toByteArray();
    }

}
