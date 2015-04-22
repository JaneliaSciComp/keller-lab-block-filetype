package org.janelia.simview.klb.bdv;

import org.janelia.simview.klb.jni.KlbImageHeader;
import org.janelia.simview.klb.jni.KlbImageIO;
import org.janelia.simview.klb.jni.KlbRoi;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * KlbPartitionResolver using a user-defined path name tag pattern.
 * <p/>
 * Example:
 * Data1Time000Chn00.klb, Data1Time001Chn00.klb, ... , Data1Time099Chn00.klb
 * With all files in folder '/folder/' and no additional resolution levels.
 * Instantiate as KlbPartitionResolver('/folder/Data1Time000Chn00.klb', 'Chn', 'Time', 0, 0, 0, 99, 1).
 */
public class KlbPartitionResolverNamePatternSimple implements KlbPartitionResolver
{

    protected final String template;
    protected final String setupTag;
    protected final String timeTag;
    protected final int firstSetup;
    protected final int lastSetup;
    protected final int firstTimePoint;
    protected final int lastTimePoint;
    protected final int numResolutionLevels;

    private final String cPlaceholder;
    private final String cFormat;
    private final String tPlaceholder;
    private final String tFormat;

    /**
     * Constructs a KlbPartitionResolver from a file system path following
     * a name tag pattern.
     *
     * @param template            absolute file system path to a data file (e.g. '/folder/Data1Time000Chn00.klb')
     * @param channelTag          part of the file path indicating the channel (e.g. 'Chn')
     * @param timeTag             part of the file path indicating the time point (e.g. 'Time')
     * @param firstChannel        index of first channel (e.g. 0)
     * @param lastChannel         index of last channel (e.g. 0)
     * @param firstTimePoint      index of first time point (e.g. 0)
     * @param lastTimePoint       index of last time point (e.g. 57)
     * @param numResolutionLevels number of resolution levels (e.g. 1 if only full resolution, original images are available)
     */
    public KlbPartitionResolverNamePatternSimple( final String template, final String channelTag, final String timeTag, final int firstChannel, final int lastChannel, final int firstTimePoint, final int lastTimePoint, final int numResolutionLevels )
    {
        this.template = template;
        this.setupTag = channelTag;
        this.timeTag = timeTag;
        this.firstSetup = firstChannel;
        this.lastSetup = lastChannel;
        this.firstTimePoint = firstTimePoint;
        this.lastTimePoint = lastTimePoint;
        this.numResolutionLevels = numResolutionLevels < 1 ? 1 : numResolutionLevels;

        Pattern pattern = Pattern.compile( String.format( "%s\\d+", channelTag ) );
        Matcher matcher = pattern.matcher( template );
        if ( matcher.find() ) {
            cPlaceholder = template.substring( matcher.start(), matcher.end() );
            cFormat = String.format( "%s%s%dd", channelTag, "%0", cPlaceholder.length() - channelTag.length() );
        } else {
            cPlaceholder = cFormat = null;
        }

        pattern = Pattern.compile( String.format( "%s\\d+", timeTag ) );
        matcher = pattern.matcher( template );
        if ( matcher.find() ) {
            tPlaceholder = template.substring( matcher.start(), matcher.end() );
            tFormat = String.format( "%s%s%dd", timeTag, "%0", tPlaceholder.length() - timeTag.length() );
        } else {
            tPlaceholder = tFormat = null;
        }
    }

    @Override
    public int getFirstViewSetup()
    {
        return firstSetup;
    }

    @Override
    public int getLastViewSetup()
    {
        return lastSetup;
    }

    @Override
    public int getFirstTimePoint()
    {
        return firstTimePoint;
    }

    @Override
    public int getLastTimePoint()
    {
        return lastTimePoint;
    }

    @Override
    public int getNumResolutionLevels( final int viewSetup )
    {
        return getMaxNumResolutionLevels();
    }

    @Override
    public int getMaxNumResolutionLevels()
    {
        return numResolutionLevels;
    }

    @Override
    public boolean getImageDimensions( final int timePoint, final int viewSetup, final int level, final long[] out )
    {
        final String filePath = getFilePath( viewSetup, timePoint );
        final KlbImageHeader header = new KlbImageHeader();
        if ( header.readHeader( filePath ) == 0 ) {
            final long[] dims = header.getXyzct();
            out[ 0 ] = dims[ 0 ];
            out[ 1 ] = dims[ 1 ];
            out[ 2 ] = dims[ 2 ];
            return true;
        }
        return false;
    }

    @Override
    public boolean getBlockDimensions( final int timePoint, final int viewSetup, final int level, final int[] out )
    {
        final String filePath = getFilePath( viewSetup, timePoint );
        final KlbImageHeader header = new KlbImageHeader();
        if ( header.readHeader( filePath ) == 0 ) {
            final long[] dims = header.getBlockSize();
            out[ 0 ] = ( int ) dims[ 0 ];
            out[ 1 ] = ( int ) dims[ 1 ];
            out[ 2 ] = ( int ) dims[ 2 ];
            return true;
        }
        return false;
    }

    @Override
    public boolean getSampling( final int timePoint, final int viewSetup, final int level, final double[] out )
    {
        final String filePath = getFilePath( viewSetup, timePoint );
        final KlbImageHeader header = new KlbImageHeader();
        if ( header.readHeader( filePath ) == 0 ) {
            final float[] smpl = header.getPixelSize();
            out[ 0 ] = smpl[ 0 ];
            out[ 1 ] = smpl[ 1 ];
            out[ 2 ] = smpl[ 2 ];
            return true;
        }
        return false;
    }

    @Override
    public String getRepresentativeFilePath()
    {
        return template;
    }

    @Override
    public void set( final int timePoint, final int viewSetup, final int level, final int[] dimensions, final long[] min, final KlbImageIO io, final KlbRoi roi )
    {
        io.setFilename( getFilePath( viewSetup, timePoint ) );
        roi.setXyzctUB( new long[]{
                min[ 0 ] + dimensions[ 0 ] - 1,
                min[ 1 ] + dimensions[ 1 ] - 1,
                min[ 2 ] + dimensions[ 2 ] - 1,
                0, 0 } );
        roi.setXyzctLB( new long[]{ min[ 0 ], min[ 1 ], min[ 2 ], 0, 0 } );
    }

    private String getFilePath( final int c, final int t )
    {
        String fn = template;

        if ( cPlaceholder != null ) {
            fn = fn.replace( cPlaceholder, String.format( cFormat, c ) );
        }

        if ( tPlaceholder != null ) {
            fn = fn.replace( tPlaceholder, String.format( tFormat, t ) );
        }

        return fn;
    }
}
