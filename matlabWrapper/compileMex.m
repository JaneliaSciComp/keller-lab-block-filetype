
%debugging
%mex -g -output writeKLBstack  writeKLBstack.cpp -I../src ../build/Debug/klblib.lib ../build/external/bzip2-1.0.6/Debug/bz2lib.lib -DWINDOWS

mex -O -output writeKLBstack  writeKLBstack.cpp -I../src ../build/Release/klblib.lib ../build/external/bzip2-1.0.6/Release/bz2lib.lib -DWINDOWS

mex -O -output readKLBheader  readKLBheader.cpp -I../src ../build/Release/klblib.lib ../build/external/bzip2-1.0.6/Release/bz2lib.lib -DWINDOWS

mex -O -output readKLBstack  readKLBstack.cpp -I../src ../build/Release/klblib.lib ../build/external/bzip2-1.0.6/Release/bz2lib.lib -DWINDOWS