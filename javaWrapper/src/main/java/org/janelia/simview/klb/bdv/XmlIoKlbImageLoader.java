package org.janelia.simview.klb.bdv;

import mpicbg.spim.data.XmlHelpers;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.ImgLoaderIo;
import mpicbg.spim.data.generic.sequence.XmlIoBasicImgLoader;
import org.jdom2.Element;

import java.io.File;

import static mpicbg.spim.data.XmlKeys.IMGLOADER_FORMAT_ATTRIBUTE_NAME;

@ImgLoaderIo( format = "klb", type = KlbImageLoader.class )
public class XmlIoKlbImageLoader implements XmlIoBasicImgLoader< KlbImageLoader >
{

    @Override
    public Element toXml( final KlbImageLoader imgLoader, final File basePath )
    {
        final Element elem = new Element( "ImageLoader" );
        elem.setAttribute( IMGLOADER_FORMAT_ATTRIBUTE_NAME, "klb" );
        elem.addContent( resolverToXml( imgLoader.getKlbDataset().getResolver() ) );
        return elem;
    }

    @Override
    public KlbImageLoader fromXml( final Element elem, final File basePath, final AbstractSequenceDescription< ?, ?, ? > sequenceDescription )
    {
        final KlbPartitionResolver resolver = resolverFromXml( elem );
        return new KlbImageLoader( new KlbDataset( resolver ) );
    }

    private Element resolverToXml( final KlbPartitionResolver resolver )
    {
        final Element elem = new Element( "resolver" );

        final String type = resolver.getClass().getName();
        elem.addContent( XmlHelpers.textElement( "type", type ) );

        if ( type.equals( KlbPartitionResolverNamePatternSimple.class.getName() ) ) {
            final KlbPartitionResolverNamePatternSimple typedInstance = ( KlbPartitionResolverNamePatternSimple ) resolver;
            elem.addContent( XmlHelpers.textElement( "template", typedInstance.template ) );
            elem.addContent( XmlHelpers.textElement( "setupTag", typedInstance.setupTag ) );
            elem.addContent( XmlHelpers.textElement( "timeTag", typedInstance.timeTag ) );
            elem.addContent( XmlHelpers.intElement( "firstSetup", typedInstance.firstSetup ) );
            elem.addContent( XmlHelpers.intElement( "lastSetup", typedInstance.lastSetup ) );
            elem.addContent( XmlHelpers.intElement( "firstTimePoint", typedInstance.firstTimePoint ) );
            elem.addContent( XmlHelpers.intElement( "lastTimePoint", typedInstance.lastTimePoint ) );
            elem.addContent( XmlHelpers.intElement( "numResolutionLevels", typedInstance.numResolutionLevels ) );
        }

        return elem;
    }

    private KlbPartitionResolver resolverFromXml( final Element elem )
    {
        final Element resolverElem = elem.getChild( "resolver" );
        final String type = XmlHelpers.getText( resolverElem, "type" );
        if ( type.equals( KlbPartitionResolverNamePatternSimple.class.getName() ) ) {
            final String template = XmlHelpers.getText( resolverElem, "template" );
            final String setupTag = XmlHelpers.getText( resolverElem, "setupTag" );
            final String timeTag = XmlHelpers.getText( resolverElem, "timeTag" );
            final int firstSetup = XmlHelpers.getInt( resolverElem, "firstSetup" );
            final int lastSetup = XmlHelpers.getInt( resolverElem, "lastSetup" );
            final int firstTimePoint = XmlHelpers.getInt( resolverElem, "firstTimePoint" );
            final int lastTimePoint = XmlHelpers.getInt( resolverElem, "lastTimePoint" );
            final int numScales = XmlHelpers.getInt( resolverElem, "numResolutionLevels" );

            return new KlbPartitionResolverNamePatternSimple( template, setupTag, timeTag, firstSetup, lastSetup, firstTimePoint, lastTimePoint, numScales );
        }

        throw new RuntimeException( "Could not instantiate KlbPartitionResolver" );
    }
}
