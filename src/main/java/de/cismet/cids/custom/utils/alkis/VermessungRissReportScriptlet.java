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

import java.awt.Color;
import java.awt.image.BufferedImage;

import java.net.URL;

import java.util.List;

import de.cismet.commons.security.handler.ExtendedAccessHandler;
import de.cismet.commons.security.handler.SimpleHttpAccessHandler;

import de.cismet.tools.Static2DTools;

/**
 * DOCUMENT ME!
 *
 * @author   jweintraut
 * @version  $Revision$, $Date$
 */
public class VermessungRissReportScriptlet extends JRDefaultScriptlet {

    //~ Static fields/initializers ---------------------------------------------

    protected static final Logger LOG = Logger.getLogger(VermessungRissReportScriptlet.class);

    private static final ExtendedAccessHandler EXTENDED_ACCESS_HANDLER = new SimpleHttpAccessHandler();

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param   host        DOCUMENT ME!
     * @param   schluessel  DOCUMENT ME!
     * @param   gemarkung   DOCUMENT ME!
     * @param   flur        DOCUMENT ME!
     * @param   blatt       DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static Boolean isImageAvailable(final String host,
            final String schluessel,
            final Integer gemarkung,
            final String flur,
            final String blatt) {
        return isImageAvailable(host, schluessel, gemarkung, flur, blatt, EXTENDED_ACCESS_HANDLER);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   host                   DOCUMENT ME!
     * @param   schluessel             DOCUMENT ME!
     * @param   gemarkung              DOCUMENT ME!
     * @param   flur                   DOCUMENT ME!
     * @param   blatt                  DOCUMENT ME!
     * @param   extendedAccessHandler  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static Boolean isImageAvailable(final String host,
            final String schluessel,
            final Integer gemarkung,
            final String flur,
            final String blatt,
            final ExtendedAccessHandler extendedAccessHandler) {
        final List<String> validDocuments;
        if (host.equals(ServerAlkisConf.getInstance().getVermessungHostGrenzniederschriften())) {
            validDocuments = VermessungsrissPictureFinder.getInstance()
                        .findGrenzniederschriftPicture(schluessel, gemarkung, flur, blatt);
        } else {
            validDocuments = VermessungsrissPictureFinder.getInstance()
                        .findVermessungsrissPicture(schluessel, gemarkung, flur, blatt);
        }

        boolean imageAvailable = false;
        for (final String document : validDocuments) {
            try {
                final URL url = ServerAlkisConf.getInstance().getDownloadUrlForDocument(document);
                if (extendedAccessHandler.checkIfURLaccessible(url)) {
                    imageAvailable = true;
                    break;
                }
            } catch (final Exception ex) {
                LOG.error(ex, ex);
            }
        }
        return imageAvailable;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   imageToRotate  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static BufferedImage rotate(final BufferedImage imageToRotate) {
        BufferedImage result = imageToRotate;

        if (imageToRotate == null) {
            return result;
        }

        if ((imageToRotate instanceof BufferedImage) && (imageToRotate.getWidth() > imageToRotate.getHeight())) {
            result = Static2DTools.rotate(imageToRotate, 90D, false, Color.white);
        }

        return result;
    }
}
