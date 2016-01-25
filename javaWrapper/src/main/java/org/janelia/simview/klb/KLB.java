package org.janelia.simview.klb;

import net.imglib2.Cursor;
import net.imglib2.img.Img;
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

public abstract class KLB
{
    public enum CompressionType
    {
        NONE, BZIP2, ZLIB
    }

    public class Header< T extends RealType< T > & NativeType< T > >
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
     * @param <T>      imglib2 data type of image
     * @return entire image, as ArrayImg or CellImg, as appropriate
     * @throws IOException
     */
    public < T extends RealType< T > & NativeType< T > > Img< T > readFull( final String filePath ) throws IOException
    {
        // KLB is always 5-dimensional, so drop trailing singleton dimensions here
        final Header header = readHeader( filePath );
        final long[] imageSize = header.imageSize;
        int maxDim = imageSize.length - 1;
        while ( maxDim > 1 && imageSize[ maxDim ] <= 1 ) {
            maxDim--;
        }
        final long[] squeezed = new long[ maxDim + 1 ];
        System.arraycopy( imageSize, 0, squeezed, 0, squeezed.length );

        // read as ArrayImg if possible, else read as CellImg
        long numElements = 1;
        for ( final long d : squeezed ) {
            numElements *= d;
        }
        if ( numElements <= Integer.MAX_VALUE ) {
            return readArrayImg( filePath, ( T ) header.dataType, squeezed, ( int ) numElements );
        } else {
            // get better block size, see getBlockSizeMultipliers function
            final int[] blockSize = new int[ squeezed.length ];
            for ( int d = 0; d < blockSize.length; ++d ) {
                blockSize[ d ] = ( int ) header.blockSize[ d ];
            }
            final int[] blockMultipliers = new int[ Math.min( 3, squeezed.length ) ]; // consider spatial (first 3) dimensions only
            getBlockSizeMultipliers( header, blockMultipliers );
            for ( int d = 0; d < blockMultipliers.length; ++d ) {
                blockSize[ d ] *= blockMultipliers[ d ];
            }
            return readCellImg( filePath, ( T ) header.dataType, squeezed, blockSize );
        }
    }

    private < T extends RealType< ? > & NativeType< ? > > Img< T > readArrayImg( final String filePath, final T dataType, final long[] imageSize, final int numElements )
            throws IOException
    {
        if ( dataType instanceof UnsignedByteType ) {
            final byte[] buffer = new byte[ numElements ];
            readFullInPlace( filePath, buffer );
            return ( Img< T > ) ArrayImgs.unsignedBytes( buffer, imageSize );
        } else if ( dataType instanceof UnsignedShortType ) {
            final short[] buffer = new short[ numElements ];
            readFullInPlace( filePath, buffer );
            return ( Img< T > ) ArrayImgs.unsignedShorts( buffer, imageSize );
        } else if ( dataType instanceof UnsignedIntType ) {
            final int[] buffer = new int[ numElements ];
            readFullInPlace( filePath, buffer );
            return ( Img< T > ) ArrayImgs.unsignedInts( buffer, imageSize );
        } else if ( dataType instanceof UnsignedLongType ) {
            final long[] buffer = new long[ numElements ];
            readFullInPlace( filePath, buffer );
            return ( Img< T > ) ArrayImgs.longs( buffer, imageSize ); // unsigned?

        } else if ( dataType instanceof ByteType ) {
            final byte[] buffer = new byte[ numElements ];
            readFullInPlace( filePath, buffer );
            return ( Img< T > ) ArrayImgs.bytes( buffer, imageSize );
        } else if ( dataType instanceof ShortType ) {
            final short[] buffer = new short[ numElements ];
            readFullInPlace( filePath, buffer );
            return ( Img< T > ) ArrayImgs.shorts( buffer, imageSize );
        } else if ( dataType instanceof IntType ) {
            final int[] buffer = new int[ numElements ];
            readFullInPlace( filePath, buffer );
            return ( Img< T > ) ArrayImgs.ints( buffer, imageSize );
        } else if ( dataType instanceof LongType ) {
            final long[] buffer = new long[ numElements ];
            readFullInPlace( filePath, buffer );
            return ( Img< T > ) ArrayImgs.longs( buffer, imageSize );

        } else if ( dataType instanceof FloatType ) {
            final float[] buffer = new float[ numElements ];
            readFullInPlace( filePath, buffer );
            return ( Img< T > ) ArrayImgs.floats( buffer, imageSize );
        } else if ( dataType instanceof DoubleType ) {
            final double[] buffer = new double[ numElements ];
            readFullInPlace( filePath, buffer );
            return ( Img< T > ) ArrayImgs.doubles( buffer, imageSize );
        } else {
            throw new IOException( String.format( "Unknown or unsupported KLB data type of file %s.", filePath ) );
        }
    }

    private < T extends RealType< T > & NativeType< T >, A extends ArrayDataAccess< A > > Img< T > readCellImg( final String filePath, final T dataType, final long[] imageSize, final int[] blockSize )
            throws IOException
    {
        final CellImgFactory< T > factory = new CellImgFactory< T >( blockSize );
        final CellImg< T, A, DefaultCell< A > > cellImg =
                ( CellImg< T, A, DefaultCell< A > > ) factory.create( imageSize, dataType );
        final int[] dims = new int[ imageSize.length ];
        final long[] min = new long[ 5 ];
        final long[] max = new long[ 5 ];
        final Cursor< DefaultCell< A > > cursor = cellImg.getCells().cursor();
        while ( cursor.hasNext() ) {
            final DefaultCell< A > cell = cursor.next();
            cell.dimensions( dims );
            cell.min( min );
            for ( int d = 0; d < min.length; ++d ) {
                final int len = d < dims.length ? dims[ d ] : 1;
                max[ d ] = min[ d ] + len - 1;
            }
            switch ( dataType.getBitsPerPixel() ) {
                case 8:
                    readROIinPlace( filePath, min, max, (( ByteArray ) cell.getData()).getCurrentStorageArray() );
                    break;
                case 16:
                    readROIinPlace( filePath, min, max, (( ShortArray ) cell.getData()).getCurrentStorageArray() );
                    break;
                case 32:
                    if ( dataType instanceof IntegerType )
                        readROIinPlace( filePath, min, max, (( IntArray ) cell.getData()).getCurrentStorageArray() );
                    else
                        readROIinPlace( filePath, min, max, (( FloatArray ) cell.getData()).getCurrentStorageArray() );
                    break;
                case 64:
                    if ( dataType instanceof IntegerType )
                        readROIinPlace( filePath, min, max, (( LongArray ) cell.getData()).getCurrentStorageArray() );
                    else
                        readROIinPlace( filePath, min, max, (( DoubleArray ) cell.getData()).getCurrentStorageArray() );
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
     * @param <T>      imglib2 data type of image
     * @return subvolume, as ArrayImg or CellImg, as appropriate
     * @throws IOException
     */
    public < T extends RealType< T > & NativeType< T > > Img< T > readROI( final String filePath, final long[] xyzctMin, final long[] xyzctMax )
            throws IOException
    {
        // KLB is always 5-dimensional, so drop trailing singleton dimensions here
        final long[] roiSize = new long[ xyzctMin.length ];
        for ( int d = 0; d < xyzctMin.length; ++d ) {
            roiSize[ d ] = 1 + xyzctMax[ d ] - xyzctMin[ d ];
        }
        int maxDim = roiSize.length - 1;
        while ( maxDim > 1 && roiSize[ maxDim ] <= 1 ) {
            maxDim--;
        }
        final long[] squeezed = new long[ maxDim + 1 ];
        System.arraycopy( roiSize, 0, squeezed, 0, squeezed.length );

        // read as ArrayImg if possible, else read as CellImg
        long numElements = 1;
        for ( final long d : squeezed ) {
            numElements *= d;
        }
        final Header header = readHeader( filePath );
        if ( numElements <= Integer.MAX_VALUE ) {
            return readArrayImgROI( filePath, ( T ) header.dataType, squeezed, xyzctMin, xyzctMax, ( int ) numElements );
        } else {
            // get better block size, see getBlockSizeMultipliers function
            final int[] blockSize = new int[ squeezed.length ];
            for ( int d = 0; d < blockSize.length; ++d ) {
                blockSize[ d ] = ( int ) header.blockSize[ d ];
            }
            final int[] blockMultipliers = new int[ Math.min( 3, squeezed.length ) ]; // consider spatial (first 3) dimensions only
            getBlockSizeMultipliers( header, blockMultipliers );
            for ( int d = 0; d < blockMultipliers.length; ++d ) {
                blockSize[ d ] *= blockMultipliers[ d ];
            }
            return readCellImgROI( filePath, ( T ) header.dataType, squeezed, xyzctMin, xyzctMax, blockSize );
        }
    }

    private < T extends RealType< ? > & NativeType< ? > > Img< T > readArrayImgROI( final String filePath, final T dataType, final long[] roiSize, final long[] xyzctMin, final long[] xyzctMax, final int numElements )
            throws IOException
    {
        if ( dataType instanceof UnsignedByteType ) {
            final byte[] buffer = new byte[ numElements ];
            readROIinPlace( filePath, xyzctMin, xyzctMax, buffer );
            return ( Img< T > ) ArrayImgs.unsignedBytes( buffer, roiSize );
        } else if ( dataType instanceof UnsignedShortType ) {
            final short[] buffer = new short[ numElements ];
            readROIinPlace( filePath, xyzctMin, xyzctMax, buffer );
            return ( Img< T > ) ArrayImgs.unsignedShorts( buffer, roiSize );
        } else if ( dataType instanceof UnsignedIntType ) {
            final int[] buffer = new int[ numElements ];
            readROIinPlace( filePath, xyzctMin, xyzctMax, buffer );
            return ( Img< T > ) ArrayImgs.unsignedInts( buffer, roiSize );
        } else if ( dataType instanceof UnsignedLongType ) {
            final long[] buffer = new long[ numElements ];
            readROIinPlace( filePath, xyzctMin, xyzctMax, buffer );
            return ( Img< T > ) ArrayImgs.longs( buffer, roiSize ); // unsigned?

        } else if ( dataType instanceof ByteType ) {
            final byte[] buffer = new byte[ numElements ];
            readROIinPlace( filePath, xyzctMin, xyzctMax, buffer );
            return ( Img< T > ) ArrayImgs.bytes( buffer, roiSize );
        } else if ( dataType instanceof ShortType ) {
            final short[] buffer = new short[ numElements ];
            readROIinPlace( filePath, xyzctMin, xyzctMax, buffer );
            return ( Img< T > ) ArrayImgs.shorts( buffer, roiSize );
        } else if ( dataType instanceof IntType ) {
            final int[] buffer = new int[ numElements ];
            readROIinPlace( filePath, xyzctMin, xyzctMax, buffer );
            return ( Img< T > ) ArrayImgs.ints( buffer, roiSize );
        } else if ( dataType instanceof LongType ) {
            final long[] buffer = new long[ numElements ];
            readROIinPlace( filePath, xyzctMin, xyzctMax, buffer );
            return ( Img< T > ) ArrayImgs.longs( buffer, roiSize );

        } else if ( dataType instanceof FloatType ) {
            final float[] buffer = new float[ numElements ];
            readROIinPlace( filePath, xyzctMin, xyzctMax, buffer );
            return ( Img< T > ) ArrayImgs.floats( buffer, roiSize );
        } else if ( dataType instanceof DoubleType ) {
            final double[] buffer = new double[ numElements ];
            readROIinPlace( filePath, xyzctMin, xyzctMax, buffer );
            return ( Img< T > ) ArrayImgs.doubles( buffer, roiSize );
        } else {
            throw new IOException( String.format( "Unknown or unsupported KLB data type of file %s.", filePath ) );
        }
    }

    private < T extends RealType< T > & NativeType< T >, A extends ArrayDataAccess< A > > Img< T > readCellImgROI( final String filePath, final T dataType, final long[] roiSize, final long[] xyzctMin, final long[] xyzctMax, final int[] blockSize )
            throws IOException
    {
        final CellImgFactory< T > factory = new CellImgFactory< T >( blockSize );
        final CellImg< T, A, DefaultCell< A > > cellImg =
                ( CellImg< T, A, DefaultCell< A > > ) factory.create( roiSize, dataType );
        final Cursor< DefaultCell< A > > cursor = cellImg.getCells().cursor();
        final int[] dims = new int[ roiSize.length ];
        final long[] min = new long[ 5 ];
        final long[] max = new long[ 5 ];
        while ( cursor.hasNext() ) {
            final DefaultCell< A > cell = cursor.next();
            cell.dimensions( dims );
            cell.min( min );
            for ( int d = 0; d < min.length; ++d ) {
                final int len = d < dims.length ? dims[ d ] : 1;
                max[ d ] = min[ d ] + len - 1;
            }
            switch ( dataType.getBitsPerPixel() ) {
                case 8:
                    readROIinPlace( filePath, min, max, (( ByteArray ) cell.getData()).getCurrentStorageArray() );
                    break;
                case 16:
                    readROIinPlace( filePath, min, max, (( ShortArray ) cell.getData()).getCurrentStorageArray() );
                    break;
                case 32:
                    if ( dataType instanceof IntegerType )
                        readROIinPlace( filePath, min, max, (( IntArray ) cell.getData()).getCurrentStorageArray() );
                    else
                        readROIinPlace( filePath, min, max, (( FloatArray ) cell.getData()).getCurrentStorageArray() );
                    break;
                case 64:
                    if ( dataType instanceof IntegerType )
                        readROIinPlace( filePath, min, max, (( LongArray ) cell.getData()).getCurrentStorageArray() );
                    else
                        readROIinPlace( filePath, min, max, (( DoubleArray ) cell.getData()).getCurrentStorageArray() );
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

    public abstract < T extends RealType< ? > & NativeType< ? > > void writeFull( final byte[] img, final String filePath, final long[] imageSize, final T dataType, final float[] pixelSpacing, final long[] blockSize, final CompressionType compressionType, final byte[] metadata )
            throws IOException;

    public abstract < T extends RealType< ? > & NativeType< ? > > void writeFull( final Buffer img, final String filePath, final long[] imageSize, final T dataType, final float[] pixelSpacing, final long[] blockSize, final CompressionType compressionType, final byte[] metadata )
            throws IOException;


    // The following functions provide default implementations for images that are better represented by
    // short[], int[], etc. than byte[]. The required conversions copy the image, so if possible, concrete
    // implementations of KLB should override these functions to read the corresponding data types directly.

    public < T extends GenericShortType< ? > > void writeFull( final short[] img, final String filePath, final long[] imageSize, final T dataType, final float[] pixelSpacing, final long[] blockSize, final CompressionType compressionType, final byte[] metadata )
            throws IOException
    {
        final ByteBuffer buffer = ByteBuffer.allocate( 2 * img.length );
        buffer.asShortBuffer().put( img );
        writeFull( buffer.array(), filePath, imageSize, dataType, pixelSpacing, blockSize, compressionType, metadata );
    }

    public < T extends GenericIntType< ? > > void writeFull( final int[] img, final String filePath, final long[] imageSize, final T dataType, final float[] pixelSpacing, final long[] blockSize, final CompressionType compressionType, final byte[] metadata )
            throws IOException
    {
        final ByteBuffer buffer = ByteBuffer.allocate( 4 * img.length );
        buffer.asIntBuffer().put( img );
        writeFull( buffer.array(), filePath, imageSize, dataType, pixelSpacing, blockSize, compressionType, metadata );
    }

    public < T extends RealType< ? > & NativeType< ? > > void writeFull( final long[] img, final String filePath, final long[] imageSize, final T dataType, final float[] pixelSpacing, final long[] blockSize, final CompressionType compressionType, final byte[] metadata )
            throws IOException
    {
        final ByteBuffer buffer = ByteBuffer.allocate( 8 * img.length );
        buffer.asLongBuffer().put( img );
        writeFull( buffer.array(), filePath, imageSize, dataType, pixelSpacing, blockSize, compressionType, metadata );
    }

    public void writeFull( final float[] img, final String filePath, final long[] imageSize, final FloatType dataType, final float[] pixelSpacing, final long[] blockSize, final CompressionType compressionType, final byte[] metadata )
            throws IOException
    {
        final ByteBuffer buffer = ByteBuffer.allocate( 4 * img.length );
        buffer.asFloatBuffer().put( img );
        writeFull( buffer.array(), filePath, imageSize, dataType, pixelSpacing, blockSize, compressionType, metadata );
    }

    public void writeFull( final double[] img, final String filePath, final long[] imageSize, final DoubleType dataType, final float[] pixelSpacing, final long[] blockSize, final CompressionType compressionType, final byte[] metadata )
            throws IOException
    {
        final ByteBuffer buffer = ByteBuffer.allocate( 8 * img.length );
        buffer.asDoubleBuffer().put( img );
        writeFull( buffer.array(), filePath, imageSize, dataType, pixelSpacing, blockSize, compressionType, metadata );
    }


    /***********************************************************
     * Helper functions
     ***********************************************************/

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
     * @param header
     * @param multipliers
     */
    protected void getBlockSizeMultipliers( final Header header, final int[] multipliers )
    {
        for ( int d = 0; d < multipliers.length; ++d ) {
            final long imageLength = header.imageSize[ d ];
            final long singleBlockLength = header.blockSize[ d ];

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
            System.out.println( "Single block " + Arrays.toString( header.blockSize ) );
            System.out.println( "Multipliers  " + Arrays.toString( multipliers ) );
            final long[] temp = header.blockSize.clone();
            for ( int d = 0; d < multipliers.length; ++d )
                temp[ d ] *= multipliers[ d ];
            System.out.println( "Multi block  " + Arrays.toString( temp ) );
            System.out.println( "Image size   " + Arrays.toString( header.imageSize ) );
            final int[] numBlocks = new int[ temp.length ];
            for ( int d = 0; d < numBlocks.length; ++d )
                numBlocks[ d ] = ( int ) Math.ceil( ( float ) header.imageSize[ d ] / temp[ d ] );
            System.out.println( "Num blocks   " + Arrays.toString( numBlocks ) );
            for ( int d = 0; d < temp.length; ++d )
                temp[ d ] *= numBlocks[ d ];
            System.out.println( "CellImg size " + Arrays.toString( temp ) );
            for ( int d = 0; d < temp.length; ++d )
                temp[ d ] -= header.imageSize[ d ];
            System.out.println( "Waste        " + Arrays.toString( temp ) );
        }
    }
}
