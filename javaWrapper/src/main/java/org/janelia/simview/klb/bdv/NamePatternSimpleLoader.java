package org.janelia.simview.klb.bdv;

import org.janelia.simview.klb.bdv.ui.NamePatternSimpleDialog;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

import javax.swing.*;

@Plugin( type = Command.class, menuPath = "File>Import>KLB Dataset (Simple Name Tag Pattern)..." )
public class NamePatternSimpleLoader implements Command
{

    @Override
    public void run()
    {
        final JDialog d = new NamePatternSimpleDialog();
        d.pack();
        d.setLocationRelativeTo( null );
        d.setVisible( true );
    }
}
