#KLB stands for Keller Lab Block image format
project ( imageBlockAPI_project )
cmake_minimum_required ( VERSION 2.8 )

#add bzip2 library
include_directories ("${PROJECT_SOURCE_DIR}/external/bzip2-1.0.6")
add_subdirectory ("${PROJECT_SOURCE_DIR}/external/bzip2-1.0.6")
LINK_DIRECTORIES( "${CMAKE_CURRENT_BINARY_DIR}/external/bzip2-1.0.6" )


#main sources and headers
file(GLOB KLB_SRCS ${PROJECT_SOURCE_DIR}/*.cpp)
file(GLOB KLB_HDRS ${PROJECT_SOURCE_DIR}/*.h)


#main library 
ADD_LIBRARY(klblib ${KLB_SRCS} ${KLB_HDRS})
target_link_libraries (klblib bz2lib )

#main shared library (DLL)
ADD_LIBRARY(klblibs SHARED ${KLB_SRCS} ${KLB_HDRS})
target_link_libraries (klblibs bz2lib )
set_property(TARGET klblibs PROPERTY COMPILE_DEFINITIONS COMPILE_SHARED_LIBRARY)
set_property(TARGET klblibs PROPERTY OUTPUT_NAME klb)


#executable to test klblib library for I/O purposes
ADD_EXECUTABLE(test_klbIO mainTest_klbIO.cxx)
target_link_libraries (test_klbIO klblib)

#executable to test klblib library for speed and size in real data
ADD_EXECUTABLE(speed_klbIO mainTestSpeed_klbIO.cxx)
target_link_libraries (speed_klbIO klblib)


IF( CMAKE_COMPILER_IS_GNUCC )
	set (CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11") #to allow C+11 features in older GCC compilers
ENDIF()

IF( "${CMAKE_CXX_COMPILER_ID}" STREQUAL "Clang" ) 
	set (CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -stdlib=libc++") #to allow C+11 features in older GCC compilers
ENDIF()

#small executable to test C++11 std::threads and compare it to pbzip2
ADD_EXECUTABLE(testPbzip2_Cpp11Threads testPbzip2_Cpp11Threads.cxx)
target_link_libraries ( testPbzip2_Cpp11Threads bz2lib )


option(BINDINGS_JAVA "Build bindings for Java" off)
if(BINDINGS_JAVA)
    find_package(SWIG REQUIRED)
    find_package(JNI REQUIRED)
    include(${SWIG_USE_FILE})

    set_source_files_properties(klb.i PROPERTIES CPLUSPLUS ON)
    set(CMAKE_SWIG_FLAGS -package org.janelia.simview.klb.jni)
	set(CMAKE_SWIG_OUTDIR ${PROJECT_BINARY_DIR}/java/org/janelia/simview/klb/jni)

    include_directories (${PROJECT_SOURCE_DIR} ${JNI_INCLUDE_DIRS})
    link_directories(${PROJECT_BINARY_DIR} ${JNI_LIBRARY_DIRS})
    swig_add_module(klb-jni java klb.i)
    swig_link_libraries(klb-jni klblibs)
endif()