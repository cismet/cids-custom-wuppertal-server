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
import Sirius.server.newuser.User;
import Sirius.server.newuser.UserServer;

import java.rmi.Naming;

import de.cismet.cids.custom.utils.vermessungsunterlagen.VermessungsunterlagenHelper;

import de.cismet.connectioncontext.ConnectionContext;
import de.cismet.connectioncontext.ConnectionContextStore;

/**
 * DOCUMENT ME!
 *
 * @author   thorsten
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = DomainServerStartupHook.class)
public class VermessungsunterlagenStartupHook extends AbstractWundaBlauStartupHook {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            VermessungsunterlagenStartupHook.class);

    //~ Methods ----------------------------------------------------------------

    @Override
    public void domainServerStarted() {
        new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        final DomainServerImpl metaService = waitForMetaService();

                        final String login_name = VermessungsunterlagenHelper.getInstance()
                                    .getProperties()
                                    .getCidsLogin();

                        final VermessungsunterlagenHelper helper = VermessungsunterlagenHelper.getInstance();

                        final Object userServer = Naming.lookup("rmi://localhost/userServer");
                        final User user = ((UserServer)userServer).getUser(
                                null,
                                null,
                                "WUNDA_BLAU",
                                login_name,
                                "");
                        helper.init(metaService, user);
                        helper.test();
                    } catch (final Exception ex) {
                        LOG.error("error while executing VermessungsunterlagenStartupHook", ex);
                    }
                }
            }).start();
    }

    @Override
    public String getDomain() {
        return "WUNDA_BLAU";
    }
}
