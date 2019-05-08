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

import net.sf.jasperreports.engine.JRDataSource;

import java.io.ByteArrayInputStream;

import java.util.Map;

import de.cismet.cids.server.actions.JasperReportServerAction;

import de.cismet.connectioncontext.ConnectionContext;
import de.cismet.connectioncontext.ConnectionContextStore;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public abstract class StampedJasperReportServerAction extends JasperReportServerAction
        implements ConnectionContextStore {

    //~ Instance fields --------------------------------------------------------

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    //~ Methods ----------------------------------------------------------------

    @Override
    public void initWithConnectionContext(final ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }

    @Override
    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }

    @Override
    protected byte[] generateReport(final Map<String, Object> parameters, final JRDataSource dataSource)
            throws Exception {
        final String documentType = "action_" + getTaskName();
        final byte[] bytes = super.generateReport(parameters, dataSource);
        try(final ByteArrayInputStream bis = new ByteArrayInputStream(bytes)) {
            return ServerStamperUtils.getInstance()
                        .stampDocument(documentType, bis, new StamperUtils.StamperFallback() {

                                @Override
                                public byte[] createProduct() throws Exception {
                                    return bytes;
                                }
                            }, getConnectionContext());
        }
    }
}
