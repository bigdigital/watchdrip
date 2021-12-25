cmake_minimum_required(VERSION 3.4.1)
add_library(tiny-edhc
        SHARED
        ecdh_lib.c
        ecdh_lib_jni.c)
