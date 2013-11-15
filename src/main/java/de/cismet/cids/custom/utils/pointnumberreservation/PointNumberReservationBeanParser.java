/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.utils.pointnumberreservation;

import org.apache.log4j.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * DOCUMENT ME!
 *
 * @author   daniel
 * @version  $Revision$, $Date$
 */
public class PointNumberReservationBeanParser {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient Logger LOG = Logger.getLogger(PointNumberReservationBeanParser.class);

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param   rootNode  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static String parseAuftragsnummer(final Node rootNode) {
        final NodeList childs = rootNode.getChildNodes();
        for (int i = 0; i < childs.getLength(); i++) {
            final Node currChild = childs.item(i);
            if (currChild.getNodeName().equals("antragsnummer") || currChild.getNodeName().equals("auftragsnummer")) {
                return currChild.getTextContent();
            }
        }
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   axReservierung  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static PointNumberWrapper parseAxReservierungNode(final Node axReservierung) {
        final PointNumberWrapper pointNumber = new PointNumberWrapper();
        final NodeList childs = axReservierung.getChildNodes();
        for (int i = 0; i < childs.getLength(); i++) {
            final Node currChild = childs.item(i);
            if (currChild.getNodeName().equals("antragsnummer")) {
                pointNumber.setAntragsnummer(currChild.getTextContent());
            } else if (currChild.getNodeName().equals("ablaufDerReservierung")) {
                pointNumber.setAblaufdatum(currChild.getTextContent());
            } else if (currChild.getNodeName().equals("nummer")) {
                pointNumber.setPunktnummer(currChild.getTextContent());
            }
        }
        return pointNumber;
    }

    /**
     * DOCUMENT ME!
     */
    public static void parseFortfuehrungsergebnis() {
    }

    /**
     * DOCUMENT ME!
     *
     * @param   resultString  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static PointNumberReservationRequest parseReservierungsErgebnis(final String resultString) {
        final InputStream result = new ByteArrayInputStream(resultString.getBytes());
        // parse the queryTemplate and insert the geom in it
        final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder dBuilder;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
            final Document doc = dBuilder.parse(result);
            final String anr = parseAuftragsnummer(doc.getLastChild());
            final PointNumberReservationRequest requestBean = new PointNumberReservationRequest();
            requestBean.setAntragsnummer(anr);
            final boolean wasSuccessFull = parseSuccessfull(doc.getLastChild());
            requestBean.setSuccessful(wasSuccessFull);
            if (wasSuccessFull) {
                final ArrayList<PointNumberReservation> pointNumbers = new ArrayList<PointNumberReservation>();
                final NodeList pointNumberNodes = doc.getElementsByTagName("reservierteNummern");
                for (int i = 0; i < pointNumberNodes.getLength(); i++) {
                    final Node resNumNode = pointNumberNodes.item(i);
                    final PointNumberReservation pointNumber = new PointNumberReservation();
                    pointNumber.setPunktnummern(resNumNode.getTextContent());
                    pointNumbers.add(pointNumber);
                }
                requestBean.setPointNumbers(pointNumbers);
            } else {
                final String protokoll = parseProtkoll(doc.getLastChild());
                requestBean.setProtokoll(protokoll);
            }
            return requestBean;
        } catch (ParserConfigurationException ex) {
            LOG.error("Could not parse 3A server Result", ex);
        } catch (SAXException ex) {
            LOG.error("Could not parse 3A server Result", ex);
        } catch (IOException ex) {
            LOG.error("Could not parse 3A server Result", ex);
        }
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   resultString  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static Collection<PointNumberReservationRequest> parseBestandsdatenauszug(final String resultString) {
        final InputStream result = new ByteArrayInputStream(resultString.getBytes());
        // parse the queryTemplate and insert the geom in it
        final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder dBuilder;
        final HashMap<String, PointNumberReservationRequest> requests =
            new HashMap<String, PointNumberReservationRequest>();
        try {
            dBuilder = dbFactory.newDocumentBuilder();
            final Document doc = dBuilder.parse(result);
            final NodeList axReservierungen = doc.getElementsByTagName("AX_Reservierung");
            for (int i = 0; i < axReservierungen.getLength(); i++) {
                final Node axReservierung = axReservierungen.item(i);
                final PointNumberWrapper pointNumber = parseAxReservierungNode(axReservierung);
                if ((pointNumber.getAntragsnummer() != null) && (pointNumber.getPunktnummer() != null)) {
                    if (!requests.containsKey(pointNumber.getAntragsnummer())) {
                        final PointNumberReservationRequest request = new PointNumberReservationRequest();
                        request.setAntragsnummer(pointNumber.getAntragsnummer());
                        requests.put(pointNumber.getAntragsnummer(), request);
                    }
                    final PointNumberReservation pnr = new PointNumberReservation();
                    pnr.setAblaufDatum(pointNumber.getAblaufdatum());
                    pnr.setPunktnummern(pointNumber.getPunktnummer());
                    requests.get(pointNumber.getAntragsnummer()).addPointNumberReservation(pnr);
                }
            }
        } catch (ParserConfigurationException ex) {
            LOG.error("Could not parse 3A server Result", ex);
        } catch (SAXException ex) {
            LOG.error("Could not parse 3A server Result", ex);
        } catch (IOException ex) {
            LOG.error("Could not parse 3A server Result", ex);
        }
        return requests.values();
    }

    /**
     * DOCUMENT ME!
     *
     * @param   rootNode  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static boolean parseSuccessfull(final Node rootNode) {
        final NodeList childs = rootNode.getChildNodes();
        boolean b = false;
        for (int i = 0; i < childs.getLength(); i++) {
            final Node currChild = childs.item(i);
            if (currChild.getNodeName().equals("erfolgreich")) {
                b = Boolean.parseBoolean(currChild.getTextContent());
                break;
            }
        }
        return b;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   rootNode  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static String parseProtkoll(final Node rootNode) {
        final NodeList childs = rootNode.getChildNodes();
        final boolean b = false;
        for (int i = 0; i < childs.getLength(); i++) {
            final Node currChild = childs.item(i);
            if (currChild.getNodeName().equals("erlaeuterung")) {
                return currChild.getTextContent();
            }
        }
        return "";
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    private static final class PointNumberWrapper {

        //~ Instance fields ----------------------------------------------------

        private String antragsnummer;
        private String ablaufdatum;
        private String punktnummer;

        //~ Methods ------------------------------------------------------------

        /**
         * DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         */
        public String getAntragsnummer() {
            return antragsnummer;
        }

        /**
         * DOCUMENT ME!
         *
         * @param  antragsnummer  DOCUMENT ME!
         */
        public void setAntragsnummer(final String antragsnummer) {
            this.antragsnummer = antragsnummer;
        }

        /**
         * DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         */
        public String getAblaufdatum() {
            return ablaufdatum;
        }

        /**
         * DOCUMENT ME!
         *
         * @param  ablaufdatum  DOCUMENT ME!
         */
        public void setAblaufdatum(final String ablaufdatum) {
            this.ablaufdatum = ablaufdatum;
        }

        /**
         * DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         */
        public String getPunktnummer() {
            return punktnummer;
        }

        /**
         * DOCUMENT ME!
         *
         * @param  punktnummer  DOCUMENT ME!
         */
        public void setPunktnummer(final String punktnummer) {
            this.punktnummer = punktnummer;
        }
    }
}
