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

import Sirius.server.middleware.impls.domainserver.DomainServerImpl;
import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.middleware.types.MetaObject;
import Sirius.server.middleware.types.MetaObjectNode;
import Sirius.server.newuser.User;

import Sirius.util.MapImageFactoryConfiguration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import java.awt.image.BufferedImage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.OutputStream;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import javax.swing.SwingWorker;

import de.cismet.cids.custom.wunda_blau.search.actions.PotenzialflaecheReportServerAction;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.server.search.CidsServerSearch;

import de.cismet.cids.utils.serverresources.JasperReportServerResource;
import de.cismet.cids.utils.serverresources.ServerResourcesLoader;

import de.cismet.connectioncontext.ConnectionContext;
import de.cismet.connectioncontext.ConnectionContextStore;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
@Getter
public class PotenzialflaecheReportCreator {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            PotenzialflaecheReportCreator.class);

    private static final SimpleDateFormat SDF = new SimpleDateFormat("dd.MM.yyyy");

    //~ Instance fields --------------------------------------------------------

    private final CidsBean flaecheBean;
    private CidsBean templateBean;
    private final User user;
    private final MetaService metaService;
    private final ConnectionContext connectionContext;
    private final PotenzialflaechenProperties properties;
    private final PotenzialflaechenMapsJson mapsJson;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new PotenzialflaecheReportCreator object.
     *
     * @param   properties         DOCUMENT ME!
     * @param   mapsJson           DOCUMENT ME!
     * @param   flaecheBean        DOCUMENT ME!
     * @param   user               DOCUMENT ME!
     * @param   metaService        DOCUMENT ME!
     * @param   connectionContext  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public PotenzialflaecheReportCreator(final PotenzialflaechenProperties properties,
            final PotenzialflaechenMapsJson mapsJson,
            final CidsBean flaecheBean,
            final User user,
            final MetaService metaService,
            final ConnectionContext connectionContext) throws Exception {
        this.properties = properties;
        this.mapsJson = mapsJson;
        this.flaecheBean = flaecheBean;
        this.user = user;
        this.metaService = metaService;
        this.connectionContext = connectionContext;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public CidsBean getTemplateBean() {
        return templateBean;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   flaecheBean   DOCUMENT ME!
     * @param   templateBean  DOCUMENT ME!
     * @param   outputStream  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public void writeReportToOutputStream(final CidsBean flaecheBean,
            final CidsBean templateBean,
            final OutputStream outputStream) throws Exception {
        final CidsBean kategorie = (flaecheBean != null) ? (CidsBean)flaecheBean.getProperty("kampagne") : null;

        CidsBean selectedTemplateBean = null;
        if (templateBean != null) {
            selectedTemplateBean = templateBean;
        } else {
            if (kategorie != null) {
                final Collection<CidsBean> templateBeans = kategorie.getBeanCollectionProperty(
                        "n_steckbrieftemplates");
                selectedTemplateBean = ((templateBeans != null) && !templateBeans.isEmpty())
                    ? templateBeans.iterator().next() : null;
                final Integer mainSteckbriefId = (Integer)kategorie.getProperty("haupt_steckbrieftemplate_id");
                if (mainSteckbriefId != null) {
                    for (final CidsBean templateSubBean : templateBeans) {
                        if ((templateSubBean != null)
                                    && (mainSteckbriefId == templateSubBean.getMetaObject().getId())) {
                            selectedTemplateBean = templateSubBean;
                            break;
                        }
                    }
                }
            }
        }

        if (selectedTemplateBean == null) {
            throw new Exception("no template found");
        }

        final String confAttr = (String)selectedTemplateBean.getProperty("conf_attr");
        if ((confAttr != null) && !confAttr.trim().isEmpty()
                    && !((DomainServerImpl)getMetaService()).hasConfigAttr(
                        getUser(),
                        confAttr,
                        getConnectionContext())) {
            throw new Exception("kein Recht an Konfigurationsattribut " + confAttr);
        }

        final JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(Arrays.asList(
                    flaecheBean));
        final Map<String, Object> parameters = generateParams(this);
        parameters.put("PF_ID", (flaecheBean != null) ? flaecheBean.getPrimaryKeyValue() : null);
        parameters.put("SUBREPORT_DIR", DomainServerImpl.getServerProperties().getServerResourcesBasePath() + "/");
        parameters.put("CREATOR", this);

        final JasperPrint print = JasperFillManager.fillReport(getJasperReport(selectedTemplateBean),
                parameters,
                dataSource);

        JasperExportManager.exportReportToPdfStream(print, outputStream);
        outputStream.flush();
    }

    /**
     * DOCUMENT ME!
     *
     * @param   creator  flaecheBean DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private Map generateParams(final PotenzialflaecheReportCreator creator) throws Exception {
        final ConcurrentHashMap params = new ConcurrentHashMap();
        final ExecutorService calcExecutor = Executors.newFixedThreadPool(4);

        for (final PotenzialflaecheReportServerAction.Property property
                    : PotenzialflaecheReportServerAction.Property.values()) {
            final String parameterName = property.name();
            final PotenzialflaecheReportServerAction.ReportProperty reportProperty = property.getValue();

            calcExecutor.execute(new SwingWorker() {

                    @Override
                    protected Void doInBackground() throws Exception {
                        final Object object = reportProperty.calculateProperty(creator);

                        if (object instanceof Collection) {
                            final List<String> list = new ArrayList<>(((Collection)object).size());
                            for (final Object single : (Collection)object) {
                                list.add((single != null) ? String.valueOf(object) : null);
                            }
                            params.put(parameterName, String.join(", ", list));
                            params.put(String.format("%s_LIST", parameterName), object);
                        } else if (object instanceof Date) {
                            params.put(parameterName, SDF.format((Date)object));
                            params.put(String.format("%s_DATE", parameterName), object);
                        } else if (object instanceof BufferedImage) {
                            params.put(parameterName, (BufferedImage)object);
                        } else {
                            params.put(parameterName, (object != null) ? object.toString() : null);
                        }

                        return null;
                    }
                });
        }
        try {
            calcExecutor.shutdown();
            calcExecutor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (final InterruptedException ex) {
            throw new Exception("calculation of params took too long", ex);
        }

        return params;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   search  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public Collection<MetaObjectNode> executeSearch(final CidsServerSearch search) throws Exception {
        search.setUser(getUser());
        if (search instanceof ConnectionContextStore) {
            ((ConnectionContextStore)search).initWithConnectionContext(
                getConnectionContext());
        }
        final Map localServers = new HashMap<>();
        localServers.put("WUNDA_BLAU", getMetaService());
        search.setActiveLocalServers(localServers);
        return search.performServerSearch();
    }

    /**
     * DOCUMENT ME!
     *
     * @param   pfId        DOCUMENT ME!
     * @param   identifier  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private MapConfiguration getMapConfiguration(final int pfId, final String identifier) {
        final PotenzialflaechenProperties properties = getProperties();
        final PotenzialflaechenMapsJson.MapProperties mapProperties = (identifier != null)
            ? getMapsJson().getMapProperties(identifier) : null;

        final PotenzialflaecheReportCreator.MapConfiguration config =
            new PotenzialflaecheReportCreator.MapConfiguration(identifier);

        config.setPfId(pfId);
        config.setCacheDirectory(properties.getPictureCacheDirectory());
        config.setUseCache(Boolean.TRUE);

        config.setWidth((mapProperties != null) ? mapProperties.getWidth()
                                                : PotenzialflaechenMapsJson.DEFAULT_MAP_WIDTH);
        config.setHeight((mapProperties != null) ? mapProperties.getHeight()
                                                 : PotenzialflaechenMapsJson.DEFAULT_MAP_HEIGHT);
        config.setBuffer((mapProperties != null) ? mapProperties.getBuffer()
                                                 : PotenzialflaechenMapsJson.DEFAULT_BUFFER);
        config.setMapDpi((mapProperties != null) ? mapProperties.getDpi() : PotenzialflaechenMapsJson.DEFAULT_MAP_DPI);
        config.setMapUrl((mapProperties != null) ? mapProperties.getWmsUrl() : null);
        config.setIds(Arrays.asList(pfId));
        config.setShowGeom((mapProperties != null) ? mapProperties.isShowGeom() : null);
        config.setBbX1(properties.getHomeX1());
        config.setBbY1(properties.getHomeY1());
        config.setBbX2(properties.getHomeX2());
        config.setBbY2(properties.getHomeY2());
        config.setSrs(properties.getSrs());

        return config;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   identifier  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public BufferedImage loadMapFor(final String identifier) throws Exception {
        final PotenzialflaecheReportCreator.MapConfiguration config = getMapConfiguration(getFlaecheBean()
                        .getPrimaryKeyValue(),
                identifier);

        final File file = config.getFileFromCache();
        if ((file != null) && file.exists() && file.isFile()) {
            return ImageIO.read(file);
        }

        final byte[] bytes = ByteArrayFactoryHandler.getInstance()
                    .execute(getProperties().getMapFactory(),
                        new ObjectMapper().writeValueAsString(config),
                        getUser(),
                        getConnectionContext());
        return ImageIO.read(new ByteArrayInputStream(bytes));
    }

    /**
     * DOCUMENT ME!
     *
     * @param   mon  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public MetaObject getMetaObject(final MetaObjectNode mon) throws Exception {
        return metaService.getMetaObject(getUser(), mon.getObjectId(), mon.getClassId(), getConnectionContext());
    }

    /**
     * DOCUMENT ME!
     *
     * @param   templateBean  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public static JasperReport getJasperReport(final CidsBean templateBean) throws Exception {
        return ServerResourcesLoader.getInstance()
                    .loadJasperReport(new JasperReportServerResource((String)templateBean.getProperty("link")));
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class MapConfiguration extends MapImageFactoryConfiguration {

        //~ Instance fields ----------------------------------------------------

        private String identifier;

        private Integer pfId;
        private Collection<Integer> ids;
        private Boolean showGeom;
        private Integer buffer;
        private Boolean useCache;
        private String cacheDirectory;

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new MapConfiguration object.
         *
         * @param  identifier  DOCUMENT ME!
         */
        public MapConfiguration(final String identifier) {
            this.identifier = identifier;
        }

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
                return new File(
                        dir,
                        String.format(
                            "%d_%s_%s.png",
                            getPfId(),
                            getIdentifier(),
                            (((getShowGeom() != null) && getShowGeom()) ? "geom" : "nogeom")));
            }
            return null;
        }
    }
}
