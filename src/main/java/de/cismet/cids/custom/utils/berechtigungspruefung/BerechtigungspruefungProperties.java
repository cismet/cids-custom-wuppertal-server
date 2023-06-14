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
public class BerechtigungspruefungProperties {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            BerechtigungspruefungProperties.class);

    //~ Instance fields --------------------------------------------------------

    private final String cidsLogin;
    private final String cidsPassword;
    private final String anhangAbsPath;
    private final String csmAnfrage;
    private final String csmBearbeitung;
    private final String csmFreigabe;
    private final Integer billingStornogrundId;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new BerechtigungspruefungProperties object.
     *
     * @param  serviceProperties  DOCUMENT ME!
     */
    public BerechtigungspruefungProperties(final Properties serviceProperties) {
        String cidsLogin = null;
        String cidsPassword = null;
        String anhangAbsPath = "/tmp";
        String csmAnfrage = "berechtigungspruefungAnfrage";
        String csmBearbeitung = "berechtigungspruefungBearbeitung";
        String csmFreigabe = "berechtigungspruefungFreigabe";
        Integer billingStornogrundId = null;

        try {
            cidsLogin = serviceProperties.getProperty("CIDS_LOGIN");
            cidsPassword = serviceProperties.getProperty("CIDS_PASSWORD");
            if (serviceProperties.getProperty("ANHANG_ABS_PATH") != null) {
                anhangAbsPath = serviceProperties.getProperty("ANHANG_ABS_PATH");
            }
            if (serviceProperties.getProperty("CSM_ANFRAGE") != null) {
                csmAnfrage = serviceProperties.getProperty("CSM_ANFRAGE");
            }
            if (serviceProperties.getProperty("CSM_BEARBEITUNG") != null) {
                csmBearbeitung = serviceProperties.getProperty("CSM_BEARBEITUNG");
            }
            if (serviceProperties.getProperty("CSM_FREIGABE") != null) {
                csmFreigabe = serviceProperties.getProperty("CSM_FREIGABE");
            }
            if (serviceProperties.getProperty("BILLING_STORNOGRUND_ID") != null) {
                billingStornogrundId = Integer.parseInt(serviceProperties.getProperty("BILLING_STORNOGRUND_ID"));
            }
        } catch (final Exception ex) {
            LOG.error("error while loading properties", ex);
        }

        this.cidsLogin = cidsLogin;
        this.cidsPassword = cidsPassword;
        this.anhangAbsPath = anhangAbsPath;
        this.csmAnfrage = csmAnfrage;
        this.csmBearbeitung = csmBearbeitung;
        this.csmFreigabe = csmFreigabe;
        this.billingStornogrundId = billingStornogrundId;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static BerechtigungspruefungProperties getInstance() {
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

        private static final BerechtigungspruefungProperties INSTANCE;

        static {
            BerechtigungspruefungProperties instance = null;

            try {
                instance = new BerechtigungspruefungProperties(ServerResourcesLoader.getInstance().loadProperties(
                            WundaBlauServerResources.BERECHTIGUNGSPRUEFUNG_PROPERTIES.getValue()));
            } catch (final Exception ex) {
                LOG.error(ex, ex);
            }

            INSTANCE = instance;
        }

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new LazyInitialiser object.
         */
        private LazyInitialiser() {
        }
    }
}
