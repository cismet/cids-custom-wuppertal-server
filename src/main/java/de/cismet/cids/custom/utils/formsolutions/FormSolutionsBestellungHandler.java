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
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Logger;

import org.openide.util.Lookup;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;

import java.net.URL;
import java.net.URLDecoder;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

import java.rmi.RemoteException;

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
import de.cismet.cids.custom.utils.berechtigungspruefung.BerechtigungspruefungDownloadInfo;
import de.cismet.cids.custom.utils.berechtigungspruefung.BerechtigungspruefungHandler;
import de.cismet.cids.custom.utils.berechtigungspruefung.baulastbescheinigung.BerechtigungspruefungBescheinigungDownloadInfo;
import de.cismet.cids.custom.utils.billing.BillingInfo;
import de.cismet.cids.custom.utils.billing.BillingInfoHandler;
import de.cismet.cids.custom.utils.billing.BillingPrice;
import de.cismet.cids.custom.utils.billing.BillingProduct;
import de.cismet.cids.custom.utils.billing.BillingProductGroupAmount;
import de.cismet.cids.custom.wunda_blau.search.server.CidsAlkisSearchStatement;
import de.cismet.cids.custom.wunda_blau.search.server.FormSolutionsBestellungSearch;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.server.search.MetaObjectNodeServerSearch;

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
    public static final int STATUS_WEITERLEITUNG_ABSCHLUSSFORMULAR = 37;
    public static final int STATUS_PRODUKT = 20;
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
    private static final String EXTERNAL_USER_QUERY_TEMPLATE = ""
                + "SELECT %d, %s "
                + "FROM %s "
                + "WHERE name = '%s';";
    private static final String UNFINISHED_BESTELLUNGEN_QUERY_TEMPLATE = ""
                + "SELECT %d, %s "
                + "FROM %s "
                + "WHERE test IS NOT TRUE "
                + "AND duplicate IS NOT TRUE "
                + "AND postweg IS NOT TRUE "
                + "AND fehler IS NULL "
                + "AND erledigt IS NOT TRUE;";
    private static final String PRODUKT_QUERY_TEMPLATE = ""
                + "SELECT DISTINCT %d, %s "
                + "FROM %s WHERE %s "
                + "LIMIT 1;";
    private static final String BESTELLUNG_BY_TRANSID_QUERY_TEMPLATE = ""
                + "SELECT DISTINCT %d, %s "
                + "FROM %s WHERE transid "
                + "LIKE '%s';";

    private static final String PDF_START = "%PDF";
    private static final String PDF_END = "%%EOF";

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum ProductType {

        //~ Enum constants -----------------------------------------------------

        SGK, ABK, BAB_WEITERLEITUNG, BAB_ABSCHLUSS
    }

    //~ Instance fields --------------------------------------------------------

    private final User user;
    private final MetaService metaService;
    private final ConnectionContext connectionContext;
    private final SimpleHttpAccessHandler httpHandler = new SimpleHttpAccessHandler(
            (getProperties().getConnectionTimeout() != null) ? getProperties().getConnectionTimeout() : 0,
            (getProperties().getSoTimeout() != null) ? getProperties().getSoTimeout() : 0);
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
                creds = new UsernamePasswordCredentials(
                        getProperties().getUser(),
                        getProperties().getPassword());
            } catch (final Exception ex) {
                LOG.error(""
                            + "UsernamePasswordCredentials couldn't be created. "
                            + "FormSolutionServerNewStuffAvailableAction will not work at all !",
                    ex);
            }
            try {
                testCismet00Type = parseProductType(getProperties().getTestCismet00());
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
     * @param   downloadinfo    DOCUMENT ME!
     * @param   transid         DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private CidsBean doBilling(final CidsBean bestellungBean,
            final BerechtigungspruefungDownloadInfo downloadinfo,
            final String transid) {
        try {
            final ProductType productType = determineProductType(bestellungBean);

            final CidsBean billingBean = CidsBean.createNewCidsBeanFromTableName(
                    "WUNDA_BLAU",
                    "Billing_Billing",
                    getConnectionContext());

            final boolean isPostweg = Boolean.TRUE.equals(bestellungBean.getProperty("postweg"));
            final Timestamp abrechnungsdatum = (Timestamp)bestellungBean.getProperty("eingang_ts");
            final String projektBezeichnung = ((bestellungBean.getProperty("fk_adresse_rechnung.firma") != null)
                    ? ((String)bestellungBean.getProperty("fk_adresse_rechnung.firma") + ", ") : "")
                        + (String)bestellungBean.getProperty("fk_adresse_rechnung.name") + " "
                        + (String)bestellungBean.getProperty("fk_adresse_rechnung.vorname");
            final String request_url = (String)bestellungBean.getProperty("request_url");

            final String kunde_login;
            switch (productType) {
                case ABK:
                case SGK: {
                    kunde_login = getProperties().getBillingKundeLoginKarte();
                }
                break;
                case BAB_ABSCHLUSS: {
                    kunde_login = getProperties().getBillingKundeLoginBB();
                }
                break;
                default: {
                    // should not be possible
                    kunde_login = null;
                }
            }

            final boolean isGutschein = bestellungBean.getProperty("gutschein_code") != null;
            final String modus = getProperties().getBillingModus();
            final String modusbezeichnung = getProperties().getBillingModusbezeichnung();
            final Double gebuehr = isGutschein
                ? 0d
                : (isPostweg ? (Double)bestellungBean.getProperty("gebuehr_postweg")
                             : (Double)bestellungBean.getProperty("gebuehr"));
            final String verwendungszweck = isPostweg ? getProperties().getBillingVerwendungskeyPostweg()
                                                      : getProperties().getBillingVerwendungszweckDownload();
            final String verwendungskey = isPostweg ? getProperties().getBillingVerwendungskeyPostweg()
                                                    : getProperties().getBillingVerwendungskeyDownload();
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
            billingBean.setProperty("request", (downloadinfo != null) ? MAPPER.writeValueAsString(downloadinfo) : null);
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
                LOG.debug(""
                            + "The metaclass for billing_kunden_logins is null. "
                            + "The current user has probably not the needed rights.");
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
        return fetchEndExecuteAllOpen(true);
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
        Map<String, BerechtigungspruefungDownloadInfo> downloadInfoMap = null;

        switch (startStep) {
            case STATUS_FETCH:
            case STATUS_PARSE:
            case STATUS_SAVE: {
                if (mons == null) {
                    try {
                        if (startStep >= STATUS_CLOSE) {
                            final Map<String, ProductType> typeMap = test ? step0GetTransIdsFromTest()
                                                                          : step0FetchTransIds();
                            final Map<String, Exception> insertExceptionMap = step1CreateMySqlEntries(typeMap.keySet());
                            final Map<String, String> fsXmlMap = step2ExtractXmlParts(typeMap.keySet());
                            fsBestellungMap = step3CreateBestellungMap(
                                    fsXmlMap,
                                    typeMap);
                            fsBeanMap = step4CreateCidsEntries(
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
                step5CloseTransactions(fsBeanMap);
                if (singleStep) {
                    break;
                }
            }
            case STATUS_PRUEFUNG: {
                step6PruefungProdukt(fsBeanMap, fsBestellungMap);
                if (singleStep) {
                    break;
                }
            }
            case STATUS_PRODUKT: {
                downloadInfoMap = step7CreateProducts(fsBeanMap);
                if (singleStep) {
                    break;
                }
            }
            case STATUS_BILLING: {
                step8Billing(fsBeanMap, downloadInfoMap);
                if (singleStep) {
                    break;
                }
            }
            case STATUS_PENDING:
            case STATUS_DONE: {
                step9FinalizeEntries(fsBeanMap);
                if (singleStep) {
                    break;
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
        specialLog("start fetching from DB. Numof objects: " + mons.size());
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
        specialLog("objects fetched from DB");
        return fsBeanMap;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private Map<String, ProductType> step0GetTransIdsFromTest() throws Exception {
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
    private Map<String, ProductType> step0FetchTransIds() throws Exception {
        specialLog("fetched from FS");

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
     * @param  fsBeanMap        DOCUMENT ME!
     * @param  downloadInfoMap  DOCUMENT ME!
     */
    private void step8Billing(final Map<String, CidsBean> fsBeanMap,
            final Map<String, BerechtigungspruefungDownloadInfo> downloadInfoMap) {
        if (fsBeanMap != null) {
            for (final String transid : new ArrayList<>(fsBeanMap.keySet())) {
                final CidsBean bestellungBean = fsBeanMap.get(transid);
                if (bestellungBean != null) {
                    try {
                        if (!Boolean.TRUE.equals(bestellungBean.getProperty("duplicate"))
                                    && (bestellungBean.getProperty("gutschein_code") == null)) {
                            if (bestellungBean.getProperty("fk_billing") == null) {
                                final CidsBean billingBean = doBilling(
                                        bestellungBean,
                                        downloadInfoMap.get(transid),
                                        transid);
                                if (billingBean != null) {
                                    bestellungBean.setProperty("fk_billing", billingBean);
                                    getMetaService().updateMetaObject(
                                        getUser(),
                                        bestellungBean.getMetaObject(),
                                        getConnectionContext());
                                }
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
        specialLog("fetching open transids from FS");

        final Collection<String> transIds = new ArrayList<>();
        try {
            final StringBuilder stringBuilder = new StringBuilder();
            final URL auftragsListeUrl;
            switch (productType) {
                case SGK: {
                    auftragsListeUrl = new URL(getProperties().getUrlAuftragslisteSgkFs());
                }
                break;
                case ABK: {
                    auftragsListeUrl = new URL(getProperties().getUrlAuftragslisteAbkFs());
                }
                break;
                case BAB_WEITERLEITUNG: {
                    auftragsListeUrl = new URL(getProperties().getUrlAuftragslisteBb1Fs());
                }
                break;
                case BAB_ABSCHLUSS: {
                    auftragsListeUrl = new URL(getProperties().getUrlAuftragslisteBb2Fs());
                }
                break;
                default: {
                    throw new Exception("unknown product type");
                }
            }

            try(final InputStream in = getHttpAccessHandler().doRequest(
                                auftragsListeUrl,
                                new StringReader(""),
                                AccessHandler.ACCESS_METHODS.GET_REQUEST,
                                null,
                                creds)) {
                final BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
            }

            specialLog("open transids fetched: " + stringBuilder.toString());

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
    private void step9FinalizeEntries(final Map<String, CidsBean> fsBeanMap) {
        if (fsBeanMap != null) {
            final Collection<String> transids = new ArrayList<>(fsBeanMap.keySet());
            for (final String transid : transids) {
                final CidsBean bestellungBean = fsBeanMap.get(transid);
                if ((bestellungBean != null) && (bestellungBean.getProperty("fehler") == null)) {
                    final ProductType productType = determineProductType(bestellungBean);
                    switch (productType) {
                        case ABK:
                        case SGK:
                        case BAB_ABSCHLUSS: {
                            final int okStatus;
                            final Boolean propPostweg = (Boolean)bestellungBean.getProperty("postweg");
                            if (Boolean.TRUE.equals(propPostweg)) {
                                okStatus = STATUS_PENDING;
                            } else {
                                okStatus = STATUS_DONE;
                            }
                            try {
                                getMySqlHelper().updateStatus(transid, okStatus);
                                doStatusChangedRequest(transid);
                            } catch (final Exception ex) {
                                setErrorStatus(
                                    transid,
                                    10,
                                    bestellungBean,
                                    "Fehler beim Abschließen des MYSQL-Datensatzes",
                                    ex);
                            }
                            final Boolean propDuplicate = (Boolean)bestellungBean.getProperty("duplicate");
                            try {
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
                        break;
                    }
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
            getMySqlHelper().updateStatus(transid, -status);
            doStatusChangedRequest(transid);
        } catch (final Exception ex2) {
            LOG.error("Fehler beim Aktualisieren des MySQL-Datensatzes", ex2);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   transid  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private String getAuftrag(final String transid) throws Exception {
        specialLog("getting auftrag from FS for: " + transid);
        if ((transid != null) && transid.startsWith(TEST_CISMET00_PREFIX)) {
            return (testCismet00Xml != null) ? testCismet00Xml.replace("${TRANSID}", transid) : null;
        } else {
            final Map<String, Object> map;
            try(final InputStream in = getHttpAccessHandler().doRequest(
                                new URL(String.format(getProperties().getUrlAuftragFs(), transid)),
                                new StringReader(""),
                                AccessHandler.ACCESS_METHODS.GET_REQUEST,
                                null,
                                creds)) {
                map = getObjectMapper().readValue(in, new TypeReference<HashMap<String, Object>>() {
                        });
            }

            if (map.get("attachments") != null) {
                final byte[] attachements = DatatypeConverter.parseBase64Binary((String)map.get("attachments"));
                final File attachementsFile = new File(String.format(
                            "%s/%s.zip",
                            getProperties().getAnhangTmpAbsPath(),
                            transid));
                try(final OutputStream out = new FileOutputStream(attachementsFile)) {
                    IOUtils.write(attachements, out);
                }
            }
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

            specialLog("auftrag returned from FS: " + convertedXml);

            return convertedXml;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param  transid  DOCUMENT ME!
     */
    private void doStatusChangedRequest(final String transid) {
        try {
            specialLog("doing status changed request for: " + transid);

            getHttpAccessHandler().doRequest(new URL(
                    String.format(getProperties().getUrlStatusUpdate(), transid)),
                new StringReader(""),
                AccessHandler.ACCESS_METHODS.GET_REQUEST);
        } catch (final Exception ex) {
            LOG.warn("STATUS_UPDATE_URL could not be requested", ex);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   in  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  JAXBException  DOCUMENT ME!
     */
    private FormSolutionsBestellung createFormSolutionsBestellung(final InputStream in) throws JAXBException {
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
        return (FormSolutionsBestellung)jaxbUnmarshaller.unmarshal(in);
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
        final Boolean farbig;
        if ("farbig".equals(farbauspraegung)) {
            farbig = true;
        } else if ("Graustufen".equals(farbauspraegung)) {
            farbig = false;
        } else {
            farbig = null;
        }

        final String massstab = formSolutionsBestellung.getMassstab();
        final StringBuffer produktSB;
        switch (type) {
            case SGK: {
                if (farbig != null) {
                    produktSB = new StringBuffer("Stadtgrundkarte mit kom. Erg.").append(Boolean.TRUE.equals(farbig)
                                ? " (farbig)" : " (sw)");
                } else {
                    return null;
                }
            }
            break;
            case ABK: {
                if (farbig != null) {
                    produktSB = new StringBuffer("Amtliche Basiskarte").append(Boolean.TRUE.equals(farbig) ? " (farbig)"
                                                                                                           : " (sw)");
                } else {
                    return null;
                }
            }
            break;
            case BAB_WEITERLEITUNG:
            case BAB_ABSCHLUSS: {
                return "Baulastenbescheinigung";
            }
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
            case BAB_WEITERLEITUNG: {
                return "BAB_WEITERLEITUNG";
            }
            case BAB_ABSCHLUSS: {
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
        final String ausgewaehlteFlurstuecke = trimedNotEmpty(formSolutionsBestellung.getAusgewaehlteFlurstuecke());
        if (ausgewaehlteFlurstuecke != null) {
            for (final String tmp : ausgewaehlteFlurstuecke.split(",")) {
                fskz.add(tmp);
            }
        } else {
            final String flurstueckskennzeichen = trimedNotEmpty(formSolutionsBestellung.getFlurstueckskennzeichen());
            if (flurstueckskennzeichen != null) {
                for (final String tmp : flurstueckskennzeichen.split(",")) {
                    fskz.add(tmp);
                }
                fskz.add(flurstueckskennzeichen);
            }
            if ("Anschrift".equals(formSolutionsBestellung.getAuswahlUeber())) {
                final String flurstueckskennzeichen1 = trimedNotEmpty(
                        formSolutionsBestellung.getFlurstueckskennzeichen1());
                if (flurstueckskennzeichen1 != null) {
                    for (final String tmp : flurstueckskennzeichen1.split(",")) {
                        fskz.add(tmp);
                    }
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

        // setting Bean properties

        final Integer plz;
        {
            Integer tmpPlz = null;
            if (formSolutionsBestellung.getAsPlz() != null) {
                try {
                    tmpPlz = Integer.parseInt(formSolutionsBestellung.getAsPlz());
                } catch (final Exception ex) {
                    LOG.warn("Exception while parsing PLZ", ex);
                }
            }
            plz = tmpPlz;
        }

        final Integer plz1;
        {
            Integer tmpPlz1 = null;
            if (formSolutionsBestellung.getAsPlz1() != null) {
                try {
                    tmpPlz1 = Integer.parseInt(formSolutionsBestellung.getAsPlz1());
                } catch (final Exception ex) {
                    LOG.warn("Exception while parsing PLZ1", ex);
                }
            }
            plz1 = tmpPlz1;
        }

        final boolean isLieferEqualsRechnungAnschrift = "ja".equalsIgnoreCase(
                formSolutionsBestellung.getDieRechnungsanschriftAuchDieLieferanschrift())
                    || "ja".equalsIgnoreCase(formSolutionsBestellung.getRechnungsanschriftistLieferanschrift());

        final CidsBean bestellungBean = bestellungMc.getEmptyInstance(getConnectionContext()).getBean();
        final CidsBean adresseRechnungBean = adresseMc.getEmptyInstance(getConnectionContext()).getBean();
        final CidsBean adresseVersandBean = (isLieferEqualsRechnungAnschrift)
            ? adresseRechnungBean : adresseMc.getEmptyInstance(getConnectionContext()).getBean();

        // https://github.com/cismet/wupp/issues/1896#issuecomment-603776307
        final boolean isBaulast = ProductType.BAB_ABSCHLUSS.equals(productType)
                    || ProductType.BAB_WEITERLEITUNG.equals(productType);
        final boolean isAdresseAlternativRechnung = formSolutionsBestellung.getAltAdresse() != null;
        final boolean isAdresseAlternativVersand = (formSolutionsBestellung.getAltAdresseAbweichendeLieferanschrift()
                        != null) || (formSolutionsBestellung.getAltAdresse1() != null);
        if (isBaulast && !isAdresseAlternativVersand && !isAdresseAlternativRechnung) {
            adresseVersandBean.setProperty(
                "firma",
                trimedNotEmpty(formSolutionsBestellung.getFirmaAbweichendeLieferanschrift()));
            adresseVersandBean.setProperty("vorname", trimedNotEmpty(formSolutionsBestellung.getAsVorname1()));
            adresseVersandBean.setProperty("name", trimedNotEmpty(formSolutionsBestellung.getAsName1()));
            adresseVersandBean.setProperty("alternativ", null);
            adresseVersandBean.setProperty("strasse", trimedNotEmpty(formSolutionsBestellung.getAsStrasse1()));
            adresseVersandBean.setProperty("hausnummer", trimedNotEmpty(formSolutionsBestellung.getAsHausnummer1()));
            adresseVersandBean.setProperty("plz", plz1);
            adresseVersandBean.setProperty("ort", trimedNotEmpty(formSolutionsBestellung.getAsOrt1()));
            adresseVersandBean.setProperty("staat", trimedNotEmpty(formSolutionsBestellung.getStaat1()));
            //
            adresseRechnungBean.setProperty("firma", trimedNotEmpty(formSolutionsBestellung.getFirma()));
            adresseRechnungBean.setProperty("vorname", trimedNotEmpty(formSolutionsBestellung.getAsVorname()));
            adresseRechnungBean.setProperty("name", trimedNotEmpty(formSolutionsBestellung.getAsName()));
            adresseRechnungBean.setProperty("alternativ", null);
            adresseRechnungBean.setProperty("strasse", trimedNotEmpty(formSolutionsBestellung.getAsStrasse()));
            adresseRechnungBean.setProperty("hausnummer", trimedNotEmpty(formSolutionsBestellung.getAsHausnummer()));
            adresseRechnungBean.setProperty("plz", plz);
            adresseRechnungBean.setProperty("ort", trimedNotEmpty(formSolutionsBestellung.getAsOrt()));
            adresseRechnungBean.setProperty("staat", trimedNotEmpty(formSolutionsBestellung.getStaat()));
        } else if (isBaulast && isAdresseAlternativVersand && !isAdresseAlternativRechnung) {
            adresseVersandBean.setProperty(
                "firma",
                trimedNotEmpty(formSolutionsBestellung.getFirmaAbweichendeLieferanschrift()));
            adresseVersandBean.setProperty("vorname", trimedNotEmpty(formSolutionsBestellung.getAsVorname1()));
            adresseVersandBean.setProperty("name", trimedNotEmpty(formSolutionsBestellung.getAsName1()));
            adresseVersandBean.setProperty(
                "alternativ",
                trimedNotEmpty(formSolutionsBestellung.getAltAdresseAbweichendeLieferanschrift()));
            adresseVersandBean.setProperty("strasse", null);
            adresseVersandBean.setProperty("hausnummer", null);
            adresseVersandBean.setProperty("plz", null);
            adresseVersandBean.setProperty("ort", null);
            adresseVersandBean.setProperty("staat", trimedNotEmpty(formSolutionsBestellung.getStaat1()));
            //
            adresseRechnungBean.setProperty("firma", trimedNotEmpty(formSolutionsBestellung.getFirma()));
            adresseRechnungBean.setProperty("vorname", trimedNotEmpty(formSolutionsBestellung.getAsVorname()));
            adresseRechnungBean.setProperty("name", trimedNotEmpty(formSolutionsBestellung.getAsName()));
            adresseRechnungBean.setProperty("alternativ", null);
            adresseRechnungBean.setProperty("strasse", trimedNotEmpty(formSolutionsBestellung.getAsStrasse()));
            adresseRechnungBean.setProperty("hausnummer", trimedNotEmpty(formSolutionsBestellung.getAsHausnummer()));
            adresseRechnungBean.setProperty("plz", plz);
            adresseRechnungBean.setProperty("ort", trimedNotEmpty(formSolutionsBestellung.getAsOrt()));
            adresseRechnungBean.setProperty("staat", trimedNotEmpty(formSolutionsBestellung.getStaat()));
        } else if (isBaulast && !isAdresseAlternativVersand && isAdresseAlternativRechnung) {
            adresseVersandBean.setProperty(
                "firma",
                trimedNotEmpty(formSolutionsBestellung.getFirmaAbweichendeLieferanschrift()));
            adresseVersandBean.setProperty("vorname", trimedNotEmpty(formSolutionsBestellung.getAsVorname1()));
            adresseVersandBean.setProperty("name", trimedNotEmpty(formSolutionsBestellung.getAsName1()));
            adresseVersandBean.setProperty("alternativ", null);
            adresseVersandBean.setProperty("strasse", trimedNotEmpty(formSolutionsBestellung.getAsStrasse()));
            adresseVersandBean.setProperty("hausnummer", trimedNotEmpty(formSolutionsBestellung.getAsHausnummer()));
            adresseVersandBean.setProperty("plz", plz);
            adresseVersandBean.setProperty("ort", trimedNotEmpty(formSolutionsBestellung.getAsOrt()));
            adresseVersandBean.setProperty("staat", trimedNotEmpty(formSolutionsBestellung.getStaat1()));
            //
            adresseRechnungBean.setProperty("firma", trimedNotEmpty(formSolutionsBestellung.getFirma()));
            adresseRechnungBean.setProperty("vorname", trimedNotEmpty(formSolutionsBestellung.getAsVorname()));
            adresseRechnungBean.setProperty("name", trimedNotEmpty(formSolutionsBestellung.getAsName()));
            adresseRechnungBean.setProperty("alternativ", trimedNotEmpty(formSolutionsBestellung.getAltAdresse()));
            adresseRechnungBean.setProperty("strasse", null);
            adresseRechnungBean.setProperty("hausnummer", null);
            adresseRechnungBean.setProperty("plz", null);
            adresseRechnungBean.setProperty("ort", null);
            adresseRechnungBean.setProperty("staat", trimedNotEmpty(formSolutionsBestellung.getStaat()));
        } else if (isBaulast && isAdresseAlternativVersand && isAdresseAlternativRechnung) {
            adresseVersandBean.setProperty(
                "firma",
                trimedNotEmpty(formSolutionsBestellung.getFirmaAbweichendeLieferanschrift()));
            adresseVersandBean.setProperty("vorname", trimedNotEmpty(formSolutionsBestellung.getAsVorname1()));
            adresseVersandBean.setProperty("name", trimedNotEmpty(formSolutionsBestellung.getAsName1()));
            adresseVersandBean.setProperty(
                "alternativ",
                trimedNotEmpty(formSolutionsBestellung.getAltAdresseAbweichendeLieferanschrift()));
            adresseVersandBean.setProperty("strasse", null);
            adresseVersandBean.setProperty("hausnummer", null);
            adresseVersandBean.setProperty("plz", null);
            adresseVersandBean.setProperty("ort", null);
            adresseVersandBean.setProperty("staat", trimedNotEmpty(formSolutionsBestellung.getStaat1()));
            //
            adresseRechnungBean.setProperty("firma", trimedNotEmpty(formSolutionsBestellung.getFirma()));
            adresseRechnungBean.setProperty("vorname", trimedNotEmpty(formSolutionsBestellung.getAsVorname()));
            adresseRechnungBean.setProperty("name", trimedNotEmpty(formSolutionsBestellung.getAsName()));
            adresseRechnungBean.setProperty("alternativ", trimedNotEmpty(formSolutionsBestellung.getAltAdresse()));
            adresseRechnungBean.setProperty("strasse", null);
            adresseRechnungBean.setProperty("hausnummer", null);
            adresseRechnungBean.setProperty("plz", null);
            adresseRechnungBean.setProperty("ort", null);
            adresseRechnungBean.setProperty("staat", trimedNotEmpty(formSolutionsBestellung.getStaat()));
        } else if (!isBaulast) {
            adresseVersandBean.setProperty("firma", trimedNotEmpty(formSolutionsBestellung.getFirma1()));
            adresseVersandBean.setProperty("vorname", trimedNotEmpty(formSolutionsBestellung.getAsVorname1()));
            adresseVersandBean.setProperty("name", trimedNotEmpty(formSolutionsBestellung.getAsName1()));
            adresseVersandBean.setProperty("alternativ", trimedNotEmpty(formSolutionsBestellung.getAltAdresse1()));
            adresseVersandBean.setProperty("strasse", trimedNotEmpty(formSolutionsBestellung.getAsStrasse1()));
            adresseVersandBean.setProperty("hausnummer", trimedNotEmpty(formSolutionsBestellung.getAsHausnummer1()));
            adresseVersandBean.setProperty("plz", plz1);
            adresseVersandBean.setProperty("ort", trimedNotEmpty(formSolutionsBestellung.getAsOrt1()));
            adresseVersandBean.setProperty("staat", trimedNotEmpty(formSolutionsBestellung.getStaat1()));
            //
            adresseRechnungBean.setProperty("firma", trimedNotEmpty(formSolutionsBestellung.getFirma()));
            adresseRechnungBean.setProperty("vorname", trimedNotEmpty(formSolutionsBestellung.getAsVorname()));
            adresseRechnungBean.setProperty("name", trimedNotEmpty(formSolutionsBestellung.getAsName()));
            adresseRechnungBean.setProperty("alternativ", trimedNotEmpty(formSolutionsBestellung.getAltAdresse()));
            adresseRechnungBean.setProperty("strasse", trimedNotEmpty(formSolutionsBestellung.getAsStrasse()));
            adresseRechnungBean.setProperty("hausnummer", trimedNotEmpty(formSolutionsBestellung.getAsHausnummer()));
            adresseRechnungBean.setProperty("plz", plz);
            adresseRechnungBean.setProperty("ort", trimedNotEmpty(formSolutionsBestellung.getAsOrt()));
            adresseRechnungBean.setProperty("staat", trimedNotEmpty(formSolutionsBestellung.getStaat()));
        }

        final String transid = formSolutionsBestellung.getTransId();

        final CidsBean nachfolgerVonBean;
        if (ProductType.BAB_ABSCHLUSS.equals(productType)) {
            final String fileUrl = (formSolutionsBestellung.getFileUrl() != null)
                ? URLDecoder.decode(formSolutionsBestellung.getFileUrl(), "UTF-8") : null;
            if (fileUrl != null) {
                nachfolgerVonBean = searchBestellungByRequestUrl(fileUrl);
            } else {
                nachfolgerVonBean = null;
            }
        } else {
            nachfolgerVonBean = null;
        }

        final Integer massstab =
            ((formSolutionsBestellung.getMassstab() != null) && formSolutionsBestellung.getMassstab().contains(":"))
            ? Integer.parseInt(formSolutionsBestellung.getMassstab().split(":")[1]) : null;

        final String landparcelcode = (nachfolgerVonBean != null)
            ? (String)nachfolgerVonBean.getProperty("landparcelcode") : extractLandparcelcode(formSolutionsBestellung);
        final boolean isGutschein = ((formSolutionsBestellung.getGutschein() != null)
                        && (GUTSCHEIN_YES == Integer.parseInt(formSolutionsBestellung.getGutschein())));
        final String gutscheinCode = isGutschein ? formSolutionsBestellung.getGutscheinCode() : null;
        final boolean isTest = ((gutscheinCode != null) && gutscheinCode.startsWith("T"))
                    || ((transid != null) && transid.startsWith(TEST_CISMET00_PREFIX));

        final Double gebuehr;
        if (isGutschein) {
            gebuehr = 0d;
        } else {
            Double tmpGebuehr = null;
            try {
                tmpGebuehr = Double.parseDouble(formSolutionsBestellung.getBetrag().replaceAll(",", "."));
            } catch (final Exception ex) {
                LOG.warn("Exception while parsing Gebuehr", ex);
            }
            gebuehr = tmpGebuehr;
        }

        final Boolean postweg;
        if ("Kartenausdruck".equals(formSolutionsBestellung.getBezugsweg())) {      // Karten
            postweg = Boolean.TRUE;
        } else if ("Post".equals(formSolutionsBestellung.getBezugsweg())) {
            postweg = Boolean.TRUE;
        } else if ("PDF-Download".equals(formSolutionsBestellung.getBezugsweg())) { // Baulasten
            postweg = Boolean.FALSE;
        } else {
            postweg = null;
        }

        final CidsBean produktBean = getProduktBean(formSolutionsBestellung, productType);

        bestellungBean.setProperty("postweg", postweg);
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
        bestellungBean.setProperty("nachfolger_von", nachfolgerVonBean);

        return bestellungBean;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   productKey    DOCUMENT ME!
     * @param   usagekey      DOCUMENT ME!
     * @param   downloadInfo  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private double calculateBabGebuehr(final String productKey,
            final String usagekey,
            final BerechtigungspruefungBescheinigungDownloadInfo downloadInfo) {
        final BillingProduct product = billingInfoHander.getProducts().get(productKey);
        final Collection<BillingProductGroupAmount> prodAmounts = new ArrayList<>();
        for (final HashMap.Entry<String, Integer> amount : downloadInfo.getAmounts().entrySet()) {
            prodAmounts.add(new BillingProductGroupAmount(amount.getKey(), amount.getValue()));
        }
        final double raw = BillingInfoHandler.calculateRawPrice(
                product,
                prodAmounts.toArray(new BillingProductGroupAmount[0]));
        final BillingPrice price = new BillingPrice(raw, usagekey, product);
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
     * @param   search  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private Collection<CidsBean> executeSearch(final MetaObjectNodeServerSearch search) throws Exception {
        final Map localServers = new HashMap<>();
        localServers.put("WUNDA_BLAU", getMetaService());
        search.setActiveLocalServers(localServers);
        search.setUser(getUser());
        final Collection<MetaObjectNode> mons = search.performServerSearch();
        final Collection<CidsBean> cidsBeans = new ArrayList<>();
        if (mons != null) {
            for (final MetaObjectNode mon : mons) {
                final CidsBean cidsBean = getMetaService().getMetaObject(
                            getUser(),
                            mon.getObjectId(),
                            mon.getClassId(),
                            getConnectionContext())
                            .getBean();
                cidsBeans.add(cidsBean);
            }
            return cidsBeans;
        } else {
            return null;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   search  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private CidsBean executeSingleResultSearch(final MetaObjectNodeServerSearch search) throws Exception {
        final Collection<CidsBean> cidsBeans = executeSearch(search);
        if ((cidsBeans != null) && !cidsBeans.isEmpty()) {
            return cidsBeans.iterator().next();
        } else {
            return null;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   requestUrl  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private CidsBean searchBestellungByRequestUrl(final String requestUrl) throws Exception {
        final FormSolutionsBestellungSearch search = new FormSolutionsBestellungSearch();
        search.setRequestUrl(requestUrl);
        return executeSingleResultSearch(search);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   flurstueckKennzeichen  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private CidsBean getFlurstueck(final String flurstueckKennzeichen) throws Exception {
        final CidsAlkisSearchStatement search = new CidsAlkisSearchStatement(
                CidsAlkisSearchStatement.Resulttyp.FLURSTUECK,
                CidsAlkisSearchStatement.SucheUeber.FLURSTUECKSNUMMER,
                flurstueckKennzeichen,
                null);
        return executeSingleResultSearch(search);
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
        if ((bestellungBean != null)
                    && !Boolean.TRUE.equals(bestellungBean.getProperty("duplicate"))
                    && (bestellungBean.getProperty("fehler") == null)) {
            final String code = (String)bestellungBean.getProperty("fk_produkt.fk_typ.key");
            final String dinFormat = (String)bestellungBean.getProperty("fk_produkt.fk_format.format");
            final Integer scale = (Integer)bestellungBean.getProperty("massstab");

            final AlkisProductDescription productDesc = getAlkisProductDescription(code, dinFormat, scale);
            final String flurstueckKennzeichen = ((String)bestellungBean.getProperty("landparcelcode")).split(",")[0];

            final String transid = (String)bestellungBean.getProperty("transid");

            final Geometry geom = (Geometry)bestellungBean.getProperty("geometrie.geo_field");
            final Point center = geom.getEnvelope().getCentroid();

            final boolean isGutschein = bestellungBean.getProperty("gutschein_code") != null;

            final String gutscheincodeAdditionalText = isGutschein
                ? String.format(GUTSCHEIN_ADDITIONAL_TEXT, bestellungBean.getProperty("gutschein_code")) : null;

            final URL url = ServerAlkisProducts.getInstance()
                        .productKarteUrl(
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
        } else {
            return null;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   in        DOCUMENT ME!
     * @param   fileName  DOCUMENT ME!
     * @param   testPdf   DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private void uploadProduktToFtp(final InputStream in, final String fileName, final boolean testPdf)
            throws Exception {
        final File tmpFile = writeProduktToFile(
                in,
                getProperties().getTmpBrokenpdfsAbsPath()
                        + DomainServerImpl.getServerProperties().getFileSeparator()
                        + fileName);

        if (testPdf) {
            // test requested Produkt
            try(final InputStream Test = new FileInputStream(tmpFile)) {
                testPdfValidity(Test);
            }
        }

        // upload Produkt to FTP
        final String ftpFilePath = getProperties().getProduktBasepath() + "/" + fileName;
        getFtpClient().upload(new FileInputStream(tmpFile), ftpFilePath);

        if (testPdf) {
            // Download Produkt from FTP and test it
            try(final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                getFtpClient().download(ftpFilePath, out);
                try(final InputStream inTest = new ByteArrayInputStream(out.toByteArray())) {
                    testPdfValidity(inTest);
                }
            }
        }

        // no errors until here => tmpFile can now be deleted
        if (!getProperties().isDeleteTmpProductAfterSuccessfulUploadDisabled() && (tmpFile != null)) {
            tmpFile.delete();
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   filePath  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static String ensureCorrectDirectorySeparator(final String filePath) {
        final String s = DomainServerImpl.getServerProperties().getFileSeparator();
        return "/".equals(s) ? filePath : filePath.replace("/", s);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   in        DOCUMENT ME!
     * @param   fileName  DOCUMENT ME!
     * @param   testPdf   DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private void saveProduktInFtpMount(final InputStream in, final String fileName, final boolean testPdf)
            throws Exception {
        final File mntFile = writeProduktToFile(
                in,
                getProperties().getFtpMountAbsPath()
                        + ensureCorrectDirectorySeparator("/" + fileName));

        if (testPdf) {
            // test requested Produkt
            try(final InputStream Test = new FileInputStream(mntFile)) {
                testPdfValidity(Test);
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   in        DOCUMENT ME!
     * @param   fileName  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private File writeProduktToFile(final InputStream in, final String fileName) throws Exception {
        final File tmpFile = new File(DomainServerImpl.getServerProperties().getFileSeparator() + fileName);
        try(final OutputStream out = new FileOutputStream(tmpFile)) {
            IOUtils.copy(in, out);
        }
        return tmpFile;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   in  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private static void testPdfValidity(final InputStream in) throws Exception {
        try(final InputStreamReader ir = new InputStreamReader(in);
                    final BufferedReader rd = new BufferedReader(ir)) {
            String firstLine = null;
            String lastLine = null;
            {
                String line;
                while ((line = rd.readLine()) != null) {
                    if (firstLine == null) {
                        firstLine = line;
                    }
                    lastLine = line;
                }
            }

            if (firstLine == null) {
                throw new Exception("PDF broken: first line is null");
            }
            if (!firstLine.startsWith(PDF_START)) {
                throw new Exception("PDF broken: first line doesn't start with " + PDF_START);
            }
            if (!PDF_END.equals(lastLine)) {
                throw new Exception("PDF broken: last line equals " + PDF_END);
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
    private Map<String, String> step2ExtractXmlParts(final Collection<String> transids) {
        specialLog("extracting xml parts for num of objects: " + transids.size());

        final Map<String, String> fsXmlMap = new HashMap<>(transids.size());

        for (final String transid : transids) {
            try {
                final String auftragXml = getAuftrag(transid);
                fsXmlMap.put(transid, auftragXml);
                getMySqlHelper().updateStatus(transid, STATUS_FETCH);
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
    private Map<String, FormSolutionsBestellung> step3CreateBestellungMap(final Map<String, String> fsXmlMap,
            final Map<String, ProductType> typeMap) {
        final Collection<String> transids = new ArrayList<>(fsXmlMap.keySet());
        specialLog("creating simple bestellung bean for num of objects: " + transids.size());

        final Map<String, FormSolutionsBestellung> fsBestellungMap = new HashMap<>(
                transids.size());
        for (final String transid : transids) {
            final String auftragXml = fsXmlMap.get(transid);
            try(final InputStream in = IOUtils.toInputStream(auftragXml, "UTF-8")) {
                specialLog("creating simple bestellung bean for: " + transid);

                final FormSolutionsBestellung formSolutionsBestellung = createFormSolutionsBestellung(in);
                fsBestellungMap.put(transid, formSolutionsBestellung);

                specialLog("simple bestellung bean created for: " + transids.size());

                final boolean downloadOnly = !"Kartenausdruck".equals(formSolutionsBestellung.getBezugsweg());
                final String email = downloadOnly ? trimedNotEmpty(formSolutionsBestellung.getEMailadresse())
                                                  : trimedNotEmpty(formSolutionsBestellung.getEMailadresse()); // 1

                specialLog("updating mysql email entry for: " + transid);

                getMySqlHelper().updateRequest(
                    transid,
                    STATUS_PARSE,
                    extractLandparcelcode(formSolutionsBestellung),
                    extractProduct(formSolutionsBestellung, typeMap.get(transid)),
                    downloadOnly,
                    email);
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
    private Map<String, CidsBean> step4CreateCidsEntries(final Map<String, String> fsXmlMap,
            final Map<String, FormSolutionsBestellung> fsBestellungMap,
            final Map<String, ProductType> typeMap,
            final Map<String, Exception> insertExceptionMap) {
        // nur die transids bearbeiten, bei denen das Parsen auch geklappt hat
        final Collection<String> transids = new ArrayList<>(fsBestellungMap.keySet());

        specialLog("creating cids entries for num of objects: " + transids.size());

        final Map<String, CidsBean> fsBeanMap = new HashMap<>(transids.size());
        for (final String transid : transids) {
            specialLog("creating cids entry for: " + transid);

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
                specialLog(message);
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
                    final MetaClass geomMc = getMetaClass("geom", getConnectionContext());
                    final CidsBean geomBean = geomMc.getEmptyInstance(getConnectionContext()).getBean();
                    Geometry geom = null;
                    if (bestellungBean.getProperty("landparcelcode") != null) {
                        final String[] landparcelcodes = ((String)bestellungBean.getProperty("landparcelcode")).split(
                                ",");
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
                        ex,
                        false);
                }

                specialLog("persisting cids entry for: " + transid);

                final MetaObject persistedMo = getMetaService().insertMetaObject(
                        getUser(),
                        bestellungBean.getMetaObject(),
                        getConnectionContext());

                final CidsBean persistedBestellungBean = persistedMo.getBean();
                fsBeanMap.put(transid, persistedBestellungBean);

                getMySqlHelper().updateStatus(transid, STATUS_SAVE);
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
        } else if (ProductType.BAB_WEITERLEITUNG.toString().equals(string)) {
            return ProductType.BAB_WEITERLEITUNG;
        } else if (ProductType.BAB_ABSCHLUSS.toString().equals(string)) {
            return ProductType.BAB_ABSCHLUSS;
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
    private Map<String, Exception> step1CreateMySqlEntries(final Collection<String> transids) {
        final Map<String, Exception> insertExceptionMap = new HashMap<>(transids.size());

        for (final String transid : transids) {
            try {
                specialLog("updating or inserting mySQL entry for: " + transid);
                getMySqlHelper().insertOrUpdateStatus(transid, STATUS_CREATE);
                doStatusChangedRequest(transid);
            } catch (final Exception ex) {
                LOG.error("Fehler beim Erzeugen/Aktualisieren des MySQL-Datensatzes.", ex);
                insertExceptionMap.put(transid, ex);
            }
        }

        return insertExceptionMap;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  fsBeanMap  DOCUMENT ME!
     */
    private void step5CloseTransactions(final Map<String, CidsBean> fsBeanMap) {
        if (fsBeanMap != null) {
            final Collection<String> transids = new ArrayList<>(fsBeanMap.keySet());
            specialLog("closing transactions for num of objects: " + transids.size());

            for (final String transid : transids) {
                final CidsBean bestellungBean = fsBeanMap.get(transid);
                if ((bestellungBean != null)) {
                    try {
                        specialLog("closing transaction for: " + transid);
                        final boolean closeVeto = (transid == null) || transid.startsWith(TEST_CISMET00_PREFIX)
                                    || DomainServerImpl.getServerInstance()
                                    .hasConfigAttr(getUser(), "custom.formsolutions.noclose", getConnectionContext());
                        if (!closeVeto) {
                            getHttpAccessHandler().doRequest(
                                new URL(String.format(getProperties().getUrlAuftragDeleteFs(), transid)),
                                new StringReader(""),
                                AccessHandler.ACCESS_METHODS.POST_REQUEST,
                                null,
                                creds);
                        }

                        getMySqlHelper().updateStatus(transid, STATUS_CLOSE);
                        doStatusChangedRequest(transid);
                    } catch (final Exception ex) {
                        setErrorStatus(
                            transid,
                            STATUS_CLOSE,
                            bestellungBean,
                            "Fehler beim Schließen der Transaktion.",
                            ex);
                        break;
                    }
                }
            }
        }
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
     * @param   productUrl  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private InputStream downloadProduct(final URL productUrl) throws Exception {
        return getHttpAccessHandler().doRequest(
                productUrl,
                new StringReader(""),
                AccessHandler.ACCESS_METHODS.GET_REQUEST,
                null,
                creds);
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
    private InputStream createRechnung(final CidsBean bestellungBean) throws Exception {
        final Map parameters = new HashMap();
        final ProductType productType = determineProductType(bestellungBean);

        final String landparcelcodesString = noNullAndTrimed((String)bestellungBean.getProperty("landparcelcode"));
        final String landparcelcode;
        if ((landparcelcodesString != null) && !landparcelcodesString.isEmpty()) {
            final String[] landparcelcodes = landparcelcodesString.split(",");
            landparcelcode = landparcelcodes[0] + ((landparcelcodes.length > 1) ? " u.a." : "");
        } else {
            landparcelcode = null;
        }

        parameters.put("DATUM_HEUTE", new SimpleDateFormat("dd.MM.yyyy").format(new Date()));
        final String datumEingang = (bestellungBean.getProperty("eingang_ts") != null)
            ? new SimpleDateFormat("dd.MM.yyyy").format(bestellungBean.getProperty("eingang_ts")) : "";
        parameters.put("DATUM_EINGANG", noNullAndTrimed(datumEingang));
        parameters.put("FLURSTUECKSKENNZEICHEN", landparcelcode);
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

        final boolean isPostweg = Boolean.TRUE.equals(bestellungBean.getProperty("postweg"));
        final boolean isGutschein = bestellungBean.getProperty("gutschein_code") != null;

        final Double gebuehr = (isGutschein
                ? 0d
                : (isPostweg ? (Double)bestellungBean.getProperty("gebuehr_postweg")
                             : (Double)bestellungBean.getProperty("gebuehr")));

        final float gebuehrFloat = (gebuehr != null) ? gebuehr.floatValue() : 0f;

        parameters.put("RECHNUNG_GES_BETRAG", gebuehrFloat);
        parameters.put("RECHNUNG_EINZELPREIS", gebuehrFloat);
        parameters.put("RECHNUNG_GESAMMTPREIS", gebuehrFloat);
        parameters.put(
            "RECHNUNG_BERECH_GRUNDLAGE",
            ProductType.BAB_ABSCHLUSS.equals(productType) ? getProperties().getRechnungBerechnugsgGrundlageBaulasten()
                                                          : getProperties().getRechnungBerechnugsgGrundlageKarte());
        parameters.put(
            "RECHNUNG_AUFTRAGSART",
            ProductType.BAB_ABSCHLUSS.equals(productType) ? getProperties().getRechnungAuftragsartBaulasten()
                                                          : getProperties().getRechnungAuftragsartKarte());
        parameters.put("RECHNUNG_ANZAHL", 1);
        parameters.put("RECHNUNG_RABATT", 0.0f);
        parameters.put("RECHNUNG_UST", 0.0f);
        parameters.put("RECHNUNG_GUTSCHEINCODE", gutscheinCode);
        parameters.put("SUBREPORT_DIR", DomainServerImpl.getServerProperties().getServerResourcesBasePath() + "/");
        final JRDataSource dataSource = new JRBeanCollectionDataSource(Arrays.asList(bestellungBean));

        final JasperReport rechnungJasperReport = ServerResourcesLoader.getInstance()
                    .loadJasperReport(WundaBlauServerResources.FS_RECHNUNG_JASPER.getValue());
        final JasperPrint print = JasperFillManager.fillReport(rechnungJasperReport, parameters, dataSource);

        try(final ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            JasperExportManager.exportReportToPdfStream(print, os);
            final byte[] bytes = os.toByteArray();
            return new ByteArrayInputStream(bytes);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param  fsBeanMap        DOCUMENT ME!
     * @param  fsBestellungMap  DOCUMENT ME!
     */
    private void step6PruefungProdukt(final Map<String, CidsBean> fsBeanMap,
            final Map<String, FormSolutionsBestellung> fsBestellungMap) {
        final Collection<String> transids = new ArrayList<>(fsBestellungMap.keySet());

        for (final String transid : transids) {
            final CidsBean bestellungBean = fsBeanMap.get(transid);
            if ((bestellungBean != null) && (bestellungBean.getProperty("fehler") == null)) {
                try {
                    final ProductType productType = determineProductType(bestellungBean);
                    switch (productType) {
                        case BAB_WEITERLEITUNG: {
                            final String flurstueckKennzeichen = ((String)bestellungBean.getProperty("landparcelcode"));
                            final String produktbezeichnung = transid;
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
                                    null,
                                    produktbezeichnung,
                                    null,
                                    flurstuecke,
                                    protocolBuffer,
                                    statusHolder);

                            final String verwendungskeyDownload = getProperties().getBillingVerwendungskeyDownload();
                            final String verwendungskeyPostweg = getProperties().getBillingVerwendungskeyPostweg();
                            final String productKeyDownload = getProperties().getBillingProduktkeyBBDownload();
                            final String productKeyPostweg = getProperties().getBillingProduktkeyBBPostweg();

                            final boolean isGutschein = bestellungBean.getProperty("gutschein_code") != null;
                            final double gebuehr = isGutschein
                                ? 0d : calculateBabGebuehr(productKeyDownload, verwendungskeyDownload, downloadInfo);
                            final Double gebuehrPostweg = isGutschein
                                ? 0d : calculateBabGebuehr(productKeyPostweg, verwendungskeyPostweg, downloadInfo);

                            bestellungBean.setProperty("gebuehr", gebuehr);
                            bestellungBean.setProperty("gebuehr_postweg", gebuehrPostweg);

                            final FormSolutionsBestellung formSolutionBestellung = fsBestellungMap.get(transid);

                            final File attachementsFile = new File(String.format(
                                        "%s/%s.zip",
                                        getProperties().getAnhangTmpAbsPath(),
                                        transid));
                            final String dateiName;
                            final byte[] data;
                            if (attachementsFile.exists()) {
                                try(final InputStream in = new FileInputStream(attachementsFile)) {
                                    dateiName = attachementsFile.getName();
                                    data = IOUtils.toByteArray(in);
                                }
                            } else {
                                dateiName = null;
                                data = null;
                            }

                            final String schluessel = BerechtigungspruefungHandler.getInstance()
                                        .createNewSchluessel(getUser(), downloadInfo);
                            downloadInfo.setAuftragsnummer(schluessel);
                            final CidsBean pruefung = BerechtigungspruefungHandler.getInstance()
                                        .addNewAnfrage(
                                            getUser(),
                                            schluessel,
                                            downloadInfo,
                                            formSolutionBestellung.getBerechtigungsgrund(),
                                            formSolutionBestellung.getBegruendungstext(),
                                            dateiName,
                                            data);

                            if (attachementsFile.exists()) {
                                attachementsFile.delete();
                            }

                            bestellungBean.setProperty("berechtigungspruefung", pruefung);
                            getMetaService().updateMetaObject(
                                getUser(),
                                bestellungBean.getMetaObject(),
                                getConnectionContext());

                            getMySqlHelper().updatePruefungFreigabe(schluessel, transid, STATUS_PRUEFUNG, null);
                        }
                    }
                } catch (final Exception ex) {
                    setErrorStatus(
                        transid,
                        STATUS_PRUEFUNG
                                - 1,
                        bestellungBean,
                        "Fehler beim Vorlage der Bestellung zur Prüfung.",
                        ex);
                }
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   transid  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static String createTransidHash(final String transid) {
        return DigestUtils.md5Hex(getProperties().getTransidHashpepper() + transid);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   fileNameOrig    DOCUMENT ME!
     * @param   in              DOCUMENT ME!
     * @param   bestellungBean  DOCUMENT ME!
     * @param   testPdf         DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private void uploadAndFillProduktFields(final String fileNameOrig,
            final InputStream in,
            final CidsBean bestellungBean,
            final boolean testPdf) throws Exception {
        final String transid = (String)bestellungBean.getProperty("transid");

        final String fileNameFtp = transid + "." + FilenameUtils.getExtension(fileNameOrig);
        if (getProperties().isFtpEnabled()) {
            uploadProduktToFtp(in, fileNameFtp, testPdf);
        } else {
            saveProduktInFtpMount(in, fileNameFtp, testPdf);
        }

        bestellungBean.setProperty("produkt_dateipfad", fileNameFtp);
        bestellungBean.setProperty("produkt_dateiname_orig", fileNameOrig);
        bestellungBean.setProperty("produkt_ts", new Timestamp(new Date().getTime()));

        createUploadAndFillRechnungFields(bestellungBean);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   bestellungBean  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private void createUploadAndFillRechnungFields(final CidsBean bestellungBean) throws Exception {
        final String transid = (String)bestellungBean.getProperty("transid");
        final String fileNameRechnung = "RE_" + transid + ".pdf";

        bestellungBean.setProperty("rechnung_dateipfad", fileNameRechnung);
        bestellungBean.setProperty("rechnung_dateiname_orig", "Rechnung - Produktbestellung " + transid + ".pdf");

        try(final InputStream in = createRechnung(bestellungBean)) {
            if (getProperties().isFtpEnabled()) {
                uploadProduktToFtp(in, fileNameRechnung, true);
            } else {
                saveProduktInFtpMount(in, fileNameRechnung, true);
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
    private Map<String, BerechtigungspruefungDownloadInfo> step7CreateProducts(final Map<String, CidsBean> fsBeanMap) {
        final Map<String, BerechtigungspruefungDownloadInfo> downloadInfoMap = new HashMap<>();
        if (fsBeanMap != null) {
            final Collection<String> transids = new ArrayList<>(fsBeanMap.keySet());
            for (final String transid : transids) {
                final CidsBean bestellungBean = fsBeanMap.get(transid);
                if ((bestellungBean != null)) {
                    try {
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

                        final ProductType productType = determineProductType(bestellungBean);
                        switch (productType) {
                            case SGK:
                            case ABK: {
                                final URL productUrl = createProductUrl(bestellungBean);
                                bestellungBean.setProperty("request_url", productUrl.toString());

                                final String fileNameOrig = String.format(
                                        "%s.%s.pdf",
                                        (String)bestellungBean.getProperty("fk_produkt.fk_typ.key"),
                                        ((String)bestellungBean.getProperty("landparcelcode")).split(",")[0].replace(
                                            "/",
                                            "--"));

                                try(final InputStream in = downloadProduct(productUrl)) {
                                    uploadAndFillProduktFields(fileNameOrig, in, bestellungBean, true);
                                }

                                getMetaService().updateMetaObject(
                                    getUser(),
                                    bestellungBean.getMetaObject(),
                                    getConnectionContext());
                                getMySqlHelper().insertOrUpdateProduct(
                                    transid,
                                    STATUS_PRODUKT,
                                    (String)bestellungBean.getProperty("landparcelcode"),
                                    (String)bestellungBean.getProperty("fk_product.fk_typ.name"),
                                    (Boolean)bestellungBean.getProperty("postweg"),
                                    (String)bestellungBean.getProperty("email"),
                                    null,
                                    (String)bestellungBean.getProperty("produkt_dateipfad"),
                                    (String)bestellungBean.getProperty("produkt_dateiname_orig"));
                                doStatusChangedRequest(transid);
                            }
                            break;
                            case BAB_WEITERLEITUNG: {
                                final CidsBean berechtigungspruefung = (CidsBean)bestellungBean.getProperty(
                                        "berechtigungspruefung");
                                if (berechtigungspruefung != null) {
                                    if (Boolean.TRUE.equals(berechtigungspruefung.getProperty("pruefstatus"))) {
                                        final String transidHash = createTransidHash(transid);
                                        final String redirectorUrlTemplate = getProperties()
                                                    .getCidsActionHttpRedirectorUrl();
                                        final String redirect2formsolutions = String.format(
                                                redirectorUrlTemplate,
                                                transidHash);

                                        bestellungBean.setProperty("request_url", new URL(redirect2formsolutions));
                                        bestellungBean.setProperty("produkt_ts", new Timestamp(new Date().getTime()));
                                        getMetaService().updateMetaObject(
                                            getUser(),
                                            bestellungBean.getMetaObject(),
                                            getConnectionContext());

                                        LOG.info(redirect2formsolutions);
                                        getMySqlHelper().updatePruefungFreigabe((String)
                                            berechtigungspruefung.getProperty("schluessel"),
                                            transid,
                                            STATUS_WEITERLEITUNG_ABSCHLUSSFORMULAR,
                                            redirect2formsolutions);
                                        doStatusChangedRequest(transid);

                                        try {
                                            berechtigungspruefung.setProperty("abgeholt", true);
                                            getMetaService().updateMetaObject(
                                                getUser(),
                                                berechtigungspruefung.getMetaObject(),
                                                getConnectionContext());
                                        } catch (final Exception ex) {
                                            LOG.error(ex, ex);
                                        }
                                    } else if (Boolean.FALSE.equals(berechtigungspruefung.getProperty("pruefstatus"))) {
                                        getMySqlHelper().updatePruefungAblehnung((String)
                                            berechtigungspruefung.getProperty("schluessel"),
                                            transid,
                                            -STATUS_PRUEFUNG,
                                            (String)berechtigungspruefung.getProperty("pruefkommentar"));
                                        doStatusChangedRequest(transid);
                                    }
                                }
                            }
                            break;
                            case BAB_ABSCHLUSS: {
                                final CidsBean vorgaengerBestellungBean = (CidsBean)bestellungBean.getProperty(
                                        "nachfolger_von");
                                if (vorgaengerBestellungBean != null) {
                                    final CidsBean berechtigungspruefung = (CidsBean)
                                        vorgaengerBestellungBean.getProperty(
                                            "berechtigungspruefung");
                                    final String downloadinfoJson = (String)berechtigungspruefung.getProperty(
                                            "downloadinfo_json");
                                    bestellungBean.setProperty(
                                        "berechtigungspruefung",
                                        vorgaengerBestellungBean.getProperty("berechtigungspruefung"));
                                    getMetaService().updateMetaObject(
                                        getUser(),
                                        bestellungBean.getMetaObject(),
                                        getConnectionContext());

                                    final String fileNameOrig = "baulastbescheinigung.zip";
                                    final File tmpFile = new File(String.format(
                                                "%s/%s.%s",
                                                getProperties().getProduktTmpAbsPath(),
                                                transid,
                                                FilenameUtils.getExtension(fileNameOrig)));

                                    final BerechtigungspruefungBescheinigungDownloadInfo downloadInfo =
                                        new ObjectMapper().readValue(
                                            downloadinfoJson,
                                            BerechtigungspruefungBescheinigungDownloadInfo.class);
                                    getBaulastBescheinigungHelper().writeFullBescheinigung(downloadInfo, tmpFile);
                                    try(final InputStream in = new FileInputStream(tmpFile)) {
                                        uploadAndFillProduktFields(fileNameOrig, in, bestellungBean, false);
                                    }
                                    if (!getProperties().isDeleteTmpProductAfterSuccessfulUploadDisabled()) {
                                        tmpFile.delete();
                                    }
                                    downloadInfoMap.put(transid, downloadInfo);

                                    getMySqlHelper().insertOrUpdateProduct(
                                        transid,
                                        STATUS_PRODUKT,
                                        (String)bestellungBean.getProperty("landparcelcode"),
                                        (String)bestellungBean.getProperty("fk_product.fk_typ.name"),
                                        (Boolean)bestellungBean.getProperty("postweg"),
                                        (String)bestellungBean.getProperty("email"),
                                        (String)berechtigungspruefung.getProperty("schluessel"),
                                        (String)bestellungBean.getProperty("produkt_dateipfad"),
                                        (String)bestellungBean.getProperty("produkt_dateiname_orig"));
                                    doStatusChangedRequest(transid);
                                } else {
                                    throw new Exception("Daten des vorgelagerten Formulars wurden nicht gefunden.");
                                }
                            }
                            break;
                        }
                    } catch (final Exception ex) {
                        setErrorStatus(
                            transid,
                            STATUS_PRODUKT,
                            bestellungBean,
                            "Fehler beim Erzeugen des Produktes",
                            ex);
                    }
                }
            }
        }
        return downloadInfoMap;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   bestellungBean  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private ProductType determineProductType(final CidsBean bestellungBean) {
        final String type = ((bestellungBean != null) && (bestellungBean.getProperty("fk_produkt.fk_typ.key") != null))
            ? (String)bestellungBean.getProperty("fk_produkt.fk_typ.key") : null;
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
                case "BAB_WEITERLEITUNG": {
                    return ProductType.BAB_WEITERLEITUNG;
                }
                case "BAB": {
                    return ProductType.BAB_ABSCHLUSS;
                }
            }
        }
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  message  DOCUMENT ME!
     */
    private void specialLog(final String message) {
        FormSolutionBestellungSpecialLogger.getInstance().log(message);
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
     * @return  DOCUMENT ME!
     */
    private static FormSolutionsProperties getProperties() {
        return FormSolutionsProperties.getInstance();
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static FormSolutionsMySqlHelper getMySqlHelper() {
        return FormSolutionsMySqlHelper.getInstance();
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static FormSolutionsFtpClient getFtpClient() {
        return FormSolutionsFtpClient.getInstance();
    }
}
