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
    private final String urlAuftragslisteSgkFs;
    private final String urlAuftragslisteAbkFs;
    private final String urlAuftragslisteBb1Fs;
    private final String urlAuftragslisteBb2Fs;
    private final String urlAuftragFs;
    private final String urlAuftragDeleteFs;
    private final String urlStatusUpdate;
    private final boolean ftpEnabled;
    private final String ftpHost;
    private final String ftpLogin;
    private final String ftpPass;
    private final boolean ftpOverTls;
    private final String ftpMountAbsPath;
    private final String testCismet00;
    private final String specialLogAbsPath;
    private final String produktTmpAbsPath;
    private final String anhangTmpAbsPath;
    private final String billingKundeLogin;
    private final String billingKundeLoginBB;
    private final String billingKundeLoginKarte;
    private final String billingModus;
    private final String billingModusbezeichnung;
    private final String billingVerwendungszweckDownload;
    private final String billingVerwendungszweckPostweg;
    private final String billingVerwendungskeyDownload;
    private final String billingVerwendungskeyPostweg;
    private final String rechnungBerechnugsgGrundlage;
    private final String tmpBrokenpdfsAbsPath;
    private final String transidHashpepper;
    private final String redirectionFormat;
    private final String urlCreateCacheid;
    private final String cidsActionHttpRedirectorUrl;
    private final boolean mysqlDisabled;
    private final boolean deleteTmpProductAfterSuccessfulUploadDisabled;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new FormSolutionsProperties object.
     *
     * @param  properties  DOCUMENT ME!
     */
    public FormSolutionsProperties(final Properties properties) {
        user = properties.getProperty("USER");
        password = properties.getProperty("PASSWORD");
        mysqlJdbc = properties.getProperty("MYSQL_JDBC");
        produktBasepath = properties.getProperty("PRODUKT_BASEPATH");
        rechnungBasepath = properties.getProperty("RECHNUNG_BASEPATH");
        cidsLogin = properties.getProperty("CIDS_LOGIN");
        cidsPassword = properties.getProperty("CIDS_PASSWORD");
        urlAuftragslisteSgkFs = properties.getProperty("URL_AUFTRAGSLISTE_SGK_FS");
        urlAuftragslisteAbkFs = properties.getProperty("URL_AUFTRAGSLISTE_ABK_FS");
        urlAuftragslisteBb1Fs = properties.getProperty("URL_AUFTRAGSLISTE_BB1_FS");
        urlAuftragslisteBb2Fs = properties.getProperty("URL_AUFTRAGSLISTE_BB2_FS");
        urlAuftragFs = properties.getProperty("URL_AUFTRAG_FS");
        urlAuftragDeleteFs = properties.getProperty("URL_AUFTRAG_DELETE_FS");
        urlStatusUpdate = properties.getProperty("URL_STATUS_UPDATE");
        ftpEnabled = Boolean.valueOf(properties.getProperty("FTP_ENABLED"));
        ftpHost = properties.getProperty("FTP_HOST");
        ftpLogin = properties.getProperty("FTP_LOGIN");
        ftpPass = properties.getProperty("FTP_PASS");
        ftpMountAbsPath = properties.getProperty("FTP_MOUNT_ABS_PATH");
        ftpOverTls = Boolean.valueOf(properties.getProperty("FTP_OVER_TLS"));
        testCismet00 = properties.getProperty("TEST_CISMET00");
        specialLogAbsPath = properties.getProperty("SPECIAL_LOG_ABS_PATH");
        produktTmpAbsPath = properties.getProperty("PRODUKT_TMP_ABS_PATH");
        anhangTmpAbsPath = properties.getProperty("ANHANG_TMP_ABS_PATH");
        billingKundeLogin = properties.getProperty("BILLING_KUNDE_LOGIN");
        billingKundeLoginBB = properties.getProperty("BILLING_KUNDE_LOGIN_BB");
        billingKundeLoginKarte = properties.getProperty("BILLING_KUNDE_LOGIN_KARTE");
        billingModus = properties.getProperty("BILLING_MODUS");
        billingModusbezeichnung = properties.getProperty("BILLING_MODUSBEZEICHNUNG");
        billingVerwendungszweckDownload = properties.getProperty("BILLING_VERWENDUNGSZWECK_DOWNLOAD");
        billingVerwendungszweckPostweg = properties.getProperty("BILLING_VERWENDUNGSZWECK_POSTWEG");
        billingVerwendungskeyDownload = properties.getProperty("BILLING_VERWENDUNGSKEY_DOWNLOAD");
        billingVerwendungskeyPostweg = properties.getProperty("BILLING_VERWENDUNGSKEY_POSTWEG");
        mysqlDisabled = Boolean.valueOf(properties.getProperty("MYSQL_DISABLED"));
        rechnungBerechnugsgGrundlage = properties.getProperty("RECHNUNG_BERECHNUNGSGRUNDLAGE");
        tmpBrokenpdfsAbsPath = properties.getProperty("TMP_BROKENPDFS_ABS_PATH");
        transidHashpepper = properties.getProperty("TRANSID_HASHPEPPER");
        redirectionFormat = properties.getProperty("REDIRECTION_FORMAT");
        urlCreateCacheid = properties.getProperty("URL_CREATE_CACHEID");
        cidsActionHttpRedirectorUrl = properties.getProperty("CIDS_ACTION_HTTP_REDIRECTOR_URL");
        deleteTmpProductAfterSuccessfulUploadDisabled = Boolean.valueOf(
                properties.getProperty("DELETE_TMP_PRODUCT_AFTER_SUCCESSFUL_UPLOAD_DISABLED"));
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
                            .loadProperties(WundaBlauServerResources.FORMSOLUTIONS_PROPERTIES.getValue());

                fsprop = new FormSolutionsProperties(properties);
            } catch (final Throwable ex) {
                if ("WUNDA_BLAU".equals(DomainServerImpl.getServerProperties().getServerName())) {
                    LOG.error(
                        "FormSolutionsConstants Initialization Error. "
                                + "All FormSolutions related features will not work.",
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
