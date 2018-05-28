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
package de.cismet.cids.custom.utils.alkis;

import Sirius.server.middleware.impls.domainserver.DomainServerImpl;

import org.apache.log4j.Logger;

import java.net.URL;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.commons.utils.MultiPagePictureReader;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class VermessungsRissReportHelper {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(VermessungsRissReportHelper.class);

    public static final String TYPE_VERMESSUNGSRISSE = "Vermessungsrisse";
    public static final String TYPE_COMPLEMENTARYDOCUMENTS = "Ergänzende Dokumente";

    private static final String PARAMETER_JOBNUMBER = "JOBNUMBER";
    private static final String PARAMETER_PROJECTNAME = "PROJECTNAME";
    private static final String PARAMETER_TYPE = "TYPE";
    private static final String PARAMETER_STARTINGPAGES = "STARTINGPAGES";
    private static final String PARAMETER_IMAGEAVAILABLE = "IMAGEAVAILABLE";
    private static final String SUBREPORT_DIR = "SUBREPORT_DIR";

    //~ Instance fields --------------------------------------------------------

    private final AlkisConf alkisConf;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new VermessungsRissReportHelper object.
     *
     * @param  alkisConf  DOCUMENT ME!
     */
    protected VermessungsRissReportHelper(final AlkisConf alkisConf) {
        this.alkisConf = alkisConf;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param   jobNumber                 DOCUMENT ME!
     * @param   projectName               DOCUMENT ME!
     * @param   selectedVermessungsrisse  DOCUMENT ME!
     * @param   host                      DOCUMENT ME!
     * @param   multiPageReaderClass      DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Object[] generateReportData(final String jobNumber,
            final String projectName,
            final Collection<CidsBean> selectedVermessungsrisse,
            final String host,
            final Class<? extends MultiPagePictureReader> multiPageReaderClass) {
        final Collection<VermessungRissImageReportBean> imageBeans = new LinkedList<VermessungRissImageReportBean>();
        final Collection<URL> additionalFilesToDownload = new LinkedList<URL>();
// Not the most elegant way, but it works. We have to calculate on which page an image will
        // appear. This can't be easily done with JasperReports. In order to let JasperReports calculate
        // which page an image appears on, we have to know how many pages the overview will take. And
        // that is not possible in JasperReports itself. Whether we evaluate the page calculation "Now"
        // - which means at the time one row is written -: Then we only get the current page count, not
        // the future page count. Or we evaluate the page calculation "Report", that means after the
        // rest of the reportwas created: Then the page count has a fix value for every row. The first
        // page can contain 27 rows, the following pages are able to hold 37 rows. The first image will
        // appear on page 2 if there are less than 27 rows to write.
        final Map startingPages = new HashMap();
        int startingPage = 2;
        if (selectedVermessungsrisse.size() > 27) {
            startingPage += Math.ceil((selectedVermessungsrisse.size() - 27D) / 37D);
        }

        final Map imageAvailable = new HashMap();

        for (final CidsBean vermessungsriss : selectedVermessungsrisse) {
            final String schluessel;
            final Integer gemarkung;
            final String flur;
            final String blatt;

            try {
                schluessel = vermessungsriss.getProperty("schluessel").toString();
                gemarkung = (Integer)vermessungsriss.getProperty("gemarkung.id");
                flur = vermessungsriss.getProperty("flur").toString();
                blatt = vermessungsriss.getProperty("blatt").toString();
            } catch (final Exception ex) {
                // TODO: User feedback?
                LOG.warn("Could not include raster document for vermessungsriss '"
                            + vermessungsriss.toJSONString(true)
                            + "'.",
                    ex);
                continue;
            }

            final StringBuilder description;
            if (host.equals(alkisConf.getVermessungHostGrenzniederschriften())) {
                description = new StringBuilder("Ergänzende Dokumente zum Vermessungsriss ");
            } else {
                description = new StringBuilder("Vermessungsriss ");
            }
            description.append(vermessungsriss.getProperty("schluessel"));
            description.append(" - ");
            description.append(vermessungsriss.getProperty("gemarkung.name"));
            description.append(" - ");
            description.append(vermessungsriss.getProperty("flur"));
            description.append(" - ");
            description.append(vermessungsriss.getProperty("blatt"));
            description.append(" - Seite ");

            final List<URL> urlList;
            // we search for reduced size images, since we need the reduced size image for the report
            if (host.equals(alkisConf.getVermessungHostGrenzniederschriften())) {
                urlList = VermessungsrissPictureFinder.getInstance()
                            .findGrenzniederschriftPicture(
                                    true,
                                    schluessel,
                                    gemarkung,
                                    flur,
                                    blatt);
            } else {
                urlList = VermessungsrissPictureFinder.getInstance()
                            .findVermessungsrissPicture(
                                    true,
                                    schluessel,
                                    gemarkung,
                                    flur,
                                    blatt);
            }

            if ((urlList == null) || urlList.isEmpty()) {
                LOG.info("No document URLS found for the Vermessungsriss report");
            }
            boolean isOfReducedSize = false;
            MultiPagePictureReader reader = null;
            int pageCount = 0;
            final StringBuilder fileReference = new StringBuilder();
            if (urlList != null) {
                for (final URL url : urlList) {
                    try {
                        if (url.toString().contains("_rs")) {
                            isOfReducedSize = true;
                        }
                        reader = multiPageReaderClass.getConstructor(URL.class, boolean.class, boolean.class)
                                    .newInstance(url, false, false);
                        pageCount = reader.getNumberOfPages();
                        // when a reduced size image was found we download the original file as jpg also
                        if (isOfReducedSize) {
                            additionalFilesToDownload.add(new URL(
                                    url.toString().replaceAll("_rs", "")));
                        }

                        String path = url.getPath();
                        path = path.substring(path.lastIndexOf('/') + 1);
                        fileReference.append(" (");
                        fileReference.append(path);
                        fileReference.append(')');
                        break;
                    } catch (final Exception ex) {
                        LOG.warn("Could not read document from URL '" + url.toExternalForm()
                                    + "'. Skipping this url.",
                            ex);
                    }
                }
            }

            imageAvailable.put(vermessungsriss.getProperty("id"), reader != null);

            if (reader == null) {
                // Couldn't open any image.
                continue;
            }

            for (int i = 0; i < pageCount; i++) {
                imageBeans.add(new VermessungRissImageReportBean(
                        description.toString()
                                + (i + 1)
                                + fileReference.toString(),
                        host,
                        schluessel,
                        gemarkung,
                        flur,
                        blatt,
                        i,
                        reader));
            }

            String startingPageString = Integer.toString(startingPage);
            if (isOfReducedSize) {
                startingPageString = startingPageString.concat("*");
            }

            startingPages.put(vermessungsriss.getProperty("id"), startingPageString);
            startingPage += pageCount;
        }

        final String type = host.equals(alkisConf.getVermessungHostGrenzniederschriften()) ? TYPE_COMPLEMENTARYDOCUMENTS
                                                                                           : TYPE_VERMESSUNGSRISSE;

        final HashMap parameters = new HashMap();
        parameters.put(PARAMETER_JOBNUMBER, jobNumber);
        parameters.put(PARAMETER_PROJECTNAME, projectName);
        parameters.put(PARAMETER_TYPE, type);
        parameters.put(PARAMETER_STARTINGPAGES, startingPages);
        parameters.put(PARAMETER_IMAGEAVAILABLE, imageAvailable);
        parameters.put(SUBREPORT_DIR, DomainServerImpl.getServerProperties().getServerResourcesBasePath() + "/");

        final Collection<VermessungRissReportBean> reportBeans = new LinkedList<VermessungRissReportBean>();
        reportBeans.add(new VermessungRissReportBean(selectedVermessungsrisse, imageBeans));

        return new Object[] { reportBeans, parameters, additionalFilesToDownload };
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static VermessungsRissReportHelper getInstance() {
        return LazyInitialiser.INSTANCE;
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    private static final class LazyInitialiser {

        //~ Static fields/initializers -----------------------------------------

        private static final VermessungsRissReportHelper INSTANCE = new VermessungsRissReportHelper(ServerAlkisConf
                        .getInstance());

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new LazyInitialiser object.
         */
        private LazyInitialiser() {
        }
    }
}
