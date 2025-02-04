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

import org.apache.log4j.Logger;

import java.io.File;
import java.io.FilenameFilter;

import de.cismet.cids.custom.utils.PotenzialflaechenProperties;
import de.cismet.cids.custom.utils.WundaBlauServerResources;

import de.cismet.cids.server.actions.DefaultServerAction;
import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;

import de.cismet.cids.utils.serverresources.PropertiesServerResource;
import de.cismet.cids.utils.serverresources.ServerResourcesLoader;

import de.cismet.connectioncontext.ConnectionContext;

/**
 * DOCUMENT ME!
 *
 * @author   therter
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class DeletePotenzialflaecheReportCacheServerAction extends DefaultServerAction {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient Logger LOG = Logger.getLogger(DeletePotenzialflaecheReportCacheServerAction.class);
    public static final String TASK_NAME = "deletePotenzialflaecheReportCache";

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Parameter {

        //~ Enum constants -----------------------------------------------------

        POTENZIALFLAECHE
    }

    //~ Instance fields --------------------------------------------------------

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    private final PropertiesServerResource POTENZIALFLAECHEN_PROPERTIES = (PropertiesServerResource)
        WundaBlauServerResources.POTENZIALFLAECHEN_PROPERTIES.getValue();

    //~ Methods ----------------------------------------------------------------

    @Override
    public void initWithConnectionContext(final ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }

    @Override
    public Object execute(final Object body, final ServerActionParameter... params) {
        try {
            Integer[] flaecheIds = null;

            if (params != null) {
                for (final ServerActionParameter sap : params) {
                    final Object value = sap.getValue();
                    if (sap.getKey().equals(Parameter.POTENZIALFLAECHE.toString())) {
                        flaecheIds = (Integer[])sap.getValue();
                    }
                }
            }

            if (flaecheIds != null) {
                final PotenzialflaechenProperties props = (PotenzialflaechenProperties)ServerResourcesLoader
                            .getInstance().get(POTENZIALFLAECHEN_PROPERTIES);
                final String cacheDirectoryString = props.getPictureCacheDirectory();

                final File cacheDirectory = new File(cacheDirectoryString);

                if (cacheDirectory.exists() && cacheDirectory.isDirectory()) {
                    for (final Integer tmpId : flaecheIds) {
                        final FilenameFilter filter = new FilenameFilter() {

                                @Override
                                public boolean accept(final File dir, final String name) {
                                    return name.startsWith(tmpId + "_");
                                }
                            };

                        for (final File tmp : cacheDirectory.listFiles(filter)) {
                            if (tmp.exists()) {
                                try {
                                    tmp.delete();
                                } catch (Exception e) {
                                    LOG.error("Cannot delete file " + tmp.getAbsolutePath(), e);
                                }
                            }
                        }
                    }
                }

                return true;
            } else {
                return false;
            }
        } catch (final Exception ex) {
            LOG.error("error while creating report", ex);
            return false;
        }
    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }

    @Override
    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }
}
