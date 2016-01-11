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

    public static final String USER;
    public static final String PASSWORD;
    public static final String MYSQL_JDBC;
    public static final String PRODUKT_BASEPATH;

    static {
        final String user;
        final String password;
        final String mysqlJdbc;
        final String produktBasepath;

        try {
            final PropertyReader serviceProperties = new PropertyReader(
                    "/de/cismet/cids/custom/wunda_blau/res/formsolutions/fs_conf.properties");

            user = serviceProperties.getProperty("USER");
            password = serviceProperties.getProperty("PASSWORD");
            mysqlJdbc = serviceProperties.getProperty("MYSQL_JDBC");
            produktBasepath = serviceProperties.getProperty("PRODUKT_BASEPATH");
        } catch (final Exception ex) {
            LOG.fatal("FormSolutionsConstants Error!", ex);
            throw new RuntimeException(ex);
        }

        USER = user;
        PASSWORD = password;
        MYSQL_JDBC = mysqlJdbc;
        PRODUKT_BASEPATH = produktBasepath;
    }
}
