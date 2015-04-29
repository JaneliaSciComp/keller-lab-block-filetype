package org.janelia.simview.klb.bdv.ui;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SingleFilePathPanel extends JPanel implements ActionListener
{
    private final JTextField textField = new JTextField( System.getProperty( "user.home" ) );
    private final JButton button = new JButton( "..." );
    private final int selectionMode;

    // ToDo: use events rather than references
    private final NameTagPanel nameTagPanel;
    private final SpecifySamplingPanel samplingPanel;

    public SingleFilePathPanel( final String label, final int fileChooserSelectionMode, final NameTagPanel nameTagPanel, final SpecifySamplingPanel samplingPanel )
    {
        this.nameTagPanel = nameTagPanel;
        this.samplingPanel = samplingPanel;

        selectionMode = fileChooserSelectionMode;
        textField.setEditable( false );

        button.addActionListener( this );

        setLayout( new MigLayout( "", "[][grow][]", "[]" ) );
        add( new JLabel( label ), "cell 0 0" );
        add( textField, "cell 1 0, grow" );
        add( button, "cell 2 0" );
    }

    public String getFilePath()
    {
        return textField.getText();
    }

    @Override
    public void actionPerformed( final ActionEvent e )
    {
        if ( e.getSource() == button ) {
            final JFileChooser chooser = new JFileChooser( textField.getText() );
            chooser.setFileSelectionMode( selectionMode );
            if ( chooser.showOpenDialog( this ) == JFileChooser.APPROVE_OPTION ) {
                final String filePath = chooser.getSelectedFile().getAbsolutePath();
                textField.setText( filePath );

                nameTagPanel.updateTemplate( filePath );
                if ( !samplingPanel.isSamplingSpecified() ) {
                    samplingPanel.updateSampling( filePath );
                }
            }
        }
    }
}
