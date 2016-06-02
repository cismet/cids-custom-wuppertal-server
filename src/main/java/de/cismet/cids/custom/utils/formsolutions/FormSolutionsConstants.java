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

import de.cismet.cids.custom.utils.alkis.AlkisConstants;

import de.cismet.tools.PropertyReader;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class FormSolutionsConstants {

    //~ Static fields/initializers ---------------------------------------------

    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(AlkisConstants.class);

    private static final String PROPERTIES = "/de/cismet/cids/custom/wunda_blau/res/formsolutions/fs_conf.properties";

    public static final String USER;
    public static final String PASSWORD;
    public static final String MYSQL_JDBC;
    public static final String PRODUKT_BASEPATH;
    public static final Integer CIDS_USERID;
    public static final Integer CIDS_GROUPID;
    public static final String URL_AUFTRAGSLISTE_FS;
    public static final String URL_AUFTRAG_FS;
    public static final String URL_AUFTRAG_DELETE_FS;
    public static final String URL_STATUS_UPDATE;
    public static final String FTP_HOST;
    public static final String FTP_LOGIN;
    public static final String FTP_PASS;
    public static final boolean TEST;
    public static final boolean TEST_CISMET00;

    static {
        final String user;
        final String password;
        final String mysqlJdbc;
        final String produktBasepath;
        final Integer cidsUserId;
        final Integer cidsGroupId;
        final String urlAuftragslisteFs;
        final String urlAuftragFs;
        final String urlAuftragDeleteFs;
        final String urlStatusUpdate;
        final String ftpHost;
        final String ftpLogin;
        final String ftpPass;
        final boolean test;
        final boolean testCismet00;

        try {
            final PropertyReader serviceProperties = new PropertyReader(PROPERTIES);

            user = serviceProperties.getProperty("USER");
            password = serviceProperties.getProperty("PASSWORD");
            mysqlJdbc = serviceProperties.getProperty("MYSQL_JDBC");
            produktBasepath = serviceProperties.getProperty("PRODUKT_BASEPATH");
            cidsUserId = Integer.parseInt(serviceProperties.getProperty("CIDS_USERID"));
            cidsGroupId = Integer.parseInt(serviceProperties.getProperty("CIDS_GROUPID"));
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
        } catch (final Exception ex) {
            LOG.fatal("FormSolutionsConstants Error!", ex);
            throw new RuntimeException(ex);
        }

        USER = user;
        PASSWORD = password;
        MYSQL_JDBC = mysqlJdbc;
        PRODUKT_BASEPATH = produktBasepath;
        CIDS_USERID = cidsUserId;
        CIDS_GROUPID = cidsGroupId;
        URL_AUFTRAGSLISTE_FS = urlAuftragslisteFs;
        URL_AUFTRAG_FS = urlAuftragFs;
        URL_AUFTRAG_DELETE_FS = urlAuftragDeleteFs;
        URL_STATUS_UPDATE = urlStatusUpdate;
        FTP_HOST = ftpHost;
        FTP_LOGIN = ftpLogin;
        FTP_PASS = ftpPass;
        TEST = test;
        TEST_CISMET00 = testCismet00;
    }
}
