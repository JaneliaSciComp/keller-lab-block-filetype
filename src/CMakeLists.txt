# External used to live under src, but has been moved out

file(GLOB SRCS *.cpp)
file(GLOB HDRS *.h)

find_package(Threads REQUIRED)

set(tgt klb_static)
add_library(${tgt} STATIC ${SRCS} ${HDRS})
target_include_directories(${tgt} PUBLIC ${CMAKE_CURRENT_LIST_DIR})
if(ZLIB_FOUND)
    target_include_directories(${tgt} PRIVATE ${ZLIB_INCLUDE_DIR})
else()
    add_dependencies(${tgt} zlib zlibstatic)
    target_include_directories(${tgt} PRIVATE "${CMAKE_CURRENT_LIST_DIR}/../external/zlib")
    target_include_directories(${tgt} PRIVATE "${CMAKE_CURRENT_BINARY_DIR}/../external/zlib_build")
    set(ZLIB_LIBRARY zlibstatic)
endif()
target_link_libraries(${tgt} Threads::Threads bzip2 ${ZLIB_LIBRARY})

set(tgt klb)
if(${MSVC})
    # MSVC_VERSION is only defined, and thus the expresion only resolves, 
    # if MSVC is defined.
    if(${MSVC_VERSION} EQUAL 1700)
        add_definitions(-D_VARIADIC_MAX=10)
    endif()
endif()
add_library(${tgt} SHARED ${SRCS} ${HDRS})
target_include_directories(${tgt} PUBLIC ${CMAKE_CURRENT_LIST_DIR})
if(ZLIB_FOUND)
    target_include_directories(${tgt} PRIVATE ${ZLIB_INCLUDE_DIR})
else()
    add_dependencies(${tgt} zlib zlibstatic)
    target_include_directories(${tgt} PRIVATE "${CMAKE_CURRENT_LIST_DIR}/../external/zlib")
    target_include_directories(${tgt} PRIVATE "${CMAKE_CURRENT_BINARY_DIR}/../external/zlib_build")
    set(ZLIB_LIBRARY zlibstatic)
endif()
target_compile_definitions(${tgt} PRIVATE COMPILE_SHARED_LIBRARY)
target_link_libraries(${tgt} Threads::Threads bzip2 ${ZLIB_LIBRARY})

option(BINDINGS_JAVA "Build bindings for Java" OFF)
if(BINDINGS_JAVA)
    add_subdirectory(jni)
endif()

# Only setup install targets if klb is the project being built.
# Projects that use klb via add_subdirectory() probably don't
# want the install targets.  They can always set that up themselves.
if(${CMAKE_PROJECT_NAME} STREQUAL klb)
    install(FILES ${HDRS} DESTINATION include)
    install(TARGETS klb LIBRARY DESTINATION lib)
    install(TARGETS klb_static ARCHIVE DESTINATION lib)
else()
    message("Not installing klb libraries. CMAKE_PROJECT_NAME is ${CMAKE_PROJECT_NAME}")
endif()
