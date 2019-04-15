/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.utils.alkis;

import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecoder;
import com.sun.media.jai.codec.TIFFDecodeParam;

import net.sf.jasperreports.engine.JRDefaultScriptlet;

import org.apache.log4j.Logger;

import org.openide.util.Exceptions;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;

import java.io.IOException;
import java.io.InputStream;

import java.net.URL;

import java.util.Collection;

import javax.imageio.ImageIO;

import javax.media.jai.RenderedImageAdapter;

import de.cismet.commons.security.handler.ExtendedAccessHandler;
import de.cismet.commons.security.handler.SimpleHttpAccessHandler;

/**
 * DOCUMENT ME!
 *
 * @author   jweintraut
 * @version  $Revision$, $Date$
 */
public class AlkisPunktReportScriptlet extends JRDefaultScriptlet {

    //~ Static fields/initializers ---------------------------------------------

    protected static final Logger LOG = Logger.getLogger(AlkisPunktReportScriptlet.class);

    public static final String[] SUFFIXES = new String[] { "tif", "jpg", "tiff", "jpeg" };

    private static final transient SimpleHttpAccessHandler EXTENDED_ACCESS_HANDLER = new SimpleHttpAccessHandler();

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param   pointcode  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static Boolean isImageAvailable(final String pointcode) {
        return isImageAvailable(pointcode, EXTENDED_ACCESS_HANDLER);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   pointcode              DOCUMENT ME!
     * @param   extendedAccessHandler  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static Boolean isImageAvailable(final String pointcode, final ExtendedAccessHandler extendedAccessHandler) {
        final Collection<String> validDocuments = ServerAlkisProducts.getInstance()
                    .getCorrespondingPointDocuments(pointcode);

        for (final String document : validDocuments) {
            final URL url;
            try {
                url = ServerAlkisConf.getInstance().getDownloadUrlForDocument(document);
                if (extendedAccessHandler.checkIfURLaccessible(url)) {
                    return true;
                }
            } catch (final Exception ex) {
            }
        }

        return Boolean.FALSE;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   pointcode  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static Image loadImage(final String pointcode) {
        return loadImage(pointcode, EXTENDED_ACCESS_HANDLER);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   pointcode              DOCUMENT ME!
     * @param   extendedAccessHandler  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static Image loadImage(final String pointcode, final ExtendedAccessHandler extendedAccessHandler) {
        final Collection<String> validDocuments = ServerAlkisProducts.getInstance()
                    .getCorrespondingPointDocuments(pointcode);
        String suffix = "";

        InputStream streamToReadFrom = null;
        for (final String document : validDocuments) {
            try {
                final URL url = ServerAlkisConf.getInstance().getDownloadUrlForDocument(document);
                if (extendedAccessHandler.checkIfURLaccessible(url)) {
                    streamToReadFrom = extendedAccessHandler.doRequest(url);
                    suffix = url.toExternalForm().substring(url.toExternalForm().lastIndexOf('.'));
                    if (streamToReadFrom != null) {
                        break;
                    }
                }
            } catch (final Exception ex) {
                LOG.warn("An exception occurred while opening URL '" + document
                            + "'. Skipping this url.",
                    ex);
            }
        }

        BufferedImage result = null;
        try {
            if (streamToReadFrom == null) {
                LOG.error("Couldn't get a connection to associated ap map.");
                return result;
            }

            if (suffix.endsWith("tif") || suffix.endsWith("tiff") || suffix.endsWith("TIF")
                        || suffix.endsWith("TIFF")) {
                final TIFFDecodeParam param = null;
                final ImageDecoder decoder = ImageCodec.createImageDecoder("tiff", streamToReadFrom, param);
                final RenderedImage image = decoder.decodeAsRenderedImage();
                final RenderedImageAdapter imageAdapter = new RenderedImageAdapter(image);
                result = imageAdapter.getAsBufferedImage();
            } else {
                result = ImageIO.read(streamToReadFrom);
            }
        } catch (IOException ex) {
            LOG.warn("Could not read image.", ex);
            return result;
        } finally {
            try {
                if (streamToReadFrom != null) {
                    streamToReadFrom.close();
                }
            } catch (IOException ex) {
                LOG.warn("Couldn't close the stream.", ex);
            }
        }

        return result;
    }
}
