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
package de.cismet.cids.custom.utils;

import java.io.ByteArrayInputStream;

import de.cismet.cids.server.actions.DefaultServerAction;
import de.cismet.cids.server.actions.ServerActionHelper;
import de.cismet.cids.server.actions.ServerActionParameter;
import de.cismet.cids.server.actions.UploadableInputStream;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public abstract class StampedByteArrayServerAction extends DefaultServerAction {

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param   o     DOCUMENT ME!
     * @param   saps  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public abstract byte[] executeBeforeStamp(final Object o, final ServerActionParameter... saps) throws Exception;

    @Override
    public Object execute(final Object o, final ServerActionParameter... saps) {
        try {
            final String documentType = "action_" + getTaskName();
            final byte[] bytes = (byte[])executeBeforeStamp(o, saps);
            try(final ByteArrayInputStream bis = new ByteArrayInputStream(bytes)) {
                return ServerActionHelper.asyncByteArrayHelper(ServerStamperUtils.getInstance().stampDocument(
                            documentType,
                            bis,
                            new StamperUtils.StamperFallback() {

                                @Override
                                public UploadableInputStream createProduct() throws Exception {
                                    return new UploadableInputStream(new ByteArrayInputStream(bytes));
                                }
                            },
                            getConnectionContext()),
                        "stamped"
                                + getTaskName()
                                + "Report.pdf");
            }
        } catch (final Exception ex) {
            return ex;
        }
    }
}
