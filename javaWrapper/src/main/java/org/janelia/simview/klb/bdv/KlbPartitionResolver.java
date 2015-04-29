package org.janelia.simview.klb.bdv;

import org.janelia.simview.klb.jni.KlbImageIO;
import org.janelia.simview.klb.jni.KlbRoi;

/**
 * Defines a dataset consisting of one or multiple KLB files.
 * <p/>
 * Interfaces with Fiji's Big Data Viewer and SPIM plugins through
 * KlbSpimDataAdapter, which generates a SpimData2 instance that can
 * be consumed by Fiji and/or saved to XML.
 * <p/>
 * Returns metadata that is apparent from the file system, such as the
 * number of ViewSetups, time points and scales.
 * <p/>
 * Retrieves basic image-related metadata (image dimensions, block
 * dimensions, sampling).
 * <p/>
 * Configures provided KlbImageIO and KlbRoi instances to read a block
 * defined by time point, ViewSetup, level, ROI start and
 * ROI dimensions.
 */
public interface KlbPartitionResolver
{

    int getNumViewSetups();

    String getViewSetupName( final int viewSetup );

    int getAngleId( final int viewSetup );

    int getChannelId( final int viewSetup );

    int getIlluminationId( final int viewSetup );

    String getAngleName( final int viewSetup );

    String getChannelName( final int viewSetup );

    String getIlluminationName( final int viewSetup );

    int getFirstTimePoint();

    int getLastTimePoint();

    /**
     * Returns the number of available resolution levels for the
     * given ViewSetup (channel).
     * Should be 1 (not 0) if only full resolution, original images
     * are available.
     *
     * @param viewSetup ViewSetup (channel) index
     * @return number of resolution levels
     */
    int getNumResolutionLevels( final int viewSetup );

    /**
     * Returns the highest number of available resolution levels
     * across all ViewSetups (channelIds).
     *
     * @return highest number of resolution levels across all channelIds
     */
    int getMaxNumResolutionLevels();

    /**
     * Writes the dimensions (xyz) of the image defined by ViewSetup index,
     * time point and level into out. Returns false in case of failure
     * (e.g. file not found).
     *
     * @param timePoint time point
     * @param viewSetup ViewSetup index,
     * @param level     resolution level
     * @param out       target
     * @return whether or not successful
     */
    boolean getImageDimensions( final int timePoint, final int viewSetup, final int level, final long[] out );

    /**
     * Writes the block dimensions (xyz) of the image defined by ViewSetup index,
     * time point and level into out. Returns false in case of failure
     * (e.g. file not found).
     *
     * @param timePoint time point
     * @param viewSetup ViewSetup index,
     * @param level     resolution level
     * @param out       target
     * @return whether or not successful
     */
    boolean getBlockDimensions( final int timePoint, final int viewSetup, final int level, final int[] out );

    /**
     * Writes the spatial sampling ("voxel size") of the image defined by
     * ViewSetup index, time point and level into out. Returns false in
     * case of failure (e.g. file not found).
     *
     * @param timePoint time point
     * @param viewSetup ViewSetup index,
     * @param level     resolution level
     * @param out       target
     * @return whether or not successful
     */
    boolean getSampling( final int timePoint, final int viewSetup, final int level, final double[] out );

    /**
     * Configures provided KlbImageIO and KlbRoi instances to read a
     * block defined by time point, ViewSetup index, resolution level,
     * block dimensions and block start.
     *
     * @param timePoint  time point index
     * @param viewSetup  ViewSetup index
     * @param level      resolution level
     * @param dimensions block dimensions
     * @param offset     block start
     * @param io         reader
     * @param roi        ROI
     */
    void set(
            final int timePoint,
            final int viewSetup,
            final int level,
            final int[] dimensions,
            final long[] offset,
            final KlbImageIO io,
            final KlbRoi roi
    );
}
