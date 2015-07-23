package org.janelia.simview.klb;

import io.scif.*;
import io.scif.common.DataTools;
import net.imagej.axis.CalibratedAxis;
import net.imagej.axis.LinearAxis;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Helper class, for testing and debugging only.
 */
public class KlbDebugMain {

    public static void main(final String[] args) throws FormatException, IOException {
        String filePath = args.length > 0 ? args[0] : "../testData/img.klb";

        File f = new File(filePath);
        if (!f.exists()) {
            System.out.format("File %s does not exist.\n", f.getAbsolutePath());
            return;
        }

        System.out.format("Reading image at %s\n", f.getAbsolutePath());
        SCIFIO scifio = new SCIFIO();
        Reader reader = scifio.initializer().initializeReader(f.getAbsolutePath());
        System.out.format("  format is '%s', implemented by %s\n", reader.getFormatName(), reader.getFormat().getClass().getName());

        Metadata meta = reader.getMetadata();
        int numImages = meta.getImageCount();
        System.out.format("  contains %d images\n", numImages);
        for (int imageIndex = 0; imageIndex < numImages; ++imageIndex) {
            System.out.format("\nImageIndex %d:\n", imageIndex);
            ImageMetadata iMeta = meta.get(imageIndex);

            int bpp = iMeta.getBitsPerPixel();
            System.out.format("  %d bits per pixel\n", bpp);

            List<CalibratedAxis> axes = iMeta.getAxes();
            System.out.format("  %d axes, %d planar, %d interleaved\n", axes.size(), iMeta.getPlanarAxisCount(), iMeta.getInterleavedAxisCount());
            for (int axisIndex = 0; axisIndex < axes.size(); ++axisIndex) {
                CalibratedAxis axis = axes.get(axisIndex);
                if (axis instanceof LinearAxis) {
                    LinearAxis linearAxis = (LinearAxis) axis;
                    String samplingUnit = linearAxis.type().isSpatial() ? "/pixel" : " sampling";
                    System.out.format("    axis %d (LinearAxis): %s, %d pixels, %.3f %s%s\n", axisIndex, linearAxis.type().toString(), iMeta.getAxisLength(axisIndex), linearAxis.scale(), linearAxis.unit(), samplingUnit);
                } else {
                    System.out.format("    axis %d (CalibratedAxis): %s, %d pixels\n", axisIndex, axis.type().toString(), iMeta.getAxisLength(axisIndex));
                }
            }

            System.out.println("\n  Reading planes:");
            for (int planeIndex = 0; planeIndex < iMeta.getPlaneCount(); ++planeIndex) {
                String txt = String.format("    plane %d:", planeIndex);
                Plane plane = reader.openPlane(imageIndex, planeIndex);
                byte[] bytes = plane.getBytes();
                switch (bpp) {
                    case 8:
                        System.out.format("%s first pixel value: %d\n", txt, bytes[0]);
                        break;
                    case 16:
                        System.out.format("%s first pixel: %d\n", txt, DataTools.bytesToShort(bytes, 0, true));
                        break;
                    default:
                        System.out.println("%s neither 8 nor 16 bpp, while other data types are supported, no message is generated here");
                        break;
                }
            }
        }

        String outFilePath = System.getProperty( "user.home" ) + File.separator + "testout.klb";
        System.out.format( "\nWriting test image to %s\n", outFilePath );
        System.out.format( "You should delete this file manually.");

        Writer writer = scifio.initializer().initializeWriter( meta, outFilePath );
        for ( int i = 0; i < reader.getImageCount(); ++i ) {
            for ( int p = 0; p < reader.getPlaneCount( i ); ++p ) {
                writer.savePlane( i, p, reader.openPlane( i, p ) );
            }
        }

        reader.close();
        writer.close();

        System.out.println( "Done." );
    }
}
