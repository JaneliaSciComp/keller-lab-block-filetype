/*
* Copyright (C) 2014  Fernando Amat
* See license.txt for full license and copyright notice.
*
* Authors: Fernando Amat
*  mainTest_klnIO.cxx
*
*  Created on: October 1st, 2014
*      Author: Fernando Amat
*
* \brief Test the main klb I/O library using the C-style code
*
*
*/


#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>  
#include "klb_Cwrapper.h"



int main(int argc, const char** argv)
{
	int numThreads = 10;//<= 0 indicates use as many as possible
	int compressionType = 1;//1->bzip2; 0->none
	
	char filenameOut[256] = "C:/Users/Fernando/temp/debugKLB";
	uint32_t	xyzct[KLB_DATA_DIMS] = { 800, 1588, 5, 1, 1 };//137 total Z planes
	uint32_t	blockSize[KLB_DATA_DIMS] = { 96, 96, 8, 1, 1 };
	char metadata_[KLB_METADATA_SIZE];
	uint8_t dataType = 1;
	char filenameAux[256];

	int ii;
	FILE* fin;
	void* im;

	if (argc > 1)//do not use default arguments
	{		
		if (argc != 2 + KLB_DATA_DIMS)
		{
			printf("ERROR: wrong number of arguments. Call the programs with <filename> x y z c t\n");
			return 2;
		}

		sprintf(filenameOut, "%s", argv[1]);
	
		for (ii = 0; ii < KLB_DATA_DIMS; ii++)
			xyzct[ii] = atoi(argv[2 + ii]);
	}
		
	sprintf(metadata_, "Testing metadata");


	//read image	
	size_t imSize = xyzct[0];
	for (ii = 1; ii < KLB_DATA_DIMS; ii++)
		imSize *= xyzct[ii];

	im = malloc(sizeof(uint16_t)* imSize);
		
	sprintf(filenameAux, "%s.raw", filenameOut);
	printf("Reading file %s\n", filenameAux);

	fin = fopen(filenameAux, "rb");

	if (fin == NULL)
	{
		printf("ERROR: opening file %s\n", filenameOut);
		return 2;
	}
	fread((char*)im, sizeof(uint16_t), imSize, fin);
	fclose(fin);


	//compress file
	sprintf(filenameAux, "%s.klb", filenameOut);	

	printf("Compressing file to %s\n", filenameAux );

	
	//write image
	int err = writeKLBstack(im, filenameAux, xyzct, dataType, numThreads, NULL, blockSize, compressionType, metadata_);
	
	
	//release memory
	free(im);

	return 0;
}