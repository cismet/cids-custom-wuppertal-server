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

import org.openide.util.Lookup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import java.net.URL;

import java.rmi.Remote;
import java.rmi.RemoteException;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;

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

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String URL_AUFTRAGSLISTE =
        "https://demo.form-solutions.net/submission/retrieve/transactionIDs/22222222-2222/AS_KF600200";
    private static final String URL_AUFTRAG =
        "https://demo.form-solutions.net/submission/retrieve/data/22222222-2222/%s";
    private static final String URL_AUFTRAG_DELETE =
        "https://demo.form-solutions.net/submission/retrieve/setStatus/DELETED/22222222-2222/%s";

    private static MetaClass METACLASS_BESTELLUNG;
    private static MetaClass METACLASS_PRODUKT;
    private static MetaClass METACLASS_PRODUKT_TYP;
    private static MetaClass METACLASS_ADRESSE;
    private static MetaClass METACLASS_FORMAT;

    //~ Instance fields --------------------------------------------------------

    private final UsernamePasswordCredentials creds;
    private final SimpleHttpAccessHandler handler = new SimpleHttpAccessHandler();

    private User user;
    private MetaService metaService;
    private final FormSolutionsMySqlHelper mySqlHelper;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new FormSolutionServerNewStuffAvailableAction object.
     */
    public FormSolutionServerNewStuffAvailableAction() {
        creds = new UsernamePasswordCredentials(FormSolutionsConstants.USER, FormSolutionsConstants.PASSWORD);
        mySqlHelper = FormSolutionsMySqlHelper.getInstance();
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static MetaClass getBestellungMetaClass() {
        if (METACLASS_BESTELLUNG == null) {
            try {
                METACLASS_BESTELLUNG = CidsBean.getMetaClassFromTableName("WUNDA_BLAU", "fs_bestellung");
            } catch (final Exception ex) {
                LOG.error("could not get metaclass of fs_bestellung", ex);
            }
        }
        return METACLASS_BESTELLUNG;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static MetaClass getProduktMetaClass() {
        if (METACLASS_PRODUKT == null) {
            try {
                METACLASS_PRODUKT = CidsBean.getMetaClassFromTableName("WUNDA_BLAU", "fs_bestellung_produkt");
            } catch (final Exception ex) {
                LOG.error("could not get metaclass of fs_bestellung_produkt", ex);
            }
        }
        return METACLASS_PRODUKT;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static MetaClass getProduktTypMetaClass() {
        if (METACLASS_PRODUKT_TYP == null) {
            try {
                METACLASS_PRODUKT_TYP = CidsBean.getMetaClassFromTableName("WUNDA_BLAU", "fs_bestellung_produkt_typ");
            } catch (final Exception ex) {
                LOG.error("could not get metaclass of fs_bestellung_produkt_typ", ex);
            }
        }
        return METACLASS_PRODUKT_TYP;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static MetaClass getAdresseMetaClass() {
        if (METACLASS_ADRESSE == null) {
            try {
                METACLASS_ADRESSE = CidsBean.getMetaClassFromTableName("WUNDA_BLAU", "fs_bestellung_adresse");
            } catch (final Exception ex) {
                LOG.error("could not get metaclass of fs_bestellung_adresse", ex);
            }
        }
        return METACLASS_ADRESSE;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static MetaClass getFormatMetaClass() {
        if (METACLASS_FORMAT == null) {
            try {
                METACLASS_FORMAT = CidsBean.getMetaClassFromTableName("WUNDA_BLAU", "fs_bestellung_format");
            } catch (final Exception ex) {
                LOG.error("could not get metaclass of fs_bestellung_format", ex);
            }
        }
        return METACLASS_FORMAT;
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
        final InputStream inputStream = handler.doRequest(
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

        final Map<String, Object> map = MAPPER.readValue("{ \"list\" : " + stringBuilder.toString() + "}",
                new TypeReference<HashMap<String, Object>>() {
                });

        return (Collection<String>)map.get("list");
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
        final InputStream inputStream = handler.doRequest(
                new URL(String.format(URL_AUFTRAG, auftrag)),
                new StringReader(""),
                AccessHandler.ACCESS_METHODS.GET_REQUEST,
                null,
                creds);
        final Map<String, Object> map = MAPPER.readValue(inputStream, new TypeReference<HashMap<String, Object>>() {
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
        handler.doRequest(
            new URL(String.format(URL_AUFTRAG_DELETE, auftrag)),
            new StringReader(""),
            AccessHandler.ACCESS_METHODS.POST_REQUEST,
            null,
            creds);
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
            produktKey = "LK.NRW.K.F";
        } else if ("Graustufen".equals(farbauspraegung)) {
            produktKey = "LK.NRW.K.SW";
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

        final MetaClass produktTypMc = getProduktTypMetaClass();
        final MetaClass produktMc = getProduktMetaClass();
        final MetaClass formatMc = getFormatMetaClass();
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
        final MetaClass bestellungMc = getBestellungMetaClass();
        final MetaClass adresseMc = getAdresseMetaClass();
        final CidsBean bestellungBean = bestellungMc.getEmptyInstance().getBean();
        final CidsBean adresseVersandBean = adresseMc.getEmptyInstance().getBean();
        final CidsBean adresseRechnungBean;

        final Integer massstab = (formSolutionsBestellung.getMassstab() != null)
            ? Integer.parseInt(formSolutionsBestellung.getMassstab().split(":")[1]) : null;

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
        bestellungBean.setProperty("transid", formSolutionsBestellung.getTransId());
        final String landparcelcode1 = trimedNotEmpty(formSolutionsBestellung.getFlurstueckskennzeichen1());
        final String landparcelcode = trimedNotEmpty(formSolutionsBestellung.getFlurstueckskennzeichen());
        if (landparcelcode1 != null) {
            bestellungBean.setProperty("landparcelcode", landparcelcode1);
        } else {
            bestellungBean.setProperty("landparcelcode", landparcelcode);
        }
        bestellungBean.setProperty("fk_produkt", produktBean);
        bestellungBean.setProperty("massstab", massstab);
        bestellungBean.setProperty("fk_adresse_versand", adresseVersandBean);
        bestellungBean.setProperty("fk_adresse_rechnung", adresseRechnungBean);
        bestellungBean.setProperty("email", "platzhalter@email.fake"); // TODO reale email Adresse aus XML verwenden
        bestellungBean.setProperty("erledigt", false);
        bestellungBean.setProperty("eingegangen_am", new Date(new java.util.Date().getTime()));

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
        localServers.put("WUNDA_BLAU", DomainServerImpl.getServerInstance());
        search.setActiveLocalServers(localServers);
        search.setUser(getUser());
        final Collection<MetaObjectNode> mons = search.performServerSearch();
        if ((mons != null) && !mons.isEmpty()) {
            final MetaObjectNode mon = new ArrayList<MetaObjectNode>(mons).get(0);
            final CidsBean flurstueck = DomainServerImpl.getServerInstance()
                        .getMetaObject(getUser(), mon.getObjectId(), mon.getClassId())
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

        final AlkisProductDescription product = getAlkisProductDescription(code, dinFormat, scale);

        final String flurstueckKennzeichen = (String)bestellungBean.getProperty("landparcelcode");
        final CidsBean flurstueck = getFlurstueck(flurstueckKennzeichen);

        final Geometry geom = (Geometry)flurstueck.getProperty("geometrie.geo_field");
        final Point center = geom.getCentroid();

        final URL url = AlkisProducts.getInstance()
                    .productKarteUrl(
                        flurstueckKennzeichen,
                        product,
                        0,
                        (int)center.getX(),
                        (int)center.getY(),
                        null,
                        null,
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
            in = handler.doRequest(
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
     * @param  transid  DOCUMENT ME!
     * @param  status   DOCUMENT ME!
     */
    private void storeErrorSql(final String transid, final int status) {
        try {
            mySqlHelper.updateMySQL(transid, status);
        } catch (final SQLException ex) {
            LOG.error("Fehler beim Aktualisieren des MySQL-Datensatzes", ex);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param  bestellungBean  DOCUMENT ME!
     * @param  message         DOCUMENT ME!
     * @param  exception       DOCUMENT ME!
     */
    private void storeErrorCids(final CidsBean bestellungBean, final String message, final Exception exception) {
        try {
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw);
            exception.printStackTrace(pw);
            bestellungBean.setProperty("erledigt", false);
            bestellungBean.setProperty("fehler", message + ":\n" + sw.toString());
        } catch (final Exception ex) {
        }
        try {
            DomainServerImpl.getServerInstance().updateMetaObject(user, bestellungBean.getMetaObject());
        } catch (final RemoteException ex) {
            LOG.error("Fehler beim Persistieren der Bestellung", ex);
        }
    }

    @Override
    public Object execute(final Object body, final ServerActionParameter... params) {
        Collection<String> transids = null;

        try {
            transids = getOpenTransids();
        } catch (Exception ex) {
            LOG.error("Die Liste der FormSolutions-Bestellungen konnte nicht abgerufen werden", ex);
        }

        if (transids != null) {
            for (final String transid : transids) {
                boolean mysqlEntryAlreadyExists = false;
                try {
                    final ResultSet resultSet = mySqlHelper.selectMySQL(transid);
                    mysqlEntryAlreadyExists = (resultSet != null) && resultSet.next();
                } catch (final SQLException ex) {
                    LOG.error("check nach bereits vorhandenen transids fehlgeschlagen.", ex);
                }

                CidsBean persistedBestellungBean = null;

                // 1: in Bearbeitung
                // 0: erfolgreich
                // -1: Fehler beim Request/Parsen der FormSolution-Schnittstelle
                // -2: Fehler beim Erzeugen des Cids-Objektes
                // -3: Fehler beim Erzeugen/Runterladen des Produktes
                // -4: Fehler beim Persistieren in Cids
                int status = 1;

                try {
                    final String auftragXml = getAuftrag(transid);
                    final InputStream inputStream = IOUtils.toInputStream(auftragXml, "UTF-8");
                    final FormSolutionsBestellung formSolutionBestellung = createFormSolutionsBestellung(inputStream);

                    try {
                        final CidsBean bestellungBean = createBestellungBean(formSolutionBestellung);
                        bestellungBean.setProperty("form_xml_orig", auftragXml);

                        final MetaObject persisted = DomainServerImpl.getServerInstance()
                                    .insertMetaObject(user, bestellungBean.getMetaObject());
                        persistedBestellungBean = persisted.getBean();
                    } catch (final Exception ex) {
                        LOG.error("Fehler beim Erstellen des Bestellungs-Objektes", ex);
                        status = -2;
                    }
                } catch (final Exception ex) {
                    LOG.error("Fehler beim Parsen FormSolution", ex);
                    status = -1;
                }

                try {
                    if (mysqlEntryAlreadyExists) {
                        mySqlHelper.updateMySQL(transid, status);
                    } else {
                        mySqlHelper.insertMySQL(transid, status);
                    }
                } catch (final SQLException ex) {
                    LOG.error("Fehler beim Erzeugen/Aktualisieren des MySQL-Datensatzes.", ex);
                    if (persistedBestellungBean != null) {
                        storeErrorCids(
                            persistedBestellungBean,
                            "Fehler beim Erzeugen/Aktualisieren des MySQL-Datensatzes.",
                            ex);
                    }
                    break;
                }

                if (status < 0) {
                    break;
                }

                if (persistedBestellungBean != null) {
                    try {
                        closeTransid(transid);
                    } catch (Exception ex) {
                        LOG.error("Fehler beim Schließen der Transaktion.", ex);
                        storeErrorCids(persistedBestellungBean, "Fehler beim Schließen der Transaktion.", ex);
                        break;
                    }

                    try {
                        final URL productUrl = createProductUrl(persistedBestellungBean);
                        persistedBestellungBean.setProperty("request_url", productUrl.toString());

                        final String filePath = FormSolutionsConstants.PRODUKT_BASEPATH + File.separator
                                    + persistedBestellungBean.getProperty("transid") + ".pdf";

                        downloadProdukt(productUrl, filePath);

                        final String fileNameOrig = (String)persistedBestellungBean.getProperty("fk_produkt.fk_typ.key")
                                    + "."
                                    + ((String)persistedBestellungBean.getProperty("landparcelcode")).replace(
                                        "/",
                                        "--")
                                    + ".pdf";

                        persistedBestellungBean.setProperty("produkt_dateipfad", filePath);
                        persistedBestellungBean.setProperty("produkt_dateiname_orig", fileNameOrig);
                        persistedBestellungBean.setProperty("erledigt", true);
                    } catch (final Exception ex) {
                        LOG.error("Fehler beim Erzeugen des Produktes", ex);
                        storeErrorSql(transid, -3);
                        storeErrorCids(persistedBestellungBean, "Fehler beim Erzeugen des Produktes", ex);
                        break;
                    }

                    try {
                        DomainServerImpl.getServerInstance()
                                .updateMetaObject(user, persistedBestellungBean.getMetaObject());
                    } catch (final RemoteException ex) {
                        LOG.error("Fehler beim Persistieren der Bestellung", ex);
                        storeErrorSql(transid, -4);
                        break;
                    }

                    try {
                        final int okStatus;
                        final Boolean propPostweg = (Boolean)persistedBestellungBean.getProperty("postweg");
                        if (Boolean.TRUE.equals(propPostweg)) {
                            okStatus = 2;
                        } else {
                            okStatus = 1;
                        }

                        mySqlHelper.updateMySQL(
                            transid,
                            okStatus,
                            (String)persistedBestellungBean.getProperty("produkt_dateipfad"),
                            (String)persistedBestellungBean.getProperty("produkt_dateiname_orig"));
                    } catch (final SQLException ex) {
                        storeErrorCids(
                            persistedBestellungBean,
                            "Fehler beim Abschließen des MYSQL-Datensatzes",
                            ex);
                    }
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
