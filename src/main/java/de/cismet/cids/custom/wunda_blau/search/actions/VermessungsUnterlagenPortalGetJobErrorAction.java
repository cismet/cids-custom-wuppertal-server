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

import de.cismet.cids.custom.utils.vermessungsunterlagen.VermessungsunterlagenException;
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
public class VermessungsUnterlagenPortalGetJobErrorAction extends AbstractVermessungsUnterlagenPortalAction {

    //~ Static fields/initializers ---------------------------------------------

    public static final String TASK_NAME = "VUPgetJobErrorAction";

    private static final String RETURN = "{\"getJobErrorReturn\":{\"$value\":\"%s\"}}";

    //~ Methods ----------------------------------------------------------------

    @Override
    public Object execute(final Object body, final ServerActionParameter... params) {
        final String jobKey = exctractJobKey(params);
        final VermessungsunterlagenJob job = VermessungsunterlagenHelper.getInstance().getJob(jobKey);

        final VermessungsunterlagenException exception = job.getException();
        final String fehlermeldung = (exception != null) ? exception.getMessage() : null;

        VermessungsunterlagenHelper.getInstance().cleanUp(jobKey);

        super.executeLog(jobKey, fehlermeldung, "");
        return String.format(RETURN, fehlermeldung);
    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }
}
