rm -rf build
mkdir build
cd build

# Depending on our platform, shared libraries end with either .so or .dylib
if [[ $(uname) == 'Darwin' ]]; then
    DYLIB_EXT=dylib
    CC=clang
    CXX=clang++
    CXXFLAGS="-I${PREFIX}/include -stdlib=libc++ -std=c++11"
else
    DYLIB_EXT=so
    CXXFLAGS="-I${PREFIX}/include"

    # Don't specify these -- let conda-build do it.
    #CC=gcc
    #CXX=g++
fi

cmake .. \
    -DCMAKE_C_COMPILER=${CC} \
    -DCMAKE_CXX_COMPILER=${CXX} \
    -DCMAKE_CXX_FLAGS="${CXXFLAGS}" \
    -DCMAKE_INSTALL_PREFIX=${PREFIX} \
    -DCMAKE_PREFIX_PATH=${PREFIX} \
\
    -DUSE_BUNDLED_BZIP2=OFF \
    -DBZIP2_INCLUDE_DIR=${PREFIX}/include \
    -DBZIP2_LIBRARY_RELEASE=${PREFIX}/lib/libbz2.a \
\
    -DUSE_BUNDLED_ZLIB=OFF \
    -DZLIB_ROOT=${PREFIX} \
##

# Build
make -j${CPU_COUNT}

# Install libs, headers
make install
