/*
* Copyright (C) 2014 by  Fernando Amat
* See license.txt for full license and copyright notice.
*
* Authors: Fernando Amat
*  klb_imageHeader.h
*
*  Created on: October 2nd, 2014
*      Author: Fernando Amat
*
* \brief Main image heade rutilities for klb format
*/

#ifndef __KLB_IMAGE_HEADER_H__
#define __KLB_IMAGE_HEADER_H__



#include <cstdint>
#include <vector>
#include <iostream>
#include <fstream>
#include <cstring>
#include <math.h>

typedef float  float32_t;
typedef double float64_t;

#define KLB_DATA_DIMS (5) //our images at the most have 5 dimensions: x,y,z, c, t


// Following mylib conventions here are the data types
enum KLB_DATA_TYPE
{
	UINT8_TYPE = 0,
	UINT16_TYPE = 1,
	UINT32_TYPE = 2,
	UINT64_TYPE = 3,
	INT8_TYPE = 4,
	INT16_TYPE = 5,
	INT32_TYPE = 6,
	INT64_TYPE = 7,
	FLOAT32_TYPE = 8,
	FLOAT64_TYPE = 9
};

//Compression type look up table (add to the list if you use a different one)
//To add more compression types just add it here and look for 
enum KLB_COMPRESSION_TYPE
{
	NONE = 0,
	BZIP2 = 1
};


#if defined(COMPILE_SHARED_LIBRARY) && defined(_MSC_VER)
class __declspec(dllexport) klb_image_header
#else
class klb_image_header
#endif
{
public:

	//header fields
	std::uint32_t					xyzct[KLB_DATA_DIMS];     //image dimensions in pixels
	float32_t						pixelSize[KLB_DATA_DIMS];     //pixel size (in um,au,secs) for each dimension
	std::uint8_t					dataType;     //lookup table for data type (uint8, uint16, etc)
	std::uint8_t					compressionType; //lookup table for compression type (none, pbzip2,etc)
	std::uint32_t					blockSize[KLB_DATA_DIMS];     //block size along each dimension to partition the data for bzip. The total size of each block should be ~1MB
	std::uint64_t*		blockOffset; //offset (in bytes) within the file for each block, so we can retrieve blocks individually. Nb = prod_i ceil(xyzct[i]/blockSize[i]). I use a pointer (instead of vector) to facilitate dllexport to shared library
	size_t Nb;//length of blockOffset array
	
	//constructors 
	klb_image_header(const klb_image_header& p);
	~klb_image_header();
	klb_image_header();
	//operators
	klb_image_header& operator=(const klb_image_header& p);

	//main functionality
	void writeHeader(std::ostream &fid);
	void writeHeader(FILE* fid);
	void readHeader(std::istream &fid);
	int readHeader(const char *filename);

	//set/get functions
	size_t getNumBlocks(){ return Nb; };
	size_t calculateNumBlocks();
	size_t getSizeInBytes() { return KLB_DATA_DIMS * (2 * sizeof(std::uint32_t) + sizeof(float32_t) ) + 2 * sizeof(std::uint8_t) + Nb * sizeof(std::uint64_t); };
	size_t getSizeInBytesFixPortion() { return KLB_DATA_DIMS * (2 * sizeof(std::uint32_t) + sizeof(float32_t)) + 2 * sizeof(std::uint8_t); };
	size_t getBytesPerPixel();
	std::uint32_t getBlockSizeBytes();
	std::uint64_t getImageSizeBytes();
	std::uint64_t getImageSizePixels();
	size_t getBlockCompressedSizeBytes(size_t blockId);
	std::uint64_t getBlockOffset(size_t blockIdx);//offset in compressed file without counting header (so you have to add getSizeInBytes() for total offset
	std::uint64_t getCompressedFileSizeInBytes();
	void setDefaultBlockSize();//sets default block size based on our analysis for our own images
	void resizeBlockOffset(size_t Nb_);

protected:

private:	
	std::uint32_t optimalBlockSizeInBytes[KLB_DATA_DIMS];//not const to make it easy to call from JAVA wrapper. Not static to make it thread-safe
};


#endif //end of __KLB_IMAGE_HEADER_H__