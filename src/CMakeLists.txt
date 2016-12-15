add_subdirectory(external)

file(GLOB SRCS *.cpp)
file(GLOB HDRS *.h)

set(tgt klb_static)
add_library(${tgt} STATIC ${SRCS} ${HDRS})
target_include_directories(${tgt} PUBLIC ${CMAKE_CURRENT_LIST_DIR})
target_link_libraries(${tgt} bzip2 zlibstatic)

set(tgt klb)
add_library(${tgt} SHARED ${SRCS} ${HDRS})
target_include_directories(${tgt} PUBLIC ${CMAKE_CURRENT_LIST_DIR})
target_link_libraries(${tgt} bzip2 zlibstatic)

find_package(JNI)
option(BINDINGS_JAVA "Build bindings for Java" ${JNI_FOUND})
if(BINDINGS_JAVA)
    add_subdirectory(jni)
endif()

# Only setup install targets if klb is the project being built.
# Projects that use klb via add_subdirectory() probably don't
# want the install targets.  They can always set that up themselves.
if(${PROJECT_NAME} STREQUAL klb)
    install(FILES ${HDRS} DESTINATION include)
    install(TARGETS klb klb_static DESTINATION lib)
else()
    message(${PROJECT_NAME})
endif()
