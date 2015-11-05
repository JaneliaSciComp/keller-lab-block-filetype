package org.janelia.simview.klb.bdv;

import bdv.export.Downsample;
import bdv.export.ExportMipmapInfo;
import bdv.export.ProposeMipmaps;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.XmlIoSpimDataMinimal;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicImgLoader;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.generic.sequence.ImgLoaderHints;
import mpicbg.spim.data.sequence.TimePoint;
import net.imglib2.Cursor;
import net.imglib2.Dimensions;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.janelia.simview.klb.jni.KlbDataType;
import org.janelia.simview.klb.jni.KlbImageHeader;
import org.janelia.simview.klb.jni.KlbImageIO;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.log.StderrLogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Plugin( type = Command.class, menuPath = "Plugins>BigDataViewer>Generate KLB Dataset resolution levels" )
public class KlbDownsampler< T extends RealType< T > & NativeType< T > > implements Command
{
    private final KlbImageIO io = new KlbImageIO();
    private final KlbImageHeader header = new KlbImageHeader();

    @Parameter
    private File xmlFile;

    @Parameter
    private boolean skipFirst = false;

    @Parameter
    private LogService log;

    @Override
    public void run()
    {
        process( xmlFile );
    }

    public void process( final File xmlFile )
    {
        process( xmlFile, skipFirst );
    }

    public void process( final File xmlFile, final boolean skipFirst )
    {
        this.skipFirst = skipFirst;
        final String filePath = xmlFile.getAbsolutePath();

        SpimDataMinimal data = null;
        try {
            data = new XmlIoSpimDataMinimal().load( filePath );
        } catch ( SpimDataException e ) {
            e.printStackTrace();
        }

        if ( data != null ) {
            process( data.getSequenceDescription() );
        }
    }

    private void process( final AbstractSequenceDescription< ?, ?, ? > seq )
    {
        if ( log == null ) {
            log = new StderrLogService();
        }
        final int numThreads = Runtime.getRuntime().availableProcessors();

        final BasicImgLoader loader = seq.getImgLoader();
        final KlbPartitionResolverDefault resolver = ( KlbPartitionResolverDefault ) (( KlbImageLoader ) loader).getResolver();


        // calculate dimensions, sampling, and relative downsampling factors for each level
        final Map< Integer, ExportMipmapInfo > proposedDownsampling = ProposeMipmaps.proposeMipmaps( seq );
        final Map< Integer, long[][] > dimensions = new HashMap< Integer, long[][] >();
        final Map< Integer, int[][] > relativeScaling = new HashMap< Integer, int[][] >();
        final Map< Integer, double[][] > sampling = new HashMap< Integer, double[][] >();

        final List< ? extends BasicViewSetup > viewSetups = seq.getViewSetupsOrdered();
        for ( final BasicViewSetup viewSetup : viewSetups ) {
            final int viewSetupId = viewSetup.getId();
            final Dimensions dimsObj = seq.getViewSetupsOrdered().get( viewSetupId ).getSize();
            final int numDims = dimsObj.numDimensions();

            final int[][] scales = proposedDownsampling.get( viewSetupId ).getExportResolutions();
            final long[][] dims = new long[ scales.length ][ numDims ];
            final double[][] smpl = new double[ scales.length ][ numDims ];
            dimsObj.dimensions( dims[ 0 ] );
            seq.getViewSetupsOrdered().get( viewSetupId ).getVoxelSize().dimensions( smpl[ 0 ] );

            for ( int level = 1; level < scales.length; ++level ) {  // scaling[0] is full sampling
                for ( int dim = 0; dim < numDims; ++dim ) {
                    scales[ level ][ dim ] /= scales[ level - 1 ][ dim ];
                    dims[ level ][ dim ] = dims[ level - 1 ][ dim ] / scales[ level ][ dim ];
                    smpl[ level ][ dim ] = smpl[ level - 1 ][ dim ] * scales[ level ][ dim ];
                }
            }

            relativeScaling.put( viewSetupId, scales );
            dimensions.put( viewSetupId, dims );
            sampling.put( viewSetupId, smpl );
        }


        // correct results when skipping first downsampled level
        if ( skipFirst ) {
            for ( final BasicViewSetup viewSetup : viewSetups ) {
                final int viewSetupId = viewSetup.getId();
                long[][] oldDims = dimensions.get( viewSetupId );
                double[][] oldSmpl = sampling.get( viewSetupId );
                int[][] oldScales = relativeScaling.get( viewSetupId );

                final int numOldLevels = oldDims.length;
                final int numNewLevels = numOldLevels - 1;
                final int numDims = oldDims[ 0 ].length;

                long[][] newDims = new long[ numNewLevels ][ numDims ];
                double[][] newSmpl = new double[ numNewLevels ][ numDims ];
                int[][] newScales = new int[ numNewLevels ][ numDims ];

                System.arraycopy( oldDims[ 0 ], 0, newDims[ 0 ], 0, numDims );
                System.arraycopy( oldSmpl[ 0 ], 0, newSmpl[ 0 ], 0, numDims );
                System.arraycopy( oldScales[ 0 ], 0, newScales[ 0 ], 0, numDims );

                for ( int newLevel = 1; newLevel < numNewLevels; ++newLevel ) {
                    final int oldLevel = newLevel + 1;
                    System.arraycopy( oldDims[ oldLevel ], 0, newDims[ newLevel ], 0, numDims );
                    System.arraycopy( oldSmpl[ oldLevel ], 0, newSmpl[ newLevel ], 0, numDims );
                    if ( newLevel == 1 ) {
                        for ( int d = 0; d < numDims; ++d ) {
                            newScales[ newLevel ][ d ] = oldScales[ oldLevel ][ d ] * oldScales[ newLevel ][ d ];
                        }
                    } else {
                        System.arraycopy( oldScales[ oldLevel ], 0, newScales[ newLevel ], 0, numDims );
                    }
                }

                relativeScaling.put( viewSetupId, newScales );
                dimensions.put( viewSetupId, newDims );
                sampling.put( viewSetupId, newSmpl );
            }
        }


        // show results
        log.info( "Downsampling factors and image dimensions" );
        for ( final BasicViewSetup viewSetup : viewSetups ) {
            final int viewSetupId = viewSetup.getId();
            log.info( String.format( "ViewSetupId %d", viewSetupId ) );
            final long[][] dims = dimensions.get( viewSetupId );
            final double[][] smpl = sampling.get( viewSetupId );
            final int[][] scales = relativeScaling.get( viewSetupId );
            for ( int level = 0; level < dims.length; ++level ) {
                log.info( String.format( "  Level %d image dimensions      %s", level, Arrays.toString( dims[ level ] ) ) );
                log.info( String.format( "          sampling              %s", Arrays.toString( smpl[ level ] ) ) );
                log.info( String.format( "          relative downsampling %s", Arrays.toString( scales[ level ] ) ) );
            }
        }


        // downsample images
        log.info( "Starting downsampling" );
        final long[] klbDims = { 0, 0, 0, 1, 1 };
        final float[] klbSampling = { 1, 1, 1, 1, 1 };
        for ( final TimePoint tp : seq.getTimePoints().getTimePointsOrdered() ) {
            final int t = tp.getId();
            log.info( String.format( "Time point %d", t ) );
            for ( final BasicViewSetup viewSetup : viewSetups ) {
                final int viewSetupId = viewSetup.getId();
                final int[][] scales = relativeScaling.get( viewSetupId );
                final long[][] dims = dimensions.get( viewSetupId );
                final double[][] smpl = sampling.get( viewSetupId );

                final T jType = ( T ) loader.getSetupImgLoader( viewSetupId ).getImageType();
                KlbDataType cType = null;
                switch ( jType.getBitsPerPixel() ) {
                    case 8:
                        cType = KlbDataType.UINT8_TYPE;
                        break;
                    case 16:
                        cType = KlbDataType.UINT16_TYPE;
                        break;
                }
                final ImgFactory< T > imageFactory = new ArrayImgFactory< T >();
                RandomAccessibleInterval currentImage = loader.getSetupImgLoader( viewSetupId ).getImage( t, ImgLoaderHints.LOAD_COMPLETELY );
                final long[] currentDims = new long[ currentImage.numDimensions() ];
                currentImage.dimensions( currentDims );

                log.info( String.format( "  ViewSetupId %d", viewSetupId ) );
                log.debug( String.format( "  Level %d", 0 ) );
                log.debug( String.format( "     image dimensions      %s", Arrays.toString( currentDims ) ) );
                log.debug( String.format( "     sampling              %s", Arrays.toString( smpl[ 0 ] ) ) );

                for ( int level = 1; level < scales.length; ++level ) {
                    final Img< T > downsampledImage = imageFactory.create( dims[ level ], jType );
                    downsampledImage.dimensions( currentDims );

                    log.info( String.format( "    Level %d", level ) );
                    log.debug( String.format( "     image dimensions      %s", Arrays.toString( currentDims ) ) );
                    log.debug( String.format( "     sampling              %s", Arrays.toString( smpl[ level ] ) ) );
                    log.debug( String.format( "     relative downsampling %s", Arrays.toString( scales[ level ] ) ) );

                    Downsample.downsample( currentImage, downsampledImage, scales[ level ] );

                    final ByteBuffer buffer = convertToBytes( downsampledImage );
                    final String filePath = resolver.getFilePath( t, viewSetupId, level );
                    log.debug( filePath );
                    io.setFilename( filePath );

                    klbDims[ 0 ] = currentDims[ 0 ];
                    klbDims[ 1 ] = currentDims[ 1 ];
                    klbDims[ 2 ] = currentDims[ 2 ];
                    klbSampling[ 0 ] = ( float ) smpl[ level ][ 0 ];
                    klbSampling[ 1 ] = ( float ) smpl[ level ][ 1 ];
                    klbSampling[ 2 ] = ( float ) smpl[ level ][ 2 ];
                    header.setHeader( klbDims, cType, klbSampling );
                    io.setHeader( header );
                    io.writeImage( buffer.array(), numThreads );

                    currentImage = downsampledImage;
                }
            }
        }
        log.info( "Done." );
    }

    private ByteBuffer convertToBytes( final IterableInterval< T > input )
    {
        final int bpp = input.firstElement().getBitsPerPixel();
        final long numBytes = bpp / 8 * input.size();
        if ( numBytes > Integer.MAX_VALUE - 8 ) {
            throw new IllegalArgumentException( String.format( "Downsampled image must not be larger than 2GB (uncompressed), but is %d bytes large", numBytes ) );
        }

        final ByteBuffer bytes = ByteBuffer.allocate( ( int ) numBytes );
        bytes.order( ByteOrder.LITTLE_ENDIAN );
        final Cursor< T > cur = input.cursor();

        switch ( bpp ) {
            case 8:
                while ( cur.hasNext() )
                    bytes.put( ( byte ) cur.next().getRealDouble() );
                return bytes;
            case 16:
                final ShortBuffer shorts = bytes.asShortBuffer();
                while ( cur.hasNext() )
                    shorts.put( ( short ) cur.next().getRealDouble() );
                return bytes;
            default:
                throw new IllegalArgumentException( "Unknown or unsupported data type." );
        }
    }

    public static void main( final String[] args )
    {
        final String filePath = args[ 0 ];

        boolean skipFirst = false;
        for ( int i = 1; i < args.length; ++i ) {
            if ( "skipfirst".equals( args[ i ].toLowerCase() ) ) {
                skipFirst = true;
            }
        }

        new KlbDownsampler().process( new File( filePath ), skipFirst );
    }

}