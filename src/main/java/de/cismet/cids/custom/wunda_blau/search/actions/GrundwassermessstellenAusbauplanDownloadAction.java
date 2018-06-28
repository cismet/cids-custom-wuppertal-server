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

import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.middleware.interfaces.domainserver.MetaServiceStore;
import Sirius.server.middleware.types.MetaClass;
import Sirius.server.middleware.types.MetaObject;
import Sirius.server.newuser.User;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;

import java.net.URL;

import java.util.Properties;

import de.cismet.cids.custom.utils.WundaBlauServerResources;
import de.cismet.cids.custom.utils.vermessungsunterlagen.GrundwassermessstellenProperties;
import de.cismet.cids.custom.utils.vermessungsunterlagen.VermessungsunterlagenHelper;
import de.cismet.cids.custom.utils.vermessungsunterlagen.VermessungsunterlagenProperties;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;
import de.cismet.cids.server.actions.UserAwareServerAction;

import de.cismet.cids.utils.serverresources.ServerResourcesLoader;

import de.cismet.commons.security.WebDavClient;

import de.cismet.connectioncontext.ConnectionContext;
import de.cismet.connectioncontext.ConnectionContextStore;

/**
 * DOCUMENT ME!
 *
 * @author   thorsten
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class GrundwassermessstellenAusbauplanDownloadAction implements ConnectionContextStore,
    UserAwareServerAction,
    MetaServiceStore {

    //~ Static fields/initializers ---------------------------------------------

    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            GrundwassermessstellenAusbauplanDownloadAction.class);
    public static final String TASK_NAME = "GrundwassermessstellenAusbauplanDownloadAction";
    private static final String TABLE_GRUNDWASSERMESSSTELLE = "grundwassermessstelle";

    //~ Instance fields --------------------------------------------------------

    private GrundwassermessstellenProperties properties;

    private MetaService metaService;
    private User user;
    private ConnectionContext connectionContext;

    //~ Methods ----------------------------------------------------------------

    @Override
    public Object execute(final Object body, final ServerActionParameter... params) {
        final Integer id = (Integer)body;
        InputStream inputStream = null;
        try {
            final MetaClass metaClass = getMetaService().getClassByTableName(
                    getUser(),
                    TABLE_GRUNDWASSERMESSSTELLE,
                    getConnectionContext());
            final MetaObject metaObject = getMetaService().getMetaObject(
                    getUser(),
                    id,
                    metaClass.getID(),
                    getConnectionContext());
            final CidsBean messstelleBean = metaObject.getBean();
            final CidsBean ausbauplanBean = (CidsBean)messstelleBean.getProperty("ausbauplan");

            final String host = getProperties().getWebDavHost();
            final String path = getProperties().getWebDavPath();
            final String login = getProperties().getWebDavLogin();
            final String pass = getProperties().getWebDavPass();
            final String fileName = (String)ausbauplanBean.getProperty("name");

            final WebDavClient webdavclient = new WebDavClient(
                    null,
                    login,
                    pass,
                    false);

            final String url = host + "/" + ((path.isEmpty() ? "" : ("/" + path)) + fileName);
            inputStream = webdavclient.getInputStream(url);
            return IOUtils.toByteArray(inputStream);
        } catch (final Exception ex) {
            return new Exception("Fehler beim Herunterladen der Zip-Datei.", ex);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }
    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private GrundwassermessstellenProperties getProperties() {
        if (this.properties == null) {
            final Properties properties = null;
            try {
                this.properties = new GrundwassermessstellenProperties(ServerResourcesLoader.getInstance()
                                .loadProperties(WundaBlauServerResources.GRUNDWASSERMESSSTELLEN_PROPERTIES.getValue()));
            } catch (final Exception ex) {
                LOG.error("VermessungsunterlagenHelper could not load the properties", ex);
            }
        }
        return this.properties;
    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }

    @Override
    public User getUser() {
        return user;
    }

    @Override
    public void setUser(final User user) {
        this.user = user;
    }

    @Override
    public void setMetaService(final MetaService metaService) {
        this.metaService = metaService;
    }

    @Override
    public MetaService getMetaService() {
        return metaService;
    }

    @Override
    public void initWithConnectionContext(final ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }

    @Override
    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }
}
