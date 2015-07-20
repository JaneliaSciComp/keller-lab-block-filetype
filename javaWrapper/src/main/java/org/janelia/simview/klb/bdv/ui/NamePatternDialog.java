package org.janelia.simview.klb.bdv.ui;

import bdv.BigDataViewer;
import bdv.export.ProgressWriterConsole;
import mpicbg.spim.data.SpimDataException;
import net.miginfocom.swing.MigLayout;
import org.janelia.simview.klb.bdv.KlbMultiFileNameTag;
import org.janelia.simview.klb.bdv.KlbPartitionResolver;
import org.janelia.simview.klb.bdv.KlbPartitionResolverDefault;
import org.janelia.simview.klb.bdv.KlbSpimDataAdapter;

import javax.swing.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class NamePatternDialog extends JDialog implements ActionListener
{
    private final List< KlbMultiFileNameTag > tags;
    private final NameTagPanel nameTagPanel;
    private final SpecifySamplingPanel samplingPanel = new SpecifySamplingPanel();
    private final SingleFilePathPanel filePathPanel;
    private final JButton viewButton = new JButton( "View in Big Data Viewer" );
    private final JButton saveXmlButton = new JButton( "Save XML" );
    private final JButton cancelButton = new JButton( "Cancel" );

    public NamePatternDialog()
    {
        setTitle( "Import KLB Dataset" );

        final KlbMultiFileNameTag angleTag = new KlbMultiFileNameTag();
        angleTag.dimension = KlbMultiFileNameTag.Dimension.ANGLE;
        angleTag.tag = "CM";

        final KlbMultiFileNameTag channelTag = new KlbMultiFileNameTag();
        channelTag.dimension = KlbMultiFileNameTag.Dimension.CHANNEL;
        channelTag.tag = "CHN";

        final KlbMultiFileNameTag illuminationTag = new KlbMultiFileNameTag();
        illuminationTag.dimension = KlbMultiFileNameTag.Dimension.ILLUMINATION;
        illuminationTag.tag = "";

        final KlbMultiFileNameTag timeTag = new KlbMultiFileNameTag();
        timeTag.dimension = KlbMultiFileNameTag.Dimension.TIME;
        timeTag.tag = "TM";

        final KlbMultiFileNameTag levelTag = new KlbMultiFileNameTag();
        levelTag.dimension = KlbMultiFileNameTag.Dimension.RESOLUTION_LEVEL;
        levelTag.tag = "RESLVL";

        tags = new ArrayList< KlbMultiFileNameTag >();
        tags.add( angleTag );
        tags.add( channelTag );
        tags.add( illuminationTag );
        tags.add( timeTag );
        tags.add( levelTag );

        nameTagPanel = new NameTagPanel( tags );
        filePathPanel = new SingleFilePathPanel( "Template file", JFileChooser.FILES_ONLY, nameTagPanel, samplingPanel );

        viewButton.addActionListener( this );
        saveXmlButton.addActionListener( this );
        cancelButton.addActionListener( this );

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

    public KlbPartitionResolver getResolver()
    {
        final KlbPartitionResolverDefault resolver = new KlbPartitionResolverDefault( filePathPanel.getFilePath(), tags );
        if ( samplingPanel.isSamplingSpecified() ) {
            final double[] fromPanel = samplingPanel.getSampling();
            final double[][] sampling = new double[ resolver.getMaxNumResolutionLevels() ][ 3 ];
            sampling[ 0 ][ 0 ] = fromPanel[ 0 ];
            sampling[ 0 ][ 1 ] = fromPanel[ 1 ];
            sampling[ 0 ][ 2 ] = fromPanel[ 2 ];
            resolver.specifySampling( sampling );
        }
        return resolver;
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
            final KlbSpimDataAdapter spimData = new KlbSpimDataAdapter( getResolver() );
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
                String filePath = chooser.getSelectedFile().getAbsolutePath();
                if (!filePath.endsWith( ".xml" )) {
                    filePath += ".xml";
                }
                final KlbSpimDataAdapter spimData = new KlbSpimDataAdapter( getResolver() );
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
