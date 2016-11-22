/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.utils.alkis;

import net.sf.jasperreports.engine.JRDefaultScriptlet;

import org.apache.log4j.Logger;

import java.awt.Image;
import java.awt.image.BufferedImage;

import java.io.IOException;
import java.io.InputStream;

import java.net.URL;

import java.util.Collection;

import javax.imageio.ImageIO;

import de.cismet.commons.security.handler.SimpleHttpAccessHandler;

/**
 * DOCUMENT ME!
 *
 * @author   jweintraut
 * @version  $Revision$, $Date$
 */
public class NivellementPunktReportScriptlet extends JRDefaultScriptlet {

    //~ Static fields/initializers ---------------------------------------------

    protected static final Logger LOG = Logger.getLogger(NivellementPunktReportScriptlet.class);
    private static final transient SimpleHttpAccessHandler EXTENDED_ACCESS_HANDLER = new SimpleHttpAccessHandler();

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param   dgkBlattnummer  DOCUMENT ME!
     * @param   laufendeNummer  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Boolean isImageAvailable(final String dgkBlattnummer, final String laufendeNummer) {
        final Collection<URL> validURLs = ServerAlkisProducts.getInstance()
                    .getCorrespondingNivPURLs(dgkBlattnummer, laufendeNummer);

        for (final URL url : validURLs) {
            if (EXTENDED_ACCESS_HANDLER.checkIfURLaccessible(url)) {
                return true;
            }
        }

        return Boolean.FALSE;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   dgkBlattnummer  DOCUMENT ME!
     * @param   laufendeNummer  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Image loadImage(final String dgkBlattnummer, final String laufendeNummer) {
        final Collection<URL> validURLs = ServerAlkisProducts.getInstance()
                    .getCorrespondingNivPURLs(dgkBlattnummer, laufendeNummer);

        InputStream streamToReadFrom = null;
        for (final URL url : validURLs) {
            try {
                if (EXTENDED_ACCESS_HANDLER.checkIfURLaccessible(url)) {
                    streamToReadFrom = EXTENDED_ACCESS_HANDLER.doRequest(url);
                    if (streamToReadFrom != null) {
                        break;
                    }
                }
            } catch (final Exception ex) {
                LOG.warn("An exception occurred while opening URL '" + url.toExternalForm() + "'. Skipping this url.",
                    ex);
            }
        }

        BufferedImage result = null;
        if (streamToReadFrom == null) {
            LOG.error("Couldn't get a connection to associated document.");
            return result;
        }

        try {
            result = ImageIO.read(streamToReadFrom);
        } catch (final IOException ex) {
            LOG.warn("Could not read image.", ex);
            return result;
        } finally {
            try {
                streamToReadFrom.close();
            } catch (IOException ex) {
                LOG.warn("Couldn't close the stream.", ex);
            }
        }

        return result;
    }
}
