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

import java.util.Collection;

import de.cismet.tools.Static2DTools;

/**
 * DOCUMENT ME!
 *
 * @author   jweintraut
 * @version  $Revision$, $Date$
 */
public class BaulastenReportScriptlet extends JRDefaultScriptlet {

    //~ Static fields/initializers ---------------------------------------------

    protected static final Logger LOG = Logger.getLogger(BaulastenReportScriptlet.class);

    //~ Methods ----------------------------------------------------------------

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

    /**
     * DOCUMENT ME!
     *
     * @param   propertyValue  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static String propertyPrettyPrint(final Object propertyValue) {
        if (propertyValue instanceof Collection) {
            final Collection beanCollection = (Collection)propertyValue;
            final StringBuilder resultSB = new StringBuilder();
            for (final Object bean : beanCollection) {
                if (resultSB.length() != 0) {
                    resultSB.append(", ");
                }
                resultSB.append(String.valueOf(bean));
            }
            return resultSB.toString();
        } else if (propertyValue != null) {
            return propertyValue.toString();
        } else {
            return "-";
        }
    }
}
