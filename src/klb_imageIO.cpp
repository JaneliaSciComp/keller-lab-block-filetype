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
#include <stdlib.h>     /* div, div_t */
#include "klb_imageIO.h"
#include "bzlib.h"



//#define DEBUG_PRINT_THREADS

using namespace std;

//static variables for the class
std::mutex				klb_imageIO::g_lockblockId;//so each worker reads a unique blockId
std::condition_variable	klb_imageIO::g_queuecheck;//to notify writer that blocks are ready



//========================================================
//======================================================
void klb_imageIO::blockCompressor(char* buffer, int* g_blockSize, uint64_t *blockId, int* g_blockThreadId, klb_circular_dequeue* cq, int threadId)
{
	const int BWTblockSize = 9;//maximum compression
	std::uint64_t blockId_t;
	int gcount;//read bytes
	unsigned int sizeCompressed;//size of block in bytes after compression

	size_t bytesPerPixel = header.getBytesPerPixel();
	uint32_t blockSizeBytes = bytesPerPixel;
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

	std::uint64_t numBlocks = header.getNumBlocks();


	//main loop to keep processing blocks while they are available
	while (1)
	{
		//get the blockId resource
		g_lockblockId.lock();
		blockId_t = (*blockId);
		(*blockId)++;
		g_lockblockId.unlock();


#ifdef DEBUG_PRINT_THREADS
		printf("Thread %d reading block %d\n", (int)(std::this_thread::get_id().hash()), (int)blockId_t);
#endif

		//check if we can access data or we cannot read longer
		if (blockId_t >= numBlocks)
			break;

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
		offsetBuffer = bytesPerPixel;
		for (int ii = 0; ii < KLB_DATA_DIMS; ii++)
		{
			offsetBuffer *= coordBlock[ii] * xyzctCum[ii];
		}

		//copy block into local buffer bufferIn
		gcount = 0;
		uint32_t bcount[KLB_DATA_DIMS];//to count elements in the block
		memset(bcount, 0, sizeof(uint32_t)* KLB_DATA_DIMS);
		int auxDim = 1;
		while (auxDim < KLB_DATA_DIMS)
		{
			//copy fastest moving coordinate all at once for efficiency
			memcpy(bufferIn, &(buffer[offsetBuffer]), bytesPerPixel * blockSizeAux[0]);
			gcount += blockSizeAux[0];

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
		gcount *= bytesPerPixel;

		//-------------------end of read block-----------------------------------

		//decide address where we write the compressed block output						
		char* bufferOutPtr = cq->reserveWriteBlock(); //this operation is thread safe	


		//apply compression to block
		switch (header.compressionType)
		{
		case 0://no compression
			sizeCompressed = gcount;
			memcpy(bufferOutPtr, bufferIn, sizeCompressed);//fid.gcount():Returns the number of characters extracted by the last unformatted input operation performed on the object
			break;
		case 1://bzip2
		{
				   sizeCompressed = blockSizeBytes;
				   // compress the memory buffer (blocksize=9*100k, verbose=0, worklevel=30)				  
				   int ret = BZ2_bzBuffToBuffCompress(bufferOutPtr, &sizeCompressed, bufferIn, gcount, BWTblockSize, 0, 30);
				   if (ret != BZ_OK)
				   {
					   std::cout << "ERROR: workerfunc: compressing data at block" << blockId_t << std::endl;
					   sizeCompressed = 0;
				   }
				   break;
		}
		default:
			std::cout << "ERROR: workerfunc: compression type not implemented" << std::endl;
			sizeCompressed = 0;
		}

		//signal blockWriter that this block can be writen
		//std::unique_lock<std::mutex> locker(g_lockqueue);//adquires the lock
		g_blockSize[blockId_t] = sizeCompressed;//I don't really need the lock to modify this. I only need to singal the condition variable
		g_blockThreadId[blockId_t] = threadId;
		g_queuecheck.notify_one();

#ifdef DEBUG_PRINT_THREADS
		printf("Thread %d finished compressing block %d into %d bytes\n", (int)(std::this_thread::get_id().hash()), (int)blockId_t, (int) sizeCompressed);
#endif
	}


	//release memory
	delete[] bufferIn;

}

//=========================================================================
//writes compressed blocks sequentially as they become available (in order) from the workers
void klb_imageIO::blockWriter(char* buffer, std::string filenameOut, int* g_blockSize, int* g_blockThreadId, klb_circular_dequeue** cq)
{
	std::int64_t nextBlockId = 0, offset = 0;
	std::uint64_t numBlocks = header.getNumBlocks();
	header.blockOffset.resize(numBlocks);//just in case it has not been setup

	//open output file
	std::ofstream fout(filenameOut.c_str(), std::ios::binary | std::ios::out);
	if (fout.is_open() == false)
	{
		std::cout << "ERROR: file " << filenameOut << " could not be opened" << std::endl;
		nextBlockId = numBlocks;
	}

	//write header
	header.writeHeader(fout);

	// loop until end is signaled	
	std::mutex              g_lockqueue;//mutex for the condition variable (dummy one)
	std::unique_lock<std::mutex> locker(g_lockqueue);//acquires the lock but this is the only thread using it. We cannot have condition_variables without a mutex
	while (nextBlockId < numBlocks)
	{

#ifdef DEBUG_PRINT_THREADS
		printf("Writer trying to append block %d out of %d\n", (int)nextBlockId, (int)numBlocks);
#endif
		g_queuecheck.wait(locker, [&](){return (g_blockSize[nextBlockId] >= 0); });//releases the lock until notify. If condition is not satisfied, it waits again

#ifdef DEBUG_PRINT_THREADS
		printf("Writer appending block %d out of %d with %d bytes\n", (int)nextBlockId, (int) numBlocks,g_blockSize[nextBlockId]);
#endif

		//update header blockOffset
		header.blockOffset[nextBlockId] = offset;
				
		//write block
		fout.write(cq[g_blockThreadId[nextBlockId]]->getReadBlock(), g_blockSize[nextBlockId]);
		cq[g_blockThreadId[nextBlockId]]->popReadBlock();//now we can release data
		offset += (std::int64_t)(g_blockSize[nextBlockId]);

		//update variables
		nextBlockId++;
	}


	//update header.blockOffset
	fout.seekp(header.getSizeInBytesFixPortion(), ios::beg);
	fout.write((char*)(&(header.blockOffset[0])) ,header.blockOffset.size() * sizeof(std::uint64_t) );

	//close file
	fout.close();
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

int klb_imageIO::writeImage(char* img, int numThreads)
{
	if (numThreads <= 0)//use maximum available
		numThreads = std::thread::hardware_concurrency();

	const uint32_t blockSizeBytes = header.getBlockSizeBytes();
	const uint64_t fLength = header.getImageSizeBytes();
	const std::uint64_t numBlocks = header.calculateNumBlocks();
	
	header.blockOffset.resize(numBlocks);

	uint64_t blockId = 0;//counter shared all workers so each worker thread knows which block to readblockId = 0;
	int* g_blockSize = new int[numBlocks];//number of bytes (after compression) to be written. If the block has not been compressed yet, it has a -1 value
	int* g_blockThreadId = new int[numBlocks];//indicates which thread wrote the nlock so the writer can find the appropoate circular queue
	for (std::int64_t ii = 0; ii < numBlocks; ii++)
		g_blockSize[ii] = -1;


	//generate circular queues to exchange blocks between read write
	const int numBlocskPerQueue = 10;//total memory = numThreads * blockSizeBytes * numBlocksPerQueue so it should be low. Also, not many blocks should be queued in general
	klb_circular_dequeue** cq = new klb_circular_dequeue*[numThreads];
	for (int ii = 0; ii < numThreads; ii++)
		cq[ii] = new klb_circular_dequeue(blockSizeBytes, numBlocskPerQueue);

	// start the thread to write
	std::thread writerthread(&klb_imageIO::blockWriter, this, img, filename, g_blockSize, g_blockThreadId, cq);

	// start the working threads
	std::vector<std::thread> threads;
	for (int i = 0; i < numThreads; ++i)
	{
		threads.push_back(std::thread(&klb_imageIO::blockCompressor, this, img, g_blockSize, &blockId, g_blockThreadId, cq[i], i));
		
	}

	//wait for the workers to finish
	for (auto& t : threads)
		t.join();

	//wait for the writer
	writerthread.join();

	//release memory
	delete[] g_blockSize;
	delete[] g_blockThreadId;
	for (int ii = 0; ii < numThreads; ii++)
		delete cq[ii];
	delete cq;

	return 0;//TODO: catch errors from threads (especially opening file)
}
