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

import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import de.cismet.cids.custom.utils.alkis.VermessungRissReportBean;
import de.cismet.cids.custom.utils.alkis.VermessungsRissReportHelper;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.server.actions.JasperReportServerAction;
import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;

import de.cismet.commons.utils.MultiPagePictureReader;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class VermessungsrissReportServerAction extends JasperReportServerAction {

    //~ Static fields/initializers ---------------------------------------------

    private static final String JASPER = "/de/cismet/cids/custom/wunda_blau/res/vermessungsrisse.jasper";
    public static final String TASK_NAME = "vermessungsrissReport";

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Parameter {

        //~ Enum constants -----------------------------------------------------

        JOB_NUMBER, PROJECT_NAME, RISSE_MONS, HOST
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public Object execute(final Object body, final ServerActionParameter... params) {
        String jobNumber = null;
        String projectName = null;
        Collection<MetaObjectNode> reportMons = null;
        String host = null;

        try {
            if (params != null) {
                for (final ServerActionParameter sap : params) {
                    if (sap.getKey().equals(Parameter.JOB_NUMBER.toString())) {
                        jobNumber = (String)sap.getValue();
                    } else if (sap.getKey().equals(Parameter.PROJECT_NAME.toString())) {
                        projectName = (String)sap.getValue();
                    } else if (sap.getKey().equals(Parameter.RISSE_MONS.toString())) {
                        reportMons = (Collection)sap.getValue();
                    } else if (sap.getKey().equals(Parameter.HOST.toString())) {
                        host = (String)sap.getValue();
                    }
                }
            }
            if (reportMons != null) {
                final Collection<CidsBean> selectedVermessungsrisse = new ArrayList<CidsBean>(reportMons.size());
                for (final MetaObjectNode reportMon : reportMons) {
                    final CidsBean bean = getMetaService().getMetaObject(
                                getUser(),
                                reportMon.getObjectId(),
                                reportMon.getClassId())
                                .getBean();
                    selectedVermessungsrisse.add(bean);
                }

                final Object[] tmp = VermessungsRissReportHelper.generateReportData(
                        jobNumber,
                        projectName,
                        selectedVermessungsrisse,
                        host,
                        MultiPagePictureReader.class);
                final Collection<VermessungRissReportBean> reportBeans = (Collection)tmp[0];
                final Map parameters = (Map)tmp[1];

                final JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportBeans);

                return generateReport(parameters, dataSource);
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
    protected String getJasperPath() {
        return JASPER;
    }
}
