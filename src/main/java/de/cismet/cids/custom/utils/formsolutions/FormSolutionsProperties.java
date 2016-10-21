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

import Sirius.server.middleware.impls.domainserver.DomainServerImpl;

import lombok.Getter;

import java.util.Properties;

import de.cismet.cids.custom.utils.WundaBlauServerResources;

import de.cismet.cids.utils.serverresources.ServerResourcesLoader;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
@Getter
public class FormSolutionsProperties {

    //~ Static fields/initializers ---------------------------------------------

    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(FormSolutionsProperties.class);

    //~ Instance fields --------------------------------------------------------

    private final String user;
    private final String password;
    private final String mysqlJdbc;
    private final String produktBasepath;
    private final String rechnungBasepath;
    private final String cidsLogin;
    private final String cidsPassword;
    private final String urlAuftragslisteFs;
    private final String urlAuftragFs;
    private final String urlAuftragDeleteFs;
    private final String urlStatusUpdate;
    private final String ftpHost;
    private final String ftpLogin;
    private final String ftpPass;
    private final boolean test;
    private final boolean testCismet00;
    private final String billingKundeLogin;
    private final String billingModus;
    private final String billingModusbezeichnung;
    private final String billingVerwendungszweckDownload;
    private final String billingVerwendungszweckPostweg;
    private final String billingVerwendungskeyDownload;
    private final String billingVerwendungskeyPostweg;
    private final String billingProduktKeyDina4;
    private final String billingProduktKeyDina3;
    private final String billingProduktKeyDina2;
    private final String billingProduktKeyDina1;
    private final String billingProduktKeyDina0;
    private final String billingProduktBezeichnungDina4;
    private final String billingProduktBezeichnungDina3;
    private final String billingProduktBezeichnungDina2;
    private final String billingProduktBezeichnungDina1;
    private final String billingProduktBezeichnungDina0;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new FormSolutionsProperties object.
     *
     * @param  properties  DOCUMENT ME!
     */
    private FormSolutionsProperties(final Properties properties) {
        user = properties.getProperty("USER");
        password = properties.getProperty("PASSWORD");
        mysqlJdbc = properties.getProperty("MYSQL_JDBC");
        produktBasepath = properties.getProperty("PRODUKT_BASEPATH");
        rechnungBasepath = properties.getProperty("RECHNUNG_BASEPATH");
        cidsLogin = properties.getProperty("CIDS_LOGIN");
        cidsPassword = properties.getProperty("CIDS_PASSWORD");
        urlAuftragslisteFs = properties.getProperty("URL_AUFTRAGSLISTE_FS");
        urlAuftragFs = properties.getProperty("URL_AUFTRAG_FS");
        urlAuftragDeleteFs = properties.getProperty("URL_AUFTRAG_DELETE_FS");
        urlStatusUpdate = properties.getProperty("URL_STATUS_UPDATE");
        ftpHost = properties.getProperty("FTP_HOST");
        ftpLogin = properties.getProperty("FTP_LOGIN");
        ftpPass = properties.getProperty("FTP_PASS");
        test = (properties.getProperty("TEST") != null)
                    && "true".equals(properties.getProperty("TEST").trim().toLowerCase());
        testCismet00 = (properties.getProperty("TEST_CISMET00") != null)
                    && "true".equals(properties.getProperty("TEST_CISMET00").trim().toLowerCase());
        billingKundeLogin = properties.getProperty("BILLING_KUNDE_LOGIN");
        billingModus = properties.getProperty("BILLING_MODUS");
        billingModusbezeichnung = properties.getProperty("BILLING_MODUSBEZEICHNUNG");
        billingVerwendungszweckDownload = properties.getProperty("BILLING_VERWENDUNGSZWECK_DOWNLOAD");
        billingVerwendungszweckPostweg = properties.getProperty("BILLING_VERWENDUNGSZWECK_POSTWEG");
        billingVerwendungskeyDownload = properties.getProperty("BILLING_VERWENDUNGSKEY_DOWNLOAD");
        billingVerwendungskeyPostweg = properties.getProperty("BILLING_VERWENDUNGSKEY_POSTWEG");
        billingProduktKeyDina4 = properties.getProperty("BILLING_PRODUKTKEY_DINA4");
        billingProduktKeyDina3 = properties.getProperty("BILLING_PRODUKTKEY_DINA3");
        billingProduktKeyDina2 = properties.getProperty("BILLING_PRODUKTKEY_DINA2");
        billingProduktKeyDina1 = properties.getProperty("BILLING_PRODUKTKEY_DINA1");
        billingProduktKeyDina0 = properties.getProperty("BILLING_PRODUKTKEY_DINA0");
        billingProduktBezeichnungDina4 = properties.getProperty("BILLING_PRODUKTBEZEICHNUNG_DINA4");
        billingProduktBezeichnungDina3 = properties.getProperty("BILLING_PRODUKTBEZEICHNUNG_DINA3");
        billingProduktBezeichnungDina2 = properties.getProperty("BILLING_PRODUKTBEZEICHNUNG_DINA2");
        billingProduktBezeichnungDina1 = properties.getProperty("BILLING_PRODUKTBEZEICHNUNG_DINA1");
        billingProduktBezeichnungDina0 = properties.getProperty("BILLING_PRODUKTBEZEICHNUNG_DINA0");
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static FormSolutionsProperties getInstance() {
        return LazyInitialiser.INSTANCE;
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    private static final class LazyInitialiser {

        //~ Static fields/initializers -----------------------------------------

        private static final FormSolutionsProperties INSTANCE;

        static {
            FormSolutionsProperties fsprop;
            try {
                final Properties properties = ServerResourcesLoader.getInstance()
                            .loadPropertiesResource(WundaBlauServerResources.FORMSOLUTIONS_PROPERTIES.getValue());

                fsprop = new FormSolutionsProperties(properties);
            } catch (final Throwable ex) {
                if ("WUNDA_BLAU".equals(DomainServerImpl.getServerProperties().getServerName())) {
                    LOG.error(
                        "FormSolutionsConstants Initialization Error. All FormSolutions related features will not work.",
                        ex);
                }
                fsprop = null;
            }
            INSTANCE = fsprop;
        }

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new LazyInitialiser object.
         */
        private LazyInitialiser() {
        }
    }
}
