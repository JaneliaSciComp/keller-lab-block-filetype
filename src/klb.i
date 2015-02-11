%module Klb

%include "typemaps.i"
%include "stdint.i"
%include "std_string.i"
%include "arrays_java.i"
%include "various.i"

%ignore klb_image_header::blockOffset;
%ignore klb_image_header::operator=;
%ignore klb_image_header::readHeader(std::istream &fid);
%ignore klb_image_header::writeHeader(std::ostream &fid);

%rename(KlbImageHeader) klb_image_header;
%rename(KlbRoi) klb_ROI;
%rename(KlbImageIO) klb_imageIO;

%{
/* Includes header and wrapper code */
#include "klb_imageHeader.h"
#include "klb_ROI.h"
#include "klb_imageIO.h"
%}

/* Parse header file to generate wrappers */
%include "klb_imageHeader.h"
%include "klb_ROI.h"
%include "klb_imageIO.h"
