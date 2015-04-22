package org.janelia.simview.klb.bdv;

import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.XmlIoSpimData;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
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
 * Interfaces a KlbDataset instance with Fiji's SpimData dataset
 * layout description. Can be consumed directly or serialized to XML.
 */
public class KlbSpimDataAdapter implements MultiViewDatasetDefinition
{

    private final KlbDataset data;

    public KlbSpimDataAdapter( final KlbDataset data )
    {
        this.data = data;
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
        final String basePath = "";

        final String[] setups = new String[ data.getNumViewSetups() ];
        for ( int i = 0; i < setups.length; ++i ) {
            setups[ i ] = "" + i;
        }

        final int firstTimePoint = data.getResolver().getFirstTimePoint();
        final int lastTimePoint = data.getResolver().getLastTimePoint();

        final HashMap< Integer, ViewSetup > setupMap = new HashMap< Integer, ViewSetup >();
        for ( int s = 0; s < setups.length; ++s ) {
            setupMap.put( s, new ViewSetup(
                    s, setups[ s ],
                    new FinalDimensions( data.getImageDimensions( 0, 0 )[ 0 ] ),
                    new FinalVoxelDimensions( "um", data.getSampling( 0, 0 )[ 0 ] ),
                    new Channel( s, setups[ s ] ),
                    new Angle( s, setups[ s ] ),
                    new Illumination( s, setups[ s ] ) ) );
        }

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
                new KlbImageLoader( data ),
                missingViews );

        final HashMap< ViewId, ViewRegistration > registrations = new HashMap< ViewId, ViewRegistration >();
        final double[] sampling = data.getSampling( 0, 0 )[ 0 ];
        final double min = Math.min( Math.min( sampling[ 0 ], sampling[ 1 ] ), sampling[ 2 ] );
        for ( int i = 0; i < 3; ++i ) {
            sampling[ i ] /= min;
        }
        for ( final BasicViewSetup setup : seq.getViewSetupsOrdered() ) {
            final int setupId = setup.getId();

            final AffineTransform3D calib = new AffineTransform3D();
            calib.set(
                    sampling[ 0 ], 0, 0, 0,
                    0, sampling[ 1 ], 0, 0,
                    0, 0, sampling[ 2 ], 0
            );
            for ( final TimePoint timepoint : seq.getTimePoints().getTimePointsOrdered() ) {
                final int timepointId = timepoint.getId();
                if ( timepointId >= firstTimePoint ) {
                    registrations.put( new ViewId( timepointId, setupId ), new ViewRegistration( timepointId, setupId, calib ) );
                }
            }
        }

        return new SpimData2( new File( basePath ), seq, new ViewRegistrations( registrations ), null );
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
        // new XmlIoSpimDataMinimal().save( getSpimDataMinimal(), filePath );
        new XmlIoSpimData().save( createDataset(), filePath );
    }

    //    public SpimDataMinimal getSpimDataMinimal()
    //    {
    //        final String basePath = "";
    //
    //        final String[] setups = new String[ data.getNumViewSetups() ];
    //        for ( int i = 0; i < setups.length; ++i ) {
    //            setups[ i ] = "" + i;
    //        }
    //
    //        final int firstTimePoint = data.getResolver().getFirstTimePoint();
    //        final int lastTimePoint = data.getResolver().getLastTimePoint();
    //
    //        final HashMap< Integer, BasicViewSetup > setupMap = new HashMap< Integer, BasicViewSetup >();
    //        for ( int s = 0; s < setups.length; ++s ) {
    //            setupMap.put( s, new BasicViewSetup(
    //                    s, setups[ s ],
    //                    new FinalDimensions( data.getImageDimensions( 0, 0 )[ 0 ] ),
    //                    new FinalVoxelDimensions( "um", data.getSampling( 0, 0 )[ 0 ] ) ) );
    //        }
    //
    //        final HashMap< Integer, TimePoint > timepointMap = new HashMap< Integer, TimePoint >();
    //        for ( int t = 0; t <= lastTimePoint; ++t ) {
    //            timepointMap.put( t, new TimePoint( t ) );
    //        }
    //        final TimePoints timePoints = new TimePoints( timepointMap );
    //
    //        MissingViews missingViews = null;
    //        if ( firstTimePoint > 0 ) {
    //            final ArrayList< ViewId > missing = new ArrayList< ViewId >();
    //            for ( int t = 0; t < firstTimePoint; ++t ) {
    //                for ( final Integer s : setupMap.keySet() ) {
    //                    missing.add( new ViewId( t, s ) );
    //                }
    //            }
    //            missingViews = new MissingViews( missing );
    //        }
    //
    //        final SequenceDescriptionMinimal seq = new SequenceDescriptionMinimal(
    //                timePoints,
    //                setupMap,
    //                new KlbImageLoader( data ),
    //                missingViews );
    //
    //        final HashMap< ViewId, ViewRegistration > registrations = new HashMap< ViewId, ViewRegistration >();
    //        final double[] sampling = data.getSampling( 0, 0 )[ 0 ];
    //        final double min = Math.min( Math.min( sampling[ 0 ], sampling[ 1 ] ), sampling[ 2 ] );
    //        for ( int i = 0; i < 3; ++i ) {
    //            sampling[ i ] /= min;
    //        }
    //        for ( final BasicViewSetup setup : seq.getViewSetupsOrdered() ) {
    //            final int setupId = setup.getId();
    //
    //            final AffineTransform3D calib = new AffineTransform3D();
    //            calib.set(
    //                    sampling[ 0 ], 0, 0, 0,
    //                    0, sampling[ 1 ], 0, 0,
    //                    0, 0, sampling[ 2 ], 0
    //            );
    //            for ( final TimePoint timepoint : seq.getTimePoints().getTimePointsOrdered() ) {
    //                final int timepointId = timepoint.getId();
    //                if ( timepointId >= firstTimePoint ) {
    //                    registrations.put( new ViewId( timepointId, setupId ), new ViewRegistration( timepointId, setupId, calib ) );
    //                }
    //            }
    //        }
    //
    //        return new SpimDataMinimal( new File( basePath ), seq, new ViewRegistrations( registrations ) );
    //    }

}
