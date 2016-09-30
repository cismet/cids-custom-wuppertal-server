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
public class VermessungsUnterlagenPortalGetJobStatusAction extends AbstractVermessungsUnterlagenPortalAction {

    //~ Methods ----------------------------------------------------------------

    @Override
    public Object execute(final Object body, final ServerActionParameter... params) {
        final String jobNumber = String.valueOf(params[0].getValue());

        String status = null;
        final VermessungsunterlagenHelper helper = new VermessungsunterlagenHelper(getMetaService(), getUser());
        final VermessungsunterlagenJob job = helper.getJob(jobNumber);
        if (null != job.getStatus()) {
            switch (job.getStatus()) {
                case FINISHED: {
                    status = "OK";
                    break;
                }
                case ERROR: {
                    status = "ERROR";
                    break;
                }
                default: {
                    status = "RUNNING";
                    break;
                }
            }
        }

        final String output = "[" + status + "," + jobNumber + "]";
        super.executeLog(jobNumber, output, "");
        return "{\"getJobStatusReturn\":{\"enumJobStatus\":{\"$value\":\"" + status
                    + "\"},\"geschaeftsbuchnummer\":{\"$value\":\"" + jobNumber + "\"}}}";
    }

    @Override
    public String getTaskName() {
        return "VUPgetJobStatusAction";
    }
}
