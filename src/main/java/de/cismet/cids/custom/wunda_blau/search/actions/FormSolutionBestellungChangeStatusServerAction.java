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
import Sirius.server.middleware.interfaces.domainserver.MetaServiceStore;
import Sirius.server.middleware.types.MetaObjectNode;
import Sirius.server.newuser.User;

import de.cismet.cids.custom.utils.formsolutions.FormSolutionsMySqlHelper;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;
import de.cismet.cids.server.actions.UserAwareServerAction;
import de.cismet.cids.server.connectioncontext.ServerConnectionContext;
import de.cismet.cids.server.connectioncontext.ServerConnectionContextProvider;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class FormSolutionBestellungChangeStatusServerAction implements UserAwareServerAction,
    MetaServiceStore,
    ServerConnectionContextProvider {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            FormSolutionBestellungChangeStatusServerAction.class);
    public static final String TASK_NAME = "formSolutionBestellungChangeStatus";

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum PARAMETER_TYPE {

        //~ Enum constants -----------------------------------------------------

        ERLEDIGT
    }

    //~ Instance fields --------------------------------------------------------

    private User user;
    private MetaService metaService;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new FormSolutionBestellungChangeStatusServerAction object.
     */
    public FormSolutionBestellungChangeStatusServerAction() {
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private FormSolutionsMySqlHelper getMySqlHelper() {
        return FormSolutionsMySqlHelper.getInstance();
    }

    @Override
    public Object execute(final Object body, final ServerActionParameter... params) {
        Boolean erledigt = null;

        if (body == null) {
            throw new RuntimeException("The body is missing.");
        } else if (!(body instanceof MetaObjectNode)) {
            throw new RuntimeException("Wrong type for body, have to be an MetaObjectNode.");
        }

        final MetaObjectNode mon = (MetaObjectNode)body;

        if (params != null) {
            for (final ServerActionParameter sap : params) {
                if (sap.getKey().equals(PARAMETER_TYPE.ERLEDIGT.toString())) {
                    erledigt = (Boolean)sap.getValue();
                }
            }
        }

        if (erledigt == null) {
            throw new RuntimeException("Missing Paremeter: PARAMETER_TYPE.ERLEDIGT.");
        }

        try {
            final CidsBean bestellungBean = DomainServerImpl.getServerInstance()
                        .getMetaObject(getUser(), mon.getObjectId(), mon.getClassId(), getServerConnectionContext())
                        .getBean();
            final String transid = (String)bestellungBean.getProperty("transid");
            final int status;
            final Boolean postweg = (Boolean)bestellungBean.getProperty("postweg");
            if (Boolean.TRUE.equals(postweg)) {
                if (erledigt) {
                    status = 0;
                } else {
                    status = 10;
                }
                getMySqlHelper().updateStatus(transid, status);
                bestellungBean.setProperty("erledigt", erledigt);
                bestellungBean.setProperty("fehler", null);
                DomainServerImpl.getServerInstance()
                        .updateMetaObject(getUser(), bestellungBean.getMetaObject(), getServerConnectionContext());
                return true;
            }
            return false;
        } catch (final Exception ex) {
            LOG.error(ex, ex);
            return ex;
        }
    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }

    @Override
    public User getUser() {
        return this.user;
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

    @Override
    public ServerConnectionContext getServerConnectionContext() {
        return ServerConnectionContext.create(FormSolutionBestellungChangeStatusServerAction.class.getSimpleName());
    }
}
