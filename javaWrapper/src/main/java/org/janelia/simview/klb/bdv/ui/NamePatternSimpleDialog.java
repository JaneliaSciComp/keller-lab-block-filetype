package org.janelia.simview.klb.bdv.ui;

import bdv.BigDataViewer;
import bdv.export.ProgressWriterConsole;
import mpicbg.spim.data.SpimDataException;
import net.miginfocom.swing.MigLayout;
import org.janelia.simview.klb.bdv.KlbDataset;
import org.janelia.simview.klb.bdv.KlbPartitionResolver;
import org.janelia.simview.klb.bdv.KlbPartitionResolverNamePatternSimple;
import org.janelia.simview.klb.bdv.KlbSpimDataAdapter;

import javax.swing.*;
import java.awt.event.*;

public class NamePatternSimpleDialog extends JDialog implements ActionListener
{
    private final NameTagPanel nameTagPanel = new NameTagPanel();
    private final OverrideSamplingPanel samplingPanel = new OverrideSamplingPanel();
    private final SingleFilePathPanel filePathPanel = new SingleFilePathPanel( "Template file", JFileChooser.FILES_ONLY, nameTagPanel, samplingPanel );
    private final JButton viewButton = new JButton( "View in Big Data Viewer" );
    private final JButton saveXmlButton = new JButton( "Save XML" );
    private final JButton cancelButton = new JButton( "Cancel" );

    public NamePatternSimpleDialog()
    {
        viewButton.addActionListener( this );
        saveXmlButton.addActionListener( this );
        cancelButton.addActionListener( this );

        setTitle( "Load simple name pattern" );

        final JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout( new MigLayout( "", "[grow][][][]", "[]" ) );
        buttonPanel.add( viewButton, "cell 1 0" );
        buttonPanel.add( saveXmlButton, "cell 2 0" );
        buttonPanel.add( cancelButton, "cell 3 0" );

        setLayout( new MigLayout( "", "[grow]", "[][][][]" ) );
        add( filePathPanel, "cell 0 0, grow" );
        add( nameTagPanel, "cell 0 1, grow" );
        add( samplingPanel, "cell 0 2, grow" );
        add( buttonPanel, "cell 0 3, grow" );

        // call onCancel() when cross is clicked
        setDefaultCloseOperation( DO_NOTHING_ON_CLOSE );
        addWindowListener( new WindowAdapter()
        {
            public void windowClosing( final WindowEvent e )
            {
                onCancel();
            }
        } );

        // call onCancel() on ESCAPE
        (( JPanel ) getContentPane()).registerKeyboardAction( new ActionListener()
        {
            public void actionPerformed( final ActionEvent e )
            {
                onCancel();
            }
        }, KeyStroke.getKeyStroke( KeyEvent.VK_ESCAPE, 0 ), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT );
    }

    private KlbDataset getDataset()
    {
        final String[] tags = nameTagPanel.getNameTags();
        final int[] indices = nameTagPanel.getIndices();
        final KlbPartitionResolver resolver = new KlbPartitionResolverNamePatternSimple( filePathPanel.getFilePath(), tags[ 0 ], tags[ 1 ], indices[ 0 ], indices[ 1 ], indices[ 2 ], indices[ 3 ], 1 );
        final KlbDataset data = samplingPanel.doOverrideSampling() ? new KlbDataset( resolver, samplingPanel.getSampling() ) : new KlbDataset( resolver );
        return data;
    }

    private void onCancel()
    {
        dispose();
    }

    @Override
    public void actionPerformed( final ActionEvent event )
    {
        final Object source = event.getSource();

        if ( source == viewButton ) {
            final KlbSpimDataAdapter spimData = new KlbSpimDataAdapter( getDataset() );
            try {
                new BigDataViewer( spimData.createDataset(), filePathPanel.getFilePath(), new ProgressWriterConsole() );
            } catch ( Exception ex ) {
                ex.printStackTrace();
            }
            dispose();

        } else if ( source == saveXmlButton ) {
            final JFileChooser chooser = new JFileChooser( filePathPanel.getFilePath() );
            chooser.setFileSelectionMode( JFileChooser.FILES_ONLY );
            if ( chooser.showSaveDialog( this ) == JFileChooser.APPROVE_OPTION ) {
                final String filePath = chooser.getSelectedFile().getAbsolutePath();
                final KlbSpimDataAdapter spimData = new KlbSpimDataAdapter( getDataset() );
                try {
                    spimData.writeXML( filePath );
                } catch ( SpimDataException ex ) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog( this,
                            "Failed to save XML.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE );
                }
            }
            dispose();

        } else if ( source == cancelButton ) {
            onCancel();
        }
    }
}
