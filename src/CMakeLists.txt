add_subdirectory(external)

file(GLOB SRCS *.cpp)
file(GLOB HDRS *.h)

install(FILES ${HDRS} DESTINATION include)

set(tgt klb)
add_library(${tgt} STATIC ${SRCS} ${HDRS})
target_include_directories(${tgt} PUBLIC ${CMAKE_CURRENT_LIST_DIR})
target_link_libraries(${tgt} bzip2 zlibstatic)
install(TARGETS ${tgt} DESTINATION lib)

set(tgt klbs)
add_library(${tgt} SHARED ${SRCS} ${HDRS})
target_include_directories(${tgt} PUBLIC ${CMAKE_CURRENT_LIST_DIR})
target_link_libraries(${tgt} bzip2 zlibstatic)
install(TARGETS ${tgt} DESTINATION bin)

option(BINDINGS_JAVA "Build bindings for Java" off)
if(BINDINGS_JAVA)
    add_subdirectory(jni)
endif()
