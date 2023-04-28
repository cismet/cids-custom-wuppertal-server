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
import Sirius.server.newuser.User;

import org.apache.log4j.Logger;

import java.text.SimpleDateFormat;

import de.cismet.cids.server.actions.ServerActionParameter;
import de.cismet.cids.server.actions.UserAwareServerAction;

import de.cismet.connectioncontext.ConnectionContext;
import de.cismet.connectioncontext.ConnectionContextStore;

/**
 * DOCUMENT ME!
 *
 * @author   thorsten
 * @version  $Revision$, $Date$
 */
public abstract class AbstractVermessungsUnterlagenPortalAction implements UserAwareServerAction,
    MetaServiceStore,
    ConnectionContextStore {

    //~ Static fields/initializers ---------------------------------------------

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final Logger LOG = Logger.getLogger(AbstractVermessungsUnterlagenPortalAction.class);

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public static enum Parameter {

        //~ Enum constants -----------------------------------------------------

        JOB_KEY {

            @Override
            public String toString() {
                return "jobNumber";
            }
        }
    }

    //~ Instance fields --------------------------------------------------------

    private User user;
    private MetaService metaService;
    private ConnectionContext connectionContext;

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param   params  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    protected String exctractJobKey(final ServerActionParameter[] params) {
        String jobKey = null;
        if (params != null) {
            for (final ServerActionParameter param : params) {
                if (param.getKey().equals(Parameter.JOB_KEY.toString())) {
                    jobKey = (String)param.getValue();
                }
            }
        }
        return jobKey;
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
    public MetaService getMetaService() {
        return metaService;
    }

    @Override
    public void setMetaService(final MetaService metaService) {
        this.metaService = metaService;
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
