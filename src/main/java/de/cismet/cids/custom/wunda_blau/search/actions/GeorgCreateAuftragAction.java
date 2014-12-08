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
package de.cismet.cids.custom.wunda_blau.search.actions;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import org.w3c.dom.Document;

import org.xml.sax.InputSource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;

import java.net.HttpURLConnection;
import java.net.URL;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;

/**
 * DOCUMENT ME!
 *
 * @author   thorsten
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class GeorgCreateAuftragAction implements ServerAction {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(GeorgCreateAuftragAction.class);

    public static final String ACTION_NAME = "georgCreateAuftragAction";
    public static final String GEORG_SOAP_SERVICE = "http://s102x003:6050";

    //~ Methods ----------------------------------------------------------------

    @Override
    public Object execute(final Object body, final ServerActionParameter... params) {
        try {
            final String jobId = createAuftrag("Amtlicher Lageplan", String.valueOf(System.currentTimeMillis()));
            final String auftragsnummer = getAuftragsnummer(jobId);
            return auftragsnummer;
        } catch (Exception e) {
            LOG.error("Error in GeorgCreateAuftragAction", e);
        }
        return null;
    }

    @Override
    public String getTaskName() {
        return ACTION_NAME;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   args  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public static void main(final String[] args) throws Exception {
        System.out.println("try");
        final String[] arten = { "Geobasis_Land", "Geobasis_LieKa", "Geodaten_kom" };
        for (final String art : arten) {
            System.out.println("=== Test für Auftragsart:" + art);
            final GeorgCreateAuftragAction gcaa = new GeorgCreateAuftragAction();
            final String eigNr = String.valueOf(System.currentTimeMillis());
            final String jobId = gcaa.createAuftrag(art, eigNr);
            System.out.println("          eigene Nummer: " + eigNr);
            System.out.println("          SOAP Job Id: " + jobId);
            // Thread.sleep(500);
            final String auftragsnummer = gcaa.getAuftragsnummer(jobId);
            System.out.println("     -->  GEORG-Auftragsnummer: " + auftragsnummer);
        }
        System.out.println("done");
    }

    /**
     * DOCUMENT ME!
     *
     * @param   auftragsart  DOCUMENT ME!
     * @param   eigeneNr     DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private String createAuftrag(final String auftragsart, final String eigeneNr) throws Exception {
        final String soapRequest = IOUtils.toString(GeorgCreateAuftragAction.class.getResourceAsStream(
                    "/de/cismet/cids/custom/wunda_blau/search/actions/georg/executeJob_Request.xml"));
        final String processed = String.format(soapRequest, auftragsart, eigeneNr);
        return manuallyPostRequestAndParseWithXPath(processed, "//executeJobReturn");
    }

    /**
     * DOCUMENT ME!
     *
     * @param   jobId  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private String getAuftragsnummer(final String jobId) throws Exception {
        final String soapRequest = IOUtils.toString(GeorgCreateAuftragAction.class.getResourceAsStream(
                    "/de/cismet/cids/custom/wunda_blau/search/actions/georg/getJobStatus_Request.xml"));
        final String processed = String.format(soapRequest, jobId);
        return manuallyPostRequestAndParseWithXPath(processed, "//geschaeftsbuchnummer");
    }

    /**
     * DOCUMENT ME!
     *
     * @param   soapRequest  DOCUMENT ME!
     * @param   xPath        DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String manuallyPostRequestAndParseWithXPath(final String soapRequest, final String xPath) {
        HttpURLConnection conn = null;
        try {
            final URL url = new URL(GEORG_SOAP_SERVICE);
            conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestProperty("SOAPAction", "SOAPAction");
            conn.setDoOutput(true);
            final OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
            writer.write(soapRequest);
            writer.flush();

            String line;
            final BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            final StringBuffer response = new StringBuffer();
            while ((line = reader.readLine()) != null) {
                response.append(line).append('\n');
            }
            writer.close();
            reader.close();
            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                            + conn.getResponseCode());
            } else {
                final XPathFactory xpathFactory = XPathFactory.newInstance();
                final XPath xpath = xpathFactory.newXPath();

                final InputSource source = new InputSource(new StringReader(response.toString()));
                final Document doc = (Document)xpath.evaluate("/", source, XPathConstants.NODE);
                String out = "nix";
                out = (String)xpath.evaluate(
                        xPath,
                        doc);
                return out;
            }
        } catch (Exception e) {
            java.awt.Toolkit.getDefaultToolkit().beep();
            e.printStackTrace();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return null;
    }
}
