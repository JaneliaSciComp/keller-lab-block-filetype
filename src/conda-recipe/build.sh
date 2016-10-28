cd src/
mkdir build
cd build

if [[ `uname` == 'Darwin' ]]; then
    DYLIB_EXT=dylib
else
    DYLIB_EXT=so
fi

cmake .. \
    -DCMAKE_C_COMPILER=${PREFIX}/bin/gcc \
    -DCMAKE_CXX_COMPILER=${PREFIX}/bin/g++ \
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

# (The klb cmake doesn't have install commands)
# Library install
cp libklb.${DYLIB_EXT} ${PREFIX}/lib/

# Install all headers
mkdir -p ${PREFIX}/include/klb
cp ../*.h ${PREFIX}/include/klb/
