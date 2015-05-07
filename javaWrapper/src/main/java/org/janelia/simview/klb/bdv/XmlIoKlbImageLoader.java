package org.janelia.simview.klb.bdv;

import mpicbg.spim.data.XmlHelpers;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.ImgLoaderIo;
import mpicbg.spim.data.generic.sequence.XmlIoBasicImgLoader;
import org.jdom2.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static mpicbg.spim.data.XmlKeys.IMGLOADER_FORMAT_ATTRIBUTE_NAME;

@ImgLoaderIo( format = "klb", type = KlbImageLoader.class )
public class XmlIoKlbImageLoader implements XmlIoBasicImgLoader< KlbImageLoader >
{

    @Override
    public Element toXml( final KlbImageLoader imgLoader, final File basePath )
    {
        final Element elem = new Element( "ImageLoader" );
        elem.setAttribute( IMGLOADER_FORMAT_ATTRIBUTE_NAME, "klb" );
        elem.addContent( resolverToXml( imgLoader.getResolver() ) );
        return elem;
    }

    @Override
    public KlbImageLoader fromXml( final Element elem, final File basePath, final AbstractSequenceDescription< ?, ?, ? > sequenceDescription )
    {
        final KlbPartitionResolver resolver = resolverFromXml( elem.getChild( "Resolver" ) );
        return new KlbImageLoader( resolver, sequenceDescription );
    }

    private Element resolverToXml( final KlbPartitionResolver resolver )
    {
        final Element resolverElem = new Element( "Resolver" );

        final String type = resolver.getClass().getName();
        resolverElem.setAttribute( "type", type );

        if ( type.equals( KlbPartitionResolverDefault.class.getName() ) ) {
            final KlbPartitionResolverDefault typedResolver = ( KlbPartitionResolverDefault ) resolver;
            for ( final String template : typedResolver.viewSetupTemplates ) {
                final Element templateElem = new Element( "ViewSetupTemplate" );
                templateElem.addContent( XmlHelpers.textElement( "template", template ) );
                resolverElem.addContent( templateElem );
            }
            if ( typedResolver.getLastTimePoint() - typedResolver.getFirstTimePoint() > 0 ) {
                KlbMultiFileNameTag tag = new KlbMultiFileNameTag();
                tag.dimension = KlbMultiFileNameTag.Dimension.TIME;
                tag.tag = typedResolver.timeTag;
                tag.first = typedResolver.getFirstTimePoint();
                tag.last = typedResolver.getLastTimePoint();
                tag.stride = 1;
                resolverElem.addContent( nameTagToXml( tag ) );
            }
        }

        return resolverElem;
    }

    private KlbPartitionResolver resolverFromXml( final Element elem )
    {
        final String type = elem.getAttributeValue( "type" );
        if ( type.equals( KlbPartitionResolverDefault.class.getName() ) ) {
            final List< String > templates = new ArrayList< String >();

            for ( final Element e : elem.getChildren( "ViewSetupTemplate" ) ) {
                templates.add( XmlHelpers.getText( e, "template" ) );
            }

            final List< KlbMultiFileNameTag > tags = new ArrayList< KlbMultiFileNameTag >();
            for ( final Element e : elem.getChildren( "MultiFileNameTag" ) ) {
                tags.add( nameTagFromXml( e ) );
            }
            if (tags.isEmpty()) {
                final KlbMultiFileNameTag tag = new KlbMultiFileNameTag();
                tag.tag = "";
                tag.dimension = KlbMultiFileNameTag.Dimension.TIME;
                tag.first = 0;
                tag.last = 0;
                tag.stride = 1;
                tags.add( tag );
            }

            String[] arr = new String[ templates.size() ];
            templates.toArray( arr );
            return new KlbPartitionResolverDefault( arr, tags.get( 0 ).tag, tags.get( 0 ).first, tags.get( 0 ).last, "RESLVL", 1 );
        }

        throw new RuntimeException( "Could not instantiate KlbPartitionResolver" );
    }

    private Element nameTagToXml( final KlbMultiFileNameTag tag )
    {
        final Element elem = new Element( "MultiFileNameTag" );
        elem.addContent( XmlHelpers.textElement( "dimension", tag.dimension.toString() ) );
        elem.addContent( XmlHelpers.textElement( "tag", tag.tag ) );
        elem.addContent( XmlHelpers.intElement( "lastIndex", tag.last ) );

        if ( tag.first != 0 ) {
            elem.addContent( XmlHelpers.intElement( "firstIndex", tag.first ) );
        }

        if ( tag.stride != 1 ) {
            elem.addContent( XmlHelpers.intElement( "indexStride", tag.stride ) );
        }

        return elem;
    }

    private KlbMultiFileNameTag nameTagFromXml( final Element elem )
    {
        final KlbMultiFileNameTag tag = new KlbMultiFileNameTag();
        final String dim = XmlHelpers.getText( elem, "dimension" );
        if ( dim.equals( KlbMultiFileNameTag.Dimension.TIME.toString() ) ) {
            tag.dimension = KlbMultiFileNameTag.Dimension.TIME;
        }
        tag.tag = XmlHelpers.getText( elem, "tag" );
        tag.last = XmlHelpers.getInt( elem, "lastIndex" );

        tag.first = 0;
        try {
            tag.first = XmlHelpers.getInt( elem, "firstIndex" );
        } catch ( Exception ex ) {
        }

        tag.stride = 1;
        try {
            tag.stride = XmlHelpers.getInt( elem, "indexStride" );
        } catch ( Exception ex ) {
        }

        return tag;
    }
}
