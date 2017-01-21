package org.janelia.simview.klb;

import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.Axis;
import net.imagej.axis.AxisType;
import net.imagej.axis.CalibratedAxis;
import net.imglib2.view.Views;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import org.scijava.widget.FileWidget;

import java.io.File;
import java.io.IOException;

@Plugin( type = Command.class, menuPath = "File>Import>KLB..." )
public class KlbImageJLoader implements Command
{
    protected static KLB klb = null;

    @Parameter
    private DatasetService datasetService;

    @Parameter
    private UIService uiService;

    @Parameter
    private LogService logService;

    @Override
    public void run()
    {
        final File file = uiService.chooseFile( null, FileWidget.OPEN_STYLE );
        if ( file == null )
            return;

        if ( klb == null )
            klb = KLB.newInstance();

        ImgPlus img;
        try {
            img = klb.readFull( file.getAbsolutePath() );
        } catch ( IOException e ) {
            logService.error( e );
            return;
        }

        Dataset data;
        // ImageJ expects xyczt order, loaded order is xyzct.
        // Permute if needed, be mindful of squeezed singleton dimensions.
        int zDim = 2, cDim = -1;
        for ( int i = 0; i < img.numDimensions(); ++i ) {
            final Axis ax = img.axis( i );
            if ( ax instanceof CalibratedAxis ) {
                final AxisType axType = (( CalibratedAxis ) ax).type();
                if ( axType == Axes.CHANNEL ) {
                    cDim = i;
                    break;
                }
            }
        }
        if ( cDim != -1 && cDim != 2 ) {
            data = datasetService.create( Views.permute( img, zDim, cDim ) );
            final CalibratedAxis[] axes = new CalibratedAxis[ img.numDimensions() ];
            for ( int i = 0; i < axes.length; ++i ) {
                if ( i == zDim )
                    axes[ i ] = ( CalibratedAxis ) img.axis( cDim );
                else if ( i == cDim )
                    axes[ i ] = ( CalibratedAxis ) img.axis( zDim );
                else
                    axes[ i ] = ( CalibratedAxis ) img.axis( i );
            }
            data.setAxes( axes );
        } else {
            data = datasetService.create( img );
        }

        uiService.show( file.getName(), data );
    }
}
