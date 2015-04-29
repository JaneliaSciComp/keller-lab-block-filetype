package org.janelia.simview.klb.bdv.ui;

import net.miginfocom.swing.MigLayout;
import org.janelia.simview.klb.jni.KlbImageHeader;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class SpecifySamplingPanel extends JPanel implements ActionListener
{
    private boolean isSamplingSpecified = false;
    private final JCheckBox checkBox = new JCheckBox( "Specify sampling" );
    private final JTable table = new JTable();
    private final SamplingTableModel model = new SamplingTableModel();

    public SpecifySamplingPanel()
    {
        table.setModel( model );

        checkBox.addActionListener( this );
        checkBox.setSelected( isSamplingSpecified );

        setLayout( new MigLayout( "", "[][grow]", "[top]" ) );
        add( checkBox, "cell 0 0" );
        add( new JScrollPane( table ), "cell 1 0, grow" );

        setMaximumSize( new Dimension( Integer.MAX_VALUE, 90 ) );
    }

    public boolean isSamplingSpecified()
    {
        return isSamplingSpecified;
    }

    public double[] getSampling()
    {
        return model.getSampling();
    }

    public void updateSampling( final String filePath )
    {
        model.updateSampling( filePath );
    }

    @Override
    public void actionPerformed( final ActionEvent e )
    {
        if ( e.getSource() == checkBox ) {
            isSamplingSpecified = checkBox.isSelected();
            table.setEnabled( isSamplingSpecified );
        }
    }


    class SamplingTableModel extends DefaultTableModel
    {

        public SamplingTableModel()
        {
            addColumn( "x", new Double[]{ 1d } );
            addColumn( "y", new Double[]{ 1d } );
            addColumn( "z", new Double[]{ 1d } );
        }

        public void updateSampling( final String filePath )
        {
            if ( filePath.toLowerCase().endsWith( ".klb" ) && new File( filePath ).exists() ) {
                final KlbImageHeader header = new KlbImageHeader();
                if ( header.readHeader( filePath ) == 0 ) {
                    final float[] sampling = header.getPixelSize();
                    setValueAt( ( double ) sampling[ 0 ], 0, 0 );
                    setValueAt( ( double ) sampling[ 1 ], 0, 1 );
                    setValueAt( ( double ) sampling[ 2 ], 0, 2 );
                    fireTableRowsUpdated( 0, 0 );
                }
            }
        }

        public double[] getSampling()
        {
            return new double[]{
                    Double.parseDouble( getValueAt( 0, 0 ).toString() ),
                    Double.parseDouble( getValueAt( 0, 1 ).toString() ),
                    Double.parseDouble( getValueAt( 0, 2 ).toString() )
            };
        }
    }

}
