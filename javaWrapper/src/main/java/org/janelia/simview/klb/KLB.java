package org.janelia.simview.klb;

import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.axis.CalibratedAxis;
import net.imagej.axis.LinearAxis;
import net.imglib2.Cursor;
import net.imglib2.Dimensions;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.basictypeaccess.array.*;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellImg;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.img.planar.PlanarImg;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.GenericByteType;
import net.imglib2.type.numeric.integer.GenericIntType;
import net.imglib2.type.numeric.integer.GenericShortType;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    public void readFullInPlace( final String filePath, final Img< T > out, final boolean noChecks )
            throws IOException
    {
        Header header = null;
        if ( !noChecks ) {
            header = readHeader( filePath );
            long size = 1;
            for ( final long i : header.imageSize ) {
                size *= i;
            }
            if ( size > out.size() || out.firstElement().getClass() == header.dataType.getClass() ) {
                throw new IOException( "Size/type mismatch!" );
            }
        }
        if ( out instanceof CellImg ) {
            readCellImgInPlace( filePath, header, ( CellImg ) out );
        } else {
            readArrayImgInPlace( filePath, ( ArrayImg ) out );
        }
    }

    private < A extends ArrayDataAccess< A > > void readArrayImgInPlace( final String filePath, final ArrayImg< T, A > out )
            throws IOException
    {
        final Object buffer = out.update( null ).getCurrentStorageArray();
        final T type = out.firstElement();
        if ( type instanceof GenericByteType ) {
            readFullInPlace( filePath, (byte[]) buffer );
        } else if ( type instanceof GenericShortType ) {
            readFullInPlace( filePath, (short[]) buffer );
        } else if ( type instanceof GenericIntType ) {
            readFullInPlace( filePath, (int[]) buffer );
        } else if ( type instanceof LongType ) {
            readFullInPlace( filePath, (long[]) buffer );
        } else if ( type instanceof FloatType ) {
            readFullInPlace( filePath, (float[]) buffer );
        } else if ( type instanceof DoubleType ) {
            readFullInPlace( filePath, (double[]) buffer );
        } else {
            throw new IOException( "Unknown or unsupported KLB data type" );
        }
    }

    private < A extends ArrayDataAccess< A > > void readCellImgInPlace( final String filePath, Header header, final CellImg< T, A > out )
            throws IOException
    {
        if ( header == null ) {
            header = readHeader( filePath );
        }
        final T type = out.firstElement();
        final int[] cellDims = new int[ out.numDimensions() ];
        final long[] cellOffset = new long[ cellDims.length ];
        final long[] klbMin = new long[ 5 ];
        final long[] klbMax = new long[ 5 ];
        final Cursor< Cell< A > > cursor = out.getCells().cursor();
        while ( cursor.hasNext() ) {
            final Cell< A > cell = cursor.next();
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
            switch ( type.getBitsPerPixel() ) {
                case 8:
                    readROIinPlace( filePath, klbMin, klbMax, (( ByteArray ) cell.getData()).getCurrentStorageArray() );
                    break;
                case 16:
                    readROIinPlace( filePath, klbMin, klbMax, (( ShortArray ) cell.getData()).getCurrentStorageArray() );
                    break;
                case 32:
                    if ( type instanceof IntegerType )
                        readROIinPlace( filePath, klbMin, klbMax, (( IntArray ) cell.getData()).getCurrentStorageArray() );
                    else
                        readROIinPlace( filePath, klbMin, klbMax, (( FloatArray ) cell.getData()).getCurrentStorageArray() );
                    break;
                case 64:
                    if ( type instanceof IntegerType )
                        readROIinPlace( filePath, klbMin, klbMax, (( LongArray ) cell.getData()).getCurrentStorageArray() );
                    else
                        readROIinPlace( filePath, klbMin, klbMax, (( DoubleArray ) cell.getData()).getCurrentStorageArray() );
                    break;
                default:
                    throw new IOException( String.format( "Unknown or unsupported KLB data type of file %s.", filePath ) );
            }
        }
    }


    /**
     * Read the entire image. Returns an instance of ArrayImg if possible, else CellImg.
     *
     * @param filePath file system path to KLB file
     * @return entire image, ArrayImg if possible, else CellImg
     * @throws IOException
     */
    public < A extends ArrayDataAccess< A > > ImgPlus< T > readFull( final String filePath )
            throws IOException
    {
        final Header header = readHeader( filePath );
        final Img< T > img = newEmptyImage( header.imageSize, header.blockSize, header.dataType );
        if ( img instanceof CellImg ) {
            readCellImgInPlace( filePath, header, ( CellImg< T, A > ) img );
        } else {
            readArrayImgInPlace( filePath, ( ArrayImg< T, A > ) img );
        }
        return imgToImgPlus( img, header, filePath );
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

    public void readROIinPlace( final String filePath, final long[] xyzctMin, final long[] xyzctMax, final Img< T > out, final boolean noChecks )
            throws IOException
    {
        Header header = null;
        if ( !noChecks ) {
            header = readHeader( filePath );
            long size = 1;
            for ( int i = 0; i < xyzctMin.length; ++i ) {
                size *= 1 + xyzctMax[ i ] - xyzctMin[ i ];
            }
            if ( size > out.size() || out.firstElement().getClass() == header.dataType.getClass() ) {
                throw new IOException( "Size/type mismatch!" );
            }
        }
        if ( out instanceof CellImg ) {
            readCellImgROIinPlace( filePath, header, xyzctMin, xyzctMax, ( CellImg ) out );
        } else {
            readArrayImgROIinPlace( filePath, xyzctMin, xyzctMax, ( ArrayImg ) out );
        }
    }

    @SuppressWarnings( "unchecked" )
    private < A extends ArrayDataAccess< A > > void readArrayImgROIinPlace( final String filePath, final long[] xyzctMin, final long[] xyzctMax, final ArrayImg< T, A > out )
            throws IOException
    {
        final T type = out.firstElement();
        if ( type instanceof GenericByteType ) {
            final byte[] buffer = (( ArrayImg< T, ByteArray > ) out).update( null ).getCurrentStorageArray();
            readROIinPlace( filePath, xyzctMin, xyzctMax, buffer );
        } else if ( type instanceof GenericShortType ) {
            final short[] buffer = (( ArrayImg< T, ShortArray > ) out).update( null ).getCurrentStorageArray();
            readROIinPlace( filePath, xyzctMin, xyzctMax, buffer );
        } else if ( type instanceof GenericIntType ) {
            final int[] buffer = (( ArrayImg< T, IntArray > ) out).update( null ).getCurrentStorageArray();
            readROIinPlace( filePath, xyzctMin, xyzctMax, buffer );
        } else if ( type instanceof LongType ) {
            final long[] buffer = (( ArrayImg< T, LongArray > ) out).update( null ).getCurrentStorageArray();
            readROIinPlace( filePath, xyzctMin, xyzctMax, buffer );
        } else if ( type instanceof FloatType ) {
            final float[] buffer = (( ArrayImg< T, FloatArray > ) out).update( null ).getCurrentStorageArray();
            readROIinPlace( filePath, xyzctMin, xyzctMax, buffer );
        } else if ( type instanceof DoubleType ) {
            final double[] buffer = (( ArrayImg< T, DoubleArray > ) out).update( null ).getCurrentStorageArray();
            readROIinPlace( filePath, xyzctMin, xyzctMax, buffer );
        } else {
            throw new IOException( "Unknown or unsupported KLB data type" );
        }
    }

    private < A extends ArrayDataAccess< A > > void readCellImgROIinPlace( final String filePath, Header header, final long[] xyzctMin, final long[] xyzctMax, final CellImg< T, A > out )
            throws IOException
    {
        if ( header == null ) {
            header = readHeader( filePath );
        }
        final T type = out.firstElement();
        final int[] cellDims = new int[ out.numDimensions() ];
        final long[] cellOffset = new long[ cellDims.length ];
        final long[] klbMin = new long[ 5 ];
        final long[] klbMax = new long[ 5 ];
        final Cursor< Cell< A > > cursor = out.getCells().cursor();
        while ( cursor.hasNext() ) {
            final Cell< A > cell = cursor.next();
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
            switch ( type.getBitsPerPixel() ) {
                case 8:
                    readROIinPlace( filePath, klbMin, klbMax, (( ByteArray ) cell.getData()).getCurrentStorageArray() );
                    break;
                case 16:
                    readROIinPlace( filePath, klbMin, klbMax, (( ShortArray ) cell.getData()).getCurrentStorageArray() );
                    break;
                case 32:
                    if ( type instanceof IntegerType )
                        readROIinPlace( filePath, klbMin, klbMax, (( IntArray ) cell.getData()).getCurrentStorageArray() );
                    else
                        readROIinPlace( filePath, klbMin, klbMax, (( FloatArray ) cell.getData()).getCurrentStorageArray() );
                    break;
                case 64:
                    if ( type instanceof IntegerType )
                        readROIinPlace( filePath, klbMin, klbMax, (( LongArray ) cell.getData()).getCurrentStorageArray() );
                    else
                        readROIinPlace( filePath, klbMin, klbMax, (( DoubleArray ) cell.getData()).getCurrentStorageArray() );
                    break;
                default:
                    throw new IOException( String.format( "Unknown or unsupported KLB data type of file %s.", filePath ) );
            }
        }
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
        // todo: blocks may be out of alignment
        final Img< T > img = newEmptyImage( roiSize, header.blockSize, header.dataType );
        if ( img instanceof CellImg ) {
            readCellImgROIinPlace( filePath, header, xyzctMin, xyzctMax, ( CellImg ) img );
        } else {
            readArrayImgROIinPlace( filePath, xyzctMin, xyzctMax, ( ArrayImg ) img );
        }
        return imgToImgPlus( img, header, filePath );
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
        if ( img instanceof CellImg ) {
            writeFull( ( CellImg ) img, filePath, pixelSpacing, blockSize, compressionType, metadata );
        } else if ( img instanceof PlanarImg ) {
            writeFull( ( PlanarImg ) img, filePath, pixelSpacing, blockSize, compressionType, metadata );
        } else {
            writeFull( ( ArrayImg ) img, filePath, pixelSpacing, blockSize, compressionType, metadata );
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

    @SuppressWarnings( "unchecked" )
    private < A extends ArrayDataAccess< A > > void writeFull( final ArrayImg< T, A > img, final String filePath, final float[] pixelSpacing, final long[] blockSize, final CompressionType compressionType, final byte[] metadata )
            throws IOException
    {
        final long[] dims = { 1, 1, 1, 1, 1 };
        img.dimensions( dims );
        final T type = img.firstElement();
        if ( type instanceof GenericByteType ) {
            final byte[] buffer = (( ArrayImg< T, ByteArray > ) img).update( null ).getCurrentStorageArray();
            writeFull( buffer, filePath, dims, type, pixelSpacing, blockSize, compressionType, metadata );
        } else if ( type instanceof GenericShortType ) {
            final short[] buffer = (( ArrayImg< T, ShortArray > ) img).update( null ).getCurrentStorageArray();
            writeFull( buffer, filePath, dims, type, pixelSpacing, blockSize, compressionType, metadata );
        } else if ( type instanceof GenericIntType ) {
            final int[] buffer = (( ArrayImg< T, IntArray > ) img).update( null ).getCurrentStorageArray();
            writeFull( buffer, filePath, dims, type, pixelSpacing, blockSize, compressionType, metadata );
        } else if ( type instanceof LongType ) {
            final long[] buffer = (( ArrayImg< T, LongArray > ) img).update( null ).getCurrentStorageArray();
            writeFull( buffer, filePath, dims, type, pixelSpacing, blockSize, compressionType, metadata );
        } else if ( type instanceof FloatType ) {
            final float[] buffer = (( ArrayImg< T, FloatArray > ) img).update( null ).getCurrentStorageArray();
            writeFull( buffer, filePath, dims, type, pixelSpacing, blockSize, compressionType, metadata );
        } else if ( type instanceof DoubleType ) {
            final double[] buffer = (( ArrayImg< T, DoubleArray > ) img).update( null ).getCurrentStorageArray();
            writeFull( buffer, filePath, dims, type, pixelSpacing, blockSize, compressionType, metadata );
        } else {
            throw new IOException( "Unknown or unsupported KLB data type" );
        }
    }

    private < A extends ArrayDataAccess< A > > void writeFull( final CellImg< T, Cell< A > > img, final String filePath, final float[] pixelSpacing, final long[] blockSize, final CompressionType compressionType, final byte[] metadata )
            throws IOException
    {
        final T type = img.firstElement();
        final Cursor< Cell< Cell<A> >> cellCur = img.getCells().cursor();
        final int[] cellDims = { 1, 1, 1, 1, 1 };
        final long[] cellDimsL = new long[ cellDims.length ];
        while ( cellCur.hasNext() ) {
            final Cell< Cell<A> > cell = cellCur.next();
            cell.dimensions( cellDims );
            for ( int i = 0; i < cellDims.length; ++i ) {
                cellDimsL[ i ] = cellDims[ i ];
            }

            String minStr = String.format( "[%04d", cell.min( 0 ) );
            for ( int i = 1; i < img.numDimensions(); ++i ) {
                minStr += String.format( ",%04d", cell.min( i ) );
            }
            minStr += "]";

            String maxStr = String.format( "[%04d", cell.min( 0 ) + cell.dimension( 0 ) - 1 );
            for ( int i = 1; i < img.numDimensions(); ++i ) {
                maxStr += String.format( ",%04d", cell.min( i ) + cell.dimension( i ) - 1 );
            }
            maxStr += "]";

            final String cellFilePath = filePath.replace( ".klb", String.format( ".%s-%s.klb", minStr, maxStr ) );

            if ( type instanceof GenericByteType ) {
                final byte[] buffer = ( byte[] ) cell.getData().getData().getCurrentStorageArray();
                writeFull( buffer, cellFilePath, cellDimsL, type, pixelSpacing, blockSize, compressionType, metadata );
            } else if ( type instanceof GenericShortType ) {
                final short[] buffer = ( short[] ) cell.getData().getData().getCurrentStorageArray();
                writeFull( buffer, cellFilePath, cellDimsL, type, pixelSpacing, blockSize, compressionType, metadata );
            } else if ( type instanceof GenericIntType ) {
                final int[] buffer = ( int[] ) cell.getData().getData().getCurrentStorageArray();
                writeFull( buffer, cellFilePath, cellDimsL, type, pixelSpacing, blockSize, compressionType, metadata );
            } else if ( type instanceof LongType ) {
                final long[] buffer = ( long[] ) cell.getData().getData().getCurrentStorageArray();
                writeFull( buffer, cellFilePath, cellDimsL, type, pixelSpacing, blockSize, compressionType, metadata );
            } else if ( type instanceof FloatType ) {
                final float[] buffer = ( float[] ) cell.getData().getData().getCurrentStorageArray();
                writeFull( buffer, cellFilePath, cellDimsL, type, pixelSpacing, blockSize, compressionType, metadata );
            } else if ( type instanceof DoubleType ) {
                final double[] buffer = ( double[] ) cell.getData().getData().getCurrentStorageArray();
                writeFull( buffer, cellFilePath, cellDimsL, type, pixelSpacing, blockSize, compressionType, metadata );
            } else {
                throw new IOException( "Unknown or unsupported KLB data type" );
            }
        }
    }

    private < A extends ArrayDataAccess< A > > void writeFull( final PlanarImg< T, A > img, final String filePath, final float[] pixelSpacing, final long[] blockSize, final CompressionType compressionType, final byte[] metadata )
            throws IOException
    {
        final Img< T > target = getImgFactory( img, blockSize ).create( img, img.firstElement() );
        final ExecutorService taskExecutor = Executors.newFixedThreadPool( numThreads );
        final List< Callable< Void > > tasks = new ArrayList< Callable< Void > >();
        final long size = target.size();
        final long threadChunkSize = size / numThreads;
        final long threadChunkMod = size % numThreads;
        for ( int portionID = 0; portionID < numThreads; ++portionID ) {
            final long startPosition = portionID * threadChunkSize;
            final long loopSize = (portionID == numThreads - 1) ? threadChunkSize + threadChunkMod : threadChunkSize;
            tasks.add(() -> {
                final RandomAccess< T > sourceRa = img.randomAccess();
                final Cursor< T > targetCur = target.localizingCursor();
                targetCur.jumpFwd( startPosition );
                for ( long j = 0; j < loopSize; ++j ) {
                    targetCur.fwd();
                    sourceRa.setPosition( targetCur );
                    targetCur.get().set( sourceRa.get() );
                }
                return null;
            });
        }

        try {
            // invokeAll() returns when all tasks are complete
            taskExecutor.invokeAll( tasks );
            taskExecutor.shutdown();
        } catch ( final InterruptedException e ) {
            e.printStackTrace();
        }

        writeFull( target, filePath, pixelSpacing, blockSize, compressionType, metadata );
    }


    /***********************************************************
     * Helper functions
     ***********************************************************/

    private long[][] getSqueezedImageAndBlockSize( final long[] imageSize, final long[] blockSize )
    {
        int i = 0;
        for ( final long d : imageSize ) {
            if ( d > 1 ) {
                i++;
            }
        }
        final long[][] squeezed = new long[ 2 ][ i ];

        i = 0;
        for ( final long d : imageSize ) {
            if ( d > 1 ) {
                squeezed[ 0 ][ i++ ] = d;
            }
        }

        if ( blockSize != null ) {
            i = 0;
            for ( int d = 0; d < imageSize.length; ++d ) {
                if ( imageSize[ d ] > 1 ) {
                    squeezed[ 1 ][ i++ ] = blockSize[ d ];
                }
            }
        }

        return squeezed;
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

    public Img< T > newEmptyImage( final long[] imageSize, final long[] blockSize, final T type )
    {
        // ToDo: duplicate call of getSqueezedImageAndBlockSize
        final long[][] imgBlkSize = getSqueezedImageAndBlockSize( imageSize, blockSize );
        return getImgFactory( imageSize, blockSize ).create( imgBlkSize[ 0 ], type );
    }

    /**
     * Returns an ImgFactory that is appropriate for the given imageSize (ArrayImgFactory if possible, else
     * CellImgFactory). In case a CellImgFactory is required, the shape of the ImgCells is chosen such that the image
     * is stored in as few as possible contiguous arrays and aligned with the KLB blocks to avoid KLB blocks that
     * intersect with multiple Cells and thus need to be loaded multiple times.
     *
     * @param imageSize
     * @param blockSize
     * @return
     */
    public ImgFactory< T > getImgFactory( final long[] imageSize, final long[] blockSize )
    {
        final long[][] imgBlkSize = getSqueezedImageAndBlockSize( imageSize, blockSize );
        final int[] cellSize = blockSize != null ? getOptimalCellSize( imgBlkSize[ 0 ], imgBlkSize[ 1 ] ) : getOptimalCellSize( imgBlkSize[ 0 ], null );
        return cellSize != null ? new CellImgFactory< T >( cellSize ) : new ArrayImgFactory< T >();
    }

    /**
     * Returns an ImgFactory that is appropriate for the given imageSize (ArrayImgFactory if possible, else
     * CellImgFactory). In case a CellImgFactory is required, the shape of the ImgCells is chosen such that the image
     * is stored in as few as possible contiguous arrays and aligned with the KLB blocks to avoid KLB blocks that
     * intersect with multiple Cells and thus need to be loaded multiple times.
     *
     * @param imageSize
     * @param blockSize
     * @return
     */
    public ImgFactory< T > getImgFactory( final Dimensions imageSize, final long[] blockSize )
    {
        final long[] size = new long[ imageSize.numDimensions() ];
        imageSize.dimensions( size );
        return getImgFactory( size, blockSize );
    }

    /**
     * When reading a KLB image as a CellImg, the shape of the ImgCells should be chosen such that the image is stored
     * in as few as possible contiguous arrays. The ImgCells should also be aligned with the KLB blocks to avoid KLB
     * blocks that intersect with multiple Cells and thus need to be loaded multiple times. This function returns a
     * proposed optimal Cell shape given the provided image and block size.
     * <p>
     * For convenience, it is valid to ignore the blockSize by passing null. If the image can be stored as an ArrayImg,
     * this function returns null.
     *
     * @param imageSize
     * @param blockSize can be null
     * @return cellSize or null if an image of size imageSize can be stored as ArrayImg
     */
    private int[] getOptimalCellSize( final long[] imageSize, final long[] blockSize )
    {
        // todo: blockSize is currently not considered
        // todo: partitioning suboptimal
        long size = 1;
        for ( final long i : imageSize ) {
            size *= i;
        }
        final int numPartitions = ( int ) Math.ceil( ( double ) size / Integer.MAX_VALUE );
        final int[] cellSize = numPartitions > 1 ? new int[ imageSize.length ] : null;
        if ( cellSize != null ) {
            int partitionDim = imageSize.length - 1;
            while ( imageSize[ partitionDim ] < numPartitions ) {
                partitionDim--;
            }

            final int partitionLength = ( int ) Math.ceil( ( double ) imageSize[ partitionDim ] / numPartitions );
            for ( int i = 0; i < cellSize.length; ++i ) {
                cellSize[ i ] = i == partitionDim ? partitionLength : ( int ) imageSize[ i ];
            }

            size = 1;
            for ( final long i : cellSize ) {
                size *= i;
            }
            while ( size > Integer.MAX_VALUE ) {
                cellSize[ partitionDim ] -= 1;
                size = 1;
                for ( final long i : cellSize ) {
                    size *= i;
                }
            }
        }
        return cellSize;
    }
}
