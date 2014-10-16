/*
*  readKLBslice.cpp
*
*  Created on: October 13, 2014
*      Author: Fernando Amat
*

*     Copyright (C) 2014 by  Fernando Amat
*     See licenseGMEM.txt for full license and copyright notice.
*     
*     \brief Set of utilities to read/write KLB format
*       header = readKLBslice(filename, slice, dim , numThreads)     
*/


#if defined(_WIN32) || defined(_WIN64)
#define NOMINMAX
#endif


#include "mex.h"

#include <cstdint>
#include <string.h>
#include <algorithm>

#include "klb_imageIO.h"

using namespace std;

void mexFunction (int nlhs, mxArray *plhs[], int nrhs,const mxArray *prhs[])
{
    
    //variables with default values
    char *filename;    
	int numThreads = -1;//by default we use all the threads available
    
	// check: only one input and one output argument
	if (nrhs < 3)
		mexErrMsgTxt("Must have three input arguments: filename, slice, dimension");
	if (nlhs !=1)
	    mexErrMsgTxt("Must have one output argument");
	if (mxIsChar(prhs[0]) != 1)
		mexErrMsgTxt("Input 1 must be a string");


	if (nrhs > 3)//set number of threads
	{
		if (mxIsEmpty(prhs[3]) == false)
		{
			numThreads = (int)(mxGetPr(prhs[3])[0]);
		}
	}

	//initialize I/O object
	filename = mxArrayToString(prhs[0]);
	string filenameOut(filename);
	
	//read other inputs
	int slice = ((int)(mxGetPr(prhs[1])[0])) - 1;//C-indexing
	int dimSlice = ((int)(mxGetPr(prhs[2])[0])) - 1;//C-indexing

	if (dimSlice >= 3 || dimSlice < 0)
		mexErrMsgTxt("Code only ready for slices in X, Y or Z. So dimSlice has to be 1, 2 or 3");
	
	//read header
	klb_imageIO imgFull(filenameOut);
	int error = imgFull.readHeader();
	if (error > 0)
		mexErrMsgTxt("Error reading the image");

	//define region of interest
	klb_ROI ROIfull;
	ROIfull.defineSlice(slice, dimSlice, imgFull.header.xyzct);

	//set dimensionality of the slice
	mwSize ndims = 2;
	mwSize dims[2];

	int count = 0;
	for (int ii = 0; ii < 3; ii++)
	{
		if (ii == dimSlice)
			continue;
		dims[count++] = imgFull.header.xyzct[ii];
	}
	
		


	
		

	//allocate space for image on the output array according to datat type
	switch (imgFull.header.dataType)
	{
	case 0:
		plhs[0] = mxCreateNumericArray(ndims, dims, mxUINT8_CLASS, mxREAL);
		break;

	case 1:
		plhs[0] = mxCreateNumericArray(ndims, dims, mxUINT16_CLASS, mxREAL);
		break;

	case 2:
		plhs[0] = mxCreateNumericArray(ndims, dims, mxUINT32_CLASS, mxREAL);
		break;

	case 3:		
		plhs[0] = mxCreateNumericArray(ndims, dims, mxUINT64_CLASS, mxREAL);
		break;

	case 4:
		plhs[0] = mxCreateNumericArray(ndims, dims, mxINT8_CLASS, mxREAL);
		break;

	case 5:
		plhs[0] = mxCreateNumericArray(ndims, dims, mxINT16_CLASS, mxREAL);
		break;

	case 6:
		plhs[0] = mxCreateNumericArray(ndims, dims, mxINT32_CLASS, mxREAL);
		break;

	case 7:
		plhs[0] = mxCreateNumericArray(ndims, dims, mxINT64_CLASS, mxREAL);
		break;

	case 8:
		plhs[0] = mxCreateNumericArray(ndims, dims, mxSINGLE_CLASS, mxREAL);
		break;

	case 9:
		plhs[0] = mxCreateNumericArray(ndims, dims, mxDOUBLE_CLASS, mxREAL);
		break;

	default:
		mexErrMsgTxt("Data type not supported");
	}

	//read image		
	error = imgFull.readImage((char*)(mxGetData(plhs[0])), &ROIfull, numThreads);
	if (error > 0)
		mexErrMsgTxt("Error reading the image slice");

	//release memory	
    mxFree(filename);
};