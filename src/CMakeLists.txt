project ( imageBlockAPI_project )
cmake_minimum_required ( VERSION 2.8 )


#add bzip2 library
include_directories ("${PROJECT_SOURCE_DIR}/external/bzip2-1.0.6")
add_subdirectory ("${PROJECT_SOURCE_DIR}/external/bzip2-1.0.6")
LINK_DIRECTORIES( "${CMAKE_CURRENT_BINARY_DIR}/external/bzip2-1.0.6" )


#small executable to test C++11 std::threads and compare it to pbzip2

ADD_EXECUTABLE(testPbzip2_Cpp11Threads testPbzip2_Cpp11Threads.cxx)
target_link_libraries ( testPbzip2_Cpp11Threads bz2lib )


