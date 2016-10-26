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

import lombok.Getter;

import de.cismet.cids.utils.serverresources.BinaryServerResource;
import de.cismet.cids.utils.serverresources.JasperReportServerResource;
import de.cismet.cids.utils.serverresources.ServerResource;
import de.cismet.cids.utils.serverresources.TextServerResource;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public enum WundaBlauServerResources {

    //~ Enum constants ---------------------------------------------------------

    VERMESSUNGSRISSE_JASPER(new JasperReportServerResource("/reports/vermessungsrisse.jasper")),
    VERMESSUNGSRISSE_DOCUMENT_JASPER(new JasperReportServerResource("/reports/vermessungsrisse_document.jasper")),
    VERMESSUNGSRISSE_OVERVIEW_JASPER(new JasperReportServerResource("/reports/vermessungsrisse_overview.jasper")),
    APMAPS_JASPER(new JasperReportServerResource("/reports/apmaps.jasper")),
    APMAPS_DOCUMENT_JASPER(new JasperReportServerResource("/reports/apmaps_document.jasper")),
    APMAPS_OVERVIEW_JASPER(new JasperReportServerResource("/reports/apmaps_overview.jasper")),
    FS_RECHNUNG_JASPER(new JasperReportServerResource("/reports/bestellung_rechnung.jasper")),
    FS_RECHNUNG_RUECKSEITE_JASPER(new JasperReportServerResource("/reports/bestellung_rechnung_rueckseite.jasper")),

    BUTLER_PROPERTIES(new TextServerResource("/butler/butler.properties")),

    NAS_SERVER_PROPERTIES(new TextServerResource("/nas/nasServer_conf.properties")),
    FME_DB_CONN_PROPERTIES(new TextServerResource("/nas/fme_db_conn.properties")),
    NAS_PRODUCT_DESCRIPTION_JSON(new TextServerResource("/nas/nasProductDescription.json")),

    PNR_PROPERTIES(new TextServerResource("/pointnumberreservation/pointNumberRes_conf.properties")),

    FS_TEST_XML(new TextServerResource("/formsolutions/TEST_CISMET00.xml")),
    FS_IGNORE_TRANSID_TXT(new TextServerResource("/formsolutions/ignoreTransids.txt")),
    FORMSOLUTIONS_PROPERTIES(new TextServerResource("/formsolutions/fs_conf.properties")),

    TIFFER_ACTION_CFG(new TextServerResource("/tiffer/tifferAction.cfg")),
    IMAGE_ANNOTATOR_FONT(new BinaryServerResource("/tiffer/Calibri_Bold.ttf")),

    ALKIS_CONF(new TextServerResource("/alkis/alkis_conf.properties")),
    ALKIS_PRODUCTS_PROPERTIES(new TextServerResource("/alkis/alkis_products.properties")),
    ALKIS_BUCHUNTSBLATTBEZIRKE_JSON(new TextServerResource("/alkis/buchungsblattbezirke.json")),
    ALKIS_FORMATS_PROPERTIES(new TextServerResource("/alkis/formats.properties")),
    ALKIS_PRODUKTBESCHREIBUNG_XML(new TextServerResource("/alkis/Produktbeschreibung_ALKIS.xml")),

    BERECHTIGUNGSPRUEFUNG_PROPERTIES(new TextServerResource("/berechtigungspruefung/berechtigungspruefung.properties")),
    BERECHTIGUNGSPRUEFUNG_CONF_JSON(new TextServerResource("/berechtigungspruefung/berechtigungspruefung_conf.json")),

    VERMESSUNGSUNTERLAGENPORTAL_PROPERTIES(new TextServerResource("/vermessungsunterlagenportal/vup_conf.properties")),

    MOTD_WUNDA_BLAU_PROPERTIES(new TextServerResource("/motd/wunda_blau.properties")),
    MOTD_VERDIS_GRUNDIS_PROPERTIES(new TextServerResource("/motd/verdis_grundis.properties")),
    MOTD_LAGIS_PROPERTIES(new TextServerResource("/motd/lagis.properties")),
    MOTD_BELIS2_PROPERTIES(new TextServerResource("/motd/belis2.properties")),

    PNR_TEMPLATE_BEN_AUFTR_ALL(new TextServerResource("/pointnumberreservation/A_Ben_Auftr_alle_PKZ.xml")),
    PNR_TEMPLATE_BEN_AUFTR_ONE_ANR(new TextServerResource("/pointnumberreservation/A_Ben_Auftr_eine_ANR.xml")),
    PNR_TEMPLATE_BEN_AUFTR_WILDCARD(new TextServerResource(
            "/pointnumberreservation/A_Ben_Auftr_ANR_Praefix_Wildcard.xml")),
    PNR_TEMPLATE_FREIGABE(new TextServerResource("/pointnumberreservation/A_Freigabe.xml")),
    PNR_TEMPLATE_PROLONG(new TextServerResource("/pointnumberreservation/A_Verlaengern.xml")),
    PNR_TEMPLATE_PROLONG_SUB(new TextServerResource("/pointnumberreservation/A_Verlaengern__Sub.xml")),
    PNR_TEMPLATE_RESERVIERUNG(new TextServerResource("/pointnumberreservation/A_reservierung.xml")),
    PNR_TEMPLATE_RESERVIERUNG_SW(new TextServerResource("/pointnumberreservation/A_reservierung_startwert.xml"));

    //~ Instance fields --------------------------------------------------------

    @Getter private final ServerResource value;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new Props object.
     *
     * @param  value  DOCUMENT ME!
     */
    private WundaBlauServerResources(final ServerResource value) {
        this.value = value;
    }
}
