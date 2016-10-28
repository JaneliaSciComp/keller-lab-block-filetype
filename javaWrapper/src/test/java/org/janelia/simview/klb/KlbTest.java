package org.janelia.simview.klb;

import net.imagej.ImgPlus;
import net.imglib2.Cursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.cell.CellImg;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

public class KlbTest
{
    private final KLB klb = KLB.newInstance();
    private final String testReadFilePath = new File( "../testData/img.klb" ).getAbsolutePath();
    private final String testCellImgReadFilePath = new File( "src/test/resources/cellimg.klb" ).getAbsolutePath();
    private final String testWriteFilePath = new File( "src/test/resources/deleteme.klb" ).getAbsolutePath();

    @Test
    public void readHeader()
    {
        KLB.Header header = null;
        try {
            header = klb.readHeader( testReadFilePath );
        } catch ( IOException e ) {
            e.printStackTrace();
        }

        assertNotNull( header );
        assertEquals( UnsignedShortType.class, header.dataType.getClass() );
        assertArrayEquals( new long[]{ 101, 151, 29, 1, 1 }, header.imageSize );
    }

    @Test
    public void readArrayImgFull()
    {
        ImgPlus img = null;
        try {
            img = klb.readFull( testReadFilePath );
        } catch ( IOException e ) {
            e.printStackTrace();
        }

        assertNotNull( img );
        assertTrue( img.getImg() instanceof ArrayImg );
        assertEquals( 3, img.numDimensions() );

        final long[] dims = new long[ 3 ];
        img.dimensions( dims );
        assertArrayEquals( new long[]{ 101, 151, 29 }, dims );

        double mean = 0;
        long n = 1;
        for ( final long d : dims ) {
            n *= d;
        }
        final Cursor< ? extends RealType< ? > > cur = img.cursor();
        while ( cur.hasNext() ) {
            mean += cur.next().getRealDouble() / n;
        }
        assertEquals( 352, Math.round( mean ) );
    }

    @Test
    public void readCellImgFull()
    {
        ImgPlus img = null;
        try {
            img = klb.readFull( testCellImgReadFilePath );
        } catch ( IOException e ) {
            e.printStackTrace();
        }

        assertNotNull( img );
        assertTrue( img.getImg() instanceof CellImg );
        assertEquals( 3, img.numDimensions() );

        final long[] dims = new long[ 3 ];
        img.dimensions( dims );
        assertArrayEquals( new long[]{ 2048, 2048, 520 }, dims );

        double mean = 0;
        long n = 1;
        for ( final long d : dims ) {
            n *= d;
        }
        final Cursor< ? extends RealType< ? > > cur = img.cursor();
        while ( cur.hasNext() ) {
            mean += cur.next().getRealDouble() / n;
        }
        assertEquals( 1025, Math.round( mean ) );
    }

    @Test
    public void readArrayImgROI()
    {
        final long[] min = { 15, 15, 9, 0, 0 }, max = { 99, 99, 11, 0, 0 };

        ImgPlus img = null;
        try {
            img = klb.readROI( testReadFilePath, min, max );
        } catch ( IOException e ) {
            e.printStackTrace();
        }

        assertNotNull( img );
        assertTrue( img.getImg() instanceof ArrayImg );
        assertEquals( 3, img.numDimensions() );

        final long[] dims = new long[ 3 ];
        img.dimensions( dims );
        assertArrayEquals( new long[]{ 1 + max[ 0 ] - min[ 0 ], 1 + max[ 1 ] - min[ 1 ], 1 + max[ 2 ] - min[ 2 ] }, dims );

        double mean = 0;
        long n = 1;
        for ( final long d : dims ) {
            n *= d;
        }
        final Cursor< ? extends RealType< ? > > cur = img.cursor();
        while ( cur.hasNext() ) {
            mean += cur.next().getRealDouble() / n;
        }
        assertEquals( 568, Math.round( mean ) );
    }

    @Test
    public void readCellImgROI()
    {
        final long[] min = { 0, 6, 0, 0, 0 }, max = { 2047, 2047, 519, 0, 0 };

        ImgPlus img = null;
        try {
            img = klb.readROI( testCellImgReadFilePath, min, max );
        } catch ( IOException e ) {
            e.printStackTrace();
        }

        assertNotNull( img );
        assertTrue( img.getImg() instanceof CellImg );
        assertEquals( 3, img.numDimensions() );

        final long[] dims = new long[ 3 ];
        img.dimensions( dims );
        assertArrayEquals( new long[]{ 1 + max[ 0 ] - min[ 0 ], 1 + max[ 1 ] - min[ 1 ], 1 + max[ 2 ] - min[ 2 ] }, dims );

        double mean = 0;
        long n = 1;
        for ( final long d : dims ) {
            n *= d;
        }
        final Cursor< ? extends RealType< ? > > cur = img.cursor();
        while ( cur.hasNext() ) {
            mean += cur.next().getRealDouble() / n;
        }
        assertEquals( 1027, Math.round( mean ) );

        assertEquals( 7, Math.round( (( RealType ) img.firstElement()).getRealDouble() ) );
    }

    @Test
    public void write()
    {
        if ( new File( testWriteFilePath ).exists() ) {
            new File( testWriteFilePath ).delete();
        }

        ImgPlus img = null;
        try {
            img = klb.readFull( testReadFilePath );
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        assertNotNull( img );
        assertTrue( img.getImg() instanceof ArrayImg );
        assertEquals( 3, img.numDimensions() );

        try {
            klb.writeFull( img, testWriteFilePath, null, KLB.CompressionType.BZIP2, null );
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        assertTrue( new File( testWriteFilePath ).exists() );

        ImgPlus img2 = null;
        try {
            img2 = klb.readFull( testWriteFilePath );
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        assertNotNull( img2 );
        assertTrue( img2.getImg() instanceof ArrayImg );
        assertEquals( 3, img2.numDimensions() );

        final long[] dims = new long[ 3 ], dims2 = new long[ 3 ];
        img.dimensions( dims );
        img2.dimensions( dims2 );
        assertArrayEquals( dims, dims2 );

        double mean = 0, mean2 = 0;
        long n = 1;
        for ( final long d : dims ) {
            n *= d;
        }
        final Cursor< ? extends RealType< ? > > cur = img.cursor(), cur2 = img2.cursor();
        while ( cur.hasNext() ) {
            mean += cur.next().getRealDouble() / n;
            mean2 += cur2.next().getRealDouble() / n;
        }
        assertEquals( Math.round( mean ), Math.round( mean2 ) );

        if ( new File( testWriteFilePath ).exists() ) {
            new File( testWriteFilePath ).delete();
        }
    }
}
