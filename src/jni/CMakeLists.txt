find_package(JNI REQUIRED)

add_library(klb-jni SHARED org_janelia_simview_klb_KLBJNI.cpp)
target_include_directories(klb-jni PUBLIC ${JNI_INCLUDE_DIRS})
target_link_libraries (klb-jni klb)

# Install the JNI product to the correct resource path
# The paths are relative to the  CMAKE_PREFIX_PATH which should be set at
# config time.
if(APPLE)
    set(nativedir x86_64-max_os_x)
elseif(UNIX)
    set(nativedir amd64-linux)
elseif(WIN32)
    set(nativedir amd64_windows)
else()
    set(nativedir unknown)
    message(WARNING "Could not detect platform for installing the JNI interface.")
endif()

install(TARGETS ${tgt} DESTINATION
    javaWrapper/src/main/resources/native/${nativedir})
