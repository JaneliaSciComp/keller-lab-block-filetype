/*
*  readKLBheader.cpp
*
*  Created on: October 13, 2014
*      Author: Fernando Amat
*

*     Copyright (C) 2014 by  Fernando Amat
*     See licenseGMEM.txt for full license and copyright notice.
*     
*     \brief Set of utilities to read/write KLB format
*       header = readKLBheader(filename)     
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
    
    
	// check: only one input and one output argument
	if (nrhs != 1)
		mexErrMsgTxt("Must have one input argument: filename");
	if (nlhs !=1)
	    mexErrMsgTxt("Must have one output argument");
	if (mxIsChar(prhs[0]) != 1)
		mexErrMsgTxt("Input 1 must be a string");



	//initialize I/O object
	filename = mxArrayToString(prhs[0]);
	string filenameOut(filename);
	klb_imageIO imgIO(filenameOut);

	int error = imgIO.readHeader();
	if (error > 0)
		mexErrMsgTxt("Error reading the header of the image");
		

	//allocate memory and prepare struct to return
	const int nfields = 7;//it has to match fieldnames
	const char *fieldnames[] = { "xyzct", "pixelSize", "dataType", "compressionType", "blockSize", "metadata", "headerVersion"};       /* pointers to field names */

	/* create a 1x1 struct matrix for output  */
	plhs[0] = mxCreateStructMatrix(1, 1, nfields, fieldnames);
    
	double *ptr;

	//set xyczt
	mxArray *field_value_0 = mxCreateDoubleMatrix(1, KLB_DATA_DIMS, mxREAL);
	ptr = mxGetPr(field_value_0);
	for (int ii = 0; ii < KLB_DATA_DIMS; ii++)
		ptr[ii] = imgIO.header.xyzct[ii];
	mxSetFieldByNumber(plhs[0], 0, 0, field_value_0);//field number is 0-indexed


	//set pixelSize
	mxArray *field_value_1 = mxCreateDoubleMatrix(1, KLB_DATA_DIMS, mxREAL);
	ptr = mxGetPr(field_value_1);
	for (int ii = 0; ii < KLB_DATA_DIMS; ii++)
		ptr[ii] = imgIO.header.pixelSize[ii];
	mxSetFieldByNumber(plhs[0], 0, 1, field_value_1);//field number is 0-indexed

	//set dataType
	mxArray *field_value_2 = mxCreateDoubleMatrix(1, 1, mxREAL);
	ptr = mxGetPr(field_value_2);	
	ptr[0] = imgIO.header.dataType;
	mxSetFieldByNumber(plhs[0], 0, 2, field_value_2);//field number is 0-indexed

	//set compression type
	mxArray *field_value_3 = mxCreateDoubleMatrix(1, 1, mxREAL);
	ptr = mxGetPr(field_value_3);
	ptr[0] = imgIO.header.compressionType;
	mxSetFieldByNumber(plhs[0], 0, 3, field_value_3);//field number is 0-indexed


	//set blockSize
	mxArray *field_value_4 = mxCreateDoubleMatrix(1, KLB_DATA_DIMS, mxREAL);
	ptr = mxGetPr(field_value_4);
	for (int ii = 0; ii < KLB_DATA_DIMS; ii++)
		ptr[ii] = imgIO.header.blockSize[ii];
	mxSetFieldByNumber(plhs[0], 0, 4, field_value_4);//field number is 0-indexed
    
    //set metadat as a char        
	mxArray *field_value_5 = mxCreateString(imgIO.header.metadata);	
	mxSetFieldByNumber(plhs[0], 0, 5, field_value_5);//field number is 0-indexed
    
    
    //set header version
	mxArray *field_value_6 = mxCreateDoubleMatrix(1, 1, mxREAL);
	ptr = mxGetPr(field_value_6);
	ptr[0] = imgIO.header.headerVersion;
	mxSetFieldByNumber(plhs[0], 0, 6, field_value_6);//field number is 0-indexed

	//release memory	
    mxFree(filename);
};