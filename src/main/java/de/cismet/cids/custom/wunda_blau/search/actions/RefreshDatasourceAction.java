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
import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.middleware.interfaces.domainserver.MetaServiceStore;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileWriter;

import de.cismet.cids.custom.utils.WundaBlauServerResources;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;

/**
 * DOCUMENT ME!
 *
 * @author   therter
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class RefreshDatasourceAction implements ServerAction {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(RefreshDatasourceAction.class);

    public static final String TASK_NAME = "refreshDatasourceServer";

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum ParameterType {

        //~ Enum constants -----------------------------------------------------

        SERVER_DOCUMENT
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public Object execute(final Object body, final ServerActionParameter... params) {
        try {
            String documentContent = null;

            for (final ServerActionParameter param : params) {
                if (RefreshDatasourceAction.ParameterType.SERVER_DOCUMENT.toString().equals(
                                param.getKey().toString())) {
                    documentContent = (String)param.getValue();
                }
            }

            if (documentContent != null) {
                final String basePath = DomainServerImpl.getServerProperties().getServerResourcesBasePath();
                final String resourcePath = WundaBlauServerResources.DATASOURCES_CAPABILITYLIST_TEXT.getValue()
                            .getPath();
                final File f = new File(basePath + resourcePath);

                final FileWriter fr = new FileWriter(f);
                fr.write(documentContent);
                fr.close();
            }

            return true;
        } catch (Exception e) {
            LOG.error("Error while extracting the data sources");
            return false;
        }
    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }
}
