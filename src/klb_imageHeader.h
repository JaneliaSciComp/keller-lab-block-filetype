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
#define KLB_METADATA_SIZE (256) //number of bytes in metadata
#define KLB_DEFAULT_HEADER_VERSION (2) //def

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
	std::uint8_t					headerVersion;		//indicates header version(always the first byte)
	std::uint32_t					xyzct[KLB_DATA_DIMS];     //image dimensions in pixels
	float32_t						pixelSize[KLB_DATA_DIMS];     //pixel size (in um,au,secs) for each dimension
	KLB_DATA_TYPE					dataType;     //lookup table for data type (uint8, uint16, etc)
	KLB_COMPRESSION_TYPE			compressionType; //lookup table for compression type (none, pbzip2,etc)
	char							metadata[KLB_METADATA_SIZE];//wildcard to store any data you want
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
	size_t getNumBlocks() const{ return Nb; };
	int getMetadataSizeInBytes() const{ return KLB_METADATA_SIZE; };
	size_t calculateNumBlocks() const;
	size_t getSizeInBytes()  const{ return KLB_DATA_DIMS * (2 * sizeof(std::uint32_t) + sizeof(float32_t)) + 2 * sizeof(std::uint8_t) + Nb * sizeof(std::uint64_t) + sizeof(char)* (KLB_METADATA_SIZE + 1); };
	size_t getSizeInBytesFixPortion()  const{ return KLB_DATA_DIMS * (2 * sizeof(std::uint32_t) + sizeof(float32_t)) + 2 * sizeof(std::uint8_t) + sizeof(char)* (KLB_METADATA_SIZE + 1); };
	size_t getBytesPerPixel() const;
	std::uint32_t getBlockSizeBytes() const;
	std::uint64_t getImageSizeBytes() const;
	std::uint64_t getImageSizePixels() const;
	size_t getBlockCompressedSizeBytes(size_t blockId) const;
	std::uint64_t getBlockOffset(size_t blockIdx) const;//offset in compressed file without counting header (so you have to add getSizeInBytes() for total offset
	std::uint64_t getCompressedFileSizeInBytes() const;
	void setDefaultBlockSize();//sets default block size based on our analysis for our own images
	void resizeBlockOffset(size_t Nb_);
	void setOptimalBlockSizeInBytes(){ optimalBlockSizeInBytes[0] = 192; optimalBlockSizeInBytes[1] = 192; optimalBlockSizeInBytes[2] = 16; optimalBlockSizeInBytes[3] = 1; optimalBlockSizeInBytes[4] = 1; };
	

	char* getMetadataPtr() { return metadata; };
	char* cloneMetadata() const{
		char* p = new char[KLB_METADATA_SIZE]; memcpy(p, metadata, sizeof(char)* KLB_METADATA_SIZE); return p;};
	void setMetadata(char meta[KLB_METADATA_SIZE]){ memcpy(metadata, meta, KLB_METADATA_SIZE * sizeof(char)); };

	void setHeader(const std::uint32_t xyzct_[KLB_DATA_DIMS], const  KLB_DATA_TYPE dataType_, const  float32_t pixelSize_[KLB_DATA_DIMS] = NULL, const std::uint32_t blockSize_[KLB_DATA_DIMS] = NULL, 
					const KLB_COMPRESSION_TYPE compressionType_ = KLB_COMPRESSION_TYPE::BZIP2, const  char metadata_[KLB_METADATA_SIZE] = NULL, const  std::uint8_t headerVersion_ = KLB_DEFAULT_HEADER_VERSION);

protected:

private:	
	std::uint32_t optimalBlockSizeInBytes[KLB_DATA_DIMS];//not const to make it easy to call from JAVA wrapper. Not static to make it thread-safe
};


#endif //end of __KLB_IMAGE_HEADER_H__