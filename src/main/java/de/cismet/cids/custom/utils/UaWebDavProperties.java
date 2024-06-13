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

import org.apache.log4j.Logger;

import java.util.Properties;

/**
 * DOCUMENT ME!
 *
 * @author   sandra
 * @version  $Revision$, $Date$
 */
@Getter
public class UaWebDavProperties {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient Logger LOG = Logger.getLogger(UaWebDavProperties.class);

    //~ Instance fields --------------------------------------------------------

    private final Properties properties;
    private final String webDavHost;
    private final String webDavPath;
    private final String webDavLogin;
    private final String webDavPass;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new UaWebDavProperties object.
     *
     * @param  properties  DOCUMENT ME!
     */
    public UaWebDavProperties(final Properties properties) {
        this.properties = properties;
        webDavHost = readProperty("WEBDAV_HOST", null);
        webDavPath = readProperty("WEBDAV_PATH", null);
        webDavLogin = readProperty("WEBDAV_LOGIN", null);
        webDavPass = readProperty("WEBDAV_PASS", null);
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param   property      DOCUMENT ME!
     * @param   defaultValue  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String readProperty(final String property, final String defaultValue) {
        String value = defaultValue;
        try {
            value = getProperties().getProperty(property, defaultValue);
        } catch (final Exception ex) {
            final String message = "could not read " + property + " from "
                        + WundaBlauServerResources.UMWELTALARM_WEBDAV_PROPERTIES.getValue()
                        + ". setting to default value: " + defaultValue;
            LOG.warn(message, ex);
        }
        return value;
    }
}
