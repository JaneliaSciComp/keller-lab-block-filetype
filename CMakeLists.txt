project(klb)
cmake_minimum_required(VERSION 3.0)

include(cmake/git-versioning.cmake)

set_property(GLOBAL PROPERTY USE_FOLDERS TRUE)

set(CMAKE_POSITION_INDEPENDENT_CODE ON)
set(CMAKE_C_STANDARD   99)
set(CMAKE_CXX_STANDARD 11)

enable_testing()
add_subdirectory(src)
add_subdirectory(test)

include(InstallRequiredSystemLibraries)
include(CPack)
