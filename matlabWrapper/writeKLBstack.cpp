/*
*  writeKLBstack.cpp
*
*  Created on: October 13, 2014
*      Author: Fernando Amat
*

*     Copyright (C) 2014 by  Fernando Amat
*     See license.txt for full license and copyright notice.
*     
*     \brief Set of utilities to read/write KLB format
*       writeKLBstack(im, filenameOut, numThreads, pixelSize, blockSize, compressionType, metadata)     
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
    int numThreads = -1;//use all the threads
    int error = 0;

    
	mwSize ndims = KLB_DATA_DIMS;
    const mwSize *dims;
    
	if (nrhs < 2)
		mexErrMsgTxt("You neeed to provide at least two inputs: im, filenameOut");

	if (mxIsChar(prhs[1]) != 1)
		mexErrMsgTxt("Input 1 must be a string");

	if (nlhs != 0)
		mexErrMsgTxt("Must not have any output argument");


	//initialize I/O object
	filename = mxArrayToString(prhs[1]);
	string filenameOut(filename);
	klb_imageIO imgIO(filenameOut);

	//default values
	for (int ii = 0; ii < KLB_DATA_DIMS; ii++)
	{
		imgIO.header.xyzct[ii] = 1;
		imgIO.header.pixelSize[ii] = 1.0f;;//so users know it was not especified
	}	
	
	//setting data type
	switch (mxGetClassID(prhs[0]))
	{
	case mxUINT8_CLASS:
		imgIO.header.dataType = KLB_DATA_TYPE::UINT8_TYPE;
		break;

	case mxUINT16_CLASS:
		imgIO.header.dataType = KLB_DATA_TYPE::UINT16_TYPE;
		break;

	case mxUINT32_CLASS:
		imgIO.header.dataType = KLB_DATA_TYPE::UINT32_TYPE;
		break;

	case mxUINT64_CLASS:
		imgIO.header.dataType = KLB_DATA_TYPE::UINT64_TYPE;
		break;

	case mxINT8_CLASS:
		imgIO.header.dataType = KLB_DATA_TYPE::INT8_TYPE;
		break;

	case mxINT16_CLASS:
		imgIO.header.dataType = KLB_DATA_TYPE::INT16_TYPE;
		break;

	case mxINT32_CLASS:
		imgIO.header.dataType = KLB_DATA_TYPE::INT32_TYPE;
		break;

	case mxINT64_CLASS:
		imgIO.header.dataType = KLB_DATA_TYPE::INT64_TYPE;
		break;

	case mxSINGLE_CLASS:
		imgIO.header.dataType = KLB_DATA_TYPE::FLOAT32_TYPE;
		break;

	case mxDOUBLE_CLASS:
		imgIO.header.dataType = KLB_DATA_TYPE::FLOAT64_TYPE;
		break;

	default:
		mexErrMsgTxt("Data type not supported");
	}

	//default block values
	imgIO.header.setDefaultBlockSize();
	imgIO.header.compressionType = KLB_COMPRESSION_TYPE::BZIP2;//bzip2 by default

    // check: only one input and one output argument
    switch( nrhs )
    {
        case 2://only image and filename are provided
			//nothing to do            
            break;
            
        case 3://numthreads is added
			if (mxIsEmpty(prhs[2]) == false)
			{
				numThreads = (int)(mxGetPr(prhs[2])[0]);
			}
            
            break;
        case 4://pizelSize is added
            
			if (mxIsEmpty(prhs[2]) == false)
			{
				numThreads = (int)(mxGetPr(prhs[2])[0]);
			}
			if (mxIsEmpty(prhs[3]) == false)
			{
				for (int ii = 0; ii < min((int)KLB_DATA_DIMS, (int)mxGetNumberOfElements(prhs[3])); ii++)
					imgIO.header.pixelSize[ii] = (float)(mxGetPr(prhs[3])[ii]);
			}
            break;
            
        case 5://block size is added 
			if (mxIsEmpty(prhs[2]) == false)
			{
				numThreads = (int)(mxGetPr(prhs[2])[0]);
			}
			if (mxIsEmpty(prhs[3]) == false)
			{
				for (int ii = 0; ii < min((int)KLB_DATA_DIMS, (int)mxGetNumberOfElements(prhs[3])); ii++)
					imgIO.header.pixelSize[ii] = (float)(mxGetPr(prhs[3])[ii]);
			}
			if (mxIsEmpty(prhs[4]) == false)
			{
				for (int ii = 0; ii < min((int)KLB_DATA_DIMS, (int)mxGetNumberOfElements(prhs[4])); ii++)
					imgIO.header.blockSize[ii] = (float)(mxGetPr(prhs[4])[ii]);
			}
            break;
            
        case 6://compression type is added
			if (mxIsEmpty(prhs[2]) == false)
			{
				numThreads = (int)(mxGetPr(prhs[2])[0]);
			}
			if (mxIsEmpty(prhs[3]) == false)
			{
				for (int ii = 0; ii < min((int)KLB_DATA_DIMS, (int)mxGetNumberOfElements(prhs[3])); ii++)
					imgIO.header.pixelSize[ii] = (float)(mxGetPr(prhs[3])[ii]);
			}
			if (mxIsEmpty(prhs[4]) == false)
			{
				for (int ii = 0; ii < min((int)KLB_DATA_DIMS, (int)mxGetNumberOfElements(prhs[4])); ii++)
					imgIO.header.blockSize[ii] = (float)(mxGetPr(prhs[4])[ii]);
			}
			if (mxIsEmpty(prhs[5]) == false)
			{
                
                switch((int)(mxGetPr(prhs[5])[0]))
                {
                    case 0:
                        imgIO.header.compressionType = KLB_COMPRESSION_TYPE::NONE;
                        break;
                    case 1:
                        imgIO.header.compressionType = KLB_COMPRESSION_TYPE::BZIP2;
                        break;
                    case 2:
                        imgIO.header.compressionType = KLB_COMPRESSION_TYPE::ZLIB;
                        break;
                    default:
                        mexErrMsgTxt("Compression type not supported");
                }
			}            
            break;
            
        case 7://metadata is added
            if (mxIsEmpty(prhs[2]) == false)
			{
				numThreads = (int)(mxGetPr(prhs[2])[0]);
			}
			if (mxIsEmpty(prhs[3]) == false)
			{
				for (int ii = 0; ii < min((int)KLB_DATA_DIMS, (int)mxGetNumberOfElements(prhs[3])); ii++)
					imgIO.header.pixelSize[ii] = (float)(mxGetPr(prhs[3])[ii]);
			}
			if (mxIsEmpty(prhs[4]) == false)
			{
				for (int ii = 0; ii < min((int)KLB_DATA_DIMS, (int)mxGetNumberOfElements(prhs[4])); ii++)
					imgIO.header.blockSize[ii] = (float)(mxGetPr(prhs[4])[ii]);
			}
			if (mxIsEmpty(prhs[5]) == false)
			{
                switch((int)(mxGetPr(prhs[5])[0]))
                {
                    case 0:
                        imgIO.header.compressionType = KLB_COMPRESSION_TYPE::NONE;
                        break;
                    case 1:
                        imgIO.header.compressionType = KLB_COMPRESSION_TYPE::BZIP2;
                        break;
                    case 2:
                        imgIO.header.compressionType = KLB_COMPRESSION_TYPE::ZLIB;
                        break;
                    default:
                        mexErrMsgTxt("Compression type not supported");
                }				
			} 
            
            if( mxIsEmpty(prhs[6]) == false )
            {
                if( mxGetClassID(prhs[6]) != mxCHAR_CLASS )
                    mexErrMsgTxt("Metadata has to been a char array");
                                
				
				size_t buflen = mxGetN(prhs[6]);
				
								
				if (buflen > KLB_METADATA_SIZE)
					mexErrMsgTxt("Metadata cannot contain more than KLB_METADATA_SIZE bytes");
                								
				char* str = mxArrayToString(prhs[6]);				

				if (str == NULL)
					mexErrMsgTxt("Error converting metadata");
				else
					memcpy(imgIO.header.metadata, str, sizeof(char)* strlen(str));
					mxFree(str);
            }
            break;
        default:
            mexErrMsgTxt("Number of arguments is incorrect");
            
    }

	//setup dimensions
	ndims = mxGetNumberOfDimensions(prhs[0]);
	dims = mxGetDimensions(prhs[0]);
	for (int ii = 0; ii < ndims; ii++)
		imgIO.header.xyzct[ii] = dims[ii];

    
    
	error = imgIO.writeImage((char*)(mxGetData(prhs[0])), numThreads);//all the threads available
	if (error > 0)
    {
        switch(error)
        {
            case 2:
                mexErrMsgTxt("Error during BZIP compression of one of the blocks");
                break;
			case 5:
				mexErrMsgTxt("Error generating the output file in the specified location");
				break;
            default:
                mexErrMsgTxt("Error writing the image");
        }
    }
	//release memory	
    mxFree(filename);
};