/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.utils.pointnumberreservation;

import de.aed_sicad.namespaces.svr.AuftragsManager;
import de.aed_sicad.namespaces.svr.AuftragsManagerSoap;

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

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new PointNumberService object.
     */
    private PointNumberReservationService() {
        final Properties serviceProperties = new Properties();
        try {
            serviceProperties.load(PointNumberReservationService.class.getResourceAsStream(
                    "pointNumberRes_conf.properties"));
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
            am = new AuftragsManager(new URL(SERVICE_URL));
        } catch (Exception ex) {
            LOG.error("error creating 3AServer interface", ex);
            return;
        }
        manager = am.getAuftragsManagerSoap();
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
        final int sessionID = manager.login(USER, PW);
        try {
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
        final InputStream templateFile = PointNumberReservationService.class.getResourceAsStream(
                "A_Ben_Auftr_alle_PKZ.xml");
        final String request = readFile(templateFile);
        final InputStream preparedQuery = new ByteArrayInputStream(request.getBytes());

        final String result = sendRequestAndAwaitResult(preparedQuery);
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
        templateFile = PointNumberReservationService.class.getResourceAsStream("A_Ben_Auftr_eine_ANR.xml");

        if (templateFile == null) {
            LOG.error("Could not load Template file");
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
        templateFile = PointNumberReservationService.class.getResourceAsStream(
                "A_Ben_Auftr_ANR_Praefix_Wildcard.xml");
        if (templateFile == null) {
            LOG.error("Could not load Template file");
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
                b.append(pnr.getPunktnummern());
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
     * @param   expirationDate  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Object prolongReservation(final Date expirationDate) {
        if (initError) {
            LOG.info("PointNumberReservationService initialisation error");
            return null;
        }
        final InputStream templateFile = PointNumberReservationService.class.getResourceAsStream("A_verlaengern.xml");

        if (templateFile == null) {
            LOG.error("Could not load Template file");
            return null;
        }

        final String request = readFile(templateFile);
        // ToDo: Replace expiration Date,

        final InputStream preparedQuery = new ByteArrayInputStream(request.getBytes());

        final String result = sendRequestAndAwaitResult(preparedQuery);
        if (result == null) {
            return null;
        }

        return result;
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

        final InputStream templateFile = PointNumberReservationService.class.getResourceAsStream("A_Freigabe.xml");

        if (templateFile == null) {
            LOG.error("Could not load Template file");
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
        if (startValue == 0) {
            templateFile = PointNumberReservationService.class.getResourceAsStream("A_reservierung.xml");
        } else {
            templateFile = PointNumberReservationService.class.getResourceAsStream("A_reservierung_startwert.xml");
        }

        if (templateFile == null) {
            LOG.error("Could not load Template file");
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
}
