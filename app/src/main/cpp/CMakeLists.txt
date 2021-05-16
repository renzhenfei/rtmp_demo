# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.10.2)

project("rtmp_demo")

include_directories(librtmp include)

add_subdirectory(librtmp)

link_directories(libs/armeabi-v7a)

add_library( # Sets the name of the library.
             native-lib

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             native-lib.cpp VideoChannel.cpp AudioChannel.cpp)


find_library( # Sets the name of the path variable.
              log-lib

              log )

target_link_libraries( # Specifies the target library.
                       native-lib
                        rtmp
                       ${log-lib}
                        x264)