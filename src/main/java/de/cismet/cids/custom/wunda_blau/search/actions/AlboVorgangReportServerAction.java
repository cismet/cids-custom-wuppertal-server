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

import Sirius.server.middleware.impls.domainserver.DomainServerImpl;
import Sirius.server.middleware.types.MetaObjectNode;

import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import de.cismet.cids.custom.utils.StampedJasperReportServerAction;
import de.cismet.cids.custom.utils.WundaBlauServerResources;
import de.cismet.cids.server.actions.ServerAction;

import de.cismet.cids.server.actions.ServerActionParameter;

import de.cismet.cids.utils.serverresources.ServerResourcesLoader;

import de.cismet.connectioncontext.ConnectionContextStore;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class AlboVorgangReportServerAction extends StampedJasperReportServerAction implements ConnectionContextStore {

    //~ Static fields/initializers ---------------------------------------------

    public static final String TASK_NAME = "alboVorgangReport";

    //~ Methods ----------------------------------------------------------------

    @Override
    protected JasperReport getJasperReport() throws Exception {
        return ServerResourcesLoader.getInstance()
                    .loadJasperReport(WundaBlauServerResources.ALBO_VORGANG_JASPER.getValue());
    }

    @Override
    public Object execute(final Object o, final ServerActionParameter... saps) {
        try {
            final MetaObjectNode vorgangMon = (MetaObjectNode)o;

            final Map<String, Object> parameters = new HashMap<>();
            parameters.put("SUBREPORT_DIR", DomainServerImpl.getServerProperties().getServerResourcesBasePath() + "/");

            final JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(Arrays.asList(
                        getMetaService().getMetaObject(
                            getUser(),
                            vorgangMon.getObjectId(),
                            vorgangMon.getClassId(),
                            getConnectionContext()).getBean()));

            return generateReport(parameters, dataSource);
        } catch (final Exception ex) {
            LOG.error(ex, ex);
            return ex;
        }
    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }
}
