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

import de.cismet.cids.custom.utils.vermessungsunterlagen.VermessungsunterlagenHelper;

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

    //~ Static fields/initializers ---------------------------------------------

    public static final String TASK_NAME = "VUPexecuteJobAction";

    private static final String RETURN = "{\"return\":{\"$value\":\"%s\"}}";

    //~ Methods ----------------------------------------------------------------

    @Override
    public Object execute(final Object body, final ServerActionParameter... params) {
        final String jsonBody = new String((byte[])body);
        final String jobKey = VermessungsunterlagenHelper.getInstance().createJob(jsonBody);

        super.executeLog("[jsonObject]", jobKey, jsonBody);
        return String.format(RETURN, jobKey);
    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }
}
