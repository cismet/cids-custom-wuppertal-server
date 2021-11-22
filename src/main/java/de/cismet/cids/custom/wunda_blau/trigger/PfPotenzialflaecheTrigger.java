/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.wunda_blau.trigger;

import Sirius.server.newuser.User;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.openide.util.lookup.ServiceProvider;

import java.util.Arrays;
import java.util.Map;

import de.cismet.cids.custom.utils.ByteArrayFactoryHandler;
import de.cismet.cids.custom.utils.PotenzialflaecheReportCreator;
import de.cismet.cids.custom.utils.PotenzialflaechenMapsJson;
import de.cismet.cids.custom.utils.PotenzialflaechenProperties;
import de.cismet.cids.custom.utils.WundaBlauServerResources;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.trigger.AbstractCidsTrigger;
import de.cismet.cids.trigger.CidsTrigger;
import de.cismet.cids.trigger.CidsTriggerKey;

import de.cismet.cids.utils.serverresources.JsonServerResource;
import de.cismet.cids.utils.serverresources.PropertiesServerResource;
import de.cismet.cids.utils.serverresources.ServerResourcesLoader;

import de.cismet.connectioncontext.AbstractConnectionContext;
import de.cismet.connectioncontext.ConnectionContext;
import de.cismet.connectioncontext.ConnectionContextProvider;

/**
 * DOCUMENT ME!
 *
 * @version  $Revision$, $Date$
 */
@ServiceProvider(service = CidsTrigger.class)
public class PfPotenzialflaecheTrigger extends AbstractCidsTrigger implements ConnectionContextProvider {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            PfPotenzialflaecheTrigger.class);

    private static final String DOMAIN = "WUNDA_BLAU";
    private static final String TABLE_POTENZIALFLAECHE = "pf_potenzialflaeche";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    //~ Instance fields --------------------------------------------------------

    private final ConnectionContext connectionContext = ConnectionContext.create(
            AbstractConnectionContext.Category.OTHER,
            PfPotenzialflaecheTrigger.class.getCanonicalName());
    private final PropertiesServerResource POTENZIALFLAECHEN_PROPERTIES = (PropertiesServerResource)
        WundaBlauServerResources.POTENZIALFLAECHEN_PROPERTIES.getValue();
    private final JsonServerResource POTENZIALFLAECHEN_MAPS_JSON = (JsonServerResource)
        WundaBlauServerResources.POTENZIALFLAECHEN_MAPS_JSON.getValue();

    //~ Methods ----------------------------------------------------------------

    @Override
    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public PotenzialflaechenProperties getProperties() throws Exception {
        return (PotenzialflaechenProperties)ServerResourcesLoader.getInstance().get(POTENZIALFLAECHEN_PROPERTIES);
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public PotenzialflaechenMapsJson getMapsJson() throws Exception {
        return (PotenzialflaechenMapsJson)ServerResourcesLoader.getInstance().get(POTENZIALFLAECHEN_MAPS_JSON);
    }

    /**
     * DOCUMENT ME!
     *
     * @param  config  DOCUMENT ME!
     * @param  user    DOCUMENT ME!
     */
    private void refreshMapThreaded(final PotenzialflaecheReportCreator.MapConfiguration config, final User user) {
        new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        ByteArrayFactoryHandler.getInstance()
                                .execute(
                                    getProperties().getMapFactory(),
                                    OBJECT_MAPPER.writeValueAsString(config),
                                    user,
                                    getConnectionContext());
                    } catch (final Exception ex) {
                        LOG.error(ex, ex);
                    }
                }
            }).start();
    }

    /**
     * DOCUMENT ME!
     *
     * @param   id          DOCUMENT ME!
     * @param   identifier  type DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private PotenzialflaecheReportCreator.MapConfiguration createConfigFor(final Integer id, final String identifier)
            throws Exception {
        final PotenzialflaechenProperties properties = getProperties();
        final PotenzialflaechenMapsJson.MapProperties mapProperties = getMapsJson().getMapProperties(identifier);

        final PotenzialflaecheReportCreator.MapConfiguration config =
            new PotenzialflaecheReportCreator.MapConfiguration(identifier);
        config.setPfId(id);
        config.setIds(Arrays.asList(id));
        config.setCacheDirectory(getProperties().getPictureCacheDirectory());
        config.setUseCache(Boolean.FALSE);

        config.setBbX1(properties.getHomeX1());
        config.setBbY1(properties.getHomeY1());
        config.setBbX2(properties.getHomeX2());
        config.setBbY2(properties.getHomeY2());
        config.setSrs(properties.getSrs());

        config.setWidth(mapProperties.getWidth());
        config.setHeight(mapProperties.getHeight());
        config.setMapUrl(mapProperties.getWmsUrl());
        config.setMapDpi(mapProperties.getDpi());
        config.setBuffer(mapProperties.getBuffer());
        config.setShowGeom(mapProperties.isShowGeom());
        return config;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  cidsBean  DOCUMENT ME!
     * @param  user      DOCUMENT ME!
     */
    private void refreshMaps(final CidsBean cidsBean, final User user) {
        try {
            final Map<String, PotenzialflaechenMapsJson.MapProperties> mapsProperties = getMapsJson()
                        .getMapProperties();
            for (final String identifier : mapsProperties.keySet()) {
                refreshMapThreaded(createConfigFor(cidsBean.getMetaObject().getId(), identifier), user);
            }
        } catch (final Exception ex) {
            LOG.error(ex, ex);
        }
    }

    @Override
    public void afterInsert(final CidsBean cidsBean, final User user) {
    }

    @Override
    public void afterDelete(final CidsBean cidsBean, final User user) {
    }

    @Override
    public void afterUpdate(final CidsBean cidsBean, final User user) {
    }

    @Override
    public void beforeInsert(final CidsBean cidsBean, final User user) {
    }

    @Override
    public void beforeUpdate(final CidsBean cidsBean, final User user) {
    }

    @Override
    public void beforeDelete(final CidsBean cidsBean, final User user) {
    }

    @Override
    public void afterCommittedInsert(final CidsBean cidsBean, final User user) {
        refreshMaps(cidsBean, user);
    }

    @Override
    public void afterCommittedUpdate(final CidsBean cidsBean, final User user) {
        refreshMaps(cidsBean, user);
    }

    @Override
    public void afterCommittedDelete(final CidsBean cidsBean, final User user) {
    }

    @Override
    public CidsTriggerKey getTriggerKey() {
        return new CidsTriggerKey(DOMAIN, TABLE_POTENZIALFLAECHE);
    }

    @Override
    public int compareTo(final CidsTrigger cidsTrigger) {
        return 0;
    }
}
