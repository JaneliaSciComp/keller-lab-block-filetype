package org.janelia.simview.klb.bdv;

import bdv.ViewerImgLoader;
import bdv.img.cache.Cache;
import bdv.img.cache.VolatileGlobalCellCache;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.sequence.MultiResolutionImgLoader;
import spim.Threads;

import java.util.HashMap;

public class KlbImageLoader implements ViewerImgLoader, MultiResolutionImgLoader
{
    private final VolatileGlobalCellCache cache;
    private final HashMap< Integer, KlbSetupImgLoaderUInt16 > setupImgLoaders = new HashMap< Integer, KlbSetupImgLoaderUInt16 >();
    private final KlbPartitionResolver resolver;

    public KlbImageLoader( final KlbPartitionResolver resolver, final AbstractSequenceDescription seq )
    {
        cache = new VolatileGlobalCellCache(
                seq.getTimePoints().size(),
                resolver.getNumViewSetups(),
                resolver.getMaxNumResolutionLevels(),
                Threads.numThreads()
        );
        this.resolver = resolver;
        for ( int i = 0; i < resolver.getNumViewSetups(); ++i ) {
            setupImgLoaders.put( i, new KlbSetupImgLoaderUInt16( seq, this.resolver, i, cache ) );
        }
    }

    public KlbPartitionResolver getResolver()
    {
        return resolver;
    }

    @Override
    public KlbSetupImgLoaderUInt16 getSetupImgLoader( final int viewSetupId )
    {
        return setupImgLoaders.get( viewSetupId );
    }

    @Override
    public Cache getCache()
    {
        return cache;
    }
}
