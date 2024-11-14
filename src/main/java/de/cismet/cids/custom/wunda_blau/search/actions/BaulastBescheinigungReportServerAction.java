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

import com.fasterxml.jackson.databind.ObjectMapper;

import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import org.apache.log4j.Logger;

import java.text.SimpleDateFormat;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;

import de.cismet.cids.custom.utils.StampedJasperReportServerAction;
import de.cismet.cids.custom.utils.WundaBlauServerResources;
import de.cismet.cids.custom.utils.berechtigungspruefung.baulastbescheinigung.BerechtigungspruefungBescheinigungGruppeInfo;

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
public class BaulastBescheinigungReportServerAction extends StampedJasperReportServerAction
        implements ConnectionContextStore,
            UserAwareServerAction,
            MetaServiceStore {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(BaulastBescheinigungReportServerAction.class);
    private static final String PARAMETER_JOBNUMBER = "JOBNUMBER";
    private static final String PARAMETER_PROJECTNAME = "PROJECTNAME";
    private static final String PARAMETER_HAS_BELASTET = "HAS_BELASTET";
    private static final String PARAMETER_HAS_BEGUENSTIGT = "HAS_BEGUENSTIGT";
    private static final String PARAMETER_FABRICATIONNOTICE = "FABRICATIONNOTICE";
    private static final String PARAMETER_FABRICATIONDATE = "FABRICATIONDATE";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static final String TASK_NAME = "baulastBescheinigungReport";

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Parameter {

        //~ Enum constants -----------------------------------------------------

        BESCHEINIGUNGGRUPPE_INFO, FABRICATION_DATE, FERTIGUNGS_VERMERK, JOB_NUMBER, PROJECT_NAME
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
            BerechtigungspruefungBescheinigungGruppeInfo bescheinigungGruppeInfo = null;
            Date fabricationDate = null;
            String fertigungsVermerk = null;
            String jobNumber = null;
            String projectName = null;
            final String anfrageSchluessel = null;

            if (params != null) {
                for (final ServerActionParameter sap : params) {
                    if (sap.getKey().equals(Parameter.BESCHEINIGUNGGRUPPE_INFO.toString())) {
                        bescheinigungGruppeInfo = OBJECT_MAPPER.readValue((String)sap.getValue(),
                                BerechtigungspruefungBescheinigungGruppeInfo.class);
                    } else if (sap.getKey().equals(Parameter.FABRICATION_DATE.toString())) {
                        fabricationDate = new Date((Long)sap.getValue());
                    } else if (sap.getKey().equals(Parameter.FERTIGUNGS_VERMERK.toString())) {
                        fertigungsVermerk = (String)sap.getValue();
                    } else if (sap.getKey().equals(Parameter.JOB_NUMBER.toString())) {
                        jobNumber = (String)sap.getValue();
                    } else if (sap.getKey().equals(Parameter.PROJECT_NAME.toString())) {
                        projectName = (String)sap.getValue();
                    }
                }
            }

            if (bescheinigungGruppeInfo != null) {
                final Collection<BerechtigungspruefungBescheinigungGruppeInfo> reportBeans = Arrays.asList(
                        new BerechtigungspruefungBescheinigungGruppeInfo[] { bescheinigungGruppeInfo });
                final JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportBeans);

                final HashMap parameters = new HashMap();
                parameters.put(PARAMETER_JOBNUMBER, jobNumber);
                parameters.put(PARAMETER_PROJECTNAME, projectName);

                parameters.put(PARAMETER_HAS_BELASTET, !bescheinigungGruppeInfo.getBaulastenBelastet().isEmpty());
                parameters.put(
                    PARAMETER_FABRICATIONDATE,
                    new SimpleDateFormat("dd.MM.yyyy").format(fabricationDate));
                parameters.put(
                    PARAMETER_HAS_BEGUENSTIGT,
                    !bescheinigungGruppeInfo.getBaulastenBeguenstigt().isEmpty());
                parameters.put(
                    PARAMETER_FABRICATIONNOTICE,
                    fertigungsVermerk);
                parameters.put(
                    "SUBREPORT_DIR",
                    DomainServerImpl.getServerProperties().getServerResourcesBasePath()
                            + "/");

                return ServerActionHelper.asyncByteArrayHelper(generateReport(parameters, dataSource), "BaulastReport.pdf");
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
        return ServerResourcesLoader.getInstance()
                    .loadJasperReport(WundaBlauServerResources.BAULASTBESCHEINIGUNG_JASPER.getValue());
    }

    @Override
    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }
}
