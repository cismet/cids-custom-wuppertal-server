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

import de.cismet.cids.custom.utils.properties.PotenzialflaechenProperties;

import de.cismet.cids.utils.serverresources.BinaryServerResource;
import de.cismet.cids.utils.serverresources.JasperReportServerResource;
import de.cismet.cids.utils.serverresources.PropertiesServerResource;
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

    BAULASTBESCHEINIGUNG_JASPER(new JasperReportServerResource("/reports/baulastbescheinigung.jasper")),
    BAULASTEN_JASPER(new JasperReportServerResource("/reports/baulasten.jasper")),
    VERMESSUNGSRISSE_JASPER(new JasperReportServerResource("/reports/vermessungsrisse.jasper")),
    VERMESSUNGSRISSE_DOCUMENT_JASPER(new JasperReportServerResource("/reports/vermessungsrisse_document.jasper")),
    VERMESSUNGSRISSE_OVERVIEW_JASPER(new JasperReportServerResource("/reports/vermessungsrisse_overview.jasper")),
    POTENZIALFLAECHEN_PROPERTIES(new PropertiesServerResource(
            "/potenzialflaechen/potenzialflaechen.properties",
            PotenzialflaechenProperties.class)),
    APMAPS_JASPER(new JasperReportServerResource("/reports/apmaps.jasper")),
    APMAPS_DOCUMENT_JASPER(new JasperReportServerResource("/reports/apmaps_document.jasper")),
    APMAPS_OVERVIEW_JASPER(new JasperReportServerResource("/reports/apmaps_overview.jasper")),
    NIVP_JASPER(new JasperReportServerResource("/reports/nivp.jasper")),
    NIVP_DOCUMENT_JASPER(new JasperReportServerResource("/reports/nivp_document.jasper")),
    NIVP_OVERVIEW_JASPER(new JasperReportServerResource("/reports/nivp_overview.jasper")),
    FS_RECHNUNG_JASPER(new JasperReportServerResource("/reports/bestellung_rechnung.jasper")),
    FS_RECHNUNG_RUECKSEITE_JASPER(new JasperReportServerResource("/reports/bestellung_rechnung_rueckseite.jasper")),
    ALBO_VORGANG_JASPER(new JasperReportServerResource("/reports/albo_vorgang.jasper")),
    ALBO_FLAECHE_JASPER(new JasperReportServerResource("/reports/albo_flaeche.jasper")),

    FORMSOLUTIONS_PROPERTIES(new TextServerResource("/formsolutions/fs_conf.properties")),
    BUTLER_PROPERTIES(new TextServerResource("/butler/butler.properties")),
    NAS_SERVER_PROPERTIES(new TextServerResource("/nas/nasServer_conf.properties")),
    NAS_PRODUCT_DESCRIPTION_JSON(new TextServerResource("/nas/nasProductDescription.json")),
    FME_DB_CONN_PROPERTIES(new TextServerResource("/nas/fme_db_conn.properties")),
    PNR_PROPERTIES(new TextServerResource("/pointnumberreservation/pointNumberRes_conf.properties")),
    VCM_PROPERTIES(new TextServerResource("/virtualcitymap/vcm.properties")),
    IMAGE_ANNOTATOR_FONT(new BinaryServerResource("/tiffer/Calibri_Bold.ttf")),
    STADTBILDER_CONF_PROPERTIES(new TextServerResource("/stadtbilder/sb_conf.properties")),
    BYTEARRAYFACTORY_PROPERTIES(new TextServerResource("/byteArrayFactory.properties")),

    URLCONFIG_PROPERTIES(new TextServerResource("/urlconfig.properties")),
    ALKIS_CONF(new TextServerResource("/alkis/alkis_conf.properties")),
    ALKIS_PRODUCTS_PROPERTIES(new TextServerResource("/alkis/alkis_products.properties")),
    ALKIS_BUCHUNTSBLATTBEZIRKE_JSON(new TextServerResource("/alkis/buchungsblattbezirke.json")),
    ALKIS_FORMATS_PROPERTIES(new TextServerResource("/alkis/formats.properties")),
    ALKIS_PRODUKTBESCHREIBUNG_XML(new TextServerResource("/alkis/Produktbeschreibung_ALKIS.xml")),
    BERECHTIGUNGSPRUEFUNG_PROPERTIES(new TextServerResource("/berechtigungspruefung/berechtigungspruefung.properties")),
    BERECHTIGUNGSPRUEFUNG_CONF_JSON(new TextServerResource("/berechtigungspruefung/berechtigungspruefung_conf.json")),

    BILLING_JSON(new TextServerResource("/billing/billing.json")),

    GRUNDWASSERMESSSTELLEN_PROPERTIES(new TextServerResource("/grundwassermessstellen/gwm_conf.properties")),
    VERMESSUNGSUNTERLAGENPORTAL_PROPERTIES(new TextServerResource("/vermessungsunterlagenportal/vup_conf.properties")),
    QSGEB_PROPERTIES(new TextServerResource("/qsgeb/qsgeb_conf.properties")),
    MOTD_PROPERTIES(new TextServerResource("/motd/wunda_blau.properties")),
    PNR_TEMPLATE_BEN_AUFTR_ALL(new TextServerResource("/pointnumberreservation/A_Ben_Auftr_alle_PKZ.xml")),
    PNR_TEMPLATE_BEN_AUFTR_ONE_ANR(new TextServerResource("/pointnumberreservation/A_Ben_Auftr_eine_ANR.xml")),
    PNR_TEMPLATE_BEN_AUFTR_WILDCARD(new TextServerResource(
            "/pointnumberreservation/A_Ben_Auftr_ANR_Praefix_Wildcard.xml")),
    PNR_TEMPLATE_FREIGABE(new TextServerResource("/pointnumberreservation/A_Freigabe.xml")),
    PNR_TEMPLATE_PROLONG(new TextServerResource("/pointnumberreservation/A_Verlaengern.xml")),
    PNR_TEMPLATE_PROLONG_SUB(new TextServerResource("/pointnumberreservation/A_Verlaengern__Sub.xml")),
    PNR_TEMPLATE_RESERVIERUNG(new TextServerResource("/pointnumberreservation/A_reservierung.xml")),
    PNR_TEMPLATE_RESERVIERUNG_SW(new TextServerResource("/pointnumberreservation/A_reservierung_startwert.xml")),
    POI_CONF_PROPERTIES(new TextServerResource("/poi/poi_conf.properties")),
    STAMPER_CONF_PROPERTIES(new TextServerResource("/stamper/stamper_conf.properties")),
    ORBIT_AUTH_JSON(new TextServerResource("/orbit/auth.json")),
    ORBIT_SETTINGS_PROPERTIES(new TextServerResource("/orbit/settings.properties")),

    EMOB_CONF_PROPERTIES(new TextServerResource("/emob/emob_conf.properties")),
    PRBR_CONF_PROPERTIES(new TextServerResource("/prbr/prbr_conf.properties")),
    KLIMA_CONF_PROPERTIES(new TextServerResource("/klima/klima_conf.properties")),
    NO2_CONF_PROPERTIES(new TextServerResource("/no2/no2_conf.properties")),
    STRADR_CONF_PROPERTIES(new TextServerResource("/stradr/stradr_conf.properties")),

    DATASOURCES_CAPABILITYLIST_TEXT(new TextServerResource("/datasources/capabilities.xml")),
    DATASOURCES_CREDENTIALS_PROPERTIES(new TextServerResource("/datasources/credentials.properties")),
    DATASOURCES_GENERAL_PROPERTIES(new TextServerResource("/datasources/general.properties")),

    VZKAT_PROPERTIES(new TextServerResource("/vzkat/vzkat.properties")),

    ALBO_PROPERTIES(new TextServerResource("/albo/albo.properties"));

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
