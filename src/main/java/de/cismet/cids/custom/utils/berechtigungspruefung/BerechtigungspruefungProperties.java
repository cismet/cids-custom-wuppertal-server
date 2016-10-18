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

import java.util.Properties;

import de.cismet.cids.custom.utils.WundaBlauServerResources;

import de.cismet.cids.utils.serverresources.CachedServerResourcesLoader;

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

    public static final String CIDS_LOGIN;
    public static final String CIDS_PASSWORD;
    public static final String ANHANG_PFAD;
    public static final String CSM_ANFRAGE;
    public static final String CSM_BEARBEITUNG;
    public static final String CSM_FREIGABE;

    static {
        String cidsLogin = null;
        String cidsPassword = null;
        String anhangPfad = "/tmp";
        String categoryAnfrage = "berechtigungspruefungAnfrage";
        String categoryBearbeitung = "berechtigungspruefungBearbeitung";
        String categoryFreigabe = "berechtigungspruefungFreigabe";

        try {
            final Properties serviceProperties = CachedServerResourcesLoader.getInstance()
                        .getPropertiesResource(WundaBlauServerResources.BERECHTIGUNGSPRUEFUNG_PROPERTIES.getValue());

            cidsLogin = serviceProperties.getProperty("CIDS_LOGIN");
            cidsPassword = serviceProperties.getProperty("CIDS_PASSWORD");
            if (serviceProperties.getProperty("ANHANG_PFAD") != null) {
                anhangPfad = serviceProperties.getProperty("ANHANG_PFAD");
            }
            if (serviceProperties.getProperty("CSM_ANFRAGE") != null) {
                categoryAnfrage = serviceProperties.getProperty("CSM_ANFRAGE");
            }
            if (serviceProperties.getProperty("CSM_BEARBEITUNG") != null) {
                categoryBearbeitung = serviceProperties.getProperty("CSM_BEARBEITUNG");
            }
            if (serviceProperties.getProperty("CSM_FREIGABE") != null) {
                categoryFreigabe = serviceProperties.getProperty("CSM_FREIGABE");
            }
        } catch (final Exception ex) {
            LOG.error("error while loading properties", ex);
        }

        CIDS_LOGIN = cidsLogin;
        CIDS_PASSWORD = cidsPassword;
        ANHANG_PFAD = anhangPfad;
        CSM_ANFRAGE = categoryAnfrage;
        CSM_BEARBEITUNG = categoryBearbeitung;
        CSM_FREIGABE = categoryFreigabe;
    }
}
