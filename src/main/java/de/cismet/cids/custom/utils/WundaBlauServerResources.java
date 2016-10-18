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
package de.cismet.cids.custom.utils;

import de.cismet.cids.utils.serverresources.CachedServerResourcesLoader;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public enum WundaBlauServerResources {

    //~ Enum constants ---------------------------------------------------------

    VERMESSUNGSRISSE_JASPER(
        "/de/cismet/cids/custom/wunda_blau/res/reports/vermessungsrisse.jasper",
        CachedServerResourcesLoader.Type.JASPER_REPORT),
    AP_MAPS_JASPER(
        "/de/cismet/cids/custom/wunda_blau/res/reports/apmaps.jasper",
        CachedServerResourcesLoader.Type.JASPER_REPORT),
    FS_RECHNUNG_JASPER(
        "/de/cismet/cids/custom/wunda_blau/res/bestellung_rechnung.jasper",
        CachedServerResourcesLoader.Type.JASPER_REPORT),

    BUTLER_PROPERTIES("/de/cismet/cids/custom/utils/butler/butler.properties", CachedServerResourcesLoader.Type.TEXT),
    NAS_SERVER_PROPERTIES(
        "/de/cismet/cids/custom/utils/nas/nasServer_conf.properties",
        CachedServerResourcesLoader.Type.TEXT),
    PNR_PROPERTIES(
        "/de/cismet/cids/custom/utils/pointnumberreservation/pointNumberRes_conf.properties",
        CachedServerResourcesLoader.Type.TEXT),
    NAS_PRODUCT_DESCRIPTION_JSON(
        "/de/cismet/cids/custom/nas/nasProductDescription.json",
        CachedServerResourcesLoader.Type.TEXT),
    FS_TEST_XML(
        "/de/cismet/cids/custom/wunda_blau/res/formsolutions/TEST_CISMET00.xml",
        CachedServerResourcesLoader.Type.TEXT),
    FS_IGNORE_TRANSID_TXT(
        "/de/cismet/cids/custom/wunda_blau/res/formsolutions/ignoreTransids.txt",
        CachedServerResourcesLoader.Type.TEXT),
    FME_DB_CONN_PROPERTIES(
        "/de/cismet/cids/custom/wunda_blau/search/actions/fme_db_conn.properties",
        CachedServerResourcesLoader.Type.TEXT),
    TIFFER_ACTION_CFG(
        "/de/cismet/cids/custom/wunda_blau/search/actions/tifferAction.cfg",
        CachedServerResourcesLoader.Type.TEXT),
    ALKIS_CONF(
        "/de/cismet/cids/custom/wunda_blau/res/alkis/alkis_conf.properties",
        CachedServerResourcesLoader.Type.TEXT),
    ALKIS_PRODUCTS(
        "/de/cismet/cids/custom/wunda_blau/res/alkis/alkis_products.properties",
        CachedServerResourcesLoader.Type.TEXT),
    ALKIS_FORMATS(
        "/de/cismet/cids/custom/wunda_blau/res/alkis/formats.properties",
        CachedServerResourcesLoader.Type.TEXT),
    ALKIS_PRODUKTBESCHREIBUNG_XML(
        "/de/cismet/cids/custom/wunda_blau/res/alkis/Produktbeschreibung_ALKIS.xml",
        CachedServerResourcesLoader.Type.TEXT),
    BERECHTIGUNGSPRUEFUNG_PROPERTIES(
        "/de/cismet/cids/custom/berechtigungspruefung/berechtigungspruefung.properties",
        CachedServerResourcesLoader.Type.TEXT),
    FORMSOLUTIONS_PROPERTIES(
        "/de/cismet/cids/custom/wunda_blau/res/formsolutions/fs_conf.properties",
        CachedServerResourcesLoader.Type.TEXT),
    VERMESSUNGSUNTERLAGENPORTAL_PROPERTIES(
        "/de/cismet/cids/custom/wunda_blau/res/vermessungsunterlagenportal/vup_conf.properties",
        CachedServerResourcesLoader.Type.TEXT),
    MOTD_WUNDA_BLAU_PROPERTIES(
        "/de/cismet/cids/custom/motd/wunda_blau.properties",
        CachedServerResourcesLoader.Type.TEXT),
    MOTD_VERDIS_GRUNDIS_PROPERTIES(
        "/de/cismet/cids/custom/motd/verdis_grundis.properties",
        CachedServerResourcesLoader.Type.TEXT),
    MOTD_LAGIS_PROPERTIES("/de/cismet/cids/custom/motd/lagis.properties", CachedServerResourcesLoader.Type.TEXT),
    MOTD_BELIS2_PROPERTIES("/de/cismet/cids/custom/motd/belis2.properties", CachedServerResourcesLoader.Type.TEXT),

    IMAGE_ANNOTATOR_FONT(
        "/de/cismet/cids/custom/wunda_blau/search/actions/Calibri_Bold.ttf",
        CachedServerResourcesLoader.Type.TRUETYPE_FONT);

    //~ Instance fields --------------------------------------------------------

    private final String value;
    private final CachedServerResourcesLoader.Type type;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new Props object.
     *
     * @param  value  DOCUMENT ME!
     * @param  type   DOCUMENT ME!
     */
    WundaBlauServerResources(final String value, final CachedServerResourcesLoader.Type type) {
        this.value = value;
        this.type = type;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getValue() {
        return value;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public CachedServerResourcesLoader.Type getType() {
        return type;
    }
}
