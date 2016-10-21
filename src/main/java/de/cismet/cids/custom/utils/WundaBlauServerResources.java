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

import de.cismet.cids.utils.serverresources.ServerResourcesLoader;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public enum WundaBlauServerResources {

    //~ Enum constants ---------------------------------------------------------

    VERMESSUNGSRISSE_JASPER("/reports/vermessungsrisse.jasper", ServerResourcesLoader.Type.JASPER_REPORT),
    VERMESSUNGSRISSE_DOCUMENT_JASPER(
        "/reports/vermessungsrisse_document.jasper",
        ServerResourcesLoader.Type.JASPER_REPORT),
    VERMESSUNGSRISSE_OVERVIEW_JASPER(
        "/reports/vermessungsrisse_overview.jasper",
        ServerResourcesLoader.Type.JASPER_REPORT),
    APMAPS_JASPER("/reports/apmaps.jasper", ServerResourcesLoader.Type.JASPER_REPORT),
    APMAPS_DOCUMENT_JASPER("/reports/apmaps_document.jasper", ServerResourcesLoader.Type.JASPER_REPORT),
    APMAPS_OVERVIEW_JASPER("/reports/apmaps_overview.jasper", ServerResourcesLoader.Type.JASPER_REPORT),
    FS_RECHNUNG_JASPER("/reports/bestellung_rechnung.jasper", ServerResourcesLoader.Type.JASPER_REPORT),
    FS_RECHNUNG_RUECKSEITE_JASPER(
        "/reports/bestellung_rechnung_rueckseite.jasper",
        ServerResourcesLoader.Type.JASPER_REPORT),

    BUTLER_PROPERTIES("/butler/butler.properties", ServerResourcesLoader.Type.TEXT),

    NAS_SERVER_PROPERTIES("/nas/nasServer_conf.properties", ServerResourcesLoader.Type.TEXT),
    FME_DB_CONN_PROPERTIES("/nas/fme_db_conn.properties", ServerResourcesLoader.Type.TEXT),
    NAS_PRODUCT_DESCRIPTION_JSON("/nas/nasProductDescription.json", ServerResourcesLoader.Type.TEXT),

    PNR_PROPERTIES("/pointnumberreservation/pointNumberRes_conf.properties", ServerResourcesLoader.Type.TEXT),

    FS_TEST_XML("/formsolutions/TEST_CISMET00.xml", ServerResourcesLoader.Type.TEXT),
    FS_IGNORE_TRANSID_TXT("/formsolutions/ignoreTransids.txt", ServerResourcesLoader.Type.TEXT),
    FORMSOLUTIONS_PROPERTIES("/formsolutions/fs_conf.properties", ServerResourcesLoader.Type.TEXT),

    TIFFER_ACTION_CFG("/tiffer/tifferAction.cfg", ServerResourcesLoader.Type.TEXT),
    IMAGE_ANNOTATOR_FONT("/tiffer/Calibri_Bold.ttf", ServerResourcesLoader.Type.BINARY),

    ALKIS_CONF("/alkis/alkis_conf.properties", ServerResourcesLoader.Type.TEXT),
    ALKIS_PRODUCTS_PROPERTIES("/alkis/alkis_products.properties", ServerResourcesLoader.Type.TEXT),
    ALKIS_BUCHUNTSBLATTBEZIRKE_JSON("/alkis/buchungsblattbezirke.json", ServerResourcesLoader.Type.TEXT),
    ALKIS_FORMATS_PROPERTIES("/alkis/formats.properties", ServerResourcesLoader.Type.TEXT),
    ALKIS_PRODUKTBESCHREIBUNG_XML("/alkis/Produktbeschreibung_ALKIS.xml", ServerResourcesLoader.Type.TEXT),

    BERECHTIGUNGSPRUEFUNG_PROPERTIES(
        "/berechtigungspruefung/berechtigungspruefung.properties",
        ServerResourcesLoader.Type.TEXT),
    BERECHTIGUNGSPRUEFUNG_CONF_JSON(
        "/berechtigungspruefung/berechtigungspruefung_conf.json",
        ServerResourcesLoader.Type.TEXT),

    VERMESSUNGSUNTERLAGENPORTAL_PROPERTIES(
        "/vermessungsunterlagenportal/vup_conf.properties",
        ServerResourcesLoader.Type.TEXT),

    MOTD_WUNDA_BLAU_PROPERTIES("/motd/wunda_blau.properties", ServerResourcesLoader.Type.TEXT),
    MOTD_VERDIS_GRUNDIS_PROPERTIES("/motd/verdis_grundis.properties", ServerResourcesLoader.Type.TEXT),
    MOTD_LAGIS_PROPERTIES("/motd/lagis.properties", ServerResourcesLoader.Type.TEXT),
    MOTD_BELIS2_PROPERTIES("/motd/belis2.properties", ServerResourcesLoader.Type.TEXT),

    PNR_TEMPLATE_BEN_AUFTR_ALL("/pointnumberreservation/A_Ben_Auftr_alle_PKZ.xml", ServerResourcesLoader.Type.TEXT),
    PNR_TEMPLATE_BEN_AUFTR_ONE_ANR("/pointnumberreservation/A_Ben_Auftr_eine_ANR.xml", ServerResourcesLoader.Type.TEXT),
    PNR_TEMPLATE_BEN_AUFTR_WILDCARD(
        "/pointnumberreservation/A_Ben_Auftr_ANR_Praefix_Wildcard.xml",
        ServerResourcesLoader.Type.TEXT),
    PNR_TEMPLATE_FREIGABE("/pointnumberreservation/A_Freigabe.xml", ServerResourcesLoader.Type.TEXT),
    PNR_TEMPLATE_PROLONG("/pointnumberreservation/A_Verlaengern.xml", ServerResourcesLoader.Type.TEXT),
    PNR_TEMPLATE_PROLONG_SUB("/pointnumberreservation/A_Verlaengern__Sub.xml", ServerResourcesLoader.Type.TEXT),
    PNR_TEMPLATE_RESERVIERUNG("/pointnumberreservation/A_reservierung.xml", ServerResourcesLoader.Type.TEXT),
    PNR_TEMPLATE_RESERVIERUNG_SW(
        "/pointnumberreservation/A_reservierung_startwert.xml",
        ServerResourcesLoader.Type.TEXT);

    //~ Instance fields --------------------------------------------------------

    private final String value;
    private final ServerResourcesLoader.Type type;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new Props object.
     *
     * @param  value  DOCUMENT ME!
     * @param  type   DOCUMENT ME!
     */
    WundaBlauServerResources(final String value, final ServerResourcesLoader.Type type) {
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
    public ServerResourcesLoader.Type getType() {
        return type;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public Object loadServerResources() throws Exception {
        final ServerResourcesLoader loader = ServerResourcesLoader.getInstance();
        switch (type) {
            case JASPER_REPORT: {
                return loader.loadJasperReportResource(value);
            }
            case TEXT: {
                return loader.loadTextResource(value);
            }
            case BINARY: {
                return loader.loadBinaryResource(value);
            }
            default: {
                throw new Exception("unknown serverResource type");
            }
        }
    }
}
