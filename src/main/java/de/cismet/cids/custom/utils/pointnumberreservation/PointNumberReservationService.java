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

import java.text.SimpleDateFormat;

import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
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

    private static PointNumberReservationService instance;

    //~ Instance fields --------------------------------------------------------

    private final String SERVICE_URL;
    private final String USER;
    private final String PW;
    private AuftragsManagerSoap manager;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new PointNumberService object.
     *
     * @throws  RuntimeException  DOCUMENT ME!
     */
    private PointNumberReservationService() {
        final Properties serviceProperties = new Properties();
        try {
            serviceProperties.load(PointNumberReservationService.class.getResourceAsStream(
                    "pointNumberRes_conf.properties"));
            SERVICE_URL = serviceProperties.getProperty("service");
            USER = serviceProperties.getProperty("user");
            PW = serviceProperties.getProperty("password");
        } catch (Exception ex) {
            LOG.fatal("NAS Datenabgabe initialisation Error!", ex);
            throw new RuntimeException(ex);
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
        final AuftragsManager am;
        try {
            am = new AuftragsManager(new URL(SERVICE_URL));
        } catch (Exception ex) {
            LOG.error("error creating 3AServer interface");
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

        final String orderId = manager.registerGZip(sessionID, gZipFile(preparedQuery));

        while ((manager.getResultCount(sessionID, orderId) < 1)
                    && (manager.getProtocolGZip(sessionID, orderId) == null)) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                LOG.error("Sleep interrupted");
            }
        }
        final int resCount = manager.getResultCount(sessionID, orderId);
        byte[] data;
        if (resCount == 0) {
            LOG.error("it seems that there is an error with NAS order: " + orderId + ". Protocol:");
            LOG.error("Protocol for NAS order " + orderId + ": "
                        + new String(gunzip(manager.getProtocolGZip(sessionID, orderId))));
            data = manager.getProtocolGZip(sessionID, orderId);
        } else {
            data = manager.getResultGZip(sessionID, orderId);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Result for request: " + orderId + ": "
                        + new String(gunzip(manager.getResultGZip(sessionID, orderId))));
        }

        return new String(gunzip(data));
    }

    /**
     * DOCUMENT ME!
     *
     * @param   requestId  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private boolean isAuftragsNummerValid(final String requestId) {
        return (requestId.length() <= 50) && requestId.matches("[a-zA-Z0-9_-]*");
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String getAblaufDatum() {
        final GregorianCalendar currDate = new GregorianCalendar();
        currDate.add(GregorianCalendar.MONTH, 18);

        final SimpleDateFormat fd = new SimpleDateFormat("yyyy-mm-dd");
        return fd.format(currDate.getTime());
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Object getAllBenAuftrAllPKZ() {
        final InputStream templateFile = PointNumberReservationService.class.getResourceAsStream(
                "A_Ben_Auftr_alle_PKZ.xml");
        final String request = readFile(templateFile);
        final InputStream preparedQuery = new ByteArrayInputStream(request.getBytes());

        final String result = sendRequestAndAwaitResult(preparedQuery);
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
            LOG.fatal("Found request: " + r.getAntragsnummer() + " with pointNUmbers: " + b.toString());
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
        final PointNumberReservationRequest auftrag = getAllBenAuftr(anr);
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
        final InputStream templateFile = PointNumberReservationService.class.getResourceAsStream("A_verlaengern.xml");

        if (templateFile == null) {
            LOG.error("Could not load Template file");
            return null;
        }

        final String request = readFile(templateFile);
        // ToDo: Replace expiration Date,

        final InputStream preparedQuery = new ByteArrayInputStream(request.getBytes());

        final String result = sendRequestAndAwaitResult(preparedQuery);

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
        final PointNumberReservationRequest request = getAllBenAuftr(requestId);
        if (request != null) {
            return request.getPointNumbers();
        }
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   anr                  DOCUMENT ME!
     * @param   nummerierungsbezirk  DOCUMENT ME!
     * @param   firstPointNumber     DOCUMENT ME!
     * @param   lastPointNumber      DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Object releaseReservation(final String anr,
            final int nummerierungsbezirk,
            final int firstPointNumber,
            final int lastPointNumber) {
        // ToDo: both numbers need to have 6 digits, last one needs to be greater than first number

        final InputStream templateFile = PointNumberReservationService.class.getResourceAsStream("A_Freigabe.xml");

        if (templateFile == null) {
            LOG.error("Could not load Template file");
            return null;
        }

        String request = readFile(templateFile);
        request = request.replaceAll(AUFTRAGS_NUMMER, anr);
        request = request.replaceAll(NUMMERIERUNGS_BEZIRK, Integer.toString(nummerierungsbezirk));
        request = request.replaceAll(FIRST_NUMBER, Integer.toString(firstPointNumber));
        request = request.replaceAll(LAST_NUMBER, Integer.toString(lastPointNumber));

        final InputStream preparedQuery = new ByteArrayInputStream(request.getBytes());

        final String result = sendRequestAndAwaitResult(preparedQuery);

        return result;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   requestId            DOCUMENT ME!
     * @param   nummerierungsbezirk  DOCUMENT ME!
     * @param   anzahl               DOCUMENT ME!
     * @param   startValue           DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  IllegalStateException  DOCUMENT ME!
     */
    public Object doReservation(final String requestId,
            final int nummerierungsbezirk,
            final int anzahl,
            final int startValue) {
        // check requestID
        if (!isAuftragsNummerValid(requestId)) {
            final String errorMsg = "Auftragsnummer " + requestId + " is not valid. Can not execute request";
            LOG.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        // check if point number exceed 999999
        if ((startValue + anzahl) > 999999) {
            final String errorMsg = "Point number for startValue " + startValue + "and point amount " + anzahl
                        + " will exceed maximum number 999999. Can not execute request.";
            LOG.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

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
        request = request.replaceAll(NUMMERIERUNGS_BEZIRK, Integer.toString(nummerierungsbezirk));
        request = request.replaceAll(ANZAHL, Integer.toString(anzahl));
        request = request.replaceAll(STARTWERT, Integer.toString(startValue));
        request = request.replaceAll(ABLAUF_RESERVIERUNG, getAblaufDatum());

        final InputStream preparedQuery = new ByteArrayInputStream(request.getBytes());

        final String result = sendRequestAndAwaitResult(preparedQuery);

        return result;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  args  DOCUMENT ME!
     */
    public static void main(final String[] args) {
        final PointNumberReservationService pos = PointNumberReservationService.instance();
        final String result = "";
        final long begin = System.currentTimeMillis();
//        result = (String) pos.getAllBenAuftrAllPKZ();
        final long end = System.currentTimeMillis();
        final float durationInSec = (end - begin) / 1000f;
        System.out.println("Dauer Ben_Auftr_alle_PKZ:" + (durationInSec) + "sec");
//        System.out.println(result);
        System.out.println("\n######################################################\n");

//        begin = System.currentTimeMillis();
//        result = (String) pos.getAllBenAuftr("12015_11054_Stenzel*", true);
//        end = System.currentTimeMillis();
//        durationInSec = (end - begin) / 1000f;
//        System.out.println(result);
//        System.out.println("\nSuche nach 12015_11054_Stenzel*:" + (durationInSec) + "sec");
//        begin = System.currentTimeMillis();
//        result = (String) pos.getAllBenAuftr("12015_11054_Stenzel", false);
//        end = System.currentTimeMillis();
//        durationInSec = (end - begin) / 1000f;
////        System.out.println(result);
//        System.out.println("Suche nach 12015_11054_Stenzel:" + (durationInSec) + "sec");
//        System.out.println("\n######################################################\n");
//
//        begin = System.currentTimeMillis();
//        result = (String) pos.getAllBenAuftr("0279_11191-E*", true);
//        end = System.currentTimeMillis();
//        durationInSec = (end - begin) / 1000f;
////        System.out.println(result);
//        System.out.println("Suche nach 0279_11191-E*:" + (durationInSec) + "sec");
//
//        begin = System.currentTimeMillis();
//        result = (String) pos.getAllBenAuftr("0279_11191-E", false);
//        end = System.currentTimeMillis();
//        durationInSec = (end - begin) / 1000f;
////        System.out.println(result);
//        System.out.println("Suche nach 0279_11191-E:" + (durationInSec) + "sec");
//        System.out.println("\n######################################################\n");
//
//        begin = System.currentTimeMillis();
//        result = (String) pos.getAllBenAuftr("3290_20120297*", true);
//        end = System.currentTimeMillis();
//        durationInSec = (end - begin) / 1000f;
////        System.out.println(result);
//        System.out.println("Suche nach 3290_20120297*:" + (durationInSec) + "sec");
//
//        begin = System.currentTimeMillis();
//        result = (String) pos.getAllBenAuftr("3290_20120297", false);
//        end = System.currentTimeMillis();
//        durationInSec = (end - begin) / 1000f;
////        System.out.println(result);
//        System.out.println("Suche nach 3290_20120297:" + (durationInSec) + "sec");
//        System.out.println("\n######################################################\n");
//
//        begin = System.currentTimeMillis();
//        result = (String) pos.getAllBenAuftr("3290_20120321*", true);
//        end = System.currentTimeMillis();
//        durationInSec = (end - begin) / 1000f;
////        System.out.println(result);
//        System.out.println("Suche nach 3290_20120321*:" + (durationInSec) + "sec");
//
//        begin = System.currentTimeMillis();
//        result = (String) pos.getAllBenAuftr("3290_20120321", false);
//        end = System.currentTimeMillis();
//        durationInSec = (end - begin) / 1000f;
////        System.out.println(result);
//        System.out.println("Suche nach 3290_20120321:" + (durationInSec) + "sec");
//        System.out.println("\n######################################################\n");
//
//        begin = System.currentTimeMillis();
//        result = (String) pos.getAllBenAuftr("3290_20110057*", true);
//        end = System.currentTimeMillis();
//        durationInSec = (end - begin) / 1000f;
////        System.out.println(result);
//        System.out.println("Suche nach 3290_20110057*:" + (durationInSec) + "sec");
//        begin = System.currentTimeMillis();
//        result = (String) pos.getAllBenAuftr("3290_20110057", false);
//        end = System.currentTimeMillis();
//        durationInSec = (end - begin) / 1000f;
////        System.out.println(result);
//        System.out.println("Suche nach 3290_20110057:" + (durationInSec) + "sec");
//        System.out.println("\n######################################################\n");
//
//        begin = System.currentTimeMillis();
//        result = (String) pos.getAllBenAuftr("0270_13007*", true);
//        end = System.currentTimeMillis();
//        durationInSec = (end - begin) / 1000f;
////        System.out.println(result);
//        System.out.println("Suche nach 0270_13007*:" + (durationInSec) + "sec");
//        begin = System.currentTimeMillis();
//        result = (String) pos.getAllBenAuftr("0270_13007", false);
//        end = System.currentTimeMillis();
//        durationInSec = (end - begin) / 1000f;
////        System.out.println(result);
//        System.out.println("Suche nach 0270_13007:" + (durationInSec) + "sec");
//        System.out.println("\n######################################################\n");
//
//        begin = System.currentTimeMillis();
//        result = (String) pos.getAllBenAuftr("0279_112121_Schmalenhofer_Bach*", true);
//        end = System.currentTimeMillis();
//        durationInSec = (end - begin) / 1000f;
////        System.out.println(result);
//        System.out.println("Suche nach 0279_112121_Schmalenhofer_Bach*:" + (durationInSec) + "sec");
//        begin = System.currentTimeMillis();
//        result = (String) pos.getAllBenAuftr("0279_112121_Schmalenhofer_Bach", false);
//        end = System.currentTimeMillis();
//        durationInSec = (end - begin) / 1000f;
////        System.out.println(result);
//        System.out.println("Suche nach 0279_112121_Schmalenhofer_Bach:" + (durationInSec) + "sec");
//        System.out.println("\n######################################################\n");
//
//        begin = System.currentTimeMillis();
//        result = (String) pos.getAllBenAuftr("0508_10-163*", true);
//        end = System.currentTimeMillis();
//        durationInSec = (end - begin) / 1000f;
////        System.out.println(result);
//        System.out.println("Suche nach 0508_10-163*:" + (durationInSec) + "sec");
//        begin = System.currentTimeMillis();
//        result = (String) pos.getAllBenAuftr("0508_10-163", true);
//        end = System.currentTimeMillis();
//        durationInSec = (end - begin) / 1000f;
////        System.out.println(result);
//        System.out.println("Suche nach 0508_10-163:" + (durationInSec) + "sec");
//        System.out.println("\n######################################################\n");
//
//        begin = System.currentTimeMillis();
//        result = (String) pos.getAllBenAuftr("aaa", false);
//        end = System.currentTimeMillis();
//        durationInSec = (end - begin) / 1000f;
//        System.out.println("Suche nach aaa (should fail):" + (durationInSec) + "sec");
//        System.out.println("isAnrValid(aaa):" + pos.isAntragsNummerExisting("aaa"));
//        System.out.println("\n######################################################\n");
//
//        begin = System.currentTimeMillis();
//        result = pos.getAllBenAuftr("02*", true);
//        end = System.currentTimeMillis();
//        durationInSec = (end - begin) / 1000f;
//        System.out.println("Suche nach 02*:" + (durationInSec) + "sec");
//        System.out.println("\n######################################################\n");
//        result = (String)pos.getAllBenAuftr(null, false);
//        System.out.println("Ben_Auftr__eine_ANR");
//        System.out.println(result);
//        System.out.println("\n######################################################\n");
//        result = (String)pos.getAllBenAuftr(null, true);
//        System.out.println("Ben_Auftr__eine_ANR_Wildcard");
////        System.out.println(result);
//        System.out.println("\n######################################################\n");
    }
}
