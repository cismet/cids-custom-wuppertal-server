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
package de.cismet.cids.custom.wunda_blau.search.actions;

import Sirius.server.middleware.types.MetaObject;
import Sirius.server.middleware.types.MetaObjectNode;

import Sirius.util.MapImageFactoryConfiguration;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

import net.sf.jasperreports.engine.JasperReport;

import java.awt.image.BufferedImage;

import java.io.File;

import java.util.Collection;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.server.search.CidsServerSearch;

import de.cismet.connectioncontext.ConnectionContextProvider;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public interface PotenzialflaecheReportCreator extends ConnectionContextProvider {

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Type {

        //~ Enum constants -----------------------------------------------------

        PF_ORTHO, PF_DGK,
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param   conf  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    byte[] createReport(final ReportConfiguration conf) throws Exception;

    /**
     * DOCUMENT ME!
     */
    void initMap();

    /**
     * DOCUMENT ME!
     *
     * @param   templateBean  steckbrief DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    JasperReport getJasperReport(final CidsBean templateBean) throws Exception;

    /**
     * DOCUMENT ME!
     *
     * @param   confAttr  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    String getConfAttr(final String confAttr) throws Exception;

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    CidsBean getFlaecheBean();

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    CidsBean getTemplateBean();

    /**
     * DOCUMENT ME!
     *
     * @param   search  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    Collection<MetaObjectNode> executeSearch(final CidsServerSearch search) throws Exception;

    /**
     * DOCUMENT ME!
     *
     * @param   type  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    BufferedImage loadMapFor(final Type type) throws Exception;

    /**
     * DOCUMENT ME!
     *
     * @param   mon  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    MetaObject getMetaObject(final MetaObjectNode mon) throws Exception;

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    ReportConfiguration getReportConfiguration();

    /**
     * DOCUMENT ME!
     *
     * @param   type  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    MapConfiguration getMapConfiguration(final Type type);

    //~ Inner Classes ----------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @Setter
    public static class ReportConfiguration extends MapConfiguration {

        //~ Instance fields ----------------------------------------------------

        private Integer id;
        private Integer templateId;
        private String subreportDirectory;
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @Setter
    public static class MapConfiguration extends MapImageFactoryConfiguration {

        //~ Instance fields ----------------------------------------------------

        private Type type;
        private Integer id;
        private Collection<Integer> ids;
        private Integer buffer;
        private Boolean useCache;
        private String cacheDirectory;

        //~ Methods ------------------------------------------------------------

        /**
         * DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         *
         * @throws  Exception  DOCUMENT ME!
         */
        @JsonIgnore
        public File getFileFromCache() throws Exception {
            final String dirName = getCacheDirectory();
            if (dirName != null) {
                final File dir = new File(dirName);
                return new File(dir, String.format("%d_%s.png", getId(), getType().name()));
            }
            return null;
        }
    }
}
