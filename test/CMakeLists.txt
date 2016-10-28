set(tgt write_float_bz2)
add_executable(${tgt} write_float_bz2.c)
target_link_libraries(${tgt} klb)
target_include_directories(${tgt} PUBLIC ${CMAKE_CURRENT_LIST_DIR}/../src)
add_test(test-${tgt} ${tgt})

set(tgt write_float_zip)
add_executable(${tgt} write_float_zip.c)
target_link_libraries(${tgt} klb)
target_include_directories(${tgt} PUBLIC ${CMAKE_CURRENT_LIST_DIR}/../src)
add_test(test-${tgt} ${tgt})

#executable to test klblib library for I/O purposes
set(tgt test_klbIO)
add_executable(${tgt} mainTest_klbIO.cxx)
target_link_libraries (${tgt} klb)
add_test(test-${tgt} ${tgt})

#executable to test klblib library for I/O purposes using C-style code
set(tgt test_klbIO_C)
add_executable(${tgt} mainTest_klbIO.c)
target_link_libraries (${tgt} klb)
add_test(test-${tgt} ${tgt})

#executable to test klblib library for speed and size in real data
set(tgt speed_klbIO)
add_executable(${tgt} mainTestSpeed_klbIO.cxx)
target_link_libraries (${tgt} klb)
add_test(test-${tgt} ${tgt})

#executable to test klblib library for large files
set(tgt large_files_klbIO)
add_executable(${tgt} mainTest_largeFiles_klbIO.cxx)
target_link_libraries (${tgt} klb)
add_test(test-${tgt} ${tgt})

#small executable to test C++11 std::threads and compare it to pbzip2
set(tgt testPbzip2_Cpp11Threads)
add_executable(${tgt} testPbzip2_Cpp11Threads.cxx)
target_link_libraries (${tgt} bzip2 zlib)
add_test(test-${tgt} ${tgt})
