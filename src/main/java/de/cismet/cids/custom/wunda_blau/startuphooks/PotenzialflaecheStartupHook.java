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
package de.cismet.cids.custom.wunda_blau.startuphooks;

import Sirius.server.middleware.interfaces.domainserver.DomainServerStartupHook;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.FilenameFilter;

import de.cismet.cids.custom.utils.PotenzialflaechenProperties;
import de.cismet.cids.custom.utils.WundaBlauServerResources;

import de.cismet.cids.utils.serverresources.PropertiesServerResource;
import de.cismet.cids.utils.serverresources.ServerResourcesLoader;

/**
 * DOCUMENT ME!
 *
 * @author   daniel
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = DomainServerStartupHook.class)
public class PotenzialflaecheStartupHook extends AbstractWundaBlauStartupHook {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(PotenzialflaecheStartupHook.class.getName());

    //~ Methods ----------------------------------------------------------------

    @Override
    public void domainServerStarted() {
        new Thread(new Runnable() {

                @Override
                public void run() {
                    waitForMetaService();

                    try {
                        final PotenzialflaechenProperties properties = (PotenzialflaechenProperties)
                            ServerResourcesLoader.getInstance()
                                    .get(
                                            (PropertiesServerResource)
                                            WundaBlauServerResources.POTENZIALFLAECHEN_PROPERTIES.getValue());

                        final String reportsDirectory = properties.getReportsDirectory();
                        if (reportsDirectory != null) {
                            final File reportsDirectoryFile = new File(reportsDirectory);
                            if (reportsDirectoryFile.exists() && reportsDirectoryFile.isDirectory()
                                        && reportsDirectoryFile.canWrite()) {
                                for (final File file : reportsDirectoryFile.listFiles(new FilenameFilter() {

                                                    @Override
                                                    public boolean accept(final File dir, final String name) {
                                                        return name.toLowerCase().endsWith(".pdf")
                                                            || name.toLowerCase().endsWith(".zip");
                                                    }
                                                })
                                ) {
                                    file.delete();
                                }
                                ;
                            }
                        }
                    } catch (final Exception ex) {
                        LOG.warn("Error while initializing the BerechtigungspruefungHandler !", ex);
                    }
                }
            }).start();
    }

    @Override
    public String getDomain() {
        return "WUNDA_BLAU";
    }
}
