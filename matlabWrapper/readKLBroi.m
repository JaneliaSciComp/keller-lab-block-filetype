% readKLBroi Read arbitrary region of interest (ROI) from Keller Lab Block file type (.klb). Since we stored in blocks, this is faster than reading the entire image
% Usage:   patch = readKLBroi(filename, ROI, numThreads) 
% 
% INPUT:
% 
% filename: string with the path to the klb file to be read.
% 
% ROI: 2 x KLB_DATA_DIMS array. ROI(1,:) contains lowerbounds of the region of interest; ROI(2,:) contains upper bounds
%                               For full image, ROI = [1 1 1 1 1; size(im)] Upper bound is included in ROI
% 
% 
% numThreads (optional): integer scalar parameter to decide how many cores to use. Speed scales linearly with the number of threads. If numThreads < -1 (default) then all the available cores are used
% 
% 
% OUTPUT:
% 
% patch:   Matlab (up to) 5-dimensional array with the roi content and the appropiate data type
    
function patch = readKLBroi(filename, ROI , numThreads) 