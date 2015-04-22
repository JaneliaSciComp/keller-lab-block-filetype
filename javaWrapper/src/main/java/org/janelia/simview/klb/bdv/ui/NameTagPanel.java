package org.janelia.simview.klb.bdv.ui;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NameTagPanel extends JPanel
{
    private final JTable table = new JTable();
    private final NameTagTableModel model = new NameTagTableModel();

    public NameTagPanel()
    {
        table.setModel( model );

        setLayout( new MigLayout( "", "[grow]", "[grow]" ) );
        add( new JScrollPane( table ), "cell 0 0, grow" );

        setMaximumSize( new Dimension( Integer.MAX_VALUE, 150 ) );
    }

    public String[] getNameTags()
    {
        return model.getNameTags();
    }

    public void updateTemplate( final String template )
    {
        model.updateTemplate( template );
    }

    public int[] getIndices()
    {
        return model.getIndices();
    }


    class NameTagTableModel extends DefaultTableModel
    {

        public NameTagTableModel()
        {
            addColumn( "Dimension", new String[]{ "Channel", "Time" } );
            addColumn( "Name tag", new String[]{ "CHN", "TM" } );
            addColumn( "First", new Integer[]{ 0, 0 } );
            addColumn( "Last", new Integer[]{ 0, 0 } );
        }

        @Override
        public boolean isCellEditable( final int rowIndex, final int columnIndex )
        {
            if ( columnIndex == 0 ) {
                return false;
            }
            return true;
        }

        public void updateTemplate( final String template )
        {
            for ( int row = 0; row < getRowCount(); ++row ) {
                final String tag = getValueAt( row, 1 ).toString();
                final Pattern pattern = Pattern.compile( String.format( "%s\\d+", tag ) );
                final Matcher matcher = pattern.matcher( template );
                if ( matcher.find() ) {
                    final String subst = template.substring( matcher.start(), matcher.end() );
                    final int value = Integer.parseInt( subst.substring( tag.length() ) );
                    setValueAt( value, row, 3 );
                    fireTableCellUpdated( row, 3 );
                }
            }
        }

        public String[] getNameTags()
        {
            return new String[]{
                    getValueAt( 0, 1 ).toString(),
                    getValueAt( 1, 1 ).toString()
            };
        }

        public int[] getIndices()
        {
            return new int[]{
                    Integer.parseInt( getValueAt( 0, 2 ).toString() ),
                    Integer.parseInt( getValueAt( 0, 3 ).toString() ),
                    Integer.parseInt( getValueAt( 1, 2 ).toString() ),
                    Integer.parseInt( getValueAt( 1, 3 ).toString() )
            };
        }
    }

}
