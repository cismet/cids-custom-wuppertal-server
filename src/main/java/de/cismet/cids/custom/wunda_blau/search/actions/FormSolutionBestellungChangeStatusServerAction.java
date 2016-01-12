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

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class FormSolutionBestellungChangeStatusServerAction implements UserAwareServerAction, MetaServiceStore {

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

        METAOBJECTNODE, ERLEDIGT
    }

    //~ Instance fields --------------------------------------------------------

    private User user;
    private MetaService metaService;
    private final FormSolutionsMySqlHelper mySqlHelper;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new FormSolutionBestellungChangeStatusServerAction object.
     */
    public FormSolutionBestellungChangeStatusServerAction() {
        mySqlHelper = FormSolutionsMySqlHelper.getInstance();
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public Object execute(final Object body, final ServerActionParameter... params) {
        MetaObjectNode mon = null;
        Boolean erledigt = null;
        if (params != null) {
            for (final ServerActionParameter sap : params) {
                if (sap.getKey().equals(PARAMETER_TYPE.METAOBJECTNODE.toString())) {
                    mon = (MetaObjectNode)sap.getValue();
                } else if (sap.getKey().equals(PARAMETER_TYPE.ERLEDIGT.toString())) {
                    erledigt = (Boolean)sap.getValue();
                }
            }
        }
        if ((mon != null) && (erledigt != null)) {
            try {
                final CidsBean bestellungBean = DomainServerImpl.getServerInstance()
                            .getMetaObject(getUser(), mon.getObjectId(), mon.getClassId())
                            .getBean();
                final String transid = (String)bestellungBean.getProperty("transid");
                final int status;
                final Boolean postweg = (Boolean)bestellungBean.getProperty("postweg");
                if (erledigt) {
                    status = 0;
                } else if (Boolean.TRUE.equals(postweg)) {
                    status = 2;
                } else {
                    status = 1;
                }
                mySqlHelper.updateMySQL(transid, status);
                bestellungBean.setProperty("erledigt", erledigt);
                DomainServerImpl.getServerInstance().updateMetaObject(getUser(), bestellungBean.getMetaObject());
                return true;
            } catch (final Exception ex) {
                LOG.error(ex, ex);
            }
        }
        return false;
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
}
