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

import java.util.Properties;

import de.cismet.cids.utils.serverresources.ServerResourcesLoader;

/**
 * DOCUMENT ME!
 *
 * @version  $Revision$, $Date$
 */
@Getter
public class AlboProperties {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            AlboProperties.class);
    protected static final WundaBlauServerResources SERVER_RESOURCE = WundaBlauServerResources.ALBO_PROPERTIES;

    //~ Instance fields --------------------------------------------------------

    private final String flaecheMapUrl;
    private final Integer flaecheMapWidth;
    private final Integer flaecheMapHeight;
    private final Integer flaecheMapDpi;

    private final String altstandort_color;
    private final String altablagerung_color;
    private final String betriebsstandort_color;
    private final String schadensfall_color;
    private final String immission_color;
    private final String materialaufbringung_color;
    private final String sonstige_color;
    private final String ohne_verdacht_color;

    private final String wz_klassifikation_link;

    // private final String xxx_color;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new AlboProperties object.
     *
     * @param  properties  DOCUMENT ME!
     */
    protected AlboProperties(final Properties properties) {
        this.flaecheMapUrl = readProperty(properties, "flaecheMapUrl", null);
        {
            Integer flaecheMapWidth;
            try {
                flaecheMapWidth = Integer.parseInt(readProperty(properties, "flaecheMapWidth", null));
            } catch (final Exception ex) {
                flaecheMapWidth = null;
                LOG.warn("could not set flaecheMapWidth=" + flaecheMapWidth, ex);
            }
            this.flaecheMapWidth = flaecheMapWidth;
        }
        {
            Integer flaecheMapHeight;
            try {
                flaecheMapHeight = Integer.parseInt(readProperty(properties, "flaecheMapHeight", null));
            } catch (final Exception ex) {
                flaecheMapHeight = null;
                LOG.warn("could not set flaecheMapHeight=" + flaecheMapHeight, ex);
            }
            this.flaecheMapHeight = flaecheMapHeight;
        }
        {
            Integer flaecheMapDpi;
            try {
                flaecheMapDpi = Integer.parseInt(readProperty(properties, "flaecheMapDpi", null));
            } catch (final Exception ex) {
                flaecheMapDpi = null;
                LOG.warn("could not set flaecheMapDpi=" + flaecheMapDpi, ex);
            }
            this.flaecheMapDpi = flaecheMapDpi;
        }

        altstandort_color = readProperty(properties, "altstandort_color", null);
        altablagerung_color = readProperty(properties, "altablagerung_color", null);
        betriebsstandort_color = readProperty(properties, "betriebsstandort_color", null);
        schadensfall_color = readProperty(properties, "schadensfall_color", null);
        immission_color = readProperty(properties, "immission_color", null);
        materialaufbringung_color = readProperty(properties, "materialaufbringung_color", null);
        sonstige_color = readProperty(properties, "sonstige_color", null);
        ohne_verdacht_color = readProperty(properties, "ohne_verdacht_color", null);

        wz_klassifikation_link = readProperty(properties, "wz_klassifikation_link", null);
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param   properties    DOCUMENT ME!
     * @param   property      DOCUMENT ME!
     * @param   defaultValue  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String readProperty(final Properties properties, final String property, final String defaultValue) {
        String value = defaultValue;
        try {
            value = properties.getProperty(property, defaultValue);
        } catch (final Exception ex) {
            final String message = "could not read " + property + " from "
                        + SERVER_RESOURCE.getValue()
                        + ". setting to default value: " + defaultValue;
            LOG.warn(message, ex);
        }
        return value;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static AlboProperties getInstance() {
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

        private static final AlboProperties INSTANCE;

        static {
            try {
                INSTANCE = new AlboProperties(ServerResourcesLoader.getInstance().loadProperties(
                            SERVER_RESOURCE.getValue()));
            } catch (final Exception ex) {
                throw new RuntimeException("Exception while initializing AlboProperties", ex);
            }
        }

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new LazyInitialiser object.
         */
        private LazyInitialiser() {
        }
    }
}
