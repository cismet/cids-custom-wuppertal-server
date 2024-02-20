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

import java.io.ByteArrayInputStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import de.cismet.cids.custom.utils.StampedJasperReportServerAction;
import de.cismet.cids.custom.utils.WundaBlauServerResources;

import de.cismet.cids.dynamics.CidsBean;

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

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Parameter {

        //~ Enum constants -----------------------------------------------------

        MAP_IMAGE_BYTES
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    protected JasperReport getJasperReport() throws Exception {
        return ServerResourcesLoader.getInstance()
                    .loadJasperReport(WundaBlauServerResources.ALBO_VORGANG_JASPER.getValue());
    }

    @Override
    public Object execute(final Object o, final ServerActionParameter... params) {
        try {
            byte[] imageBytes = null;
            if (params != null) {
                for (final ServerActionParameter sap : params) {
                    if (sap.getKey().equals(Parameter.MAP_IMAGE_BYTES.toString())) {
                        imageBytes = (byte[])sap.getValue();
                    }
                }
            }

            final MetaObjectNode vorgangMon = (MetaObjectNode)o;

            final Map<String, Object> parameters = new HashMap<>();
            parameters.put("SUBREPORT_DIR", DomainServerImpl.getServerProperties().getServerResourcesBasePath() + "/");

            if (imageBytes != null) {
                try(final ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes)) {
                    parameters.put("MAP_IMAGE", ImageIO.read(bis));
                }
            }

            // load the vorgang bean and remove all deleted flaechen
            final CidsBean bean = getMetaService().getMetaObject(
                        getUser(),
                        vorgangMon.getObjectId(),
                        vorgangMon.getClassId(),
                        getConnectionContext())
                        .getBean();
            final List<CidsBean> flBeans = bean.getBeanCollectionProperty("arr_flaechen");

            for (final CidsBean tmpBean : new ArrayList<>(flBeans)) {
                final Boolean loeschen = (Boolean)tmpBean.getProperty("loeschen");

                if ((loeschen != null) && loeschen) {
                    flBeans.remove(tmpBean);
                }
            }

            final JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(Arrays.asList(bean));

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
