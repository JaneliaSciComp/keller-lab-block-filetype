
%debugging
% mex -g -output writeKLBstack  writeKLBstack.cpp -I../src ../build/Debug/klb_static.lib ../build/external/bzip2-1.0.6/Debug/bzip2.lib ../build/external/zlib-1.2.8/Debug/zlibstaticd.lib -DWINDOWS

mex -O -output writeKLBstack  writeKLBstack.cpp -I../src ../build/Release/klb_static.lib ../build/external/bzip2-1.0.6/Release/bzip2.lib ../build/external/zlib-1.2.8/Release/zlibstatic.lib -DWINDOWS -D_FILE_OFFSET_BITS=64

mex -O -output readKLBheader  readKLBheader.cpp -I../src ../build/Release/klb_static.lib ../build/external/bzip2-1.0.6/Release/bzip2.lib ../build/external/zlib-1.2.8/Release/zlibstatic.lib -DWINDOWS -D_FILE_OFFSET_BITS=64

mex -O -output readKLBstack  readKLBstack.cpp -I../src ../build/Release/klb_static.lib ../build/external/bzip2-1.0.6/Release/bzip2.lib ../build/external/zlib-1.2.8/Release/zlibstatic.lib -DWINDOWS -D_FILE_OFFSET_BITS=64
 
mex -O -output readKLBslice  readKLBslice.cpp -I../src ../build/Release/klb_static.lib ../build/external/bzip2-1.0.6/Release/bzip2.lib ../build/external/zlib-1.2.8/Release/zlibstatic.lib -DWINDOWS -D_FILE_OFFSET_BITS=64


mex -O -output readKLBroi  readKLBroi.cpp -I../src ../build/Release/klb_static.lib ../build/external/bzip2-1.0.6/Release/bzip2.lib ../build/external/zlib-1.2.8/Release/zlibstatic.lib -DWINDOWS -D_FILE_OFFSET_BITS=64