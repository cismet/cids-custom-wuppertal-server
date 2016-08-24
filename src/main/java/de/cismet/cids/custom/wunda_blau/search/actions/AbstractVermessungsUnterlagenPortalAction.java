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

import Sirius.server.newuser.User;

import org.apache.log4j.Logger;

import org.openide.util.Exceptions;

import java.io.FileWriter;
import java.io.IOException;

import java.text.SimpleDateFormat;

import java.util.Date;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;
import de.cismet.cids.server.actions.UserAwareServerAction;

/**
 * DOCUMENT ME!
 *
 * @author   thorsten
 * @version  $Revision$, $Date$
 */
public abstract class AbstractVermessungsUnterlagenPortalAction implements UserAwareServerAction {

    //~ Static fields/initializers ---------------------------------------------

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    //~ Instance fields --------------------------------------------------------

    private final Logger LOG = Logger.getLogger(this.getClass());

    private User user;
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
}
