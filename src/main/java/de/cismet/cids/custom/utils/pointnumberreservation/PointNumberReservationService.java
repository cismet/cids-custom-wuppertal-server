/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.utils.pointnumberreservation;

import Sirius.server.middleware.impls.domainserver.DomainServerImpl;
import Sirius.server.property.ServerProperties;

import de.aed_sicad.namespaces.svr.AuftragsManager;
import de.aed_sicad.namespaces.svr.AuftragsManagerLocator;
import de.aed_sicad.namespaces.svr.AuftragsManagerSoap;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import java.net.URL;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import de.cismet.cids.utils.serverresources.CachedServerResourcesLoader;
import de.cismet.cids.utils.serverresources.TextServerResources;

/**
 * DOCUMENT ME!
 *
 * @author   daniel
 * @version  $Revision$, $Date$
 */
public class PointNumberReservationService {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            PointNumberReservationService.class);
    private static final String AUFTRAGS_NUMMER = "ANR";
    private static final String NUMMERIERUNGS_BEZIRK = "NBZ";
    private static final String ABLAUF_RESERVIERUNG = "ADR";
    private static final String STARTWERT = "START_VALUE";
    private static final String ANZAHL = "POINT_AMOUNT";
    private static final String LEBENSZEIT_BEGINN = "LBZ";
    private static final String PUNKT_NUMMER = "PNR";
    private static final String PUNKT_UUID = "PUUID";
    private static final String PUNKT_UUIDLBZ = "PUUIDLBZ";
    private static final String FIRST_NUMBER = "FIRST_NUMBER";
    private static final String LAST_NUMBER = "LAST_NUMBER";
    private static final String VERMESSUNG_STELLE = "VERMESSUNG_STELLE";

    private static PointNumberReservationService instance;

    //~ Instance fields --------------------------------------------------------

    private final String SERVICE_URL;
    private final String USER;
    private final String PW;
    private AuftragsManagerSoap manager;
    private boolean initError = false;
    private String TEMPLATE_BEN_AUFTR_ALL;
    private String TEMPLATE_BEN_AUFTR_ONE_ANR;
    private String TEMPLATE_BEN_AUFTR_WILDCARD;
    private String TEMPLATE_FREIGABE;
    private String TEMPLATE_PROLONG;
    private String TEMPLATE_PROLONG_SUB;
    private String TEMPLATE_RESERVIERUNG;
    private String TEMPLATE_RESERVIERUNG_SW;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new PointNumberService object.
     */
    private PointNumberReservationService() {
        final Properties serviceProperties = new Properties();
        try {
            serviceProperties.load(CachedServerResourcesLoader.getInstance().getStringReaderResource(
                    TextServerResources.PNR_PROPERTIES));
            final ServerProperties serverProps = DomainServerImpl.getServerProperties();
            final String serverRespath = serverProps.getServerResourcesBasePath();
            TEMPLATE_BEN_AUFTR_ALL = serverRespath
                        + "/de/cismet/cids/custom/utils/pointnumberreservation/A_Ben_Auftr_alle_PKZ.xml";
            TEMPLATE_BEN_AUFTR_ONE_ANR = serverRespath
                        + "/de/cismet/cids/custom/utils/pointnumberreservation/A_Ben_Auftr_eine_ANR.xml";
            TEMPLATE_BEN_AUFTR_WILDCARD = serverRespath
                        + "/de/cismet/cids/custom/utils/pointnumberreservation/A_Ben_Auftr_ANR_Praefix_Wildcard.xml";
            TEMPLATE_FREIGABE = serverRespath + "/de/cismet/cids/custom/utils/pointnumberreservation/A_Freigabe.xml";
            TEMPLATE_PROLONG = serverRespath + "/de/cismet/cids/custom/utils/pointnumberreservation/A_Verlaengern.xml";
            TEMPLATE_PROLONG_SUB = serverRespath
                        + "/de/cismet/cids/custom/utils/pointnumberreservation/A_Verlaengern__Sub.xml";
            TEMPLATE_RESERVIERUNG = serverRespath
                        + "/de/cismet/cids/custom/utils/pointnumberreservation/A_reservierung.xml";
            TEMPLATE_RESERVIERUNG_SW = serverRespath
                        + "/de/cismet/cids/custom/utils/pointnumberreservation/A_reservierung_startwert.xml";

            if (!checkTemplateFilesAccessible()) {
                LOG.warn("Punktnummernreservierung initialisation Error!");
                initError = true;
            }
        } catch (Exception ex) {
            LOG.warn("Punktnummernreservierung initialisation Error!", ex);
            initError = true;
        }
        if (!serviceProperties.containsKey("service") || !serviceProperties.containsKey("user")
                    || !serviceProperties.containsKey("password")) {
            LOG.warn(
                "Could not read all necessary properties from pointNumberRes_conf.properties. Disabling PointNumberReservationService!");
            initError = true;
        }
        SERVICE_URL = serviceProperties.getProperty("service", "");
        USER = serviceProperties.getProperty("user", "");
        PW = serviceProperties.getProperty("password", "");
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static PointNumberReservationService instance() {
        if (instance == null) {
            instance = new PointNumberReservationService();
        }
        return instance;
    }

    /**
     * DOCUMENT ME!
     */
    private void initAmManager() {
        if (initError) {
            return;
        }
        final AuftragsManager am;
        try {
            am = new AuftragsManagerLocator();
            manager = am.getAuftragsManagerSoap(new URL(SERVICE_URL));
        } catch (Exception ex) {
            LOG.error("error creating 3AServer interface", ex);
            return;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   is  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private byte[] gZipFile(final InputStream is) {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        OutputStream zipOut = null;
        try {
            zipOut = new GZIPOutputStream(bos);
            final byte[] buffer = new byte[8192];
            int length = is.read(buffer, 0, 8192);
            while (length != -1) {
                zipOut.write(buffer, 0, length);
                length = is.read(buffer, 0, 8192);
            }
            is.close();
            zipOut.close();
            return bos.toByteArray();
        } catch (FileNotFoundException ex) {
            LOG.error("error during gzip of file", ex);
        } catch (IOException ex) {
            LOG.error("error during gzip of file", ex);
        } finally {
        }
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   file  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String readFile(final InputStream file) {
        final BufferedReader reader;
        final StringBuilder stringBuilder = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(file));

            String line = null;

            final String ls = System.getProperty("line.separator");

            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append(ls);
            }
        } catch (FileNotFoundException ex) {
            LOG.error("could not find request file: " + file, ex);
        } catch (IOException ex) {
            LOG.error("error during reading request file: " + file, ex);
        }
        return stringBuilder.toString();
    }

    /**
     * DOCUMENT ME!
     *
     * @param   data  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private byte[] gunzip(final byte[] data) {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        InputStream is = null;
        try {
            is = new GZIPInputStream(new ByteArrayInputStream(data));
            final byte[] buffer = new byte[8192];
            int length = is.read(buffer, 0, 8192);
            while (length != -1) {
                bos.write(buffer, 0, length);
                length = is.read(buffer, 0, 8192);
            }
            return bos.toByteArray();
        } catch (IOException ex) {
            LOG.error("error during gunzip of nas response files", ex);
        } finally {
            try {
                bos.close();
                if (is != null) {
                    is.close();
                }
            } catch (IOException ex) {
            }
        }
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   preparedQuery  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String sendRequestAndAwaitResult(final InputStream preparedQuery) {
        initAmManager();
        if (manager == null) {
            LOG.error("3AServer manager interface is  null");
            return null;
        }
        try {
            final int sessionID = manager.login(USER, PW);
            final String orderId = manager.registerGZip(
                    sessionID,
                    gZipFile(preparedQuery));

            while ((manager.getResultCount(sessionID, orderId) < 1)
                        && (manager.getProtocolGZip(sessionID, orderId) == null)) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    LOG.error("Sleep interrupted");
                }
            }
            final int resCount = manager.getResultCount(sessionID, orderId);
            final byte[] data;
            if (resCount == 0) {
                LOG.error("Protocol for PointNumberReservation order " + orderId + ": "
                            + new String(gunzip(manager.getProtocolGZip(sessionID, orderId))));
                return null;
            } else {
                data = manager.getResultGZip(sessionID, orderId);
            }

            return new String(gunzip(data), "UTF-8");
        } catch (Exception e) {
            LOG.error("Error during registering order at aaa service", e);
        }
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String getAblaufDatum() {
        final GregorianCalendar currDate = new GregorianCalendar();
        currDate.add(GregorianCalendar.MONTH, 18);

        final SimpleDateFormat fd = new SimpleDateFormat("yyyy-MM-dd");
        return fd.format(currDate.getTime());
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Collection<PointNumberReservationRequest> getAllBenAuftr() {
        if (initError) {
            LOG.info("PointNumberReservationService initialisation error");
            return null;
        }
        final InputStream templateFile;
        String result = null;
        try {
            templateFile = new FileInputStream(TEMPLATE_BEN_AUFTR_ALL);
            final String request = readFile(templateFile);
            final InputStream preparedQuery = new ByteArrayInputStream(request.getBytes());

            result = sendRequestAndAwaitResult(preparedQuery);
        } catch (FileNotFoundException ex) {
            LOG.error("Could not find PointnumberReservation tempalte file " + TEMPLATE_BEN_AUFTR_ALL);
        }

        if (result == null) {
            return null;
        }
        return PointNumberReservationBeanParser.parseBestandsdatenauszug(result);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   anr  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public PointNumberReservationRequest getAllBenAuftr(final String anr) {
        if (initError) {
            LOG.info("PointNumberReservationService initialisation error");
            return null;
        }
        InputStream templateFile = null;
        try {
            templateFile = new FileInputStream(TEMPLATE_BEN_AUFTR_ONE_ANR);
        } catch (FileNotFoundException ex) {
            LOG.error("Could not find PointnumberReservation tempalte file " + TEMPLATE_BEN_AUFTR_ONE_ANR);
        }

        if (templateFile == null) {
            return null;
        }

        final String request = readFile(templateFile);
        final String preparedRequest = request.replaceAll(AUFTRAGS_NUMMER, anr);

        final InputStream preparedQuery = new ByteArrayInputStream(preparedRequest.getBytes());

        final String result = sendRequestAndAwaitResult(preparedQuery);
        if (result == null) {
            return null;
        }

        final Collection<PointNumberReservationRequest> requests = PointNumberReservationBeanParser
                    .parseBestandsdatenauszug(result);
        // should only contain one element
        if (requests.isEmpty()) {
            LOG.info("Could not find a result for Auftragsnummer " + anr);
            return null;
        }
        if (request.length() > 1) {
            LOG.warn(
                "There should be exact one Auftragsnummer but the result contains multiple one. Returning only the first one");
        }
        final PointNumberReservationRequest r = requests.toArray(new PointNumberReservationRequest[1])[0];

        return r;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   anr  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Collection<PointNumberReservationRequest> getAllBenAuftrWithWildCard(final String anr) {
        if (initError) {
            LOG.info("PointNumberReservationService initialisation error");
            return null;
        }
        InputStream templateFile = null;
        try {
            templateFile = new FileInputStream(TEMPLATE_BEN_AUFTR_WILDCARD);
        } catch (FileNotFoundException ex) {
            LOG.error("Could not find PointnumberReservation tempalte file " + TEMPLATE_BEN_AUFTR_WILDCARD);
        }
        if (templateFile == null) {
            return null;
        }

        final String request = readFile(templateFile);
        // ToDo: Replace the ANR number
        final String preparedRequest = request.replaceAll(AUFTRAGS_NUMMER, anr);

        final InputStream preparedQuery = new ByteArrayInputStream(preparedRequest.getBytes());

        final String result = sendRequestAndAwaitResult(preparedQuery);
        if (result == null) {
            return null;
        }
        final Collection<PointNumberReservationRequest> requests = PointNumberReservationBeanParser
                    .parseBestandsdatenauszug(result);

        if (requests.isEmpty()) {
            LOG.info("Could not find a result for Auftragsnummer " + anr);
        }

        for (final PointNumberReservationRequest r : requests) {
            final Collection<PointNumberReservation> pointNUmbers = r.getPointNumbers();
            final StringBuffer b = new StringBuffer();
            for (final PointNumberReservation pnr : pointNUmbers) {
                b.append(pnr.getPunktnummer());
                b.append(",");
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Found request: " + r.getAntragsnummer() + " with pointNUmbers: " + b.toString());
            }
        }

        return requests;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   anr  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public boolean isAntragsNummerExisting(final String anr) {
        if (initError) {
            LOG.info("PointNumberReservationService initialisation error");
            return false;
        }
        final PointNumberReservationRequest auftrag = getAllBenAuftr(anr);
        if ((auftrag == null) || (auftrag.getAntragsnummer() == null)) {
            return false;
        }
        return auftrag.getAntragsnummer().equals(anr);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   requestId  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Collection<PointNumberReservation> getReserviertePunkte(final String requestId) {
        if (initError) {
            LOG.info("PointNumberReservationService initialisation error");
            return null;
        }
        final PointNumberReservationRequest request = getAllBenAuftr(requestId);
        if (request != null) {
            return request.getPointNumbers();
        }
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   prefix               DOCUMENT ME!
     * @param   anr                  DOCUMENT ME!
     * @param   nummerierungsbezirk  DOCUMENT ME!
     * @param   firstPointNumber     DOCUMENT ME!
     * @param   lastPointNumber      DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public PointNumberReservationRequest releaseReservation(final String prefix,
            final String anr,
            final String nummerierungsbezirk,
            final int firstPointNumber,
            final int lastPointNumber) {
        if (initError) {
            LOG.info("PointNumberReservationService initialisation error");
            return null;
        }

        InputStream templateFile = null;
        try {
            templateFile = new FileInputStream(TEMPLATE_FREIGABE);
        } catch (FileNotFoundException ex) {
            LOG.error("Could not find PointnumberReservation tempalte file " + TEMPLATE_FREIGABE);
        }

        if (templateFile == null) {
            return null;
        }

        String request = readFile(templateFile);
        request = request.replaceAll(AUFTRAGS_NUMMER, anr);
        request = request.replaceAll(NUMMERIERUNGS_BEZIRK, nummerierungsbezirk);
        request = request.replaceAll(VERMESSUNG_STELLE, prefix);
        final DecimalFormat dcf = new DecimalFormat("000000");

        request = request.replaceAll(FIRST_NUMBER, dcf.format(firstPointNumber));
        request = request.replaceAll(LAST_NUMBER, dcf.format(lastPointNumber));

        final InputStream preparedQuery = new ByteArrayInputStream(request.getBytes());

        final String result = sendRequestAndAwaitResult(preparedQuery);
        if (result == null) {
            return null;
        }
        return PointNumberReservationBeanParser.parseReservierungsErgebnis(result);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   prefix  DOCUMENT ME!
     * @param   anr     DOCUMENT ME!
     * @param   points  DOCUMENT ME!
     * @param   date    DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public PointNumberReservationRequest prolongReservation(final String prefix,
            final String anr,
            final Collection<Integer> points,
            final Date date) {
        if (initError) {
            LOG.info("PointNumberReservationService initialisation error");
            return null;
        }

        InputStream templateFile = null;
        try {
            templateFile = new FileInputStream(TEMPLATE_PROLONG);
        } catch (FileNotFoundException ex) {
            LOG.error("Could not find PointnumberProlong template file " + TEMPLATE_PROLONG);
        }

        InputStream templateSubFile = null;
        try {
            templateSubFile = new FileInputStream(TEMPLATE_PROLONG_SUB);
        } catch (FileNotFoundException ex) {
            LOG.error("Could not find PointnumberProlong template file " + TEMPLATE_PROLONG_SUB);
        }

        if ((templateFile == null) || (templateSubFile == null)) {
            return null;
        }

        final PointNumberReservationRequest result = PointNumberReservationService.instance().getAllBenAuftr(anr);
        if (result != null) {
            // for having the pnrs in the right sort order, first push them in HM
            // then getting them from the HM in the right points order.
            final Map<Integer, PointNumberReservation> pnrMap = new HashMap<Integer, PointNumberReservation>();
            for (final PointNumberReservation pointNumberReserveration : result.getPointNumbers()) {
                final Integer tmp = Integer.parseInt(pointNumberReserveration.getPunktnummer().substring(
                            pointNumberReserveration.getPunktnummer().length()
                                    - 6,
                            pointNumberReserveration.getPunktnummer().length()));
                if (points.contains(tmp)) {
                    pnrMap.put(tmp, pointNumberReserveration);
                }
            }

            String request = readFile(templateFile);
            request = request.replaceAll(AUFTRAGS_NUMMER, anr);

            final String requestSub = readFile(templateSubFile);
            final StringBuffer subs = new StringBuffer();

            for (final Integer point : points) {
                final PointNumberReservation pointNumberReserveration = pnrMap.get(point);
                final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

                String requestSubTmp = requestSub;
                requestSubTmp = requestSubTmp.replaceAll(PUNKT_UUIDLBZ, pointNumberReserveration.getFeatureId());
                requestSubTmp = requestSubTmp.replaceAll(PUNKT_UUID, pointNumberReserveration.getUuid());
                requestSubTmp = requestSubTmp.replaceAll(
                        LEBENSZEIT_BEGINN,
                        pointNumberReserveration.getIntervallbeginn());
                requestSubTmp = requestSubTmp.replaceAll(PUNKT_NUMMER, pointNumberReserveration.getPunktnummer());
                requestSubTmp = requestSubTmp.replaceAll(
                        ABLAUF_RESERVIERUNG,
                        sdf.format(date));
                requestSubTmp = requestSubTmp.replaceAll(
                        VERMESSUNG_STELLE,
                        pointNumberReserveration.getVermessungsstelle());
                requestSubTmp = requestSubTmp.replaceAll(AUFTRAGS_NUMMER, anr);

                subs.append(requestSubTmp);
            }

            request = request.replaceAll("SUBTEMPLATE", subs.toString());

            final InputStream preparedQuery = new ByteArrayInputStream(request.getBytes());

            final String verlaengernResult = sendRequestAndAwaitResult(preparedQuery);
            if (verlaengernResult == null) {
                return null;
            }
            return PointNumberReservationBeanParser.parseReservierungsErgebnis(verlaengernResult);
        } else {
            LOG.warn("no pointnumbers found to prolong");
            return null;
        }
    }
    /**
     * DOCUMENT ME!
     *
     * @param   prefix               DOCUMENT ME!
     * @param   requestId            DOCUMENT ME!
     * @param   nummerierungsbezirk  DOCUMENT ME!
     * @param   anzahl               DOCUMENT ME!
     * @param   startValue           DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public PointNumberReservationRequest doReservation(final String prefix,
            final String requestId,
            final String nummerierungsbezirk,
            final int anzahl,
            final int startValue) {
        if (initError) {
            LOG.info("PointNumberReservationService initialisation error");
            return null;
        }
        // check requestID check if point number exceed 999999 if ((startValue + anzahl) > 999999) { final String
        // errorMsg = "Point number for startValue " + startValue + "and point amount " + anzahl + " will exceed maximum
        // number 999999. Can not execute request."; LOG.error(errorMsg); throw new IllegalStateException(errorMsg); }

        InputStream templateFile = null;
        try {
            if (startValue == 0) {
                templateFile = new FileInputStream(TEMPLATE_RESERVIERUNG);
            } else {
                templateFile = new FileInputStream(TEMPLATE_RESERVIERUNG_SW);
            }
        } catch (FileNotFoundException ex) {
            LOG.error("Could not find PointnumberReservation tempalte file " + TEMPLATE_FREIGABE);
        }

        if (templateFile == null) {
            return null;
        }

        String request = readFile(templateFile);
        // Insert values in the template file,
        request = request.replaceAll(AUFTRAGS_NUMMER, requestId);
        request = request.replaceAll(NUMMERIERUNGS_BEZIRK, nummerierungsbezirk);
        request = request.replaceAll(ANZAHL, Integer.toString(anzahl));
        request = request.replaceAll(STARTWERT, Integer.toString(startValue));
        request = request.replaceAll(ABLAUF_RESERVIERUNG, getAblaufDatum());
        request = request.replaceAll(VERMESSUNG_STELLE, prefix);

        final InputStream preparedQuery = new ByteArrayInputStream(request.getBytes());

        final String result = sendRequestAndAwaitResult(preparedQuery);
        if (result == null) {
            return null;
        }
        final PointNumberReservationRequest tmpResult = PointNumberReservationBeanParser.parseReservierungsErgebnis(
                result);
        if (tmpResult.isSuccessfull()) {
            fillWithAblaufDatum(requestId, tmpResult);
        }
        return tmpResult;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  requestId          DOCUMENT ME!
     * @param  resultWithoutDate  DOCUMENT ME!
     */
    private void fillWithAblaufDatum(final String requestId, final PointNumberReservationRequest resultWithoutDate) {
        final List<PointNumberReservation> tmp = getAllBenAuftr(requestId).getPointNumbers();
        final List<PointNumberReservation> pnrWithoutDate = resultWithoutDate.getPointNumbers();
        tmp.retainAll(resultWithoutDate.getPointNumbers());
        Collections.sort(tmp);
        Collections.sort(pnrWithoutDate);

        for (final PointNumberReservation pnr : pnrWithoutDate) {
            // search for the corresponding pnr of the getAllAuftrag
            final PointNumberReservation completePnr = tmp.get(pnrWithoutDate.indexOf(pnr));
            if (pnr.equals(completePnr)) {
                pnr.setAblaufDatum(completePnr.getAblaufDatum());
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private boolean checkTemplateFilesAccessible() {
        final File benAuftrAllTempl = new File(TEMPLATE_BEN_AUFTR_ALL);
        final File benAuftrOneTempl = new File(TEMPLATE_BEN_AUFTR_ONE_ANR);
        final File benAuftrWildTempl = new File(TEMPLATE_BEN_AUFTR_WILDCARD);
        final File releaseTempl = new File(TEMPLATE_FREIGABE);
        final File prolongTempl = new File(TEMPLATE_PROLONG);
        final File prolongSubTempl = new File(TEMPLATE_PROLONG_SUB);
        final File reservationTempl = new File(TEMPLATE_RESERVIERUNG);
        final File reservationSWTempl = new File(TEMPLATE_RESERVIERUNG_SW);

        return benAuftrAllTempl.canRead() && benAuftrOneTempl.canRead() && benAuftrWildTempl.canRead()
                    && releaseTempl.canRead() && prolongTempl.canRead() && prolongSubTempl.canRead()
                    && reservationSWTempl.canRead()
                    && reservationTempl.canRead();
    }
}
