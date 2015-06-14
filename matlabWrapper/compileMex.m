
%debugging
%mex -g -output writeKLBstack  writeKLBstack.cpp -I../src ../build/Debug/klblib.lib ../build/external/bzip2-1.0.6/Debug/bz2lib.lib -DWINDOWS

mex -O -output writeKLBstack  writeKLBstack.cpp -I../src ../build/Release/klblib.lib ../build/external/bzip2-1.0.6/Release/bz2lib.lib ../build/external/zlib-1.2.8/Release/zlibstatic.lib -DWINDOWS -D_FILE_OFFSET_BITS=64

mex -O -output readKLBheader  readKLBheader.cpp -I../src ../build/Release/klblib.lib ../build/external/bzip2-1.0.6/Release/bz2lib.lib ../build/external/zlib-1.2.8/Release/zlibstatic.lib -DWINDOWS -D_FILE_OFFSET_BITS=64

mex -O -output readKLBstack  readKLBstack.cpp -I../src ../build/Release/klblib.lib ../build/external/bzip2-1.0.6/Release/bz2lib.lib ../build/external/zlib-1.2.8/Release/zlibstatic.lib -DWINDOWS -D_FILE_OFFSET_BITS=64
 
mex -O -output readKLBslice  readKLBslice.cpp -I../src ../build/Release/klblib.lib ../build/external/bzip2-1.0.6/Release/bz2lib.lib ../build/external/zlib-1.2.8/Release/zlibstatic.lib -DWINDOWS -D_FILE_OFFSET_BITS=64


mex -O -output readKLBroi  readKLBroi.cpp -I../src ../build/Release/klblib.lib ../build/external/bzip2-1.0.6/Release/bz2lib.lib ../build/external/zlib-1.2.8/Release/zlibstatic.lib -DWINDOWS -D_FILE_OFFSET_BITS=64