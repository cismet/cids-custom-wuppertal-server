/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.utils.nas;

import Sirius.server.middleware.impls.domainserver.DomainServerImpl;
import Sirius.server.newuser.User;
import Sirius.server.property.ServerProperties;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryCollection;

import de.aed_sicad.namespaces.svr.AMAuftragServer;
import de.aed_sicad.namespaces.svr.AuftragsManager;
import de.aed_sicad.namespaces.svr.AuftragsManagerSoap;

import org.apache.commons.io.IOUtils;

import org.openide.util.Exceptions;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;

import java.net.URL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import de.cismet.cids.server.api.types.ActionTask;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 * DOCUMENT ME!
 *
 * @author   daniel
 * @version  $Revision$, $Date$
 */
public class NASProductGenerator {

    //~ Static fields/initializers ---------------------------------------------

    private static final String FILE_APPENDIX = ".xml";
    private static NASProductGenerator instance;
    private static final int REQUEST_PERIOD = 3000;
    private static final String REQUEST_PLACE_HOLDER = "REQUEST-ID";
    private static final String DATA_FORMAT_STD = "<datenformat>1000</datenformat>";
    private static final String DATA_FORMAT_500 = "<datenformat>NAS_500m</datenformat>";

    //~ Instance fields --------------------------------------------------------

    private String EIGENTUEMER_TEMPLATE_RES = "/de/cismet/cids/custom/utils/nas/A_o_eigentuemer.xml";
    private String KOMPLETT_TEMPLATE_RES = "/de/cismet/cids/custom/utils/nas/A_komplett.xml";
    private String POINTS_TEMPLATE_RES = "/de/cismet/cids/custom/utils/nas/A_points.xml";
    private File openOrdersLogFile;
    private File undeliveredOrdersLogFile;
    private final transient org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(this.getClass());
    private AuftragsManagerSoap manager;
    private String SERVICE_URL;
    private String USER;
    private String PW;
    private String OUTPUT_DIR;
    private String ACTION_SERVICE;
    private String ACTION_DOMAIN;
    private HashMap<String, HashMap<String, NasProductInfo>> openOrderMap =
        new HashMap<String, HashMap<String, NasProductInfo>>();
    private HashMap<String, HashMap<String, NasProductInfo>> undeliveredOrderMap =
        new HashMap<String, HashMap<String, NasProductInfo>>();
    private HashMap<String, NasProductDownloader> downloaderMap = new HashMap<String, NasProductDownloader>();
    private boolean initError = false;
    private DXFConverterAction dxfConverter;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new NASProductGenerator object.
     */
    private NASProductGenerator() {
        final Properties serviceProperties = new Properties();
        try {
            final ServerProperties serverProps = DomainServerImpl.getServerProperties();
            final String resFolder = serverProps.getServerResourcesBasePath();
            KOMPLETT_TEMPLATE_RES = resFolder + "/de/cismet/cids/custom/utils/nas/A_komplett.xml";
            EIGENTUEMER_TEMPLATE_RES = resFolder + "/de/cismet/cids/custom/utils/nas/A_o_eigentuemer.xml";
            POINTS_TEMPLATE_RES = resFolder + "/de/cismet/cids/custom/utils/nas/A_points.xml";

            if (!checkTemplateFilesAccesible()) {
                log.warn(
                    "NAS Datenabgabe initialisation Error. Could not read all necessary template files. NAS support is disabled");
                initError = true;
                return;
            }

            serviceProperties.load(NASProductGenerator.class.getResourceAsStream("nasServer_conf.properties"));
            SERVICE_URL = serviceProperties.getProperty("service");
            USER = serviceProperties.getProperty("user");
            PW = serviceProperties.getProperty("pw");
            OUTPUT_DIR = serviceProperties.getProperty("outputDir");
            ACTION_DOMAIN = serviceProperties.getProperty("actionDomain");
            ACTION_SERVICE = serviceProperties.getProperty("actionServiceURL");
            if ((OUTPUT_DIR == null) || OUTPUT_DIR.isEmpty()) {
                log.info("Could not read nas nas output dir property. using server working dir as fallback");
                OUTPUT_DIR = ".";
            }
            if (((SERVICE_URL == null) || SERVICE_URL.isEmpty()) || ((USER == null) || USER.isEmpty())
                        || ((PW == null) || PW.isEmpty())) {
                log.warn(
                    "NAS Datenabgabe initialisation Error. Could not read all properties for connecting 3A Server. NAS support is disabled");
                initError = true;
                return;
            }
            final File outputDir = new File(OUTPUT_DIR);
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            if (!outputDir.isDirectory() || !outputDir.canWrite()) {
                log.warn("NAS Datenabgabe initialisation Error. Could not write to the given nas output directory: "
                            + outputDir);
                initError = true;
                return;
            }
            if ((ACTION_DOMAIN == null) || (ACTION_SERVICE == null)) {
                log.warn(
                    "NAS Datenabgabe initialisation Error. Can not read properties for connecting to DXF converter Action");
                initError = true;
                return;
            }
            dxfConverter = new DXFConverterAction(ACTION_DOMAIN, ACTION_SERVICE);
            final StringBuilder fileNameBuilder = new StringBuilder(OUTPUT_DIR);
            fileNameBuilder.append(System.getProperty("file.separator"));
            openOrdersLogFile = new File(fileNameBuilder.toString() + "openOrdersMap.json");
            undeliveredOrdersLogFile = new File(fileNameBuilder.toString() + "undeliveredOrdersMap.json");
            if (!openOrdersLogFile.exists()) {
                openOrdersLogFile.createNewFile();
            }
            if (!undeliveredOrdersLogFile.exists()) {
                undeliveredOrdersLogFile.createNewFile();
                // serialiaze en empty map to the file to avoid parsing exception
                updateJsonLogFiles();
            }
            if (!(openOrdersLogFile.isFile() && openOrdersLogFile.canWrite())
                        || !(undeliveredOrdersLogFile.isFile() && undeliveredOrdersLogFile.canWrite())) {
                log.warn(
                    "NAS Datenabgabe initialisation Error. Could not write to NAS order log files. NAS support is disabled");
                initError = true;
                return;
            }
            initFromOrderLogFiles();
        } catch (Exception ex) {
            log.warn("NAS Datenabgabe initialisation Error! NAS support is disabled", ex);
            initError = true;
        }
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static NASProductGenerator instance() {
        if (instance == null) {
            instance = new NASProductGenerator();
        }
        return instance;
    }

    /**
     * DOCUMENT ME!
     */
    private void initFromOrderLogFiles() {
        loadFromLogFile(openOrderMap, openOrdersLogFile);
        loadFromLogFile(undeliveredOrderMap, undeliveredOrdersLogFile);
        // check of there are open orders that arent downloaded from the 3a server yet
        for (final String userId
                    : openOrderMap.keySet()) {
            final HashMap<String, NasProductInfo> openOrderIds = openOrderMap.get(userId);
//            for (final String orderId : openOrderIds.keySet()) {
//                final NasProductDownloader downloader = new NasProductDownloader(userId, orderId);
//                downloaderMap.put(orderId, downloader);
//                final Thread workerThread = new Thread(downloader);
//                workerThread.start();
//            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public boolean reInitFromOrderLogFiles() {
//        if (!openOrderMap.isEmpty()) {
//            log.info(
//                "The open order map (requests that need to be downloaded from the cids server) is not empty. can not re-init from log files");
//            return false;
//        }
//        if (!undeliveredOrderMap.isEmpty()) {
//            log.info(
//                "The open order map (requests that need to be downloaded from the client) is not empty. can not re-init from log files");
//            return false;
//        }
        openOrderMap = new HashMap<String, HashMap<String, NasProductInfo>>();
        undeliveredOrderMap = new HashMap<String, HashMap<String, NasProductInfo>>();
        initFromOrderLogFiles();
        return true;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   geom          DOCUMENT ME!
     * @param   templateFile  DOCUMENT ME!
     * @param   requestName   DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private InputStream generateQeury(final GeometryCollection geom,
            final InputStream templateFile,
            final String requestName) {
        int gmlId = 0;
        try {
            final String xmlGeom = GML3Writer.writeGML3_2WithETRS89(geom);
            // parse the queryTemplate and insert the geom in it
            final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            final Document doc = dBuilder.parse(templateFile);
            final NodeList intersectNodes = doc.getElementsByTagName("ogc:Intersects");
            final Document doc2 = dBuilder.parse(new InputSource(new StringReader(xmlGeom)));
            final Element newPolygonNode = doc2.getDocumentElement();
            for (int i = 0; i < intersectNodes.getLength(); i++) {
                Node oldPolygonNode = null;
                Node child = intersectNodes.item(i).getFirstChild();
                while (child != null) {
                    if (child.getNodeName().equals("gml:Polygon")) {
                        oldPolygonNode = child;
                        break;
                    }
                    child = child.getNextSibling();
                }
                if (oldPolygonNode == null) {
                    log.error("corrupt query template file, could not find a geometry node");
                }
                newPolygonNode.setAttribute("gml:id", "G" + gmlId);
                gmlId++;
                // set id for surface nodes
                final NodeList surfaceNodes = newPolygonNode.getElementsByTagName("gml:Surface");
                for (int j = 0; j < surfaceNodes.getLength(); j++) {
                    final Element surfaceNode = (Element)surfaceNodes.item(j);
                    surfaceNode.setAttribute("gml:id", "G" + gmlId);
                    gmlId++;
                }
                final Node importedNode = doc.importNode(newPolygonNode, true);
                intersectNodes.item(i).removeChild(oldPolygonNode);
                intersectNodes.item(i).appendChild(importedNode);
            }
            final OutputFormat format = new OutputFormat(doc);
            // as a String
            final StringWriter stringOut = new StringWriter();
            final XMLSerializer serial = new XMLSerializer(stringOut,
                    format);
            serial.serialize(doc);

            // set the request id that is shown in the 3A Auftagsmanagement Interface
            String request = stringOut.toString();
            request = request.replaceAll(REQUEST_PLACE_HOLDER, requestName);

            // check if this request needs to be portioned
            if (isOrderSplitted(geom)) {
                request = request.replaceAll(DATA_FORMAT_STD, DATA_FORMAT_500);
            }
            if (log.isDebugEnabled()) {
                log.debug(request);
            }
            return new ByteArrayInputStream(request.getBytes());
        } catch (ParserConfigurationException ex) {
            log.error("Parser Configuration Error", ex);
        } catch (SAXException ex) {
            log.error("Error during parsing document", ex);
        } catch (IOException ex) {
            log.error("Error while openeing nas template file", ex);
        }
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   product  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private InputStream loadTemplateFile(final NasProduct product) {
        InputStream templateFile = null;
        try {
            if ((product != null) && (product.getTemplate() != null)) {
                final ServerProperties serverProps = DomainServerImpl.getServerProperties();
                final String resPath = serverProps.getServerResourcesBasePath();
                templateFile = new FileInputStream(resPath + product.getTemplate());
            }
        } catch (FileNotFoundException ex) {
            log.fatal("Could not read template template file for Template :" + product.toString(), ex);
        }
        return templateFile;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   product      DOCUMENT ME!
     * @param   geoms        DOCUMENT ME!
     * @param   user         DOCUMENT ME!
     * @param   requestName  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String executeAsynchQuery(final NasProduct product,
            final GeometryCollection geoms,
            final User user,
            final String requestName) {
        if (initError) {
            if (log.isDebugEnabled()) {
                log.debug("NASProductGenerator doesnt work hence there was an error during the initialisation.");
            }
            return null;
        }
//        try {
        final InputStream templateFile = loadTemplateFile(product);

        if (templateFile == null) {
            log.error("Error laoding the NAS template file.");
            return null;
        }

        if (geoms == null) {
            log.error("geometry is null, cannot execute nas query");
            return null;
        }

        final String requestId = getRequestId(user, requestName);
        final InputStream preparedQuery = generateQeury(geoms, templateFile, requestId);
        initAmManager();
        final int sessionID = manager.login(USER, PW);
        final String orderId = manager.registerGZip(sessionID, gZipFile(preparedQuery));

        final boolean isSplitted = isOrderSplitted(geoms);
        final boolean isDXF = product.getFormat().equals(NasProduct.Format.DXF.toString());
        addToOpenOrders(determineUserPrefix(user), orderId, new NasProductInfo(isSplitted, requestName, isDXF));
        addToUndeliveredOrders(determineUserPrefix(user), orderId, new NasProductInfo(isSplitted, requestName, isDXF));
        final NasProductDownloader downloader = new NasProductDownloader(determineUserPrefix(user), orderId, isDXF);
        downloaderMap.put(orderId, downloader);
        final Thread workerThread = new Thread(downloader);
        workerThread.start();

        return orderId;
//        } catch (RemoteException ex) {
//            log.error("could not create conenction to 3A Server", ex);
//        }
//        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  query  DOCUMENT ME!
     * @param  file   DOCUMENT ME!
     */
    public void writeResultToFileforRequest(final InputStream query, final File file) {
        if (initError) {
            if (log.isDebugEnabled()) {
                log.debug("NASProductGenerator doesnt work hence there was an error during the initialisation.");
            }
            return;
        }
        initAmManager();
        final int sessionID = manager.login(USER, PW);
        final String orderId = manager.registerGZip(sessionID, gZipFile(query));
        final AMAuftragServer amServer = manager.listAuftrag(sessionID, orderId);

        while ((manager.getResultCount(sessionID, orderId) < 1)
                    && (manager.getProtocolGZip(sessionID, orderId) == null)) {
            try {
                Thread.sleep(1000);
//                this.logProtocol(manager.getProtocolGZip(sessionID, orderId));
            } catch (InterruptedException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        final int resCount = manager.getResultCount(sessionID, orderId);

        if (resCount > 1) {
            // unzip and save all files, then zip them
            final ArrayList<byte[]> resultFiles = new ArrayList<byte[]>();
            for (int i = 0; i < resCount; i++) {
                resultFiles.add(manager.getNResultGZip(sessionID, orderId, i));
            }
            final ArrayList<byte[]> unzippedFileCollection = new ArrayList<byte[]>();
            for (final byte[] zipFile : resultFiles) {
                unzippedFileCollection.add(gunzip(zipFile));
            }
            FileOutputStream fos = null;
            ZipOutputStream zos = null;
            try {
                fos = new FileOutputStream(file);
                zos = new ZipOutputStream(fos);
                for (int i = 0; i < unzippedFileCollection.size(); i++) {
                    final byte[] unzippedFile = unzippedFileCollection.get(i);
                    final String fileEntryName = orderId + "#" + i + FILE_APPENDIX;
                    zos.putNextEntry(new ZipEntry(fileEntryName));
                    zos.write(unzippedFile);
                    zos.closeEntry();
                }
            } catch (IOException ex) {
                log.warn("error during creation of zip file");
            } finally {
                try {
                    if (zos != null) {
                        zos.close();
                    }
                    if (fos != null) {
                        fos.close();
                    }
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        } else {
            final byte[] data;
            if (resCount == 0) {
                log.error("it seems that there is an error with NAS order: " + orderId + ". Writing protocol to file "
                            + file);
                log.error("Protocol for NAS order " + orderId + ": "
                            + new String(gunzip(manager.getProtocolGZip(sessionID, orderId))));
                data = manager.getProtocolGZip(sessionID, orderId);
            } else {
                data = manager.getResultGZip(sessionID, orderId);
            }

            if (data == null) {
                log.error("result of nas order " + orderId + " is null");
                return;
            }
            InputStream is = null;
            OutputStream os = null;
            try {
                is = new GZIPInputStream(new ByteArrayInputStream(manager.getResultGZip(sessionID, orderId)));
                os = new FileOutputStream(file);
                final byte[] buffer = new byte[8192];
                int length = is.read(buffer, 0, 8192);
                while (length != -1) {
                    os.write(buffer, 0, length);
                    length = is.read(buffer, 0, 8192);
                }
            } catch (IOException ex) {
                log.error("error during gunzip of nas response files", ex);
            } finally {
                try {
                    if (is != null) {
                        is.close();
                    }
                    if (os != null) {
                        os.close();
                    }
                } catch (IOException ex) {
                }
            }
        }
    }

    /**
     * DOCUMENT ME!
     */
    private void initAmManager() {
//        try {
//            final AuftragsManagerLocator am = new AuftragsManagerLocator();
//            manager = am.getAuftragsManagerSoap(new URL(SERVICE_URL));
        final AuftragsManager am;
        try {
            am = new AuftragsManager(new URL(SERVICE_URL));
        } catch (Exception ex) {
            log.error("error creating 3AServer interface", ex);
            return;
        }
        manager = am.getAuftragsManagerSoap();
//        } catch (Exception ex) {
//            log.error("error creating 3AServer interface", ex);
//            Exceptions.printStackTrace(ex);
//        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   orderId  DOCUMENT ME!
     * @param   user     DOCUMENT ME!
     *
     * @return  DOCUMcd ENT ME!
     */
    public byte[] getResultForOrder(final String orderId, final User user) {
        if (initError) {
            if (log.isDebugEnabled()) {
                log.debug("NASProductGenerator doesnt work hence there was an error during the initialisation.");
            }
            return null;
        }
        final HashMap<String, NasProductInfo> openUserOrders = openOrderMap.get(determineUserPrefix(user));
        if ((openUserOrders != null) && openUserOrders.keySet().contains(orderId)) {
//            if (log.isDebugEnabled()) {
//                log.debug("requesting an order that isnt not done");
//            }
            return new byte[0];
        }
        final HashMap<String, NasProductInfo> undeliveredUserOrders = undeliveredOrderMap.get(determineUserPrefix(
                    user));
        if ((undeliveredUserOrders == null) || undeliveredUserOrders.isEmpty()) {
            log.error("there are no undelivered nas orders for the user " + user.toString());
            return null;
        }
        if (!undeliveredUserOrders.keySet().contains(orderId)) {
            log.error("there is no order for user " + user.toString() + " with order id " + orderId);
            return null;
        }
        final NasProductInfo productInfo = undeliveredUserOrders.get(orderId);

        removeFromUndeliveredOrders(determineUserPrefix(user), orderId);
        String fileExtension = ".xml";
        if (productInfo.isDxf()) {
            fileExtension = ".dxf";
        } else if (productInfo.isIsSplittet()) {
            fileExtension = ".zip";
        }
        return loadFile(determineUserPrefix(user), orderId, fileExtension);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   orderId   DOCUMENT ME!
     * @param   userId    DOCUMENT ME!
     * @param   isZipped  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public File getNasFileForOrder(final String orderId, final String userId, final boolean isZipped) {
        if (initError) {
            if (log.isDebugEnabled()) {
                log.debug("NASProductGenerator doesnt work hence there was an error during the initialisation.");
            }
            return null;
        }
        return new File(determineFileName(userId, orderId, isZipped ? ".zip" : ".xml"));
    }

    /**
     * DOCUMENT ME!
     *
     * @param   user  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public HashMap<String, NasProductInfo> getUndeliveredOrders(final User user) {
        final HashMap<String, NasProductInfo> result = new HashMap<String, NasProductInfo>();
        if (initError) {
            if (log.isDebugEnabled()) {
                log.debug("NASProductGenerator doesnt work hence there was an error during the initialisation.");
            }
            return result;
        }
        final HashMap<String, NasProductInfo> undeliveredOrders = undeliveredOrderMap.get(determineUserPrefix(user));
        if ((undeliveredOrders != null) && !undeliveredOrders.isEmpty()) {
            for (final String undeliveredOrderId : undeliveredOrders.keySet()) {
                final NasProductInfo pInfo = (NasProductInfo)undeliveredOrders.get(undeliveredOrderId);
                result.put(
                    undeliveredOrderId,
                    new NasProductInfo(pInfo.isIsSplittet(), new String(pInfo.getRequestName()), pInfo.isDxf()));
            }
        }
        return result;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  orderId  DOCUMENT ME!
     * @param  user     DOCUMENT ME!
     */
    public void cancelOrder(final String orderId, final User user) {
        if (initError) {
            if (log.isDebugEnabled()) {
                log.debug("NASProductGenerator doesnt work hence there was an error during the initialisation.");
            }
            return;
        }
        final String userKey = determineUserPrefix(user);
        final NasProductDownloader downloader = downloaderMap.get(orderId);
        if (downloader != null) {
            downloader.setInterrupted(true);
            downloaderMap.remove(orderId);
        }
        removeFromOpenOrders(userKey, orderId);
        removeFromUndeliveredOrders(userKey, orderId);
        deleteFileIfExists(orderId, user);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   is  queryFile DOCUMENT ME!
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
            log.error("error during gzip of gile", ex);
        } catch (IOException ex) {
            log.error("error during gzip of gile", ex);
        } finally {
        }
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  protocol  DOCUMENT ME!
     */
    private void logProtocol(final byte[] protocol) {
        final byte[] unzippedProtocol = gunzip(protocol);
        if (log.isDebugEnabled()) {
            log.debug("Nas Protokoll " + new String(unzippedProtocol));
        }
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
            log.error("error during gunzip of nas response files", ex);
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
     * @param  userKey      DOCUMENT ME!
     * @param  orderId      DOCUMENT ME!
     * @param  zippedFiles  DOCUMENT ME!
     */
    private void saveZipFileOfUnzippedFileCollection(final String userKey,
            final String orderId,
            final ArrayList<byte[]> zippedFiles) {
        final ArrayList<byte[]> unzippedFileCollection = new ArrayList<byte[]>();
        for (final byte[] zipFile : zippedFiles) {
            unzippedFileCollection.add(gunzip(zipFile));
        }
        final String filename = determineFileName(userKey, orderId);
        final File file = new File(filename.replace(FILE_APPENDIX, ".zip"));
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        FileOutputStream fos = null;
        ZipOutputStream zos = null;
        try {
            fos = new FileOutputStream(file);
            zos = new ZipOutputStream(fos);
            for (int i = 0; i < unzippedFileCollection.size(); i++) {
                final byte[] unzippedFile = unzippedFileCollection.get(i);
                final String fileEntryName = orderId + "#" + i + FILE_APPENDIX;
                zos.putNextEntry(new ZipEntry(fileEntryName));
                zos.write(unzippedFile);
                zos.closeEntry();
            }
        } catch (IOException ex) {
            log.warn("error during creation of zip file");
        } finally {
            try {
                if (zos != null) {
                    zos.close();
                }
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param  userKey  DOCUMENT ME!
     * @param  orderId  DOCUMENT ME!
     * @param  data     DOCUMENT ME!
     */
    private void unzipAndSaveFile(final String userKey, final String orderId, final byte[] data) {
        if (data == null) {
            log.error("result of nas order " + orderId + " is null");
            return;
        }
        final File file = new File(determineFileName(userKey, orderId));
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new GZIPInputStream(new ByteArrayInputStream(data));
//            is = new ByteArrayInputStream(data);
            os = new FileOutputStream(file);
            final byte[] buffer = new byte[8192];
            int length = is.read(buffer, 0, 8192);
            while (length != -1) {
                os.write(buffer, 0, length);
                length = is.read(buffer, 0, 8192);
            }
        } catch (IOException ex) {
            log.error("error during gunzip of nas response files", ex);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (os != null) {
                    os.close();
                }
            } catch (IOException ex) {
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   userKey        DOCUMENT ME!
     * @param   orderId        DOCUMENT ME!
     * @param   fileExtension  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private byte[] loadFile(final String userKey, final String orderId, final String fileExtension) {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        InputStream is = null;
        try {
            is = new FileInputStream(determineFileName(userKey, orderId, fileExtension));
            final byte[] buffer = new byte[8192];
            int length = is.read(buffer, 0, 8192);
            while (length != -1) {
                bos.write(buffer, 0, length);
                length = is.read(buffer, 0, 8192);
            }
            return bos.toByteArray();
        } catch (FileNotFoundException ex) {
            log.error("could not find result file for order id " + orderId, ex);
        } catch (IOException ex) {
            log.error("error during loading result file for order id " + orderId, ex);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                bos.close();
            } catch (IOException ex) {
            }
        }
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   userKey  DOCUMENT ME!
     * @param   orderId  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String determineFileName(final String userKey, final String orderId) {
        return determineFileName(userKey, orderId, FILE_APPENDIX);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   userKey        DOCUMENT ME!
     * @param   orderId        DOCUMENT ME!
     * @param   fileExtension  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String determineFileName(final String userKey, final String orderId, final String fileExtension) {
        final StringBuilder fileNameBuilder = new StringBuilder(OUTPUT_DIR);
        fileNameBuilder.append(System.getProperty("file.separator"));
        fileNameBuilder.append(userKey);
        fileNameBuilder.append(System.getProperty("file.separator"));
        fileNameBuilder.append(orderId);
        fileNameBuilder.append(fileExtension);
        return fileNameBuilder.toString();
    }

    /**
     * DOCUMENT ME!
     *
     * @param   user  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String determineUserPrefix(final User user) {
        String prefix = user.getId() + "_" + user.getName();
        prefix = prefix.replaceAll("Ö", "oe");
        prefix = prefix.replaceAll("ö", "oe");
        prefix = prefix.replaceAll("Ä", "ae");
        prefix = prefix.replaceAll("ä", "ae");
        prefix = prefix.replaceAll("Ü", "ue");
        prefix = prefix.replaceAll("ü", "ue");
        prefix = prefix.replaceAll("ß", "ss");
        return prefix;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  userKey  userId DOCUMENT ME!
     * @param  orderId  DOCUMENT ME!
     * @param  pInfo    isSplitted DOCUMENT ME!
     */
    private void addToOpenOrders(final String userKey, final String orderId, final NasProductInfo pInfo) {
        HashMap<String, NasProductInfo> openUserOders = openOrderMap.get(userKey);
        if (openUserOders == null) {
            openUserOders = new HashMap<String, NasProductInfo>();
            openOrderMap.put(userKey, openUserOders);
        }
        openUserOders.put(orderId, pInfo);
        updateJsonLogFiles();
    }

    /**
     * DOCUMENT ME!
     *
     * @param  userKey  userId DOCUMENT ME!
     * @param  orderId  DOCUMENT ME!
     */
    private void removeFromOpenOrders(final String userKey, final String orderId) {
        final HashMap<String, NasProductInfo> openUserOrders = openOrderMap.get(userKey);
        if (openUserOrders == null) {
            log.info("there are no undelivered nas orders for the user with id " + userKey);
            return;
        }
        openUserOrders.remove(orderId);
        if (openUserOrders.isEmpty()) {
            openOrderMap.remove(userKey);
        }
        updateJsonLogFiles();
    }

    /**
     * DOCUMENT ME!
     *
     * @param  userKey  userId DOCUMENT ME!
     * @param  orderId  DOCUMENT ME!
     * @param  pInfo    isSplitted DOCUMENT ME!
     */
    private void addToUndeliveredOrders(final String userKey, final String orderId, final NasProductInfo pInfo) {
        HashMap<String, NasProductInfo> undeliveredUserOders = undeliveredOrderMap.get(userKey);
        if (undeliveredUserOders == null) {
            undeliveredUserOders = new HashMap<String, NasProductInfo>();
            undeliveredOrderMap.put(userKey, undeliveredUserOders);
        }
        undeliveredUserOders.put(orderId, pInfo);
        updateJsonLogFiles();
    }

    /**
     * DOCUMENT ME!
     *
     * @param  userKey  userId DOCUMENT ME!
     * @param  orderId  DOCUMENT ME!
     */
    private void removeFromUndeliveredOrders(final String userKey, final String orderId) {
        final HashMap<String, NasProductInfo> undeliveredUserOders = undeliveredOrderMap.get(userKey);
        if (undeliveredUserOders == null) {
            log.info("there are no undelivered nas orders for the user with id " + userKey);
            return;
        }
        undeliveredUserOders.remove(orderId);
        if (undeliveredUserOders.isEmpty()) {
            undeliveredOrderMap.remove(userKey);
        }
        updateJsonLogFiles();
    }

    /**
     * DOCUMENT ME!
     */
    private synchronized void updateJsonLogFiles() {
        final ObjectMapper mapper = new ObjectMapper();
        final ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();

        try {
            final HashMap<String, MapWrapper> openOrdersToSerialize = new HashMap<String, MapWrapper>();
            for (final String i : openOrderMap.keySet()) {
                final MapWrapper openuserOders = new MapWrapper(openOrderMap.get(i));
                openOrdersToSerialize.put(i, openuserOders);
            }
            writer.writeValue(openOrdersLogFile, openOrdersToSerialize);

            final HashMap<String, MapWrapper> undeliveredOrdersToSerialize = new HashMap<String, MapWrapper>();
            for (final String i : undeliveredOrderMap.keySet()) {
                final MapWrapper openuserOders = new MapWrapper(undeliveredOrderMap.get(i));
                undeliveredOrdersToSerialize.put(i, openuserOders);
            }

            writer.writeValue(undeliveredOrdersLogFile, undeliveredOrdersToSerialize);
        } catch (IOException ex) {
            log.error("error during writing open butler orders to log file", ex);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param  orderId  DOCUMENT ME!
     * @param  user     DOCUMENT ME!
     */
    private void deleteFileIfExists(final String orderId, final User user) {
        final String userKey = determineUserPrefix(user);
        final File file = new File(determineFileName(userKey, orderId));
        if (file.exists()) {
            if (!file.delete()) {
                log.warn("could not delete file " + file.toString());
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   user       DOCUMENT ME!
     * @param   requestId  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String getRequestId(final User user, final String requestId) {
        return user.getName() + "_" + requestId;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   geoms  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private boolean isOrderSplitted(final GeometryCollection geoms) {
        final Envelope env = geoms.getEnvelopeInternal();
        final double xSize = env.getMaxX() - env.getMinX();
        final double ySize = env.getMaxY() - env.getMinY();

        if ((xSize > 500) && (ySize > 500)) {
            return true;
        }
        return false;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  map                DOCUMENT ME!
     * @param  openOrdersLogFile  DOCUMENT ME!
     */
    private void loadFromLogFile(final HashMap<String, HashMap<String, NasProductInfo>> map,
            final File openOrdersLogFile) {
        final ObjectMapper mapper = new ObjectMapper();
        try {
            final HashMap<String, MapWrapper> wrapperMap = mapper.readValue(
                    openOrdersLogFile,
                    new TypeReference<HashMap<String, MapWrapper>>() {
                    });
            for (final String s : wrapperMap.keySet()) {
                map.put(s, wrapperMap.get(s).getMap());
            }
        } catch (JsonParseException ex) {
            log.warn("Could not parse nas order log files", ex);
        } catch (JsonMappingException ex) {
            log.warn("error while json mapping/unmarshalling of nas order log file", ex);
        } catch (IOException ex) {
            log.warn("error while loading nas order log file", ex);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param  args  DOCUMENT ME!
     */
    public static void main(final String[] args) {
        FileInputStream fis = null;
        try {
//            final InputStream templateFile = NASProductGenerator.class.getResourceAsStream(
//                    "test_request.xml");
            fis = new FileInputStream(new File(
                        "/home/daniel/Documents/punktreservierung/Wunda_Reservierung2/Muster-Dateien/A_AMGR000000003012_Ben_Auftr_alle_PKZ_alt.xml"));
            final File f = new File("/home/daniel/Desktop/result.xml");
            NASProductGenerator.instance().writeResultToFileforRequest(fis, f);
        } catch (FileNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        } finally {
            try {
                fis.close();
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private boolean checkTemplateFilesAccesible() {
        final File komplettTemplate = new File(KOMPLETT_TEMPLATE_RES);
        final File eignetuemerTemplate = new File(EIGENTUEMER_TEMPLATE_RES);
        final File pointTemplate = new File(POINTS_TEMPLATE_RES);

        return (komplettTemplate.canRead() && eignetuemerTemplate.canRead() && pointTemplate.canRead());
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * NasProductDownloader checks at a fixed rate if the nas order is completed in the 3A order management system.
     *
     * @version  $Revision$, $Date$
     */
    private class NasProductDownloader implements Runnable {

        //~ Instance fields ----------------------------------------------------

        private String orderId;
        private String userId;
        private boolean isDxf;
        private boolean interrupted = false;

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new NasProductDownloader object.
         *
         * @param  userId     DOCUMENT ME!
         * @param  orderId    DOCUMENT ME!
         * @param  dxfFormat  DOCUMENT ME!
         */
        public NasProductDownloader(final String userId, final String orderId, final boolean dxfFormat) {
            this.orderId = orderId;
            this.userId = userId;
            this.isDxf = dxfFormat;
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public void run() {
            try {
                initAmManager();
                final int sessionId = manager.login(USER, PW);
                final Timer t = new Timer();
                t.scheduleAtFixedRate(new TimerTask() {

                        @Override
                        public void run() {
                            AMAuftragServer amServer = null;
                            if (interrupted) {
                                log.info(
                                    "interrupting the dowload of nas order "
                                            + orderId);
                                t.cancel();
                                return;
                            }
                            amServer = manager.listAuftrag(sessionId, orderId);
                            if (amServer.getWannBeendet() == null) {
                                return;
                            }
                            t.cancel();
                            logProtocol(manager.getProtocolGZip(sessionId, orderId));
                            boolean isZip = false;
                            if (!interrupted) {
                                final int resCount = manager.getResultCount(sessionId, orderId);
                                if (resCount > 1) {
                                    // unzip and save all files, then zip them
                                    final ArrayList<byte[]> resultFiles = new ArrayList<byte[]>();
                                    for (int i = 0; i < resCount; i++) {
                                        resultFiles.add(manager.getNResultGZip(sessionId, orderId, i));
                                    }
                                    saveZipFileOfUnzippedFileCollection(userId, orderId, resultFiles);
                                    isZip = true;
                                } else {
                                    unzipAndSaveFile(userId, orderId, manager.getResultGZip(sessionId, orderId));
                                }
                                if (isDxf) {
                                    try {
                                        log.error("sending nas file to dxf converter");
                                        final ActionTask at = dxfConverter.createDxfActionTask(
                                                new HashMap<String, Object>(),
                                                getNasFileForOrder(orderId, userId, isZip),
                                                isZip);
                                        log.error("task id for converter action is: " + at.getKey());
                                        if (at.getKey() == null) {
                                            log.error("There was an error creating the dxf converter action");
                                            return;
                                        }
                                        final Future<File> converterFuture = dxfConverter.getResult(at.getKey());
                                        log.error("start polling dxf converter action for result.");
                                        final File dxfFile = converterFuture.get();
                                        log.error("DXF file from converter received: " + dxfFile.toString());
                                        final File resultDxfFile = new File(determineFileName(userId, orderId, ".dxf"));
                                        log.error("Copying dxf file to : " + resultDxfFile.toString());
                                        IOUtils.copy(new FileInputStream(dxfFile), new FileOutputStream(resultDxfFile));
                                    } catch (InterruptedException ex) {
                                        log.error("DXF Converter Thread was interrupted", ex);
                                    } catch (ExecutionException ex) {
                                        log.error("Error during the execution of the dxf converter thread", ex);
                                    } catch (Exception ex) {
                                        log.error(ex.getMessage(), ex);
                                    }
                                }
                                removeFromOpenOrders(userId, orderId);
                                downloaderMap.remove(orderId);
                            } else {
                                log.info(
                                    "interrupting the download of nas order "
                                            + orderId);
                            }
                        }
                    }, REQUEST_PERIOD, REQUEST_PERIOD);
            } catch (Exception ex) {
                log.warn("Could not connect to 3A server", ex);
            }
        }

        /**
         * DOCUMENT ME!
         *
         * @param  interrupted  DOCUMENT ME!
         */
        public void setInterrupted(final boolean interrupted) {
            this.interrupted = interrupted;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    private static final class MapWrapper {

        //~ Instance fields ----------------------------------------------------

        private HashMap<String, NasProductInfo> map;

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new OpenOrderMapWrapper object.
         */
        public MapWrapper() {
        }

        /**
         * Creates a new OpenOrderMapWrapper object.
         *
         * @param  map  DOCUMENT ME!
         */
        public MapWrapper(final HashMap<String, NasProductInfo> map) {
            this.map = map;
        }

        //~ Methods ------------------------------------------------------------

        /**
         * DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         */
        public HashMap<String, NasProductInfo> getMap() {
            return map;
        }

        /**
         * DOCUMENT ME!
         *
         * @param  map  DOCUMENT ME!
         */
        public void setMap(final HashMap<String, NasProductInfo> map) {
            this.map = map;
        }
    }
}
