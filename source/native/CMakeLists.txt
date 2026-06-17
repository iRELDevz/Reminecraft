cmake_minimum_required(VERSION 3.15)
project(reminecraft_native LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_package(JNI REQUIRED)

add_library(reminecraft_native SHARED
    src/compression_helper.cpp
)

target_include_directories(reminecraft_native PRIVATE
    include
    ${JNI_INCLUDE_DIRS}
)

# Standard DLL settings for Windows
if(WIN32)
    set_target_properties(reminecraft_native PROPERTIES
        PREFIX ""
        SUFFIX ".dll"
    )
    target_link_libraries(reminecraft_native PRIVATE Cabinet)
endif()
