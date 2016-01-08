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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;

import java.net.URL;

import java.rmi.Remote;
import java.rmi.RemoteException;

import java.sql.Date;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;

import de.cismet.cids.custom.utils.alkis.AlkisProductDescription;
import de.cismet.cids.custom.utils.alkis.AlkisProducts;
import de.cismet.cids.custom.utils.formsolutions.FormSolutionsBestellung;
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
    private static MetaClass METACLASS_ADRESSE;
    private static MetaClass METACLASS_FORMAT;

    //~ Instance fields --------------------------------------------------------

    private final UsernamePasswordCredentials creds;
    private final SimpleHttpAccessHandler handler = new SimpleHttpAccessHandler();

    private User user;
    private MetaService metaService;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new FormSolutionServerNewStuffAvailableAction object.
     */
    public FormSolutionServerNewStuffAvailableAction() {
        final String usermame = "22222222-2222";
        final String password = "actipassfoso42";

        creds = new UsernamePasswordCredentials(usermame, password);
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
    private Collection<String> getOpenAuftraege() throws Exception {
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

        return new String(Base64.getDecoder().decode((String)map.get("xml")));
    }

    /**
     * DOCUMENT ME!
     *
     * @param   auftrag  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private void closeAuftrag(final String auftrag) throws Exception {
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
     * @param   rawDin          DOCUMENT ME!
     * @param   rawAusrichtung  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  RemoteException  DOCUMENT ME!
     */
    private CidsBean getFormatBean(final String rawDin, final String rawAusrichtung) throws RemoteException {
        final String formatKey;

        if ((rawDin != null) && (rawAusrichtung != null)) {
            final String din = rawDin.trim().toUpperCase().split("DIN")[1].trim();
            final String ausrichtung = rawAusrichtung.trim().toLowerCase();
            formatKey = din + "-" + ausrichtung;
        } else {
            formatKey = null;
        }
        final MetaClass formatMC = getFormatMetaClass();
        final String formatQuery = "SELECT DISTINCT " + formatMC.getID() + ", "
                    + formatMC.getPrimaryKey() + " "
                    + "FROM " + formatMC.getTableName() + " "
                    + "WHERE key = '" + formatKey + "' "
                    + "LIMIT 1;";
        final MetaObject[] formatMos = getMetaService().getMetaObject(getUser(), formatQuery);
        formatMos[0].setAllClasses(((MetaClassCacheService)Lookup.getDefault().lookup(MetaClassCacheService.class))
                    .getAllClasses(formatMos[0].getDomain()));
        return formatMos[0].getBean();
    }

    /**
     * DOCUMENT ME!
     *
     * @param   farbauspraegung  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  RemoteException  DOCUMENT ME!
     */
    private CidsBean getProduktBean(final String farbauspraegung) throws RemoteException {
        final String produktKey;
        if ("farbig".equals(farbauspraegung)) {
            produktKey = "LK.NRW.K.F";
        } else if ("Graustufen".equals(farbauspraegung)) {
            produktKey = "LK.NRW.K.SW";
        } else {
            produktKey = null;
        }
        final MetaClass produktMc = getProduktMetaClass();
        final String produktQuery = "SELECT DISTINCT " + produktMc.getID() + ", "
                    + produktMc.getPrimaryKey() + " "
                    + "FROM " + produktMc.getTableName() + " "
                    + "WHERE key = '" + produktKey + "' "
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
        final CidsBean adresseRechnungBean = adresseMc.getEmptyInstance().getBean();

        final Integer massstab = (formSolutionsBestellung.getMassstab() != null)
            ? Integer.parseInt(formSolutionsBestellung.getMassstab().split(":")[1]) : null;

        final CidsBean formatBean = getFormatBean(formSolutionsBestellung.getFormat(),
                formSolutionsBestellung.getAusrichtung());
        final CidsBean produktBean = getProduktBean(formSolutionsBestellung.getFarbauspraegung());

        adresseVersandBean.setProperty(
            "firma",
            formSolutionsBestellung.getFirma().trim().isEmpty() ? null : formSolutionsBestellung.getFirma());
        adresseVersandBean.setProperty(
            "name",
            formSolutionsBestellung.getAsName().trim().isEmpty() ? null : formSolutionsBestellung.getAsName());
        adresseVersandBean.setProperty(
            "vorname",
            formSolutionsBestellung.getAsVorname().trim().isEmpty() ? null : formSolutionsBestellung.getAsVorname());
        adresseVersandBean.setProperty(
            "strasse",
            formSolutionsBestellung.getAsStrasse().trim().isEmpty() ? null : formSolutionsBestellung.getAsStrasse());
        adresseVersandBean.setProperty(
            "hausnummer",
            formSolutionsBestellung.getAsHausnummer().trim().isEmpty() ? null
                                                                       : formSolutionsBestellung.getAsHausnummer());
        adresseVersandBean.setProperty("plz", formSolutionsBestellung.getAsPlz());
        adresseVersandBean.setProperty(
            "ort",
            formSolutionsBestellung.getAsOrt().trim().isEmpty() ? null : formSolutionsBestellung.getAsOrt());
        adresseVersandBean.setProperty(
            "staat",
            formSolutionsBestellung.getStaat().trim().isEmpty() ? null : formSolutionsBestellung.getStaat());

        adresseRechnungBean.setProperty(
            "firma",
            formSolutionsBestellung.getFirma1().trim().isEmpty() ? null : formSolutionsBestellung.getFirma1());
        adresseRechnungBean.setProperty(
            "name",
            formSolutionsBestellung.getAsName1().trim().isEmpty() ? null : formSolutionsBestellung.getAsName1());
        adresseRechnungBean.setProperty(
            "vorname",
            formSolutionsBestellung.getAsVorname1().trim().isEmpty() ? null : formSolutionsBestellung.getAsVorname1());
        adresseRechnungBean.setProperty(
            "strasse",
            formSolutionsBestellung.getAsStrasse1().trim().isEmpty() ? null : formSolutionsBestellung.getAsStrasse1());
        adresseRechnungBean.setProperty(
            "hausnummer",
            formSolutionsBestellung.getAsHausnummer1().trim().isEmpty() ? null
                                                                        : formSolutionsBestellung.getAsHausnummer1());
        adresseRechnungBean.setProperty("plz", formSolutionsBestellung.getAsPlz1());
        adresseRechnungBean.setProperty(
            "ort",
            formSolutionsBestellung.getAsOrt1().trim().isEmpty() ? null : formSolutionsBestellung.getAsOrt1());
        adresseRechnungBean.setProperty(
            "staat",
            formSolutionsBestellung.getStaat1().trim().isEmpty() ? null : formSolutionsBestellung.getStaat1());

        bestellungBean.setProperty("postweg", "Kartenauszug".equals(formSolutionsBestellung.getBezugsweg()));
        bestellungBean.setProperty("transid", formSolutionsBestellung.getTransId());
        bestellungBean.setProperty("landparcelcode", formSolutionsBestellung.getFlurstueckskennzeichen());
        bestellungBean.setProperty("fk_produkt", produktBean);
        bestellungBean.setProperty("fk_format", formatBean);
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
        final Collection<MetaObjectNode> res = search.performServerSearch();
        final MetaObjectNode mon = new ArrayList<MetaObjectNode>(res).get(0);
        final CidsBean flurstueck = DomainServerImpl.getServerInstance()
                    .getMetaObject(getUser(), mon.getObjectId(), mon.getClassId())
                    .getBean();
        return flurstueck;
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
        final String code = (String)bestellungBean.getProperty("fk_produkt.key");
        final String dinFormat = (String)bestellungBean.getProperty("fk_format.format");
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

    @Override
    public Object execute(final Object body, final ServerActionParameter... params) {
        try {
            for (final String auftrag : getOpenAuftraege()) {
                final String auftragXml = getAuftrag(auftrag);
                LOG.fatal(auftragXml);
                final InputStream inputStream = IOUtils.toInputStream(auftragXml, "UTF-8");
                final FormSolutionsBestellung formSolutionBestellung = createFormSolutionsBestellung(inputStream);
                final CidsBean bestellungBean = createBestellungBean(formSolutionBestellung);

                final MetaObject persisted = DomainServerImpl.getServerInstance()
                            .insertMetaObject(user, bestellungBean.getMetaObject());
                final CidsBean persistedBestellungBean = persisted.getBean();

                final URL productUrl = createProductUrl(persistedBestellungBean);
                persistedBestellungBean.setProperty("request_url", productUrl.toString());

                // TODO request product, store it to his destination path, and then save the file path to the bean
                persistedBestellungBean.persist();

                closeAuftrag(auftrag);
            }
        } catch (final Exception ex) {
            LOG.fatal(ex, ex);
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
