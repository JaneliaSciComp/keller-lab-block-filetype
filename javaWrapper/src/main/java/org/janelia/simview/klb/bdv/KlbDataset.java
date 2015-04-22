package org.janelia.simview.klb.bdv;

import net.imglib2.realtransform.AffineTransform3D;

/**
 * Defines a dataset of KLB files and metadata. Constructed around a
 * KlbPartitionResolver instance.
 * <p/>
 * Interfaces with KLB files through a KlbPartitionResolver. Is used
 * by KlbSpimDataAdapter to generate a SpimData2 instance that can be
 * consumed by Fiji's Big Data Viewer and SPIM plugins and/or saved
 * to XML.
 */
public class KlbDataset
{

    private final long[][] imageDimensions;
    private final int[][] blockDimensions;
    private final double[][] sampling;
    private final AffineTransform3D[] mipMapTransforms;
    private final KlbPartitionResolver resolver;

    /**
     * Read sampling from file
     *
     * @param resolver
     */
    public KlbDataset( final KlbPartitionResolver resolver )
    {
        this.resolver = resolver;

        imageDimensions = new long[ getNumResolutionLevels( resolver.getFirstViewSetup() ) ][ 3 ];

        // Define time point and channel to read image metadata from.
        // Data files can be missing, so probe until one is found.
        // Start with last time point to catch the biggest volume in
        // case the image volume is growing over time.
        // Currently, we are assuming a constant image size, and same
        // sampling and number of resolution levels for all channels.
        int timePoint = resolver.getLastTimePoint();
        int viewSetup = resolver.getFirstViewSetup();
        time:
        for (; timePoint >= resolver.getFirstTimePoint(); --timePoint ) {
            for (; viewSetup <= resolver.getLastViewSetup(); ++viewSetup ) {
                if ( resolver.getImageDimensions( timePoint, viewSetup, 0, imageDimensions[ 0 ] ) ) {
                    break time;
                }
            }
        }

        for ( int i = 1; i < imageDimensions.length; ++i ) {
            resolver.getImageDimensions( timePoint, viewSetup, i, imageDimensions[ i ] );
        }

        blockDimensions = new int[ getNumResolutionLevels( viewSetup ) ][ 3 ];
        for ( int i = 0; i < blockDimensions.length; ++i ) {
            resolver.getBlockDimensions( timePoint, viewSetup, i, blockDimensions[ i ] );
        }

        sampling = new double[ getNumResolutionLevels( viewSetup ) ][ 3 ];
        for ( int i = 0; i < imageDimensions.length; ++i ) {
            resolver.getSampling( timePoint, viewSetup, i, sampling[ i ] );
        }

        mipMapTransforms = new AffineTransform3D[ getNumResolutionLevels( viewSetup ) ];
        for ( int i = 0; i < mipMapTransforms.length; ++i ) {
            mipMapTransforms[ i ] = new AffineTransform3D();
        }
    }

    /**
     * Manually specify sampling
     *
     * @param resolver
     * @param sampling
     */
    public KlbDataset( final KlbPartitionResolver resolver, final double[] sampling )
    {
        this.resolver = resolver;

        imageDimensions = new long[ getNumResolutionLevels( resolver.getFirstViewSetup() ) ][ 3 ];

        // Define time point and channel to read image metadata from.
        // Data files can be missing, so probe until one is found.
        // Start with last time point to catch the biggest volume in
        // case the image volume is growing over time.
        // Currently, we are assuming a constant image size, and same
        // sampling and number of resolution levels for all channels.
        int timePoint = resolver.getLastTimePoint();
        int viewSetup = resolver.getFirstViewSetup();
        time:
        for (; timePoint >= resolver.getFirstTimePoint(); --timePoint ) {
            for (; viewSetup <= resolver.getLastViewSetup(); ++viewSetup ) {
                if ( resolver.getImageDimensions( timePoint, viewSetup, 0, imageDimensions[ 0 ] ) ) {
                    break time;
                }
            }
        }

        for ( int i = 1; i < imageDimensions.length; ++i ) {
            resolver.getImageDimensions( timePoint, viewSetup, i, imageDimensions[ i ] );
        }

        blockDimensions = new int[ getNumResolutionLevels( viewSetup ) ][ 3 ];
        for ( int i = 0; i < blockDimensions.length; ++i ) {
            resolver.getBlockDimensions( timePoint, viewSetup, i, blockDimensions[ i ] );
        }

        this.sampling = new double[ getNumResolutionLevels( viewSetup ) ][ 3 ];
        this.sampling[ 0 ][ 0 ] = sampling[ 0 ];
        this.sampling[ 0 ][ 1 ] = sampling[ 1 ];
        this.sampling[ 0 ][ 2 ] = sampling[ 2 ];

        mipMapTransforms = new AffineTransform3D[ getNumResolutionLevels( viewSetup ) ];
        for ( int i = 0; i < mipMapTransforms.length; ++i ) {
            mipMapTransforms[ i ] = new AffineTransform3D();
        }
    }

    public int getNumViewSetups()
    {
        return 1 + resolver.getLastViewSetup() - resolver.getFirstViewSetup();
    }

    public int getNumTimePoints()
    {
        return 1 + resolver.getLastTimePoint();
    }

    public int getNumResolutionLevels( final int viewSetup )
    {
        return resolver.getNumResolutionLevels( viewSetup );
    }

    public int getMaxNumResolutionLevels()
    {
        return resolver.getMaxNumResolutionLevels();
    }

    public long[][] getImageDimensions( final int timePoint, final int viewSetup )
    {
        return imageDimensions;
    }

    public int[][] getBlockDimensions( final int timePoint, final int viewSetup )
    {
        return blockDimensions;
    }

    public double[][] getSampling( final int timePoint, final int viewSetup )
    {
        return sampling;
    }

    public double[][] getMipMapResolutions( final int viewSetup )
    {
        return sampling;
    }

    public AffineTransform3D[] getMipMapTransforms( final int viewSetup )
    {
        return mipMapTransforms;
    }

    public KlbPartitionResolver getResolver()
    {
        return resolver;
    }
}
