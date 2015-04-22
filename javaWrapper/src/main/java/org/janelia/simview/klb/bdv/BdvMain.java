package org.janelia.simview.klb.bdv;

import org.janelia.simview.klb.bdv.ui.NamePatternSimpleDialog;

import javax.swing.*;
import java.awt.*;

public class BdvMain
{

    public static void main( final String[] args )
    {
        try {
            UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
        } catch ( Exception ex ) {
        }

        EventQueue.invokeLater( new Runnable()
        {
            public void run()
            {
                try {
                    final JDialog dialog = new NamePatternSimpleDialog();
                    dialog.pack();
                    dialog.setLocationRelativeTo( null );
                    dialog.setVisible( true );
                } catch ( Exception ex ) {
                    ex.printStackTrace();
                }
            }
        } );
    }
}
