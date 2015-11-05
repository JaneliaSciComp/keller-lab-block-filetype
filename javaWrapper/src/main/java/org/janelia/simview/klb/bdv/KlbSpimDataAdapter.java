package org.janelia.simview.klb.bdv;

import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.sequence.*;
import net.imglib2.FinalDimensions;
import net.imglib2.realtransform.AffineTransform3D;
import spim.fiji.datasetmanager.MultiViewDatasetDefinition;
import spim.fiji.spimdata.SpimData2;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Interfaces a KlbPartitionResolver instance with Fiji's SpimData
 * dataset layout description. Can be consumed directly or serialized
 * to XML.
 */
public class KlbSpimDataAdapter implements MultiViewDatasetDefinition
{

    private final KlbPartitionResolver resolver;

    public KlbSpimDataAdapter( final KlbPartitionResolver resolver )
    {
        this.resolver = resolver;
    }

    @Override
    public String getTitle()
    {
        return "KLB Dataset";
    }

    @Override
    public String getExtendedDescription()
    {
        return "KLB Dataset";
    }

    @Override
    public SpimData2 createDataset()
    {
        final String[] setups = new String[ resolver.getNumViewSetups() ];
        for ( int i = 0; i < setups.length; ++i ) {
            setups[ i ] = resolver.getViewSetupName( i );
        }

        final HashMap< Integer, ViewSetup > setupMap = new HashMap< Integer, ViewSetup >();
        int timePoint = resolver.getLastTimePoint();
        final long[] dimensions = new long[ 3 ];
        final double[] sampling = new double[ 3 ];
        for ( int s = 0; s < setups.length; ++s ) {
            // Define time point to read image metadata from.
            // Data files can be missing, so probe until one is found.
            // Start with last time point to catch the biggest volume in
            // case the image volume is growing over time.
            // Currently, we are assuming a constant image size, and same
            // sampling and number of resolution levels for all channelIds.
            boolean dimensionsRead = false;
            for (; timePoint >= resolver.getFirstTimePoint(); --timePoint ) {
                dimensionsRead = resolver.getImageDimensions( timePoint, s, 0, dimensions );
                if ( dimensionsRead ) {
                    break;
                }
            }
            // if ( !dimensionsRead ) {
            //     throw new IllegalArgumentException( "Could not determine image dimensions for ViewSetup " + s + "." );
            // }

            resolver.getSampling( timePoint, s, 0, sampling );
            setupMap.put( s, new ViewSetup(
                    s, setups[ s ],
                    new FinalDimensions( dimensions ),
                    new FinalVoxelDimensions( "um", sampling ),
                    new Channel( resolver.getChannelId( s ), resolver.getChannelName( s ) ),
                    new Angle( resolver.getAngleId( s ), resolver.getAngleName( s ) ),
                    new Illumination( resolver.getIlluminationId( s ), resolver.getIlluminationName( s ) ) ) );
        }

        final int firstTimePoint = resolver.getFirstTimePoint();
        final int lastTimePoint = resolver.getLastTimePoint();
        final HashMap< Integer, TimePoint > timepointMap = new HashMap< Integer, TimePoint >();
        for ( int t = 0; t <= lastTimePoint; ++t ) {
            timepointMap.put( t, new TimePoint( t ) );
        }
        final TimePoints timePoints = new TimePoints( timepointMap );

        MissingViews missingViews = null;
        if ( firstTimePoint > 0 ) {
            final ArrayList< ViewId > missing = new ArrayList< ViewId >();
            for ( int t = 0; t < firstTimePoint; ++t ) {
                for ( final Integer s : setupMap.keySet() ) {
                    missing.add( new ViewId( t, s ) );
                }
            }
            missingViews = new MissingViews( missing );
        }

        final SequenceDescription seq = new SequenceDescription(
                timePoints,
                setupMap,
                null,
                missingViews );

        final KlbImageLoader loader = new KlbImageLoader( resolver, seq );
        seq.setImgLoader( loader );

        final HashMap< ViewId, ViewRegistration > registrations = new HashMap< ViewId, ViewRegistration >();
        for ( final ViewSetup setup : seq.getViewSetupsOrdered() ) {
            final int id = setup.getId();
            resolver.getSampling( timePoint, id, 0, sampling );
            final double min = Math.min( Math.min( sampling[ 0 ], sampling[ 1 ] ), sampling[ 2 ] );
            for ( int d = 0; d < sampling.length; ++d ) {
                sampling[ d ] /= min;
            }

            final AffineTransform3D calib = new AffineTransform3D();
            calib.set(
                    sampling[ 0 ], 0, 0, 0,
                    0, sampling[ 1 ], 0, 0,
                    0, 0, sampling[ 2 ], 0
            );
            for ( final TimePoint timepoint : seq.getTimePoints().getTimePointsOrdered() ) {
                final int timepointId = timepoint.getId();
                if ( timepointId >= firstTimePoint ) {
                    registrations.put( new ViewId( timepointId, id ), new ViewRegistration( timepointId, id, calib ) );
                }
            }
        }

        return new SpimData2( new File( System.getProperty( "user.home" ) ), seq, new ViewRegistrations( registrations ), null, null );
    }

    @Override
    public MultiViewDatasetDefinition newInstance()
    {
        return new KlbSpimDataAdapter( null );
    }

    /**
     * Writes the dataset definition to XML.
     * Returns whether or not this succeeded.
     *
     * @param filePath file system path to write to
     * @return whether or not successful
     */
    public void writeXML( final String filePath ) throws SpimDataException
    {
        final SpimData2 data = createDataset();
        data.setBasePath( new File( filePath ).getParentFile() );
        new XmlIoSpimData().save( data, filePath );
    }
}
