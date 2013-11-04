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
package de.cismet.cids.custom.utils.pointnumberreservation;

import de.aed_sicad.namespaces.svr.AMAuftragServer;
import de.aed_sicad.namespaces.svr.AuftragsManager;
import de.aed_sicad.namespaces.svr.AuftragsManagerSoap;

import org.openide.util.Exceptions;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.URL;

import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
            serviceProperties.load(PointNumberReservationService.class.getResourceAsStream("pointNumberRes_conf.properties"));
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
    private String readFile(final String file) {
        final BufferedReader reader;
        final StringBuilder stringBuilder = new StringBuilder();
        try {
            reader = new BufferedReader(new FileReader(file));

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
        final int sessionID = manager.login(USER, PW);
        final String orderId = manager.registerGZip(sessionID, gZipFile(preparedQuery));

        while ((manager.getResultCount(sessionID, orderId) < 1)
                    && (manager.getProtocolGZip(sessionID, orderId) == null)) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Exceptions.printStackTrace(ex);
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
     * @return  DOCUMENT ME!
     */
    public Object getAllBenAuftrAllPKZ() {
        final URL templateFile = PointNumberReservationService.class.getResource("A_Ben_Auftr_alle_PKZ.xml");

        final String request = readFile(templateFile.getFile());

        final InputStream preparedQuery = new ByteArrayInputStream(request.getBytes());

        final String result = sendRequestAndAwaitResult(preparedQuery);

        return result;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   anr          DOCUMENT ME!
     * @param   useWildCard  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Object getAllBenAuftr(final String anr, final boolean useWildCard) {
        URL templateFile = null;
        if (useWildCard) {
            templateFile = PointNumberReservationService.class.getResource("A_Ben_Auftr_ANR_Praefix_Wildcard.xml");
        } else {
            templateFile = PointNumberReservationService.class.getResource("A_Ben_Auftr_eine_ANR.xml");
        }

        if (templateFile == null) {
            LOG.error("Could not load Template file");
            return null;
        }

        final String request = readFile(templateFile.getFile());
        // ToDo: Replace the ANR number

        final InputStream preparedQuery = new ByteArrayInputStream(request.getBytes());

        final String result = sendRequestAndAwaitResult(preparedQuery);

        return result;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   expirationDate  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Object prolongReservation(final Date expirationDate) {
        final URL templateFile = PointNumberReservationService.class.getResource("A_verlaengern.xml");

        if (templateFile == null) {
            LOG.error("Could not load Template file");
            return null;
        }

        final String request = readFile(templateFile.getFile());
        // ToDo: Replace expiration Date,

        final InputStream preparedQuery = new ByteArrayInputStream(request.getBytes());

        final String result = sendRequestAndAwaitResult(preparedQuery);

        return result;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   nummerierungsbezirk  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Object releaseReservation(final String nummerierungsbezirk) {
        final URL templateFile = PointNumberReservationService.class.getResource("A_verlaengern.xml");

        if (templateFile == null) {
            LOG.error("Could not load Template file");
            return null;
        }

        final String request = readFile(templateFile.getFile());
        // ToDo: Replace values

        final InputStream preparedQuery = new ByteArrayInputStream(request.getBytes());

        final String result = sendRequestAndAwaitResult(preparedQuery);

        return result;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   nummerierungsbezirk  DOCUMENT ME!
     * @param   startValue           DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Object doReservation(final String nummerierungsbezirk, final String startValue) {
        URL templateFile = null;
        if (startValue == null) {
            templateFile = PointNumberReservationService.class.getResource("A_STU_std.xml");
        } else {
            templateFile = PointNumberReservationService.class.getResource("A_Startw_100.xml");
        }

        if (templateFile == null) {
            LOG.error("Could not load Template file");
            return null;
        }

        final String request = readFile(templateFile.getFile());
        // ToDo: Replace values,

        final InputStream preparedQuery = new ByteArrayInputStream(request.getBytes());

        final String result = sendRequestAndAwaitResult(preparedQuery);

        return result;
    }
    
    public void writeResultToFile(String result, File f){
        
    }

    /**
     * DOCUMENT ME!
     *
     * @param  args  DOCUMENT ME!
     */
    public static void main(final String[] args) {
        final PointNumberReservationService pos = PointNumberReservationService.instance();
        String result = "";
//        result = (String)pos.getAllBenAuftrAllPKZ();
//        System.out.println("Ben_Auftr_alle_PKZ");
//        System.out.println(result);
//        System.out.println("\n######################################################\n");

//        result = (String)pos.getAllBenAuftr(null, false);
//        System.out.println("Ben_Auftr__eine_ANR");
//        System.out.println(result);
//        System.out.println("\n######################################################\n");

        result = (String)pos.getAllBenAuftr(null, true);
        System.out.println("Ben_Auftr__eine_ANR_Wildcard");
        System.out.println(result);
        System.out.println("\n######################################################\n");
    }
}
