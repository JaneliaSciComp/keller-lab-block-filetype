package org.janelia.simview.klb.bdv.ui;

import net.miginfocom.swing.MigLayout;
import org.janelia.simview.klb.bdv.KlbMultiFileNameTag;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NameTagPanel extends JPanel
{
    private String template = "";
    private final List< KlbMultiFileNameTag > tags;
    private final JTable table = new JTable();
    private final NameTagTableModel model;

    public NameTagPanel( final List< KlbMultiFileNameTag > tags )
    {
        this.tags = tags;
        Collections.sort( this.tags );
        model = new NameTagTableModel();
        table.setModel( model );

        setLayout( new MigLayout( "", "[grow]", "[][grow]" ) );
        add( new JLabel( "Name tags that are blank or not found in template file path will be ignored." ), "cell 0 0" );
        add( new JScrollPane( table ), "cell 0 1, grow" );

        setMaximumSize( new Dimension( Integer.MAX_VALUE, model.getRowCount() * 35 ) );
    }

    public void updateTemplate( final String template )
    {
        this.template = template;
        model.updateTemplate();
    }


    private class NameTagTableModel extends DefaultTableModel
    {
        private final String[] columnHeaders = {
                "Dimension", "File Path Tag", "First", "Last", "Stride" };

        public void updateTemplate()
        {
            for ( int row = 0; row < getRowCount(); ++row ) {
                updateTemplate( row );
            }
        }

        private void updateTemplate( final int row )
        {
            final KlbMultiFileNameTag tag = tags.get( row );
            if ( tag.tag.trim().isEmpty() ) {
                return;
            }
            final Pattern pattern = Pattern.compile( String.format( "%s\\d+", tag.tag ) );
            final Matcher matcher = pattern.matcher( template );
            if ( matcher.find() ) {
                final String subst = template.substring( matcher.start(), matcher.end() );
                final int last = Integer.parseInt( subst.substring( tag.tag.length() ) );
                setValueAt( last, row, 3 );
            }
        }

        @Override
        public int getColumnCount()
        {
            return 5;
        }

        @Override
        public int getRowCount()
        {
            return tags.size();
        }

        @Override
        public String getColumnName( final int column )
        {
            return columnHeaders[ column ];
        }

        @Override
        public void setValueAt( final Object value, final int row, final int column )
        {
            final KlbMultiFileNameTag tag = tags.get( row );
            switch ( column ) {
                case 1:
                    tag.tag = value.toString();
                    break;
                case 2:
                    tag.first = Integer.parseInt( value.toString() );
                    break;
                case 3:
                    tag.last = Integer.parseInt( value.toString() );
                    break;
                case 4:
                    tag.stride = Integer.parseInt( value.toString() );
                    break;
                default:
                    throw new IndexOutOfBoundsException();
            }
            fireTableCellUpdated( row, column );
            if ( column == 0 || column == 2 ) {
                updateTemplate( row );
            }
        }

        @Override
        public Object getValueAt( final int row, final int column )
        {
            final KlbMultiFileNameTag tag = tags.get( row );
            switch ( column ) {
                case 0:
                    return tag.dimension.toString();
                case 1:
                    return tag.tag;
                case 2:
                    return tag.first;
                case 3:
                    return tag.last;
                case 4:
                    return tag.stride;
                default:
                    throw new IndexOutOfBoundsException();
            }
        }

        @Override
        public Class< ? > getColumnClass( final int column )
        {
            final KlbMultiFileNameTag tag = tags.get( 0 );
            switch ( column ) {
                case 0:
                    return tag.dimension.toString().getClass();
                case 1:
                    return tag.tag.getClass();
                case 2:
                    return tag.first.getClass();
                case 3:
                    return tag.last.getClass();
                case 4:
                    return tag.stride.getClass();
                default:
                    throw new IndexOutOfBoundsException();
            }
        }

        @Override
        public boolean isCellEditable( final int row, final int column )
        {
            if ( tags.get( row ).dimension == KlbMultiFileNameTag.Dimension.RESOLUTION_LEVEL && (column == 2 || column == 4) )
                return false;
            return column != 0;
        }
    }
}

