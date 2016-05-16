package org.janelia.simview.klb;

import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imagej.ImgPlus;
import org.scijava.command.Command;
import org.scijava.display.DisplayService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.io.IOException;

@Plugin( type = Command.class, menuPath = "File>Import>KLB..." )
public class KlbImageJLoader implements Command
{
    protected static KLB klb = null;

    @Parameter
    private DatasetService datasetService;

    @Parameter
    private DisplayService displayService;

    @Override
    public void run()
    {
        try {
            UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
        } catch ( ClassNotFoundException e ) {
            e.printStackTrace();
        } catch ( InstantiationException e ) {
            e.printStackTrace();
        } catch ( IllegalAccessException e ) {
            e.printStackTrace();
        } catch ( UnsupportedLookAndFeelException e ) {
            e.printStackTrace();
        }

        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle( "Import KLB file" );
        fileChooser.setFileFilter( new FileFilter()
        {
            @Override
            public boolean accept( final File f )
            {
                return f.isDirectory() || f.getName().toLowerCase().endsWith( ".klb" );
            }

            @Override
            public String getDescription()
            {
                return "KLB files";
            }
        } );
        if ( fileChooser.showOpenDialog( null ) != JFileChooser.APPROVE_OPTION ) {
            return;
        }

        if ( klb == null ) {
            klb = KLB.newInstance();
        }

        final String file = fileChooser.getSelectedFile().getAbsolutePath();
        try {
            final ImgPlus img = klb.readFull( file );
            final Dataset data = datasetService.create( img );
            displayService.createDisplay( data );
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }
}
