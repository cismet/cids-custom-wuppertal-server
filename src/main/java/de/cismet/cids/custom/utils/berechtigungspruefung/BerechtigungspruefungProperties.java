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
package de.cismet.cids.custom.utils.berechtigungspruefung;

import de.cismet.tools.PropertyReader;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class BerechtigungspruefungProperties {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            BerechtigungspruefungProperties.class);

    private static final String PROPERTIES =
        "/de/cismet/cids/custom/berechtigungspruefung/berechtigungspruefung.properties";

    public static final Integer CIDS_USER_ID;
    public static final String CIDS_USER_NAME;
    public static final Integer CIDS_GROUP_ID;
    public static final String CIDS_GROUP_NAME;
    public static final String ANHANG_PFAD;
    public static final String CSM_ANFRAGE;
    public static final String CSM_FREIGABE;

    static {
        Integer cidsUserId = null;
        String cidsUserName = null;
        Integer cidsGroupId = null;
        String cidsGroupName = null;
        String anhangPfad = "/tmp";
        String categoryAnfrage = "berechtigungspruefungAnfrage";
        String categoryFreigabe = "berechtigungspruefungFreigabe";

        try {
            final PropertyReader serviceProperties = new PropertyReader(PROPERTIES);

            cidsUserId = Integer.parseInt(serviceProperties.getProperty("CIDS_USERID"));
            cidsUserName = serviceProperties.getProperty("CIDS_USER_NAME");
            cidsGroupId = Integer.parseInt(serviceProperties.getProperty("CIDS_GROUPID"));
            cidsGroupName = serviceProperties.getProperty("CIDS_GROUPID");
            if (serviceProperties.getProperty("ANHANG_PFAD") != null) {
                anhangPfad = serviceProperties.getProperty("ANHANG_PFAD");
            }
            if (serviceProperties.getProperty("CSM_ANFRAGE") != null) {
                categoryAnfrage = serviceProperties.getProperty("CSM_ANFRAGE");
            }
            if (serviceProperties.getProperty("CSM_FREIGABE") != null) {
                categoryFreigabe = serviceProperties.getProperty("CSM_FREIGABE");
            }
        } catch (final Exception ex) {
            LOG.error("error while loading properties", ex);
        }

        CIDS_USER_ID = cidsUserId;
        CIDS_USER_NAME = cidsUserName;
        CIDS_GROUP_ID = cidsGroupId;
        CIDS_GROUP_NAME = cidsGroupName;
        ANHANG_PFAD = anhangPfad;
        CSM_ANFRAGE = categoryAnfrage;
        CSM_FREIGABE = categoryFreigabe;
    }
}
