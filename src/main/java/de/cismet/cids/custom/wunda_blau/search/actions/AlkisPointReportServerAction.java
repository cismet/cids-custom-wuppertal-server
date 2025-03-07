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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import de.cismet.cids.custom.utils.StampedJasperReportServerAction;
import de.cismet.cids.custom.utils.WundaBlauServerResources;
import de.cismet.cids.custom.utils.alkis.AlkisPointReportBean;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionHelper;
import de.cismet.cids.server.actions.ServerActionParameter;

import de.cismet.cids.utils.serverresources.ServerResourcesLoader;

import de.cismet.connectioncontext.ConnectionContext;
import de.cismet.connectioncontext.ConnectionContextStore;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class AlkisPointReportServerAction extends StampedJasperReportServerAction implements ConnectionContextStore {

    //~ Static fields/initializers ---------------------------------------------

    public static final String TASK_NAME = "alkisPointReport";

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Parameter {

        //~ Enum constants -----------------------------------------------------

        POINT_MONS
    }

    //~ Instance fields --------------------------------------------------------

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    //~ Methods ----------------------------------------------------------------

    @Override
    public void initWithConnectionContext(final ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }

    @Override
    public Object execute(final Object body, final ServerActionParameter... params) {
        Collection<MetaObjectNode> reportMons = null;

        try {
            if (params != null) {
                for (final ServerActionParameter sap : params) {
                    if (sap.getKey().equals(Parameter.POINT_MONS.toString())) {
                        reportMons = (Collection)sap.getValue();
                    }
                }
            }
            if (reportMons != null) {
                final Collection<CidsBean> cidsBeans = new ArrayList<>(reportMons.size());
                for (final MetaObjectNode reportMon : reportMons) {
                    final CidsBean cidsBean = getMetaService().getMetaObject(
                                getUser(),
                                reportMon.getObjectId(),
                                reportMon.getClassId(),
                                getConnectionContext())
                                .getBean();
                    cidsBeans.add(cidsBean);
                }
                final Collection<AlkisPointReportBean> reportBeans = new ArrayList<>(
                        reportMons.size());
                reportBeans.add(new AlkisPointReportBean(cidsBeans));
                final JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportBeans);

                final Map<String, Object> parameters = new HashMap<>();
                parameters.put(
                    "SUBREPORT_DIR",
                    DomainServerImpl.getServerProperties().getServerResourcesBasePath()
                            + "/");

                return ServerActionHelper.asyncByteArrayHelper(generateReport(parameters, dataSource),
                        "PunktReport.pdf");
            } else {
                return null;
            }
        } catch (final Exception ex) {
            LOG.error(ex, ex);
            return ex;
        }
    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }

    @Override
    protected JasperReport getJasperReport() throws Exception {
        return ServerResourcesLoader.getInstance().loadJasperReport(WundaBlauServerResources.APMAPS_JASPER.getValue());
    }

    @Override
    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }
}
