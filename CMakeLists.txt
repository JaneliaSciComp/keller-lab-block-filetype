project(klb)
cmake_minimum_required(VERSION 3.0)

include(cmake/git-versioning.cmake)

set_property(GLOBAL PROPERTY USE_FOLDERS TRUE)

set(CMAKE_POSITION_INDEPENDENT_CODE ON)
set(CMAKE_C_STANDARD   99)
set(CMAKE_CXX_STANDARD 11)

add_subdirectory(src)

option(build_tests "Build tests" OFF)
if (build_tests)
    enable_testing()
    add_subdirectory(test)
endif()

include(InstallRequiredSystemLibraries)
include(CPack)
