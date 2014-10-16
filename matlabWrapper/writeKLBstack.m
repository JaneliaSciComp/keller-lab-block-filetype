% writeKLBstack save Matlab array (up to 5-dimensional) into  Keller Lab Block file type (.klb) format. 
% Usage:   writeKLBstack(im, filename, numThreads, pixelSize, blockSize, compressionType) 
% 
% INPUT (all the optional parameters can be set to [] to use its default value):
% 
% 
% im:     Matlab array to be saved. It can be up to 5-dimensional of any class and any size.
% filename: string with the path to the klb file to be written
% 
% 
% 
% numThreads (optional): integer scalar parameter to decide how many cores to use. Speed scales linearly with the number of threads. If numThreads < -1 (default) then all the available cores are used
% 
% pixelSize (optional):   float precision array of length 5 to store pixel size (in um,au,secs) along each dimension. Default value is -1.0 to indicate N/A
% 
% blockSize (optional): integer array of length <= 5 to define the block size along each dimension to partition the data. Default value is 256x256x15x1x1. Blocks should be approximately between 1MB-4MB since it is a good trade off between speed and compression ratio.
% 
% compressionType (optional): integer scalar to define compression used to store each block. Right now, 0 = none, 1 = bzip2 (default). You can add your own using the source code of the klb library;
% 
    
function  writeKLBstack(im, filename, numThreads, pixelSize, blockSize, compressionType) 