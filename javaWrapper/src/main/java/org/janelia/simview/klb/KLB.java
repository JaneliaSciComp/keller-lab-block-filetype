package org.janelia.simview.klb;

import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.axis.CalibratedAxis;
import net.imagej.axis.LinearAxis;
import net.imglib2.Cursor;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.*;
import net.imglib2.img.cell.CellImg;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.img.cell.DefaultCell;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.*;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Arrays;

public abstract class KLB< T extends RealType< T > & NativeType< T > >
{
    public enum CompressionType
    {
        NONE, BZIP2, ZLIB
    }

    public class Header
    {
        /**
         * Image size, in order xyzct
         */
        public final long[] imageSize = new long[ 5 ];

        /**
         * Size of KLB blocks (units that are de-/compressed in parallel), in order xyzct
         */
        public final long[] blockSize = new long[ 5 ];

        /**
         * Physical pixel spacing, in order xyzct; standard units are microns and seconds
         */
        public final float[] pixelSpacing = new float[ 5 ];

        /**
         * Image data type, extends imglib2's RealType<T> & NativeType<T>
         */
        public T dataType;

        /**
         * Compression algorithm
         */
        public CompressionType compressionType;

        /**
         * Metadata header field
         */
        public byte[] metadata;

        @Override
        public String toString()
        {
            return String.format( "Data type: %s\nCompression type: %s\nImage size: %s\nBlock size: %s\nPixel spacing: %s\nMetadata (as ASCII): %s",
                    dataType.getClass().getSimpleName(), compressionType, Arrays.toString( imageSize ), Arrays.toString( blockSize ), Arrays.toString( pixelSpacing ), new String( metadata ).trim() );
        }
    }

    /**
     * Returns an instance of a concrete KLB implementation. Such instances should in turn have protected constructors
     * to prevent their direct instantiation.
     * This may eventually enable the use of multiple implementations (JNI and pure Java) side-by-side.
     *
     * @return
     */
    public static KLB newInstance()
    {
        return new KLBJNI();
    }

    /**
     * Number of threads to use for parallel de-/compression, default is all available processors
     */
    protected int numThreads = Runtime.getRuntime().availableProcessors();

    /**
     * Set number of threads to use for parallel de-/compression, default is all available processors
     */
    public void setNumThreads( final int n )
    {
        numThreads = n;
    }

    /**
     * Get number of threads used for parallel de-/compression
     */
    public int getNumThreads()
    {
        return numThreads;
    }

    /**
     * Read header from KLB file
     *
     * @param filePath file system path to KLB file
     * @return org.janelia.simview.KLB.Header instance
     * @throws IOException
     */
    public abstract Header readHeader( final String filePath ) throws IOException;


    /***********************************************************
     * Read entire image
     ***********************************************************/

    public abstract void readFullInPlace( final String filePath, final byte[] out )
            throws IOException;

    public abstract void readFullInPlace( final String filePath, final Buffer out )
            throws IOException;


    // The following functions provide default implementations for images that are better represented by
    // short[], int[], etc. than byte[]. The required conversions copy the image, so if possible, concrete
    // implementations of KLB should override these functions to read the corresponding data types directly.

    public void readFullInPlace( final String filePath, final short[] out )
            throws IOException
    {
        final ByteBuffer bytes = ByteBuffer.allocate( 2 * out.length );
        readFullInPlace( filePath, bytes.array() );
        bytes.asShortBuffer().get( out );
    }

    public void readFullInPlace( final String filePath, final int[] out )
            throws IOException
    {
        final ByteBuffer bytes = ByteBuffer.allocate( 4 * out.length );
        readFullInPlace( filePath, bytes.array() );
        bytes.asIntBuffer().get( out );
    }

    public void readFullInPlace( final String filePath, final long[] out )
            throws IOException
    {
        final ByteBuffer bytes = ByteBuffer.allocate( 8 * out.length );
        readFullInPlace( filePath, bytes.array() );
        bytes.asLongBuffer().get( out );
    }

    public void readFullInPlace( final String filePath, final float[] out )
            throws IOException
    {
        final ByteBuffer bytes = ByteBuffer.allocate( 4 * out.length );
        readFullInPlace( filePath, bytes.array() );
        bytes.asFloatBuffer().get( out );
    }

    public void readFullInPlace( final String filePath, final double[] out )
            throws IOException
    {
        final ByteBuffer bytes = ByteBuffer.allocate( 8 * out.length );
        readFullInPlace( filePath, bytes.array() );
        bytes.asDoubleBuffer().get( out );
    }

    /**
     * Read the entire image. Returns an instance of ArrayImg if possible, else CellImg.
     *
     * @param filePath file system path to KLB file
     * @return entire image, as ArrayImg or CellImg, as appropriate
     * @throws IOException
     */
    public ImgPlus< T > readFull( final String filePath )
            throws IOException
    {
        final Header header = readHeader( filePath );
        long numElements = 1;
        for ( final long d : header.imageSize ) {
            numElements *= d;
        }
        if ( numElements <= Integer.MAX_VALUE ) {
            final Img< T > img = readArrayImg( filePath, header, ( int ) numElements );
            return imgToImgPlus( img, header, filePath );
        } else {
            final Img< T > img = readCellImg( filePath, header );
            return imgToImgPlus( img, header, filePath );
        }
    }

    @SuppressWarnings( "unchecked" )
    private Img< T > readArrayImg( final String filePath, final Header header, final int numElements )
            throws IOException
    {
        final T dataType = header.dataType;
        final long[] imageSize = squeeze( header.imageSize );

        if ( dataType instanceof UnsignedByteType ) {
            final byte[] buffer = new byte[ numElements ];
            readFullInPlace( filePath, buffer );
            final Img img = ArrayImgs.unsignedBytes( buffer, imageSize );
            return img;
        } else if ( dataType instanceof UnsignedShortType ) {
            final short[] buffer = new short[ numElements ];
            readFullInPlace( filePath, buffer );
            final Img img = ArrayImgs.unsignedShorts( buffer, imageSize );
            return img;
        } else if ( dataType instanceof UnsignedIntType ) {
            final int[] buffer = new int[ numElements ];
            readFullInPlace( filePath, buffer );
            final Img img = ArrayImgs.unsignedInts( buffer, imageSize );
            return img;
        } else if ( dataType instanceof UnsignedLongType ) {
            final long[] buffer = new long[ numElements ];
            readFullInPlace( filePath, buffer );
            final Img img = ArrayImgs.longs( buffer, imageSize );
            return img;

        } else if ( dataType instanceof ByteType ) {
            final byte[] buffer = new byte[ numElements ];
            readFullInPlace( filePath, buffer );
            final Img img = ArrayImgs.bytes( buffer, imageSize );
            return img;
        } else if ( dataType instanceof ShortType ) {
            final short[] buffer = new short[ numElements ];
            readFullInPlace( filePath, buffer );
            final Img img = ArrayImgs.shorts( buffer, imageSize );
            return img;
        } else if ( dataType instanceof IntType ) {
            final int[] buffer = new int[ numElements ];
            readFullInPlace( filePath, buffer );
            final Img img = ArrayImgs.ints( buffer, imageSize );
            return img;
        } else if ( dataType instanceof LongType ) {
            final long[] buffer = new long[ numElements ];
            readFullInPlace( filePath, buffer );
            final Img img = ArrayImgs.longs( buffer, imageSize );
            return img;

        } else if ( dataType instanceof FloatType ) {
            final float[] buffer = new float[ numElements ];
            readFullInPlace( filePath, buffer );
            final Img img = ArrayImgs.floats( buffer, imageSize );
            return img;
        } else if ( dataType instanceof DoubleType ) {
            final double[] buffer = new double[ numElements ];
            readFullInPlace( filePath, buffer );
            final Img img = ArrayImgs.doubles( buffer, imageSize );
            return img;

        } else {
            throw new IOException( String.format( "Unknown or unsupported KLB data type of file %s.", filePath ) );
        }
    }

    @SuppressWarnings( "unchecked" )
    private < A extends ArrayDataAccess< A > > Img< T > readCellImg( final String filePath, final Header header )
            throws IOException
    {
        final T dataType = header.dataType;
        final long[] imageSize = squeeze( header.imageSize );
        final long[] blockSize = squeezeBlockSize( header, imageSize.length );
        final int[] cellSize = getCellSize( imageSize, blockSize );

        final CellImgFactory< T > factory = new CellImgFactory< T >( cellSize );
        final CellImg< T, A, DefaultCell< A > > cellImg =
                ( CellImg< T, A, DefaultCell< A > > ) factory.create( imageSize, dataType );
        final int[] cellDims = new int[ imageSize.length ];
        final long[] cellOffset = new long[ cellDims.length ];
        final long[] klbMin = new long[ 5 ];
        final long[] klbMax = new long[ 5 ];
        final Cursor< DefaultCell< A > > cursor = cellImg.getCells().cursor();
        while ( cursor.hasNext() ) {
            final DefaultCell< A > cell = cursor.next();
            cell.dimensions( cellDims );
            cell.min( cellOffset );
            int i = 0;
            for ( int d = 0; d < klbMin.length; ++d ) {
                if ( header.imageSize[ d ] == 1 ) {
                    klbMin[ d ] = klbMax[ d ] = 0;
                } else {
                    klbMin[ d ] = cellOffset[ i ];
                    klbMax[ d ] = klbMin[ d ] + cellDims[ i++ ] - 1;
                }
            }
            switch ( dataType.getBitsPerPixel() ) {
                case 8:
                    readROIinPlace( filePath, klbMin, klbMax, (( ByteArray ) cell.getData()).getCurrentStorageArray() );
                    break;
                case 16:
                    readROIinPlace( filePath, klbMin, klbMax, (( ShortArray ) cell.getData()).getCurrentStorageArray() );
                    break;
                case 32:
                    if ( dataType instanceof IntegerType )
                        readROIinPlace( filePath, klbMin, klbMax, (( IntArray ) cell.getData()).getCurrentStorageArray() );
                    else
                        readROIinPlace( filePath, klbMin, klbMax, (( FloatArray ) cell.getData()).getCurrentStorageArray() );
                    break;
                case 64:
                    if ( dataType instanceof IntegerType )
                        readROIinPlace( filePath, klbMin, klbMax, (( LongArray ) cell.getData()).getCurrentStorageArray() );
                    else
                        readROIinPlace( filePath, klbMin, klbMax, (( DoubleArray ) cell.getData()).getCurrentStorageArray() );
                    break;
                default:
                    throw new IOException( String.format( "Unknown or unsupported KLB data type of file %s.", filePath ) );
            }
        }
        return cellImg;
    }


    /***********************************************************
     * Read ROI
     ***********************************************************/

    public abstract void readROIinPlace( final String filePath, final long[] xyzctMin, final long[] xyzctMax, final byte[] out )
            throws IOException;

    public abstract void readROIinPlace( final String filePath, final long[] xyzctMin, final long[] xyzctMax, final Buffer out )
            throws IOException;


    // The following functions provide default implementations for images that are better represented by
    // short[], int[], etc. than byte[]. The required conversions copy the image, so if possible, concrete
    // implementations of KLB should override these functions to read the corresponding data types directly.

    public void readROIinPlace( final String filePath, final long[] xyzctMin, final long[] xyzctMax, final short[] out )
            throws IOException
    {
        final ByteBuffer bytes = ByteBuffer.allocate( 2 * out.length );
        readROIinPlace( filePath, xyzctMin, xyzctMax, bytes.array() );
        bytes.asShortBuffer().get( out );
    }

    public void readROIinPlace( final String filePath, final long[] xyzctMin, final long[] xyzctMax, final int[] out )
            throws IOException
    {
        final ByteBuffer bytes = ByteBuffer.allocate( 4 * out.length );
        readROIinPlace( filePath, xyzctMin, xyzctMax, bytes.array() );
        bytes.asIntBuffer().get( out );
    }

    public void readROIinPlace( final String filePath, final long[] xyzctMin, final long[] xyzctMax, final long[] out )
            throws IOException
    {
        final ByteBuffer bytes = ByteBuffer.allocate( 8 * out.length );
        readROIinPlace( filePath, xyzctMin, xyzctMax, bytes.array() );
        bytes.asLongBuffer().get( out );
    }

    public void readROIinPlace( final String filePath, final long[] xyzctMin, final long[] xyzctMax, final float[] out )
            throws IOException
    {
        final ByteBuffer bytes = ByteBuffer.allocate( 4 * out.length );
        readROIinPlace( filePath, xyzctMin, xyzctMax, bytes.array() );
        bytes.asFloatBuffer().get( out );
    }

    public void readROIinPlace( final String filePath, final long[] xyzctMin, final long[] xyzctMax, final double[] out )
            throws IOException
    {
        final ByteBuffer bytes = ByteBuffer.allocate( 8 * out.length );
        readROIinPlace( filePath, xyzctMin, xyzctMax, bytes.array() );
        bytes.asDoubleBuffer().get( out );
    }

    /**
     * Read subvolume from image. Returns an instance of ArrayImg if possible, else CellImg.
     * <p>
     * When reading as a CellImg (Img.size() > Integer.MAX_VALUE), ensure that xyzctMin points to the start of a
     * KLB block for max performance. Otherwise, all KLB blocks that intersect with multiple ImgCells will be
     * uncompressed multiple times (when populating each intersecting ImgCell).
     *
     * @param filePath file system path to KLB file
     * @param xyzctMin lower limit of bounding box subvolume, in order xyzct
     * @param xyzctMax upper limit of bounding box subvolume (inclusive), in order xyzct
     * @return subvolume, as ArrayImg or CellImg, as appropriate
     * @throws IOException
     */
    public ImgPlus< T > readROI( final String filePath, final long[] xyzctMin, final long[] xyzctMax )
            throws IOException
    {
        final Header header = readHeader( filePath );
        final long[] roiSize = new long[ xyzctMin.length ];
        for ( int d = 0; d < xyzctMin.length; ++d ) {
            roiSize[ d ] = 1 + xyzctMax[ d ] - xyzctMin[ d ];
        }
        long numElements = 1;
        for ( final long d : roiSize ) {
            numElements *= d;
        }
        if ( numElements <= Integer.MAX_VALUE ) {
            final Img< T > img = readArrayImgROI( filePath, header, xyzctMin, xyzctMax, roiSize, ( int ) numElements );
            return imgToImgPlus( img, header, filePath );
        } else {
            final Img< T > img = readCellImgROI( filePath, header, xyzctMin, xyzctMax, roiSize );
            return imgToImgPlus( img, header, filePath );
        }
    }

    @SuppressWarnings( "unchecked" )
    private Img< T > readArrayImgROI( final String filePath, final Header header, final long[] xyzctMin, final long[] xyzctMax, final long[] roiSize, final int numElements )
            throws IOException
    {
        final T dataType = header.dataType;
        final long[] imageSize = squeeze( roiSize );

        if ( dataType instanceof UnsignedByteType ) {
            final byte[] buffer = new byte[ numElements ];
            readROIinPlace( filePath, xyzctMin, xyzctMax, buffer );
            final Img img = ArrayImgs.unsignedBytes( buffer, imageSize );
            return img;
        } else if ( dataType instanceof UnsignedShortType ) {
            final short[] buffer = new short[ numElements ];
            readROIinPlace( filePath, xyzctMin, xyzctMax, buffer );
            final Img img = ArrayImgs.unsignedShorts( buffer, imageSize );
            return img;
        } else if ( dataType instanceof UnsignedIntType ) {
            final int[] buffer = new int[ numElements ];
            readROIinPlace( filePath, xyzctMin, xyzctMax, buffer );
            final Img img = ArrayImgs.unsignedInts( buffer, imageSize );
            return img;
        } else if ( dataType instanceof UnsignedLongType ) {
            final long[] buffer = new long[ numElements ];
            readROIinPlace( filePath, xyzctMin, xyzctMax, buffer );
            final Img img = ArrayImgs.longs( buffer, imageSize );
            return img;

        } else if ( dataType instanceof ByteType ) {
            final byte[] buffer = new byte[ numElements ];
            readROIinPlace( filePath, xyzctMin, xyzctMax, buffer );
            final Img img = ArrayImgs.bytes( buffer, imageSize );
            return img;
        } else if ( dataType instanceof ShortType ) {
            final short[] buffer = new short[ numElements ];
            readROIinPlace( filePath, xyzctMin, xyzctMax, buffer );
            final Img img = ArrayImgs.shorts( buffer, imageSize );
            return img;
        } else if ( dataType instanceof IntType ) {
            final int[] buffer = new int[ numElements ];
            readROIinPlace( filePath, xyzctMin, xyzctMax, buffer );
            final Img img = ArrayImgs.ints( buffer, imageSize );
            return img;
        } else if ( dataType instanceof LongType ) {
            final long[] buffer = new long[ numElements ];
            readROIinPlace( filePath, xyzctMin, xyzctMax, buffer );
            final Img img = ArrayImgs.longs( buffer, imageSize );
            return img;

        } else if ( dataType instanceof FloatType ) {
            final float[] buffer = new float[ numElements ];
            readROIinPlace( filePath, xyzctMin, xyzctMax, buffer );
            final Img img = ArrayImgs.floats( buffer, imageSize );
            return img;
        } else if ( dataType instanceof DoubleType ) {
            final double[] buffer = new double[ numElements ];
            readROIinPlace( filePath, xyzctMin, xyzctMax, buffer );
            final Img img = ArrayImgs.doubles( buffer, imageSize );
            return img;

        } else {
            throw new IOException( String.format( "Unknown or unsupported KLB data type of file %s.", filePath ) );
        }
    }

    @SuppressWarnings( "unchecked" )
    private < A extends ArrayDataAccess< A > > Img< T > readCellImgROI( final String filePath, final Header header, final long[] xyzctMin, final long[] xyzctMax, final long[] roiSize )
            throws IOException
    {
        final T dataType = header.dataType;
        final long[] imageSize = squeeze( roiSize );
        final long[] blockSize = squeezeBlockSize( header, imageSize.length );
        final int[] cellSize = getCellSize( imageSize, blockSize );

        final CellImgFactory< T > factory = new CellImgFactory< T >( cellSize );
        final CellImg< T, A, DefaultCell< A > > cellImg =
                ( CellImg< T, A, DefaultCell< A > > ) factory.create( imageSize, dataType );
        final Cursor< DefaultCell< A > > cursor = cellImg.getCells().cursor();
        final int[] cellDims = new int[ roiSize.length ];
        final long[] cellOffset = new long[ cellDims.length ];
        final long[] klbMin = new long[ 5 ];
        final long[] klbMax = new long[ 5 ];
        while ( cursor.hasNext() ) {
            final DefaultCell< A > cell = cursor.next();
            cell.dimensions( cellDims );
            cell.min( cellOffset );
            int i = 0;
            for ( int d = 0; d < klbMin.length; ++d ) {
                if ( header.imageSize[ d ] == 1 ) {
                    klbMin[ d ] = klbMax[ d ] = 0;
                } else {
                    klbMin[ d ] = xyzctMin[ d ] + cellOffset[ i ];
                    klbMax[ d ] = klbMin[ d ] + cellDims[ i++ ] - 1;
                }
            }
            switch ( dataType.getBitsPerPixel() ) {
                case 8:
                    readROIinPlace( filePath, klbMin, klbMax, (( ByteArray ) cell.getData()).getCurrentStorageArray() );
                    break;
                case 16:
                    readROIinPlace( filePath, klbMin, klbMax, (( ShortArray ) cell.getData()).getCurrentStorageArray() );
                    break;
                case 32:
                    if ( dataType instanceof IntegerType )
                        readROIinPlace( filePath, klbMin, klbMax, (( IntArray ) cell.getData()).getCurrentStorageArray() );
                    else
                        readROIinPlace( filePath, klbMin, klbMax, (( FloatArray ) cell.getData()).getCurrentStorageArray() );
                    break;
                case 64:
                    if ( dataType instanceof IntegerType )
                        readROIinPlace( filePath, klbMin, klbMax, (( LongArray ) cell.getData()).getCurrentStorageArray() );
                    else
                        readROIinPlace( filePath, klbMin, klbMax, (( DoubleArray ) cell.getData()).getCurrentStorageArray() );
                    break;
                default:
                    throw new IOException( String.format( "Unknown or unsupported KLB data type of file %s.", filePath ) );
            }
        }
        return cellImg;
    }


    /***********************************************************
     * Write
     ***********************************************************/

    public abstract void writeFull( final byte[] img, final String filePath, final long[] imageSize, final T dataType, final float[] pixelSpacing, final long[] blockSize, final CompressionType compressionType, final byte[] metadata )
            throws IOException;

    public abstract void writeFull( final Buffer img, final String filePath, final long[] imageSize, final T dataType, final float[] pixelSpacing, final long[] blockSize, final CompressionType compressionType, final byte[] metadata )
            throws IOException;


    // The following functions provide default implementations for images that are better represented by
    // short[], int[], etc. than byte[]. The required conversions copy the image, so if possible, concrete
    // implementations of KLB should override these functions to read the corresponding data types directly.

    public void writeFull( final short[] img, final String filePath, final long[] imageSize, final T dataType, final float[] pixelSpacing, final long[] blockSize, final CompressionType compressionType, final byte[] metadata )
            throws IOException
    {
        final ByteBuffer buffer = ByteBuffer.allocate( 2 * img.length );
        buffer.asShortBuffer().put( img );
        writeFull( buffer.array(), filePath, imageSize, dataType, pixelSpacing, blockSize, compressionType, metadata );
    }

    public void writeFull( final int[] img, final String filePath, final long[] imageSize, final T dataType, final float[] pixelSpacing, final long[] blockSize, final CompressionType compressionType, final byte[] metadata )
            throws IOException
    {
        final ByteBuffer buffer = ByteBuffer.allocate( 4 * img.length );
        buffer.asIntBuffer().put( img );
        writeFull( buffer.array(), filePath, imageSize, dataType, pixelSpacing, blockSize, compressionType, metadata );
    }

    public void writeFull( final long[] img, final String filePath, final long[] imageSize, final T dataType, final float[] pixelSpacing, final long[] blockSize, final CompressionType compressionType, final byte[] metadata )
            throws IOException
    {
        final ByteBuffer buffer = ByteBuffer.allocate( 8 * img.length );
        buffer.asLongBuffer().put( img );
        writeFull( buffer.array(), filePath, imageSize, dataType, pixelSpacing, blockSize, compressionType, metadata );
    }

    public void writeFull( final float[] img, final String filePath, final long[] imageSize, final T dataType, final float[] pixelSpacing, final long[] blockSize, final CompressionType compressionType, final byte[] metadata )
            throws IOException
    {
        final ByteBuffer buffer = ByteBuffer.allocate( 4 * img.length );
        buffer.asFloatBuffer().put( img );
        writeFull( buffer.array(), filePath, imageSize, ( T ) dataType, pixelSpacing, blockSize, compressionType, metadata );
    }

    public void writeFull( final double[] img, final String filePath, final long[] imageSize, final T dataType, final float[] pixelSpacing, final long[] blockSize, final CompressionType compressionType, final byte[] metadata )
            throws IOException
    {
        final ByteBuffer buffer = ByteBuffer.allocate( 8 * img.length );
        buffer.asDoubleBuffer().put( img );
        writeFull( buffer.array(), filePath, imageSize, ( T ) dataType, pixelSpacing, blockSize, compressionType, metadata );
    }

    public void writeFull( final Img< T > img, final String filePath, final float[] pixelSpacing, final long[] blockSize, final CompressionType compressionType, final byte[] metadata )
            throws IOException
    {
        final long size = img.size();
        if ( size > Integer.MAX_VALUE ) {
            throw new UnsupportedOperationException( String.format( "KLB Java wrapper (JNI) can currently not write images with more pixels than %d, but this image has %d pixels.", Integer.MAX_VALUE, size ) );
        }
        final long[] dims = { 1, 1, 1, 1, 1 };
        img.dimensions( dims );
        final T type = img.firstElement();
        if ( type instanceof GenericByteType ) {
            final byte[] buffer = (( ArrayImg< ?, ByteArray > ) img).update( null ).getCurrentStorageArray();
            writeFull( buffer, filePath, dims, type, pixelSpacing, blockSize, compressionType, metadata );
        } else if ( type instanceof GenericShortType ) {
            final short[] buffer = (( ArrayImg< ?, ShortArray > ) img).update( null ).getCurrentStorageArray();
            writeFull( buffer, filePath, dims, type, pixelSpacing, blockSize, compressionType, metadata );
        } else if ( type instanceof GenericIntType ) {
            final int[] buffer = (( ArrayImg< ?, IntArray > ) img).update( null ).getCurrentStorageArray();
            writeFull( buffer, filePath, dims, type, pixelSpacing, blockSize, compressionType, metadata );
        } else if ( type instanceof LongType ) {
            final long[] buffer = (( ArrayImg< ?, LongArray > ) img).update( null ).getCurrentStorageArray();
            writeFull( buffer, filePath, dims, type, pixelSpacing, blockSize, compressionType, metadata );
        } else if ( type instanceof FloatType ) {
            final float[] buffer = (( ArrayImg< ?, FloatArray > ) img).update( null ).getCurrentStorageArray();
            writeFull( buffer, filePath, dims, type, pixelSpacing, blockSize, compressionType, metadata );
        } else if ( type instanceof DoubleType ) {
            final double[] buffer = (( ArrayImg< ?, DoubleArray > ) img).update( null ).getCurrentStorageArray();
            writeFull( buffer, filePath, dims, type, pixelSpacing, blockSize, compressionType, metadata );
        } else {
            throw new IOException( "Unknown or unsupported KLB data type" );
        }
    }

    public void writeFull( final ImgPlus< T > img, final String filePath, final long[] blockSize, final CompressionType compressionType, final byte[] metadata )
            throws IOException
    {
        final float[] pixelSpacing = { 1, 1, 1, 1, 1 };
        for ( int d = 0; d < img.numDimensions(); ++d ) {
            final CalibratedAxis axis = img.axis( d );
            final AxisType axisType = axis.type();
            final float scale = ( float ) (( LinearAxis ) axis).scale();
            if ( axisType == Axes.X ) {
                pixelSpacing[ 0 ] = scale;
            } else if ( axisType == Axes.Y ) {
                pixelSpacing[ 1 ] = scale;
            } else if ( axisType == Axes.Z ) {
                pixelSpacing[ 2 ] = scale;
            } else if ( axisType == Axes.CHANNEL ) {
                pixelSpacing[ 3 ] = scale;
            } else if ( axisType == Axes.TIME ) {
                pixelSpacing[ 4 ] = scale;
            }
        }
        writeFull( img.getImg(), filePath, pixelSpacing, blockSize, compressionType, metadata );
    }


    /***********************************************************
     * Helper functions
     ***********************************************************/

    private long[] squeeze( final long[] dims )
    {
        int i = 0;
        for ( final long d : dims ) {
            if ( d > 1 ) {
                i++;
            }
        }
        final long[] squeezed = new long[ i ];
        i = 0;
        for ( final long d : dims ) {
            if ( d > 1 ) {
                squeezed[ i++ ] = d;
            }
        }
        return squeezed;
    }

    private long[] squeezeBlockSize( final Header header, final int numNonSingletonDims )
    {
        final long[] squeezedBlockSize = new long[ numNonSingletonDims ];
        int i = 0;
        for ( int d = 0; d < 5; ++d ) {
            if ( header.imageSize[ d ] > 1 ) {
                squeezedBlockSize[ i++ ] = ( int ) header.blockSize[ d ];
            }
        }
        return squeezedBlockSize;
    }

    private ImgPlus< T > imgToImgPlus( final Img< T > img, final Header header, final String name )
    {
        final AxisType[] axisTypes = new AxisType[ img.numDimensions() ];
        final double[] pixelSpacing = new double[ axisTypes.length ];
        int i = 0;
        for ( int d = 0; d < 5; ++d ) {
            if ( header.imageSize[ d ] > 1 ) {
                axisTypes[ i ] = KlbScifio.AXIS_TYPES[ d ];
                pixelSpacing[ i++ ] = header.pixelSpacing[ d ];
            }
        }
        return new ImgPlus< T >( img, name, axisTypes, pixelSpacing );
    }

    /**
     * When reading a KLB image as a CellImg, it is desirable to use as few Cells as possible.
     * On the other hand, the Cells should be aligned with the KLB blocks (i.e. each Cell should be an integer multiple
     * of KLB blocks), and the excess volume (waste) should be minimal.
     * <p>
     * This function tries to come up with the highest integer multiple of KLB blocks, in each dimension, that requires
     * the smallest waste volume.
     * <p>
     * The length of the multipliers vector determines how many dimensions will be considered. Generally, using int[3]
     * is a good idea, since this will try to optimize the Cell dimensions in x,y,z but leave c,t alone.
     *
     * @param imageSize
     * @param blockSize
     * @return cellSize
     */
    protected int[] getCellSize( final long[] imageSize, final long[] blockSize )
    {
        final int[] multipliers = new int[ imageSize.length ];
        Arrays.fill( multipliers, 1 );
        for ( int d = 0; d < multipliers.length; ++d ) {
            final long imageLength = imageSize[ d ];
            final long singleBlockLength = blockSize[ d ];

            int multiplier = 1;
            long multiBlockLength = multiplier * singleBlockLength;
            int numBlocks = ( int ) Math.ceil( ( float ) imageLength / multiBlockLength );
            long cellImgLength = numBlocks * multiBlockLength;
            int minWaste = ( int ) (cellImgLength - imageLength);
            multipliers[ d ] = multiplier;

            while ( multiBlockLength < imageLength * 0.66 ) {
                multiBlockLength = ++multiplier * singleBlockLength;
                numBlocks = ( int ) Math.ceil( ( float ) imageLength / multiBlockLength );
                cellImgLength = numBlocks * multiBlockLength;
                final int waste = ( int ) (cellImgLength - imageLength);
                if ( waste <= minWaste ) {
                    multipliers[ d ] = multiplier;
                    minWaste = waste;
                }
            }
        }

        if ( false ) { // set to true for diagnostic output
            System.out.println( "Single block " + Arrays.toString( blockSize ) );
            System.out.println( "Multipliers  " + Arrays.toString( multipliers ) );
            final long[] temp = blockSize.clone();
            for ( int d = 0; d < multipliers.length; ++d )
                temp[ d ] *= multipliers[ d ];
            System.out.println( "Multi block  " + Arrays.toString( temp ) );
            System.out.println( "Image size   " + Arrays.toString( imageSize ) );
            final int[] numBlocks = new int[ temp.length ];
            for ( int d = 0; d < numBlocks.length; ++d )
                numBlocks[ d ] = ( int ) Math.ceil( ( float ) imageSize[ d ] / temp[ d ] );
            System.out.println( "Num blocks   " + Arrays.toString( numBlocks ) );
            for ( int d = 0; d < temp.length; ++d )
                temp[ d ] *= numBlocks[ d ];
            System.out.println( "CellImg size " + Arrays.toString( temp ) );
            for ( int d = 0; d < temp.length; ++d )
                temp[ d ] -= imageSize[ d ];
            System.out.println( "Waste        " + Arrays.toString( temp ) );
        }

        long numElements = 1;
        for ( int d = 0; d < multipliers.length; ++d ) {
            multipliers[ d ] *= blockSize[ d ];
            numElements *= multipliers[ d ];
        }
        if ( numElements > Integer.MAX_VALUE ) {
            throw new IllegalArgumentException( "ImgCell size is too big." );
        }
        return multipliers;
    }
}
