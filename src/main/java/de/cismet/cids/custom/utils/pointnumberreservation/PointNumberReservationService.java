/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.utils.pointnumberreservation;

import de.aed_sicad.www.namespaces.svr.AuftragsManager;
import de.aed_sicad.www.namespaces.svr.AuftragsManagerLocator;
import de.aed_sicad.www.namespaces.svr.AuftragsManagerSoap;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import java.net.URL;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import de.cismet.cids.custom.utils.WundaBlauServerResources;

import de.cismet.cids.utils.serverresources.ServerResourcesLoader;

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
    private static final String PROFIL_KENNUNG = "WUNDA_RES";

    private static PointNumberReservationService instance;

    //~ Instance fields --------------------------------------------------------

    private String SERVICE_URL;
    private String USER;
    private String PW;
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
        try {
            final Properties serviceProperties = ServerResourcesLoader.getInstance()
                        .loadProperties(WundaBlauServerResources.PNR_PROPERTIES.getValue());
            TEMPLATE_BEN_AUFTR_ALL = ServerResourcesLoader.getInstance()
                        .loadText(WundaBlauServerResources.PNR_TEMPLATE_BEN_AUFTR_ALL.getValue());
            TEMPLATE_BEN_AUFTR_ONE_ANR = ServerResourcesLoader.getInstance()
                        .loadText(WundaBlauServerResources.PNR_TEMPLATE_BEN_AUFTR_ONE_ANR.getValue());
            TEMPLATE_BEN_AUFTR_WILDCARD = ServerResourcesLoader.getInstance()
                        .loadText(WundaBlauServerResources.PNR_TEMPLATE_BEN_AUFTR_WILDCARD.getValue());
            TEMPLATE_FREIGABE = ServerResourcesLoader.getInstance()
                        .loadText(WundaBlauServerResources.PNR_TEMPLATE_FREIGABE.getValue());
            TEMPLATE_PROLONG = ServerResourcesLoader.getInstance()
                        .loadText(WundaBlauServerResources.PNR_TEMPLATE_PROLONG.getValue());
            TEMPLATE_PROLONG_SUB = ServerResourcesLoader.getInstance()
                        .loadText(WundaBlauServerResources.PNR_TEMPLATE_PROLONG_SUB.getValue());
            TEMPLATE_RESERVIERUNG = ServerResourcesLoader.getInstance()
                        .loadText(WundaBlauServerResources.PNR_TEMPLATE_RESERVIERUNG.getValue());
            TEMPLATE_RESERVIERUNG_SW = ServerResourcesLoader.getInstance()
                        .loadText(WundaBlauServerResources.PNR_TEMPLATE_RESERVIERUNG_SW.getValue());
            ;

            if (!checkTemplateFilesAccessible()) {
                LOG.warn("Punktnummernreservierung initialisation Error!");
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
        } catch (Exception ex) {
            LOG.warn("Punktnummernreservierung initialisation Error!", ex);
            initError = true;
        }
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
     * @param   profilKennung  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getAllBenAuftr(final String profilKennung) {
        if (initError) {
            LOG.info("PointNumberReservationService initialisation error");
            return null;
        }
        final String request = TEMPLATE_BEN_AUFTR_ALL.replaceAll(PROFIL_KENNUNG, profilKennung);
        final InputStream preparedQuery = new ByteArrayInputStream(request.getBytes());

        return sendRequestAndAwaitResult(preparedQuery);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   result  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Collection<PointNumberReservationRequest> parseAllBenAuftr(final String result) {
        if (result == null) {
            return null;
        }
        return PointNumberReservationBeanParser.parseBestandsdatenauszug(result);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   anr            DOCUMENT ME!
     * @param   profilKennung  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getBenAuftr(final String anr, final String profilKennung) {
        if (initError) {
            LOG.info("PointNumberReservationService initialisation error");
            return null;
        }

        if (TEMPLATE_BEN_AUFTR_ONE_ANR == null) {
            return null;
        }

        final String request = TEMPLATE_BEN_AUFTR_ONE_ANR.replaceAll(AUFTRAGS_NUMMER, anr)
                    .replaceAll(PROFIL_KENNUNG, profilKennung);

        final InputStream preparedQuery = new ByteArrayInputStream(request.getBytes());

        return sendRequestAndAwaitResult(preparedQuery);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   result  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public PointNumberReservationRequest parseBenAuftr(final String result) {
        if (result == null) {
            return null;
        }

        final Collection<PointNumberReservationRequest> requests = PointNumberReservationBeanParser
                    .parseBestandsdatenauszug(result);
        // should only contain one element
        if (requests.isEmpty()) {
            return null;
        }
        if (requests.size() > 1) {
            LOG.warn(
                "There should be exact one Auftragsnummer but the result contains multiple one. Returning only the first one");
        }
        final PointNumberReservationRequest r = requests.toArray(new PointNumberReservationRequest[1])[0];

        return r;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   anr            DOCUMENT ME!
     * @param   profilKennung  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Collection<PointNumberReservationRequest> getAllBenAuftrWithWildCard(final String anr,
            final String profilKennung) {
        if (initError) {
            LOG.info("PointNumberReservationService initialisation error");
            return null;
        }
        if (TEMPLATE_BEN_AUFTR_WILDCARD == null) {
            return null;
        }

        final String request = TEMPLATE_BEN_AUFTR_WILDCARD.replaceAll(AUFTRAGS_NUMMER, anr)
                    .replaceAll(PROFIL_KENNUNG, profilKennung);

        final InputStream preparedQuery = new ByteArrayInputStream(request.getBytes());

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
     * @param   anr            DOCUMENT ME!
     * @param   profilKennung  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public boolean isAntragsNummerExisting(final String anr, final String profilKennung) {
        if (initError) {
            LOG.info("PointNumberReservationService initialisation error");
            return false;
        }
        final PointNumberReservationRequest auftrag = parseBenAuftr(getBenAuftr(anr, profilKennung));
        if ((auftrag == null) || (auftrag.getAntragsnummer() == null)) {
            return false;
        }
        return auftrag.getAntragsnummer().equals(anr);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   requestId      DOCUMENT ME!
     * @param   profilKennung  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Collection<PointNumberReservation> getReserviertePunkte(final String requestId, final String profilKennung) {
        if (initError) {
            LOG.info("PointNumberReservationService initialisation error");
            return null;
        }
        final PointNumberReservationRequest request = parseBenAuftr(getBenAuftr(requestId, profilKennung));
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
     * @param   profilKennung        DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String doReleaseReservation(final String prefix,
            final String anr,
            final String nummerierungsbezirk,
            final int firstPointNumber,
            final int lastPointNumber,
            final String profilKennung) {
        if (initError) {
            LOG.info("PointNumberReservationService initialisation error");
            return null;
        }

        if (TEMPLATE_FREIGABE == null) {
            return null;
        }

        final DecimalFormat dcf = new DecimalFormat("000000");

        final String request = TEMPLATE_FREIGABE.replaceAll(AUFTRAGS_NUMMER, anr)
                    .replaceAll(NUMMERIERUNGS_BEZIRK, nummerierungsbezirk)
                    .replaceAll(VERMESSUNG_STELLE, prefix)
                    .replaceAll(PROFIL_KENNUNG, profilKennung)
                    .replaceAll(FIRST_NUMBER, dcf.format(firstPointNumber))
                    .replaceAll(LAST_NUMBER, dcf.format(lastPointNumber));
        
        final InputStream preparedQuery = new ByteArrayInputStream(request.getBytes());

        return sendRequestAndAwaitResult(preparedQuery);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   result  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public PointNumberReservationRequest parseReleaseReservation(final String result) {
        if (result == null) {
            return null;
        }
        return PointNumberReservationBeanParser.parseReservierungsErgebnis(result);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   prefix         DOCUMENT ME!
     * @param   anr            DOCUMENT ME!
     * @param   points         DOCUMENT ME!
     * @param   date           DOCUMENT ME!
     * @param   profilKennung  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String doProlongReservation(final String prefix,
            final String anr,
            final Collection<Long> points,
            final Date date,
            final String profilKennung) {
        if (initError) {
            LOG.info("PointNumberReservationService initialisation error");
            return null;
        }

        if ((TEMPLATE_PROLONG == null) || (TEMPLATE_PROLONG_SUB == null)) {
            return null;
        }

        final PointNumberReservationRequest result = parseBenAuftr(getBenAuftr(anr, profilKennung));
        if (result != null) {
            // for having the pnrs in the right sort order, first push them in HM
            // then getting them from the HM in the right points order.
            final List<PointNumberReservation> pnrList = new ArrayList<>();
            for (final PointNumberReservation pointNumberReserveration : result.getPointNumbers()) {
                if (points.contains(Long.parseLong(pointNumberReserveration.getPunktnummer()))) {
                    pnrList.add(pointNumberReserveration);
                }
            }

            final String requestSub = TEMPLATE_PROLONG_SUB;
            final StringBuffer subs = new StringBuffer();

            for (final PointNumberReservation pointNumberReserveration : pnrList) {
                final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

                final String requestSubTmp = requestSub.replaceAll(
                            PUNKT_UUIDLBZ,
                            pointNumberReserveration.getFeatureId())
                            .replaceAll(PUNKT_UUID, pointNumberReserveration.getUuid())
                            .replaceAll(LEBENSZEIT_BEGINN, pointNumberReserveration.getIntervallbeginn())
                            .replaceAll(PUNKT_NUMMER, pointNumberReserveration.getPunktnummer())
                            .replaceAll(ABLAUF_RESERVIERUNG, sdf.format(date))
                            .replaceAll(VERMESSUNG_STELLE, pointNumberReserveration.getVermessungsstelle())
                            .replaceAll(AUFTRAGS_NUMMER, anr);

                subs.append(requestSubTmp);
            }

            final String request = TEMPLATE_PROLONG.replaceAll(AUFTRAGS_NUMMER, anr)
                        .replaceAll(PROFIL_KENNUNG, profilKennung)
                        .replaceAll("SUBTEMPLATE", subs.toString());

            final InputStream preparedQuery = new ByteArrayInputStream(request.getBytes());

            return sendRequestAndAwaitResult(preparedQuery);
        } else {
            LOG.warn("no pointnumbers found to prolong");
            return null;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   verlaengernResult  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public PointNumberReservationRequest parseProlongReservation(final String verlaengernResult) {
        if (verlaengernResult == null) {
            return null;
        }
        return PointNumberReservationBeanParser.parseReservierungsErgebnis(verlaengernResult);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   prefix               DOCUMENT ME!
     * @param   requestId            DOCUMENT ME!
     * @param   nummerierungsbezirk  DOCUMENT ME!
     * @param   anzahl               DOCUMENT ME!
     * @param   startValue           DOCUMENT ME!
     * @param   profilKennung        DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String doReservation(final String prefix,
            final String requestId,
            final String nummerierungsbezirk,
            final int anzahl,
            final int startValue,
            final String profilKennung) {
        if (initError) {
            LOG.info("PointNumberReservationService initialisation error");
            return null;
        }
        // check requestID check if point number exceed 999999 if ((startValue + anzahl) > 999999) { final String
        // errorMsg = "Point number for startValue " + startValue + "and point amount " + anzahl + " will exceed maximum
        // number 999999. Can not execute request."; LOG.error(errorMsg); throw new IllegalStateException(errorMsg); }

        if (TEMPLATE_RESERVIERUNG == null) {
            return null;
        }

        final String request = ((startValue == 0) ? TEMPLATE_RESERVIERUNG : TEMPLATE_RESERVIERUNG_SW).replaceAll(
                    AUFTRAGS_NUMMER,
                    requestId)
                    .replaceAll(NUMMERIERUNGS_BEZIRK, nummerierungsbezirk)
                    .replaceAll(ANZAHL, Integer.toString(anzahl))
                    .replaceAll(STARTWERT, Integer.toString(startValue))
                    .replaceAll(ABLAUF_RESERVIERUNG, getAblaufDatum())
                    .replaceAll(VERMESSUNG_STELLE, prefix)
                    .replaceAll(PROFIL_KENNUNG, profilKennung);
        
        final InputStream preparedQuery = new ByteArrayInputStream(request.getBytes());

        return sendRequestAndAwaitResult(preparedQuery);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   requestId      DOCUMENT ME!
     * @param   profilKennung  DOCUMENT ME!
     * @param   result         DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public PointNumberReservationRequest parseReservationResult(final String requestId,
            final String profilKennung,
            final String result) {
        if (result == null) {
            return null;
        } else {
            final PointNumberReservationRequest tmpResult = PointNumberReservationBeanParser.parseReservierungsErgebnis(
                    result);
            if (tmpResult.isSuccessfull()) {
                fillWithAblaufDatum(requestId, tmpResult, profilKennung);
            }
            return tmpResult;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param  requestId          DOCUMENT ME!
     * @param  resultWithoutDate  DOCUMENT ME!
     * @param  profilKennung      DOCUMENT ME!
     */
    private void fillWithAblaufDatum(final String requestId,
            final PointNumberReservationRequest resultWithoutDate,
            final String profilKennung) {
        final List<PointNumberReservation> tmp = parseBenAuftr(getBenAuftr(requestId, profilKennung)).getPointNumbers();
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
        final boolean benAuftrAllTempl = TEMPLATE_BEN_AUFTR_ALL != null;
        final boolean benAuftrOneTempl = TEMPLATE_BEN_AUFTR_ONE_ANR != null;
        final boolean benAuftrWildTempl = TEMPLATE_BEN_AUFTR_WILDCARD != null;
        final boolean releaseTempl = TEMPLATE_FREIGABE != null;
        final boolean prolongTempl = TEMPLATE_PROLONG != null;
        final boolean prolongSubTempl = TEMPLATE_PROLONG_SUB != null;
        final boolean reservationTempl = TEMPLATE_RESERVIERUNG != null;
        final boolean reservationSWTempl = TEMPLATE_RESERVIERUNG_SW != null;

        return benAuftrAllTempl && benAuftrOneTempl && benAuftrWildTempl
                    && releaseTempl && prolongTempl && prolongSubTempl
                    && reservationSWTempl
                    && reservationTempl;
    }
}
