% readKLBslice Read spatial slice (XY, XZ, or YZ) from Keller Lab Block file type (.klb). Since we stored in blocks, this is faster than reading the entire image
% Usage:   slice = readKLBslice(filename, slice, dim , numThreads) 
% 
% INPUT:
% 
% filename: string with the path to the klb file to be read.
% 
% slice:  integer scalar parameter to indicate which plane we want to read
% dim:    integer scalar parameter to indicate the plane type (dim = 1 -> XY plane; dim = 2 -> XZ plane; dim = 3 -> YZ plane)
% 
% 
% numThreads (optional): integer scalar parameter to decide how many cores to use. Speed scales linearly with the number of threads. If numThreads < -1 (default) then all the available cores are used
% 
% 
% OUTPUT:
% 
% slice:   Matlab 2-dimensional array with the slice content and the appropiate data type
    
function slice = readKLBslice(filename, slice, dim , numThreads) 