package org.janelia.simview.klb;

import io.scif.*;
import io.scif.config.SCIFIOConfig;
import io.scif.io.RandomAccessInputStream;
import io.scif.util.FormatTools;
import net.imagej.axis.*;
import net.imglib2.util.Util;
import org.janelia.simview.klb.jni.*;
import org.scijava.Priority;
import org.scijava.plugin.Plugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Plugin(type = Format.class, priority = Priority.VERY_HIGH_PRIORITY + 1, name = "Keller Lab Block File Format")
public class KlbScifio extends AbstractFormat {

    private static final int NUM_THREADS = Math.max(Runtime.getRuntime().availableProcessors() - 1, 1);
    private static final AxisType[] AXIS_TYPES = {Axes.X, Axes.Y, Axes.Z, Axes.CHANNEL, Axes.TIME};
    private static final String[] UNITS = {"um", "um", "um", "au", "s"};

    private static final Map<KlbDataType, Integer> DTYPES_C2J = new HashMap<KlbDataType, Integer>();
    private static final Map<Integer, KlbDataType> DTYPES_J2C = new HashMap<Integer, KlbDataType>();
    static {
        // Map KLB C data types to SCIFIO Java data types and back.
        DTYPES_J2C.put(FormatTools.UINT8, KlbDataType.UINT8_TYPE);
        DTYPES_C2J.put(KlbDataType.UINT8_TYPE, FormatTools.UINT8);
        DTYPES_J2C.put(FormatTools.UINT16, KlbDataType.UINT16_TYPE);
        DTYPES_C2J.put(KlbDataType.UINT16_TYPE, FormatTools.UINT16);
        DTYPES_J2C.put(FormatTools.UINT32, KlbDataType.UINT32_TYPE);
        DTYPES_C2J.put(KlbDataType.UINT32_TYPE, FormatTools.UINT32);
        DTYPES_J2C.put(FormatTools.INT8, KlbDataType.INT8_TYPE);
        DTYPES_C2J.put(KlbDataType.INT8_TYPE, FormatTools.INT8);
        DTYPES_J2C.put(FormatTools.INT16, KlbDataType.INT16_TYPE);
        DTYPES_C2J.put(KlbDataType.INT16_TYPE, FormatTools.INT16);
        DTYPES_J2C.put(FormatTools.INT32, KlbDataType.INT32_TYPE);
        DTYPES_C2J.put(KlbDataType.INT32_TYPE, FormatTools.INT32);
        DTYPES_J2C.put(FormatTools.FLOAT, KlbDataType.FLOAT32_TYPE);
        DTYPES_C2J.put(KlbDataType.FLOAT32_TYPE, FormatTools.FLOAT);
        DTYPES_J2C.put(FormatTools.DOUBLE, KlbDataType.FLOAT64_TYPE);
        DTYPES_C2J.put(KlbDataType.FLOAT64_TYPE, FormatTools.DOUBLE);
    }

    @Override
    protected String[] makeSuffixArray() {
        return new String[]{"klb"};
    }

    @Override
    public String getFormatName() {
        return "Keller Lab Block File Format";
    }


    public static class Metadata extends AbstractMetadata {
        @Override
        public void populateImageMetadata() {
        }
    }


    public static class Parser extends AbstractParser<Metadata> {

        private final KlbImageIO io;

        public Parser() {
            io = new KlbImageIO();
            io.setNumThreads(NUM_THREADS);
        }

        @Override
        protected void typedParse(final RandomAccessInputStream source, final Metadata meta, final SCIFIOConfig config) throws IOException, FormatException {
            io.setFilename(getSource().getFileName());
            io.readHeader();
            final KlbImageHeader header = io.getHeader();

            meta.createImageMetadata(1);
            final ImageMetadata iMeta = meta.get(0);

            // Unlike the C library, SCIFIO/ImageJ does not (yet) easily support 64bit u/ints.
            // Catch exception when trying to read unsupported or unknown data types.
            int pixelType;
            try {
                pixelType = DTYPES_C2J.get(header.getDataType());
            } catch (NullPointerException ex) {
                throw new FormatException("Unknown or unsupported data type.");
            }

            final long[] dimensions = header.getXyzct();
            final float[] sampling = header.getPixelSize();
            final List<CalibratedAxis> axes = new ArrayList<CalibratedAxis>(sampling.length);
            for (int d = 0; d < sampling.length; ++d) {
                axes.add(new DefaultLinearAxis(AXIS_TYPES[d], UNITS[d], sampling[d]));
            }

            boolean isCalibrated = false;
            for (final float s : sampling) {
                if (s != 1f) {
                    isCalibrated = true;
                    break;
                }
            }
            if (!isCalibrated) {
                for (int d = 0; d < 3; ++d)
                    axes.get(d).setUnit("pixel");
            }

            iMeta.populate(source.getFileName(), axes, dimensions, pixelType, true, true, false, false, true);
            iMeta.setPlanarAxisCount(3); // should be Klb.KLB_DATA_DIMS (=5), but ImageJ doesn't like that currently

            // if uncompressed image >2GB, SCIFIO currently has to read 2D planes
            long nElements = 1;
            for (final long d : dimensions)
                nElements *= d;
            nElements *= iMeta.getBitsPerPixel() / 8;
            if (nElements > Integer.MAX_VALUE - 8)
                iMeta.setPlanarAxisCount(2);
        }
    }


    public static class Reader extends ByteArrayReader<Metadata> {

        private final KlbImageIO io;
        private final KlbRoi roi;
        private final long[] klb_min, klb_max;

        // cache
        private byte[] cachedPlanes;
        private int numPlaneBytes;
        private long cacheMin, cacheMax;

        public Reader() {
            io = new KlbImageIO();
            io.setNumThreads(NUM_THREADS);
            roi = new KlbRoi();
            klb_min = new long[Klb.KLB_DATA_DIMS];
            klb_max = new long[Klb.KLB_DATA_DIMS];
        }

        @Override
        public ByteArrayPlane openPlane(final int imageIndex, final long planeIndex, final ByteArrayPlane plane, final long[] min, final long[] max, final SCIFIOConfig config) throws FormatException, IOException {
            // ImageJ doesn't currently like >3D planes, but KlbRoi is 5D
            System.arraycopy(min, 0, klb_min, 0, min.length);
            System.arraycopy(max, 0, klb_max, 0, max.length);
            for (int d = 0; d < max.length; ++d)
                klb_max[d] -= 1;

            io.setFilename(getCurrentFile());
            io.readHeader();

            /*
             * If uncompressed image >2GB, SCIFIO currently has to read 2D planes.
             * Planar axes count is thus set to 2 when parsing metadata (see above).
             * If so, decompress a block of numPlanarBlocks*blockDims[2] full planes
             * at once and keep them in a cache.
             */
            final ImageMetadata iMeta = getMetadata().get(imageIndex);
            if (iMeta.getPlanarAxisCount() == 2) {
                final int numPlanarBlocks = 6;
                final long lastPlane = iMeta.getAxisLength(2) - 1;
                numPlaneBytes = (int) (max[0] * max[1]) * (iMeta.getBitsPerPixel() / 8);
                final long[] blockDims = io.getHeader().getBlockSize();
                final int numBufferBytes = numPlanarBlocks * (int) blockDims[2] * numPlaneBytes;
                if (cachedPlanes == null || numBufferBytes != cachedPlanes.length) {
                    cachedPlanes = new byte[numBufferBytes];
                    cacheMin = cacheMax = -1;
                }
                if (planeIndex < cacheMin || planeIndex > cacheMax) {
                    cacheMin = (planeIndex / blockDims[2]) * blockDims[2];
                    cacheMax = cacheMin + numPlanarBlocks * blockDims[2] - 1;
                    klb_min[2] = cacheMin;
                    klb_max[2] = cacheMax;
                    roi.setXyzctLB(klb_min);
                    roi.setXyzctUB(klb_max);
                    io.readImage(cachedPlanes, roi, NUM_THREADS);
                }
                final int start = (int) (planeIndex - cacheMin) * numPlaneBytes;
                System.arraycopy(cachedPlanes, start, plane.getBytes(), 0, numPlaneBytes);
                if (planeIndex == lastPlane)
                    cachedPlanes = null;
                return plane;
            }

            // if planar axes count >2, read normally
            roi.setXyzctLB(klb_min);
            roi.setXyzctUB(klb_max);
            io.readImage(plane.getBytes(), roi, NUM_THREADS);
            return plane;
        }

        @Override
        protected String[] createDomainArray() {
            return new String[]{FormatTools.LM_DOMAIN};
        }
    }


    public static class Writer extends AbstractWriter<Metadata> {

        private final KlbImageIO io;
        private final KlbImageHeader header;
        private final long[] dimensions;
        private final float[] sampling;

        public Writer() {
            io = new KlbImageIO();
            io.setNumThreads(NUM_THREADS);
            header = new KlbImageHeader();
            dimensions = new long[Klb.KLB_DATA_DIMS];
            sampling = new float[Klb.KLB_DATA_DIMS];
        }

        @Override
        public void writePlane(final int imageIndex, final long planeIndex, final Plane plane, final long[] min, final long[] max) throws FormatException, IOException {
            log().debug(String.format("KLB: %s.writePlane(imageIndex %d, planeIndex %d, Plane, min %s, max %s)", getClass().getSimpleName(), imageIndex, planeIndex, Util.printCoordinates(min), Util.printCoordinates(max)));
            final ImageMetadata iMeta = getMetadata().get(imageIndex);
            for (int d = 0; d < dimensions.length; ++d) {
                try {
                    final CalibratedAxis axis = iMeta.getAxis(d);
                    dimensions[d] = iMeta.getAxisLength(d);
                    sampling[d] = (float) ((LinearAxis) axis).scale();
                } catch (IndexOutOfBoundsException ex) {
                    dimensions[d] = 1l;
                    sampling[d] = 1f;
                }
            }
            log().debug("KLB: Dimensions: " + Util.printCoordinates(dimensions));
            log().debug("KLB: Sampling: " + Util.printCoordinates(sampling));

            // KLB writes the full image at once, SCIFIO currently handles 2GB max at once.
            long nElements = 1;
            for (final long d : dimensions)
                nElements *= d;
            nElements *= iMeta.getBitsPerPixel() / 8;
            if (nElements > Integer.MAX_VALUE - 8)
                throw new FormatException("Writing KLB files bigger than 2 GB (uncompressed) is currently not supported through SCIFIO/ImageJ.");

            header.setDataType(DTYPES_J2C.get(iMeta.getPixelType()));
            header.setXyzct(dimensions);
            header.setPixelSize(sampling);
            header.setCompressionType(KlbCompressionType.BZIP2);

            io.setHeader(header);
            io.setFilename(getMetadata().getDatasetName());
            io.writeImage(plane.getBytes(), NUM_THREADS);
        }

        @Override
        public boolean canDoStacks() {
            return true;
        }

        @Override
        public boolean writeSequential() {
            return false;
        }

        @Override
        protected String[] makeCompressionTypes() {
            return new String[]{"none", "bzip2"};
        }
    }


    @Plugin(type = Translator.class)
    public static class Translator extends AbstractTranslator<io.scif.Metadata, Metadata> {

        // after adding the @Plugin annotation, Klb.KLB_DATA_DIMS is unavailable, hence use magic 5;
        private final long[] dimensions = new long[5];

        @Override
        public Class<? extends io.scif.Metadata> source() {
            return io.scif.Metadata.class;
        }

        @Override
        public Class<? extends io.scif.Metadata> dest() {
            return Metadata.class;
        }

        @Override
        protected void translateImageMetadata(final List<ImageMetadata> src, final Metadata dst) {
            dst.createImageMetadata(src.size());
            for (int imageIndex = 0; imageIndex < src.size(); ++imageIndex) {
                final ImageMetadata srcIMeta = src.get(imageIndex);
                final ImageMetadata dstIMeta = dst.get(imageIndex);
                final List<CalibratedAxis> axes = new ArrayList<CalibratedAxis>(AXIS_TYPES.length);
                for (int d = 0; d < dimensions.length; ++d) {
                    final int axisIdx = srcIMeta.getAxisIndex(AXIS_TYPES[d]);
                    if (axisIdx != -1) {
                        axes.add(srcIMeta.getAxis(axisIdx).copy());
                        dimensions[d] = srcIMeta.getAxisLength(d);
                    } else {
                        axes.add(new DefaultLinearAxis(AXIS_TYPES[d], UNITS[d], 1f));
                        dimensions[d] = 1l;
                    }
                }
                dstIMeta.populate(dst.getDatasetName(), axes, dimensions, srcIMeta.getPixelType(), true, true, false, false, true);
                dstIMeta.setPlanarAxisCount(3); // should be Klb.KLB_DATA_DIMS (=5), but ImageJ doesn't like that currently
            }
        }
    }
}
