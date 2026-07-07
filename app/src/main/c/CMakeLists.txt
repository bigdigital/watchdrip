cmake_minimum_required(VERSION 3.4.1)

# Asegurar compatibilidad con tamaños de página de 16 KB (Android 15+)
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,-z,max-page-size=16384")

add_library(tiny-edhc
        SHARED
        ecdh_lib.c
        ecdh_lib_jni.c)
