/*
* Copyright (C) 2014 by  Fernando Amat
* See license.txt for full license and copyright notice.
*
* Authors: Fernando Amat
*  klb_imageIO.cpp
*
*  Created on: October 2nd, 2014
*      Author: Fernando Amat
*
* \brief Main class to read/write klb format
*/

#if defined(_WIN32) || defined(_WIN64)
#define NOMINMAX
#endif

#include <iostream>
#include <fstream>
#include <thread>
#include <algorithm>
#include <mutex>
#include <chrono>
#include <stdlib.h>     /* div, div_t */
#include "klb_imageIO.h"
#include "bzlib.h"



//#define DEBUG_PRINT_THREADS
typedef std::chrono::high_resolution_clock Clock;
using namespace std;

//static variables for the class
std::mutex				klb_imageIO::g_lockblockId;//so each worker reads a unique blockId
std::condition_variable	klb_imageIO::g_queuecheck;//to notify writer that blocks are ready

#ifdef PROFILE_COMPRESSION
std::atomic <long long> klb_imageIO::g_countCompression;//in case we want to measure only compression timing
#endif

//Round a / b to nearest higher integer value (T should be an integer class)
template<class T>
inline T iDivUp(const T a, const T b){
	return (a % b != 0) ? (a / b + 1) : (a / b);
}

//========================================================
//======================================================
void klb_imageIO::blockCompressor(const char* buffer, int* g_blockSize, uint64_t *blockId, int* g_blockThreadId, klb_circular_dequeue* cq, int threadId, int* errFlag)
{
	*errFlag = 0;
	int BWTblockSize = 9;//maximum compression
	std::uint64_t blockId_t;
	int gcount;//read bytes
	unsigned int sizeCompressed;//size of block in bytes after compression

	size_t bytesPerPixel = header.getBytesPerPixel();
	uint32_t blockSizeBytes = bytesPerPixel;
	uint32_t maxBlockSizeBytesCompressed = maximumBlockSizeCompressedInBytes();
	uint64_t fLength = bytesPerPixel;
	uint64_t dimsBlock[KLB_DATA_DIMS];//number of blocks on each dimension
	uint64_t coordBlock[KLB_DATA_DIMS];//coordinates (in block space). blockId_t = coordBblock[0] + dimsBblock[0] * coordBlock[1] + dimsBblock[0] * dimsBblock[1] * coordBlock[2] + ...
	uint64_t offsetBuffer;//starting offset for each buffer
	uint32_t blockSizeAux[KLB_DATA_DIMS];//for border cases where the blocksize might be different
	uint64_t xyzctCum[KLB_DATA_DIMS];//to calculate offsets for each dimension

	xyzctCum[0] = 1;
	for (int ii = 0; ii < KLB_DATA_DIMS; ii++)
	{
		blockSizeBytes *= header.blockSize[ii];
		fLength *= header.xyzct[ii];
		dimsBlock[ii] = ceil((float)(header.xyzct[ii]) / (float)(header.blockSize[ii]));
		if (ii > 0)
			xyzctCum[ii] = xyzctCum[ii - 1] * header.xyzct[ii - 1];
	}
	char* bufferIn = new char[blockSizeBytes];
	
	BWTblockSize = std::min( BWTblockSize, iDivUp ((int)blockSizeBytes , (int)100000) );//packages of 100,000 bytes
	
	

	std::uint64_t numBlocks = header.getNumBlocks();


	//main loop to keep processing blocks while they are available
	while (1)
	{
		//get the blockId resource
		std::unique_lock<std::mutex> locker(g_lockblockId);//exception safe
		blockId_t = (*blockId);
		(*blockId)++;
		locker.unlock();

		//check if we can access data or we cannot read longer
		if (blockId_t >= numBlocks)
			break;


#ifdef DEBUG_PRINT_THREADS
		printf("Thread %d reading block %d out of %d total blocks\n", (int)(std::this_thread::get_id().hash()), (int)blockId_t, (int)numBlocks);
		fflush(stdout); // Will now print everything in the stdout buffer
#endif

		//-------------------read block-----------------------------------

		//calculate coordinate (in block space)
		std::uint64_t blockIdx_aux = blockId_t;
		for (int ii = 0; ii < KLB_DATA_DIMS; ii++)
		{
			coordBlock[ii] = blockIdx_aux % dimsBlock[ii];
			blockIdx_aux -= coordBlock[ii];
			blockIdx_aux /= dimsBlock[ii];
			coordBlock[ii] *= header.blockSize[ii];//parsing coordinates to image space (not block anymore)
		}

		//make sure it is not a border block
		for (int ii = 0; ii < KLB_DATA_DIMS; ii++)
		{
			blockSizeAux[ii] = std::min(header.blockSize[ii], (uint32_t)(header.xyzct[ii] - coordBlock[ii]));
		}

		//calculate starting offset in the buffer
		offsetBuffer = 0;
		for (int ii = 0; ii < KLB_DATA_DIMS; ii++)
		{
			offsetBuffer += coordBlock[ii] * xyzctCum[ii];
		}
		offsetBuffer *= bytesPerPixel;

		//copy block into local buffer bufferIn
		gcount = 0;
		uint32_t bcount[KLB_DATA_DIMS];//to count elements in the block
		memset(bcount, 0, sizeof(uint32_t)* KLB_DATA_DIMS);
		int auxDim = 1;
		while (auxDim < KLB_DATA_DIMS)
		{
			//copy fastest moving coordinate all at once for efficiency
			memcpy(&(bufferIn[gcount]), &(buffer[offsetBuffer]), bytesPerPixel * blockSizeAux[0]);
			gcount += bytesPerPixel * blockSizeAux[0];

			//increment counter
			auxDim = 1;
			bcount[auxDim]++;
			offsetBuffer += xyzctCum[auxDim] * bytesPerPixel;//update offset 
			while (bcount[auxDim] == blockSizeAux[auxDim])
			{
				offsetBuffer -= bcount[auxDim] * xyzctCum[auxDim] * bytesPerPixel;//update buffer
				bcount[auxDim++] = 0;
				if (auxDim == KLB_DATA_DIMS)
					break;
				bcount[auxDim]++;
				offsetBuffer += xyzctCum[auxDim] * bytesPerPixel; //update buffer
			}
		}

		//-------------------end of read block-----------------------------------

		//decide address where we write the compressed block output						
		char* bufferOutPtr = cq->getWriteBlock(); //this operation is thread safe	


#ifdef PROFILE_COMPRESSION
		auto t1 = Clock::now();
#endif
		//apply compression to block
		switch (header.compressionType)
		{
		case 0://no compression
			sizeCompressed = gcount;
			memcpy(bufferOutPtr, bufferIn, sizeCompressed);//fid.gcount():Returns the number of characters extracted by the last unformatted input operation performed on the object
			break;
		case 1://bzip2
		{
				   sizeCompressed = maxBlockSizeBytesCompressed;
				   // compress the memory buffer (blocksize=9*100k, verbose=0, worklevel=30)				  
				   int ret = BZ2_bzBuffToBuffCompress(bufferOutPtr, &sizeCompressed, bufferIn, gcount, BWTblockSize, 0, 30);
				   if (ret != BZ_OK)
				   {
					   std::cout << "ERROR: workerfunc: compressing data at block " << blockId_t << " with bzip2. Error code " << ret << std::endl;
					   *errFlag = 2;
					   sizeCompressed = 0;
				   }
				   break;
		}
		default:
			std::cout << "ERROR: workerfunc: compression type not implemented" << std::endl;
			*errFlag = 5;
			sizeCompressed = 0;
		}

#ifdef PROFILE_COMPRESSION
		auto t2 = Clock::now();
		long long auxChrono = std::chrono::duration_cast<std::chrono::milliseconds>(t2 - t1).count();//in ms
		atomic_fetch_add(&g_countCompression, auxChrono);
#endif

		cq->pushWriteBlock();//notify content is ready in the queue

		//signal blockWriter that this block can be writen
		//std::unique_lock<std::mutex> locker(g_lockqueue);//adquires the lock
		g_blockSize[blockId_t] = sizeCompressed;//I don't really need the lock to modify this. I only need to singal the condition variable
		g_blockThreadId[blockId_t] = threadId;
		g_queuecheck.notify_one();

#ifdef DEBUG_PRINT_THREADS
		printf("Thread %d finished compressing block %d into %d bytes\n", (int)(std::this_thread::get_id().hash()), (int)blockId_t, (int) sizeCompressed);
		fflush(stdout); // Will now print everything in the stdout buffer
#endif
	}


	//release memory
	delete[] bufferIn;

}

//======================================================
void klb_imageIO::blockUncompressor(char* bufferOut, std::atomic<uint64_t> *blockId, const klb_ROI* ROI, int *errFlag)
{
	*errFlag = 0;
	//open file to read elements
	ifstream fid(filename.c_str(), ios::binary | ios::in);
	if (fid.is_open() == false)
	{
		cout << "ERROR: blockUncompressor: thread opening file " << filename << endl;
		*errFlag = 3;
		return;
	}

	//define variables
	std::uint64_t blockId_t;//to know which block we are processing
	unsigned int sizeCompressed, gcount;
	std::uint64_t offset;//size of block in bytes after compression
	

	size_t bytesPerPixel = header.getBytesPerPixel();
	uint32_t blockSizeBytes = bytesPerPixel;
	uint64_t fLength = bytesPerPixel;
	uint64_t dimsBlock[KLB_DATA_DIMS];//number of blocks on each dimension
	uint64_t coordBlock[KLB_DATA_DIMS];//coordinates (in block space). blockId_t = coordBblock[0] + dimsBblock[0] * coordBlock[1] + dimsBblock[0] * dimsBblock[1] * coordBlock[2] + ...
	uint64_t offsetBuffer;//starting offset for each buffer within ROI
	uint32_t offsetBufferBlock;////starting offset for each buffer within decompressed block
	uint32_t blockSizeAux[KLB_DATA_DIMS];//for border cases where the blocksize might be different
	uint64_t xyzctCum[KLB_DATA_DIMS];//to calculate offsets for each dimension in THE ROI
	uint64_t offsetHeaderBytes = header.getSizeInBytes();

	xyzctCum[0] = bytesPerPixel;
	for (int ii = 0; ii < KLB_DATA_DIMS; ii++)
	{
		blockSizeBytes *= header.blockSize[ii];
		fLength *= header.xyzct[ii];
		dimsBlock[ii] = ceil((float)(header.xyzct[ii]) / (float)(header.blockSize[ii]));
		if (ii > 0)
			xyzctCum[ii] = xyzctCum[ii - 1] * ROI->getSizePixels(ii-1);
	}

	std::uint64_t numBlocks = header.getNumBlocks();
	char* bufferIn = new char[blockSizeBytes];//temporary storage for decompressed block
	char* bufferFile = new char[blockSizeBytes];//temporary storage for compressed block from file

	//main loop to keep processing blocks while they are available
	while (1)
	{
		//get the blockId resource
		blockId_t = atomic_fetch_add(blockId, 1);

		//check if we have more blocks
		if (blockId_t >= numBlocks)
			break;

		//calculate coordinate (in block space)
		std::uint64_t blockIdx_aux = blockId_t;
		for (int ii = 0; ii < KLB_DATA_DIMS; ii++)
		{
			coordBlock[ii] = blockIdx_aux % dimsBlock[ii];
			blockIdx_aux -= coordBlock[ii];
			blockIdx_aux /= dimsBlock[ii];
			coordBlock[ii] *= header.blockSize[ii];//parsing coordinates to image space (not block anymore)
		}

		//check if ROI and bloock coordinates intersect
		bool intersect = true;
		for (int ii = 0; ii < KLB_DATA_DIMS; ii++)
		{
			//for each coordinate, we have to check: RectA.X1 < RectB.X2 && RectA.X2 > RectB.X1, where X1 is minimum nad X2 is maximum coordinate
			//from http://stackoverflow.com/questions/306316/determine-if-two-rectangles-overlap-each-other
			if ( !( (coordBlock[ii] <= ROI->xyzctUB[ii]) && (coordBlock[ii] + header.blockSize[ii] - 1 >= ROI->xyzctLB[ii] ) ))
			{
				intersect = false;
				break;
			}
		}

		if (intersect == false)
			continue;//process another block

#ifdef DEBUG_PRINT_THREADS
		printf("Thread %d reading block %d out of %d total blocks\n", (int)(std::this_thread::get_id().hash()), (int)blockId_t, (int)numBlocks);
		fflush(stdout); // Will now print everything in the stdout buffer
#endif

		
		//uncompress block into temp bufferIn
		sizeCompressed = header.getBlockCompressedSizeBytes(blockId_t);
		offset = header.getBlockOffset(blockId_t);

		fid.seekg(offsetHeaderBytes + offset , ios::beg);
		fid.read(bufferFile, sizeCompressed);//read compressed block

		//apply decompression to block
		switch (header.compressionType)
		{
		case 0://no compression
			gcount = sizeCompressed;
			memcpy(bufferIn, bufferFile, gcount);
			break;
		case 1://bzip2
		{
				   gcount = blockSizeBytes;
				   int ret = BZ2_bzBuffToBuffDecompress(bufferIn, &gcount, bufferFile, sizeCompressed, 0, 0);				   
				   if (ret != BZ_OK)
				   {
					   std::cout << "ERROR: workerfunc: decompressing data at block " << blockId_t << std::endl;
					   *errFlag = 2;
					   gcount = 0;
				   }
				   break;
		}
		default:
			std::cout << "ERROR: workerfunc: decompression type not implemented" << std::endl;
			*errFlag = 5;
			sizeCompressed = 0;
		}



		//-------------------parse bufferIn to bufferOut image buffer-----------------------------------
		//------------------intersection of two ROI (blopck and image ROI) is another ROI, so we just need to calculate the intersection and its offsets

		//calculate block size in case we had border block
		uint32_t blockSizeAuxCum[KLB_DATA_DIMS];
		blockSizeAux[0] = std::min(header.blockSize[0], (uint32_t)(header.xyzct[0] - coordBlock[0]));
		blockSizeAuxCum[0] = bytesPerPixel;
		for (int ii = 1; ii < KLB_DATA_DIMS; ii++)
		{
			blockSizeAux[ii] = std::min(header.blockSize[ii], (uint32_t)(header.xyzct[ii] - coordBlock[ii]));
			blockSizeAuxCum[ii] = blockSizeAuxCum[ii - 1] * blockSizeAux[ii - 1];
		}

		//calculate upper and lower coordinate of the intersection between block and ROI wrt to block dimensions, so it is bounded by [0, blockSizeAux[ii])
		uint32_t bLB[KLB_DATA_DIMS], bUB[KLB_DATA_DIMS], bcount[KLB_DATA_DIMS];
		for (int ii = 0; ii < KLB_DATA_DIMS; ii++)
		{
			if (coordBlock[ii] >= ROI->xyzctLB[ii])
				bLB[ii] = 0;
			else
				bLB[ii] = ROI->xyzctLB[ii] - coordBlock[ii];

			if (coordBlock[ii] + blockSizeAux[ii] - 1 <= ROI->xyzctUB[ii])
				bUB[ii] = blockSizeAux[ii];
			else
				bUB[ii] = ROI->xyzctUB[ii] - coordBlock[ii] + 1;

			blockSizeAux[ii] = bUB[ii] - bLB[ii];//one we have the cum, we do not need the individual dimensions. What we need is the size of the intersection with the ROI
		}
		memcpy(bcount, bLB, sizeof(uint32_t)* KLB_DATA_DIMS);

		//calculate starting offset in the buffer in ROI space
		offsetBuffer = 0;
		offsetBufferBlock = 0;
		for (int ii = 0; ii < KLB_DATA_DIMS; ii++)
		{
			offsetBuffer += (coordBlock[ii] + bLB[ii] - ROI->xyzctLB[ii]) * xyzctCum[ii];
			offsetBufferBlock += bLB[ii] * blockSizeAuxCum[ii];
		}		
		

		//copy block into local buffer bufferIn
		int auxDim = 1;
		const size_t bufferCopySize = bytesPerPixel * blockSizeAux[0];
		const size_t bufferInOffset = blockSizeAuxCum[1];
		char* bufferInAux = &(bufferIn[offsetBufferBlock]);
		while (auxDim < KLB_DATA_DIMS)
		{
			//copy fastest moving coordinate all at once for efficiency
			//memcpy(&(bufferOut[offsetBuffer]), &(bufferIn[offsetBufferBlock]), bufferCopySize);
			memcpy(&(bufferOut[offsetBuffer]), bufferInAux, bufferCopySize);
			bufferInAux += bufferInOffset;

			//increment counter
			auxDim = 1;
			bcount[auxDim]++;
			offsetBuffer += xyzctCum[auxDim];//update offset for output buffer		
			offsetBufferBlock += blockSizeAuxCum[auxDim];

			while (bcount[auxDim] == bUB[auxDim])
			{
				offsetBuffer -= blockSizeAux[auxDim] * xyzctCum[auxDim];//update buffer				
				offsetBufferBlock -= blockSizeAux[auxDim] * blockSizeAuxCum[auxDim];
				bcount[auxDim] = bLB[auxDim];
				auxDim++;
				if (auxDim == KLB_DATA_DIMS)
					break;
				bcount[auxDim]++;
				offsetBuffer += xyzctCum[auxDim]; //update buffer
				offsetBufferBlock += blockSizeAuxCum[auxDim];

				bufferInAux = &(bufferIn[offsetBufferBlock]);//with ROI it is not a constant increment
			}
		}
		//-------------------end of parse bufferIn to bufferOut image buffer-----------------------------------

		

#ifdef DEBUG_PRINT_THREADS
		printf("Thread %d finished decompressing block %d into %d bytes\n", (int)(std::this_thread::get_id().hash()), (int)blockId_t, (int)gcount);
		fflush(stdout); // Will now print everything in the stdout buffer
#endif
	}


	//release memory
	fid.close();
	delete[] bufferIn;
	delete[] bufferFile;

}

//======================================================
void klb_imageIO::blockUncompressorInMem(char* bufferOut, std::atomic<uint64_t>	*blockId, char* bufferImgFull, int *errFlag)
{
	*errFlag = 0;
	

	//define variables
	std::uint64_t blockId_t;//to know which block we are processing
	unsigned int sizeCompressed, gcount;
	std::uint64_t offset;//size of block in bytes after compression


	const size_t bytesPerPixel = header.getBytesPerPixel();
	uint32_t blockSizeBytes = bytesPerPixel;	
	uint64_t dimsBlock[KLB_DATA_DIMS];//number of blocks on each dimension
	uint64_t coordBlock[KLB_DATA_DIMS];//coordinates (in block space). blockId_t = coordBblock[0] + dimsBblock[0] * coordBlock[1] + dimsBblock[0] * dimsBblock[1] * coordBlock[2] + ...
	uint64_t offsetBuffer;//starting offset for each buffer within ROI
	uint32_t blockSizeAux[KLB_DATA_DIMS];//for border cases where the blocksize might be different
	uint64_t xyzctCum[KLB_DATA_DIMS];//to calculate offsets for each dimension in THE ROI
	uint64_t offsetHeaderBytes = header.getSizeInBytes();

	xyzctCum[0] = bytesPerPixel;
	for (int ii = 0; ii < KLB_DATA_DIMS; ii++)
	{
		blockSizeBytes *= header.blockSize[ii];
		dimsBlock[ii] = ceil((float)(header.xyzct[ii]) / (float)(header.blockSize[ii]));
		if (ii > 0)
			xyzctCum[ii] = xyzctCum[ii - 1] * header.xyzct[ii - 1];
	}

	std::uint64_t numBlocks = header.getNumBlocks();
	char* bufferIn = new char[blockSizeBytes];//temporary storage for decompressed block
	char* bufferPtr;//pointer to preloaded compressed file in memory

	//main loop to keep processing blocks while they are available
	while (1)
	{
		//get the blockId resource		
		blockId_t = atomic_fetch_add(blockId, 1);

		//check if we have more blocks
		if (blockId_t >= numBlocks)
			break;

		//calculate coordinate (in block space)
		std::uint64_t blockIdx_aux = blockId_t;
		for (int ii = 0; ii < KLB_DATA_DIMS; ii++)
		{
			coordBlock[ii] = blockIdx_aux % dimsBlock[ii];
			blockIdx_aux -= coordBlock[ii];
			blockIdx_aux /= dimsBlock[ii];
			coordBlock[ii] *= header.blockSize[ii];//parsing coordinates to image space (not block anymore)
		}		

#ifdef DEBUG_PRINT_THREADS
		printf("Thread %d reading block %d out of %d total blocks\n", (int)(std::this_thread::get_id().hash()), (int)blockId_t, (int)numBlocks);
		fflush(stdout); // Will now print everything in the stdout buffer
#endif


		//uncompress block into temp bufferIn
		sizeCompressed = header.getBlockCompressedSizeBytes(blockId_t);
		offset = header.getBlockOffset(blockId_t);

		bufferPtr = &(bufferImgFull[offsetHeaderBytes + offset]);		

		//apply decompression to block
		switch (header.compressionType)
		{
		case 0://no compression
			gcount = sizeCompressed;
			memcpy(bufferIn, bufferPtr, gcount);
			break;
		case 1://bzip2
		{
				   gcount = blockSizeBytes;
				   int ret = BZ2_bzBuffToBuffDecompress(bufferIn, &gcount, bufferPtr, sizeCompressed, 0, 0);
				   if (ret != BZ_OK)
				   {
					   std::cout << "ERROR: workerfunc: decompressing data at block " << blockId_t << std::endl;
					   *errFlag = 2;
					   gcount = 0;
				   }
				   break;
		}
		default:
			std::cout << "ERROR: workerfunc: decompression type not implemented" << std::endl;
			*errFlag = 5;
			sizeCompressed = 0;
		}



		//-------------------parse bufferIn to bufferOut image buffer-----------------------------------		

		//calculate block size in case we had border block				
		blockSizeAux[0] = std::min(header.blockSize[0], (uint32_t)(header.xyzct[0] - coordBlock[0]));
		for (int ii = 1; ii < KLB_DATA_DIMS; ii++)
		{
			blockSizeAux[ii] = std::min(header.blockSize[ii], (uint32_t)(header.xyzct[ii] - coordBlock[ii]));
		}
		
				

		//calculate starting offset in the buffer in image space
		offsetBuffer = 0;
		for (int ii = 0; ii < KLB_DATA_DIMS; ii++)
		{
			offsetBuffer += coordBlock[ii] * xyzctCum[ii];//xyzct already has bytesPerPixel
		}

		//copy block into local buffer bufferIn
		int auxDim = 1;
		uint32_t bcount[KLB_DATA_DIMS];
		memset(bcount, 0, sizeof(uint32_t)* KLB_DATA_DIMS);
		const size_t bufferCopySize = bytesPerPixel * blockSizeAux[0];
		char* bufferInPtr = bufferIn;
		while (auxDim < KLB_DATA_DIMS)
		{
			
			//copy fastest moving coordinate all at once for efficiency
			memcpy(&(bufferOut[offsetBuffer]), bufferInPtr, bufferCopySize);
			bufferInPtr += bufferCopySize;

			//increment counter
			auxDim = 1;
			bcount[auxDim]++;
			offsetBuffer += xyzctCum[auxDim];//update offset for output buffer. xyzct already has bytesPerPixel		

			while (bcount[auxDim] == blockSizeAux[auxDim])
			{
				offsetBuffer -= blockSizeAux[auxDim] * xyzctCum[auxDim];//update buffer				
				bcount[auxDim] = 0;
				auxDim++;
				if (auxDim == KLB_DATA_DIMS)
					break;
				bcount[auxDim]++;
				offsetBuffer += xyzctCum[auxDim]; //update buffer.xyzct already has bytesPerPixel
			}
		}
		//-------------------end of parse bufferIn to bufferOut image buffer-----------------------------------



#ifdef DEBUG_PRINT_THREADS
		printf("Thread %d finished decompressing block %d into %d bytes\n", (int)(std::this_thread::get_id().hash()), (int)blockId_t, (int)gcount);
		fflush(stdout); // Will now print everything in the stdout buffer
#endif
	}


	//release memory
	delete[] bufferIn;

}

//=========================================================================
//writes compressed blocks sequentially as they become available (in order) from the workers
void klb_imageIO::blockWriter(std::string filenameOut, int* g_blockSize, int* g_blockThreadId, klb_circular_dequeue** cq, int* errFlag)
{
	*errFlag = 0;
	std::int64_t nextBlockId = 0, offset = 0;
	std::uint64_t numBlocks = header.getNumBlocks();
	header.blockOffset.resize(numBlocks);//just in case it has not been setup

//#define USE_MEM_BUFFER //uncomment this line to use a large memory buffer before writing to file. It is slower since C++ write already buffers ofstream before flushing

#ifdef USE_MEM_BUFFER
	//buffer to avoid writing to disk all the time
	int bufferMaxSize = std::min( header.getImageSizeBytes() / 10, (uint64_t) (500 * 1048576));//maximum is 500MB or 10th of the original image size
	char* bufferMem = new char[bufferMaxSize];
	int bufferOffset = 0;
#endif	

	//open output file
	std::ofstream fout(filenameOut.c_str(), std::ios::binary | std::ios::out);
	if (fout.is_open() == false)
	{
		std::cout << "ERROR: file " << filenameOut << " could not be opened" << std::endl;
		nextBlockId = numBlocks;
		*errFlag = 5;
	}

	//write header
	header.writeHeader(fout);

	// loop until end is signaled	
	std::mutex              g_lockqueue;//mutex for the condition variable (dummy one)
	std::unique_lock<std::mutex> locker(g_lockqueue);//acquires the lock but this is the only thread using it. We cannot have condition_variables without a mutex

	std::int64_t blockSize;
	while (nextBlockId < numBlocks)
	{

#ifdef DEBUG_PRINT_THREADS
		printf("Writer trying to append block %d out of %d\n", (int)nextBlockId, (int)numBlocks);
		fflush(stdout); // Will now print everything in the stdout buffer
#endif
		g_queuecheck.wait(locker, [&](){return (g_blockSize[nextBlockId] >= 0 && g_blockThreadId[nextBlockId] >= 0); });//releases the lock until notify. If condition is not satisfied, it waits again

#ifdef DEBUG_PRINT_THREADS
		printf("Writer appending block %d out of %d with %d bytes\n", (int)nextBlockId, (int) numBlocks,g_blockSize[nextBlockId]);
		fflush(stdout); // Will now print everything in the stdout buffer
#endif
				
		//write block
		blockSize = g_blockSize[nextBlockId];

#ifdef USE_MEM_BUFFER
		//use memory large memory buffer
		if (bufferOffset + blockSize > bufferMaxSize)//we need to flush the buffer
		{
			fout.write(bufferMem, bufferOffset);
			bufferOffset = 0;
		}
		
		//add block to the memory buufer
		memcpy(&(bufferMem[bufferOffset]), cq[g_blockThreadId[nextBlockId]]->getReadBlock(), blockSize);
		bufferOffset += blockSize;
#else
		fout.write(cq[g_blockThreadId[nextBlockId]]->getReadBlock(), blockSize);
#endif
		//now we can release data
		cq[g_blockThreadId[nextBlockId]]->popReadBlock();
		offset += blockSize;

		//update header blockOffset
		header.blockOffset[nextBlockId] = offset;//at the end, so we can recover length for all blocks

		//update variables
		nextBlockId++;
	}
#ifdef USE_MEM_BUFFER
	//flush the rest of the buffer
	fout.write(bufferMem, bufferOffset);
#endif
	//update header.blockOffset
	fout.seekp(header.getSizeInBytesFixPortion(), ios::beg);
	fout.write((char*)(&(header.blockOffset[0])) ,header.blockOffset.size() * sizeof(std::uint64_t) );

	//close file
	fout.close();
#ifdef USE_MEM_BUFFER
	delete[] bufferMem;
#endif
}






//=======================================================

klb_imageIO::klb_imageIO()
{
	numThreads = std::thread::hardware_concurrency();
}

klb_imageIO::klb_imageIO(const std::string &filename_)
{
	filename = filename_;//it could be used as output or input file
	numThreads = std::thread::hardware_concurrency();
}



//=================================================

int klb_imageIO::writeImage(const char* img, int numThreads)
{

	//redirect standard out
#ifdef DEBUG_PRINT_THREADS
	freopen("E:/temp/cout_klb_imageIO.txt", "w", stdout);
	cout << "Redirecting stdout for klb_imageIO::writeImage" << endl;
#endif

	if (numThreads <= 0)//use maximum available
		numThreads = std::thread::hardware_concurrency();


#ifdef PROFILE_COMPRESSION
	//reset counter
	atomic_store(&g_countCompression, 0);
#endif

	//safety checks to avoid blocksize too large
	for (int ii = 0; ii < KLB_DATA_DIMS; ii++)
		header.blockSize[ii] = std::min(header.blockSize[ii], header.xyzct[ii]);//block size cannot be larger than dimensions

	//set constants
	const uint32_t blockSizeBytes = header.getBlockSizeBytes();
	const uint64_t fLength = header.getImageSizeBytes();			
	const std::uint64_t numBlocks = header.calculateNumBlocks();
	
	header.blockOffset.resize(numBlocks);


	//number of threads should not be highr than number of blocks (in case somebody set block size too large)
	numThreads = std::min((std::uint64_t) numThreads, numBlocks);

	uint64_t blockId = 0;//counter shared all workers so each worker thread knows which block to readblockId = 0;
	int* g_blockSize = new int[numBlocks];//number of bytes (after compression) to be written. If the block has not been compressed yet, it has a -1 value
	int* g_blockThreadId = new int[numBlocks];//indicates which thread wrote the nlock so the writer can find the appropoate circular queue
	for (std::int64_t ii = 0; ii < numBlocks; ii++)
	{
		g_blockSize[ii] = -1;
		g_blockThreadId[ii] = -1;
	}

	//generate circular queues to exchange blocks between read write
	int numBlocskPerQueue = std::max(numThreads, 5);//total memory = numThreads * blockSizeBytes * numBlocksPerQueue so it should be low. Also, not many blocks should be queued in general
	numBlocskPerQueue = std::min(numBlocskPerQueue, 20);
	numBlocskPerQueue = std::min(numBlocskPerQueue, (int)iDivUp(numBlocks, (std::uint64_t)numThreads));

	//TODO: find the best method to adjust this number automatically
	const uint32_t maxBlockSizeBytesCompressed = maximumBlockSizeCompressedInBytes();
	klb_circular_dequeue** cq = new klb_circular_dequeue*[numThreads];
	for (int ii = 0; ii < numThreads; ii++)
		cq[ii] = new klb_circular_dequeue(maxBlockSizeBytesCompressed, numBlocskPerQueue);

	// start the thread to write
	int errFlagW = 0;
	std::thread writerthread(&klb_imageIO::blockWriter, this, filename, g_blockSize, g_blockThreadId, cq, &errFlagW);

	// start the working threads
	std::vector<std::thread> threads;
	std::vector<int> errFlagVec(numThreads, 0);
	for (int i = 0; i < numThreads; ++i)
	{
		threads.push_back(std::thread(&klb_imageIO::blockCompressor, this, img, g_blockSize, &blockId, g_blockThreadId, cq[i], i, &(errFlagVec[i])));
	}

	//wait for the workers to finish
	for (auto& t : threads)
	{
		t.join();
	}

	//wait for the writer
	writerthread.join();

	//release memory
	delete[] g_blockSize;
	delete[] g_blockThreadId;
	for (int ii = 0; ii < numThreads; ii++)
		delete cq[ii];
	delete[] cq;

	if (errFlagW != 0)
		return errFlagW;
	for (int ii = 0; ii < numThreads; ii++)
	{
		if (errFlagVec[ii] != 0)
			return errFlagVec[ii];
	}


#ifdef PROFILE_COMPRESSION
	long long auxChrono = atomic_load(&g_countCompression);
	cout << "Average time spent in compression per thread is =" << auxChrono / numThreads << " ms"<<endl;
#endif

	return 0;//TODO: catch errors from threads (especially opening file)
}

//=================================================

int klb_imageIO::readImage(char* img, const klb_ROI* ROI, int numThreads)
{
	if (numThreads <= 0)//use maximum available
		numThreads = std::thread::hardware_concurrency();
	
	const std::uint64_t numBlocks = header.calculateNumBlocks();

	//number of threads should not be highr than number of blocks (in case somebody set block size too large)
	numThreads = std::min((std::uint64_t) numThreads, numBlocks);	
	
	std::atomic<uint64_t> blockId;
	atomic_store(&blockId, 0);

	// start the working threads
	std::vector<std::thread> threads;
	std::vector<int> errFlagVec(numThreads, 0);
	for (int i = 0; i < numThreads; ++i)
	{
		threads.push_back(std::thread(&klb_imageIO::blockUncompressor, this, img, &blockId, ROI, &(errFlagVec[i])));
	}

	//wait for the workers to finish
	for (auto& t : threads)
		t.join();

	//release memory
	
	for (int ii = 0; ii < numThreads; ii++)
	{
		if (errFlagVec[ii] != 0)
			return errFlagVec[ii];
	}
	return 0;//TODO: catch errors from threads (especially opening file)
}

//=================================================

int klb_imageIO::readImageFull(char* imgOut, int numThreads)
{	

	if (numThreads <= 0)//use maximum available
		numThreads = std::thread::hardware_concurrency();

	const std::uint64_t numBlocks = header.calculateNumBlocks();

	//number of threads should not be highr than number of blocks (in case somebody set block size too large)
	numThreads = std::min((std::uint64_t) numThreads, numBlocks);

	std::atomic<uint64_t>	g_blockId;
	atomic_store(&g_blockId, 0);

	//read compressed file from disk into memory
	//open file to read elements
	ifstream fid(filename.c_str(), ios::binary | ios::in);
	if (fid.is_open() == false)
	{
		cout << "ERROR: readImageFull: reading file " << filename << endl;
		return 3;
	}

	//auto t1 = Clock::now();

	char* imgIn = new char[header.getCompressedFileSizeInBytes()];
	fid.read(imgIn, header.getCompressedFileSizeInBytes());
	fid.close();

	//auto t2 = Clock::now();
	//std::cout << "=======DEBUGGING:took " << std::chrono::duration_cast<std::chrono::milliseconds>(t2 - t1).count() << " ms to read file from disk memory by a single thread" << std::endl;

	// start the working threads
	std::vector<std::thread> threads;
	std::vector<int> errFlagVec(numThreads, 0);
	for (int i = 0; i < numThreads; ++i)
	{
		threads.push_back(std::thread(&klb_imageIO::blockUncompressorInMem, this, imgOut, &g_blockId, imgIn, &(errFlagVec[i])));
	}

	//wait for the workers to finish
	for (auto& t : threads)
		t.join();

	//release memory
	delete[] imgIn;
	for (int ii = 0; ii < numThreads; ii++)
	{
		if (errFlagVec[ii] != 0)
			return errFlagVec[ii];
	}
	return 0;//TODO: catch errors from threads (especially opening file)
}


//======================================================
std::uint32_t klb_imageIO::maximumBlockSizeCompressedInBytes()
{
	uint32_t blockSizeBytes = header.getBlockSizeBytes();

	switch (header.compressionType)
	{
	case 0://no compression
		//nothing to do
		break;
	case 1://bzip2
		/*
			From man page: Compression is  always  performed,  even	 if  the  compressed  file  is
			slightly	 larger	 than the original.Files of less than about one hun -
			dred bytes tend to get larger, since the compression  mechanism	has  a
			constant	 overhead  in  the region of 50 bytes.Random data(including
			the output of most file compressors) is coded at about  8.05  bits  per
			byte, giving an expansion of around 0.5%.
		*/
		blockSizeBytes = ceil(((float)blockSizeBytes) * 1.05f + 50.0f );
		break;
	
	default:
		std::cout << "ERROR: maximumBlockSizeCompressedInBytes: compression type not implemented" << std::endl;
		blockSizeBytes = 0;
	}

	return blockSizeBytes;
}