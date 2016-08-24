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
public class VermessungsUnterlagenPortalGetJobResultAction extends AbstractVermessungsUnterlagenPortalAction {

    //~ Methods ----------------------------------------------------------------

    @Override
    public Object execute(final Object body, final ServerActionParameter... params) {
        final String jobNumber = String.valueOf(params[0].getValue());
        final String output = "4712-test.zip";
        super.executeLog(jobNumber, output, "");
        return "{\"getJobResultReturn\":{\"$value\":\"" + output + "\"}}";
    }

    @Override
    public String getTaskName() {
        return "VUPgetJobResultAction";
    }
}
