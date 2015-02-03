# Keller Lab Block file type (.klb) source C++11 code and API  #

The KLB is a file format developed at the Keller Lab at Janelia Research Campus to efficiently store and retrieve large 5D images (>4GB) using lossless compression. The format tries to combine the JPEG2000 lossless compression levels with the block flexibility offered by HDF5 to access arbitrary regions of interest. Inspired by Paralle BZIP2, a common Linux command, we partition images into blocks and each block is compressed in parallel using the Bzip2. Both reading and writing are parallelized and scale linearly with the number of cores making it much faster than JPEG2000 and HDF5 in common multi-core machines. 

All the code has been developed using standard C++11 which makes it easy to compile across platforms. Moreover, a simple API allows to wrap the open-source C++ code with other languages such as Python, Java, Fiji or Matlab. The KLB format also allows future extensions, such as new compression formats. At the time of writing of this readme file, compressionType supports two file formats, ‘no compression’ and ‘pbzip2’. Thus, if the user wishes to use other types of compression algorithms for different data sets, it is straight-forward to include support for additional algorithms. This modification simply requires an extra line in the source code to indicate the location of the code for the new compression library.


## KLB header format ##

The KLB header contains the following items stored in binary format:




- uint32	xyzct[5]: image dimensions (x, y, z, channels, time points)

- float32 pixelSize[5]: sampling of each dimension (in µm, a.u., seconds)

- uint8 dataType: look-up-table for data type (uint8, uint16, etc.)

- uint8 compressionType: look-up-table for compression type (none, pbzip2, etc.)

- uint32	blockSize[5]: block size used to partition the data in each dimension

- uint64	blockOffset[Nb]: offset (in bytes) of each block in the file


Note: the offset information stored here allows us to retrieve individual blocks in order to read arbitrary regions of interest efficiently.

The KLB file format is implemented for up to five-dimensional data (three spatial dimensions, one channel dimension, one time dimension). All items listed above have a fixed length in bytes, except for the last item (blockOffset), which is data-dependent. The number of blocks, Nb, is equal to the product of ceil (xyzct[i]/blockSize[i] ) for i = 1...5. The API has a function call to retrieve this number.





