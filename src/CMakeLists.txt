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

#executable to test klblib library for I/O purposes
ADD_EXECUTABLE(test_klbIO mainTest_klbIO.cxx)
target_link_libraries (test_klbIO klblib)

#executable to test klblib library for speed and size in real data
ADD_EXECUTABLE(speed_klbIO mainTestSpeed_klbIO.cxx)
target_link_libraries (speed_klbIO klblib)


#small executable to test C++11 std::threads and compare it to pbzip2
ADD_EXECUTABLE(testPbzip2_Cpp11Threads testPbzip2_Cpp11Threads.cxx)
target_link_libraries ( testPbzip2_Cpp11Threads bz2lib )


