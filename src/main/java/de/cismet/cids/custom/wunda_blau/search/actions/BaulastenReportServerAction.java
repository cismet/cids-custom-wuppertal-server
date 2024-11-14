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
import Sirius.server.middleware.interfaces.domainserver.MetaServiceStore;
import Sirius.server.middleware.types.MetaObjectNode;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JasperReport;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import de.cismet.cids.custom.utils.StampedJasperReportServerAction;
import de.cismet.cids.custom.utils.WundaBlauServerResources;
import de.cismet.cids.custom.utils.alkis.BaulastenPictureFinder;
import de.cismet.cids.custom.utils.alkis.BaulastenReportGenerator;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionHelper;
import de.cismet.cids.server.actions.ServerActionParameter;
import de.cismet.cids.server.actions.UserAwareServerAction;

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
public class BaulastenReportServerAction extends StampedJasperReportServerAction implements ConnectionContextStore,
    UserAwareServerAction,
    MetaServiceStore {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(BaulastenReportServerAction.class);

    public static final String TASK_NAME = "baulastenReport";

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Parameter {

        //~ Enum constants -----------------------------------------------------

        BAULASTEN_MONS, FERTIGUNGS_VERMERK, JOB_NUMBER, PROJECT_NAME, TYPE,
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
        try {
            Collection<MetaObjectNode> mons = null;
            String jobNumber = null;
            String projectName = null;
            BaulastenReportGenerator.Type type = null;
            String fertigungsVermerk = null;

            if (params != null) {
                for (final ServerActionParameter sap : params) {
                    if (sap.getKey().equals(Parameter.BAULASTEN_MONS.toString())) {
                        mons = (Collection<MetaObjectNode>)sap.getValue();
                    } else if (sap.getKey().equals(Parameter.FERTIGUNGS_VERMERK.toString())) {
                        fertigungsVermerk = (String)sap.getValue();
                    } else if (sap.getKey().equals(Parameter.JOB_NUMBER.toString())) {
                        jobNumber = (String)sap.getValue();
                    } else if (sap.getKey().equals(Parameter.PROJECT_NAME.toString())) {
                        projectName = (String)sap.getValue();
                    } else if (sap.getKey().equals(Parameter.TYPE.toString())) {
                        type = (BaulastenReportGenerator.Type)sap.getValue();
                    }
                }
            }

            final Collection<CidsBean> baulstBeans = new ArrayList<>();
            for (final MetaObjectNode mon : mons) {
                final CidsBean baulastBean = DomainServerImpl.getServerInstance()
                            .getMetaObject(getUser(), mon.getObjectId(), mon.getClassId(), getConnectionContext())
                            .getBean();
                baulstBeans.add(baulastBean);
            }

            final BaulastenReportGenerator generator = new BaulastenReportGenerator(BaulastenPictureFinder
                            .getInstance());
            generator.generate(
                baulstBeans,
                type,
                jobNumber,
                projectName,
                fertigungsVermerk);

            final JRDataSource dataSource = generator.getDataSource();
            final Map parameters = generator.getParameters();
            parameters.put(
                "SUBREPORT_DIR",
                DomainServerImpl.getServerProperties().getServerResourcesBasePath()
                        + "/");
            return ServerActionHelper.asyncByteArrayHelper(generateReport(parameters, dataSource),
                    "BaulastenReport.pdf");
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
        return ServerResourcesLoader.getInstance()
                    .loadJasperReport(WundaBlauServerResources.BAULASTEN_JASPER.getValue());
    }

    @Override
    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }
}
