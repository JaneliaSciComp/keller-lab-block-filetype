package org.janelia.simview.klb.bdv;

import bdv.spimdata.XmlIoSpimDataMinimal;
import mpicbg.spim.data.generic.sequence.BasicImgLoader;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.ViewSetup;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import spim.fiji.spimdata.SpimData2;
import spim.fiji.spimdata.XmlIoSpimData2;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Plugin( type = Command.class, menuPath = "Plugins>Multiview Reconstruction>Convert transforms to SiMView" )
public class TransformsSpimreg2Simview implements Command
{
    @Parameter
    private File xmlFile;

    @Parameter
    private File psfFolder;

    @Parameter
    private double lambdaTV = 0.0001;

    @Parameter
    private int iterations = 40;

    @Parameter
    private double background = 100d;

    @Parameter
    private int blockSizeZ = -1;

    @Parameter
    private boolean debugOutput = false;

    @Parameter
    private LogService log;

    @Override
    public void run()
    {
        log.info( "Converting SPIM Registration transforms to SiMView standard" );

        SpimData2 spimData = null;
        BasicImgLoader imageLoader = null;
        try {
            spimData = new XmlIoSpimData2( null ).load( xmlFile.getAbsolutePath() );
            imageLoader = new XmlIoSpimDataMinimal().load( xmlFile.getAbsolutePath() ).getSequenceDescription().getImgLoader();
        } catch ( Exception e ) {
            log.error( e );
        }

        final Map< Integer, String > anglePsfs = new HashMap< Integer, String >();
        final Pattern pattern = Pattern.compile( "view\\d+" );
        for ( final File file : psfFolder.isDirectory() ? psfFolder.listFiles() : psfFolder.getParentFile().listFiles() ) {
            final String name = file.getName().toLowerCase();
            final Matcher matcher = pattern.matcher( name );
            if ( matcher.find() ) {
                final int angleId = Integer.parseInt( name.substring( matcher.start() + "view".length(), matcher.end() ) );
                anglePsfs.put( angleId, file.getAbsolutePath() );
            }
        }

        final List< Integer > angleIds = new ArrayList< Integer >( anglePsfs.keySet() );
        Collections.sort( angleIds );
        for ( final int angleId : angleIds ) {
            log.info( String.format( "PSF for view/angle %d: %s", angleId, anglePsfs.get( angleId ) ) );
        }

        final String outputDir = xmlFile.getParent() + File.separator + "SiMView-XMLs" + File.separator;
        new File( outputDir ).mkdir();

        final KlbPartitionResolverDefault resolver = ( KlbPartitionResolverDefault ) (( KlbImageLoader ) imageLoader).getResolver();
        final List< ViewSetup > viewSetups = spimData.getSequenceDescription().getViewSetupsOrdered();
        final ViewRegistrations viewRegistrations = spimData.getViewRegistrations();
        final DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        final AffineTransform3D xyflip = new AffineTransform3D();
        xyflip.set( 0, 1, 0, 0,
                1, 0, 0, 0,
                0, 0, 1, 0 );
        final double[] transformArray = new double[ 16 ];
        transformArray[ 15 ] = 1d;
        try {
            for ( final TimePoint timePoint : spimData.getSequenceDescription().getTimePoints().getTimePointsOrdered() ) {
                final Map< Integer, Document > channelDocs = new HashMap< Integer, Document >();
                final int timePointId = timePoint.getId();
                final DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
                for ( final ViewSetup viewSetup : viewSetups ) {
                    final int channelId = viewSetup.getChannel().getId();
                    Document doc = channelDocs.get( channelId );
                    if ( doc == null ) {
                        doc = docBuilder.newDocument();
                        final Element docRoot = doc.createElement( "document" );
                        doc.appendChild( docRoot );
                        channelDocs.put( channelId, doc );
                    }

                    final int viewSetupId = viewSetup.getId();
                    final String filePath = resolver.getFilePath( timePointId, viewSetupId, 0 );
                    final ViewRegistration registration = viewRegistrations.getViewRegistration( timePointId, viewSetupId );
                    final AffineTransform3D transform = registration.getModel();
                    transform.preConcatenate( xyflip ).concatenate( xyflip ).toArray( transformArray );

                    final Element view = doc.createElement( "view" );
                    view.setAttribute( "imgFilename", filePath );
                    view.setAttribute( "psfFilename", anglePsfs.get( viewSetup.getAngle().getId() ) );
                    view.setAttribute( "A", String.format( "%.12f %.12f %.12f %.12f %.12f %.12f %.12f %.12f %.12f %.12f %.12f %.12f %.12f %.12f %.12f %.12f", transformArray[ 0 ], transformArray[ 1 ], transformArray[ 2 ], transformArray[ 3 ], transformArray[ 4 ], transformArray[ 5 ], transformArray[ 6 ], transformArray[ 7 ], transformArray[ 8 ], transformArray[ 9 ], transformArray[ 10 ], transformArray[ 11 ], transformArray[ 12 ], transformArray[ 13 ], transformArray[ 14 ], transformArray[ 15 ] ) );
                    doc.getDocumentElement().appendChild( view );
                }

                for ( final int channelId : channelDocs.keySet() ) {
                    final Document doc = channelDocs.get( channelId );
                    final Element deconv = doc.createElement( "deconvolution" );
                    deconv.setAttribute( "lambdaTV", String.format( "%.6f", lambdaTV ) );
                    deconv.setAttribute( "numIter", Integer.toString( iterations ) );
                    deconv.setAttribute( "imBackground", String.format( "%.6f", background ) );
                    deconv.setAttribute( "verbose", debugOutput ? "1" : "0" );
                    deconv.setAttribute( "blockZsize", Integer.toString( blockSizeZ ) );
                    doc.getDocumentElement().appendChild( deconv );

                    final String outputFile = String.format( "%sTM%06dCHN%02d.xml", outputDir, timePointId, channelId );
                    Transformer transformer = TransformerFactory.newInstance().newTransformer();
                    transformer.setOutputProperty( OutputKeys.INDENT, "yes" );
                    DOMSource source = new DOMSource( doc );
                    StreamResult console = new StreamResult( outputFile );
                    transformer.transform( source, console );
                }
            }

        } catch ( Exception e ) {
            log.error( e );
        }

        log.info( "Done." );
    }
}
