package org.janelia.simview.klb.bdv;

import bdv.AbstractViewerImgLoader;
import bdv.img.cache.*;
import bdv.img.cache.VolatileImgCells.CellCache;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.sequence.ImgLoader;
import mpicbg.spim.data.sequence.ViewId;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.*;
import net.imglib2.img.Img;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.type.volatiles.VolatileUnsignedShortType;
import net.imglib2.util.Fraction;
import net.imglib2.view.Views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KlbImageLoader
        extends AbstractViewerImgLoader< UnsignedShortType, VolatileUnsignedShortType >
        implements ImgLoader< UnsignedShortType >
{
    private final KlbPartitionResolver resolver;
    private final AbstractSequenceDescription< ?, ?, ? > sequenceDescription;
    private final Map< Integer, double[][] > mipMapResolutionMap = new HashMap< Integer, double[][] >();
    private final Map< Integer, AffineTransform3D[] > mipMapTransformMap = new HashMap< Integer, AffineTransform3D[] >();

    private VolatileGlobalCellCache< VolatileShortArray > cache;

    public KlbImageLoader( final KlbPartitionResolver resolver, final AbstractSequenceDescription< ?, ?, ? > seq )
    {
        super( new UnsignedShortType(), new VolatileUnsignedShortType() );
        this.resolver = resolver;
        sequenceDescription = seq;
        final int numTimePoints = 1 + resolver.getLastTimePoint() - resolver.getFirstTimePoint();

        cache = new VolatileGlobalCellCache< VolatileShortArray >(
                new KlbVolatileShortArrayLoader( resolver ),
                numTimePoints,
                resolver.getNumViewSetups(),
                resolver.getMaxNumResolutionLevels(),
                Runtime.getRuntime().availableProcessors()
        );
    }

    public KlbPartitionResolver getResolver()
    {
        return resolver;
    }

    @Override
    public int numMipmapLevels( final int viewSetup )
    {
        return resolver.getNumResolutionLevels( viewSetup );
    }

    @Override
    public double[][] getMipmapResolutions( final int viewSetup )
    {
        double[][] resolutions = mipMapResolutionMap.get( viewSetup );
        if ( resolutions == null ) {
            resolutions = new double[ resolver.getNumResolutionLevels( viewSetup ) ][ 3 ];
            for ( int level = 0; level < resolutions.length; ++level ) {
                resolver.getSampling( resolver.getFirstTimePoint(), viewSetup, level, resolutions[ level ] );
            }
            mipMapResolutionMap.put( viewSetup, resolutions );
        }
        return resolutions;
    }

    @Override
    public AffineTransform3D[] getMipmapTransforms( final int viewSetup )
    {
        AffineTransform3D[] transforms = mipMapTransformMap.get( viewSetup );
        if ( transforms == null ) {
            transforms = new AffineTransform3D[ resolver.getNumResolutionLevels( viewSetup ) ];
            for ( int level = 0; level < transforms.length; ++level ) {
                transforms[ level ] = new AffineTransform3D();
            }
            mipMapTransformMap.put( viewSetup, transforms );
        }
        return transforms;
    }

    @Override
    public Cache getCache()
    {
        return cache;
    }

    @Override
    public Dimensions getImageSize( final ViewId view )
    {
        return sequenceDescription.getViewSetups().get( view.getViewSetupId() ).getSize();
    }

    @Override
    public VoxelDimensions getVoxelSize( final ViewId view )
    {
        return sequenceDescription.getViewSetups().get( view.getViewSetupId() ).getVoxelSize();
    }

    @Override
    public UnsignedShortType getImageType()
    {
        return new UnsignedShortType();
    }

    @Override
    public RandomAccessibleInterval< UnsignedShortType > getImage( final ViewId view )
    {
        return getImage( view, 0 );
    }

    @Override
    public RandomAccessibleInterval< UnsignedShortType > getImage( final ViewId view, final int level )
    {
        final CachedCellImg< UnsignedShortType, VolatileShortArray > img = prepareCachedImage( view, level, LoadingStrategy.BLOCKING );
        final UnsignedShortType linkedType = new UnsignedShortType( img );
        img.setLinkedType( linkedType );
        return img;
    }

    @Override
    public RandomAccessibleInterval< VolatileUnsignedShortType > getVolatileImage( final ViewId view, final int level )
    {
        final CachedCellImg< VolatileUnsignedShortType, VolatileShortArray > img = prepareCachedImage( view, level, LoadingStrategy.VOLATILE );
        final VolatileUnsignedShortType linkedType = new VolatileUnsignedShortType( img );
        img.setLinkedType( linkedType );
        return img;
    }

    /**
     * (Almost) create a {@link CachedCellImg} backed by the cache. The created image
     * needs a {@link net.imglib2.img.NativeImg#setLinkedType(net.imglib2.type.Type) linked
     * type} before it can be used. The type should be either {@link net.imglib2.type.numeric.ARGBType}
     * and {@link net.imglib2.type.volatiles.VolatileARGBType}.
     */
    protected < T extends NativeType< T > > CachedCellImg< T, VolatileShortArray > prepareCachedImage( final ViewId view, final int level, final LoadingStrategy loadingStrategy )
    {
        final int timePoint = view.getTimePointId();
        final int viewSetup = view.getViewSetupId();

        final long[] imageDimensions = new long[ 3 ];
        final int[] blockDimensions = new int[ 3 ];
        resolver.getImageDimensions( timePoint, viewSetup, 0, imageDimensions );
        resolver.getBlockDimensions( timePoint, viewSetup, 0, blockDimensions );

        final int priority = resolver.getNumResolutionLevels( viewSetup ) - 1 - level;
        final CacheHints cacheHints = new CacheHints( loadingStrategy, priority, false );
        final CellCache< VolatileShortArray > c = cache.new VolatileCellCache( timePoint, viewSetup, level, cacheHints );
        final VolatileImgCells< VolatileShortArray > cells = new VolatileImgCells< VolatileShortArray >( c, new Fraction(), imageDimensions, blockDimensions );
        final CachedCellImg< T, VolatileShortArray > img = new CachedCellImg< T, VolatileShortArray >( cells );
        return img;
    }

    // This function was copied from bdv.img.hdf5.Hdf5ImageLoader by Tobias Pietzsch et al.
    @Override
    public RandomAccessibleInterval< FloatType > getFloatImage( final ViewId view, final boolean normalize )
    {
        final RandomAccessibleInterval< UnsignedShortType > ushortImg = getImage( view, 0 );

        // copy unsigned short img to float img
        final FloatType f = new FloatType();
        final Img< FloatType > floatImg = net.imglib2.util.Util.getArrayOrCellImgFactory( ushortImg, f ).create( ushortImg, f );

        // set up executor service
        final int numProcessors = Runtime.getRuntime().availableProcessors();
        final ExecutorService taskExecutor = Executors.newFixedThreadPool( numProcessors );
        final ArrayList< Callable< Void > > tasks = new ArrayList< Callable< Void > >();

        // set up all tasks
        final int numPortions = numProcessors * 2;
        final long threadChunkSize = floatImg.size() / numPortions;
        final long threadChunkMod = floatImg.size() % numPortions;

        for ( int portionID = 0; portionID < numPortions; ++portionID ) {
            // move to the starting position of the current thread
            final long startPosition = portionID * threadChunkSize;

            // the last thread may has to run longer if the number of pixels cannot be divided by the number of threads
            final long loopSize = (portionID == numPortions - 1) ? threadChunkSize + threadChunkMod : threadChunkSize;

            tasks.add( new Callable< Void >()
            {
                @Override
                public Void call() throws Exception
                {
                    final Cursor< UnsignedShortType > in = Views.iterable( ushortImg ).localizingCursor();
                    final RandomAccess< FloatType > out = floatImg.randomAccess();

                    in.jumpFwd( startPosition );

                    for ( long j = 0; j < loopSize; ++j ) {
                        final UnsignedShortType vin = in.next();
                        out.setPosition( in );
                        out.get().set( vin.getRealFloat() );
                    }

                    return null;
                }
            } );
        }

        try {
            // invokeAll() returns when all tasks are complete
            taskExecutor.invokeAll( tasks );
            taskExecutor.shutdown();
        } catch ( final InterruptedException e ) {
            return null;
        }

        if ( normalize )
            // normalize the image to 0...1
            normalize( floatImg );

        return floatImg;
    }

    // This function was copied from bdv.img.hdf5.Hdf5ImageLoader by Tobias Pietzsch et al.
    private void normalize( final IterableInterval< FloatType > img )
    {
        float currentMax = img.firstElement().get();
        float currentMin = currentMax;
        for ( final FloatType t : img ) {
            final float f = t.get();
            if ( f > currentMax )
                currentMax = f;
            else if ( f < currentMin )
                currentMin = f;
        }

        final float scale = ( float ) (1.0 / (currentMax - currentMin));
        for ( final FloatType t : img )
            t.set( (t.get() - currentMin) * scale );
    }
}
