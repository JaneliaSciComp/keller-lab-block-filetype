package org.janelia.simview.klb.bdv;

/**
 * Definition of a name tag in a file system path.
 * <p>
 * Example multi-file dataset with 3 channels and 50 time points:
 * /data/chn00_tm000.klb, ..., /data/chn02_tm049.klb
 * NameTag.tag would be "chn" and "tm", respectively.
 */
public class KlbMultiFileNameTag implements Comparable< KlbMultiFileNameTag >
{
    public enum Dimension
    {
        ANGLE, CHANNEL, ILLUMINATION, TIME, RESOLUTION_LEVEL
    }

    public Dimension dimension = null;
    public String tag = "";
    public Integer first = 0;
    public Integer last = 0;
    public Integer stride = 1;

    @Override
    public int compareTo( final KlbMultiFileNameTag o )
    {
        return this.dimension.compareTo( o.dimension );
    }
}
