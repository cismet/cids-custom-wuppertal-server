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
import java.awt.Color;

/**
 * DOCUMENT ME!
 *
 * @author   sandra
 * @version  $Revision$, $Date$
 */
@Getter
public class BaumProperties {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient org.apache.log4j.Logger LOG = 
            org.apache.log4j.Logger.getLogger(BaumProperties.class);
    protected static final WundaBlauServerResources SERVER_RESOURCE = 
            WundaBlauServerResources.BAUM_CONF_PROPERTIES;

    //~ Instance fields --------------------------------------------------------

    private final Properties properties;
    private final String urlErsatzbaum;
    private final String urlFestsetzung;
    private final String urlSchaden;
    private final String urlDefault;
    private final Integer gebietMapDpi;
    private final Integer gebietMapWidth;
    private final Integer gebietMapHeight;
    private final Double gebietMapBuffer;
    private final Integer schadenMapDpi;
    private final Integer schadenMapWidth;
    private final Integer schadenMapHeight;
    private final Double schadenMapBuffer;
    private final Integer ersatzMapDpi;
    private final Integer ersatzMapWidth;
    private final Integer ersatzMapHeight;
    private final Double ersatzMapBuffer;
    private final Integer festMapDpi;
    private final Integer festMapWidth;
    private final Integer festMapHeight;
    private final Double festMapBuffer;
    private final String mapSrs;
    private final String gebietColor;
    private final String ersatzColor;
    private final String baumColor;
    private final String festColor;
    private final String schadenColor;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new BaumProperties object.
     *
     * @param  properties  DOCUMENT ME!
     */
    protected BaumProperties(final Properties properties) {
        this.properties = properties;

        urlErsatzbaum = String.valueOf(properties.getProperty("MAP_CALL_STRING_ERSATZBAUM"));
        urlFestsetzung = String.valueOf(properties.getProperty("MAP_CALL_STRING_FESTSETZUNG"));
        urlSchaden = String.valueOf(properties.getProperty("MAP_CALL_STRING_SCHADEN"));
        urlDefault = String.valueOf(properties.getProperty("MAP_CALL_STRING_DEFAULT"));
        gebietMapDpi = Integer.valueOf(properties.getProperty("GEBIET_MAP_DPI"));
        gebietMapHeight = Integer.valueOf(properties.getProperty("GEBIET_MAP_HEIGHT"));
        gebietMapWidth = Integer.valueOf(properties.getProperty("GEBIET_MAP_WIDTH"));
        gebietMapBuffer = Double.valueOf(properties.getProperty("GEBIET_MAP_BUFFER"));
        schadenMapDpi = Integer.valueOf(properties.getProperty("SCHADEN_MAP_DPI"));
        schadenMapHeight = Integer.valueOf(properties.getProperty("SCHADEN_MAP_HEIGHT"));
        schadenMapWidth = Integer.valueOf(properties.getProperty("SCHADEN_MAP_WIDTH"));
        schadenMapBuffer = Double.valueOf(properties.getProperty("SCHADEN_MAP_BUFFER"));
        ersatzMapDpi = Integer.valueOf(properties.getProperty("ERSATZ_MAP_DPI"));
        ersatzMapHeight = Integer.valueOf(properties.getProperty("ERSATZ_MAP_HEIGHT"));
        ersatzMapWidth = Integer.valueOf(properties.getProperty("ERSATZ_MAP_WIDTH"));
        ersatzMapBuffer = Double.valueOf(properties.getProperty("ERSATZ_MAP_BUFFER"));
        festMapDpi = Integer.valueOf(properties.getProperty("FEST_MAP_DPI"));
        festMapHeight = Integer.valueOf(properties.getProperty("FEST_MAP_HEIGHT"));
        festMapWidth = Integer.valueOf(properties.getProperty("FEST_MAP_WIDTH"));
        festMapBuffer = Double.valueOf(properties.getProperty("FEST_MAP_BUFFER"));
        mapSrs = String.valueOf(properties.getProperty("MAP_SRS"));
        gebietColor = String.valueOf(properties.getProperty("GEBIET_COLOR"));
        ersatzColor = String.valueOf(properties.getProperty("ERSATZ_COLOR"));
        baumColor = String.valueOf(properties.getProperty("BAUM_COLOR"));
        festColor = String.valueOf(properties.getProperty("FEST_COLOR"));
        schadenColor = String.valueOf(properties.getProperty("SCHADEN_COLOR"));
    }

    //~ Methods ----------------------------------------------------------------

    
    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static BaumProperties getInstance() {
        return LazyInitialiser.INSTANCE;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   property      DOCUMENT ME!
     * @param   defaultValue  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public final String valueOfString(final String property, final String defaultValue) {
        String value = defaultValue;
        if (properties.getProperty(property) != null) {
            value = properties.getProperty(property);
        }
        return value;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   property      DOCUMENT ME!
     * @param   defaultValue  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public final Integer valueOfInteger(final String property, final Integer defaultValue) {
        Integer value = defaultValue;
        try {
            value = Integer.valueOf(properties.getProperty(property));
        } catch (final NumberFormatException ex) {
            LOG.warn(String.format(
                    "value of %s is set to %s and can't be cast to Integer.",
                    property,
                    properties.getProperty(property)),
                ex);
        }
        return value;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   property      DOCUMENT ME!
     * @param   defaultValue  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public final Boolean valueOfBoolean(final String property, final Boolean defaultValue) {
        Boolean value = defaultValue;
        try {
            value = Boolean.valueOf(properties.getProperty(property));
        } catch (final Exception ex) {
            LOG.warn(String.format(
                    "value of %s is set to %s and can't be cast to Boolean.",
                    property,
                    properties.getProperty(property)),
                ex);
        }
        return value;
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    private static final class LazyInitialiser {

        //~ Static fields/initializers -----------------------------------------

        private static final BaumProperties INSTANCE;

        static {
            try {
                INSTANCE = getNewInstance();
            } catch (final Exception ex) {
                throw new RuntimeException("Exception while initializing BaumProperties", ex);
            }
        }

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new LazyInitialiser object.
         */
        private LazyInitialiser() {
        }

        //~ Methods ------------------------------------------------------------

        /**
         * DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         *
         * @throws  RuntimeException  DOCUMENT ME!
         */
        public static BaumProperties getNewInstance() {
            try {
                return new BaumProperties(ServerResourcesLoader.getInstance().loadProperties(
                            SERVER_RESOURCE.getValue()));
            } catch (final Exception ex) {
                throw new RuntimeException("Exception while initializing BaumProperties", ex);
            }
        }
    }
}
