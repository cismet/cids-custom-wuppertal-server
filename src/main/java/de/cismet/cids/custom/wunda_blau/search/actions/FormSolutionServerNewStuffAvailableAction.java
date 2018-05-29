/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cismet.cids.custom.wunda_blau.search.actions;

import Sirius.server.middleware.impls.domainserver.DomainServerImpl;
import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.middleware.interfaces.domainserver.MetaServiceStore;
import Sirius.server.middleware.types.MetaClass;
import Sirius.server.middleware.types.MetaObject;
import Sirius.server.middleware.types.MetaObjectNode;
import Sirius.server.newuser.User;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.RandomStringUtils;

import org.openide.util.Lookup;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;

import java.net.URL;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

import java.rmi.Remote;
import java.rmi.RemoteException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;

import de.cismet.cids.custom.utils.WundaBlauServerResources;
import de.cismet.cids.custom.utils.alkis.AlkisProductDescription;
import de.cismet.cids.custom.utils.alkis.ServerAlkisProducts;
import de.cismet.cids.custom.utils.formsolutions.FormSolutionFtpClient;
import de.cismet.cids.custom.utils.formsolutions.FormSolutionsBestellung;
import de.cismet.cids.custom.utils.formsolutions.FormSolutionsMySqlHelper;
import de.cismet.cids.custom.utils.formsolutions.FormSolutionsProperties;
import de.cismet.cids.custom.wunda_blau.search.server.CidsAlkisSearchStatement;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;
import de.cismet.cids.server.actions.UserAwareServerAction;

import de.cismet.cids.utils.MetaClassCacheService;
import de.cismet.cids.utils.serverresources.ServerResourcesLoader;

import de.cismet.commons.security.AccessHandler;
import de.cismet.commons.security.handler.SimpleHttpAccessHandler;

import de.cismet.connectioncontext.ConnectionContext;
import de.cismet.connectioncontext.ConnectionContextStore;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class FormSolutionServerNewStuffAvailableAction implements UserAwareServerAction,
    MetaServiceStore,
    ConnectionContextStore {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            FormSolutionServerNewStuffAvailableAction.class);

    public static final String TASK_NAME = "formSolutionServerNewStuffAvailable";

    private static final String TEST_CISMET00_PREFIX = "TEST_CISMET00-";
    private static final String GUTSCHEIN_ADDITIONAL_TEXT = "TESTAUSZUG - nur zur Demonstration (%s)";

    private static final Map<String, MetaClass> METACLASS_CACHE = new HashMap();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static final int STATUS_FETCH = 70;
    public static final int STATUS_PARSE = 60;
    public static final int STATUS_GETFLURSTUECK = 55;
    public static final int STATUS_SAVE = 50;
    public static final int STATUS_CLOSE = 40;
    public static final int STATUS_CREATEURL = 30;
    public static final int STATUS_DOWNLOAD = 20;
    public static final int STATUS_BILLING = 15;
    public static final int STATUS_PENDING = 10;
    public static final int STATUS_DONE = 0;

    public static final int GUTSCHEIN_YES = 1;
    public static final int GUTSCHEIN_NO = 2;

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum PARAMETER_TYPE {

        //~ Enum constants -----------------------------------------------------

        STEP_TO_EXECUTE, SINGLE_STEP, METAOBJECTNODES
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum ProductType {

        //~ Enum constants -----------------------------------------------------

        SGK, ABK
    }

    //~ Instance fields --------------------------------------------------------

    private final SimpleHttpAccessHandler HTTP_HANDLER = new SimpleHttpAccessHandler();

    private final UsernamePasswordCredentials creds;

    private User user;
    private MetaService metaService;
    private final String testCismet00Xml;
    private final Set<String> ignoreTransids = new HashSet<>();
    private final ProductType testCismet00Type;
    private final FileWriter specialLogWriter;

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new FormSolutionServerNewStuffAvailableAction object.
     */
    public FormSolutionServerNewStuffAvailableAction() {
        this(false);
    }

    /**
     * Creates a new FormSolutionServerNewStuffAvailableAction object.
     *
     * @param  fromStartupHook  DOCUMENT ME!
     */
    public FormSolutionServerNewStuffAvailableAction(final boolean fromStartupHook) {
        UsernamePasswordCredentials creds = null;
        ProductType testCismet00Type = null;
        String testCismet00Xml = null;
        FileWriter specialLogWriter = null;

        if ((DomainServerImpl.getServerProperties() != null)
                    && "WUNDA_BLAU".equals(DomainServerImpl.getServerProperties().getServerName())) {
            try {
                creds = new UsernamePasswordCredentials(FormSolutionsProperties.getInstance().getUser(),
                        FormSolutionsProperties.getInstance().getPassword());
            } catch (final Exception ex) {
                LOG.error(
                    "UsernamePasswordCredentials couldn't be created. FormSolutionServerNewStuffAvailableAction will not work at all !",
                    ex);
            }

            try {
                if (fromStartupHook) {
                    testCismet00Type = parseProductType(FormSolutionsProperties.getInstance().getTestCismet00());
                }
            } catch (final Exception ex) {
                LOG.error("could not read FormSolutionsConstants.TEST_CISMET00. TEST_CISMET00 stays disabled", ex);
            }

            if (testCismet00Type != null) {
                try {
                    testCismet00Xml = ServerResourcesLoader.getInstance()
                                .loadText(WundaBlauServerResources.FS_TEST_XML.getValue());
                } catch (final Exception ex) {
                    LOG.error("could not load " + WundaBlauServerResources.FS_TEST_XML.getValue(), ex);
                }
            }

            try {
                final String ignoreFileContent = ServerResourcesLoader.getInstance()
                            .loadText(WundaBlauServerResources.FS_IGNORE_TRANSID_TXT.getValue());
                final String[] lines = ignoreFileContent.split("\n");
                for (final String line : lines) {
                    if (!line.trim().isEmpty()) {
                        ignoreTransids.add(line.trim());
                    }
                }
            } catch (final Exception ex) {
                LOG.error("could not load " + WundaBlauServerResources.FS_IGNORE_TRANSID_TXT.getValue(), ex);
            }

            final String specialLogAbsPath = FormSolutionsProperties.getInstance().getSpecialLogAbsPath();
            try {
                if ((specialLogAbsPath != null) && !specialLogAbsPath.isEmpty()) {
                    final File specialLogFile = new File(specialLogAbsPath);
                    if (!specialLogFile.exists() || (specialLogFile.isFile() && specialLogFile.canWrite())) {
                        specialLogWriter = new FileWriter(specialLogFile, true);
                    }
                }
            } catch (final IOException ex) {
                LOG.error("special log file writer could not be created", ex);
            }
        }
        this.specialLogWriter = specialLogWriter;
        this.testCismet00Type = testCismet00Type;
        this.testCismet00Xml = testCismet00Xml;
        this.creds = creds;
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public void initWithConnectionContext(final ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private SimpleHttpAccessHandler getHttpAccessHandler() {
        return HTTP_HANDLER;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private FormSolutionsMySqlHelper getMySqlHelper() {
        return FormSolutionsMySqlHelper.getInstance();
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private ObjectMapper getObjectMapper() {
        return MAPPER;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   table_name         DOCUMENT ME!
     * @param   connectionContext  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static MetaClass getMetaClass(final String table_name, final ConnectionContext connectionContext) {
        if (!METACLASS_CACHE.containsKey(table_name)) {
            MetaClass mc = null;
            try {
                mc = CidsBean.getMetaClassFromTableName("WUNDA_BLAU", table_name, connectionContext);
            } catch (final Exception ex) {
                LOG.error("could not get metaclass of " + table_name, ex);
            }
            METACLASS_CACHE.put(table_name, mc);
        }
        return METACLASS_CACHE.get(table_name);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   productType  DOCUMENT ME!
     * @param   typeMap      DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private Collection<String> getOpenExtendedTransids(final ProductType productType,
            final Map<String, ProductType> typeMap) throws Exception {
        logSpecial("fetching open transids from FS");

        final Collection<String> transIds = new ArrayList<>();
        try {
            final StringBuilder stringBuilder = new StringBuilder();
            final URL auftragsListeUrl;
            switch (productType) {
                case SGK: {
                    auftragsListeUrl = new URL(FormSolutionsProperties.getInstance().getUrlAuftragslisteSgkFs());
                }
                break;
                case ABK: {
                    auftragsListeUrl = new URL(FormSolutionsProperties.getInstance().getUrlAuftragslisteAbkFs());
                }
                break;
                default: {
                    throw new Exception("unknown product type");
                }
            }

            final InputStream inputStream = getHttpAccessHandler().doRequest(
                    auftragsListeUrl,
                    new StringReader(""),
                    AccessHandler.ACCESS_METHODS.GET_REQUEST,
                    null,
                    creds);

            final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
            inputStream.close();

            logSpecial("open transids fetched: " + stringBuilder.toString());

            final Map<String, Object> map = getObjectMapper().readValue("{ \"list\" : " + stringBuilder.toString()
                            + "}",
                    new TypeReference<HashMap<String, Object>>() {
                    });
            for (final String transId : (Collection<String>)map.get("list")) {
                transIds.add(transId);
                typeMap.put(transId, productType);
            }
        } catch (final Exception ex) {
            LOG.error("error while retrieving open transids", ex);
        }

        return transIds;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  transid         DOCUMENT ME!
     * @param  status          DOCUMENT ME!
     * @param  bestellungBean  DOCUMENT ME!
     * @param  message         DOCUMENT ME!
     * @param  exception       DOCUMENT ME!
     */
    private void setErrorStatus(final String transid,
            final int status,
            final CidsBean bestellungBean,
            final String message,
            final Exception exception) {
        setErrorStatus(transid, status, bestellungBean, message, exception, true);
    }

    /**
     * DOCUMENT ME!
     *
     * @param  transid         DOCUMENT ME!
     * @param  status          DOCUMENT ME!
     * @param  bestellungBean  DOCUMENT ME!
     * @param  message         DOCUMENT ME!
     * @param  exception       DOCUMENT ME!
     * @param  persist         DOCUMENT ME!
     */
    private void setErrorStatus(final String transid,
            final int status,
            final CidsBean bestellungBean,
            final String message,
            final Exception exception,
            final boolean persist) {
        LOG.error(message, exception);
        if (bestellungBean != null) {
            try {
                bestellungBean.setProperty("erledigt", false);
                bestellungBean.setProperty("fehler", message);
                bestellungBean.setProperty("fehler_ts", new Timestamp(new Date().getTime()));
                bestellungBean.setProperty("exception", getObjectMapper().writeValueAsString(exception));
                if (persist) {
                    getMetaService().updateMetaObject(
                        user,
                        bestellungBean.getMetaObject(),
                        getConnectionContext());
                }
            } catch (final Exception ex) {
                LOG.error("Fehler beim Persistieren der Bean", ex);
            }
        }
        try {
            if (!FormSolutionsProperties.getInstance().isMysqlDisabled()) {
                getMySqlHelper().updateStatus(transid, -status);
            }
            doStatusChangedRequest(transid);
        } catch (final Exception ex2) {
            LOG.error("Fehler beim Aktualisieren des MySQL-Datensatzes", ex2);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   auftrag  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private String getAuftrag(final String auftrag) throws Exception {
        logSpecial("getting auftrag from FS for: " + auftrag);
        if ((auftrag != null) && auftrag.startsWith(TEST_CISMET00_PREFIX)) {
            return (testCismet00Xml != null) ? testCismet00Xml.replace("${TRANSID}", auftrag) : null;
        } else {
            final InputStream inputStream = getHttpAccessHandler().doRequest(new URL(
                        String.format(FormSolutionsProperties.getInstance().getUrlAuftragFs(), auftrag)),
                    new StringReader(""),
                    AccessHandler.ACCESS_METHODS.GET_REQUEST,
                    null,
                    creds);
            final Map<String, Object> map = getObjectMapper().readValue(
                    inputStream,
                    new TypeReference<HashMap<String, Object>>() {
                    });
            inputStream.close();

            final String xml = new String(DatatypeConverter.parseBase64Binary((String)map.get("xml")));

            final Charset utf8charset = Charset.forName("UTF-8");
            final Charset iso885915charset = Charset.forName("ISO-8859-15");

            final ByteBuffer inputBuffer = ByteBuffer.wrap(xml.getBytes());

            // decode UTF-8
            final CharBuffer data = utf8charset.decode(inputBuffer);

            // encode ISO-8559-15
            final ByteBuffer outputBuffer = iso885915charset.encode(data);
            final byte[] outputData = outputBuffer.array();

            String convertedXml;
            try {
                convertedXml = new String(new String(outputData, "ISO-8859-15").getBytes(), "UTF-8");
            } catch (final UnsupportedEncodingException ex) {
                LOG.warn("could not convert to LATIN9", ex);
                convertedXml = xml;
            }

            logSpecial("auftrag returned from FS: " + convertedXml);

            return convertedXml;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   transid  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private void closeTransid(final String transid) throws Exception {
        logSpecial("closing transaction for: " + transid);

        final boolean noClose = (transid == null) || transid.startsWith(TEST_CISMET00_PREFIX)
                    || DomainServerImpl.getServerInstance()
                    .hasConfigAttr(getUser(), "custom.formsolutions.noclose", getConnectionContext());
        if (noClose) {
            return;
        }
        getHttpAccessHandler().doRequest(new URL(
                String.format(FormSolutionsProperties.getInstance().getUrlAuftragDeleteFs(), transid)),
            new StringReader(""),
            AccessHandler.ACCESS_METHODS.POST_REQUEST,
            null,
            creds);
    }

    /**
     * DOCUMENT ME!
     *
     * @param  transid  DOCUMENT ME!
     */
    private void doStatusChangedRequest(final String transid) {
        try {
            logSpecial("doing status changed request for: " + transid);

            getHttpAccessHandler().doRequest(new URL(
                    String.format(FormSolutionsProperties.getInstance().getUrlStatusUpdate(), transid)),
                new StringReader(""),
                AccessHandler.ACCESS_METHODS.GET_REQUEST);
        } catch (final Exception ex) {
            LOG.warn("STATUS_UPDATE_URL could not be requested", ex);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   auftragXmlInputStream  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  JAXBException  DOCUMENT ME!
     */
    private FormSolutionsBestellung createFormSolutionsBestellung(final InputStream auftragXmlInputStream)
            throws JAXBException {
        final JAXBContext jaxbContext = JAXBContext.newInstance(FormSolutionsBestellung.class);
        final Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        jaxbUnmarshaller.setEventHandler(
            new ValidationEventHandler() {

                @Override
                public boolean handleEvent(final ValidationEvent event) {
                    LOG.warn(event.getMessage(), event.getLinkedException());
                    return true;
                }
            });

        final FormSolutionsBestellung formSolutionsBestellung = (FormSolutionsBestellung)jaxbUnmarshaller.unmarshal(
                auftragXmlInputStream);
        return formSolutionsBestellung;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   formSolutionsBestellung  DOCUMENT ME!
     * @param   type                     DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static String extractProdukt(final FormSolutionsBestellung formSolutionsBestellung,
            final ProductType type) {
        if (type == null) {
            return null;
        }

        final String farbauspraegung = formSolutionsBestellung.getFarbauspraegung();
        final boolean farbig;
        if ("farbig".equals(farbauspraegung)) {
            farbig = true;
        } else if ("Graustufen".equals(farbauspraegung)) {
            farbig = false;
        } else {
            return null;
        }

        final String massstab = formSolutionsBestellung.getMassstab();

        final StringBuffer produktSB;
        switch (type) {
            case SGK: {
                produktSB = new StringBuffer("Stadtgrundkarte mit kom. Erg.").append(farbig ? " (farbig)" : " (sw)");
            }
            break;
            case ABK: {
                produktSB = new StringBuffer("Amtliche Basiskarte").append(farbig ? " (farbig)" : " (sw)");
            }
            break;
            default: {
                return null;
            }
        }
        produktSB.append(", ").append(extractFormat(formSolutionsBestellung)).append(" ").append(massstab);
        return produktSB.toString();
    }

    /**
     * DOCUMENT ME!
     *
     * @param   formSolutionsBestellung  DOCUMENT ME!
     * @param   type                     DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static String extractProduktKey(final FormSolutionsBestellung formSolutionsBestellung,
            final ProductType type) {
        if (type == null) {
            return null;
        }

        final String farbauspraegung = formSolutionsBestellung.getFarbauspraegung();
        final boolean farbig;
        if ("farbig".equals(farbauspraegung)) {
            farbig = true;
        } else if ("Graustufen".equals(farbauspraegung)) {
            farbig = false;
        } else {
            return null;
        }

        switch (type) {
            case SGK: {
                return farbig ? "LK.NRW.K.BF" : "LK.NRW.K.BSW";
            }
            case ABK: {
                return farbig ? "LK.GDBNRW.A.ABKF" : "LK.GDBNRW.A.ABKSW";
            }
            default: {
                return null;
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   formSolutionsBestellung  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static String extractFormatKey(final FormSolutionsBestellung formSolutionsBestellung) {
        final String rawDin = formSolutionsBestellung.getFormat();
        final String rawAusrichtung = formSolutionsBestellung.getAusrichtung();

        final String formatKey;
        if ((rawDin != null) && (rawAusrichtung != null)) {
            final String din = rawDin.trim().toUpperCase().split("DIN")[1].trim();
            final String ausrichtung = rawAusrichtung.trim().toLowerCase();
            formatKey = din + "-" + ausrichtung;
        } else {
            formatKey = null;
        }
        return formatKey;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   formSolutionsBestellung  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static String extractFormat(final FormSolutionsBestellung formSolutionsBestellung) {
        final String rawDin = formSolutionsBestellung.getFormat();
        final String rawAusrichtung = formSolutionsBestellung.getAusrichtung();

        final String format;
        if ((rawDin != null) && (rawAusrichtung != null)) {
            final String din = rawDin.trim().toUpperCase();
            final String ausrichtung = rawAusrichtung.trim().toLowerCase();
            if ("hoch".equals(ausrichtung)) {
                format = din + " Hochformat";
            } else {
                format = din + " Querformat";
            }
        } else {
            format = null;
        }
        return format;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   formSolutionsBestellung  farbauspraegung DOCUMENT ME!
     * @param   productType              DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  RemoteException  DOCUMENT ME!
     */
    private CidsBean getProduktBean(final FormSolutionsBestellung formSolutionsBestellung,
            final ProductType productType) throws RemoteException {
        final String produktKey = extractProduktKey(formSolutionsBestellung, productType);
        final String formatKey = extractFormatKey(formSolutionsBestellung);

        final MetaClass produktTypMc = getMetaClass("fs_bestellung_produkt_typ", getConnectionContext());
        final MetaClass produktMc = getMetaClass("fs_bestellung_produkt", getConnectionContext());
        final MetaClass formatMc = getMetaClass("fs_bestellung_format", getConnectionContext());
        final String produktQuery = "SELECT DISTINCT " + produktMc.getID() + ", "
                    + produktMc.getTableName() + "." + produktMc.getPrimaryKey() + " "
                    + "FROM " + produktMc.getTableName() + ", " + produktTypMc.getTableName() + ", "
                    + formatMc.getTableName() + " "
                    + "WHERE " + produktMc.getTableName() + ".fk_format = " + formatMc.getTableName() + ".id "
                    + "AND " + produktMc.getTableName() + ".fk_typ = " + produktTypMc.getTableName() + ".id "
                    + "AND " + produktTypMc.getTableName() + ".key = '" + produktKey + "' "
                    + "AND " + formatMc.getTableName() + ".key = '" + formatKey + "' "
                    + "LIMIT 1;";
        final MetaObject[] produktMos = getMetaService().getMetaObject(
                getUser(),
                produktQuery,
                getConnectionContext());
        produktMos[0].setAllClasses(((MetaClassCacheService)Lookup.getDefault().lookup(MetaClassCacheService.class))
                    .getAllClasses(produktMos[0].getDomain(), getConnectionContext()));
        return produktMos[0].getBean();
    }

    /**
     * DOCUMENT ME!
     *
     * @param   formSolutionsBestellung  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static String extractLandparcelcode(final FormSolutionsBestellung formSolutionsBestellung) {
        final Set<String> fskz = new LinkedHashSet<String>();
        final String flurstueckskennzeichen = trimedNotEmpty(formSolutionsBestellung.getFlurstueckskennzeichen());
        if (flurstueckskennzeichen != null) {
            fskz.add(flurstueckskennzeichen);
        }
        if ("Anschrift".equals(formSolutionsBestellung.getAuswahlUeber())) {
            final String flurstueckskennzeichen1 = trimedNotEmpty(formSolutionsBestellung.getFlurstueckskennzeichen1());
            if (flurstueckskennzeichen1 != null) {
                for (final String tmp : flurstueckskennzeichen1.split(",")) {
                    fskz.add(tmp);
                }
            }
        }

        if (fskz.isEmpty()) {
            return null;
        } else {
            final Iterator<String> it = fskz.iterator();
            final StringBuffer sb = new StringBuffer(it.next());
            while (it.hasNext()) {
                sb.append(",").append(it.next());
            }
            return sb.toString();
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   formSolutionsBestellung  DOCUMENT ME!
     * @param   productType              DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private CidsBean createBestellungBean(final FormSolutionsBestellung formSolutionsBestellung,
            final ProductType productType) throws Exception {
        final MetaClass bestellungMc = getMetaClass("fs_bestellung", getConnectionContext());
        final MetaClass adresseMc = getMetaClass("fs_bestellung_adresse", getConnectionContext());

        final CidsBean bestellungBean = bestellungMc.getEmptyInstance(getConnectionContext()).getBean();
        final CidsBean adresseRechnungBean = adresseMc.getEmptyInstance(getConnectionContext()).getBean();
        final CidsBean adresseVersandBean;

        final String transid = formSolutionsBestellung.getTransId();
        final Integer massstab = (formSolutionsBestellung.getMassstab() != null)
            ? Integer.parseInt(formSolutionsBestellung.getMassstab().split(":")[1]) : null;

        final Double gebuehr;
        {
            Double tmpGebuehr = null;
            try {
                tmpGebuehr = Double.parseDouble(formSolutionsBestellung.getBetrag());
            } catch (final Exception ex) {
                LOG.warn("Exception while parsing Gebuehr", ex);
            }
            gebuehr = tmpGebuehr;
        }
        final boolean isGutschein = ((formSolutionsBestellung.getGutschein() != null)
                        && (GUTSCHEIN_YES == Integer.parseInt(formSolutionsBestellung.getGutschein())));
        final String gutscheinCode = isGutschein ? formSolutionsBestellung.getGutscheinCode() : null;

        final boolean isTest = ((gutscheinCode != null) && gutscheinCode.startsWith("T"))
                    || ((transid != null) && transid.startsWith(TEST_CISMET00_PREFIX));

        final String landparcelcode = extractLandparcelcode(formSolutionsBestellung);

        final boolean isLieferEqualsRechnungAnschrift = "ja".equalsIgnoreCase(
                formSolutionsBestellung.getRechnungsanschriftLieferanschrift());

        final Integer plz1;
        {
            Integer tmpPlz1 = null;
            if (!isLieferEqualsRechnungAnschrift) {
                try {
                    tmpPlz1 = Integer.parseInt(formSolutionsBestellung.getAsPlz1());
                } catch (final Exception ex) {
                    LOG.warn("Exception while parsing PLZ1", ex);
                }
            }
            plz1 = tmpPlz1;
        }

        // setting Bean properties

        final CidsBean produktBean = getProduktBean(formSolutionsBestellung, productType);

        adresseRechnungBean.setProperty("firma", trimedNotEmpty(formSolutionsBestellung.getFirma()));
        adresseRechnungBean.setProperty("name", trimedNotEmpty(formSolutionsBestellung.getAsName()));
        adresseRechnungBean.setProperty("vorname", trimedNotEmpty(formSolutionsBestellung.getAsVorname()));
        adresseRechnungBean.setProperty("strasse", trimedNotEmpty(formSolutionsBestellung.getAsStrasse()));
        adresseRechnungBean.setProperty("hausnummer", trimedNotEmpty(formSolutionsBestellung.getAsHausnummer()));
        Integer plz;
        try {
            plz = Integer.parseInt(formSolutionsBestellung.getAsPlz());
        } catch (final Exception ex) {
            LOG.warn("Exception while parsing PLZ", ex);
            plz = null;
        }
        adresseRechnungBean.setProperty("plz", plz);
        adresseRechnungBean.setProperty("ort", trimedNotEmpty(formSolutionsBestellung.getAsOrt()));
        adresseRechnungBean.setProperty("staat", trimedNotEmpty(formSolutionsBestellung.getStaat()));
        adresseRechnungBean.setProperty("alternativ", trimedNotEmpty(formSolutionsBestellung.getAltAdresse()));

        if (isLieferEqualsRechnungAnschrift) {
            adresseVersandBean = adresseRechnungBean;
        } else {
            adresseVersandBean = adresseMc.getEmptyInstance(getConnectionContext()).getBean();
            adresseVersandBean.setProperty("firma", trimedNotEmpty(formSolutionsBestellung.getFirma1()));
            adresseVersandBean.setProperty("name", trimedNotEmpty(formSolutionsBestellung.getAsName1()));
            adresseVersandBean.setProperty("vorname", trimedNotEmpty(formSolutionsBestellung.getAsVorname1()));
            adresseVersandBean.setProperty("strasse", trimedNotEmpty(formSolutionsBestellung.getAsStrasse1()));
            adresseVersandBean.setProperty("hausnummer", trimedNotEmpty(formSolutionsBestellung.getAsHausnummer1()));

            adresseVersandBean.setProperty("plz", plz1);
            adresseVersandBean.setProperty("ort", trimedNotEmpty(formSolutionsBestellung.getAsOrt1()));
            adresseVersandBean.setProperty("staat", trimedNotEmpty(formSolutionsBestellung.getStaat1()));
            adresseVersandBean.setProperty("alternativ", trimedNotEmpty(formSolutionsBestellung.getAltAdresse1()));
        }

        bestellungBean.setProperty("postweg", "Kartenausdruck".equals(formSolutionsBestellung.getBezugsweg()));
        bestellungBean.setProperty("transid", transid);
        bestellungBean.setProperty("landparcelcode", landparcelcode);
        bestellungBean.setProperty("fk_produkt", produktBean);
        bestellungBean.setProperty("massstab", massstab);
        bestellungBean.setProperty("fk_adresse_versand", adresseVersandBean);
        bestellungBean.setProperty("fk_adresse_rechnung", adresseRechnungBean);
        bestellungBean.setProperty("email", trimedNotEmpty(formSolutionsBestellung.getEMailadresse()));
        bestellungBean.setProperty("erledigt", false);
        bestellungBean.setProperty("eingang_ts", new Timestamp(new Date().getTime()));
        bestellungBean.setProperty("gebuehr", gebuehr);
        bestellungBean.setProperty("gutschein_code", gutscheinCode);
        bestellungBean.setProperty("test", isTest);

        return bestellungBean;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   string  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static String trimedNotEmpty(final String string) {
        if (string == null) {
            return null;
        } else {
            final String trimed = string.trim();
            if (trimed.isEmpty()) {
                return null;
            } else {
                return trimed;
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param  args  DOCUMENT ME!
     */
    public static void main(final String[] args) {
        System.out.println("’  " + trimedNotEmpty("’"));
    }
    /**
     * DOCUMENT ME!
     *
     * @param   produktKey  DOCUMENT ME!
     * @param   dinFormat   DOCUMENT ME!
     * @param   massstab    DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private AlkisProductDescription getAlkisProductDescription(final String produktKey,
            final String dinFormat,
            final Integer massstab) {
        final String scale = Integer.toString(massstab);
        AlkisProductDescription selectedProduct = null;
        for (final AlkisProductDescription product : ServerAlkisProducts.getInstance().getAlkisMapProducts()) {
            if (product.getCode().startsWith(produktKey) && scale.equals(product.getMassstab())
                        && dinFormat.equals(product.getDinFormat())) {
                selectedProduct = product;
                break;
            }
        }
        return selectedProduct;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   flurstueckKennzeichen  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  RemoteException  DOCUMENT ME!
     */
    private CidsBean getFlurstueck(final String flurstueckKennzeichen) throws RemoteException {
        final CidsAlkisSearchStatement search = new CidsAlkisSearchStatement(
                CidsAlkisSearchStatement.Resulttyp.FLURSTUECK,
                CidsAlkisSearchStatement.SucheUeber.FLURSTUECKSNUMMER,
                flurstueckKennzeichen,
                null);

        final Map localServers = new HashMap<String, Remote>();
        localServers.put("WUNDA_BLAU", getMetaService());
        search.setActiveLocalServers(localServers);
        search.setUser(getUser());
        final Collection<MetaObjectNode> mons = search.performServerSearch();
        if ((mons != null) && !mons.isEmpty()) {
            final MetaObjectNode mon = new ArrayList<MetaObjectNode>(mons).get(0);
            final CidsBean flurstueck = getMetaService().getMetaObject(
                        getUser(),
                        mon.getObjectId(),
                        mon.getClassId(),
                        getConnectionContext())
                        .getBean();
            return flurstueck;
        } else {
            return null;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   bestellungBean  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private URL createProductUrl(final CidsBean bestellungBean) throws Exception {
        final String code = (String)bestellungBean.getProperty("fk_produkt.fk_typ.key");
        final String dinFormat = (String)bestellungBean.getProperty("fk_produkt.fk_format.format");
        final Integer scale = (Integer)bestellungBean.getProperty("massstab");

        final AlkisProductDescription productDesc = getAlkisProductDescription(code, dinFormat, scale);
        final String flurstueckKennzeichen = ((String)bestellungBean.getProperty("landparcelcode")).split(",")[0];

        final String transid = (String)bestellungBean.getProperty("transid");

        final Geometry geom = (Geometry)bestellungBean.getProperty("geometrie.geo_field");
        final Point center = geom.getEnvelope().getCentroid();

        final String gutscheincodeAdditionalText = (bestellungBean.getProperty("gutschein_code") != null)
            ? String.format(GUTSCHEIN_ADDITIONAL_TEXT, bestellungBean.getProperty("gutschein_code")) : null;

        final URL url = ServerAlkisProducts.productKarteUrl(
                        flurstueckKennzeichen,
                        productDesc.getCode(),
                        0,
                        (int)center.getX(),
                        (int)center.getY(),
                        productDesc.getMassstabMin(),
                        productDesc.getMassstabMax(),
                        gutscheincodeAdditionalText,
                        transid,
                        false,
                        null);

        return url;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   productUrl       DOCUMENT ME!
     * @param   destinationPath  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private void downloadProdukt(final URL productUrl, final String destinationPath) throws Exception {
        InputStream in = null;
        try {
            in = getHttpAccessHandler().doRequest(
                    productUrl,
                    new StringReader(""),
                    AccessHandler.ACCESS_METHODS.GET_REQUEST,
                    null,
                    creds);

            FormSolutionFtpClient.getInstance()
                    .upload(in, FormSolutionsProperties.getInstance().getProduktBasepath() + destinationPath);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   transids  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private Map<String, String> extractXmlParts(final Collection<String> transids) {
        logSpecial("extracting xml parts for num of objects: " + transids.size());

        final Map<String, String> fsXmlMap = new HashMap<String, String>(transids.size());

        for (final String transid : transids) {
            try {
                final String auftragXml = getAuftrag(transid);
                fsXmlMap.put(transid, auftragXml);
                logSpecial("updating mysql entry for: " + transid);
                if (!FormSolutionsProperties.getInstance().isMysqlDisabled()) {
                    getMySqlHelper().updateStatus(transid, STATUS_FETCH);
                }
                doStatusChangedRequest(transid);
            } catch (final Exception ex) {
                setErrorStatus(transid, STATUS_FETCH, null, "Fehler beim Abholen FormSolution", ex);
            }
        }
        return fsXmlMap;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   fsXmlMap  DOCUMENT ME!
     * @param   typeMap   DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private Map<String, FormSolutionsBestellung> createBestellungMap(final Map<String, String> fsXmlMap,
            final Map<String, ProductType> typeMap) {
        final Collection<String> transids = new ArrayList<String>(fsXmlMap.keySet());

        logSpecial("creating simple bestellung bean for num of objects: " + transids.size());

        final Map<String, FormSolutionsBestellung> fsBestellungMap = new HashMap<String, FormSolutionsBestellung>(
                transids.size());
        for (final String transid : transids) {
            try {
                final String auftragXml = fsXmlMap.get(transid);
                final InputStream inputStream = IOUtils.toInputStream(auftragXml, "UTF-8");

                logSpecial("creating simple bestellung bean for: " + transid);

                final FormSolutionsBestellung formSolutionsBestellung = createFormSolutionsBestellung(inputStream);
                fsBestellungMap.put(transid, formSolutionsBestellung);

                logSpecial("simple bestellung bean created for: " + transids.size());

                final boolean downloadOnly = !"Kartenausdruck".equals(formSolutionsBestellung.getBezugsweg());
                final String email = downloadOnly ? trimedNotEmpty(formSolutionsBestellung.getEMailadresse())
                                                  : trimedNotEmpty(formSolutionsBestellung.getEMailadresse()); // 1

                logSpecial("updating mysql email entry for: " + transid);

                if (!FormSolutionsProperties.getInstance().isMysqlDisabled()) {
                    getMySqlHelper().updateEmail(
                        transid,
                        STATUS_PARSE,
                        extractLandparcelcode(formSolutionsBestellung),
                        extractProdukt(formSolutionsBestellung, typeMap.get(transid)),
                        downloadOnly,
                        email);
                }
                doStatusChangedRequest(transid);
            } catch (final Exception ex) {
                setErrorStatus(transid, STATUS_PARSE, null, "Fehler beim Parsen FormSolution", ex);
            }
        }

        return fsBestellungMap;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   fsXmlMap            transids DOCUMENT ME!
     * @param   fsBestellungMap     DOCUMENT ME!
     * @param   typeMap             DOCUMENT ME!
     * @param   insertExceptionMap  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private Map<String, CidsBean> createCidsEntries(final Map<String, String> fsXmlMap,
            final Map<String, FormSolutionsBestellung> fsBestellungMap,
            final Map<String, ProductType> typeMap,
            final Map<String, Exception> insertExceptionMap) {
        // nur die transids bearbeiten, bei denen das Parsen auch geklappt hat
        final Collection<String> transids = new ArrayList<>(fsBestellungMap.keySet());

        logSpecial("creating cids entries for num of objects: " + transids.size());

        final Map<String, CidsBean> fsBeanMap = new HashMap<>(transids.size());
        for (final String transid : transids) {
            logSpecial("creating cids entry for: " + transid);

            final String auftragXml = fsXmlMap.get(transid);
            final FormSolutionsBestellung formSolutionBestellung = fsBestellungMap.get(transid);

            boolean duplicate = false;
            try {
                final MetaClass bestellungMc = getMetaClass("fs_bestellung", getConnectionContext());
                final String searchQuery = "SELECT DISTINCT " + bestellungMc.getID() + ", "
                            + bestellungMc.getTableName() + "." + bestellungMc.getPrimaryKey() + " "
                            + "FROM " + bestellungMc.getTableName() + " "
                            + "WHERE transid LIKE '" + transid + "';";
                final MetaObject[] mos = getMetaService().getMetaObject(getUser(), searchQuery);
                if ((mos != null) && (mos.length > 0)) {
                    duplicate = true;
                }
            } catch (final Exception ex) {
                final String message = "error while search for duplicates for " + transid;
                LOG.error(message, ex);
                logSpecial(message);
            }

            try {
                final Exception insertException = insertExceptionMap.get(transid);
                final ProductType type = typeMap.get(transid);

                final CidsBean bestellungBean = createBestellungBean(formSolutionBestellung, type);
                bestellungBean.setProperty("form_xml_orig", auftragXml);
                bestellungBean.setProperty("duplicate", duplicate);
                if (insertException != null) {
                    bestellungBean.setProperty("fehler", "Fehler beim Erzeugen des MySQL-Datensatzes");
                    bestellungBean.setProperty("exception", getObjectMapper().writeValueAsString(insertException));
                    bestellungBean.setProperty("fehler_ts", new Timestamp(new Date().getTime()));
                }

                try {
                    final String[] landparcelcodes = ((String)bestellungBean.getProperty("landparcelcode")).split(",");

                    final MetaClass geomMc = getMetaClass("geom", getConnectionContext());
                    final CidsBean geomBean = geomMc.getEmptyInstance(getConnectionContext()).getBean();
                    Geometry geom = null;
                    for (final String landparcelcode : landparcelcodes) {
                        final CidsBean flurstueck = getFlurstueck(landparcelcode);
                        if (flurstueck == null) {
                            throw new Exception("ALKIS Flurstück wurde nicht gefunden (" + landparcelcode + ")");
                        }
                        final Geometry flurgeom = (Geometry)flurstueck.getProperty("geometrie.geo_field");
                        if (geom == null) {
                            geom = flurgeom;
                        } else {
                            geom = flurgeom.union(geom);
                        }
                    }
                    if (geom != null) {
                        geomBean.setProperty("geo_field", geom);
                        bestellungBean.setProperty("geometrie", geomBean);
                    }
                } catch (final Exception ex) {
                    setErrorStatus(
                        transid,
                        STATUS_GETFLURSTUECK,
                        bestellungBean,
                        "Fehler beim Laden des Flurstücks",
                        getObjectMapper().readValue((String)bestellungBean.getProperty("exception"), Exception.class),
                        false);
                }

                logSpecial("persisting cids entry for: " + transid);

                final MetaObject persistedMo = getMetaService().insertMetaObject(
                        user,
                        bestellungBean.getMetaObject(),
                        getConnectionContext());

                final CidsBean persistedBestellungBean = persistedMo.getBean();
                fsBeanMap.put(transid, persistedBestellungBean);

                if (!FormSolutionsProperties.getInstance().isMysqlDisabled()) {
                    logSpecial("updating mysql entry for: " + transid);
                    getMySqlHelper().updateStatus(transid, STATUS_SAVE);
                }
                doStatusChangedRequest(transid);
            } catch (final Exception ex) {
                setErrorStatus(transid, STATUS_SAVE, null, "Fehler beim Erstellen des Bestellungs-Objektes", ex);
            }
        }

        return fsBeanMap;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   string  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static ProductType parseProductType(final String string) {
        if (ProductType.SGK.toString().equals(string)) {
            return ProductType.SGK;
        } else if (ProductType.ABK.toString().equals(string)) {
            return ProductType.ABK;
        } else {
            return null;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   transids  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public final Map<String, Exception> createMySqlEntries(final Collection<String> transids) {
        logSpecial("creating mySQL entries for num of objects: " + transids.size());

        final Map<String, Exception> insertExceptionMap = new HashMap<>(transids.size());

        if (!FormSolutionsProperties.getInstance().isMysqlDisabled()) {
            for (final String transid : transids) {
                boolean mysqlEntryAlreadyExists = false;
                ResultSet resultSet = null;
                try {
                    resultSet = getMySqlHelper().select(transid);
                    mysqlEntryAlreadyExists = (resultSet != null) && resultSet.next();
                } catch (final SQLException ex) {
                    LOG.error("check nach bereits vorhandenen transids fehlgeschlagen.", ex);
                } finally {
                    if (resultSet != null) {
                        try {
                            resultSet.close();
                        } catch (SQLException ex) {
                        }
                    }
                }

                logSpecial("updating or inserting mySQL entry for: " + transid);
                try {
                    if (mysqlEntryAlreadyExists) {
                        getMySqlHelper().updateStatus(transid, 100);
                    } else {
                        getMySqlHelper().insertMySql(transid, 100);
                    }
                    doStatusChangedRequest(transid);
                } catch (final Exception ex) {
                    LOG.error("Fehler beim Erzeugen/Aktualisieren des MySQL-Datensatzes.", ex);
                    insertExceptionMap.put(transid, ex);
                }
            }
        }

        return insertExceptionMap;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  fsBeanMap  DOCUMENT ME!
     */
    private void closeTransactions(final Map<String, CidsBean> fsBeanMap) {
        final Collection<String> transids = new ArrayList<>(fsBeanMap.keySet());
        logSpecial("closing transactions for num of objects: " + transids.size());

        for (final String transid : transids) {
            final CidsBean bestellungBean = fsBeanMap.get(transid);
            if ((bestellungBean != null)) {
                try {
                    closeTransid(transid);

                    logSpecial("updating mysql entry for: " + transid);
                    if (!FormSolutionsProperties.getInstance().isMysqlDisabled()) {
                        getMySqlHelper().updateStatus(transid, STATUS_CLOSE);
                    }
                    doStatusChangedRequest(transid);
                } catch (final Exception ex) {
                    setErrorStatus(transid, STATUS_CLOSE, bestellungBean, "Fehler beim Schließen der Transaktion.", ex);
                    break;
                }
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   fsBeanMap  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private Map<String, URL> createUrlMap(final Map<String, CidsBean> fsBeanMap) {
        final Collection<String> transids = new ArrayList<>(fsBeanMap.keySet());

        final Map<String, URL> fsUrlMap = new HashMap<>(transids.size());

        for (final String transid : new ArrayList<>(fsBeanMap.keySet())) {
            final CidsBean bestellungBean = fsBeanMap.get(transid);

            if ((bestellungBean != null)
                        && !Boolean.TRUE.equals(bestellungBean.getProperty("duplicate"))
                        && (bestellungBean.getProperty("fehler") == null)) {
                try {
                    final URL productUrl = createProductUrl(bestellungBean);
                    fsUrlMap.put(transid, productUrl);
                    if (!FormSolutionsProperties.getInstance().isMysqlDisabled()) {
                        getMySqlHelper().updateStatus(transid, STATUS_CREATEURL);
                    }
                    doStatusChangedRequest(transid);
                } catch (final Exception ex) {
                    setErrorStatus(
                        transid,
                        STATUS_CREATEURL,
                        bestellungBean,
                        "Fehler beim Erzeugen der Produkt-URL",
                        ex);
                }
            }
        }
        return fsUrlMap;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   string  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static String noNullAndTrimed(final String string) {
        if (string == null) {
            return "";
        } else {
            return string.trim();
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   bestellungBean  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private JasperPrint createRechnung(final CidsBean bestellungBean) throws Exception {
        final Map parameters = new HashMap();

        parameters.put("DATUM_HEUTE", new SimpleDateFormat("dd.MM.yyyy").format(new Date()));
        final String datumEingang = (bestellungBean.getProperty("eingang_ts") != null)
            ? new SimpleDateFormat("dd.MM.yyyy").format(bestellungBean.getProperty("eingang_ts")) : "";
        parameters.put("DATUM_EINGANG", noNullAndTrimed(datumEingang));
        parameters.put("FLURSTUECKSKENNZEICHEN", noNullAndTrimed((String)bestellungBean.getProperty("landparcelcode")));
        parameters.put("TRANSAKTIONSID", noNullAndTrimed((String)bestellungBean.getProperty("transid")));
        parameters.put("LIEFER_FIRMA", noNullAndTrimed((String)bestellungBean.getProperty("fk_adresse_versand.firma")));
        parameters.put(
            "LIEFER_VORNAME",
            noNullAndTrimed((String)bestellungBean.getProperty("fk_adresse_versand.vorname")));
        parameters.put("LIEFER_NAME", noNullAndTrimed((String)bestellungBean.getProperty("fk_adresse_versand.name")));

        final String lieferStrasse = noNullAndTrimed((String)bestellungBean.getProperty("fk_adresse_versand.strasse"));
        final String lieferHausnummer = noNullAndTrimed((String)bestellungBean.getProperty(
                    "fk_adresse_versand.hausnummer"));
        final String lieferPlz = noNullAndTrimed((bestellungBean.getProperty("fk_adresse_versand.plz") != null)
                    ? Integer.toString((Integer)bestellungBean.getProperty("fk_adresse_versand.plz")) : null);
        final String lieferOrt = noNullAndTrimed((String)bestellungBean.getProperty("fk_adresse_versand.ort"));
        final String lieferStaat = noNullAndTrimed((String)bestellungBean.getProperty(
                    "fk_adresse_versand.staat"));
        final String lieferAlternativ = noNullAndTrimed((String)bestellungBean.getProperty(
                    "fk_adresse_versand.alternativ"));
        final String lieferAdresse = (lieferAlternativ.isEmpty())
            ? (lieferStrasse + " " + lieferHausnummer + "\n" + lieferPlz + " " + lieferOrt)
            : (lieferAlternativ + "\n" + lieferStaat);

        parameters.put("LIEFER_STRASSE", lieferStrasse);
        parameters.put("LIEFER_HAUSNUMMER", lieferHausnummer);
        parameters.put("LIEFER_PLZ", lieferPlz);
        parameters.put("LIEFER_ORT", lieferOrt);
        parameters.put("LIEFER_ALTERNATIV", lieferAlternativ);
        parameters.put("LIEFER_ADRESSE", lieferAdresse);

        parameters.put(
            "RECHNUNG_FIRMA",
            noNullAndTrimed((String)bestellungBean.getProperty("fk_adresse_rechnung.firma")));
        parameters.put(
            "RECHNUNG_VORNAME",
            noNullAndTrimed((String)bestellungBean.getProperty("fk_adresse_rechnung.vorname")));
        parameters.put(
            "RECHNUNG_NAME",
            noNullAndTrimed((String)bestellungBean.getProperty("fk_adresse_rechnung.name")));

        final String rechnungStrasse = noNullAndTrimed((String)bestellungBean.getProperty(
                    "fk_adresse_rechnung.strasse"));
        final String rechnungHausnummer = noNullAndTrimed((String)bestellungBean.getProperty(
                    "fk_adresse_rechnung.hausnummer"));
        final String rechnungPlz = noNullAndTrimed((bestellungBean.getProperty("fk_adresse_rechnung.plz") != null)
                    ? Integer.toString((Integer)bestellungBean.getProperty("fk_adresse_rechnung.plz")) : null);
        final String rechnungOrt = noNullAndTrimed((String)bestellungBean.getProperty("fk_adresse_rechnung.ort"));
        final String rechnungStaat = noNullAndTrimed((String)bestellungBean.getProperty(
                    "fk_adresse_rechnung.staat"));
        final String rechnungAlternativ = noNullAndTrimed((String)bestellungBean.getProperty(
                    "fk_adresse_rechnung.alternativ"));
        final String rechnungAdresse = (rechnungAlternativ.isEmpty())
            ? (rechnungStrasse + " " + rechnungHausnummer + "\n" + rechnungPlz + " " + rechnungOrt)
            : (rechnungAlternativ + "\n" + rechnungStaat);
        final String gutscheinCode = (String)bestellungBean.getProperty("gutschein_code");

        parameters.put("RECHNUNG_STRASSE", rechnungStrasse);
        parameters.put("RECHNUNG_HAUSNUMMER", rechnungHausnummer);
        parameters.put("RECHNUNG_PLZ", rechnungPlz);
        parameters.put("RECHNUNG_ORT", rechnungOrt);
        parameters.put("RECHNUNG_ALTERNATIV", rechnungAlternativ);
        parameters.put("RECHNUNG_ADRESSE", rechnungAdresse);

        parameters.put(
            "RECHNUNG_FORMAT",
            noNullAndTrimed((String)bestellungBean.getProperty("fk_produkt.fk_format.format")));
        parameters.put(
            "RECHNUNG_LEISTUNG",
            noNullAndTrimed((String)bestellungBean.getProperty("fk_produkt.fk_typ.name"))
                    + "\n"
                    + bestellungBean.getProperty("transid"));

        final float gebuehr = (bestellungBean.getProperty("gebuehr") != null)
            ? ((Double)bestellungBean.getProperty("gebuehr")).floatValue() : 0f;
        parameters.put("RECHNUNG_GES_BETRAG", gebuehr);
        parameters.put("RECHNUNG_EINZELPREIS", gebuehr);
        parameters.put("RECHNUNG_GESAMMTPREIS", gebuehr);
        parameters.put(
            "RECHNUNG_BERECH_GRUNDLAGE",
            FormSolutionsProperties.getInstance().getRechnungBerechnugsgGrundlage());
        parameters.put("RECHNUNG_ANZAHL", 1);
        parameters.put("RECHNUNG_RABATT", 0.0f);
        parameters.put("RECHNUNG_UST", 0.0f);
        parameters.put("RECHNUNG_GUTSCHEINCODE", gutscheinCode);
        parameters.put("SUBREPORT_DIR", DomainServerImpl.getServerProperties().getServerResourcesBasePath() + "/");
        final JRDataSource dataSource = new JRBeanCollectionDataSource(Arrays.asList(bestellungBean));

        final JasperReport rechnungJasperReport = ServerResourcesLoader.getInstance()
                    .loadJasperReport(WundaBlauServerResources.FS_RECHNUNG_JASPER.getValue());
        final JasperPrint print = JasperFillManager.fillReport(rechnungJasperReport, parameters, dataSource);
        return print;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   fileName        DOCUMENT ME!
     * @param   bestellungBean  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private void createRechnung(final String fileName, final CidsBean bestellungBean) throws Exception {
        final JasperPrint print = createRechnung(bestellungBean);

        ByteArrayOutputStream os = null;
        ByteArrayInputStream is = null;
        try {
            os = new ByteArrayOutputStream();
            JasperExportManager.exportReportToPdfStream(print, os);
            final byte[] bytes = os.toByteArray();

            is = new ByteArrayInputStream(bytes);

            FormSolutionFtpClient.getInstance()
                    .upload(is, FormSolutionsProperties.getInstance().getProduktBasepath() + fileName);
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
            } catch (Exception e) {
            }
            try {
                if (is != null) {
                    is.close();
                }
            } catch (Exception e) {
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param  fsBeanMap  DOCUMENT ME!
     * @param  fsUrlMap   DOCUMENT ME!
     */
    private void downloadProdukte(final Map<String, CidsBean> fsBeanMap, final Map<String, URL> fsUrlMap) {
        final Collection<String> transids = new ArrayList<>(fsUrlMap.keySet());

        for (final String transid : transids) {
            final CidsBean bestellungBean = fsBeanMap.get(transid);
            if ((bestellungBean != null) && (bestellungBean.getProperty("fehler") == null)) {
                final URL productUrl = fsUrlMap.get(transid);
                try {
                    bestellungBean.setProperty("produkt_dateipfad", null);
                    bestellungBean.setProperty("produkt_dateiname_orig", null);
                    bestellungBean.setProperty("produkt_ts", null);

                    bestellungBean.setProperty("request_url", productUrl.toString());

                    final String fileName = transid + ".pdf";
                    final String fileNameRechnung = "RE_" + transid + ".pdf";

                    downloadProdukt(productUrl, fileName);

                    final String fileNameOrig = (String)bestellungBean.getProperty("fk_produkt.fk_typ.key")
                                + "."
                                + ((String)bestellungBean.getProperty("landparcelcode")).split(",")[0].replace(
                                    "/",
                                    "--")
                                + ".pdf";

                    bestellungBean.setProperty("produkt_dateipfad", fileName);
                    bestellungBean.setProperty("produkt_dateiname_orig", fileNameOrig);
                    bestellungBean.setProperty("rechnung_dateipfad", fileNameRechnung);
                    bestellungBean.setProperty(
                        "rechnung_dateiname_orig",
                        "Rechnung - Produktbestellung "
                                + transid
                                + ".pdf");
                    bestellungBean.setProperty("produkt_ts", new Timestamp(new Date().getTime()));

                    getMetaService().updateMetaObject(
                        user,
                        bestellungBean.getMetaObject(),
                        getConnectionContext());

                    createRechnung(fileNameRechnung, bestellungBean);

                    if (!FormSolutionsProperties.getInstance().isMysqlDisabled()) {
                        getMySqlHelper().updateProdukt(
                            transid,
                            STATUS_DOWNLOAD,
                            (String)bestellungBean.getProperty("produkt_dateipfad"),
                            (String)bestellungBean.getProperty("produkt_dateiname_orig"));
                    }
                    doStatusChangedRequest(transid);
                } catch (final Exception ex) {
                    setErrorStatus(transid, STATUS_DOWNLOAD, bestellungBean, "Fehler beim Erzeugen des Produktes", ex);
                }
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param  bestellungBean  DOCUMENT ME!
     * @param  transid         DOCUMENT ME!
     */
    private void doPureBilling(final CidsBean bestellungBean, final String transid) {
        try {
            if (bestellungBean.getProperty("fk_billing") == null) {
                final CidsBean billingBean = doBilling(bestellungBean, transid);
                if (billingBean != null) {
                    bestellungBean.setProperty("fk_billing", billingBean);
                    getMetaService().updateMetaObject(
                        user,
                        bestellungBean.getMetaObject(),
                        getConnectionContext());
                }
            }
        } catch (final Exception ex) {
            setErrorStatus(
                transid,
                STATUS_BILLING,
                bestellungBean,
                "Fehler beim Erzeugen des Billings",
                ex);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param  fsBeanMap  DOCUMENT ME!
     */
    private void finalizeBeans(final Map<String, CidsBean> fsBeanMap) {
        final Collection<String> transids = new ArrayList<>(fsBeanMap.keySet());
        for (final String transid : transids) {
            final CidsBean bestellungBean = fsBeanMap.get(transid);
            if ((bestellungBean != null) && (bestellungBean.getProperty("fehler") == null)) {
                try {
                    final Boolean propPostweg = (Boolean)bestellungBean.getProperty("postweg");
                    final Boolean propDuplicate = (Boolean)bestellungBean.getProperty("duplicate");
                    if (!Boolean.TRUE.equals(propDuplicate) && !Boolean.TRUE.equals(propPostweg)) {
                        bestellungBean.setProperty("erledigt", true);
                    }
                    getMetaService().updateMetaObject(
                        user,
                        bestellungBean.getMetaObject(),
                        getConnectionContext());
                } catch (final Exception ex) {
                    LOG.error("Fehler beim Persistieren der Bestellung", ex);
                }
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   loginName  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public CidsBean getExternalUser(final String loginName) throws Exception {
        final MetaClass mc = CidsBean.getMetaClassFromTableName(
                "WUNDA_BLAU",
                "billing_kunden_logins",
                getConnectionContext());
        if (mc == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(
                    "The metaclass for billing_kunden_logins is null. The current user has probably not the needed rights.");
            }
            return null;
        }
        String query = "SELECT " + mc.getID() + ", " + mc.getPrimaryKey() + " ";
        query += "FROM " + mc.getTableName();
        query += " WHERE name = '" + loginName + "'";

        CidsBean externalUser = null;
        final MetaObject[] metaObjects = getMetaService().getMetaObject(user, query, getConnectionContext());
        if ((metaObjects != null) && (metaObjects.length > 0)) {
            externalUser = metaObjects[0].getBean();
        }
        return externalUser;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   bestellungBean  DOCUMENT ME!
     * @param   transid         DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private CidsBean doBilling(final CidsBean bestellungBean, final String transid) {
        try {
            final CidsBean billingBean = CidsBean.createNewCidsBeanFromTableName(
                    "WUNDA_BLAU",
                    "Billing_Billing",
                    getConnectionContext());

            final boolean isPostweg = Boolean.TRUE.equals(bestellungBean.getProperty("postweg"));
            final Timestamp abrechnungsdatum = (Timestamp)bestellungBean.getProperty("eingang_ts");
            final Double gebuehr = (Double)bestellungBean.getProperty("gebuehr");
            final String projektBezeichnung = ((bestellungBean.getProperty("fk_adresse_rechnung.firma") != null)
                    ? ((String)bestellungBean.getProperty("fk_adresse_rechnung.firma") + ", ") : "")
                        + (String)bestellungBean.getProperty("fk_adresse_rechnung.name") + " "
                        + (String)bestellungBean.getProperty("fk_adresse_rechnung.vorname");
            final String request_url = (String)bestellungBean.getProperty("request_url");

            final String kunde_login = FormSolutionsProperties.getInstance().getBillingKundeLogin();
            final String modus = FormSolutionsProperties.getInstance().getBillingModus();
            final String modusbezeichnung = FormSolutionsProperties.getInstance().getBillingModusbezeichnung();
            final String verwendungszweck = isPostweg
                ? FormSolutionsProperties.getInstance().getBillingVerwendungskeyPostweg()
                : FormSolutionsProperties.getInstance().getBillingVerwendungszweckDownload();
            final String verwendungskey = isPostweg
                ? FormSolutionsProperties.getInstance().getBillingVerwendungskeyPostweg()
                : FormSolutionsProperties.getInstance().getBillingVerwendungskeyDownload();
            final String produktkey = (String)bestellungBean.getProperty("fk_produkt.billing_key");
            final String produktbezeichnung = (String)bestellungBean.getProperty("fk_produkt.billing_desc");

            billingBean.setProperty("request", request_url);
            billingBean.setProperty(
                "geometrie",
                (CidsBean)bestellungBean.getProperty("geometrie"));

            billingBean.setProperty("username", kunde_login);
            billingBean.setProperty("angelegt_durch", getExternalUser(kunde_login));
            billingBean.setProperty("ts", abrechnungsdatum);
            billingBean.setProperty("abrechnungsdatum", abrechnungsdatum);
            billingBean.setProperty("modus", modus);
            billingBean.setProperty("modusbezeichnung", modusbezeichnung);
            billingBean.setProperty("produktkey", produktkey);
            billingBean.setProperty("produktbezeichnung", produktbezeichnung);
            billingBean.setProperty("netto_summe", gebuehr);
            billingBean.setProperty("brutto_summe", gebuehr);
            billingBean.setProperty("geschaeftsbuchnummer", transid);
            billingBean.setProperty("verwendungszweck", verwendungszweck);
            billingBean.setProperty("verwendungskey", verwendungskey);
            billingBean.setProperty("projektbezeichnung", projektBezeichnung);
            billingBean.setProperty("mwst_satz", 0d);
            billingBean.setProperty("angeschaeftsbuch", Boolean.FALSE);
            billingBean.setProperty("abgerechnet", Boolean.TRUE);
            if ((transid != null) && transid.startsWith(TEST_CISMET00_PREFIX)) {
                LOG.info("Test-Object would have created this Billing-Entry: " + billingBean.getMOString());
                return null;
            } else {
                return getMetaService().insertMetaObject(
                            user,
                            billingBean.getMetaObject(),
                            getConnectionContext()).getBean();
            }
        } catch (Exception e) {
            LOG.error("Error during the persitence of the billing log.", e);
            return null;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param  fsBeanMap  DOCUMENT ME!
     */
    private void finalizeMySqls(final Map<String, CidsBean> fsBeanMap) {
        final Collection<String> transids = new ArrayList<>(fsBeanMap.keySet());
        for (final String transid : transids) {
            final CidsBean bestellungBean = fsBeanMap.get(transid);
            if ((bestellungBean != null) && (bestellungBean.getProperty("fehler") == null)) {
                final int okStatus;
                final Boolean propPostweg = (Boolean)bestellungBean.getProperty("postweg");
                if (Boolean.TRUE.equals(propPostweg)) {
                    okStatus = STATUS_PENDING;
                } else {
                    okStatus = STATUS_DONE;
                }
                try {
                    if (!FormSolutionsProperties.getInstance().isMysqlDisabled()) {
                        getMySqlHelper().updateStatus(transid, okStatus);
                    }
                    doStatusChangedRequest(transid);
                } catch (final Exception ex) {
                    setErrorStatus(transid, 10, bestellungBean, "Fehler beim Abschließen des MYSQL-Datensatzes", ex);
                }
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param  fsBeanMap  DOCUMENT ME!
     */
    private void finalizeEntries(final Map<String, CidsBean> fsBeanMap) {
        finalizeMySqls(fsBeanMap);
        finalizeBeans(fsBeanMap);
    }

    @Override
    public Object execute(final Object body, final ServerActionParameter... params) {
        logSpecial("execute by: " + getUser().getName());

        boolean fetchFromFs = true;
        boolean singleStep = false;
        int startStep = STATUS_FETCH;

        Collection<MetaObjectNode> mons = null;

        if (params != null) {
            for (final ServerActionParameter sap : params) {
                if (sap.getKey().equals(PARAMETER_TYPE.METAOBJECTNODES.toString())) {
                    mons = (Collection)sap.getValue();
                    fetchFromFs = false;
                } else if (sap.getKey().equals(PARAMETER_TYPE.STEP_TO_EXECUTE.toString())) {
                    startStep = (Integer)sap.getValue();
                } else if (sap.getKey().equals(PARAMETER_TYPE.SINGLE_STEP.toString())) {
                    singleStep = (Boolean)sap.getValue();
                }
            }
        }

        if (fetchFromFs) {
            // fetchFromFs always beginns with STATUS_FETCH
            startStep = STATUS_FETCH;
            singleStep = false;
        } else {
            if (startStep >= STATUS_SAVE) {
                throw new IllegalStateException("fetch not allowed with metaobjectnodes");
            }
            if (mons == null) {
                throw new IllegalStateException("metaobjectnodes are null");
            }
        }

        synchronized (this) {
            final Map<String, CidsBean> fsBeanMap = new HashMap();

            if (!fetchFromFs) {
                if (mons != null) {
                    logSpecial("start fetching from DB. Numof objects: " + mons.size());
                    for (final MetaObjectNode mon : mons) {
                        final CidsBean bestellungBean;
                        try {
                            bestellungBean = DomainServerImpl.getServerInstance()
                                        .getMetaObject(
                                                getUser(),
                                                mon.getObjectId(),
                                                mon.getClassId(),
                                                getConnectionContext())
                                        .getBean();
                            final String transid = (String)bestellungBean.getProperty("transid");
                            fsBeanMap.put(transid, bestellungBean);
                            if (!((startStep == STATUS_BILLING) && singleStep)) {
                                bestellungBean.setProperty("erledigt", false);
                                bestellungBean.setProperty("fehler", null);
                                bestellungBean.setProperty("fehler_ts", null);
                                bestellungBean.setProperty("exception", null);
                                bestellungBean.setProperty("produkt_dateipfad", null);
                                bestellungBean.setProperty("produkt_dateiname_orig", null);
                                metaService.updateMetaObject(
                                    getUser(),
                                    bestellungBean.getMetaObject(),
                                    getConnectionContext());
                            }
                        } catch (final Exception ex) {
                            LOG.error(ex, ex);
                        }
                    }
                    logSpecial("objects fetched from DB");
                }
            }

            switch (startStep) {
                case STATUS_FETCH:
                case STATUS_PARSE:
                case STATUS_SAVE: {
                    if (fetchFromFs) {
                        try {
                            final Map<String, ProductType> typeMap = new HashMap<>();

                            final Collection<String> transIds = new ArrayList<>();
                            transIds.addAll(getOpenExtendedTransids(ProductType.SGK, typeMap));
                            transIds.addAll(getOpenExtendedTransids(ProductType.ABK, typeMap));

                            // TEST OBJECTS
                            try {
                                if (testCismet00Type != null) {
                                    final String transId = TEST_CISMET00_PREFIX
                                                + RandomStringUtils.randomAlphanumeric(8);
                                    transIds.add(transId);
                                    typeMap.put(transId, testCismet00Type);
                                }
                            } catch (final Exception ex) {
                                LOG.error("error while generating TEST_CISMET00 transid", ex);
                            }
                            transIds.removeAll(ignoreTransids);
                            //

                            final Map<String, Exception> insertExceptionMap = createMySqlEntries(transIds);
                            final Map<String, String> fsXmlMap = extractXmlParts(transIds);
                            final Map<String, FormSolutionsBestellung> fsBestellungMap = createBestellungMap(
                                    fsXmlMap,
                                    typeMap);
                            fsBeanMap.putAll(createCidsEntries(
                                    fsXmlMap,
                                    fsBestellungMap,
                                    typeMap,
                                    insertExceptionMap));

                            logSpecial("fetched from FS");
                        } catch (Exception ex) {
                            LOG.error("Die Liste der FormSolutions-Bestellungen konnte nicht abgerufen werden", ex);
                        }
                    }
                    if (singleStep) {
                        break;
                    }
                }
                case STATUS_CLOSE: {
                    closeTransactions(fsBeanMap);
                    if (singleStep) {
                        break;
                    }
                }
                case STATUS_CREATEURL:
                case STATUS_DOWNLOAD: {
                    final Map<String, URL> fsUrlMap = createUrlMap(fsBeanMap);
                    downloadProdukte(fsBeanMap, fsUrlMap);
                    if (singleStep) {
                        break;
                    }
                }
                case STATUS_BILLING: {
                    for (final String transid : new ArrayList<>(fsBeanMap.keySet())) {
                        final CidsBean bestellungBean = fsBeanMap.get(transid);

                        if ((bestellungBean != null)
                                    && !Boolean.TRUE.equals(bestellungBean.getProperty("duplicate"))
                                    && (bestellungBean.getProperty("gutschein_code") == null)) {
                            doPureBilling(bestellungBean, transid);
                        }
                    }
                    if (singleStep) {
                        break;
                    }
                }
                case STATUS_PENDING:
                case STATUS_DONE: {
                    finalizeEntries(fsBeanMap);
                }
            }
        }
        return null;
    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }

    @Override
    public User getUser() {
        return user;
    }

    @Override
    public void setUser(final User user) {
        this.user = user;
    }

    @Override
    public void setMetaService(final MetaService metaService) {
        this.metaService = metaService;
    }

    @Override
    public MetaService getMetaService() {
        return metaService;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  message  DOCUMENT ME!
     */
    private void logSpecial(final String message) {
        if (specialLogWriter != null) {
            try {
                specialLogWriter.write(System.currentTimeMillis() + " - " + message + "\n");
                specialLogWriter.flush();
            } catch (final IOException ex) {
                LOG.warn("could not write to logSpecial", ex);
            }
        }
    }

    @Override
    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }
}
