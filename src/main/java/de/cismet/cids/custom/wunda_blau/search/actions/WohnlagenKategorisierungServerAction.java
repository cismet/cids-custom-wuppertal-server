/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.wunda_blau.search.actions;

import Sirius.server.middleware.impls.domainserver.DomainServerImpl;
import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.middleware.interfaces.domainserver.MetaServiceStore;
import Sirius.server.middleware.types.MetaObject;
import Sirius.server.middleware.types.MetaObjectNode;
import Sirius.server.newuser.User;

import org.apache.log4j.Logger;

import java.util.Collection;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;
import de.cismet.cids.server.actions.UserAwareServerAction;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class WohnlagenKategorisierungServerAction implements ServerAction, UserAwareServerAction, MetaServiceStore {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient Logger LOG = Logger.getLogger(WohnlagenKategorisierungServerAction.class);
    public static final String TASK_NAME = "wohnlagenKategorisierung";

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum ParameterType {

        //~ Enum constants -----------------------------------------------------

        WOHNLAGEN, KATEGORIE
    }

    //~ Instance fields --------------------------------------------------------

    private User user;
    private MetaService metaService;

    //~ Methods ----------------------------------------------------------------

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }

    @Override
    public Object execute(final Object body, final ServerActionParameter... params) {
        try {
            Collection<MetaObjectNode> wohnlageNodes = null;
            MetaObjectNode kategorieNode = null;
            if (params != null) {
                for (final ServerActionParameter sap : params) {
                    if (sap.getKey().equals(ParameterType.WOHNLAGEN.toString())) {
                        wohnlageNodes = (Collection)sap.getValue();
                    } else if (sap.getKey().equals(ParameterType.KATEGORIE.toString())) {
                        kategorieNode = (MetaObjectNode)sap.getValue();
                    }
                }
            }

            if ((wohnlageNodes != null) && (kategorieNode != null)) {
                final CidsBean kategorieBean = DomainServerImpl.getServerInstance()
                            .getMetaObject(getUser(), kategorieNode.getObjectId(), kategorieNode.getClassId())
                            .getBean();
                for (final MetaObjectNode mon : wohnlageNodes) {
                    final CidsBean wohnlagenBean = DomainServerImpl.getServerInstance()
                                .getMetaObject(getUser(), mon.getObjectId(), mon.getClassId())
                                .getBean();
                    final CidsBean kategorisierungBean = CidsBean.createNewCidsBeanFromTableName(
                            "WUNDA_BLAU",
                            "WOHNLAGE_KATEGORISIERUNG");
                    kategorisierungBean.setProperty("fk_wohnlage", wohnlagenBean);
                    kategorisierungBean.setProperty("fk_kategorie", kategorieBean);
                    kategorisierungBean.setProperty("login_name", getUser().getName());
                    final MetaObject persistedMo = getMetaService().insertMetaObject(
                            getUser(),
                            kategorisierungBean.getMetaObject());
                }
            }
        } catch (final Exception ex) {
            LOG.error(ex, ex);
            return ex;
        }
        return null;
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
        return this.metaService;
    }
}
