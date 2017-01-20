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
package de.cismet.cids.custom.wunda_blau.cidslayer;

import Sirius.server.middleware.types.MetaClass;
import Sirius.server.newuser.User;

/**
 * DOCUMENT ME!
 *
 * @author   therter
 * @version  $Revision$, $Date$
 */
public class WohnlageCidsLayer extends DefaultCidsLayer {

    //~ Static fields/initializers ---------------------------------------------

    private static final String QUERY = ""
                + "SELECT "
                + "wohnlage.ID, wohnlage.laufende_nummer, "
                + "CASE WHEN wohnlage_kategorie.ID IS NULL THEN default_kategorie.ID ELSE wohnlage_kategorie.farbcode END AS kategorie_id, "
                + "CASE WHEN wohnlage_kategorie.ID IS NULL THEN default_kategorie.farbcode ELSE wohnlage_kategorie.farbcode END AS farbcode, "
                + "st_asBinary(geom.geo_field) AS geometrie "
                + "from wohnlage JOIN geom ON (fk_geometrie = geom.id) "
                + "LEFT JOIN wohnlage_kategorisierung ON wohnlage_kategorisierung.fk_wohnlage = wohnlage.ID AND wohnlage_kategorisierung.login_name = '#LOGIN_NAME#'"
                + "LEFT JOIN wohnlage_kategorie ON wohnlage_kategorisierung.fk_kategorie = wohnlage_kategorie.ID "
                + "LEFT JOIN wohnlage_kategorie AS default_kategorie ON default_kategorie.schluessel = 'keine'";

    private static final String[] NAMES = new String[] {
            "id",
            "laufende_nummer",
            "kategorie_id",
            "farbcode",
            "geometrie"
        };

    private static final String[] PROPERTY_NAMES = new String[] {
            "ID",
            "LaufendeNummer",
            "KategorieID",
            "Farbcode",
            "Geometrie"
        };

    private static final String[] TYPES = new String[] {
            "java.lang.Integer",
            "java.lang.Integer",
            "java.lang.Integer",
            "java.lang.Integer",
            "Geometry"
        };

    //~ Instance fields --------------------------------------------------------

    private final User user;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new WohnlageCidsLayer object.
     *
     * @param  mc    DOCUMENT ME!
     * @param  user  DOCUMENT ME!
     */
    public WohnlageCidsLayer(final MetaClass mc, final User user) {
        super(mc);
        this.user = user;
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public String getSelectString() {
        return QUERY.replaceAll("#LOGIN_NAME#", user.getName());
    }

    @Override
    public String[] getColumnNames() {
        return NAMES;
    }

    @Override
    public String[] getColumnPropertyNames() {
        return PROPERTY_NAMES;
    }

    @Override
    public String[] getPrimitiveColumnTypes() {
        return TYPES;
    }

    @Override
    public String getGeoField() {
        return "geometrie";
    }

    @Override
    public String getSqlGeoField() {
        return "geo_field";
    }
}
