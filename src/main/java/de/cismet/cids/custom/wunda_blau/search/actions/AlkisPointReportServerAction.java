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

import Sirius.server.middleware.types.MetaObjectNode;

import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import de.cismet.cids.custom.utils.alkis.AlkisPointReportBean;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.server.actions.JasperReportServerAction;
import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;

import de.cismet.cids.utils.serverresources.CachedServerResourcesLoader;
import de.cismet.cids.utils.serverresources.JasperReportServerResources;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class AlkisPointReportServerAction extends JasperReportServerAction {

    //~ Static fields/initializers ---------------------------------------------

    public static final JasperReport JASPER;

    static {
        JasperReport report = null;
        try {
            report = CachedServerResourcesLoader.getInstance()
                        .getJasperReportResource(JasperReportServerResources.AP_MAPS_JASPER);
        } catch (final Exception ex) {
            LOG.error("Error while loading " + JasperReportServerResources.AP_MAPS_JASPER, ex);
        }
        JASPER = report;
    }

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

    //~ Methods ----------------------------------------------------------------

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
                final Collection<CidsBean> cidsBeans = new ArrayList<CidsBean>(reportMons.size());
                for (final MetaObjectNode reportMon : reportMons) {
                    final CidsBean cidsBean = getMetaService().getMetaObject(
                                getUser(),
                                reportMon.getObjectId(),
                                reportMon.getClassId())
                                .getBean();
                    cidsBeans.add(cidsBean);
                }
                final Collection<AlkisPointReportBean> reportBeans = new ArrayList<AlkisPointReportBean>(
                        reportMons.size());
                reportBeans.add(new AlkisPointReportBean(cidsBeans));
                final JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportBeans);

                return generateReport(new HashMap<String, Object>(), dataSource);
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
    protected JasperReport getJasperReport() {
        return JASPER;
    }
}
