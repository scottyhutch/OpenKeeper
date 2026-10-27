# realesrgan_jni, the native side of toniarts.openkeeper.tools.hdtextures.RealEsrgan
#
# Only realesrgan_jni.cpp is OpenKeeper's. The upscaler itself is realesrgan.cpp/.h and its
# shaders from Real-ESRGAN-ncnn-vulkan (MIT, https://github.com/xinntao/Real-ESRGAN-ncnn-vulkan),
# fetched at configure time, and it runs on Tencent's ncnn (BSD-3-Clause,
# https://github.com/Tencent/ncnn), linked from ncnn's prebuilt release package. Neither is
# copied into this repository.
#
# Windows, from a "x64 Native Tools Command Prompt for VS 2022", in the repository root:
#   cmake -S src/main/native/realesrgan_jni -B build/realesrgan_jni -G Ninja -DCMAKE_BUILD_TYPE=Release
#         -Dncnn_DIR=<ncnn-YYYYMMDD-windows-vs2022>/x64/lib/cmake/ncnn -DGLSLANG_EXECUTABLE=<glslang>/bin/glslang.exe
#   cmake --build build/realesrgan_jni
# glslang (https://github.com/KhronosGroup/glslang/releases) compiles the shaders to SPIR-V.
# A local clone of Real-ESRGAN-ncnn-vulkan can be used with -DFETCHCONTENT_SOURCE_DIR_REALESRGAN=<clone>.
cmake_minimum_required(VERSION 3.24)
project(realesrgan_jni CXX)

set(CMAKE_CXX_STANDARD 17)

find_package(JNI REQUIRED)
find_package(ncnn REQUIRED)
find_program(GLSLANG_EXECUTABLE NAMES glslang glslangValidator REQUIRED)

include(FetchContent)
FetchContent_Declare(realesrgan
    GIT_REPOSITORY https://github.com/xinntao/Real-ESRGAN-ncnn-vulkan.git
    GIT_TAG 37026f49824c5cf84062e7c6a5dd71445dcf610f # v0.2.0, 2022-04-24
    GIT_SUBMODULES "")
FetchContent_MakeAvailable(realesrgan)
set(REALESRGAN_SRC ${realesrgan_SOURCE_DIR}/src)

# realesrgan.cpp includes each shader as SPIR-V in hex, plain, with fp16 storage and with int8 storage
set(SHADERS)
foreach(SHADER realesrgan_preproc realesrgan_postproc realesrgan_preproc_tta realesrgan_postproc_tta)
    foreach(VARIANT "" _fp16s _int8s)
        set(DEFINES)
        if(VARIANT)
            list(APPEND DEFINES -DNCNN_fp16_storage=1)
        endif()
        if(VARIANT STREQUAL "_int8s")
            list(APPEND DEFINES -DNCNN_int8_storage=1)
        endif()
        set(OUTPUT ${CMAKE_CURRENT_BINARY_DIR}/${SHADER}${VARIANT}.spv.hex.h)
        add_custom_command(OUTPUT ${OUTPUT}
            COMMAND ${GLSLANG_EXECUTABLE} ${DEFINES} -V -s -x -o ${OUTPUT} ${REALESRGAN_SRC}/${SHADER}.comp
            DEPENDS ${REALESRGAN_SRC}/${SHADER}.comp
            VERBATIM)
        list(APPEND SHADERS ${OUTPUT})
    endforeach()
endforeach()

add_library(realesrgan_jni SHARED realesrgan_jni.cpp ${REALESRGAN_SRC}/realesrgan.cpp ${SHADERS})
target_include_directories(realesrgan_jni PRIVATE ${REALESRGAN_SRC} ${CMAKE_CURRENT_BINARY_DIR} ${JNI_INCLUDE_DIRS})
target_link_libraries(realesrgan_jni PRIVATE ncnn)
