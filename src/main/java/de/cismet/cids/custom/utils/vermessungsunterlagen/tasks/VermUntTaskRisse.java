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

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.URL;

import java.util.Collection;
import java.util.Map;

import de.cismet.cids.custom.utils.alkis.AlkisConstants;
import de.cismet.cids.custom.utils.alkis.VermessungsRissReportHelper;
import de.cismet.cids.custom.utils.vermessungsunterlagen.VermessungsunterlagenHelper;
import de.cismet.cids.custom.utils.vermessungsunterlagen.VermessungsunterlagenTask;
import de.cismet.cids.custom.wunda_blau.search.actions.VermessungsrissReportServerAction;

import de.cismet.cids.dynamics.CidsBean;

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
public abstract class VermUntTaskRisse extends VermessungsunterlagenTask {

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
    public void performTask() throws Exception {
        final String filename = getPath() + "/"
                    + (AlkisConstants.COMMONS.VERMESSUNG_HOST_BILDER.equalsIgnoreCase(host) ? "vermriss" : "ergdok")
                    + ".pdf";

        final Object[] tmp = VermessungsRissReportHelper.generateReportData(
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
                jasperReportDownload(VermessungsrissReportServerAction.JASPER, parameters, dataSource, out);
            } finally {
                closeStream(out);
            }
        }

        final String filePath = filename.substring(0, filename.lastIndexOf('/'));

        final ExtendedAccessHandler extendedAccessHandler = new SimpleHttpAccessHandler();
        for (final URL additionalFileToDownload : additionalFilesToDownload) {
            final String additionalFilename = filePath
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
                    VermessungsunterlagenHelper.getPath(getJobKey())
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
}
