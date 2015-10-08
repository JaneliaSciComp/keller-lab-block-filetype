package org.janelia.simview.klb.bdv;

import bdv.img.cache.CacheArrayLoader;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;
import org.janelia.simview.klb.jni.KlbImageIO;
import org.janelia.simview.klb.jni.KlbRoi;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

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
