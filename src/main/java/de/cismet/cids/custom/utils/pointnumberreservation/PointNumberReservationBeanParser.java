/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.utils.pointnumberreservation;

import org.apache.log4j.Logger;

import org.openide.util.Exceptions;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

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
        final Node gmlIdNode = axReservierung.getAttributes().getNamedItem("gml:id");
        if (gmlIdNode != null) {
            pointNumber.setUuid(gmlIdNode.getNodeValue());
        }
        final NodeList childs = axReservierung.getChildNodes();
        for (int i = 0; i < childs.getLength(); i++) {
            final Node currChild = childs.item(i);
            if (currChild.getNodeName().equals("antragsnummer")) {
                pointNumber.setAntragsnummer(currChild.getTextContent());
            } else if (currChild.getNodeName().equals("ablaufDerReservierung")) {
                pointNumber.setAblaufdatum(currChild.getTextContent());
            } else if (currChild.getNodeName().equals("nummer")) {
                pointNumber.setPunktnummer(currChild.getTextContent());
            } else if (currChild.getNodeName().equals("lebenszeitintervall")) {
                final Node lebenszeitNode = currChild.getChildNodes().item(1);
                final Node beginntNode = lebenszeitNode.getChildNodes().item(1);
                pointNumber.setIntervallbeginn(beginntNode.getTextContent());
            } else if (currChild.getNodeName().equals("vermessungsstelle")) {
                final Node schluesselNode = currChild.getChildNodes().item(1);
                final Node stelleNode = schluesselNode.getChildNodes().item(3);
                pointNumber.setStelle(stelleNode.getTextContent());
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
        final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder dBuilder;
        try {
            final byte[] res = resultString.getBytes("UTF-8");
            byte[] b;
            if (resultString.startsWith("\uFEFF")) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("resultString starts with \\uFEFF which is a UTF-8 BOM. Removing first 3 bytes");
                }
                b = Arrays.copyOfRange(res, 3, res.length);
            } else {
                b = res;
            }

            dBuilder = dbFactory.newDocumentBuilder();
            final InputStream is = new ByteArrayInputStream(b);

            final Document doc = dBuilder.parse(is);
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
                    pointNumber.setPunktnummer(resNumNode.getTextContent());
                    pointNumbers.add(pointNumber);
                }
                requestBean.setPointNumbers(pointNumbers);
            } else {
                final String protokoll = parseProtkoll(doc.getLastChild());
                final List<String> errorMsg = parseProtokollErrorMessages(protokoll);
                requestBean.setProtokoll(protokoll);
                requestBean.setErrorMessages(errorMsg);
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
        final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder dBuilder;
        final HashMap<String, PointNumberReservationRequest> requests =
            new HashMap<String, PointNumberReservationRequest>();
        try {
            final byte[] res = resultString.getBytes("UTF-8");
            byte[] b;
            if (resultString.startsWith("\uFEFF")) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("resultString starts with \\uFEFF which is a UTF-8 BOM. Removing first 3 bytes");
                }
                b = Arrays.copyOfRange(res, 3, res.length);
            } else {
                b = res;
            }
            dBuilder = dbFactory.newDocumentBuilder();
            final InputStream is = new ByteArrayInputStream(b);
            final Document doc = dBuilder.parse(is);
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
                    pnr.setPunktnummer(pointNumber.getPunktnummer());
                    pnr.setIntervallbeginn(pointNumber.getIntervallbeginn());
                    pnr.setVermessungsstelle(pointNumber.getStelle());
                    pnr.setUuid(pointNumber.getUuid());
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
                final String protString = currChild.getTextContent();
                final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                final DocumentBuilder dBuilder;
                try {
                    final byte[] res = protString.getBytes("UTF-8");
                    byte[] bytes;
                    if (protString.startsWith("\uFEFF")) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Protokoll starts with \\uFEFF which is a UTF-8 BOM. Removing first 3 bytes");
                        }
                        bytes = Arrays.copyOfRange(res, 3, res.length);
                    } else {
                        bytes = res;
                    }
                    dBuilder = dbFactory.newDocumentBuilder();
                    final InputStream is = new ByteArrayInputStream(bytes);
                    final Document doc = dBuilder.parse(is);
                    final org.apache.xml.serialize.OutputFormat format = new org.apache.xml.serialize.OutputFormat(
                            doc,
                            "UTF-8",
                            true);
                    // as a String
                    final StringWriter stringOut = new StringWriter();
                    final org.apache.xml.serialize.XMLSerializer serial = new org.apache.xml.serialize.XMLSerializer(
                            stringOut,
                            format);
                    serial.serialize(doc);

                    // set the request id that is shown in the 3A Auftagsmanagement Interface
                    return stringOut.toString();
                } catch (ParserConfigurationException ex) {
                    LOG.error("Could not parse 3A server Protokoll: ", ex);
                } catch (SAXException ex) {
                    LOG.error("Could not parse 3A server Protokoll: ", ex);
                } catch (IOException ex) {
                    LOG.error("Could not parse 3A server Protokoll: ", ex);
                }
            }
        }
        return "";
    }

    /**
     * DOCUMENT ME!
     *
     * @param   protokoll  rootNode DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static List<String> parseProtokollErrorMessages(final String protokoll) {
        final ArrayList<String> errorMsg = new ArrayList<String>();
        try {
            final InputStream result = new ByteArrayInputStream(protokoll.getBytes());
            // parse the queryTemplate and insert the geom in it
            final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            final Document doc = dBuilder.parse(result);
            final Node rootNode = doc.getLastChild();
            final NodeList childs = rootNode.getChildNodes();
            for (int i = 0; i < childs.getLength(); i++) {
                final Node n = childs.item(i);
                if (n.getNodeName().equals("Message")) {
                    final NodeList messageChilds = n.getChildNodes();
                    for (int j = 0; j < messageChilds.getLength(); j++) {
                        final Node tmp = messageChilds.item(j);
                        if (tmp.getNodeName().equals("MessageLevel")) {
                            final String messageLevel = tmp.getTextContent();
                            if (messageLevel.equals("Error")) {
                                final String errorMessage = tmp.getNextSibling().getNextSibling().getTextContent();
                                errorMsg.add(errorMessage);
                            }
                        }
                    }
                }
            }
        } catch (SAXException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ParserConfigurationException ex) {
            Exceptions.printStackTrace(ex);
        }
        return errorMsg;
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
        private String stelle;
        private String intervallbeginn;
        private String uuid;

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

        /**
         * DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         */
        public String getStelle() {
            return stelle;
        }

        /**
         * DOCUMENT ME!
         *
         * @param  stelle  DOCUMENT ME!
         */
        public void setStelle(final String stelle) {
            this.stelle = stelle;
        }

        /**
         * DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         */
        public String getIntervallbeginn() {
            return intervallbeginn;
        }

        /**
         * DOCUMENT ME!
         *
         * @param  intervallbeginn  DOCUMENT ME!
         */
        public void setIntervallbeginn(final String intervallbeginn) {
            this.intervallbeginn = intervallbeginn;
        }

        /**
         * DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         */
        public String getUuid() {
            return uuid;
        }

        /**
         * DOCUMENT ME!
         *
         * @param  uuid  DOCUMENT ME!
         */
        public void setUuid(final String uuid) {
            this.uuid = uuid;
        }
    }
}
