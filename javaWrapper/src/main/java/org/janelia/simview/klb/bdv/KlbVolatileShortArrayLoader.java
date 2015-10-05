package org.janelia.simview.klb.bdv;

import bdv.img.cache.CacheArrayLoader;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;
import org.janelia.simview.klb.jni.KlbImageIO;
import org.janelia.simview.klb.jni.KlbRoi;

public class KlbVolatileShortArrayLoader implements CacheArrayLoader< VolatileShortArray >
{

    final private KlbPartitionResolver resolver;
    private VolatileShortArray theEmptyArray;

    public KlbVolatileShortArrayLoader( final KlbPartitionResolver resolver )
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

        final short[] data = new short[ dimensions[ 0 ] * dimensions[ 1 ] * dimensions[ 2 ] ];

        resolver.set( timePoint, viewSetup, level, dimensions, offset, io, roi );
        final byte[] bytes = new byte[ 2 * dimensions[ 0 ] * dimensions[ 1 ] * dimensions[ 2 ] ];
        if ( io.readImage( bytes, roi, 1 ) == 0 ) {
            for ( int i = 0, j = -1; i < data.length; ++i ) {
                data[ i ] = ( short ) ((bytes[ ++j ] & 0xff) | ((bytes[ ++j ] & 0xff) << 8));
            }
        }

        return new VolatileShortArray( data, true );
    }

    @Override
    public VolatileShortArray emptyArray( final int[] dimensions )
    {
        int numEntities = 1;
        for ( int i = 0; i < dimensions.length; ++i )
            numEntities *= dimensions[ i ];
        if ( theEmptyArray.getCurrentStorageArray().length < numEntities )
            theEmptyArray = new VolatileShortArray( numEntities, false );
        return theEmptyArray;
    }
}
