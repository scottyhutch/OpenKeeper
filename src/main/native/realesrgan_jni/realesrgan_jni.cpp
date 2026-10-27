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

// The native side of toniarts.openkeeper.tools.hdtextures.RealEsrgan: runs the RealESRGAN class
// of Real-ESRGAN-ncnn-vulkan (realesrgan.cpp, MIT) on ncnn (BSD-3-Clause) with Vulkan

#include <jni.h>

#include <string>
#include <utility>
#include <vector>

#include "gpu.h"
#include "realesrgan.h"

// RealESRGAN sets the fp16 options of its private ncnn::Net in its constructor and uses them in
// load(). An explicit template instantiation may name a private member, so this reaches them.
using NetMember = ncnn::Net RealESRGAN::*;
NetMember netMember();
template <NetMember Member>
struct NetAccess {
    friend NetMember netMember() { return Member; }
};
template struct NetAccess<&RealESRGAN::net>;

static void throwNew(JNIEnv* env, const char* className, const char* message) {
    env->ThrowNew(env->FindClass(className), message);
}

// RealESRGAN takes and gives BGR(A) on Windows, where Real-ESRGAN's program decodes with WIC
static void swapRedAndBlue(unsigned char* pixels, size_t length, int channels) {
#if _WIN32
    for (size_t i = 0; i < length; i += channels) {
        std::swap(pixels[i], pixels[i + 2]);
    }
#endif
}

#if _WIN32
static std::wstring path(JNIEnv* env, jstring string) {
    const jchar* chars = env->GetStringChars(string, nullptr);
    std::wstring result(reinterpret_cast<const wchar_t*>(chars), env->GetStringLength(string));
    env->ReleaseStringChars(string, chars);
    return result;
}
#else
static std::string path(JNIEnv* env, jstring string) {
    const char* chars = env->GetStringUTFChars(string, nullptr);
    std::string result(chars);
    env->ReleaseStringUTFChars(string, chars);
    return result;
}
#endif

// The Vulkan instance lives while a RealEsrgan is open: the JVM never unloads the library, and
// leaving the instance to ncnn's static destructor crashes the JVM on exit. RealEsrgan objects
// are opened and closed on one thread.
static int openCount = 0;

extern "C" JNIEXPORT jlong JNICALL Java_toniarts_openkeeper_tools_hdtextures_RealEsrgan_nativeInit(JNIEnv* env, jclass,
        jstring paramPath, jstring binPath, jint gpuId, jint tileSize, jboolean fp16) {
    if (openCount++ == 0) {
        ncnn::create_gpu_instance();
    }
    int gpu = gpuId < 0 ? ncnn::get_default_gpu_index() : gpuId;
    if (gpu >= ncnn::get_gpu_count()) {
        if (--openCount == 0) {
            ncnn::destroy_gpu_instance();
        }
        throwNew(env, "java/lang/IllegalStateException", "No such Vulkan GPU");
        return 0;
    }
    RealESRGAN* realEsrgan = new RealESRGAN(gpu);
    // Real-ESRGAN's program always stores fp16 (when the GPU can) and never computes in it
    ncnn::Option& options = (realEsrgan->*netMember()).opt;
    options.use_fp16_packed = fp16;
    options.use_fp16_storage = fp16;
    options.use_fp16_arithmetic = false;
    realEsrgan->load(path(env, paramPath), path(env, binPath));
    realEsrgan->scale = 4;
    realEsrgan->prepadding = 10;
    // The program's choice for the 4x models, by the GPU memory
    uint32_t heapBudget = ncnn::get_gpu_device(gpu)->get_heap_budget();
    realEsrgan->tilesize = tileSize > 0 ? tileSize
            : heapBudget > 1900 ? 200 : heapBudget > 550 ? 100 : heapBudget > 190 ? 64 : 32;
    return reinterpret_cast<jlong>(realEsrgan);
}

extern "C" JNIEXPORT jbyteArray JNICALL Java_toniarts_openkeeper_tools_hdtextures_RealEsrgan_nativeProcess(JNIEnv* env,
        jclass, jlong handle, jbyteArray pixels, jint width, jint height) {
    const RealESRGAN* realEsrgan = reinterpret_cast<const RealESRGAN*>(handle);
    jsize length = env->GetArrayLength(pixels);
    int channels = width > 0 && height > 0 ? length / (width * height) : 0;
    if (realEsrgan == nullptr || (channels != 3 && channels != 4) || length != width * height * channels) {
        throwNew(env, "java/lang/IllegalArgumentException", "Expected an open RealEsrgan and RGB or RGBA pixels");
        return nullptr;
    }
    std::vector<unsigned char> in(length);
    env->GetByteArrayRegion(pixels, 0, length, reinterpret_cast<jbyte*>(in.data()));
    swapRedAndBlue(in.data(), in.size(), channels);

    ncnn::Mat inImage(width, height, in.data(), (size_t) channels, channels);
    ncnn::Mat outImage(width * realEsrgan->scale, height * realEsrgan->scale, (size_t) channels, channels);
    if (realEsrgan->process(inImage, outImage) != 0) {
        throwNew(env, "java/lang/IllegalStateException", "Real-ESRGAN failed");
        return nullptr;
    }
    unsigned char* out = static_cast<unsigned char*>(outImage.data);
    jsize outLength = outImage.w * outImage.h * channels;
    swapRedAndBlue(out, outLength, channels);
    jbyteArray result = env->NewByteArray(outLength);
    if (result != nullptr) {
        env->SetByteArrayRegion(result, 0, outLength, reinterpret_cast<const jbyte*>(out));
    }
    return result;
}

extern "C" JNIEXPORT void JNICALL Java_toniarts_openkeeper_tools_hdtextures_RealEsrgan_nativeFree(JNIEnv*, jclass,
        jlong handle) {
    if (handle != 0) {
        delete reinterpret_cast<RealESRGAN*>(handle);
        if (--openCount == 0) {
            ncnn::destroy_gpu_instance();
        }
    }
}
