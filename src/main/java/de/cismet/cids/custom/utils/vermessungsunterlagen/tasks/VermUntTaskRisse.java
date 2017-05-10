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

import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.URL;

import java.util.Collection;
import java.util.Map;

import de.cismet.cids.custom.utils.WundaBlauServerResources;
import de.cismet.cids.custom.utils.alkis.ServerAlkisConf;
import de.cismet.cids.custom.utils.alkis.VermessungsRissReportHelper;
import de.cismet.cids.custom.utils.vermessungsunterlagen.VermessungsunterlagenHelper;
import de.cismet.cids.custom.utils.vermessungsunterlagen.VermessungsunterlagenTask;
import de.cismet.cids.custom.utils.vermessungsunterlagen.VermessungsunterlagenTaskRetryable;
import de.cismet.cids.custom.utils.vermessungsunterlagen.exceptions.VermessungsunterlagenTaskException;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.utils.serverresources.ServerResourcesLoader;

import de.cismet.commons.security.handler.ExtendedAccessHandler;
import de.cismet.commons.security.handler.SimpleHttpAccessHandler;

import de.cismet.commons.utils.MultiPagePictureReader;

import static de.cismet.cids.custom.utils.vermessungsunterlagen.VermessungsunterlagenHelper.closeStream;
import static de.cismet.cids.custom.utils.vermessungsunterlagen.VermessungsunterlagenHelper.downloadStream;
import static de.cismet.cids.custom.utils.vermessungsunterlagen.VermessungsunterlagenHelper.jasperReportDownload;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public abstract class VermUntTaskRisse extends VermessungsunterlagenTask implements VermessungsunterlagenTaskRetryable {

    //~ Instance fields --------------------------------------------------------

    private final Collection<CidsBean> risseBeans;
    private final String host;
    private final String auftragsnummer;
    private final String projektnummer;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new VermUntTaskRisse object.
     *
     * @param  type            DOCUMENT ME!
     * @param  jobkey          DOCUMENT ME!
     * @param  risseBeans      DOCUMENT ME!
     * @param  host            DOCUMENT ME!
     * @param  auftragsnummer  DOCUMENT ME!
     * @param  projektnummer   DOCUMENT ME!
     */
    public VermUntTaskRisse(final String type,
            final String jobkey,
            final Collection<CidsBean> risseBeans,
            final String host,
            final String auftragsnummer,
            final String projektnummer) {
        super(type, jobkey);

        this.risseBeans = risseBeans;
        this.host = host;
        this.auftragsnummer = auftragsnummer;
        this.projektnummer = projektnummer;
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public void performTask() throws VermessungsunterlagenTaskException {
        final String prefix = (ServerAlkisConf.getInstance().VERMESSUNG_HOST_BILDER.equalsIgnoreCase(host)
                ? "Vermessungsrisse-Bericht" : "Ergänzende-Dokumente-Bericht");
        final String suffix = getJobKey().substring(getJobKey().indexOf("_") + 1, getJobKey().length());
        final String filename = getPath() + "/" + prefix + "_" + suffix.replace("/", "--") + ".pdf";

        final File src = new File(VermessungsunterlagenHelper.getInstance().getProperties().getAbsPathPdfRisse());
        final File dst = new File(getPath() + "/" + src.getName());
        if (!dst.exists()) {
            try {
                FileUtils.copyFile(src, dst);
            } catch (final Exception ex) {
                final String message = "Beim Kopieren des Risse-Informations-PDFs kam es zu einem unerwarteten Fehler.";
                throw new VermessungsunterlagenTaskException(getType(), message, ex);
            }
        }

        final Object[] tmp = VermessungsRissReportHelper.getInstance()
                    .generateReportData(
                        auftragsnummer,
                        projektnummer,
                        risseBeans,
                        host,
                        MultiPagePictureReader.class);

        final Collection<CidsBean> reportBeans = (Collection)tmp[0];
        final Map parameters = (Map)tmp[1];
        final Collection<URL> additionalFilesToDownload = (Collection)tmp[2];

        final JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reportBeans);

        {
            OutputStream out = null;
            try {
                out = new FileOutputStream(filename);
                jasperReportDownload(ServerResourcesLoader.getInstance().loadJasperReport(
                        WundaBlauServerResources.VERMESSUNGSRISSE_JASPER.getValue()),
                    parameters,
                    dataSource,
                    out);
            } catch (final Exception ex) {
                final String message =
                    "Beim Erzeugen des Vermessungsrisse-Berichtes kam es zu einem unerwarteten Fehler.";
                throw new VermessungsunterlagenTaskException(getType(), message, ex);
            } finally {
                closeStream(out);
            }
        }

        final ExtendedAccessHandler extendedAccessHandler = new SimpleHttpAccessHandler();
        for (final URL additionalFileToDownload : additionalFilesToDownload) {
            final String additionalFilename = getPath() + "/"
                        + additionalFileToDownload.getFile()
                        .substring(additionalFileToDownload.getFile().lastIndexOf('/') + 1);
            final String pureAdditionalFilename = additionalFilename.substring(0, additionalFilename.lastIndexOf('.'));

            InputStream in = null;
            OutputStream out = null;
            try {
                in = extendedAccessHandler.doRequest(additionalFileToDownload);
                out = new FileOutputStream(additionalFilename);
                downloadStream(in, out);
            } catch (Exception ex) {
                LOG.warn("could not download additional File", ex);
                VermessungsunterlagenHelper.writeExceptionJson(
                    ex,
                    VermessungsunterlagenHelper.getInstance().getPath(getJobKey().replace("/", "--"))
                            + "/fehlerprotokoll_"
                            + pureAdditionalFilename
                            + ".json");
            } finally {
                closeStream(in);
                closeStream(out);
            }
        }
    }

    @Override
    protected String getSubPath() {
        return "/Risse";
    }

    @Override
    public long getMaxTotalWaitTimeMs() {
        return DEFAULT_MAX_TOTAL_WAIT_TIME_MS;
    }

    @Override
    public long getFirstWaitTimeMs() {
        return DEFAULT_FIRST_WAIT_TIME_MS;
    }

    @Override
    public double getWaitTimeMultiplicator() {
        return DEFAULT_WAIT_TIME_MULTIPLICATOR;
    }
}
