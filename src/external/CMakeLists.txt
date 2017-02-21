add_subdirectory(bzip2-1.0.6)

set(SKIP_INSTALL_ALL       TRUE)
set(SKIP_INSTALL_EXPORT    TRUE)
set(SKIP_TESTS             TRUE)
add_subdirectory(zlib-1.2.8)

# organize things for the IDE
set_target_properties(
        bzip2
        bzip2s
        zlib
        zlibstatic
    PROPERTIES
        FOLDER external
)
