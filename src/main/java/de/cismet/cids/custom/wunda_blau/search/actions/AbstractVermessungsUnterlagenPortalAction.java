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

import java.io.FileWriter;
import java.io.IOException;

import java.text.SimpleDateFormat;

import java.util.Date;

import de.cismet.cids.server.actions.ServerActionParameter;
import de.cismet.cids.server.actions.UserAwareServerAction;

/**
 * DOCUMENT ME!
 *
 * @author   thorsten
 * @version  $Revision$, $Date$
 */
public abstract class AbstractVermessungsUnterlagenPortalAction implements UserAwareServerAction, MetaServiceStore {

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
    private final FileWriter fw;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new VermessungsUnterlagenPortalExecuteJobAction object.
     */
    public AbstractVermessungsUnterlagenPortalAction() {
        FileWriter tmpfw = null;
        try {
            tmpfw = new FileWriter("VermessungsUnterlagenPortal.log", true);
        } catch (final IOException ex) {
            LOG.error(ex, ex);
        }
        fw = tmpfw;
    }

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

    /**
     * DOCUMENT ME!
     *
     * @param  in    DOCUMENT ME!
     * @param  out   DOCUMENT ME!
     * @param  body  DOCUMENT ME!
     */
    public void executeLog(final String in, final String out, final String body) {
        if (fw != null) {
            final String userText = (getUser().getName()
                            + ((getUser().getUserGroup() != null) ? ("@" + getUser().getUserGroup().getName()) : ""));

            final String text = SDF.format(new Date()) + " (" + userText + ") " + this.getTaskName() + " in-->:" + in
                        + "; out:" + out + "-->";
            final String jsonBody = "";

            LOG.info(text + "\nbody:\n" + body);
            try {
                fw.write(text + "\n");
                fw.flush();
            } catch (final IOException ex) {
                LOG.error(ex, ex);
            }
        }
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
}
