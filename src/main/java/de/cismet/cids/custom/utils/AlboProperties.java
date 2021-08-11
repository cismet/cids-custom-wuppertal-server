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

    private final Properties properties;
    private final String flaecheMapUrl;
    private final Integer flaecheMapWidth;
    private final Integer flaecheMapHeight;
    private final Integer flaecheMapDpi;

    private final String wzKlassifikationLink;

    private final String vorgangMapUrl;
    private final Integer vorgangMapWidth;
    private final Integer vorgangMapHeight;
    private final Integer vorgangMapDpi;

    private final String exportTmpAbsPath;
    private final String exportViewName;
    private final String exportOrderbyField;
    private final String exportRowidField;

    // private final String xxx_color;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new AlboProperties object.
     *
     * @param  properties  DOCUMENT ME!
     */
    protected AlboProperties(final Properties properties) {
        this.properties = properties;

        flaecheMapUrl = valueOfString("flaecheMapUrl", null);
        flaecheMapWidth = valueOfInteger("flaecheMapWidth", null);
        flaecheMapHeight = valueOfInteger("flaecheMapHeight", null);
        flaecheMapDpi = valueOfInteger("flaecheMapDpi", null);

        wzKlassifikationLink = valueOfString("wz_klassifikation_link", null);
        vorgangMapUrl = valueOfString("vorgang_map_url", null);

        vorgangMapDpi = valueOfInteger("vorgang_map_dpi", 300);
        vorgangMapWidth = valueOfInteger("vorgang_map_width", 275);
        vorgangMapHeight = valueOfInteger("vorgang_map_height", 130);

        exportTmpAbsPath = valueOfString("export_tmp_abs_path", "/tmp");
        exportViewName = valueOfString("export_view_name", "view_albo_export");
        exportOrderbyField = valueOfString("export_orderby_field", "id");
        exportRowidField = valueOfString("export_rowid_field", "id");
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param   art  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getColorOfArt(final String art) {
        return (art != null) ? valueOfString(String.format("%s_color", art), null) : null;
    }
    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static AlboProperties getInstance() {
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
        } catch (final Exception ex) {
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

        private static final AlboProperties INSTANCE;

        static {
            try {
                INSTANCE = getNewInstance();
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

        //~ Methods ------------------------------------------------------------

        /**
         * DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         *
         * @throws  RuntimeException  DOCUMENT ME!
         */
        public static AlboProperties getNewInstance() {
            try {
                return new AlboProperties(ServerResourcesLoader.getInstance().loadProperties(
                            SERVER_RESOURCE.getValue()));
            } catch (final Throwable ex) {
                throw new RuntimeException("Exception while initializing AlboProperties", ex);
            }
        }
    }
}
