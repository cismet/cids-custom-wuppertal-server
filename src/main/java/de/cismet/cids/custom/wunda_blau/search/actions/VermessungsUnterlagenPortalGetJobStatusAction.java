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

import org.openide.util.Exceptions;

import java.io.IOException;

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
        final String status = "OK";
        final String geschaeftsbuchnummer = "4711-test-geschaeftsbuchnummer-from-java-action@"
                    + System.currentTimeMillis();
        final String output = "[" + status + "," + geschaeftsbuchnummer + "]";
        super.executeLog(jobNumber, output, "");
        return "{\"getJobStatusReturn\":{\"enumJobStatus\":{\"$value\":\"" + status
                    + "\"},\"geschaeftsbuchnummer\":{\"$value\":\"" + geschaeftsbuchnummer + "\"}}}";
    }

    @Override
    public String getTaskName() {
        return "VUPgetJobStatusAction";
    }
}
