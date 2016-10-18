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
package de.cismet.cids.custom.utils.formsolutions;

import java.util.Properties;

import de.cismet.cids.custom.utils.WundaBlauServerResources;
import de.cismet.cids.custom.utils.alkis.AlkisConstants;

import de.cismet.cids.utils.serverresources.CachedServerResourcesLoader;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class FormSolutionsConstants {

    //~ Static fields/initializers ---------------------------------------------

    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(AlkisConstants.class);

    public static final String USER;
    public static final String PASSWORD;
    public static final String MYSQL_JDBC;
    public static final String PRODUKT_BASEPATH;
    public static final String RECHNUNG_BASEPATH;
    public static final String CIDS_LOGIN;
    public static final String CIDS_PASSWORD;
    public static final String URL_AUFTRAGSLISTE_FS;
    public static final String URL_AUFTRAG_FS;
    public static final String URL_AUFTRAG_DELETE_FS;
    public static final String URL_STATUS_UPDATE;
    public static final String FTP_HOST;
    public static final String FTP_LOGIN;
    public static final String FTP_PASS;
    public static final boolean TEST;
    public static final boolean TEST_CISMET00;
    public static final String BILLING_KUNDE_LOGIN;
    public static final String BILLING_MODUS;
    public static final String BILLING_MODUSBEZEICHNUNG;
    public static final String BILLING_VERWENDUNGSZWECK_DOWNLOAD;
    public static final String BILLING_VERWENDUNGSZWECK_POSTWEG;
    public static final String BILLING_VERWENDUNGSKEY_DOWNLOAD;
    public static final String BILLING_VERWENDUNGSKEY_POSTWEG;
    public static final String BILLING_PRODUKTKEY_DINA4;
    public static final String BILLING_PRODUKTKEY_DINA3;
    public static final String BILLING_PRODUKTKEY_DINA2;
    public static final String BILLING_PRODUKTKEY_DINA1;
    public static final String BILLING_PRODUKTKEY_DINA0;
    public static final String BILLING_PRODUKTBEZEICHNUNG_DINA4;
    public static final String BILLING_PRODUKTBEZEICHNUNG_DINA3;
    public static final String BILLING_PRODUKTBEZEICHNUNG_DINA2;
    public static final String BILLING_PRODUKTBEZEICHNUNG_DINA1;
    public static final String BILLING_PRODUKTBEZEICHNUNG_DINA0;

    static {
        final String user;
        final String password;
        final String mysqlJdbc;
        final String produktBasepath;
        final String rechnungBasepath;
        final String cidsLogin;
        final String cidsPassword;
        final String urlAuftragslisteFs;
        final String urlAuftragFs;
        final String urlAuftragDeleteFs;
        final String urlStatusUpdate;
        final String ftpHost;
        final String ftpLogin;
        final String ftpPass;
        final boolean test;
        final boolean testCismet00;
        final String billingKundeLogin;
        final String billingModus;
        final String billingModusbezeichnung;
        final String billingVerwendungszweckDownload;
        final String billingVerwendungszweckPostweg;
        final String billingVerwendungskeyDownload;
        final String billingVerwendungskeyPostweg;
        final String billingProduktKeyDina4;
        final String billingProduktKeyDina3;
        final String billingProduktKeyDina2;
        final String billingProduktKeyDina1;
        final String billingProduktKeyDina0;
        final String billingProduktBezeichnungDina4;
        final String billingProduktBezeichnungDina3;
        final String billingProduktBezeichnungDina2;
        final String billingProduktBezeichnungDina1;
        final String billingProduktBezeichnungDina0;

        try {
            final Properties serviceProperties = CachedServerResourcesLoader.getInstance()
                        .getPropertiesResource(WundaBlauServerResources.FORMSOLUTIONS_PROPERTIES.getValue());
            user = serviceProperties.getProperty("USER");
            password = serviceProperties.getProperty("PASSWORD");
            mysqlJdbc = serviceProperties.getProperty("MYSQL_JDBC");
            produktBasepath = serviceProperties.getProperty("PRODUKT_BASEPATH");
            rechnungBasepath = serviceProperties.getProperty("RECHNUNG_BASEPATH");
            cidsLogin = serviceProperties.getProperty("CIDS_LOGIN");
            cidsPassword = serviceProperties.getProperty("CIDS_PASSWORD");
            urlAuftragslisteFs = serviceProperties.getProperty("URL_AUFTRAGSLISTE_FS");
            urlAuftragFs = serviceProperties.getProperty("URL_AUFTRAG_FS");
            urlAuftragDeleteFs = serviceProperties.getProperty("URL_AUFTRAG_DELETE_FS");
            urlStatusUpdate = serviceProperties.getProperty("URL_STATUS_UPDATE");
            ftpHost = serviceProperties.getProperty("FTP_HOST");
            ftpLogin = serviceProperties.getProperty("FTP_LOGIN");
            ftpPass = serviceProperties.getProperty("FTP_PASS");
            test = (serviceProperties.getProperty("TEST") != null)
                        && "true".equals(serviceProperties.getProperty("TEST").trim().toLowerCase());
            testCismet00 = (serviceProperties.getProperty("TEST_CISMET00") != null)
                        && "true".equals(serviceProperties.getProperty("TEST_CISMET00").trim().toLowerCase());
            billingKundeLogin = serviceProperties.getProperty("BILLING_KUNDE_LOGIN");
            billingModus = serviceProperties.getProperty("BILLING_MODUS");
            billingModusbezeichnung = serviceProperties.getProperty("BILLING_MODUSBEZEICHNUNG");
            billingVerwendungszweckDownload = serviceProperties.getProperty("BILLING_VERWENDUNGSZWECK_DOWNLOAD");
            billingVerwendungszweckPostweg = serviceProperties.getProperty("BILLING_VERWENDUNGSZWECK_POSTWEG");
            billingVerwendungskeyDownload = serviceProperties.getProperty("BILLING_VERWENDUNGSKEY_DOWNLOAD");
            billingVerwendungskeyPostweg = serviceProperties.getProperty("BILLING_VERWENDUNGSKEY_POSTWEG");
            billingProduktKeyDina4 = serviceProperties.getProperty("BILLING_PRODUKTKEY_DINA4");
            billingProduktKeyDina3 = serviceProperties.getProperty("BILLING_PRODUKTKEY_DINA3");
            billingProduktKeyDina2 = serviceProperties.getProperty("BILLING_PRODUKTKEY_DINA2");
            billingProduktKeyDina1 = serviceProperties.getProperty("BILLING_PRODUKTKEY_DINA1");
            billingProduktKeyDina0 = serviceProperties.getProperty("BILLING_PRODUKTKEY_DINA0");
            billingProduktBezeichnungDina4 = serviceProperties.getProperty("BILLING_PRODUKTBEZEICHNUNG_DINA4");
            billingProduktBezeichnungDina3 = serviceProperties.getProperty("BILLING_PRODUKTBEZEICHNUNG_DINA3");
            billingProduktBezeichnungDina2 = serviceProperties.getProperty("BILLING_PRODUKTBEZEICHNUNG_DINA2");
            billingProduktBezeichnungDina1 = serviceProperties.getProperty("BILLING_PRODUKTBEZEICHNUNG_DINA1");
            billingProduktBezeichnungDina0 = serviceProperties.getProperty("BILLING_PRODUKTBEZEICHNUNG_DINA0");
        } catch (final Throwable ex) {
            LOG.fatal("FormSolutionsConstants Error!", ex);
            throw new RuntimeException(ex);
        }

        USER = user;
        PASSWORD = password;
        MYSQL_JDBC = mysqlJdbc;
        PRODUKT_BASEPATH = produktBasepath;
        RECHNUNG_BASEPATH = rechnungBasepath;
        CIDS_LOGIN = cidsLogin;
        CIDS_PASSWORD = cidsPassword;
        URL_AUFTRAGSLISTE_FS = urlAuftragslisteFs;
        URL_AUFTRAG_FS = urlAuftragFs;
        URL_AUFTRAG_DELETE_FS = urlAuftragDeleteFs;
        URL_STATUS_UPDATE = urlStatusUpdate;
        FTP_HOST = ftpHost;
        FTP_LOGIN = ftpLogin;
        FTP_PASS = ftpPass;
        TEST = test;
        TEST_CISMET00 = testCismet00;
        BILLING_KUNDE_LOGIN = billingKundeLogin;
        BILLING_MODUS = billingModus;
        BILLING_MODUSBEZEICHNUNG = billingModusbezeichnung;
        BILLING_VERWENDUNGSZWECK_DOWNLOAD = billingVerwendungszweckDownload;
        BILLING_VERWENDUNGSZWECK_POSTWEG = billingVerwendungszweckPostweg;
        BILLING_VERWENDUNGSKEY_DOWNLOAD = billingVerwendungskeyDownload;
        BILLING_VERWENDUNGSKEY_POSTWEG = billingVerwendungskeyPostweg;
        BILLING_PRODUKTKEY_DINA4 = billingProduktKeyDina4;
        BILLING_PRODUKTKEY_DINA3 = billingProduktKeyDina3;
        BILLING_PRODUKTKEY_DINA2 = billingProduktKeyDina2;
        BILLING_PRODUKTKEY_DINA1 = billingProduktKeyDina1;
        BILLING_PRODUKTKEY_DINA0 = billingProduktKeyDina0;
        BILLING_PRODUKTBEZEICHNUNG_DINA4 = billingProduktBezeichnungDina4;
        BILLING_PRODUKTBEZEICHNUNG_DINA3 = billingProduktBezeichnungDina3;
        BILLING_PRODUKTBEZEICHNUNG_DINA2 = billingProduktBezeichnungDina2;
        BILLING_PRODUKTBEZEICHNUNG_DINA1 = billingProduktBezeichnungDina1;
        BILLING_PRODUKTBEZEICHNUNG_DINA0 = billingProduktBezeichnungDina0;
    }
}
