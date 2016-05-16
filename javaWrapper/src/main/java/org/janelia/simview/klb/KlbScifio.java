package org.janelia.simview.klb;

import io.scif.*;
import io.scif.config.SCIFIOConfig;
import io.scif.io.RandomAccessInputStream;
import io.scif.util.FormatTools;
import net.imagej.axis.*;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.*;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import org.scijava.Priority;
import org.scijava.plugin.Plugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Plugin( type = Format.class, priority = Priority.VERY_HIGH_PRIORITY + 1, name = "Keller Lab Block File Format" )
public class KlbScifio extends AbstractFormat
{

    protected static final AxisType[] AXIS_TYPES = { Axes.X, Axes.Y, Axes.Z, Axes.CHANNEL, Axes.TIME };
    private static final String[] UNITS = { "um", "um", "um", "au", "s" };

    @Override
    protected String[] makeSuffixArray()
    {
        return new String[]{ "klb" };
    }

    @Override
    public String getFormatName()
    {
        return "Keller Lab Block File Format";
    }


    public static class Metadata extends AbstractMetadata
    {
        @Override
        public void populateImageMetadata()
        {
        }
    }


    public static class Parser extends AbstractParser< Metadata >
    {

        private final KLB klb = KLB.newInstance();

        @Override
        protected void typedParse( final RandomAccessInputStream source, final Metadata meta, final SCIFIOConfig config )
                throws IOException, FormatException
        {
            final KLB.Header header = klb.readHeader( getSource().getFileName() );

            meta.createImageMetadata( 1 );
            final ImageMetadata iMeta = meta.get( 0 );

            final long[] dimensions = header.imageSize;
            final float[] pixelSpacing = header.pixelSpacing;
            final List< CalibratedAxis > axes = new ArrayList< CalibratedAxis >( pixelSpacing.length );
            for ( int d = 0; d < pixelSpacing.length; ++d ) {
                axes.add( new DefaultLinearAxis( AXIS_TYPES[ d ], UNITS[ d ], pixelSpacing[ d ] ) );
            }

            boolean isCalibrated = false;
            for ( final double s : pixelSpacing ) {
                if ( s != 1f ) {
                    isCalibrated = true;
                    break;
                }
            }
            if ( !isCalibrated ) {
                for ( int d = 0; d < 3; ++d )
                    axes.get( d ).setUnit( "pixel" );
            }

            int format;
            final Class clazz = header.dataType.getClass();
            if ( clazz == UnsignedByteType.class )
                format = FormatTools.UINT8;
            else if ( clazz == UnsignedShortType.class )
                format = FormatTools.UINT16;
            else if ( clazz == UnsignedIntType.class )
                format = FormatTools.UINT32;
            else if ( clazz == ByteType.class )
                format = FormatTools.INT8;
            else if ( clazz == ShortType.class )
                format = FormatTools.INT16;
            else if ( clazz == IntType.class )
                format = FormatTools.INT32;
            else if ( clazz == FloatType.class )
                format = FormatTools.FLOAT;
            else if ( clazz == DoubleType.class )
                format = FormatTools.DOUBLE;
            else
                throw new FormatException( String.format( "Unknown or unsupported KLB data type of file %s.", getSource().getFileName() ) );

            iMeta.populate( source.getFileName(), axes, dimensions, format, true, true, false, false, true );
            iMeta.setPlanarAxisCount( 3 ); // should be Klb.KLB_DATA_DIMS (=5), but ImageJ doesn't like that currently

            // if uncompressed image >2GB, SCIFIO currently has to read 2D planes
            long nElements = 1;
            for ( final long d : dimensions )
                nElements *= d;
            nElements *= iMeta.getBitsPerPixel() / 8;
            if ( nElements > Integer.MAX_VALUE )
                iMeta.setPlanarAxisCount( 2 );
        }
    }


    public static class Reader extends ByteArrayReader< Metadata >
    {

        private final KLB klb = KLB.newInstance();
        private final long[]
                klb_min = new long[ 5 ],
                klb_max = new long[ 5 ];

        // cache
        private byte[] cachedPlanes;
        private int numPlaneBytes;
        private long cacheMin, cacheMax;

        @Override
        public ByteArrayPlane openPlane( final int imageIndex, final long planeIndex, final ByteArrayPlane plane, final long[] min, final long[] max, final SCIFIOConfig config )
                throws FormatException, IOException
        {
            // ImageJ doesn't currently like >3D planes, but KlbRoi is 5D
            System.arraycopy( min, 0, klb_min, 0, min.length );
            System.arraycopy( max, 0, klb_max, 0, max.length );
            for ( int d = 0; d < max.length; ++d )
                klb_max[ d ] -= 1;

            /*
             * If uncompressed image >2GB, SCIFIO currently has to read 2D planes.
             * Planar axes count is thus set to 2 when parsing metadata (see above).
             * If so, decompress a block of numPlanarBlocks*blockDims[2] full planes
             * at once and keep them in a cache.
             */
            final ImageMetadata iMeta = getMetadata().get( imageIndex );
            if ( iMeta.getPlanarAxisCount() == 2 ) {
                final int numPlanarBlocks = 6;
                final long lastPlane = iMeta.getAxisLength( 2 ) - 1;
                numPlaneBytes = ( int ) (max[ 0 ] * max[ 1 ]) * (iMeta.getBitsPerPixel() / 8);
                final long[] blockDims = klb.readHeader( getCurrentFile() ).blockSize;
                final int numBufferBytes = numPlanarBlocks * ( int ) blockDims[ 2 ] * numPlaneBytes;
                if ( cachedPlanes == null || numBufferBytes != cachedPlanes.length ) {
                    cachedPlanes = new byte[ numBufferBytes ];
                    cacheMin = cacheMax = -1;
                }
                if ( planeIndex < cacheMin || planeIndex > cacheMax ) {
                    cacheMin = (planeIndex / blockDims[ 2 ]) * blockDims[ 2 ];
                    cacheMax = cacheMin + numPlanarBlocks * blockDims[ 2 ] - 1;
                    klb_min[ 2 ] = cacheMin;
                    klb_max[ 2 ] = cacheMax;
                    klb.readROIinPlace( getCurrentFile(), klb_min, klb_max, cachedPlanes );
                }
                final int start = ( int ) (planeIndex - cacheMin) * numPlaneBytes;
                System.arraycopy( cachedPlanes, start, plane.getBytes(), 0, numPlaneBytes );
                if ( planeIndex == lastPlane )
                    cachedPlanes = null;
                return plane;
            }

            // if planar axes count >2, read normally
            klb.readROIinPlace( getCurrentFile(), klb_min, klb_max, plane.getBytes() );
            return plane;
        }

        @Override
        protected String[] createDomainArray()
        {
            return new String[]{ FormatTools.LM_DOMAIN };
        }
    }


    public static class Writer< T extends RealType< ? > & NativeType< ? > > extends AbstractWriter< Metadata >
    {

        private final KLB klb = KLB.newInstance();
        private final long[] dimensions = new long[ 5 ];
        private final float[] sampling = new float[ 5 ];

        @Override
        public void writePlane( final int imageIndex, final long planeIndex, final Plane plane, final long[] min, final long[] max )
                throws FormatException, IOException
        {
            log().debug( String.format( "KLB: %s.writePlane(imageIndex %d, planeIndex %d, Plane, min %s, max %s)", getClass().getSimpleName(), imageIndex, planeIndex, Util.printCoordinates( min ), Util.printCoordinates( max ) ) );
            final ImageMetadata iMeta = getMetadata().get( imageIndex );
            for ( int d = 0; d < dimensions.length; ++d ) {
                try {
                    final CalibratedAxis axis = iMeta.getAxis( d );
                    dimensions[ d ] = iMeta.getAxisLength( d );
                    sampling[ d ] = ( float ) (( LinearAxis ) axis).scale();
                } catch ( IndexOutOfBoundsException ex ) {
                    dimensions[ d ] = 1L;
                    sampling[ d ] = 1f;
                }
            }
            log().debug( "KLB: Dimensions: " + Util.printCoordinates( dimensions ) );
            log().debug( "KLB: Sampling: " + Util.printCoordinates( sampling ) );

            // KLB writes the full image at once, SCIFIO currently handles 2GB max at once.
            long nElements = 1;
            for ( final long d : dimensions )
                nElements *= d;
            nElements *= iMeta.getBitsPerPixel() / 8;
            if ( nElements > Integer.MAX_VALUE )
                throw new FormatException( "Writing KLB files bigger than 2 GB (uncompressed) is currently not supported through SCIFIO/ImageJ." );

            T type;
            switch ( iMeta.getPixelType() ) {
                case FormatTools.UINT8:
                    type = ( T ) new UnsignedByteType();
                    break;
                case FormatTools.UINT16:
                    type = ( T ) new UnsignedShortType();
                    break;
                case FormatTools.UINT32:
                    type = ( T ) new UnsignedIntType();
                    break;
                case FormatTools.INT8:
                    type = ( T ) new ShortType();
                    break;
                case FormatTools.INT16:
                    type = ( T ) new ByteType();
                    break;
                case FormatTools.INT32:
                    type = ( T ) new IntType();
                    break;
                case FormatTools.FLOAT:
                    type = ( T ) new FloatType();
                    break;
                case FormatTools.DOUBLE:
                    type = ( T ) new DoubleType();
                    break;
                default:
                    throw new FormatException( "Unknown or unsupported data type" );
            }

            klb.writeFull( plane.getBytes(), getMetadata().getDatasetName(), dimensions, type, sampling, null, null, null );
        }

        @Override
        public boolean canDoStacks()
        {
            return true;
        }

        @Override
        public boolean writeSequential()
        {
            return false;
        }

        @Override
        protected String[] makeCompressionTypes()
        {
            return new String[]{ "none", "bzip2" };
        }
    }


    @Plugin( type = Translator.class )
    public static class Translator extends AbstractTranslator< io.scif.Metadata, Metadata >
    {

        // after adding the @Plugin annotation, Klb.KLB_DATA_DIMS is unavailable, hence use magic 5;
        private final long[] dimensions = new long[ 5 ];

        @Override
        public Class< ? extends io.scif.Metadata > source()
        {
            return io.scif.Metadata.class;
        }

        @Override
        public Class< ? extends io.scif.Metadata > dest()
        {
            return Metadata.class;
        }

        @Override
        protected void translateImageMetadata( final List< ImageMetadata > src, final Metadata dst )
        {
            dst.createImageMetadata( src.size() );
            for ( int imageIndex = 0; imageIndex < src.size(); ++imageIndex ) {
                final ImageMetadata srcIMeta = src.get( imageIndex );
                final ImageMetadata dstIMeta = dst.get( imageIndex );
                final List< CalibratedAxis > axes = new ArrayList< CalibratedAxis >( AXIS_TYPES.length );
                for ( int d = 0; d < dimensions.length; ++d ) {
                    final int axisIdx = srcIMeta.getAxisIndex( AXIS_TYPES[ d ] );
                    if ( axisIdx != -1 ) {
                        axes.add( srcIMeta.getAxis( axisIdx ).copy() );
                        dimensions[ d ] = srcIMeta.getAxisLength( d );
                    } else {
                        axes.add( new DefaultLinearAxis( AXIS_TYPES[ d ], UNITS[ d ], 1f ) );
                        dimensions[ d ] = 1L;
                    }
                }
                dstIMeta.populate( dst.getDatasetName(), axes, dimensions, srcIMeta.getPixelType(), true, true, false, false, true );
                dstIMeta.setPlanarAxisCount( 3 ); // should be Klb.KLB_DATA_DIMS (=5), but ImageJ doesn't like that currently
            }
        }
    }
}
