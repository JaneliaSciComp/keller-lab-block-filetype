package org.janelia.simview.klb.bdv;

import org.janelia.simview.klb.jni.KlbImageHeader;
import org.janelia.simview.klb.jni.KlbImageIO;
import org.janelia.simview.klb.jni.KlbRoi;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * KlbPartitionResolver using a user-defined path name tag pattern.
 */
public class KlbPartitionResolverDefault implements KlbPartitionResolver
{
    protected final String[] viewSetupTemplates;
    protected final int[] angleIds, channelIds, illuminationIds;
    protected final String[] angleNames, channelNames, illuminationNames;
    protected String timeTag, timeMatch, timeFormat;
    protected String resLvlTag, resLvlMatch, resLvlFormat;
    private double[][] sampling = null;
    private int firstTimePoint = 0, lastTimePoint = 0;
    private int numResolutionLevels = 1;

    /**
     * Constructs a KlbPartitionResolver from a file system path following
     * a name tag pattern.
     *
     * @param template absolute file system path to a data file (e.g. '/folder/Data1Time000Chn00.klb')
     * @param nameTags list of KlbMultiFileNameTag instances
     */
    public KlbPartitionResolverDefault( final String template, final List< KlbMultiFileNameTag > nameTags )
    {
        // NameTags that are found in template file path
        final List< KlbMultiFileNameTag > foundTags = new ArrayList< KlbMultiFileNameTag >();

        // Format of KlbMultiFileNameTag in template file path, in same order as 'foundTags', eg. 'TM%06d'
        final List< String > formats = new ArrayList< String >();

        // Depth
        final List< Integer > depths = new ArrayList< Integer >();

        for ( final KlbMultiFileNameTag tag : nameTags ) {
            if ( tag.tag.trim().isEmpty() ) {
                continue;
            }
            final Pattern pattern = Pattern.compile( String.format( "%s\\d+", tag.tag ) );
            final Matcher matcher = pattern.matcher( template );
            if ( matcher.find() ) {
                final String match = template.substring( matcher.start(), matcher.end() );
                final String format = String.format( "%s%s%dd", tag.tag, "%0", match.length() - tag.tag.length() );
                final int depth = 1 + (tag.last - tag.first) / tag.stride;
                if ( tag.dimension == KlbMultiFileNameTag.Dimension.TIME ) {
                    timeTag = tag.tag;
                    timeMatch = match;
                    timeFormat = format;
                    firstTimePoint = tag.first;
                    lastTimePoint = tag.last;
                } else if ( tag.dimension == KlbMultiFileNameTag.Dimension.RESOLUTION_LEVEL ) {
                    resLvlTag = tag.tag;
                    resLvlMatch = match;
                    resLvlFormat = format;
                    numResolutionLevels = tag.last;
                } else {
                    if ( depth > 1 ) {
                        foundTags.add( tag );
                        formats.add( format );
                        depths.add( depth );
                    }
                }
            }
        }

        int numViewSetups = 1;
        for ( final int depth : depths ) {
            numViewSetups *= depth;
        }

        viewSetupTemplates = new String[ numViewSetups ];
        angleIds = new int[ numViewSetups ];
        channelIds = new int[ numViewSetups ];
        illuminationIds = new int[ numViewSetups ];
        angleNames = new String[ numViewSetups ];
        channelNames = new String[ numViewSetups ];
        illuminationNames = new String[ numViewSetups ];

        for ( int setup = 0; setup < numViewSetups; ++setup ) {
            String fn = template;
            int angleId = 0, channelId = 0, illuminationId = 0;
            String angleName = "0", channelName = "0", illuminationName = "0";
            for ( int d = 0; d < foundTags.size(); ++d ) {
                final KlbMultiFileNameTag tag = foundTags.get( d );
                final int depth = depths.get( d );

                int depthHigherDims = 1;
                for ( int i = d + 1; i < foundTags.size(); ++i ) {
                    depthHigherDims *= depths.get( i );
                }

                int id = setup / depthHigherDims;
                while ( id >= depth ) {
                    id -= depth;
                }
                final int name = id * tag.stride;

                switch ( tag.dimension ) {
                    case ANGLE:
                        angleId = id;
                        angleName = "" + name;
                        break;
                    case CHANNEL:
                        channelId = id;
                        channelName = "" + name;
                        break;
                    case ILLUMINATION:
                        illuminationId = id;
                        illuminationName = "" + name;
                        break;
                }

                fn = fn.replaceAll( String.format( "%s\\d+", tag.tag ), String.format( formats.get( d ), name ) );
            }
            viewSetupTemplates[ setup ] = fn;
            angleIds[ setup ] = angleId;
            channelIds[ setup ] = channelId;
            illuminationIds[ setup ] = illuminationId;
            angleNames[ setup ] = angleName;
            channelNames[ setup ] = channelName;
            illuminationNames[ setup ] = illuminationName;
        }
    }

    public KlbPartitionResolverDefault( final String[] viewSetupTemplates, final String timeTag, final int firstTimePoint, final int lastTimePoint, final String resolutionLevelTag, final int numResolutionLevels )
    {
        this.viewSetupTemplates = viewSetupTemplates;
        this.timeTag = timeTag;
        this.firstTimePoint = firstTimePoint;
        this.lastTimePoint = lastTimePoint;
        this.resLvlTag = resolutionLevelTag;
        this.numResolutionLevels = numResolutionLevels;
        angleIds = channelIds = illuminationIds = null;
        angleNames = channelNames = illuminationNames = null;

        final Pattern pattern = Pattern.compile( String.format( "%s\\d+", this.timeTag ) );
        for ( final String template : viewSetupTemplates ) {
            final Matcher matcher = pattern.matcher( template );
            if ( matcher.find() ) {
                timeMatch = template.substring( matcher.start(), matcher.end() );
                timeFormat = String.format( "%s%s%dd", this.timeTag, "%0", timeMatch.length() - this.timeTag.length() );
                break;
            }
        }
    }

    public void specifySampling( final double[][] sampling )
    {
        this.sampling = sampling;
    }

    @Override
    public int getNumViewSetups()
    {
        return viewSetupTemplates.length;
    }

    @Override
    public String getViewSetupName( final int viewSetup )
    {
        return new File(viewSetupTemplates[ viewSetup ]).getName().replace( ".klb", "" );
    }

    @Override
    public int getAngleId( final int viewSetup )
    {
        return angleIds[ viewSetup ];
    }

    @Override
    public int getChannelId( final int viewSetup )
    {
        return channelIds[ viewSetup ];
    }

    @Override
    public int getIlluminationId( final int viewSetup )
    {
        return illuminationIds[ viewSetup ];
    }

    @Override
    public String getAngleName( final int viewSetup )
    {
        return angleNames[ viewSetup ];
    }

    @Override
    public String getChannelName( final int viewSetup )
    {
        return channelNames[ viewSetup ];
    }

    @Override
    public String getIlluminationName( final int viewSetup )
    {
        return illuminationNames[ viewSetup ];
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
        final String filePath = getFilePath( timePoint, viewSetup, level );
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
        final String filePath = getFilePath( timePoint, viewSetup, level );
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
        if ( level == 0 && sampling != null ) {
            out[ 0 ] = sampling[ level ][ 0 ];
            out[ 1 ] = sampling[ level ][ 1 ];
            out[ 2 ] = sampling[ level ][ 2 ];
            return true;
        }
        final String filePath = getFilePath( timePoint, viewSetup, level );
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
    public void set( final int timePoint, final int viewSetup, final int level, final int[] dimensions, final long[] min, final KlbImageIO io, final KlbRoi roi )
    {
        io.setFilename( getFilePath( timePoint, viewSetup, level ) );
        roi.setXyzctUB( new long[]{
                min[ 0 ] + dimensions[ 0 ] - 1,
                min[ 1 ] + dimensions[ 1 ] - 1,
                min[ 2 ] + dimensions[ 2 ] - 1,
                0, 0 } );
        roi.setXyzctLB( new long[]{ min[ 0 ], min[ 1 ], min[ 2 ], 0, 0 } );
    }

    protected String getFilePath( final int timePoint, final int viewSetup, final int level )
    {
        String fn = viewSetupTemplates[ viewSetup ];
        if ( timeMatch != null ) {
            fn = fn.replaceAll( timeMatch, String.format( timeFormat, timePoint ) );
        }

        if (level == 0 && resLvlMatch != null) {
            fn = fn.replace( "." + resLvlMatch, "" );
        } else if (level > 0) {
            if (resLvlMatch == null) {
                fn = fn.substring( 0, fn.lastIndexOf( ".klb" ) ) + String.format( ".RESLVL%d.klb", level );
            } else {
                fn = fn.replaceAll( resLvlMatch, String.format( resLvlFormat, level ) );
            }
        }
        return fn;
    }
}
