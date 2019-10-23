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
package de.cismet.cids.custom.utils.formsolutions;

import Sirius.server.middleware.impls.domainserver.DomainServerImpl;
import Sirius.server.middleware.interfaces.domainserver.MetaService;
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

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Logger;

import org.openide.util.Lookup;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;

import java.net.URL;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

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
import java.util.List;
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
import de.cismet.cids.custom.utils.alkis.BaulastBescheinigungHelper;
import de.cismet.cids.custom.utils.alkis.ServerAlkisProducts;
import de.cismet.cids.custom.utils.berechtigungspruefung.BerechtigungspruefungHandler;
import de.cismet.cids.custom.utils.berechtigungspruefung.baulastbescheinigung.BerechtigungspruefungBescheinigungDownloadInfo;
import de.cismet.cids.custom.utils.billing.BillingInfo;
import de.cismet.cids.custom.utils.billing.BillingInfoHandler;
import de.cismet.cids.custom.utils.billing.BillingPrice;
import de.cismet.cids.custom.utils.billing.BillingProduct;
import de.cismet.cids.custom.utils.billing.BillingProductGroupAmount;
import de.cismet.cids.custom.wunda_blau.search.server.CidsAlkisSearchStatement;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.utils.MetaClassCacheService;
import de.cismet.cids.utils.serverresources.ServerResourcesLoader;

import de.cismet.commons.security.AccessHandler;
import de.cismet.commons.security.handler.SimpleHttpAccessHandler;

import de.cismet.connectioncontext.ConnectionContext;
import de.cismet.connectioncontext.ConnectionContextProvider;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class FormSolutionsBestellungHandler implements ConnectionContextProvider {

    //~ Static fields/initializers ---------------------------------------------

    public static final int STATUS_CREATE = 100;
    public static final int STATUS_FETCH = 70;
    public static final int STATUS_PARSE = 60;
    public static final int STATUS_GETFLURSTUECK = 55;
    public static final int STATUS_SAVE = 50;
    public static final int STATUS_CLOSE = 40;
    public static final int STATUS_PRUEFUNG = 35;
    public static final int STATUS_CREATEURL = 30;
    public static final int STATUS_DOWNLOAD = 20;
    public static final int STATUS_BILLING = 15;
    public static final int STATUS_PENDING = 10;
    public static final int STATUS_DONE = 0;

    public static final int GUTSCHEIN_YES = 1;
    public static final int GUTSCHEIN_NO = 2;

    private static final Logger LOG = Logger.getLogger(FormSolutionsBestellungHandler.class);
    private static final String TEST_CISMET00_PREFIX = "TEST_CISMET00-";
    private static final String GUTSCHEIN_ADDITIONAL_TEXT = "TESTAUSZUG - nur zur Demonstration (%s)";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Map<String, MetaClass> METACLASS_CACHE = new HashMap();
    private static final String EXTERNAL_USER_QUERY_TEMPLATE = "SELECT %d, %s FROM %s WHERE name = '%s';";
    private static final String UNFINISHED_BESTELLUNGEN_QUERY_TEMPLATE =
        "SELECT %d, %s FROM %s WHERE test IS NOT TRUE AND duplicate IS NOT TRUE AND postweg IS NOT TRUE AND fehler IS NULL AND erledigt IS NOT TRUE;";
    private static final String PRODUKT_QUERY_TEMPLATE = "SELECT DISTINCT %d, %s FROM %s WHERE %s LIMIT 1;";
    private static final String BESTELLUNG_BY_TRANSID_QUERY_TEMPLATE =
        "SELECT DISTINCT %d, %s FROM %s WHERE transid LIKE '%s';";

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum ProductType {

        //~ Enum constants -----------------------------------------------------

        SGK, ABK, BAB_VORBEREITUNG, BAB_ABSCHLUSS
    }

    //~ Instance fields --------------------------------------------------------

    private final User user;
    private final MetaService metaService;
    private final ConnectionContext connectionContext;
    private final SimpleHttpAccessHandler httpHandler = new SimpleHttpAccessHandler();
    private final UsernamePasswordCredentials creds;
    private final String testCismet00Xml;
    private final ProductType testCismet00Type;
    private final Set<String> ignoreTransids = new HashSet<>();
    private final BaulastBescheinigungHelper baulastBescheinigungHelper;
    private final BillingInfoHandler billingInfoHander;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new FormSolutionsBestellungHandler object.
     *
     * @param  user               DOCUMENT ME!
     * @param  metaService        DOCUMENT ME!
     * @param  connectionContext  DOCUMENT ME!
     */
    public FormSolutionsBestellungHandler(final User user,
            final MetaService metaService,
            final ConnectionContext connectionContext) {
        this(false, user, metaService, connectionContext);
    }

    /**
     * Creates a new FormSolutionsBestellungHandler object.
     *
     * @param  fromStartupHook    DOCUMENT ME!
     * @param  user               DOCUMENT ME!
     * @param  metaService        DOCUMENT ME!
     * @param  connectionContext  DOCUMENT ME!
     */
    public FormSolutionsBestellungHandler(final boolean fromStartupHook,
            final User user,
            final MetaService metaService,
            final ConnectionContext connectionContext) {
        UsernamePasswordCredentials creds = null;
        ProductType testCismet00Type = null;
        String testCismet00Xml = null;

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
                testCismet00Type = parseProductType(FormSolutionsProperties.getInstance().getTestCismet00());
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
        }

        BillingInfoHandler billingInfoHander = null;
        try {
            billingInfoHander = new BillingInfoHandler(getObjectMapper().readValue(
                        ServerResourcesLoader.getInstance().loadText(
                            WundaBlauServerResources.BILLING_JSON.getValue()),
                        BillingInfo.class));
        } catch (final Exception ex) {
            LOG.error(ex, ex);
        }
        this.billingInfoHander = billingInfoHander;
        this.baulastBescheinigungHelper = new BaulastBescheinigungHelper(user, metaService, connectionContext);
        this.testCismet00Type = testCismet00Type;
        this.testCismet00Xml = testCismet00Xml;
        this.creds = creds;
        this.user = user;
        this.metaService = metaService;
        this.connectionContext = connectionContext;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private BaulastBescheinigungHelper getBaulastBescheinigungHelper() {
        return baulastBescheinigungHelper;
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
                            getUser(),
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
     * @return  DOCUMENT ME!
     */
    public MetaService getMetaService() {
        return metaService;
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
    private CidsBean getExternalUser(final String loginName) throws Exception {
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
        final String query = String.format(
                EXTERNAL_USER_QUERY_TEMPLATE,
                mc.getID(),
                mc.getPrimaryKey(),
                mc.getTableName(),
                loginName);

        CidsBean externalUser = null;
        final MetaObject[] metaObjects = getMetaService().getMetaObject(getUser(), query, getConnectionContext());
        if ((metaObjects != null) && (metaObjects.length > 0)) {
            externalUser = metaObjects[0].getBean();
        }
        return externalUser;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public User getUser() {
        return user;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Collection fetchEndExecuteAllOpen() {
        return fetchEndExecuteAllOpen(false);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   test  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Collection fetchEndExecuteAllOpen(final boolean test) {
        return execute(STATUS_FETCH, false, test, null);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   startStep  DOCUMENT ME!
     * @param   mons       DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Collection executeBeginningWithStep(final Integer startStep, final Collection<MetaObjectNode> mons) {
        return executeBeginningWithStep(startStep, false, mons);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   startStep  DOCUMENT ME!
     * @param   test       DOCUMENT ME!
     * @param   mons       DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Collection executeBeginningWithStep(final Integer startStep,
            final boolean test,
            final Collection<MetaObjectNode> mons) {
        return execute(startStep, false, test, mons);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   startStep  DOCUMENT ME!
     * @param   mons       DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Collection executeSingleStep(final Integer startStep, final Collection<MetaObjectNode> mons) {
        return executeSingleStep(startStep, false, mons);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   startStep  DOCUMENT ME!
     * @param   test       DOCUMENT ME!
     * @param   mons       DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Collection executeSingleStep(final Integer startStep,
            final boolean test,
            final Collection<MetaObjectNode> mons) {
        return execute(startStep, true, test, mons);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   startStep    DOCUMENT ME!
     * @param   _singleStep  DOCUMENT ME!
     * @param   test         DOCUMENT ME!
     * @param   mons         DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  IllegalStateException  DOCUMENT ME!
     */
    public Collection execute(final int startStep,
            final boolean _singleStep,
            final boolean test,
            final Collection<MetaObjectNode> mons) {
        final boolean singleStep;
        if (mons == null) {
            singleStep = false;
        } else {
            singleStep = _singleStep;
            if (startStep >= FormSolutionsBestellungHandler.STATUS_SAVE) {
                throw new IllegalStateException("fetch not allowed with metaobjectnodes");
            }
            if (mons == null) {
                throw new IllegalStateException("metaobjectnodes are null");
            }
        }

        Map<String, CidsBean> fsBeanMap = null;
        if (mons != null) {
            fsBeanMap = loadCidsEntries(mons);
        }

        Map<String, FormSolutionsBestellung> fsBestellungMap = null;

        switch (startStep) {
            case STATUS_FETCH:
            case STATUS_PARSE:
            case STATUS_SAVE: {
                if (mons == null) {
                    try {
                        if (startStep >= STATUS_CLOSE) {
                            final Map<String, ProductType> typeMap = test ? getTransIdsFromTest() : doFetchTransIds();
                            final Map<String, Exception> insertExceptionMap = createMySqlEntries(typeMap.keySet());
                            final Map<String, String> fsXmlMap = extractXmlParts(typeMap.keySet());
                            fsBestellungMap = createBestellungMap(
                                    fsXmlMap,
                                    typeMap);
                            fsBeanMap = createCidsEntries(
                                    fsXmlMap,
                                    fsBestellungMap,
                                    typeMap,
                                    insertExceptionMap);
                        }
                    } catch (final Exception ex) {
                        LOG.error("Die Liste der FormSolutions-Bestellungen konnte nicht abgerufen werden", ex);
                    }
                }
            }
            case STATUS_CLOSE: {
                if (fsBeanMap != null) {
                    closeTransactions(fsBeanMap);
                }
                if (singleStep) {
                    break;
                }
            }
            case STATUS_PRUEFUNG: {
                if (!pruefungProdukt(fsBeanMap, fsBestellungMap)) {
                    break;
                }
            }
            case STATUS_CREATEURL:
            case STATUS_DOWNLOAD: {
                if (fsBeanMap != null) {
                    downloadProdukte(fsBeanMap);
                }
                if (singleStep) {
                    break;
                }
            }
            case STATUS_BILLING: {
                if (fsBeanMap != null) {
                    doPureBilling(fsBeanMap);
                }
                if (singleStep) {
                    break;
                }
            }
            case STATUS_PENDING:
            case STATUS_DONE: {
                if (fsBeanMap != null) {
                    finalizeEntries(fsBeanMap);
                }
            }
        }
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   mons  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private Map<String, CidsBean> loadCidsEntries(final Collection<MetaObjectNode> mons) {
        FormSolutionBestellungSpecialLogger.getInstance().log("start fetching from DB. Numof objects: " + mons.size());
        final Map<String, CidsBean> fsBeanMap = new HashMap();
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
            } catch (final Exception ex) {
                LOG.error(ex, ex);
            }
        }
        FormSolutionBestellungSpecialLogger.getInstance().log("objects fetched from DB");
        return fsBeanMap;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private Map<String, ProductType> getTransIdsFromTest() throws Exception {
        final Map<String, ProductType> transIdProductTypeMap = new HashMap<>();
        try {
            if (testCismet00Type != null) {
                final String transId = TEST_CISMET00_PREFIX
                            + RandomStringUtils.randomAlphanumeric(8);
                transIdProductTypeMap.put(transId, testCismet00Type);
            }
        } catch (final Exception ex) {
            LOG.error("error while generating TEST_CISMET00 transid", ex);
        }
        return transIdProductTypeMap;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private Map<String, ProductType> doFetchTransIds() throws Exception {
        FormSolutionBestellungSpecialLogger.getInstance().log("fetched from FS");

        final Map<String, ProductType> transIdProductTypeMap = new HashMap<>();

        for (final ProductType productType : ProductType.values()) {
            final Collection<String> transIds = getOpenExtendedTransids(productType);
            for (final String transId : transIds) {
                transIdProductTypeMap.put(transId, productType);
            }
        }

        for (final String transId : ignoreTransids) {
            transIdProductTypeMap.remove(transId);
        }

        return transIdProductTypeMap;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  fsBeanMap  DOCUMENT ME!
     */
    private void doPureBilling(final Map<String, CidsBean> fsBeanMap) {
        for (final String transid : new ArrayList<>(fsBeanMap.keySet())) {
            final CidsBean bestellungBean = fsBeanMap.get(transid);

            if ((bestellungBean != null)
                        && !Boolean.TRUE.equals(bestellungBean.getProperty("duplicate"))
                        && (bestellungBean.getProperty("gutschein_code") == null)) {
                doPureBilling(bestellungBean, transid);
            }
        }
    }
    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public MetaObject[] getUnfinishedBestellungen() throws Exception {
        final MetaClass mcBestellung = CidsBean.getMetaClassFromTableName(
                "WUNDA_BLAU",
                "fs_bestellung",
                getConnectionContext());
        final String pruefungQuery = String.format(
                UNFINISHED_BESTELLUNGEN_QUERY_TEMPLATE,
                mcBestellung.getID(),
                mcBestellung.getTableName()
                        + "."
                        + mcBestellung.getPrimaryKey(),
                mcBestellung.getTableName());
        return getMetaService().getMetaObject(getUser(), pruefungQuery, getConnectionContext());
    }

    /**
     * DOCUMENT ME!
     *
     * @param   productType  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private Collection<String> getOpenExtendedTransids(final ProductType productType) throws Exception {
        FormSolutionBestellungSpecialLogger.getInstance().log("fetching open transids from FS");

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
                case BAB_VORBEREITUNG: {
                    auftragsListeUrl = new URL(FormSolutionsProperties.getInstance().getUrlAuftragslisteBb1Fs());
                }
                break;
                case BAB_ABSCHLUSS: {
                    auftragsListeUrl = new URL(FormSolutionsProperties.getInstance().getUrlAuftragslisteBb2Fs());
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

            FormSolutionBestellungSpecialLogger.getInstance().log("open transids fetched: " + stringBuilder.toString());

            final Map<String, Object> map = getObjectMapper().readValue("{ \"list\" : " + stringBuilder.toString()
                            + "}",
                    new TypeReference<HashMap<String, Object>>() {
                    });
            for (final String transId : (Collection<String>)map.get("list")) {
                transIds.add(transId);
            }
        } catch (final Exception ex) {
            LOG.error("error while retrieving open transids", ex);
        }

        return transIds;
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
                        FormSolutionsMySqlHelper.getInstance().updateStatus(transid, okStatus);
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
                        getUser(),
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
                        getUser(),
                        bestellungBean.getMetaObject(),
                        getConnectionContext());
                }
            } catch (final Exception ex) {
                LOG.error("Fehler beim Persistieren der Bean", ex);
            }
        }
        try {
            if (!FormSolutionsProperties.getInstance().isMysqlDisabled()) {
                FormSolutionsMySqlHelper.getInstance().updateStatus(transid, -status);
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
        FormSolutionBestellungSpecialLogger.getInstance().log("getting auftrag from FS for: " + auftrag);
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

            FormSolutionBestellungSpecialLogger.getInstance().log("auftrag returned from FS: " + convertedXml);

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
        FormSolutionBestellungSpecialLogger.getInstance().log("closing transaction for: " + transid);

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
            FormSolutionBestellungSpecialLogger.getInstance().log("doing status changed request for: " + transid);

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
    private static String extractProduct(final FormSolutionsBestellung formSolutionsBestellung,
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
            case BAB_VORBEREITUNG: {
                produktSB = new StringBuffer("Baulastenbescheinigung");
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

        switch (type) {
            case SGK: {
                if ("farbig".equals(farbauspraegung)) {
                    return "LK.NRW.K.BF";
                } else if ("Graustufen".equals(farbauspraegung)) {
                    return "LK.NRW.K.BSW";
                } else {
                    return null;
                }
            }
            case ABK: {
                if ("farbig".equals(farbauspraegung)) {
                    return "LK.GDBNRW.A.ABKF";
                } else if ("Graustufen".equals(farbauspraegung)) {
                    return "LK.GDBNRW.A.ABKSW";
                } else {
                    return null;
                }
            }
            case BAB_VORBEREITUNG: {
                return "BAB";
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
        if ((rawDin != null) && rawDin.toUpperCase().contains("DIN") && (rawAusrichtung != null)) {
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

        final String produktQuery = String.format(
                PRODUKT_QUERY_TEMPLATE,
                produktMc.getID(),
                produktMc.getTableName()
                        + "."
                        + produktMc.getPrimaryKey(),
                produktMc.getTableName()
                        + ", "
                        + produktTypMc.getTableName()
                        + ", "
                        + formatMc.getTableName(),
                ((formatKey != null) ? (produktMc.getTableName() + ".fk_format = " + formatMc.getTableName() + ".id")
                                     : "TRUE")
                        + " "
                        + "AND "
                        + produktMc.getTableName()
                        + ".fk_typ = "
                        + produktTypMc.getTableName()
                        + ".id "
                        + "AND "
                        + produktTypMc.getTableName()
                        + ".key = '"
                        + produktKey
                        + "' "
                        + "AND "
                        + ((formatKey != null) ? (formatMc.getTableName() + ".key = '" + formatKey + "'") : "TRUE"));
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
        final Set<String> fskz = new LinkedHashSet<>();
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
        final Integer massstab =
            ((formSolutionsBestellung.getMassstab() != null) && formSolutionsBestellung.getMassstab().contains(":"))
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
     * @param   downloadInfo  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private double calculateBabGebuehr(final BerechtigungspruefungBescheinigungDownloadInfo downloadInfo) {
        final BillingProduct product = billingInfoHander.getProducts().get("blab_be");
        final String usage = "eigG frei";
        final Collection<BillingProductGroupAmount> prodAmounts = new ArrayList<>();
        for (final HashMap.Entry<String, Integer> amount : downloadInfo.getAmounts().entrySet()) {
            prodAmounts.add(new BillingProductGroupAmount(amount.getKey(), amount.getValue()));
        }
        final double raw = BillingInfoHandler.calculateRawPrice(
                product,
                prodAmounts.toArray(new BillingProductGroupAmount[0]));
        final BillingPrice price = new BillingPrice(raw, usage, product);
        return price.getNetto();
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

    @Override
    public ConnectionContext getConnectionContext() {
        return connectionContext;
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

        final Map localServers = new HashMap<>();
        localServers.put("WUNDA_BLAU", getMetaService());
        search.setActiveLocalServers(localServers);
        search.setUser(getUser());
        final Collection<MetaObjectNode> mons = search.performServerSearch();
        if ((mons != null) && !mons.isEmpty()) {
            final MetaObjectNode mon = new ArrayList<>(mons).get(0);
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
                productDesc.getMassstab(),
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
     * @param   in               DOCUMENT ME!
     * @param   destinationPath  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private void downloadProdukt(final InputStream in, final String destinationPath) throws Exception {
        FormSolutionsFtpClient.getInstance()
                .upload(in, FormSolutionsProperties.getInstance().getProduktBasepath() + destinationPath);
    }
    /**
     * DOCUMENT ME!
     *
     * @param   productUrl       DOCUMENT ME!
     * @param   destinationPath  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private void downloadKartenProdukt(final URL productUrl, final String destinationPath) throws Exception {
        try(final InputStream in = getHttpAccessHandler().doRequest(
                            productUrl,
                            new StringReader(""),
                            AccessHandler.ACCESS_METHODS.GET_REQUEST,
                            null,
                            creds);
            ) {
            downloadProdukt(in, destinationPath);
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
        FormSolutionBestellungSpecialLogger.getInstance()
                .log("extracting xml parts for num of objects: " + transids.size());

        final Map<String, String> fsXmlMap = new HashMap<>(transids.size());

        for (final String transid : transids) {
            try {
                final String auftragXml = getAuftrag(transid);
                fsXmlMap.put(transid, auftragXml);
                FormSolutionBestellungSpecialLogger.getInstance().log("updating mysql entry for: " + transid);
                if (!FormSolutionsProperties.getInstance().isMysqlDisabled()) {
                    FormSolutionsMySqlHelper.getInstance().updateStatus(transid, STATUS_FETCH);
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
        final Collection<String> transids = new ArrayList<>(fsXmlMap.keySet());

        FormSolutionBestellungSpecialLogger.getInstance()
                .log("creating simple bestellung bean for num of objects: " + transids.size());

        final Map<String, FormSolutionsBestellung> fsBestellungMap = new HashMap<>(
                transids.size());
        for (final String transid : transids) {
            try {
                final String auftragXml = fsXmlMap.get(transid);
                final InputStream inputStream = IOUtils.toInputStream(auftragXml, "UTF-8");

                FormSolutionBestellungSpecialLogger.getInstance()
                        .log("creating simple bestellung bean for: " + transid);

                final FormSolutionsBestellung formSolutionsBestellung = createFormSolutionsBestellung(inputStream);
                fsBestellungMap.put(transid, formSolutionsBestellung);

                FormSolutionBestellungSpecialLogger.getInstance()
                        .log("simple bestellung bean created for: " + transids.size());

                final boolean downloadOnly = !"Kartenausdruck".equals(formSolutionsBestellung.getBezugsweg());
                final String email = downloadOnly ? trimedNotEmpty(formSolutionsBestellung.getEMailadresse())
                                                  : trimedNotEmpty(formSolutionsBestellung.getEMailadresse()); // 1

                FormSolutionBestellungSpecialLogger.getInstance().log("updating mysql email entry for: " + transid);

                if (!FormSolutionsProperties.getInstance().isMysqlDisabled()) {
                    FormSolutionsMySqlHelper.getInstance()
                            .updateRequest(
                                transid,
                                STATUS_PARSE,
                                extractLandparcelcode(formSolutionsBestellung),
                                extractProduct(formSolutionsBestellung, typeMap.get(transid)),
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

        FormSolutionBestellungSpecialLogger.getInstance()
                .log("creating cids entries for num of objects: " + transids.size());

        final Map<String, CidsBean> fsBeanMap = new HashMap<>(transids.size());
        for (final String transid : transids) {
            FormSolutionBestellungSpecialLogger.getInstance().log("creating cids entry for: " + transid);

            final String auftragXml = fsXmlMap.get(transid);
            final FormSolutionsBestellung formSolutionBestellung = fsBestellungMap.get(transid);

            boolean duplicate = false;
            try {
                final MetaClass bestellungMc = getMetaClass("fs_bestellung", getConnectionContext());
                final String searchQuery = String.format(
                        BESTELLUNG_BY_TRANSID_QUERY_TEMPLATE,
                        bestellungMc.getID(),
                        bestellungMc.getTableName()
                                + "."
                                + bestellungMc.getPrimaryKey(),
                        bestellungMc.getTableName(),
                        transid);
                final MetaObject[] mos = getMetaService().getMetaObject(getUser(), searchQuery);
                if ((mos != null) && (mos.length > 0)) {
                    duplicate = true;
                }
            } catch (final Exception ex) {
                final String message = "error while search for duplicates for " + transid;
                LOG.error(message, ex);
                FormSolutionBestellungSpecialLogger.getInstance().log(message);
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

                FormSolutionBestellungSpecialLogger.getInstance().log("persisting cids entry for: " + transid);

                final MetaObject persistedMo = getMetaService().insertMetaObject(
                        getUser(),
                        bestellungBean.getMetaObject(),
                        getConnectionContext());

                final CidsBean persistedBestellungBean = persistedMo.getBean();
                fsBeanMap.put(transid, persistedBestellungBean);

                if (!FormSolutionsProperties.getInstance().isMysqlDisabled()) {
                    FormSolutionBestellungSpecialLogger.getInstance().log("updating mysql entry for: " + transid);
                    FormSolutionsMySqlHelper.getInstance().updateStatus(transid, STATUS_SAVE);
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
        } else if (ProductType.BAB_VORBEREITUNG.toString().equals(string)) {
            return ProductType.BAB_VORBEREITUNG;
        } else {
            return null;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   transid  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private boolean checkMysqlEntry(final String transid) {
        boolean mysqlEntryAlreadyExists = false;
        ResultSet resultSet = null;
        try {
            resultSet = FormSolutionsMySqlHelper.getInstance().select(transid);
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
        return mysqlEntryAlreadyExists;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   transids  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private Map<String, Exception> createMySqlEntries(final Collection<String> transids) {
        final Map<String, Exception> insertExceptionMap = new HashMap<>(transids.size());

        if (!FormSolutionsProperties.getInstance().isMysqlDisabled()) {
            for (final String transid : transids) {
                try {
                    FormSolutionBestellungSpecialLogger.getInstance()
                            .log("updating or inserting mySQL entry for: " + transid);

                    if (checkMysqlEntry(transid)) {
                        FormSolutionsMySqlHelper.getInstance().updateStatus(transid, STATUS_CREATE);
                    } else {
                        FormSolutionsMySqlHelper.getInstance().insertMySql(transid, STATUS_CREATE);
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
        FormSolutionBestellungSpecialLogger.getInstance()
                .log("closing transactions for num of objects: " + transids.size());

        for (final String transid : transids) {
            final CidsBean bestellungBean = fsBeanMap.get(transid);
            if ((bestellungBean != null)) {
                try {
                    closeTransid(transid);

                    FormSolutionBestellungSpecialLogger.getInstance().log("updating mysql entry for: " + transid);
                    if (!FormSolutionsProperties.getInstance().isMysqlDisabled()) {
                        FormSolutionsMySqlHelper.getInstance().updateStatus(transid, STATUS_CLOSE);
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
     * @param   transid         fsBean DOCUMENT ME!
     * @param   bestellungBean  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private URL createUrl(final String transid, final CidsBean bestellungBean) {
        URL productUrl = null;
        if ((bestellungBean != null)
                    && !Boolean.TRUE.equals(bestellungBean.getProperty("duplicate"))
                    && (bestellungBean.getProperty("fehler") == null)) {
            try {
                productUrl = createProductUrl(bestellungBean);
                if (!FormSolutionsProperties.getInstance().isMysqlDisabled()) {
                    FormSolutionsMySqlHelper.getInstance().updateStatus(transid, STATUS_CREATEURL);
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
        return productUrl;
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

            FormSolutionsFtpClient.getInstance()
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
     * @param   fsBeanMap        DOCUMENT ME!
     * @param   fsBestellungMap  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private boolean pruefungProdukt(final Map<String, CidsBean> fsBeanMap,
            final Map<String, FormSolutionsBestellung> fsBestellungMap) {
        final Collection<String> transids = new ArrayList<>(fsBestellungMap.keySet());

        for (final String transid : transids) {
            final CidsBean bestellungBean = fsBeanMap.get(transid);
            if ((bestellungBean != null) && (bestellungBean.getProperty("fehler") == null)) {
                try {
                    final ProductType productType = determineProductType(bestellungBean);
                    switch (productType) {
                        case SGK: {
                            return true;
                        }
                        case ABK: {
                            return true;
                        }
                        case BAB_VORBEREITUNG: {
                            final String flurstueckKennzeichen = ((String)bestellungBean.getProperty("landparcelcode"));
                            final String auftragsNummer = transid;
                            final String produktBezeichnung = (String)bestellungBean.getProperty("landparcelcode");
                            final List<CidsBean> flurstuecke = new ArrayList<>();
                            if (flurstueckKennzeichen != null) {
                                for (final String einzelFSKennzeichen : flurstueckKennzeichen.split(",")) {
                                    flurstuecke.add(getFlurstueck(einzelFSKennzeichen));
                                }
                            }
                            final BaulastBescheinigungHelper.ProtocolBuffer protocolBuffer =
                                new BaulastBescheinigungHelper.ProtocolBuffer();
                            final BaulastBescheinigungHelper.StatusHolder statusHolder =
                                new BaulastBescheinigungHelper.StatusHolder() {

                                    @Override
                                    public void setMessage(final String message) {
                                        super.setMessage(message);
                                        LOG.info(message);
                                    }
                                };

                            final BerechtigungspruefungBescheinigungDownloadInfo downloadInfo =
                                getBaulastBescheinigungHelper().calculateDownloadInfo(
                                    auftragsNummer,
                                    produktBezeichnung,
                                    flurstuecke,
                                    protocolBuffer,
                                    statusHolder);
                            final Double gebuehr = calculateBabGebuehr(downloadInfo);
                            bestellungBean.setProperty("gebuehr", gebuehr);

                            final FormSolutionsBestellung formSolutionBestellung = fsBestellungMap.get(transid);

                            // fetch nachweis
                            formSolutionBestellung.getNachweis();
                            final String dateiName = "upload.dat";
                            final byte[] data = null;
                            final String schluessel = BerechtigungspruefungHandler.getInstance()
                                        .createNewSchluessel(getUser(), downloadInfo);
                            final CidsBean pruefung = BerechtigungspruefungHandler.getInstance()
                                        .addNewAnfrage(
                                            getUser(),
                                            schluessel,
                                            downloadInfo,
                                            formSolutionBestellung.getBerechtigungsgrund(),
                                            formSolutionBestellung.getBegruendungstext(),
                                            dateiName,
                                            data);

                            bestellungBean.setProperty("berechtigungspruefung", pruefung);
                            getMetaService().updateMetaObject(
                                getUser(),
                                bestellungBean.getMetaObject(),
                                getConnectionContext());
                        }
                        return false;
                    }
                } catch (final Exception ex) {
                    setErrorStatus(transid, STATUS_PRUEFUNG, bestellungBean, "Fehler beim Erzeugen des Produktes", ex);
                }
            }
        }
        return false;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   transid  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static String createTransidHash(final String transid) {
        return DigestUtils.md5Hex(FormSolutionsProperties.getInstance().getTransidHashpepper() + transid);
    }

    /**
     * DOCUMENT ME!
     *
     * @param  fsBeanMap  DOCUMENT ME!
     */
    private void downloadProdukte(final Map<String, CidsBean> fsBeanMap) {
        final Collection<String> transids = new ArrayList<>(fsBeanMap.keySet());
        for (final String transid : transids) {
            final CidsBean bestellungBean = fsBeanMap.get(transid);
            if ((bestellungBean != null) && (bestellungBean.getProperty("fehler") == null)) {
                try {
                    final ProductType productType = determineProductType(bestellungBean);
                    bestellungBean.setProperty("erledigt", false);
                    bestellungBean.setProperty("fehler", null);
                    bestellungBean.setProperty("fehler_ts", null);
                    bestellungBean.setProperty("exception", null);
                    bestellungBean.setProperty("produkt_dateipfad", null);
                    bestellungBean.setProperty("produkt_dateiname_orig", null);
                    bestellungBean.setProperty("produkt_ts", null);

                    getMetaService().updateMetaObject(
                        getUser(),
                        bestellungBean.getMetaObject(),
                        getConnectionContext());

                    switch (productType) {
                        case SGK:
                        case ABK: {
                            final URL productUrl = createUrl(transid, bestellungBean);
                            final String fileName = transid + ".pdf";
                            final String fileNameRechnung = "RE_" + transid + ".pdf";

                            downloadKartenProdukt(productUrl, fileName);

                            final String fileNameOrig = (String)bestellungBean.getProperty("fk_produkt.fk_typ.key")
                                        + "."
                                        + ((String)bestellungBean.getProperty("landparcelcode")).split(",")[0].replace(
                                            "/",
                                            "--")
                                        + ".pdf";

                            bestellungBean.setProperty("request_url", productUrl.toString());
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
                                getUser(),
                                bestellungBean.getMetaObject(),
                                getConnectionContext());

                            createRechnung(fileNameRechnung, bestellungBean);

                            if (!FormSolutionsProperties.getInstance().isMysqlDisabled()) {
                                if (checkMysqlEntry(transid)) {
                                    FormSolutionsMySqlHelper.getInstance()
                                            .updateProduct(
                                                transid,
                                                STATUS_DOWNLOAD,
                                                (String)bestellungBean.getProperty("produkt_dateipfad"),
                                                (String)bestellungBean.getProperty("produkt_dateiname_orig"));
                                } else {
                                    if (!FormSolutionsProperties.getInstance().isMysqlDisabled()) {
                                        FormSolutionsMySqlHelper.getInstance()
                                                .insertProductMySql(
                                                    transid,
                                                    STATUS_DOWNLOAD,
                                                    (String)bestellungBean.getProperty("landparcelcode"),
                                                    (String)bestellungBean.getProperty("fk_product.fk_typ.name"),
                                                    Boolean.TRUE.equals((Boolean)bestellungBean.getProperty("postweg")),
                                                    (String)bestellungBean.getProperty("email"),
                                                    (String)bestellungBean.getProperty("produkt_dateipfad"),
                                                    (String)bestellungBean.getProperty("produkt_dateiname_orig"));
                                    }
                                }
                                doStatusChangedRequest(transid);
                            }
                        }
                        break;
                        case BAB_VORBEREITUNG: {
                            final String transidHash = createTransidHash(transid);
                            final String redirectorUrlTemplate = FormSolutionsProperties.getInstance()
                                        .getCidsActionHttpRedirectorUrl();
                            final URL redirect2formsolutionsUrl = new URL(String.format(
                                        redirectorUrlTemplate,
                                        transidHash));

                            bestellungBean.setProperty("request_url", redirect2formsolutionsUrl.toString());
                            bestellungBean.setProperty("produkt_ts", new Timestamp(new Date().getTime()));
                            getMetaService().updateMetaObject(
                                getUser(),
                                bestellungBean.getMetaObject(),
                                getConnectionContext());

                            LOG.fatal(redirect2formsolutionsUrl);
                            // mysql update
                            // doStatusChangedRequest(transid);
                        }
                        break;
                        case BAB_ABSCHLUSS: {
                            final CidsBean berechtigungspruefung = (CidsBean)bestellungBean.getProperty(
                                    "berechtigungspruefung");
                            if (berechtigungspruefung != null) {
                                final BerechtigungspruefungBescheinigungDownloadInfo downloadInfo =
                                    new ObjectMapper().readValue((String)berechtigungspruefung.getProperty(
                                            "downloadinfo_json"),
                                        BerechtigungspruefungBescheinigungDownloadInfo.class);
                                final File file = new File(FormSolutionsProperties.getInstance().getProduktTmpAbsPath()
                                                + transid + ".zip");
                                getBaulastBescheinigungHelper().writeFullBescheinigung(downloadInfo, file);
                                LOG.fatal(downloadInfo);
                            }
                        }
                        break;
                    }
                } catch (final Exception ex) {
                    setErrorStatus(transid, STATUS_DOWNLOAD, bestellungBean, "Fehler beim Erzeugen des Produktes", ex);
                }
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   bestellungBean  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private ProductType determineProductType(final CidsBean bestellungBean) {
        final boolean hasTypKey = (bestellungBean != null)
                    && (bestellungBean.getProperty("fk_produkt.fk_typ.key") != null);
        final String type = hasTypKey ? (String)bestellungBean.getProperty("fk_produkt.fk_typ.key") : null;
        if (type != null) {
            switch (type) {
                case "LK.NRW.K.BF":
                case "LK.NRW.K.BSW": {
                    return ProductType.SGK;
                }
                case "LK.GDBNRW.A.ABKF":
                case "LK.GDBNRW.A.ABKSW": {
                    return ProductType.ABK;
                }
                case "BAB": {
                    return ProductType.BAB_VORBEREITUNG;
                }
            }
        }
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private SimpleHttpAccessHandler getHttpAccessHandler() {
        return httpHandler;
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
                        getUser(),
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
}
