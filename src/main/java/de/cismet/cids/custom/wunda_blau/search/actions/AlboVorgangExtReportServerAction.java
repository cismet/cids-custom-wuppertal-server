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
import Sirius.server.middleware.types.MetaClass;
import Sirius.server.middleware.types.MetaObject;
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
 * @author   therter
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class AlboVorgangExtReportServerAction extends StampedJasperReportServerAction
        implements ConnectionContextStore {

    //~ Static fields/initializers ---------------------------------------------

    public static final String TASK_NAME = "alboVorgangExtReport";
    private static final String QUERY =
        "select %s, %s from albo_erhebungsklasse where legende is true  order by schluessel";
    private static final String QUERY_WZ =
        "select %s, %s from albo_wirtschaftszweig_erhebungsklasse where legende is true order by schluessel";

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
                    .loadJasperReport(WundaBlauServerResources.ALBO_VORGANG_EXT_JASPER.getValue());
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

            final MetaClass erhebungsklasse = getMetaService().getClassByTableName(
                    getUser(),
                    "albo_erhebungsklasse",
                    getConnectionContext());

            final MetaObject[] allErhebungsklassen = getMetaService().getMetaObject(
                    getUser(),
                    String.format(
                        QUERY,
                        erhebungsklasse.getID(),
                        erhebungsklasse.getPrimaryKey(),
                        getConnectionContext()));
            final List<CidsBean> erhebungsklassenBeans = new ArrayList<>();

            for (final MetaObject mo : allErhebungsklassen) {
                erhebungsklassenBeans.add(mo.getBean());
            }

            final JRBeanCollectionDataSource dataSourceEk = new JRBeanCollectionDataSource(erhebungsklassenBeans);

            parameters.put("ERHEBUNGSKLASSEN", dataSourceEk);

            final MetaClass erhebungsklasseWz = getMetaService().getClassByTableName(
                    getUser(),
                    "albo_wirtschaftszweig_erhebungsklasse",
                    getConnectionContext());

            final MetaObject[] allErhebungsklassenWz = getMetaService().getMetaObject(
                    getUser(),
                    String.format(
                        QUERY_WZ,
                        erhebungsklasseWz.getID(),
                        erhebungsklasseWz.getPrimaryKey(),
                        getConnectionContext()));
            final List<CidsBean> erhebungsklassenWzBeans = new ArrayList<>();

            for (final MetaObject mo : allErhebungsklassenWz) {
                erhebungsklassenWzBeans.add(mo.getBean());
            }

            final JRBeanCollectionDataSource dataSourceEkWz = new JRBeanCollectionDataSource(erhebungsklassenWzBeans);

            parameters.put("ERHEBUNGSKLASSEN_WZ", dataSourceEkWz);
            parameters.put("ERHEBUNGSKLASSEN_WZ_STRING", mo2String(allErhebungsklassenWz));
            parameters.put("ERHEBUNGSKLASSEN_STRING", mo2String(allErhebungsklassen));

            return generateReport(parameters, dataSource);
        } catch (final Exception ex) {
            LOG.error(ex, ex);
            return ex;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   schluessel  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String mo2String(final MetaObject[] schluessel) {
        final StringBuilder sb = new StringBuilder();

        for (final MetaObject tmp : schluessel) {
            final CidsBean bean = tmp.getBean();

            sb.append(String.valueOf(bean.getProperty("schluessel")))
                    .append(" = ")
                    .append(String.valueOf(bean.getProperty("name")))
                    .append("\n");
        }

        return sb.toString();
    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }
}
