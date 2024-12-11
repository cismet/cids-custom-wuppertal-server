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
import Sirius.server.middleware.types.MetaObject;
import Sirius.server.middleware.types.MetaObjectNode;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import de.cismet.cids.custom.utils.PotenzialflaechenProperties;
import de.cismet.cids.custom.utils.WundaBlauServerResources;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.server.actions.DefaultServerAction;
import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionHelper;
import de.cismet.cids.server.actions.ServerActionParameter;

import de.cismet.cids.utils.serverresources.PropertiesServerResource;
import de.cismet.cids.utils.serverresources.ServerResourcesLoader;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class PfReportDownloadAction extends DefaultServerAction {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            PfReportDownloadAction.class);

    public static final String TASK_NAME = "pfJasperDownload";

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Parameter {

        //~ Enum constants -----------------------------------------------------

        MAINREPORT, REPORT_FILE
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public Object execute(final Object body, final ServerActionParameter... params) {
        try {
            MetaObjectNode mon = (MetaObjectNode)body;
            String reportFile = null;

            if (params != null) {
                for (final ServerActionParameter sap : params) {
                    if (sap.getKey().equals(Parameter.MAINREPORT.toString())) {
                        mon = (MetaObjectNode)sap.getValue();
                    } else if (sap.getKey().equals(Parameter.REPORT_FILE.toString())) {
                        reportFile = (String)sap.getValue();
                    }
                }
            }

            String fileName;
            if (mon != null) {
                final MetaObject mo = getMetaService().getMetaObject(
                        getUser(),
                        mon.getObjectId(),
                        mon.getClassId(),
                        getConnectionContext());
                final CidsBean bean = mo.getBean();
                fileName = (String)bean.getProperty("link");
            } else if (reportFile != null) {
                fileName = getProperties().getReportFile(reportFile);
            } else {
                return null;
            }
            try(final InputStream inputStream = new FileInputStream(
                                new File(
                                    DomainServerImpl.getServerProperties().getServerResourcesBasePath(),
                                    fileName.startsWith("/") ? fileName.substring(1) : fileName))) {
                return ServerActionHelper.asyncByteArrayHelper(IOUtils.toByteArray(inputStream), "PfReport.pdf");
            }
        } catch (final Exception ex) {
            LOG.info("error while jasper file", ex);
            return ex;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private PotenzialflaechenProperties getProperties() throws Exception {
        return (PotenzialflaechenProperties)ServerResourcesLoader.getInstance()
                    .get((PropertiesServerResource)WundaBlauServerResources.POTENZIALFLAECHEN_PROPERTIES.getValue());
    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }
}
