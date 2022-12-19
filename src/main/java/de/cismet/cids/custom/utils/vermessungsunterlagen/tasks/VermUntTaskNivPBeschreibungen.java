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
package de.cismet.cids.custom.utils.vermessungsunterlagen.tasks;

import Sirius.server.middleware.impls.domainserver.DomainServerImpl;

import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import java.io.FileOutputStream;
import java.io.OutputStream;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import de.cismet.cids.custom.utils.WundaBlauServerResources;
import de.cismet.cids.custom.utils.alkis.NivellementPunktReportBean;
import de.cismet.cids.custom.utils.vermessungsunterlagen.VermessungsunterlagenHandler;
import de.cismet.cids.custom.utils.vermessungsunterlagen.exceptions.VermessungsunterlagenTaskException;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.utils.serverresources.ServerResourcesLoader;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class VermUntTaskNivPBeschreibungen extends VermUntTaskNivP {

    //~ Static fields/initializers ---------------------------------------------

    public static final String TYPE = "NivP_Beschreibungen";

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new VermUntTaskAPList object.
     *
     * @param  jobkey     DOCUMENT ME!
     * @param  nivPoints  DOCUMENT ME!
     */
    public VermUntTaskNivPBeschreibungen(final String jobkey, final Collection<CidsBean> nivPoints) {
        super(TYPE, jobkey, nivPoints);
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public void performTask() throws VermessungsunterlagenTaskException {
        OutputStream out = null;
        try {
            final String prefix = "NivP-Bericht";
            final String suffix = getJobKey().substring(getJobKey().indexOf("_") + 1, getJobKey().length());
            final String filename = getPath() + "/" + prefix + "_" + suffix.replace("/", "--") + ".pdf";
            out = new FileOutputStream(filename);
            final Map parameters = new HashMap();
            parameters.put("SUBREPORT_DIR", DomainServerImpl.getServerProperties().getServerResourcesBasePath() + "/");

            VermessungsunterlagenHandler.jasperReportDownload(
                ServerResourcesLoader.getInstance().loadJasperReport(
                    WundaBlauServerResources.NIVP_JASPER.getValue()),
                parameters,
                new JRBeanCollectionDataSource(Arrays.asList(new NivellementPunktReportBean(getNivPoints()))),
                out);
        } catch (final Exception ex) {
            final String message = "Beim Erstellen des NIVP-Berichtes kam es zu einem unerwarteten Fehler.";
            throw new VermessungsunterlagenTaskException(getType(), message, ex);
        } finally {
            VermessungsunterlagenHandler.closeStream(out);
        }
    }
}
