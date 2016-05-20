package org.janelia.simview.klb;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.*;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import org.janelia.simview.util.NativeLibraryLoader;

import java.io.IOException;
import java.nio.Buffer;


public class KLBJNI< T extends RealType< T > & NativeType< T > > extends KLB< T >
{

    // load bundled native libraries
    static {
        final NativeLibraryLoader loader = new NativeLibraryLoader();
        try {
            loader.unpackAndLoadFromResources( "klb-jni" );
        } catch ( UnsatisfiedLinkError e1 ) {
            try {
                // If loading klb fails with an UnsatisfiedLinkError,
                // most likely the OS is Windows and the Visual C++
                // Redistributable is not installed.
                // Try again, this time loading the runtime libs first.
                loader.unpackAndLoadFromResources( "msvcr120" );
                loader.unpackAndLoadFromResources( "msvcp120" );
                loader.unpackAndLoadFromResources( "klb-jni" );
            } catch ( Throwable e2 ) {
                throw new UnsatisfiedLinkError( "[KLB] Failed to unpack or load native KLB libraries.\n" + e2.getMessage() );
            }
        } catch ( Throwable e3 ) {
            throw new UnsatisfiedLinkError( "[KLB] Failed to unpack or load native KLB libraries.\n" + e3.getMessage() );
        }
    }

    /**
     * Constructor is protected to prevent direct instantiation. Use org.janelia.simview.klb.KLB.newInstance() instead.
     * This may eventually enable the use of multiple implementations (JNI and pure Java) side-by-side.
     */
    protected KLBJNI()
    {
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public Header readHeader( final String filePath ) throws IOException
    {
        final Header header = new Header();
        final int[] dataAndCompressionType = new int[ 2 ];
        final byte[] meta = new byte[ 256 ];
        final int err = jniReadHeader( filePath, header.imageSize, header.blockSize, header.pixelSpacing, dataAndCompressionType, meta );
        if ( err != 0 )
            throw new IOException( String.format( "Could not read KLB header of file %s, error code %d.", filePath, err ) );

        header.metadata = meta;

        switch ( dataAndCompressionType[ 0 ] ) {
            case 0:
                header.dataType = ( T ) new UnsignedByteType();
                break;
            case 1:
                header.dataType = ( T ) new UnsignedShortType();
                break;
            case 2:
                header.dataType = ( T ) new UnsignedIntType();
                break;
            case 3:
                header.dataType = ( T ) new UnsignedLongType();
                break;
            case 4:
                header.dataType = ( T ) new ByteType();
                break;
            case 5:
                header.dataType = ( T ) new ShortType();
                break;
            case 6:
                header.dataType = ( T ) new IntType();
                break;
            case 7:
                header.dataType = ( T ) new LongType();
                break;
            case 8:
                header.dataType = ( T ) new FloatType();
                break;
            case 9:
                header.dataType = ( T ) new DoubleType();
                break;
            default:
                throw new IOException( String.format( "Unknown or unsupported KLB data type of file %s.", filePath ) );
        }

        switch ( dataAndCompressionType[ 1 ] ) {
            case 0:
                header.compressionType = CompressionType.NONE;
                break;
            case 1:
                header.compressionType = CompressionType.BZIP2;
                break;
            case 2:
                header.compressionType = CompressionType.ZLIB;
                break;
            default:
                throw new IOException( String.format( "Unknown or unsupported compression type of file %s.", filePath ) );
        }

        return header;
    }


    /***********************************************************
     * Read entire image
     ***********************************************************/

    @Override
    public void readFullInPlace( final String filePath, final byte[] out )
            throws IOException
    {
        final int err = jniReadFull( filePath, numThreads, out );
        if ( err != 0 )
            throw new IOException( String.format( "Could not read KLB file %s, error code %d.", filePath, err ) );
    }

    @Override
    public void readFullInPlace( final String filePath, final Buffer out )
            throws IOException
    {
        final int err = jniReadFull( filePath, numThreads, out );
        if ( err != 0 )
            throw new IOException( String.format( "Could not read KLB file %s, error code %d.", filePath, err ) );
    }


    // Override default implementations in org.janelia.simview.klb.KLB to use JNI functions.
    // This avoids copying the image to convert from byte[] to short[], int[], etc.

    @Override
    public void readFullInPlace( final String filePath, final short[] out )
            throws IOException
    {
        final int err = jniReadFull( filePath, numThreads, out );
        if ( err != 0 )
            throw new IOException( String.format( "Could not read KLB file %s, error code %d.", filePath, err ) );
    }

    @Override
    public void readFullInPlace( final String filePath, final int[] out )
            throws IOException
    {
        final int err = jniReadFull( filePath, numThreads, out );
        if ( err != 0 )
            throw new IOException( String.format( "Could not read KLB file %s, error code %d.", filePath, err ) );
    }

    @Override
    public void readFullInPlace( final String filePath, final long[] out )
            throws IOException
    {
        final int err = jniReadFull( filePath, numThreads, out );
        if ( err != 0 )
            throw new IOException( String.format( "Could not read KLB file %s, error code %d.", filePath, err ) );
    }

    @Override
    public void readFullInPlace( final String filePath, final float[] out )
            throws IOException
    {
        final int err = jniReadFull( filePath, numThreads, out );
        if ( err != 0 )
            throw new IOException( String.format( "Could not read KLB file %s, error code %d.", filePath, err ) );
    }

    @Override
    public void readFullInPlace( final String filePath, final double[] out )
            throws IOException
    {
        final int err = jniReadFull( filePath, numThreads, out );
        if ( err != 0 )
            throw new IOException( String.format( "Could not read KLB file %s, error code %d.", filePath, err ) );
    }


    /***********************************************************
     * Read ROI
     ***********************************************************/

    @Override
    public void readROIinPlace( final String filePath, final long[] xyzctMin, final long[] xyzctMax, final byte[] out )
            throws IOException
    {
        final int err = jniReadROI( filePath, xyzctMin, xyzctMax, numThreads, out );
        if ( err != 0 )
            throw new IOException( String.format( "Could not read ROI from KLB file %s, error code %d.", filePath, err ) );
    }

    @Override
    public void readROIinPlace( final String filePath, final long[] xyzctMin, final long[] xyzctMax, final Buffer out )
            throws IOException
    {
        final int err = jniReadROI( filePath, xyzctMin, xyzctMax, numThreads, out );
        if ( err != 0 )
            throw new IOException( String.format( "Could not read ROI from KLB file %s, error code %d.", filePath, err ) );
    }


    // Override default implementations in org.janelia.simview.klb.KLB to use JNI functions.
    // This avoids copying the image to convert from byte[] to short[], int[], etc.

    @Override
    public void readROIinPlace( final String filePath, final long[] xyzctMin, final long[] xyzctMax, final short[] out )
            throws IOException
    {
        final int err = jniReadROI( filePath, xyzctMin, xyzctMax, numThreads, out );
        if ( err != 0 )
            throw new IOException( String.format( "Could not read ROI from KLB file %s, error code %d.", filePath, err ) );
    }

    @Override
    public void readROIinPlace( final String filePath, final long[] xyzctMin, final long[] xyzctMax, final int[] out )
            throws IOException
    {
        final int err = jniReadROI( filePath, xyzctMin, xyzctMax, numThreads, out );
        if ( err != 0 )
            throw new IOException( String.format( "Could not read ROI from KLB file %s, error code %d.", filePath, err ) );
    }

    @Override
    public void readROIinPlace( final String filePath, final long[] xyzctMin, final long[] xyzctMax, final long[] out )
            throws IOException
    {
        final int err = jniReadROI( filePath, xyzctMin, xyzctMax, numThreads, out );
        if ( err != 0 )
            throw new IOException( String.format( "Could not read ROI from KLB file %s, error code %d.", filePath, err ) );
    }

    @Override
    public void readROIinPlace( final String filePath, final long[] xyzctMin, final long[] xyzctMax, final float[] out )
            throws IOException
    {
        final int err = jniReadROI( filePath, xyzctMin, xyzctMax, numThreads, out );
        if ( err != 0 )
            throw new IOException( String.format( "Could not read ROI from KLB file %s, error code %d.", filePath, err ) );
    }

    @Override
    public void readROIinPlace( final String filePath, final long[] xyzctMin, final long[] xyzctMax, final double[] out )
            throws IOException
    {
        final int err = jniReadROI( filePath, xyzctMin, xyzctMax, numThreads, out );
        if ( err != 0 )
            throw new IOException( String.format( "Could not read ROI from KLB file %s, error code %d.", filePath, err ) );
    }


    /***********************************************************
     * Write
     ***********************************************************/

    @Override
    public void writeFull( final byte[] img, final String filePath, final long[] imageSize, final T dataType, final float[] pixelSpacing, final long[] blockSize, final CompressionType compressionType, final byte[] metadata )
            throws IOException
    {
        final int err = jniWriteFull( img, filePath, imageSize, getDataType( dataType ), numThreads, pixelSpacing, blockSize, getCompressionType( compressionType ), metadata );
        if ( err != 0 )
            throw new IOException( "Failed to write " + err );
    }

    @Override
    public void writeFull( final Buffer img, final String filePath, final long[] imageSize, final T dataType, final float[] pixelSpacing, final long[] blockSize, final CompressionType compressionType, final byte[] metadata )
            throws IOException
    {
        final int err = jniWriteFull( img, filePath, imageSize, getDataType( dataType ), numThreads, pixelSpacing, blockSize, getCompressionType( compressionType ), metadata );
        if ( err != 0 )
            throw new IOException( "Failed to write " + err );
    }


    // Override default implementations in org.janelia.simview.klb.KLB to use JNI functions.
    // This avoids copying the image to convert from byte[] to short[], int[], etc.

    @Override
    public void writeFull( final short[] img, final String filePath, final long[] imageSize, final T dataType, final float[] pixelSpacing, final long[] blockSize, final CompressionType compressionType, final byte[] metadata )
            throws IOException
    {
        final int err = jniWriteFull( img, filePath, imageSize, getDataType( dataType ), numThreads, pixelSpacing, blockSize, getCompressionType( compressionType ), metadata );
        if ( err != 0 )
            throw new IOException( "Failed to write " + err );
    }

    @Override
    public void writeFull( final int[] img, final String filePath, final long[] imageSize, final T dataType, final float[] pixelSpacing, final long[] blockSize, final CompressionType compressionType, final byte[] metadata )
            throws IOException
    {
        final int err = jniWriteFull( img, filePath, imageSize, getDataType( dataType ), numThreads, pixelSpacing, blockSize, getCompressionType( compressionType ), metadata );
        if ( err != 0 )
            throw new IOException( "Failed to write " + err );
    }

    @Override
    public void writeFull( final long[] img, final String filePath, final long[] imageSize, final T dataType, final float[] pixelSpacing, final long[] blockSize, final CompressionType compressionType, final byte[] metadata )
            throws IOException
    {
        final int err = jniWriteFull( img, filePath, imageSize, getDataType( dataType ), numThreads, pixelSpacing, blockSize, getCompressionType( compressionType ), metadata );
        if ( err != 0 )
            throw new IOException( "Failed to write " + err );
    }

    @Override
    public void writeFull( final float[] img, final String filePath, final long[] imageSize, final T dataType, final float[] pixelSpacing, final long[] blockSize, final CompressionType compressionType, final byte[] metadata )
            throws IOException
    {
        final int err = jniWriteFull( img, filePath, imageSize, getDataType( dataType ), numThreads, pixelSpacing, blockSize, getCompressionType( compressionType ), metadata );
        if ( err != 0 )
            throw new IOException( "Failed to write " + err );
    }

    @Override
    public void writeFull( final double[] img, final String filePath, final long[] imageSize, final T dataType, final float[] pixelSpacing, final long[] blockSize, final CompressionType compressionType, final byte[] metadata )
            throws IOException
    {
        final int err = jniWriteFull( img, filePath, imageSize, getDataType( dataType ), numThreads, pixelSpacing, blockSize, getCompressionType( compressionType ), metadata );
        if ( err != 0 )
            throw new IOException( "Failed to write " + err );
    }


    /***********************************************************
     * Helper functions
     ***********************************************************/

    /**
     * Get value of data type enum of native library.
     *
     * @param dataType
     * @return
     * @throws IOException
     */
    private int getDataType( final T dataType ) throws IOException
    {
        if ( dataType instanceof UnsignedByteType )
            return 0;
        else if ( dataType instanceof UnsignedShortType )
            return 1;
        else if ( dataType instanceof UnsignedIntType )
            return 2;
        else if ( dataType instanceof UnsignedLongType )
            return 3;
        else if ( dataType instanceof ByteType )
            return 4;
        else if ( dataType instanceof ShortType )
            return 5;
        else if ( dataType instanceof IntType )
            return 6;
        else if ( dataType instanceof LongType )
            return 7;
        else if ( dataType instanceof FloatType )
            return 8;
        else if ( dataType instanceof DoubleType )
            return 9;
        else
            throw new IOException( "Unknown or unsupported data type" );
    }

    /**
     * Get value of compression type enum of native library.
     * Default value is 1 (BZIP2), which is returned in case of any errors.
     *
     * @param compressionType
     * @return value of compression type enum of native library
     */
    private int getCompressionType( final CompressionType compressionType )
    {
        try {
            if ( compressionType == CompressionType.NONE )
                return 0;
            else if ( compressionType == CompressionType.BZIP2 )
                return 1;
            else if ( compressionType == CompressionType.ZLIB )
                return 2;
            else
                return 1;
        } catch ( Exception ex ) {
            return 1;
        }
    }


    /***********************************************************
     * JNI function declarations
     ***********************************************************/

    private native int jniReadHeader( final String filePath, final long[] imageSize, final long[] blockSize, final float[] pixelSpacing, final int[] dataAndCompressionType, final byte[] metadata );

    private native int jniReadFull( final String filePath, final int numThreads, final byte[] out );

    private native int jniReadFull( final String filePath, final int numThreads, final short[] out );

    private native int jniReadFull( final String filePath, final int numThreads, final int[] out );

    private native int jniReadFull( final String filePath, final int numThreads, final long[] out );

    private native int jniReadFull( final String filePath, final int numThreads, final float[] out );

    private native int jniReadFull( final String filePath, final int numThreads, final double[] out );

    private native int jniReadFull( final String filePath, final int numThreads, final Buffer out );

    private native int jniReadROI( final String filePath, final long[] xyzctMin, final long[] xyzctMax, final int numThreads, final byte[] out );

    private native int jniReadROI( final String filePath, final long[] xyzctMin, final long[] xyzctMax, final int numThreads, final short[] out );

    private native int jniReadROI( final String filePath, final long[] xyzctMin, final long[] xyzctMax, final int numThreads, final int[] out );

    private native int jniReadROI( final String filePath, final long[] xyzctMin, final long[] xyzctMax, final int numThreads, final long[] out );

    private native int jniReadROI( final String filePath, final long[] xyzctMin, final long[] xyzctMax, final int numThreads, final float[] out );

    private native int jniReadROI( final String filePath, final long[] xyzctMin, final long[] xyzctMax, final int numThreads, final double[] out );

    private native int jniReadROI( final String filePath, final long[] xyzctMin, final long[] xyzctMax, final int numThreads, final Buffer out );

    private native int jniWriteFull( final byte[] img, final String filePath, final long[] imageSize, final int dataType, final int numThreads, final float[] pixelSpacing, final long[] blockSize, final int compressionType, final byte[] metadata );

    private native int jniWriteFull( final short[] img, final String filePath, final long[] imageSize, final int dataType, final int numThreads, final float[] pixelSpacing, final long[] blockSize, final int compressionType, final byte[] metadata );

    private native int jniWriteFull( final int[] img, final String filePath, final long[] imageSize, final int dataType, final int numThreads, final float[] pixelSpacing, final long[] blockSize, final int compressionType, final byte[] metadata );

    private native int jniWriteFull( final long[] img, final String filePath, final long[] imageSize, final int dataType, final int numThreads, final float[] pixelSpacing, final long[] blockSize, final int compressionType, final byte[] metadata );

    private native int jniWriteFull( final float[] img, final String filePath, final long[] imageSize, final int dataType, final int numThreads, final float[] pixelSpacing, final long[] blockSize, final int compressionType, final byte[] metadata );

    private native int jniWriteFull( final double[] img, final String filePath, final long[] imageSize, final int dataType, final int numThreads, final float[] pixelSpacing, final long[] blockSize, final int compressionType, final byte[] metadata );

    private native int jniWriteFull( final Buffer img, final String filePath, final long[] imageSize, final int dataType, final int numThreads, final float[] pixelSpacing, final long[] blockSize, final int compressionType, final byte[] metadata );
}
