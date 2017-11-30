# Sets the minimum version of CMake required to build the native
# library. You should either keep the default value or only pass a
# value of 3.4.0 or lower.

cmake_minimum_required(VERSION 3.4.1)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds it for you.
# Gradle automatically packages shared libraries with your APK.

include_directories($ENV{HOME}/external/lib/android/ffmpeg-3.2-android-19)
include_directories($ENV{HOME}/external/lib/libyuv-android-master/jni/include)

add_library( # Sets the name of the library.
             native-lib

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             # Associated headers in the same location as their source
             # file are automatically included.
             src/main/cpp/native-lib.cpp src/main/cpp/queue.c src/main/cpp/player.c src/main/cpp/decode.c src/main/cpp/convert.cpp)

# Searches for a specified prebuilt library and stores the path as a
# variable. Because system libraries are included in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

SET(COPY_TO_PATH ${CMAKE_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI})
ADD_CUSTOM_COMMAND(TARGET native-lib POST_BUILD
                   COMMAND ${CMAKE_COMMAND} -E copy
                        $<TARGET_FILE:native-lib>
                        ${COPY_TO_PATH}
                   COMMENT "Copying1 'test' library to '${COPY_TO_PATH}'"

)


message( "Copying ${CMAKE_LIBRARY_TARGET_DIRECTORY}")



find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )


add_library( ffmpeg
             SHARED
             IMPORTED )

set_property(TARGET ffmpeg PROPERTY IMPORTED_NO_SONAME 1)

set_target_properties( # Specifies the target library.
                       ffmpeg


                       # Specifies the parameter you want to define.
                       PROPERTIES IMPORTED_LOCATION

                       # Provides the path to the library you want to import.
                       #imported-lib/src/${ANDROID_ABI}/libimported-lib.so
                      ${CMAKE_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}/libffmpeg.so)



add_library( yuv
             SHARED
             IMPORTED )

set_property(TARGET yuv PROPERTY IMPORTED_NO_SONAME 1)

set_target_properties( # Specifies the target library.
                       yuv


                       # Specifies the parameter you want to define.
                       PROPERTIES IMPORTED_LOCATION

                       # Provides the path to the library you want to import.
                       #imported-lib/src/${ANDROID_ABI}/libimported-lib.so
                      ${CMAKE_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}/libyuv.so)


# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in the
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
                       native-lib



                       ffmpeg

                       android

                       yuv

                       # Links the target library to the log library
                       # included in the NDK.

                       ${log-lib} )

#message("hellows123 ${CMAKE_LIBRARY_OUTPUT_DIRECTORY}")

