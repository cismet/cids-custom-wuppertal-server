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

import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.io.IOUtils;

import org.openide.util.Exceptions;
import org.openide.util.Lookup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;

import java.net.URL;

import java.rmi.Remote;
import java.rmi.RemoteException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;

import de.cismet.cids.custom.utils.alkis.AlkisProductDescription;
import de.cismet.cids.custom.utils.alkis.AlkisProducts;
import de.cismet.cids.custom.utils.formsolutions.FormSolutionsBestellung;
import de.cismet.cids.custom.utils.formsolutions.FormSolutionsConstants;
import de.cismet.cids.custom.utils.formsolutions.FormSolutionsMySqlHelper;
import de.cismet.cids.custom.wunda_blau.search.server.CidsAlkisSearchStatement;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;
import de.cismet.cids.server.actions.UserAwareServerAction;

import de.cismet.cids.utils.MetaClassCacheService;

import de.cismet.commons.security.AccessHandler;
import de.cismet.commons.security.handler.SimpleHttpAccessHandler;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class FormSolutionServerNewStuffAvailableAction implements UserAwareServerAction, MetaServiceStore {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            FormSolutionServerNewStuffAvailableAction.class);
    public static final String TASK_NAME = "formSolutionServerNewStuffAvailable";

    private static final String URL_AUFTRAGSLISTE =
        "https://demo.form-solutions.net/submission/retrieve/transactionIDs/22222222-2222/AS_KF600200";
    private static final String URL_AUFTRAG =
        "https://demo.form-solutions.net/submission/retrieve/data/22222222-2222/%s";
    private static final String URL_AUFTRAG_DELETE =
        "https://demo.form-solutions.net/submission/retrieve/setStatus/DELETED/22222222-2222/%s";
    private static final String STATUS_UPDATE_URL =
        "http://www.wuppertal.de/kartendownload/index.php?tid=%s&secret=P4rFx9As1bBc2R9Ya8";

    private static final Map<String, MetaClass> METACLASS_CACHE = new HashMap();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static final int STATUS_FETCH = 70;
    public static final int STATUS_PARSE = 60;
    public static final int STATUS_CLOSE = 50;
    public static final int STATUS_GETFLURSTUECK = 45;
    public static final int STATUS_SAVE = 40;
    public static final int STATUS_CREATEURL = 30;
    public static final int STATUS_DOWNLOAD = 20;
    public static final int STATUS_PENDING = 10;
    public static final int STATUS_DONE = 0;

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

    //~ Instance fields --------------------------------------------------------

    private final SimpleHttpAccessHandler HTTP_HANDLER = new SimpleHttpAccessHandler();

    private final UsernamePasswordCredentials creds;

    private User user;
    private MetaService metaService;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new FormSolutionServerNewStuffAvailableAction object.
     */
    public FormSolutionServerNewStuffAvailableAction() {
        creds = new UsernamePasswordCredentials(FormSolutionsConstants.USER, FormSolutionsConstants.PASSWORD);

//        getObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    //~ Methods ----------------------------------------------------------------

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
     * @param   table_name  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static MetaClass getMetaClass(final String table_name) {
        if (!METACLASS_CACHE.containsKey(table_name)) {
            MetaClass mc = null;
            try {
                mc = CidsBean.getMetaClassFromTableName("WUNDA_BLAU", table_name);
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
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private Collection<String> getOpenTransids() throws Exception {
        final StringBuilder stringBuilder = new StringBuilder();
        final InputStream inputStream = getHttpAccessHandler().doRequest(
                new URL(URL_AUFTRAGSLISTE),
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

        final Map<String, Object> map = getObjectMapper().readValue("{ \"list\" : " + stringBuilder.toString() + "}",
                new TypeReference<HashMap<String, Object>>() {
                });

        return (Collection<String>)map.get("list");
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
                bestellungBean.setProperty("fehler_ts", new Timestamp(new java.util.Date().getTime()));
                bestellungBean.setProperty("exception", getObjectMapper().writeValueAsString(exception));
                if (persist) {
                    getMetaService().updateMetaObject(user, bestellungBean.getMetaObject());
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
     * @param   auftrag  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private String getAuftrag(final String auftrag) throws Exception {
        final InputStream inputStream = getHttpAccessHandler().doRequest(
                new URL(String.format(URL_AUFTRAG, auftrag)),
                new StringReader(""),
                AccessHandler.ACCESS_METHODS.GET_REQUEST,
                null,
                creds);
        final Map<String, Object> map = getObjectMapper().readValue(
                inputStream,
                new TypeReference<HashMap<String, Object>>() {
                });
        inputStream.close();

        return new String(DatatypeConverter.parseBase64Binary((String)map.get("xml")));
    }

    /**
     * DOCUMENT ME!
     *
     * @param   auftrag  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private void closeTransid(final String auftrag) throws Exception {
        final boolean noClose = DomainServerImpl.getServerInstance()
                    .hasConfigAttr(getUser(), "custom.formsolutions.noclose");
        if (noClose) {
            return;
        }
        getHttpAccessHandler().doRequest(
            new URL(String.format(URL_AUFTRAG_DELETE, auftrag)),
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
            getHttpAccessHandler().doRequest(new URL(String.format(STATUS_UPDATE_URL, transid)),
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
     * @param   farbauspraegung  DOCUMENT ME!
     * @param   rawDin           DOCUMENT ME!
     * @param   rawAusrichtung   DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  RemoteException  DOCUMENT ME!
     */
    private CidsBean getProduktBean(final String farbauspraegung, final String rawDin, final String rawAusrichtung)
            throws RemoteException {
        final String produktKey;
        final String formatKey;

        if ("farbig".equals(farbauspraegung)) {
            produktKey = "LK.NRW.K.BF";
        } else if ("Graustufen".equals(farbauspraegung)) {
            produktKey = "LK.NRW.K.BSW";
        } else {
            produktKey = null;
        }

        if ((rawDin != null) && (rawAusrichtung != null)) {
            final String din = rawDin.trim().toUpperCase().split("DIN")[1].trim();
            final String ausrichtung = rawAusrichtung.trim().toLowerCase();
            formatKey = din + "-" + ausrichtung;
        } else {
            formatKey = null;
        }

        final MetaClass produktTypMc = getMetaClass("fs_bestellung_produkt_typ");
        final MetaClass produktMc = getMetaClass("fs_bestellung_produkt");
        final MetaClass formatMc = getMetaClass("fs_bestellung_format");
        final String produktQuery = "SELECT DISTINCT " + produktMc.getID() + ", "
                    + produktMc.getTableName() + "." + produktMc.getPrimaryKey() + " "
                    + "FROM " + produktMc.getTableName() + ", " + produktTypMc.getTableName() + ", "
                    + formatMc.getTableName() + " "
                    + "WHERE " + produktMc.getTableName() + ".fk_format = " + formatMc.getTableName() + ".id "
                    + "AND " + produktMc.getTableName() + ".fk_typ = " + produktTypMc.getTableName() + ".id "
                    + "AND " + produktTypMc.getTableName() + ".key = '" + produktKey + "' "
                    + "AND " + formatMc.getTableName() + ".key = '" + formatKey + "' "
                    + "LIMIT 1;";
        final MetaObject[] produktMos = getMetaService().getMetaObject(getUser(), produktQuery);
        produktMos[0].setAllClasses(((MetaClassCacheService)Lookup.getDefault().lookup(MetaClassCacheService.class))
                    .getAllClasses(produktMos[0].getDomain()));
        return produktMos[0].getBean();
    }

    /**
     * DOCUMENT ME!
     *
     * @param   formSolutionsBestellung  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private CidsBean createBestellungBean(final FormSolutionsBestellung formSolutionsBestellung) throws Exception {
        final MetaClass bestellungMc = getMetaClass("fs_bestellung");
        final MetaClass adresseMc = getMetaClass("fs_bestellung_adresse");
        final MetaClass geomMc = getMetaClass("geom");

        final CidsBean bestellungBean = bestellungMc.getEmptyInstance().getBean();
        final CidsBean adresseVersandBean = adresseMc.getEmptyInstance().getBean();
        final CidsBean geomBean = geomMc.getEmptyInstance().getBean();
        final CidsBean adresseRechnungBean;

        final String transid = formSolutionsBestellung.getTransId();
        final Integer massstab = (formSolutionsBestellung.getMassstab() != null)
            ? Integer.parseInt(formSolutionsBestellung.getMassstab().split(":")[1]) : null;

        final String flurstueckskennzeichen1 = trimedNotEmpty(formSolutionsBestellung.getFlurstueckskennzeichen1());
        final String flurstueckskennzeichen = trimedNotEmpty(formSolutionsBestellung.getFlurstueckskennzeichen());
        final String landparcelcode = (flurstueckskennzeichen1 != null) ? flurstueckskennzeichen1
                                                                        : flurstueckskennzeichen;

        CidsBean flurstueck = null;
        Geometry geom = null;
        try {
            flurstueck = getFlurstueck(landparcelcode);
            if (flurstueck == null) {
                throw new Exception("ALKIS Flurstück wurde nicht gefunden (" + landparcelcode + ")");
            }
            geom = (Geometry)flurstueck.getProperty("geometrie.geo_field");
        } catch (final Exception ex) {
            bestellungBean.setProperty("exception", getObjectMapper().writeValueAsString(ex));
        }

        final CidsBean produktBean = getProduktBean(formSolutionsBestellung.getFarbauspraegung(),
                formSolutionsBestellung.getFormat(),
                formSolutionsBestellung.getAusrichtung());

        adresseVersandBean.setProperty("firma", trimedNotEmpty(formSolutionsBestellung.getFirma()));
        adresseVersandBean.setProperty("name", trimedNotEmpty(formSolutionsBestellung.getAsName()));
        adresseVersandBean.setProperty("vorname", trimedNotEmpty(formSolutionsBestellung.getAsVorname()));
        adresseVersandBean.setProperty("strasse", trimedNotEmpty(formSolutionsBestellung.getAsStrasse()));
        adresseVersandBean.setProperty("hausnummer", trimedNotEmpty(formSolutionsBestellung.getAsHausnummer()));
        adresseVersandBean.setProperty("plz", formSolutionsBestellung.getAsPlz());
        adresseVersandBean.setProperty("ort", trimedNotEmpty(formSolutionsBestellung.getAsOrt()));
        adresseVersandBean.setProperty("staat", trimedNotEmpty(formSolutionsBestellung.getStaat()));

        if ("ja".equalsIgnoreCase(formSolutionsBestellung.getRechnungsanschriftLieferanschrift())) {
            adresseRechnungBean = adresseVersandBean;
        } else {
            adresseRechnungBean = adresseMc.getEmptyInstance().getBean();
            adresseRechnungBean.setProperty("firma", trimedNotEmpty(formSolutionsBestellung.getFirma1()));
            adresseRechnungBean.setProperty("name", trimedNotEmpty(formSolutionsBestellung.getAsName1()));
            adresseRechnungBean.setProperty("vorname", trimedNotEmpty(formSolutionsBestellung.getAsVorname1()));
            adresseRechnungBean.setProperty("strasse", trimedNotEmpty(formSolutionsBestellung.getAsStrasse1()));
            adresseRechnungBean.setProperty("hausnummer", trimedNotEmpty(formSolutionsBestellung.getAsHausnummer1()));
            adresseRechnungBean.setProperty("plz", formSolutionsBestellung.getAsPlz1());
            adresseRechnungBean.setProperty("ort", trimedNotEmpty(formSolutionsBestellung.getAsOrt1()));
            adresseRechnungBean.setProperty("staat", trimedNotEmpty(formSolutionsBestellung.getStaat1()));
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
        bestellungBean.setProperty("eingang_ts", new Timestamp(new java.util.Date().getTime()));

        if (geom != null) {
            geomBean.setProperty("geo_field", geom);
            bestellungBean.setProperty("geometrie", geomBean);
        }

        return bestellungBean;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   string  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String trimedNotEmpty(final String string) {
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
        for (final AlkisProductDescription product : AlkisProducts.getInstance().ALKIS_MAP_PRODUCTS) {
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
            final CidsBean flurstueck = getMetaService().getMetaObject(getUser(), mon.getObjectId(), mon.getClassId())
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
        final String flurstueckKennzeichen = (String)bestellungBean.getProperty("landparcelcode");

        final String transid = (String)bestellungBean.getProperty("transid");

        final Geometry geom = (Geometry)bestellungBean.getProperty("geometrie.geo_field");
        final Point center = geom.getCentroid();

        final URL url = AlkisProducts.getInstance()
                    .productKarteUrl(
                        flurstueckKennzeichen,
                        productDesc,
                        0,
                        (int)center.getX(),
                        (int)center.getY(),
                        null,
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
        OutputStream out = null;
        try {
            in = getHttpAccessHandler().doRequest(
                    productUrl,
                    new StringReader(""),
                    AccessHandler.ACCESS_METHODS.GET_REQUEST,
                    null,
                    creds);

            out = new FileOutputStream(destinationPath);
            final byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.close();
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
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
        final Map<String, String> fsXmlMap = new HashMap<String, String>(transids.size());

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
     *
     * @return  DOCUMENT ME!
     */
    private Map<String, FormSolutionsBestellung> createBestellungMap(final Map<String, String> fsXmlMap) {
        final Collection<String> transids = new ArrayList<String>(fsXmlMap.keySet());

        final Map<String, FormSolutionsBestellung> fsBestellungMap = new HashMap<String, FormSolutionsBestellung>(
                transids.size());
        for (final String transid : transids) {
            try {
                final String auftragXml = fsXmlMap.get(transid);
                final InputStream inputStream = IOUtils.toInputStream(auftragXml, "UTF-8");
                final FormSolutionsBestellung formSolutionBestellung = createFormSolutionsBestellung(inputStream);
                fsBestellungMap.put(transid, formSolutionBestellung);

                getMySqlHelper().updateEmail(transid, STATUS_PARSE, formSolutionBestellung.getEMailadresse());
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
     * @param   insertExceptionMap  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private Map<String, CidsBean> createCidsEntries(final Map<String, String> fsXmlMap,
            final Map<String, FormSolutionsBestellung> fsBestellungMap,
            final Map<String, Exception> insertExceptionMap) {
        // nur die transids bearbeiten, bei denen das Parsen auch geklappt hat
        final Collection<String> transidsNew = new ArrayList<String>(fsBestellungMap.keySet());

        final Map<String, CidsBean> fsBeanMap = new HashMap<String, CidsBean>(transidsNew.size());
        for (final String transid : transidsNew) {
            final String auftragXml = fsXmlMap.get(transid);
            final FormSolutionsBestellung formSolutionBestellung = fsBestellungMap.get(transid);

            try {
                final Exception insertException = insertExceptionMap.get(transid);

                final CidsBean bestellungBean = createBestellungBean(formSolutionBestellung);
                bestellungBean.setProperty("form_xml_orig", auftragXml);
                if (insertException != null) {
                    bestellungBean.setProperty("fehler", "Fehler beim Erzeugen des MySQL-Datensatzes");
                    bestellungBean.setProperty("exception", getObjectMapper().writeValueAsString(insertException));
                    bestellungBean.setProperty("fehler_ts", new Timestamp(new java.util.Date().getTime()));
                }
                if ((bestellungBean.getProperty("geometrie") == null)
                            && (bestellungBean.getProperty("exception") != null)) {
                    setErrorStatus(
                        transid,
                        STATUS_GETFLURSTUECK,
                        bestellungBean,
                        "Fehler beim Laden des Flurstücks",
                        getObjectMapper().readValue((String)bestellungBean.getProperty("exception"), Exception.class),
                        false);
                }

                final MetaObject persistedMo = getMetaService().insertMetaObject(
                        user,
                        bestellungBean.getMetaObject());

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
     * @param   transids  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public final Map<String, Exception> createMySqlEntries(final Collection<String> transids) {
        final Map<String, Exception> insertExceptionMap = new HashMap<String, Exception>(transids.size());

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

        return insertExceptionMap;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  fsBeanMap  DOCUMENT ME!
     */
    private void closeTransactions(final Map<String, CidsBean> fsBeanMap) {
        final Collection<String> transids = new ArrayList<String>(fsBeanMap.keySet());
        for (final String transid : transids) {
            final CidsBean bestellungBean = fsBeanMap.get(transid);
            if ((bestellungBean != null)) {
                try {
                    closeTransid(transid);

                    getMySqlHelper().updateStatus(transid, STATUS_CLOSE);
                    doStatusChangedRequest(transid);
                } catch (Exception ex) {
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
        final Collection<String> transids = new ArrayList<String>(fsBeanMap.keySet());

        final Map<String, URL> fsUrlMap = new HashMap<String, URL>(transids.size());

        for (final String transid : new ArrayList<String>(fsBeanMap.keySet())) {
            final CidsBean bestellungBean = fsBeanMap.get(transid);
            if ((bestellungBean != null) && (bestellungBean.getProperty("fehler") == null)) {
                try {
                    final URL productUrl = createProductUrl(bestellungBean);
                    fsUrlMap.put(transid, productUrl);
                    getMySqlHelper().updateStatus(transid, STATUS_CREATEURL);
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
     * @param  fsBeanMap  DOCUMENT ME!
     * @param  fsUrlMap   DOCUMENT ME!
     */
    private void downloadProdukte(final Map<String, CidsBean> fsBeanMap, final Map<String, URL> fsUrlMap) {
        final Collection<String> transids = new ArrayList<String>(fsUrlMap.keySet());

        for (final String transid : transids) {
            final CidsBean bestellungBean = fsBeanMap.get(transid);
            if ((bestellungBean != null) && (bestellungBean.getProperty("fehler") == null)) {
                final URL productUrl = fsUrlMap.get(transid);
                try {
                    bestellungBean.setProperty("request_url", productUrl.toString());

                    final String filePath = bestellungBean.getProperty("transid") + ".pdf";

                    final String fullFilePath = FormSolutionsConstants.PRODUKT_BASEPATH + File.separator + filePath;

                    downloadProdukt(productUrl, fullFilePath);

                    final String fileNameOrig = (String)bestellungBean.getProperty("fk_produkt.fk_typ.key")
                                + "."
                                + ((String)bestellungBean.getProperty("landparcelcode")).replace(
                                    "/",
                                    "--")
                                + ".pdf";

                    bestellungBean.setProperty("produkt_dateipfad", filePath);
                    bestellungBean.setProperty("produkt_dateiname_orig", fileNameOrig);

                    getMySqlHelper().updateProdukt(
                        transid,
                        STATUS_DOWNLOAD,
                        (String)bestellungBean.getProperty("produkt_dateipfad"),
                        (String)bestellungBean.getProperty("produkt_dateiname_orig"));
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
     * @param  fsBeanMap  DOCUMENT ME!
     */
    private void finalizeBeans(final Map<String, CidsBean> fsBeanMap) {
        final Collection<String> transids = new ArrayList<String>(fsBeanMap.keySet());
        for (final String transid : transids) {
            final CidsBean bestellungBean = fsBeanMap.get(transid);
            if ((bestellungBean != null) && (bestellungBean.getProperty("fehler") == null)) {
                try {
                    final Boolean propPostweg = (Boolean)bestellungBean.getProperty("postweg");
                    if (!Boolean.TRUE.equals(propPostweg)) {
                        bestellungBean.setProperty("erledigt", true);
                    }
                    getMetaService().updateMetaObject(user, bestellungBean.getMetaObject());
                } catch (final Exception ex) {
                    LOG.error("Fehler beim Persistieren der Bestellung", ex);
                }
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param  fsBeanMap  DOCUMENT ME!
     */
    private void finalizeMySqls(final Map<String, CidsBean> fsBeanMap) {
        final Collection<String> transids = new ArrayList<String>(fsBeanMap.keySet());
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
                    getMySqlHelper().updateStatus(transid, okStatus);
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
                    for (final MetaObjectNode mon : mons) {
                        final CidsBean bestellungBean;
                        try {
                            bestellungBean = DomainServerImpl.getServerInstance()
                                        .getMetaObject(getUser(), mon.getObjectId(), mon.getClassId())
                                        .getBean();
                            final String transid = (String)bestellungBean.getProperty("transid");
                            fsBeanMap.put(transid, bestellungBean);
                            bestellungBean.setProperty("erledigt", false);
                            bestellungBean.setProperty("fehler", null);
                            bestellungBean.setProperty("fehler_ts", null);
                            bestellungBean.setProperty("exception", null);
                            bestellungBean.setProperty("produkt_dateipfad", null);
                            bestellungBean.setProperty("produkt_dateiname_orig", null);
                            metaService.updateMetaObject(getUser(), bestellungBean.getMetaObject());
                        } catch (final Exception ex) {
                            LOG.error(ex, ex);
                        }
                    }
                }
            }

            switch (startStep) {
                case STATUS_FETCH:
                case STATUS_PARSE:
                case STATUS_SAVE: {
                    if (fetchFromFs) {
                        try {
                            final Collection<String> transids = getOpenTransids();
                            final Map<String, Exception> insertExceptionMap = createMySqlEntries(transids);
                            final Map<String, String> fsXmlMap = extractXmlParts(transids);
                            final Map<String, FormSolutionsBestellung> fsBestellungMap = createBestellungMap(fsXmlMap);
                            fsBeanMap.putAll(createCidsEntries(fsXmlMap, fsBestellungMap, insertExceptionMap));
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
}
