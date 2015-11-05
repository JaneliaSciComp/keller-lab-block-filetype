package org.janelia.simview.klb.bdv;

import bdv.ViewerSetupImgLoader;
import bdv.img.cache.*;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.ImgLoaderHint;
import mpicbg.spim.data.generic.sequence.ImgLoaderHints;
import mpicbg.spim.data.sequence.MultiResolutionSetupImgLoader;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.Cursor;
import net.imglib2.Dimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ShortArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;
import net.imglib2.img.cell.CellImg;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.img.cell.DefaultCell;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.type.volatiles.VolatileUnsignedShortType;
import net.imglib2.util.Fraction;
import org.janelia.simview.klb.jni.KlbImageIO;
import org.janelia.simview.klb.jni.KlbRoi;
import spim.Threads;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Map;

public class KlbSetupImgLoaderUInt16 implements ViewerSetupImgLoader< UnsignedShortType, VolatileUnsignedShortType >, MultiResolutionSetupImgLoader< UnsignedShortType >
{
    private final UnsignedShortType type = new UnsignedShortType();
    private final VolatileUnsignedShortType volatileType = new VolatileUnsignedShortType();

    private final int viewSetupId;
    private final AbstractSequenceDescription< ?, ?, ? > seq;
    private final KlbPartitionResolver resolver;
    private final long[] imageDimensions = new long[ 3 ];
    private final int[] blockDimensions = new int[ 3 ];
    private final KlbVolatileArrayLoaderUInt16 arrayLoader;
    private double[][] mipMapResolutions;
    private AffineTransform3D[] mipMapTransforms;
    private final VolatileGlobalCellCache cache;

    public KlbSetupImgLoaderUInt16( final AbstractSequenceDescription< ?, ?, ? > seq, final KlbPartitionResolver resolver, final int viewSetupId, final VolatileGlobalCellCache cache )
    {
        this.cache = cache;
        this.seq = seq;
        this.resolver = resolver;
        this.viewSetupId = viewSetupId;
        arrayLoader = new KlbVolatileArrayLoaderUInt16( resolver );
    }

    @Override
    public Dimensions getImageSize( final int timePointId, final int level )
    {
        return seq.getViewSetups().get( viewSetupId ).getSize();
    }

    @Override
    public Dimensions getImageSize( final int timePointId )
    {
        return getImageSize( timePointId, 0 );
    }

    @Override
    public VoxelDimensions getVoxelSize( final int timePointId )
    {
        return seq.getViewSetups().get( viewSetupId ).getVoxelSize();
    }

    @Override
    public RandomAccessibleInterval< UnsignedShortType > getImage( final int timePointId, final int level, final ImgLoaderHint... hints )
    {
        if ( Arrays.asList( hints ).contains( ImgLoaderHints.LOAD_COMPLETELY ) ) {
            return loadImageCompletely( timePointId, level );
        }
        final CachedCellImg< UnsignedShortType, VolatileShortArray > img = prepareCachedImage( timePointId, level, LoadingStrategy.BLOCKING );
        final UnsignedShortType linkedType = new UnsignedShortType( img );
        img.setLinkedType( linkedType );
        return img;
    }

    @Override
    public RandomAccessibleInterval< UnsignedShortType > getImage( final int timePointId, final ImgLoaderHint... hints )
    {
        return getImage( timePointId, 0, hints );
    }

    @Override
    public RandomAccessibleInterval< FloatType > getFloatImage( final int timePointId, final int level, final boolean normalize, final ImgLoaderHint... hints )
    {
        return null;
    }

    @Override
    public RandomAccessibleInterval< FloatType > getFloatImage( final int timePointId, final boolean normalize, final ImgLoaderHint... hints )
    {
        return getFloatImage( timePointId, 0, normalize, hints );
    }

    @Override
    public RandomAccessibleInterval< VolatileUnsignedShortType > getVolatileImage( final int timePointId, final int level, final ImgLoaderHint... hints )
    {
        final CachedCellImg< VolatileUnsignedShortType, VolatileShortArray > img = prepareCachedImage( timePointId, level, LoadingStrategy.VOLATILE );
        final VolatileUnsignedShortType linkedType = new VolatileUnsignedShortType( img );
        img.setLinkedType( linkedType );
        return img;
    }

    private < T extends NativeType< T > > CachedCellImg< T, VolatileShortArray > prepareCachedImage( final int timePointId, final int level, final LoadingStrategy loadingStrategy )
    {
        seq.getViewSetups().get( viewSetupId ).getSize().dimensions( imageDimensions );
        if ( blockDimensions[ 0 ] == 0 ) {
            if ( !resolver.getBlockDimensions( timePointId, viewSetupId, level, blockDimensions ) ) {
                final Map< Integer, TimePoint > timePoints = seq.getTimePoints().getTimePoints();
                for ( final Integer t : timePoints.keySet() ) {
                    if ( resolver.getBlockDimensions( timePoints.get( t ).getId(), viewSetupId, level, blockDimensions ) ) {
                        break;
                    }
                }
            }
        }

        final int priority = resolver.getNumResolutionLevels( viewSetupId ) - 1 - level;
        final CacheHints cacheHints = new CacheHints( loadingStrategy, priority, false );
        final VolatileImgCells.CellCache< VolatileShortArray > c = cache.new VolatileCellCache( timePointId, viewSetupId, level, cacheHints, arrayLoader );
        final VolatileImgCells< VolatileShortArray > cells = new VolatileImgCells< VolatileShortArray >( c, new Fraction(), imageDimensions, blockDimensions );
        return new CachedCellImg< T, VolatileShortArray >( cells );
    }

    private RandomAccessibleInterval< UnsignedShortType > loadImageCompletely( final int timePointId, final int level )
    {
        seq.getViewSetups().get( viewSetupId ).getSize().dimensions( imageDimensions );
        long numBytes = arrayLoader.getBytesPerElement();
        for ( final long d : imageDimensions ) {
            numBytes *= d;
        }
        final int[] dimsInt = new int[ imageDimensions.length ];
        for ( int d = 0; d < dimsInt.length; ++d ) {
            dimsInt[ d ] = ( int ) imageDimensions[ d ];
        }

        final KlbImageIO io = new KlbImageIO( (( KlbPartitionResolverDefault ) resolver).getFilePath( timePointId, viewSetupId, level ) );
        io.readHeader();
        if ( numBytes <= Integer.MAX_VALUE ) {
            final ByteBuffer bytes = ByteBuffer.allocate( ( int ) numBytes );
            io.readImageFull( bytes.array(), Threads.numThreads() );
            final ArrayImg img = ArrayImgs.unsignedShorts( bytes.asShortBuffer().array(), imageDimensions );
            return img;
        } else {
            if ( blockDimensions[ 0 ] == 0 ) {
                if ( !resolver.getBlockDimensions( timePointId, viewSetupId, level, blockDimensions ) ) {
                    final Map< Integer, TimePoint > timePoints = seq.getTimePoints().getTimePoints();
                    for ( final Integer t : timePoints.keySet() ) {
                        if ( resolver.getBlockDimensions( timePoints.get( t ).getId(), viewSetupId, level, blockDimensions ) ) {
                            break;
                        }
                    }
                }
            }
            final CellImgFactory< UnsignedShortType > factory = new CellImgFactory< UnsignedShortType >( blockDimensions );
            final CellImg< UnsignedShortType, ShortArray, DefaultCell< ShortArray > > cellImg =
                    ( CellImg< UnsignedShortType, ShortArray, DefaultCell< ShortArray > > ) factory.create( imageDimensions, new UnsignedShortType() );
            final Cursor< DefaultCell< ShortArray > > cursor = cellImg.getCells().cursor();
            final long[] min = new long[ dimsInt.length ];
            final KlbRoi roi = new KlbRoi();
            while ( cursor.hasNext() ) {
                final DefaultCell< ShortArray > cell = cursor.next();
                final short[] dataBlock = cell.getData().getCurrentStorageArray();
                io.readImage( null, roi, Threads.numThreads() );
            }
            return null;
        }
    }

    @Override
    public int numMipmapLevels()
    {
        return resolver.getNumResolutionLevels( viewSetupId );
    }

    @Override
    public double[][] getMipmapResolutions()
    {
        if ( mipMapResolutions == null ) {
            mipMapResolutions = new double[ resolver.getNumResolutionLevels( viewSetupId ) ][ 3 ];
            for ( int level = 0; level < mipMapResolutions.length; ++level ) {
                resolver.getSampling( resolver.getFirstTimePoint(), viewSetupId, level, mipMapResolutions[ level ] );
            }
        }
        return mipMapResolutions;
    }

    @Override
    public AffineTransform3D[] getMipmapTransforms()
    {
        if ( mipMapTransforms == null ) {
            mipMapTransforms = new AffineTransform3D[ resolver.getNumResolutionLevels( viewSetupId ) ];
            for ( int level = 0; level < mipMapTransforms.length; ++level ) {
                mipMapTransforms[ level ] = new AffineTransform3D();
            }
        }
        return mipMapTransforms;
    }

    @Override
    public UnsignedShortType getImageType()
    {
        return type;
    }

    @Override
    public VolatileUnsignedShortType getVolatileImageType()
    {
        return volatileType;
    }


    public class KlbVolatileArrayLoaderUInt16 implements CacheArrayLoader< VolatileShortArray >
    {

        final private KlbPartitionResolver resolver;
        private VolatileShortArray theEmptyArray;

        public KlbVolatileArrayLoaderUInt16( final KlbPartitionResolver resolver )
        {
            this.resolver = resolver;
            theEmptyArray = new VolatileShortArray( 96 * 96 * 8, false );
        }

        @Override
        public int getBytesPerElement()
        {
            return 2;
        }

        @Override
        public VolatileShortArray loadArray(
                final int timePoint,
                final int viewSetup,
                final int level,
                final int[] dimensions,
                final long[] offset
        )
                throws InterruptedException
        {
            try {
                return tryLoadArray( timePoint, viewSetup, level, dimensions, offset );
            } catch ( final OutOfMemoryError e ) {
                cache.clearCache();
                System.gc();
                return tryLoadArray( timePoint, viewSetup, level, dimensions, offset );
            }
        }

        private VolatileShortArray tryLoadArray(
                final int timePoint,
                final int viewSetup,
                final int level,
                final int[] dimensions,
                final long[] offset
        )
                throws InterruptedException
        {
            final KlbImageIO io = new KlbImageIO();
            final KlbRoi roi = new KlbRoi();
            final ByteBuffer bytes = ByteBuffer.allocate( 2 * dimensions[ 0 ] * dimensions[ 1 ] * dimensions[ 2 ] );
            bytes.order( ByteOrder.LITTLE_ENDIAN );
            resolver.set( timePoint, viewSetup, level, dimensions, offset, io, roi );
            if ( io.readImage( bytes.array(), roi, 1 ) == 0 ) {
                final short[] shorts = new short[ bytes.limit() / 2 ];
                bytes.asShortBuffer().get( shorts );
                return new VolatileShortArray( shorts, true );
            }
            return new VolatileShortArray( bytes.limit() / 2, true );
        }

        @Override
        public VolatileShortArray emptyArray( final int[] dimensions )
        {
            int numEntities = 1;
            for ( int d : dimensions )
                numEntities *= d;
            if ( theEmptyArray.getCurrentStorageArray().length < numEntities )
                theEmptyArray = new VolatileShortArray( numEntities, false );
            return theEmptyArray;
        }
    }
}

