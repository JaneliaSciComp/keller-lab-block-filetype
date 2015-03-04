/*
* Copyright (C) 2014 by  Fernando Amat
* See license.txt for full license and copyright notice.
*
* Authors: Fernando Amat
*  klb_Cwrapper.c
*
*  Created on: October 2nd, 2014
*      Author: Fernando Amat
*
* \brief 
*/

#include <string>
#include "klb_Cwrapper.h"
#include "klb_imageIO.h"


int writeKLBstack(const void* im, const char* filename, uint32_t xyzct[KLB_DATA_DIMS], uint8_t dataType, int numThreads = -1, float32_t pixelSize[KLB_DATA_DIMS] = NULL, uint32_t blockSize[KLB_DATA_DIMS] = NULL, uint8_t compressionType = KLB_COMPRESSION_TYPE::BZIP2, char metadata[KLB_METADATA_SIZE] = NULL)
{
	
	//initialize I/O object
	std::string filenameOut(filename);
	klb_imageIO imgIO(filenameOut);

	//set header
	imgIO.header.setHeader(xyzct, dataType, pixelSize, blockSize, compressionType, metadata);
		

	
	int error = imgIO.writeImage((char*)(im), numThreads);//all the threads available
	
	if (error > 0)
	{
		switch (error)
		{
		case 2:
			printf("Error during BZIP compression of one of the blocks");
			break;
		case 5:
			printf("Error generating the output file in the specified location");
			break;
		default:
			printf("Error writing the image");
		}
	}
	

	return error;
}

//================================================================================================================================================
int writeKLBstackSlices(const void** im, const char* filename, uint32_t xyzct[KLB_DATA_DIMS], uint8_t dataType, int numThreads = -1, float32_t pixelSize[KLB_DATA_DIMS] = NULL, uint32_t blockSize[KLB_DATA_DIMS] = NULL, uint8_t compressionType = KLB_COMPRESSION_TYPE::BZIP2, char metadata[KLB_METADATA_SIZE] = NULL)
{

	//initialize I/O object
	std::string filenameOut(filename);
	klb_imageIO imgIO(filenameOut);

	//set header
	imgIO.header.setHeader(xyzct, dataType, pixelSize, blockSize, compressionType, metadata);

	int error = imgIO.writeImageStackSlices((const char**)im, numThreads);//all the threads available

	if (error > 0)
	{
		switch (error)
		{
		case 2:
			printf("Error during BZIP compression of one of the blocks");
			break;
		case 3:
			printf("Error: number of channels or number of time points must be 1 for this API call\n");
			break;
		case 5:
			printf("Error generating the output file in the specified location");
			break;
		default:
			printf("Error writing the image");
		}
	}


	return error;
}

//===================================================================================
int readKLBheader(const char* filename, uint32_t xyzct[KLB_DATA_DIMS], uint8_t *dataType, float32_t pixelSize[KLB_DATA_DIMS], uint32_t blockSize[KLB_DATA_DIMS], uint8_t *compressionType, char metadata[KLB_METADATA_SIZE])
{
	std::string filenameOut(filename);

	klb_image_header header;
	int error = header.readHeader(filename);

	if (error != 0)
	{
		return error;
	}

	//parse header
	memcpy(xyzct, header.xyzct, sizeof(uint32_t)* KLB_DATA_DIMS);
	*dataType = header.dataType;	
	*compressionType = header.compressionType;
	memcpy(pixelSize, header.pixelSize, sizeof(float32_t)* KLB_DATA_DIMS);
	memcpy(metadata, header.metadata, sizeof(char)* KLB_METADATA_SIZE);
	memcpy(blockSize, header.blockSize, sizeof(uint32_t)* KLB_DATA_DIMS);

	return error;
}