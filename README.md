# Keller Lab Block file type (.klb) C++11 source code and API  #

The KLB is a file format developed at the Keller Lab (Janelia Research Campus) for efficiently storing and retrieving large 5D images (>4GB) using lossless compression. The purpose of this format is to combine JPEG2000-like lossless compression levels with the block flexibility offered by HDF5 for accessing arbitrary regions of interest. Inspired by Parallel BZip2, a common Linux module, we partition images into blocks and compress blocks in parallel using Bzip2. Both reading and writing are parallelized and scale linearly with the number of compute cores, thus making KLB significantly faster than JPEG2000 and HDF5 on common multi-core machines.

All code has been developed using standard C++11 which makes it easy to compile KLB code across platforms. Moreover, a simple API allows wrapping the open-source C++ code with other languages such as Python, Java, Fiji or Matlab. The KLB format also allows future extensions, such as inclusion of new compression formats. At the time of writing of this readme file, the header item compressionType supports two file formats, ‘no compression’ and ‘pbzip2’. If the user wishes to utilize other types of compression algorithms as well, it is straight-forward to include support for such algorithms through the infrastructure associated with compressionType. Such a modification simply requires a corresponding line in the source code, indicating the location of the code for the new compression library.


## KLB installation and compilation ##

This software package contains the C++11 source code for the KLB file format implementation as well as wrappers for Matlab and Java. The folder *bin* contains the precompiled static and shared (DLL) libraries for Windows 7 64-bit as well as a simple executable test_KLBIO.exe for testing read/write operations. The source code of this executable represents a good example of how to use the API for the KLB file format. For Windows 7 64-bit, we also provide precompiled MEX files in the folder *matlabWrapper*.

The code has been tested on various Unix systems. However, Linux and Mac OS users need to compile both the source code and the Matlab wrappers to obtain libraries and executables. For the first part, a CMake file is available in the folder *src*. For the second part, the folder *matlabWrapper* contains the script compileMex.m for generating MEX files. The C++ libraries need to be compiled in release mode before compiling the MEX files.


## Java Native Interface (JNI) ##

The KLB API is exposed on the Java side through a JNI wrapper, included in the *javaWrapper* subfolder. It can be built with Maven, includes compiled native libraries for Windows and Linux (both 64-bit) and will eventually be available as an artifact on a Maven repository. ImageJ users on supported platforms can simply install KLB support by following the update site (see below).


## Installation via ImageJ update site ##

KLB and its ImageJ integration are available through an ImageJ update site at http://sites.imagej.net/SiMView/. For information on how to follow an update site please see http://wiki.imagej.net/How_to_follow_a_3rd_party_update_site. Currently supported platforms are Windows and Linux (both 64-bit). Users on other platforms have to build the native libraries first.


## Build JNI library from source ##

1) Install Maven

2) Navigate to the *javaWrapper* subfolder

3) Run "mvn clean package"

4) JAR file will be built at "javaWrapper/target/klb-[version].jar"



## KLB header format ##

The KLB header contains the following items stored in binary format:

- uint8 headerVersion: KLB header version

- uint32 xyzct[5]: image dimensions (x, y, z, channels, time points)

- float32 pixelSize[5]: sampling of each dimension (in units of µm, index count, seconds)

- uint8 dataType: look-up-table for data type (uint8, uint16, etc.)

- uint8 compressionType: look-up-table for compression type (none, pbzip2, etc.)

- char metadata[256]: character block providing space for user-defined metadata

- uint32 blockSize[5]: block size used to partition the data in each dimension

- uint64 blockOffset[Nb]: offset (in bytes) of each block in the file

Note: offset information stored here enables efficiently retrieving individual blocks.

The KLB file format is implemented for data with up to five dimensions (three spatial dimensions, one channel dimension, one time dimension). All items listed above have a fixed length in bytes, except for the last item (blockOffset), which is data-dependent. The number of blocks, Nb, is equal to the product of ceil(xyzct[i]/blockSize[i]) for i = 1...5. The API provides a function call to retrieve this number.