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

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;

import java.io.IOException;
import java.io.InputStream;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.Collection;
import java.util.LinkedList;

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

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new AlkisPunktReportScriptlet object.
     */
    private AlkisPunktReportScriptlet() {
    }

    //~ Methods ----------------------------------------------------------------

    private AlkisPunktReportScriptlet() {
        
    }
    
    /**
     * DOCUMENT ME!
     *
     * @param   pointcode  dgkBlattnummer the value of dgkBlattnummer
     *
     * @return  DOCUMENT ME!
     */
    public Collection<URL> getCorrespondingURLs(final String pointcode) {
        final Collection<URL> validURLs = new LinkedList<URL>();

        // The pointcode of a alkis point has a specific format:
        // 25xx56xx1xxxxx
        // ^  ^
        // |  Part 2 of the "Kilometerquadrat"
        // Part 1 of the "Kilometerquadrat"
        if ((pointcode == null) || (pointcode.trim().length() < 9) || (pointcode.trim().length() > 15)) {
            return validURLs;
        }

        final StringBuilder urlBuilder;
        if (pointcode.trim().length() < 15) {
            urlBuilder = new StringBuilder(AlkisConstants.COMMONS.APMAPS_HOST);

            final String kilometerquadratPart1 = pointcode.substring(2, 4);
            final String kilometerquadratPart2 = pointcode.substring(6, 8);

            urlBuilder.append('/');
            urlBuilder.append(kilometerquadratPart1);
            urlBuilder.append(kilometerquadratPart2);
            urlBuilder.append('/');
            urlBuilder.append(AlkisConstants.COMMONS.APMAPS_PREFIX);
            urlBuilder.append(pointcode);
            urlBuilder.append('.');
        } else {
            urlBuilder = new StringBuilder(AlkisConstants.COMMONS.APMAPS_ETRS_HOST);
            urlBuilder.append('/');
            urlBuilder.append(AlkisConstants.COMMONS.APMAPS_PREFIX);
            urlBuilder.append(pointcode);
            urlBuilder.append('.');
        }
        for (final String suffix : SUFFIXES) {
            URL urlToTry = null;
            try {
                urlToTry = new URL(urlBuilder.toString() + suffix);
            } catch (MalformedURLException ex) {
                LOG.warn("The URL '" + urlBuilder.toString() + suffix
                            + "' is malformed. Can't load the corresponding picture.",
                    ex);
            }

            if (urlToTry != null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Valid URL: " + urlToTry.toExternalForm());
                }

                validURLs.add(urlToTry);
            }
        }
        return validURLs;
    }

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
        final Collection<URL> validURLs = getCorrespondingURLs(pointcode);

        for (final URL url : validURLs) {
            if (extendedAccessHandler.checkIfURLaccessible(url)) {
                return true;
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
        final Collection<URL> validURLs = getCorrespondingURLs(pointcode);
        String suffix = "";

        InputStream streamToReadFrom = null;
        for (final URL url : validURLs) {
            try {
                if (extendedAccessHandler.checkIfURLaccessible(url)) {
                    streamToReadFrom = extendedAccessHandler.doRequest(url);
                    suffix = url.toExternalForm().substring(url.toExternalForm().lastIndexOf('.'));
                    if (streamToReadFrom != null) {
                        break;
                    }
                }
            } catch (final Exception ex) {
                LOG.warn("An exception occurred while opening URL '" + url.toExternalForm()
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

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static AlkisPunktReportScriptlet getInstance() {
        return LazyInitialiser.INSTANCE;
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    private static final class LazyInitialiser {

        //~ Static fields/initializers -----------------------------------------

        private static final AlkisPunktReportScriptlet INSTANCE = new AlkisPunktReportScriptlet();

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new LazyInitialiser object.
         */
        private LazyInitialiser() {
        }
    }
}
