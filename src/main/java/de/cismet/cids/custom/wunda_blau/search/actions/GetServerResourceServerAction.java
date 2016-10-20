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

import de.cismet.cids.custom.utils.WundaBlauServerResources;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;

import de.cismet.cids.utils.serverresources.CachedServerResourcesLoader;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class GetServerResourceServerAction implements ServerAction {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            GetServerResourceServerAction.class);

    public static final String TASK_NAME = "getServerResource";

    //~ Methods ----------------------------------------------------------------

    @Override
    public Object execute(final Object body, final ServerActionParameter... params) {
        try {
            final WundaBlauServerResources serverResource = (WundaBlauServerResources)body;

            final String value = serverResource.getValue();
            final CachedServerResourcesLoader loader = CachedServerResourcesLoader.getInstance();
            switch (serverResource.getType()) {
                case JASPER_REPORT: {
                    return loader.getJasperReportResource(value);
                }
                case TEXT: {
                    return loader.getTextResource(value);
                }
                case BINARY: {
                    return loader.getBinaryResource(value);
                }
                default: {
                    throw new Exception("unknown serverResource type");
                }
            }
        } catch (final Exception ex) {
            LOG.info("error while getting ServerResource " + body + ". Returning exception", ex);
            return ex;
        }
    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }
}
