/*
* Copyright (C) 2014 by  Fernando Amat
* See license.txt for full license and copyright notice.
*
* Authors: Fernando Amat
*  klb_imageHeader.cpp
*
*  Created on: October 2nd, 2014
*      Author: Fernando Amat
*
* \brief Main image heade rutilities for klb format
*
*
*/

#include <limits>
#include "klb_imageHeader.h"



using namespace std;

klb_image_header& klb_image_header::operator=(const klb_image_header& p)
{
	if (this != &p)
	{
		memcpy(xyzct, p.xyzct, sizeof(uint32_t)* KLB_DATA_DIMS);
		memcpy(pixelSize, p.pixelSize, sizeof(float32_t)* KLB_DATA_DIMS);
		dataType = p.dataType;
		compressionType = p.compressionType;
		memcpy(blockSize, p.blockSize, sizeof(uint32_t)* KLB_DATA_DIMS);
		resizeBlockOffset(p.Nb);
		memcpy(blockOffset, p.blockOffset, sizeof(uint64_t)* Nb);
		memcpy(optimalBlockSizeInBytes, p.optimalBlockSizeInBytes, sizeof(uint32_t)* KLB_DATA_DIMS);
	}
	return *this;
}

//copy constructor
klb_image_header::klb_image_header(const klb_image_header& p)
{
	
	memcpy(xyzct, p.xyzct, sizeof(uint32_t)* KLB_DATA_DIMS);
	memcpy(pixelSize, p.pixelSize, sizeof(float32_t)* KLB_DATA_DIMS);
	dataType = p.dataType;
	compressionType = p.compressionType;
	memcpy(blockSize, p.blockSize, sizeof(uint32_t)* KLB_DATA_DIMS);
	
	Nb = p.Nb;
	blockOffset = new std::uint64_t[Nb];
	memcpy(blockOffset, p.blockOffset, sizeof(uint64_t)* Nb);

	memcpy(optimalBlockSizeInBytes, p.optimalBlockSizeInBytes, sizeof(uint32_t)* KLB_DATA_DIMS);

}

klb_image_header::klb_image_header()
{
	memset(xyzct, 0, sizeof(uint32_t)* KLB_DATA_DIMS);

	Nb = 0;
	blockOffset = NULL;

	const uint32_t optimalBlockSizeInBytes_[KLB_DATA_DIMS] = { 192, 192, 16, 1, 1 };
	memcpy(optimalBlockSizeInBytes, optimalBlockSizeInBytes_, sizeof(uint32_t)* KLB_DATA_DIMS);
}

klb_image_header::~klb_image_header()
{
	if (Nb != 0)
	{
		delete[] blockOffset;
		blockOffset = NULL;
	}
}

//==========================================
size_t klb_image_header::calculateNumBlocks()
{
	size_t numBlocks = 1;

	for (int ii = 0; ii < KLB_DATA_DIMS; ii++)
		numBlocks *= (size_t)ceil( (float)(xyzct[ii]) / (float)(blockSize[ii]) );

	return numBlocks;
}

//======================================================
size_t klb_image_header::getBytesPerPixel()
{
	switch (dataType)
	{
	case 0:
		return sizeof(std::uint8_t);
		break;
	case 1:
		return sizeof(std::uint16_t);
		break;
	case 2:
		return sizeof(std::uint32_t);
		break;
	case 3:
		return sizeof(std::uint64_t);
		break;
	case 4:
		return sizeof(std::int8_t);
		break;
	case 5:
		return sizeof(std::int16_t);
		break;
	case 6:
		return sizeof(std::int32_t);
		break;
	case 7:
		return sizeof(std::int64_t);
		break;
	case 8:
		return sizeof(float32_t);
		break;
	case 9:
		return sizeof(float64_t);
		break;
	default:
		return 0;
		break;
	}
}

//======================================================
uint32_t klb_image_header::getBlockSizeBytes()
{
	uint32_t blockSizeTotal = 1;
	for (int ii = 0; ii < KLB_DATA_DIMS; ii++)
		blockSizeTotal *= blockSize[ii];

	return blockSizeTotal * getBytesPerPixel();
}

//======================================================
uint64_t klb_image_header::getImageSizeBytes()
{	
	return getImageSizePixels() * getBytesPerPixel();
}
//========================================
uint64_t klb_image_header::getImageSizePixels()
{
	uint64_t imgSize = 1;
	for (int ii = 0; ii < KLB_DATA_DIMS; ii++)
		imgSize *= xyzct[ii];

	return imgSize;
}
//==============================================================
void klb_image_header::writeHeader(std::ostream &fid)
{
	fid.write((char*)xyzct, sizeof(uint32_t)* KLB_DATA_DIMS); 
	fid.write((char*)pixelSize, sizeof(float32_t)* KLB_DATA_DIMS);
	fid.write((char*)(&dataType), sizeof(uint8_t));
	fid.write((char*)(&compressionType), sizeof(uint8_t));
	fid.write((char*)blockSize, sizeof(uint32_t)* KLB_DATA_DIMS);
	fid.write((char*)blockOffset, sizeof(uint64_t)* Nb);//this is the only variable size element
};

//=======================================================
void klb_image_header::readHeader(std::istream &fid)
{
	fid.read((char*)xyzct, sizeof(uint32_t)* KLB_DATA_DIMS);
	fid.read((char*)pixelSize, sizeof(float32_t)* KLB_DATA_DIMS);
	fid.read((char*)(&dataType), sizeof(uint8_t));
	fid.read((char*)(&compressionType), sizeof(uint8_t));
	fid.read((char*)blockSize, sizeof(uint32_t)* KLB_DATA_DIMS);

	
	//resize if necessary
	resizeBlockOffset(calculateNumBlocks());
	
	fid.read((char*)blockOffset, sizeof(uint64_t)* Nb);//this is the only variable size element
};

//======================================================
void klb_image_header::resizeBlockOffset(size_t Nb_)
{
	//resize if necessary	
	if (Nb != Nb_)
	{
		if (Nb != 0)
			delete[] blockOffset;
		Nb = Nb_;
		blockOffset = new std::uint64_t[Nb];
	}
}

//=======================================================
int klb_image_header::readHeader(const char *filename)
{
	ifstream fid(filename, ios::binary | ios::in);
	if (fid.is_open() == false)
	{
		cout << "ERROR: klb_image_header::readHeader : file " << filename << " could not be opened to read header" << endl;
		return 2;
	}

	readHeader(fid);
	fid.close();
	return 0;
};

//==========================================================
size_t  klb_image_header::getBlockCompressedSizeBytes(size_t blockIdx)
{
	if (blockIdx >= Nb)
		return 0;
	else if (blockIdx == 0 )//first block
	{
		return blockOffset[blockIdx];
	}
	else{
		return blockOffset[blockIdx] - blockOffset[blockIdx - 1];
	}

}

//======================================================
std::uint64_t klb_image_header::getBlockOffset(size_t blockIdx)
{
	if (blockIdx >= Nb)
		return numeric_limits<std::uint64_t>::max();
	else if (blockIdx == 0)//first block
	{
		return 0;
	}
	else{
		return blockOffset[blockIdx - 1];
	}
}

//======================================================
std::uint64_t klb_image_header::getCompressedFileSizeInBytes()
{
	return getSizeInBytes() + blockOffset[Nb-1];
}

void klb_image_header::setDefaultBlockSize()
{
	std::uint32_t bytesPerPixel = getBytesPerPixel();	

	for (int ii = 0; ii < KLB_DATA_DIMS; ii++)
	{
		blockSize[ii] = optimalBlockSizeInBytes[ii] / bytesPerPixel;
		if (blockSize[ii] == 0)
		{
			blockSize[ii] = 1;
		}
	}
}