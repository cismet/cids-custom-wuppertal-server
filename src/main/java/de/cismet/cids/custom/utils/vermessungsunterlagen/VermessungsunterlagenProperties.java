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
package de.cismet.cids.custom.utils.vermessungsunterlagen;

import lombok.Getter;

import org.apache.log4j.Logger;

import java.util.Properties;

import de.cismet.cids.custom.utils.WundaBlauServerResources;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
@Getter
public class VermessungsunterlagenProperties {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient Logger LOG = Logger.getLogger(VermessungsunterlagenProperties.class);

    //~ Instance fields --------------------------------------------------------

    private final Properties properties;
    private final String cidsLogin;
    private final String absPathTmp;
    private final String ftpHost;
    private final String ftpLogin;
    private final String ftpPass;
    private final String ftpPath;
    private final String absPathTest;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new VermessungsunterlagenProperties object.
     *
     * @param  properties  DOCUMENT ME!
     */
    public VermessungsunterlagenProperties(final Properties properties) {
        this.properties = properties;

        cidsLogin = readProperty("CIDS_LOGIN", null);
        absPathTmp = readProperty("ABS_PATH_TMP", "/tmp");
        ftpHost = readProperty("FTP_HOST", null);
        ftpLogin = readProperty("FTP_LOGIN", null);
        ftpPass = readProperty("FTP_PASS", null);
        ftpPath = readProperty("FTP_PATH", null);
        absPathTest = readProperty("ABS_PATH_TEST", null);
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
            value = getProperties().getProperty(property);
        } catch (final Exception ex) {
            final String message = "could not read " + property + " from "
                        + WundaBlauServerResources.VERMESSUNGSUNTERLAGENPORTAL_PROPERTIES.getValue()
                        + ". setting to default value: " + defaultValue;
            LOG.warn(message, ex);
        }
        return value;
    }
}
