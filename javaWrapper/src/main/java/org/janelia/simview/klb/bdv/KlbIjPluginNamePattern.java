package org.janelia.simview.klb.bdv;

import org.janelia.simview.klb.bdv.ui.NamePatternDialog;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

import javax.swing.*;

@Plugin( type = Command.class, menuPath = "File>Import>KLB Dataset (by File Name Pattern)..." )
public class KlbIjPluginNamePattern implements Command
{

    @Override
    public void run()
    {
        final JDialog dialog = new NamePatternDialog();
        dialog.pack();
        dialog.setLocationRelativeTo( null );
        dialog.setVisible( true );
    }
}
