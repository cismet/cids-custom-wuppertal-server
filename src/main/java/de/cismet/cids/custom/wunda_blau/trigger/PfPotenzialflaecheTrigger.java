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
import java.util.Collection;

import de.cismet.cids.custom.utils.ByteArrayFactoryHandler;
import de.cismet.cids.custom.utils.WundaBlauServerResources;
import de.cismet.cids.custom.utils.properties.PotenzialflaechenProperties;
import de.cismet.cids.custom.wunda_blau.search.actions.PotenzialflaecheReportCreator;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.trigger.AbstractCidsTrigger;
import de.cismet.cids.trigger.CidsTrigger;
import de.cismet.cids.trigger.CidsTriggerKey;

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
    private static final String TABLE_KAMPAGNE = "pf_kampagne";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    //~ Instance fields --------------------------------------------------------

    private final ConnectionContext connectionContext = ConnectionContext.create(
            AbstractConnectionContext.Category.OTHER,
            PfPotenzialflaecheTrigger.class.getCanonicalName());
    private final PropertiesServerResource PSR = (PropertiesServerResource)
        WundaBlauServerResources.POTENZIALFLAECHEN_PROPERTIES.getValue();

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
    private PotenzialflaechenProperties getProperties() throws Exception {
        return (PotenzialflaechenProperties)ServerResourcesLoader.getInstance().get(PSR);
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public final Collection<String> getTriggeringTableNames() {
        return Arrays.asList(TABLE_POTENZIALFLAECHE, TABLE_KAMPAGNE);
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
     * @param   id    DOCUMENT ME!
     * @param   type  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private PotenzialflaecheReportCreator.MapConfiguration createConfigFor(final Integer id,
            final PotenzialflaecheReportCreator.Type type) throws Exception {
        final PotenzialflaecheReportCreator.MapConfiguration config =
            new PotenzialflaecheReportCreator.MapConfiguration();
        config.setId(id);
        config.setIds(Arrays.asList(id));
        config.setCacheDirectory(getProperties().getPictureCacheDirectory());
        config.setUseCache(Boolean.FALSE);
        config.setBbX1(getProperties().getHomeX1());
        config.setBbY1(getProperties().getHomeY1());
        config.setBbX2(getProperties().getHomeX2());
        config.setBbY2(getProperties().getHomeY2());
        config.setSrs(getProperties().getSrs());

        config.setType(type);
        config.setWidth(getProperties().getWidth(type));
        config.setHeight(getProperties().getHeight(type));
        config.setMapUrl(getProperties().getMapUrl(type));
        config.setMapDpi(getProperties().getMapDPI(type));
        config.setBuffer(getProperties().getBuffer(type));
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
            refreshMapThreaded(createConfigFor(
                    cidsBean.getMetaObject().getId(),
                    PotenzialflaecheReportCreator.Type.PF_DGK),
                user);
            refreshMapThreaded(createConfigFor(
                    cidsBean.getMetaObject().getId(),
                    PotenzialflaecheReportCreator.Type.PF_ORTHO),
                user);
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
        return new CidsTriggerKey(DOMAIN, CidsTriggerKey.ALL);
    }

    @Override
    public int compareTo(final CidsTrigger cidsTrigger) {
        return 0;
    }
}
