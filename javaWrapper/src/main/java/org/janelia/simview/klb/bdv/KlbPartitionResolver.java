package org.janelia.simview.klb.bdv;

import org.janelia.simview.klb.jni.KlbImageIO;
import org.janelia.simview.klb.jni.KlbRoi;

/**
 * Defines a dataset consisting of one or multiple KLB files.
 * <p/>
 * Interfaces with Fiji's Big Data Viewer and SPIM plugins through
 * KlbDataset and KlbSpimDataAdapter.
 * KlbDataset uses a KlbPartitionResolver to assemble both file
 * system-related and image-related metadata.
 * KlbSpimDataAdapter uses a KlbDataset to generate a SpimData2
 * instance that can be consumed by Fiji and/or saved to XML.
 * <p/>
 * Returns metadata that is apparent from the file system, such as the
 * number of ViewSetups, time points and scales.
 * <p/>
 * Retrieves basic image-related metadata (image dimensions, block
 * dimensions, sampling).
 * <p/>
 * Provides the file system path to a representative KLB file to read
 * additional metadata, if necessary.
 * <p/>
 * Configures provided KlbImageIO and KlbRoi instances to read a block
 * defined by time point, ViewSetup, level, ROI start and
 * ROI dimensions.
 */
public interface KlbPartitionResolver
{

    int getFirstViewSetup();

    int getLastViewSetup();

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
     * across all ViewSetups (channels).
     *
     * @return highest number of resolution levels across all channels
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

    /**
     * Returns the file system path to a KLB file that is
     * representative for this dataset. This can for
     * instance be used to read additional image-related
     * metadata.
     *
     * @return file system path to representative KLB file
     */
    String getRepresentativeFilePath();
}
