% readKLBstack Read entire image stack from Keller Lab Block file type (.klb)
% Usage:    [im, header] = readKLBstack(filename, numThreads)
% 
% INPUT:
% 
% filename: string with the path to the klb file to be read.
% 
% numThreads (optional): integer scalar parameter to decide how many cores to use. Speed scales linearly with the number of threads. If numThreads < -1 (default) then all the available cores are used
% 
% 
% OUTPUT:
% 
% im:   Matlab N-dimensional array with the stack content and the appropiate data type
% 
% header:  Struct with header file parameters (the same as readKLBheader.m)
    
function [im, header] = readKLBstack(filename, numThreads)