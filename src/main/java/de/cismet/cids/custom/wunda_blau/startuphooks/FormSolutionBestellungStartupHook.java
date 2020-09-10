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
import Sirius.server.middleware.types.MetaObject;
import Sirius.server.middleware.types.MetaObjectNode;
import Sirius.server.newuser.User;

import java.util.Arrays;

import de.cismet.cids.custom.utils.formsolutions.FormSolutionsBestellungHandler;

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
    public void initWithConnectionContext(final ConnectionContext connectionContext) {
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
                        setUser(FormSolutionsBestellungHandler.getFsUser());

                        final FormSolutionsBestellungHandler handler = new FormSolutionsBestellungHandler(
                                true,
                                getUser(),
                                getMetaService(),
                                getConnectionContext());
                        final MetaObject[] mos = handler.getUnfinishedBestellungen();
                        if (mos != null) {
                            for (final MetaObject mo : mos) {
                                try {
                                    redoBestellung(mo, handler);
                                } catch (final Exception ex) {
                                    LOG.error("error while retrying FS_bestellung " + mo, ex);
                                }
                            }
                        }
                        requestOpenBestellungen(handler);
                    } catch (final Exception ex) {
                        LOG.error("error while executing FormSolutionBestellungStartupHook", ex);
                    }
                }
            }).start();
    }

    /**
     * DOCUMENT ME!
     *
     * @param  mo       DOCUMENT ME!
     * @param  handler  DOCUMENT ME!
     */
    private void redoBestellung(final MetaObject mo, final FormSolutionsBestellungHandler handler) {
        final MetaObjectNode mon = new MetaObjectNode(mo.getDomain(), mo.getId(), mo.getClassID());
        handler.execute(FormSolutionsBestellungHandler.STATUS_CLOSE, false, false, false, Arrays.asList(mon));
    }

    /**
     * DOCUMENT ME!
     *
     * @param   handler  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private void requestOpenBestellungen(final FormSolutionsBestellungHandler handler) throws Exception {
        handler.fetchEndExecuteAllOpen();
    }

    @Override
    public String getDomain() {
        return "WUNDA_BLAU";
    }
}
