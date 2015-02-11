/*
*  readKLBstack.cpp
*
*  Created on: October 13, 2014
*      Author: Fernando Amat
*

*     Copyright (C) 2014 by  Fernando Amat
*     See license.txt for full license and copyright notice.
*     
*     \brief Set of utilities to read/write KLB format
*       im = readKLBstack(filename, numThreads)     
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
	if (nrhs < 1)
		mexErrMsgTxt("Must have one input argument: filename");
	if (nlhs > 2)
	    mexErrMsgTxt("Must have less than two output arguments");
	if (mxIsChar(prhs[0]) != 1)
		mexErrMsgTxt("Input 1 must be a string");


	if (nrhs > 1)//set number of threads
	{
		if (mxIsEmpty(prhs[1]) == false)
		{
			numThreads = (int)(mxGetPr(prhs[1])[0]);
		}
	}

	//initialize I/O object
	filename = mxArrayToString(prhs[0]);
	string filenameOut(filename);
	

	//read header
	klb_imageIO imgFull(filenameOut);
	int error = imgFull.readHeader();
	if (error > 0)
		mexErrMsgTxt("Error reading the header of the image");


	//set dimensionality
	mwSize ndims = KLB_DATA_DIMS;
	mwSize dims[KLB_DATA_DIMS];
	for (int ii = 0; ii < KLB_DATA_DIMS; ii++)
		dims[ii] = imgFull.header.xyzct[ii];

	//squeeze
	int pp = KLB_DATA_DIMS - 1;
	while (pp>= 0 && dims[pp] == 1)
	{
		ndims--;
		pp--;
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
	klb_ROI ROIfull; 
	//ROIfull.defineFullImage(imgFull.header.xyzct);
	//error = imgFull.readImage((char*)(mxGetData(plhs[0])), &ROIfull, numThreads);
    error = imgFull.readImageFull((char*)(mxGetData(plhs[0])), numThreads);
	if (error > 0)
		mexErrMsgTxt("Error reading the image");

    
    //return header if requested
    //copied from readKLBheader.cpp
    if( nlhs > 1 )
    {
        //allocate memory and prepare struct to return
        const int nfields = 7;//it has to match fieldnames
        const char *fieldnames[] = { "xyzct", "pixelSize", "dataType", "compressionType", "blockSize", "metadata", "headerVersion"};       /* pointers to field names */
        
        /* create a 1x1 struct matrix for output  */
        plhs[1] = mxCreateStructMatrix(1, 1, nfields, fieldnames);
        
        double *ptr;
        
        //set xyczt
        mxArray *field_value_0 = mxCreateDoubleMatrix(1, KLB_DATA_DIMS, mxREAL);
        ptr = mxGetPr(field_value_0);
        for (int ii = 0; ii < KLB_DATA_DIMS; ii++)
            ptr[ii] = imgFull.header.xyzct[ii];
        mxSetFieldByNumber(plhs[1], 0, 0, field_value_0);//field number is 0-indexed
        
        
        //set pixelSize
        mxArray *field_value_1 = mxCreateDoubleMatrix(1, KLB_DATA_DIMS, mxREAL);
        ptr = mxGetPr(field_value_1);
        for (int ii = 0; ii < KLB_DATA_DIMS; ii++)
            ptr[ii] = imgFull.header.pixelSize[ii];
        mxSetFieldByNumber(plhs[1], 0, 1, field_value_1);//field number is 0-indexed
        
        //set dataType
        mxArray *field_value_2 = mxCreateDoubleMatrix(1, 1, mxREAL);
        ptr = mxGetPr(field_value_2);
        ptr[0] = imgFull.header.dataType;
        mxSetFieldByNumber(plhs[1], 0, 2, field_value_2);//field number is 0-indexed
        
        //set compression type
        mxArray *field_value_3 = mxCreateDoubleMatrix(1, 1, mxREAL);
        ptr = mxGetPr(field_value_3);
        ptr[0] = imgFull.header.compressionType;
        mxSetFieldByNumber(plhs[1], 0, 3, field_value_3);//field number is 0-indexed
        
        
        //set blockSize
        mxArray *field_value_4 = mxCreateDoubleMatrix(1, KLB_DATA_DIMS, mxREAL);
        ptr = mxGetPr(field_value_4);
        for (int ii = 0; ii < KLB_DATA_DIMS; ii++)
            ptr[ii] = imgFull.header.blockSize[ii];
        mxSetFieldByNumber(plhs[1], 0, 4, field_value_4);//field number is 0-indexed
        
        //set metadat as a char
        mxArray *field_value_5 = mxCreateString(imgFull.header.metadata);
        mxSetFieldByNumber(plhs[1], 0, 5, field_value_5);//field number is 0-indexed
        
        
        //set header version
        mxArray *field_value_6 = mxCreateDoubleMatrix(1, 1, mxREAL);
        ptr = mxGetPr(field_value_6);
        ptr[0] = imgFull.header.headerVersion;
        mxSetFieldByNumber(plhs[1], 0, 6, field_value_6);//field number is 0-indexed
    }
    
	//release memory	
    mxFree(filename);
};