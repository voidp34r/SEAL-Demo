# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

cmake_minimum_required(VERSION 3.4.1)

add_library(cryptoadapter
            SHARED
            src/main/cpp/cryptoadapter.cpp
            src/main/cpp/cryptocontext.cpp
            ../../../../external/common/base64.cpp)

include_directories(../../../../external/common
                    ../../../../external/seal/src)

add_library(seal STATIC "")
add_subdirectory(../../../../external/seal/src/seal build)

target_link_libraries(cryptoadapter
                      seal)