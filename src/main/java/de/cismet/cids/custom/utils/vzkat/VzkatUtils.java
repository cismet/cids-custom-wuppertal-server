/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cismet.cids.custom.utils.vzkat;

import de.cismet.cids.dynamics.CidsBean;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class VzkatUtils {

    //~ Static fields/initializers ---------------------------------------------

    public static final String ZEICHEN_TOSTRING_QUERY = ""
                + "SELECT "
                + " vzkat_zeichen.*, "
                + " CASE WHEN vzkat_stvo.id IS NOT NULL THEN vzkat_stvo.name ELSE '?' END AS vzkat_stvo__name "
                + "FROM "
                + " vzkat_zeichen "
                + " LEFT JOIN vzkat_stvo ON vzkat_zeichen.fk_stvo = vzkat_stvo.id "
                + "%s "
                + "ORDER BY schluessel ASC";
    public static final String ZEICHEN_TOSTRING_TEMPLATE = "%s (%s) - %s";
    public static final String[] ZEICHEN_TOSTRING_FIELDS = { "schluessel", "vzkat_stvo__name", "name" };

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param   cidsBean  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static String createZeichenToString(final CidsBean cidsBean) {
        final String name = (String)cidsBean.getProperty("name");
        final String schluessel = (String)cidsBean.getProperty("schluessel");
        final String stvoName = (String)cidsBean.getProperty("fk_stvo.name");
        return String.format("%s (%s) - %s", schluessel, (stvoName != null) ? stvoName : "?", name);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   cidsBean  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static String createSchildToString(final CidsBean cidsBean) {
        final String standort = createStandortToString((CidsBean)cidsBean.getProperty("fk_standort"));
        final String richtung = (String)cidsBean.getProperty("fk_richtung.name");
        final Integer position = (Integer)cidsBean.getProperty("reihenfolge");
        final String zeichenSchluessel = (String)cidsBean.getProperty("fk_zeichen.schluessel");
        final String stvoName = (String)cidsBean.getProperty("fk_zeichen.fk_stvo.name");

        return String.format("%s (%s), %s, %s %s", zeichenSchluessel, stvoName, standort, richtung, position);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   cidsBean  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static String createStandortToString(final CidsBean cidsBean) {
        final String importId = "Standort " + String.valueOf(cidsBean.getProperty("import_id"));
        return importId;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   cidsBean  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static String createZeichenKey(final CidsBean cidsBean) {
        if (cidsBean != null) {
            return String.format(
                    "%s_%s",
                    (String)cidsBean.getProperty("fk_stvo.schluessel"),
                    (String)cidsBean.getProperty("schluessel"));
        } else {
            return null;
        }
    }
}
