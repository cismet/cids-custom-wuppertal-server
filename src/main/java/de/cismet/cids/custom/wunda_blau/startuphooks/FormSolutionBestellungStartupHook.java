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
package de.cismet.cids.custom.wunda_blau.startuphooks;

import Sirius.server.middleware.impls.domainserver.DomainServerImpl;
import Sirius.server.middleware.interfaces.domainserver.DomainServerStartupHook;
import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.middleware.types.MetaClass;
import Sirius.server.middleware.types.MetaObject;
import Sirius.server.middleware.types.MetaObjectNode;
import Sirius.server.newuser.User;
import Sirius.server.newuser.UserServer;

import java.rmi.Naming;

import java.util.Arrays;

import de.cismet.cids.custom.utils.formsolutions.FormSolutionsProperties;
import de.cismet.cids.custom.wunda_blau.search.actions.FormSolutionServerNewStuffAvailableAction;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.server.actions.ServerActionParameter;
import de.cismet.connectioncontext.ConnectionContext;
import de.cismet.connectioncontext.ConnectionContextStore;

/**
 * DOCUMENT ME!
 *
 * @author   thorsten
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = DomainServerStartupHook.class)
public class FormSolutionBestellungStartupHook implements DomainServerStartupHook, ConnectionContextStore {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            FormSolutionBestellungStartupHook.class);

    //~ Instance fields --------------------------------------------------------

    private MetaService metaService;
    private User user;
    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    //~ Methods ----------------------------------------------------------------

    @Override
    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }

    @Override
    public void initWithConnectionContext(ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }
    
    /**
     * DOCUMENT ME!
     *
     * @param  user  DOCUMENT ME!
     */
    private void setUser(final User user) {
        this.user = user;
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

    /**
     * DOCUMENT ME!
     *
     * @param  metaService  DOCUMENT ME!
     */
    public void setMetaService(final MetaService metaService) {
        this.metaService = metaService;
    }

    @Override
    public void domainServerStarted() {
        new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        MetaService metaService = null;
                        while (metaService == null) {
                            metaService = DomainServerImpl.getServerInstance();
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ex) {
                            }
                        }
                        setMetaService(metaService);

                        final Object userServer = Naming.lookup("rmi://localhost/userServer");
                        setUser(
                            ((UserServer)userServer).getUser(
                                null,
                                null,
                                "WUNDA_BLAU",
                                FormSolutionsProperties.getInstance().getCidsLogin(),
                                FormSolutionsProperties.getInstance().getCidsPassword()));

                        final MetaObject[] mos = getUnfinishedBestellungen();
                        if (mos != null) {
                            for (final MetaObject mo : mos) {
                                try {
                                    redoBestellung(mo);
                                } catch (final Exception ex) {
                                    LOG.error("error while retrying FS_bestellung " + mo, ex);
                                }
                            }
                        }
                        requestOpenBestellungen();
                    } catch (final Exception ex) {
                        LOG.error("error while executing FormSolutionBestellungStartupHook", ex);
                    }
                }
            }).start();
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private MetaObject[] getUnfinishedBestellungen() throws Exception {
        final MetaClass mcBestellung = CidsBean.getMetaClassFromTableName(
                "WUNDA_BLAU",
                "fs_bestellung", getConnectionContext());

        final String pruefungQuery = "SELECT DISTINCT " + mcBestellung.getID() + ", "
                    + mcBestellung.getTableName() + "." + mcBestellung.getPrimaryKey() + " "
                    + "FROM " + mcBestellung.getTableName() + " "
                    + "WHERE "
                    + "  test IS NOT TRUE AND "
                    + "  postweg IS NOT TRUE AND "
                    + "  fehler IS NULL AND "
                    + "  erledigt IS NOT TRUE "
                    + ";";

        return getMetaService().getMetaObject(getUser(), pruefungQuery, getConnectionContext());
    }

    /**
     * DOCUMENT ME!
     *
     * @param  mo  DOCUMENT ME!
     */
    private void redoBestellung(final MetaObject mo) {
        final MetaObjectNode mon = new MetaObjectNode(mo.getDomain(), mo.getId(), mo.getClassID());

        final FormSolutionServerNewStuffAvailableAction action = new FormSolutionServerNewStuffAvailableAction(true);

        action.setMetaService(getMetaService());
        action.setUser(getUser());
        action.execute(
            null,
            new ServerActionParameter(
                FormSolutionServerNewStuffAvailableAction.PARAMETER_TYPE.METAOBJECTNODES.toString(),
                Arrays.asList(mon)),
            new ServerActionParameter(
                FormSolutionServerNewStuffAvailableAction.PARAMETER_TYPE.STEP_TO_EXECUTE.toString(),
                FormSolutionServerNewStuffAvailableAction.STATUS_CLOSE),
            new ServerActionParameter(
                FormSolutionServerNewStuffAvailableAction.PARAMETER_TYPE.SINGLE_STEP.toString(),
                Boolean.FALSE));
    }

    /**
     * DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private void requestOpenBestellungen() throws Exception {
        final FormSolutionServerNewStuffAvailableAction action = new FormSolutionServerNewStuffAvailableAction(true);

        action.setMetaService(getMetaService());
        action.setUser(getUser());
        action.execute(null);
    }

    @Override
    public String getDomain() {
        return "WUNDA_BLAU";
    }
}
