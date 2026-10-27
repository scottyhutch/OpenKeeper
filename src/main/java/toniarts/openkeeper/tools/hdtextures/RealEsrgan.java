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
package toniarts.openkeeper.tools.hdtextures;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Real-ESRGAN in this process, on the GPU with Vulkan: the native library
 * {@code realesrgan_jni} (see {@code src/main/native/realesrgan_jni}) runs
 * Real-ESRGAN-ncnn-vulkan's upscaler with the same 4x models as its program.
 * The library is loaded from the path in the {@value #LIBRARY_PROPERTY}
 * system property, or else from {@code java.library.path}.
 *
 * @author OpenKeeper
 */
public final class RealEsrgan implements AutoCloseable {

    /** The system property with the native library's path */
    public static final String LIBRARY_PROPERTY = "openkeeper.realesrgan.library";

    private long handle;

    static {
        String library = System.getProperty(LIBRARY_PROPERTY);
        if (library != null) {
            System.load(library);
        } else {
            System.loadLibrary("realesrgan_jni");
        }
    }

    /**
     * Loads a 4x model on a GPU
     *
     * @param paramPath the model's .param file
     * @param binPath the model's .bin file
     * @param gpuId the Vulkan GPU, -1 for the default one
     * @param tileSize the tile size, 0 to pick it by the GPU's memory like the
     * program does
     * @param fp16 store fp16 on the GPU like the program does, rather than fp32
     */
    public RealEsrgan(String paramPath, String binPath, int gpuId, int tileSize, boolean fp16) {
        if (!Files.isRegularFile(Path.of(paramPath)) || !Files.isRegularFile(Path.of(binPath))) {
            throw new IllegalArgumentException("No model in " + paramPath + " and " + binPath);
        }
        handle = nativeInit(paramPath, binPath, gpuId, tileSize, fp16);
    }

    /**
     * Upscales an image 4x
     *
     * @param pixels the image's RGB or RGBA bytes, row by row
     * @param width the image's width
     * @param height the image's height
     * @return the (4 * width) * (4 * height) upscaled pixels, with as many
     * channels
     */
    public byte[] upscale(byte[] pixels, int width, int height) {
        return nativeProcess(handle, pixels, width, height);
    }

    @Override
    public void close() {
        nativeFree(handle);
        handle = 0;
    }

    private static native long nativeInit(String paramPath, String binPath, int gpuId, int tileSize, boolean fp16);

    private static native byte[] nativeProcess(long handle, byte[] pixels, int width, int height);

    private static native void nativeFree(long handle);
}
