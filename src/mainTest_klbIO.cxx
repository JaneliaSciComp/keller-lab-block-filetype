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
* \brief Test the main klb I/O library
*
*
*/


#include <string>
#include <cstdint>
#include <chrono>

#include "klb_imageIO.h"


using namespace std;
typedef std::chrono::high_resolution_clock Clock;

int main(int argc, const char** argv)
{
	int numThreads = 12;//<= 0 indicates use as many as possible
	std::uint32_t	xyzct[KLB_DATA_DIMS] = {1002, 414, 111, 1, 1};
	std::uint32_t	blockSize[KLB_DATA_DIMS] = {256, 128, 32, 1, 1};

	std::string filenameOut("E:/compressionFormatData/debugGradient.klb");
	


	//initialize I/O object
	klb_imageIO imgIO( filenameOut );

	//setup header
	memcpy(imgIO.header.xyzct, xyzct, sizeof(uint32_t)* KLB_DATA_DIMS);
	memcpy(imgIO.header.blockSize, blockSize, sizeof(uint32_t)* KLB_DATA_DIMS);
	imgIO.header.dataType = 1;//uint16
	imgIO.header.compressionType = 1;//bzip2
	for (int ii = 0; ii < KLB_DATA_DIMS; ii++)
		imgIO.header.pixelSize[ii] = 1.2f*ii;


	//generate artificial image: gradient is nice since we can debug very fast by visual inspection
	uint16_t* img = new uint16_t[imgIO.header.getImageSizePixels()];
	for (uint64_t ii = 0; ii < imgIO.header.getImageSizePixels(); ii++)
	{
		img[ii] = ii % 65535;
	}


	auto t1 = Clock::now();
	int err = imgIO.writeImage((char*)img, numThreads);//all the threads available
	if (err > 0)
		return 2;

	auto t2 = Clock::now();

	//release memory
	delete[] img;


	std::cout << "Written test file at "<<filenameOut<<" compress + write file =" << std::chrono::duration_cast<std::chrono::milliseconds>(t2 - t1).count() << " ms using "<<numThreads<<" threads"<< std::endl;

	return 0;
}