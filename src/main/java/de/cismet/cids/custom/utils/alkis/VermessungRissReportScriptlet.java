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

    private final ExtendedAccessHandler extendedAccessHandler = new SimpleHttpAccessHandler();
    
    //~ Methods ----------------------------------------------------------------

    public Boolean isImageAvailable(final String host,
            final String schluessel,
            final Integer gemarkung,
            final String flur,
            final String blatt) {
        return isImageAvailable(host, schluessel, gemarkung, flur, blatt, extendedAccessHandler);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   host        DOCUMENT ME!
     * @param   schluessel  DOCUMENT ME!
     * @param   gemarkung   DOCUMENT ME!
     * @param   flur        DOCUMENT ME!
     * @param   blatt       DOCUMENT ME!
     * @param extendedAccessHandler
     *
     * @return  DOCUMENT ME!
     */
    public Boolean isImageAvailable(final String host,
            final String schluessel,
            final Integer gemarkung,
            final String flur,
            final String blatt,
            final ExtendedAccessHandler extendedAccessHandler) {
        final List<URL> validURLs;
        if (host.equals(AlkisConstants.COMMONS.VERMESSUNG_HOST_GRENZNIEDERSCHRIFTEN)) {
            validURLs = VermessungsrissPictureFinder.getInstance()
                        .findGrenzniederschriftPicture(schluessel, gemarkung, flur, blatt);
        } else {
            validURLs = VermessungsrissPictureFinder.getInstance()
                        .findVermessungsrissPicture(schluessel, gemarkung, flur, blatt);
        }

        boolean imageAvailable = false;
        for (final URL urls : validURLs) {
            final URL url = urls;
            if (extendedAccessHandler.checkIfURLaccessible(url)) {
                imageAvailable = true;
                break;
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
    public BufferedImage rotate(final BufferedImage imageToRotate) {
        BufferedImage result = imageToRotate;

        if (imageToRotate == null) {
            return result;
        }

        if ((imageToRotate instanceof BufferedImage) && (imageToRotate.getWidth() > imageToRotate.getHeight())) {
            result = Static2DTools.rotate(imageToRotate, 90D, false, Color.white);
        }

        return result;
    }

    public static VermessungRissReportScriptlet getInstance() {
        return LazyInitialiser.INSTANCE;
    }
    
    //~ Inner Classes ----------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    protected static final class LazyInitialiser {

        //~ Static fields/initializers -----------------------------------------

        private static final VermessungRissReportScriptlet INSTANCE = new VermessungRissReportScriptlet();

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new LazyInitialiser object.
         */
        private LazyInitialiser() {
        }
    }
}
