set(SRCS
	crctable.c
	randtable.c
	compress.c
	decompress.c
	bzlib.c
	blocksort.c
    huffman.c)

set(tgt bzip2)
add_library(${tgt}  STATIC ${SRCS})
target_include_directories(${tgt} PUBLIC ${CMAKE_CURRENT_LIST_DIR})

set(tgt bzip2s)
add_library(${tgt} SHARED ${SRCS} libbz2.def)
target_include_directories(${tgt} PUBLIC ${CMAKE_CURRENT_LIST_DIR})

#add_executable(bzip2 bzip2.c ${SRCS})

#add_executable(bzip2recover bzip2recover.c)
#target_link_libraries(bzip2recover bz2)
