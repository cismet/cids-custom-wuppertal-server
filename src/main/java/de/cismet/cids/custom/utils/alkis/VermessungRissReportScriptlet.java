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

import de.cismet.commons.security.handler.ExtendedAccessHandler;
import de.cismet.commons.security.handler.SimpleHttpAccessHandler;

import de.cismet.connectioncontext.AbstractConnectionContext;
import de.cismet.connectioncontext.ConnectionContext;

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
        final String validDocument;
        final VermessungsrissPictureFinder finder = new VermessungsrissPictureFinder(
                null,
                null,
                ConnectionContext.create(
                    AbstractConnectionContext.Category.STATIC,
                    VermessungRissReportScriptlet.class.getSimpleName()));
        if (host.equals(ServerAlkisConf.getInstance().getVermessungHostGrenzniederschriften())) {
            validDocument = finder.findGrenzniederschriftPicture(schluessel, gemarkung, flur, blatt);
        } else {
            validDocument = finder.findVermessungsrissPicture(schluessel, gemarkung, flur, blatt);
        }
        return validDocument != null;
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
