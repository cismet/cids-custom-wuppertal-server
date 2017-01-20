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
import de.cismet.cids.custom.utils.vermessungsunterlagen.VermessungsunterlagenJob;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;

/**
 * DOCUMENT ME!
 *
 * @author   thorsten
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class VermessungsUnterlagenPortalGetJobResultAction extends AbstractVermessungsUnterlagenPortalAction {

    //~ Static fields/initializers ---------------------------------------------

    public static final String TASK_NAME = "VUPgetJobResultAction";

    private static final String RETURN = "{\"getJobResultReturn\":{\"$value\":\"%s\"}}";

    //~ Methods ----------------------------------------------------------------

    @Override
    public Object execute(final Object body, final ServerActionParameter... params) {
        final String jobKey = exctractJobKey(params);
        final VermessungsunterlagenJob job = VermessungsunterlagenHelper.getInstance().getJob(jobKey);

        final String ftpZipPath = job.getFtpZipPath();

        VermessungsunterlagenHelper.getInstance().cleanUp(jobKey);

        super.executeLog(jobKey, ftpZipPath, "");
        return String.format(RETURN, ftpZipPath);
    }

    @Override
    public String getTaskName() {
        return "VUPgetJobResultAction";
    }
}
