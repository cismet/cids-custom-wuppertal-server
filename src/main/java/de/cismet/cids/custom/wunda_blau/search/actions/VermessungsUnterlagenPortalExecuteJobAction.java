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

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;

/**
 * DOCUMENT ME!
 *
 * @author   thorsten
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class VermessungsUnterlagenPortalExecuteJobAction extends AbstractVermessungsUnterlagenPortalAction {

    //~ Methods ----------------------------------------------------------------

    @Override
    public Object execute(final Object body, final ServerActionParameter... params) {
        final String jobNumber = "1234567890-" + System.currentTimeMillis();
        final String jsonBody = new String((byte[])body);
        super.executeLog("[jsonObject]", jobNumber, jsonBody);
        return "{\"executeJobReturn\":{\"$value\":\"" + jobNumber + "\"}}";
    }

    @Override
    public String getTaskName() {
        return "VUPexecuteJobAction";
    }
}
