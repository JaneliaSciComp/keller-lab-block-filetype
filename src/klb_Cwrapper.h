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

#ifndef __KLB_IMAGE_C_WRAPPER_H__
#define __KLB_IMAGE_C_WRAPPER_H__


#ifdef __cplusplus
extern "C" {  // only need to export C interface if
	// used by C++ source code
#endif

#include <stdint.h>
#include "common.h"


#if defined(COMPILE_SHARED_LIBRARY) && defined(_MSC_VER)
#define DECLSPECIFIER __declspec(dllexport)
#define EXPIMP_TEMPLATE

#else

#define DECLSPECIFIER
#define EXPIMP_TEMPLATE

#endif



	DECLSPECIFIER int writeKLBstack(const void* im, const char* filename, uint32_t xyzct[KLB_DATA_DIMS], uint8_t dataType, int numThreads, float32_t pixelSize[KLB_DATA_DIMS], uint32_t blockSize[KLB_DATA_DIMS], uint8_t compressionType, char metadata[KLB_METADATA_SIZE]);
	
	/*
	\brief Same as writeKLBstack but we use a double pointer to have each slice in a separate address. xyzct[3] = xyzct[4] = 1 (no time or channel information). We assume xyzct[2] = number of slices
	*/
	DECLSPECIFIER int writeKLBstackSlices(const void** im, const char* filename, uint32_t xyzct[KLB_DATA_DIMS], uint8_t dataType, int numThreads, float32_t pixelSize[KLB_DATA_DIMS], uint32_t blockSize[KLB_DATA_DIMS], uint8_t compressionType, char metadata[KLB_METADATA_SIZE]);

	DECLSPECIFIER int readKLBheader(const char* filename, uint32_t xyzct[KLB_DATA_DIMS], uint8_t *dataType, float32_t pixelSize[KLB_DATA_DIMS], uint32_t blockSize[KLB_DATA_DIMS], uint8_t *compressionType, char metadata[KLB_METADATA_SIZE]);

#ifdef __cplusplus
} 
#endif
#endif //end of __KLB_IMAGE_IO_H__