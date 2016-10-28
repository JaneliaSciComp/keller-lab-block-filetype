add_subdirectory(bzip2-1.0.6)
add_subdirectory(zlib-1.2.8)

# organize things for the IDE
set_target_properties(
        bzip2
        bzip2s
        example
        minigzip
        zlib
        zlibstatic
    PROPERTIES
        FOLDER external
)
