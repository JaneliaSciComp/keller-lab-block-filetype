#add_subdirectory(bzip2-1.0.6)
set(BZIPDIR bzip2-1.0.6)

set(BZIPSRCS
	${BZIPDIR}/crctable.c
	${BZIPDIR}/randtable.c
	${BZIPDIR}/compress.c
	${BZIPDIR}/decompress.c
	${BZIPDIR}/bzlib.c
	${BZIPDIR}/blocksort.c
    ${BZIPDIR}/huffman.c)

set(tgt bzip2)
add_library(${tgt}  STATIC ${BZIPSRCS})
target_include_directories(${tgt} PUBLIC "${CMAKE_CURRENT_LIST_DIR}/${BZIPDIR}")

set(tgt bzip2s)
add_library(${tgt} SHARED ${BZIPSRCS} "${BZIPDIR}/libbz2.def")
target_include_directories(${tgt} PUBLIC "${CMAKE_CURRENT_LIST_DIR}/${BZIPDIR}")

#add_executable(bzip2 bzip2.c ${BZIPSRCS})

#add_executable(bzip2recover bzip2recover.c)
#target_link_libraries(bzip2recover bz2)


set(SKIP_INSTALL_ALL       TRUE)
set(SKIP_INSTALL_EXPORT    TRUE)
set(SKIP_TESTS             TRUE)
add_subdirectory(zlib "${CMAKE_CURRENT_BINARY_DIR}/zlib_build")
set_target_properties(zlib PROPERTIES EXCLUDE_FROM_ALL TRUE EXCLUDE_FROM_DEFAULT_BUILD TRUE)

# organize things for the IDE
set_target_properties(
        bzip2
        bzip2s
        zlib
        zlibstatic
    PROPERTIES
        FOLDER external
)
