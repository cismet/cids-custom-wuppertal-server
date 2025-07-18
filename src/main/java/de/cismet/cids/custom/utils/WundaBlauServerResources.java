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
import de.cismet.cids.utils.serverresources.JsonServerResource;
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
    BAUMGEBIET_JASPER(new JasperReportServerResource("/reports/baum_gebiet_ueberblick.jasper")),
    VERMESSUNGSRISSE_JASPER(new JasperReportServerResource("/reports/vermessungsrisse.jasper")),
    VERMESSUNGSRISSE_DOCUMENT_JASPER(new JasperReportServerResource("/reports/vermessungsrisse_document.jasper")),
    VERMESSUNGSRISSE_OVERVIEW_JASPER(new JasperReportServerResource("/reports/vermessungsrisse_overview.jasper")),
    APMAPS_JASPER(new JasperReportServerResource("/reports/apmaps.jasper")),
    APMAPS_DOCUMENT_JASPER(new JasperReportServerResource("/reports/apmaps_document.jasper")),
    APMAPS_OVERVIEW_JASPER(new JasperReportServerResource("/reports/apmaps_overview.jasper")),
    NIVP_JASPER(new JasperReportServerResource("/reports/nivp.jasper")),
    NIVP_DOCUMENT_JASPER(new JasperReportServerResource("/reports/nivp_document.jasper")),
    NIVP_OVERVIEW_JASPER(new JasperReportServerResource("/reports/nivp_overview.jasper")),
    FS_RECHNUNG_JASPER(new JasperReportServerResource("/reports/bestellung_rechnung.jasper")),
    FS_RECHNUNG_RUECKSEITE_JASPER(new JasperReportServerResource("/reports/bestellung_rechnung_rueckseite.jasper")),
    ALBO_VORGANG_JASPER(new JasperReportServerResource("/reports/albo_vorgang.jasper")),
    ALBO_VORGANG_EXT_JASPER(new JasperReportServerResource("/reports/albo_2_vorgang.jasper")),
    ALBO_FLAECHE_JASPER(new JasperReportServerResource("/reports/albo_flaeche.jasper")),

    ALKIS_PRODUKTBESCHREIBUNG_XML(new TextServerResource("/alkis/Produktbeschreibung_ALKIS.xml")),
    PNR_TEMPLATE_BEN_AUFTR_ALL(new TextServerResource("/pointnumberreservation/A_Ben_Auftr_alle_PKZ.xml")),
    PNR_TEMPLATE_BEN_AUFTR_ONE_ANR(new TextServerResource("/pointnumberreservation/A_Ben_Auftr_eine_ANR.xml")),
    PNR_TEMPLATE_BEN_AUFTR_WILDCARD(new TextServerResource(
            "/pointnumberreservation/A_Ben_Auftr_ANR_Praefix_Wildcard.xml")),
    PNR_TEMPLATE_FREIGABE(new TextServerResource("/pointnumberreservation/A_Freigabe.xml")),
    PNR_TEMPLATE_PROLONG(new TextServerResource("/pointnumberreservation/A_Verlaengern.xml")),
    PNR_TEMPLATE_PROLONG_SUB(new TextServerResource("/pointnumberreservation/A_Verlaengern__Sub.xml")),
    PNR_TEMPLATE_RESERVIERUNG(new TextServerResource("/pointnumberreservation/A_reservierung.xml")),
    PNR_TEMPLATE_RESERVIERUNG_SW(new TextServerResource("/pointnumberreservation/A_reservierung_startwert.xml")),
    DATASOURCES_CAPABILITYLIST_TEXT(new TextServerResource("/datasources/capabilities.xml")),

    POTENZIALFLAECHEN_MAPS_JSON(new JsonServerResource("/potenzialflaechen/maps.json", PotenzialflaechenMapsJson.class)),
    NAS_PRODUCT_DESCRIPTION_JSON(new TextServerResource("/nas/nasProductDescription.json")),
    ALKIS_BUCHUNTSBLATTBEZIRKE_JSON(new TextServerResource("/alkis/buchungsblattbezirke.json")),
    BERECHTIGUNGSPRUEFUNG_CONF_JSON(new TextServerResource("/berechtigungspruefung/berechtigungspruefung_conf.json")),
    BILLING_JSON(new TextServerResource("/billing/billing.json")),
    ORBIT_AUTH_JSON(new TextServerResource("/orbit/auth.json")),

    MAUERN_PROPERTIES(new PropertiesServerResource("/mauern/mauern.properties", MauernProperties.class)),
    POTENZIALFLAECHEN_PROPERTIES(new PropertiesServerResource(
            "/potenzialflaechen/potenzialflaechen.properties",
            PotenzialflaechenProperties.class)),
    FORMSOLUTIONS_PROPERTIES(new TextServerResource("/formsolutions/fs_conf.properties")),
    BUTLER_PROPERTIES(new TextServerResource("/butler/butler.properties")),
    NAS_SERVER_PROPERTIES(new TextServerResource("/nas/nasServer_conf.properties")),
    FME_DB_CONN_PROPERTIES(new TextServerResource("/nas/fme_db_conn.properties")),
    PNR_PROPERTIES(new TextServerResource("/pointnumberreservation/pointNumberRes_conf.properties")),
    VCM_PROPERTIES(new TextServerResource("/virtualcitymap/vcm.properties")),
    STADTBILDER_CONF_PROPERTIES(new TextServerResource("/stadtbilder/sb_conf.properties")),
    BYTEARRAYFACTORY_PROPERTIES(new TextServerResource("/byteArrayFactory.properties")),
    URLCONFIG_PROPERTIES(new TextServerResource("/urlconfig.properties")),
    ALKIS_CONF(new TextServerResource("/alkis/alkis_conf.properties")),
    ALKIS_REST_CONF(new TextServerResource("/alkis/alkis_rest_conf.properties")),
    ALKIS_PRODUCTS_PROPERTIES(new TextServerResource("/alkis/alkis_products.properties")),
    ALKIS_FORMATS_PROPERTIES(new TextServerResource("/alkis/formats.properties")),
    BERECHTIGUNGSPRUEFUNG_PROPERTIES(new TextServerResource("/berechtigungspruefung/berechtigungspruefung.properties")),
    GRUNDWASSERMESSSTELLEN_PROPERTIES(new TextServerResource("/grundwassermessstellen/gwm_conf.properties")),
    VERMESSUNGSUNTERLAGENPORTAL_PROPERTIES(new TextServerResource("/vermessungsunterlagenportal/vup_conf.properties")),
    QSGEB_PROPERTIES(new TextServerResource("/qsgeb/qsgeb_conf.properties")),
    BPARK_CONF_PROPERTIES(new TextServerResource("/bpark/bpark_conf.properties")),
    MOTD_PROPERTIES(new TextServerResource("/motd/wunda_blau.properties")),
    POI_CONF_PROPERTIES(new TextServerResource("/poi/poi_conf.properties")),
    STAMPER_CONF_PROPERTIES(new TextServerResource("/stamper/stamper_conf.properties")),
    ORBIT_SETTINGS_PROPERTIES(new TextServerResource("/orbit/settings.properties")),
    EMOB_CONF_PROPERTIES(new TextServerResource("/emob/emob_conf.properties")),
    PRBR_CONF_PROPERTIES(new TextServerResource("/prbr/prbr_conf.properties")),
    BAUM_CONF_PROPERTIES(new TextServerResource("/baum/baum_conf.properties")),
    VK_CONF_PROPERTIES(new TextServerResource("/vorhabenkarte/vk_conf.properties")),
    AL_CONF_PROPERTIES(new TextServerResource("/alLuftbild/al_conf.properties")),
    KLIMA_CONF_PROPERTIES(new TextServerResource("/klima/klima_conf.properties")),
    KLIMAROUTE_CONF_PROPERTIES(new TextServerResource("/klimaroute/klimaroute_conf.properties")),
    NO2_CONF_PROPERTIES(new TextServerResource("/no2/no2_conf.properties")),
    SPST_CONF_PROPERTIES(new TextServerResource("/spst/spst_conf.properties")),
    UA_CONF_PROPERTIES(new TextServerResource("/umweltalarm/ua_conf.properties")),
    TW_CONF_PROPERTIES(new TextServerResource("/trinkwasser/tw_conf.properties")),
    STRADR_CONF_PROPERTIES(new TextServerResource("/stradr/stradr_conf.properties")),
    DATASOURCES_CREDENTIALS_PROPERTIES(new TextServerResource("/datasources/credentials.properties")),
    DATASOURCES_GENERAL_PROPERTIES(new TextServerResource("/datasources/general.properties")),
    VZKAT_PROPERTIES(new TextServerResource("/vzkat/vzkat.properties")),
    ALBO_PROPERTIES(new TextServerResource("/albo/albo.properties")),
    FS_MAIL_CONFIGURATION(new JsonServerResource("/formsolutions/mail_configuration.json")),
    FS_MAIL_CONTENT_TEMPLATE(new TextServerResource("/formsolutions/file_size_mail_content.txt")),
    VK_MAIL_CONFIGURATION(new JsonServerResource("/vorhabenkarte/mail_configuration.json")),
    FS_LARGE_PRODUCT_RESPONSE(new BinaryServerResource("/formsolutions/largeProduct.zip")),
    ALBO_WEBDAV_PROPERTIES(new TextServerResource("/albo/webdav_conf.properties")),
    UMWELTALARM_WEBDAV_PROPERTIES(new TextServerResource("/umweltalarm/webdav_conf.properties")),

    IMAGE_ANNOTATOR_FONT(new BinaryServerResource("/tiffer/Calibri_Bold.ttf"));

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
