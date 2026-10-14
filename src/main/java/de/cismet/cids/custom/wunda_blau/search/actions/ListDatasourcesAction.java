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

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.util.stream.Collectors;

import de.cismet.cids.custom.utils.DatasourceExtractor;
import de.cismet.cids.custom.utils.GeneralUtils;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;

/**
 * DOCUMENT ME!
 *
 * @author   therter
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class ListDatasourcesAction implements ServerAction {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(ListDatasourcesAction.class);

    public static final String TASK_NAME = "listDatasources";

    //~ Methods ----------------------------------------------------------------

    @Override
    public Object execute(final Object body, final ServerActionParameter... saps) {
        try {
            new Thread() {

                    @Override
                    public void run() {
                        try {
                            DatasourceExtractor.extractDatasources();

                            final String afterScript = DomainServerImpl.getServerProperties()
                                        .getDatasourcechangeTriggerScriptAfter();

                            if (afterScript != null) {
                                LOG.info("process script: " + afterScript);
                                final ProcessBuilder builder = new ProcessBuilder(afterScript.trim());
                                builder.redirectErrorStream(true);
                                final Process process = builder.start();
                                
                                final String out = IOUtils.toString(new InputStreamReader(process.getInputStream()));
                                
                                final int exitCode = process.waitFor();
                                
                                if (exitCode == 0) {
                                    LOG.info("afterScript finished, output:\n" + out);
                                } else {
                                    LOG.error("afterScript failed with exit code " + exitCode + ", output:\n" + out);
                                }
//                                GeneralUtils.executeCmd(afterScript);
                            } else {
                                LOG.warn("no afterScript for datasource list found");
                            }
                        } catch (Exception e) {
                            LOG.error("Error while extracting the data sources", e);
                        }
                    }
                }.start();

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
