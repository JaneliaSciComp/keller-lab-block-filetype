project(klb)
cmake_minimum_required(VERSION 3.12)
cmake_policy(VERSION 3.12)

include(cmake/git-versioning.cmake)

set_property(GLOBAL PROPERTY USE_FOLDERS TRUE)

set(CMAKE_POSITION_INDEPENDENT_CODE ON)
set(CMAKE_C_STANDARD   99)
set(CMAKE_CXX_STANDARD 11)

find_package(ZLIB REQUIRED)
if(ZLIB_FOUND)
    include_directories(${ZLIB_INCLUDE_DIR})
    message("Found ZLIB." + ${ZLIB_INCLUDE_DIR} + "  " + ${ZLIB_LIBRARY})
else()
    message("Not found and not installing ZLIB.")
endif()

add_subdirectory(src)

option(build_tests "Build tests" ON)
if (build_tests)
    enable_testing()
    add_subdirectory(test)
endif()

include(InstallRequiredSystemLibraries)
include(CPack)
