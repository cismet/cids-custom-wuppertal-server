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

import Sirius.server.middleware.impls.domainserver.DomainServerImpl;
import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.middleware.types.MetaObject;
import Sirius.server.middleware.types.MetaObjectNode;
import Sirius.server.newuser.User;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.sf.jasperreports.engine.JasperReport;

import java.awt.image.BufferedImage;

import java.io.ByteArrayInputStream;
import java.io.File;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import de.cismet.cids.custom.utils.ByteArrayFactoryHandler;
import de.cismet.cids.custom.utils.properties.PotenzialflaechenProperties;

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
public class PotenzialflaecheReportCreatorImpl extends AbstractPotenzialflaecheReportCreator {

    //~ Instance fields --------------------------------------------------------

    private final User user;
    private final MetaService metaService;
    private final ConnectionContext connectionContext;
    private final PotenzialflaechenProperties properties;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new PotenzialflaecheReportCreatorImpl object.
     *
     * @param   properties         DOCUMENT ME!
     * @param   flaecheBean        DOCUMENT ME!
     * @param   user               DOCUMENT ME!
     * @param   metaService        DOCUMENT ME!
     * @param   connectionContext  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public PotenzialflaecheReportCreatorImpl(final PotenzialflaechenProperties properties,
            final CidsBean flaecheBean,
            final User user,
            final MetaService metaService,
            final ConnectionContext connectionContext) throws Exception {
        this.properties = properties;
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
    public PotenzialflaechenProperties getProperties() {
        return properties;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public User getUser() {
        return user;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public MetaService getMetaService() {
        return metaService;
    }

    @Override
    public String getConfAttr(final String confAttr) throws Exception {
        return ((DomainServerImpl)getMetaService()).getConfigAttr(getUser(), confAttr, getConnectionContext());
    }

    @Override
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

    @Override
    public MapConfiguration getMapConfiguration(final Type type) {
        final PotenzialflaecheReportCreator.MapConfiguration config =
            new PotenzialflaecheReportCreator.MapConfiguration();
        config.setBbX1(getProperties().getHomeX1());
        config.setBbY1(getProperties().getHomeY1());
        config.setBbX2(getProperties().getHomeX2());
        config.setBbY2(getProperties().getHomeY2());
        config.setSrs(getProperties().getSrs());

        config.setType(type);
        config.setWidth(getProperties().getWidth(type));
        config.setHeight(getProperties().getHeight(type));
        config.setBuffer(getProperties().getBuffer(type));
        config.setMapUrl(getProperties().getMapUrl(type));
        config.setMapDpi(getProperties().getMapDPI(type));
        config.setId(getFlaecheBean().getMetaObject().getId());
        config.setIds(Arrays.asList(getFlaecheBean().getMetaObject().getId()));
        config.setCacheDirectory(getProperties().getPictureCacheDirectory());
        return config;
    }

    @Override
    public BufferedImage loadMapFor(final Type type) throws Exception {
        final PotenzialflaecheReportCreator.MapConfiguration config = getMapConfiguration(type);

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

    @Override
    public MetaObject getMetaObject(final MetaObjectNode mon) throws Exception {
        return metaService.getMetaObject(getUser(), mon.getObjectId(), mon.getClassId(), getConnectionContext());
    }

    @Override
    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }

    @Override
    public void initMap() {
    }

    @Override
    public JasperReport getJasperReport(final CidsBean templateBean) throws Exception {
        return ServerResourcesLoader.getInstance()
                    .loadJasperReport(new JasperReportServerResource((String)templateBean.getProperty("link")));
    }
}
