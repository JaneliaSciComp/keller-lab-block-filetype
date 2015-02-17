%module Klb

%include "typemaps.i"
%include "stdint.i"
%include "std_string.i"
%include "carrays.i"
%include "exception.i"

// Java-specific includes
#if SWIGJAVA
	%include "arrays_java.i"
	%include "various.i"
	%include "enums.swg"
#endif


// don't expose block offsets
%ignore klb_image_header::Nb;
%ignore klb_image_header::blockOffset;
%ignore klb_image_header::getBlockOffset;
%ignore klb_image_header::resizeBlockOffset;

// don't expose C read/write operations
%ignore klb_image_header::readHeader(std::istream &fid);
%ignore klb_image_header::writeHeader(std::ostream &fid);
%ignore klb_image_header::writeHeader(FILE* fid);

// hide unnecessary members and functions
%ignore KLB_METADATA_SIZE;
%ignore klb_image_header::operator=;
%ignore klb_image_header::cloneMetadata;
%ignore klb_image_header::getSizeInBytes;
%ignore klb_image_header::getSizeInBytesFixPortion;
%ignore klb_image_header::getBlockCompressedSizeBytes;
%ignore klb_image_header::getCompressedFileSizeInBytes;
%ignore klb_image_header::setDefaultBlockSize;
%ignore klb_image_header::setOptimalBlockSizeInBytes;

// temporarily disable metadata wildcard
%ignore klb_image_header::setMetadata;
%ignore klb_image_header::getMetadataPtr;

// access metadata wildcard through properly named accessors
%ignore metadata;
%rename(getMetadata) getMetadataPtr;

// friendlier class names
%rename(KlbImageHeader) klb_image_header;
%rename(KlbRoi) klb_ROI;
%rename(KlbImageIO) klb_imageIO;
%rename(KlbDataType) KLB_DATA_TYPE;
%rename(KlbCompressionType) KLB_COMPRESSION_TYPE;


%{
// Includes header and wrapper code
#include "klb_imageHeader.h"
#include "klb_ROI.h"
#include "klb_imageIO.h"
%}

// Parse header file to generate wrappers
%include "klb_imageHeader.h"
%include "klb_ROI.h"
%include "klb_imageIO.h"


// Java wrapper auto-loading of bundled native libs
// Requires class NativeLibraryLoader provided in soures.
// To compile, the following import statement must be
// added to class org.janelia.simview.klb.jni.KlbJni:
// import org.janelia.simview.util.NativeLibraryLoader;
%pragma(java) jniclasscode=%{
    // auto-load native libs
    static {
        final NativeLibraryLoader loader = new NativeLibraryLoader();
        try {
            loader.unpackAndLoadFromResources("klb");
            loader.unpackAndLoadFromResources("klb-jni");
        } catch (Exception e) {
            throw new UnsatisfiedLinkError("[KLB] Failed to unpack native KLB libraries from jar.\n" + e.getMessage());
        }
    }
%}