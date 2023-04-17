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

import org.apache.log4j.Logger;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;

/**
 * DOCUMENT ME!
 *
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class StamperServerAction implements ServerAction {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(StamperServerAction.class);

    public static final String TASK_NAME = "stamper";

    //~ Methods ----------------------------------------------------------------

    @Override
    public Object execute(final Object body, final ServerActionParameter... saps) {
        if (body instanceof byte[]) {
            final byte[] bytes = (byte[])body;
            LOG.debug("The AYES have it ! the AYES...");
            return bytes;
        } else {
            LOG.debug("The NOES have it ! the NOES...");
            return null;
        }
    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }
}
