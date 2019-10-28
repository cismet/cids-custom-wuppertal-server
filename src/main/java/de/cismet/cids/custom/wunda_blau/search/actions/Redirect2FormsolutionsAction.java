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

import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.middleware.interfaces.domainserver.MetaServiceStore;
import Sirius.server.middleware.types.MetaObject;
import Sirius.server.middleware.types.MetaObjectNode;
import Sirius.server.newuser.User;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.io.StringReader;

import java.net.URL;
import java.net.URLEncoder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import de.cismet.cids.custom.utils.formsolutions.FormSolutionsProperties;
import de.cismet.cids.custom.wunda_blau.search.server.FormSolutionsBestellungSearch;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;
import de.cismet.cids.server.actions.UserAwareServerAction;

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
public class Redirect2FormsolutionsAction implements UserAwareServerAction, MetaServiceStore, ConnectionContextStore {

    //~ Static fields/initializers ---------------------------------------------

    public static final String TASK_NAME = "redirect2Formsolutions";
    private static final transient org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            Redirect2FormsolutionsAction.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum PARAMETER_TYPE {

        //~ Enum constants -----------------------------------------------------

        TRANSID_HASH
    }

    //~ Instance fields --------------------------------------------------------

    private User user;
    private MetaService metaService;
    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    //~ Methods ----------------------------------------------------------------

    @Override
    public void initWithConnectionContext(final ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   map  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private static String urlEncodeUTF8(final Map<String, Object> map) throws Exception {
        final Collection<String> keyValues = new ArrayList<>();
        for (final Map.Entry<String, Object> entry : map.entrySet()) {
            final String key = entry.getKey();
            final Object value = entry.getValue();
            if (key != null) {
                keyValues.add(String.format(
                        "%s=%s",
                        URLEncoder.encode(key, "UTF-8"),
                        URLEncoder.encode((value != null) ? value.toString() : "", "UTF-8")));
            }
        }
        return String.join("&", keyValues);
    }

    @Override
    public Object execute(final Object body, final ServerActionParameter... params) {
        String transidHash = null;
        if (params != null) {
            for (final ServerActionParameter sap : params) {
                if (sap.getKey().equals(PARAMETER_TYPE.TRANSID_HASH.toString())) {
                    transidHash = (String)sap.getValue();
                }
            }
        }

        if (transidHash != null) {
            try {
                final FormSolutionsBestellungSearch search = new FormSolutionsBestellungSearch();
                final Map localServers = new HashMap<>();
                localServers.put("WUNDA_BLAU", getMetaService());
                search.setActiveLocalServers(localServers);
                search.setUser(getUser());
                search.setTransidHash(transidHash);
                final Collection<MetaObjectNode> mons = search.performServerSearch();
                if ((mons != null) && (mons.size() == 1)) {
                    final MetaObjectNode mon = mons.iterator().next();
                    final MetaObject mo = getMetaService().getMetaObject(
                            getUser(),
                            mon.getObjectId(),
                            mon.getClassId(),
                            getConnectionContext());
                    final CidsBean bestellungBean = mo.getBean();

                    final String transid = (String)bestellungBean.getProperty("transid");
                    final Double gebuehr = (Double)bestellungBean.getProperty("gebuehr");
                    final String email = (String)bestellungBean.getProperty("email");

                    final String rechnungFirma = (String)bestellungBean.getProperty(
                            "fk_adresse_rechnung.firma");
                    final String rechnungVorname = (String)bestellungBean.getProperty(
                            "fk_adresse_rechnung.vorname");
                    final String rechnungName = (String)bestellungBean.getProperty("fk_adresse_rechnung.name");
                    final String rechnungStrasse = (String)bestellungBean.getProperty(
                            "fk_adresse_rechnung.strasse");
                    final String rechnungHausnummer = (String)bestellungBean.getProperty(
                            "fk_adresse_rechnung.hausnummer");
                    final Integer rechnungPlz = (Integer)bestellungBean.getProperty("fk_adresse_rechnung.plz");
                    final String rechnungOrt = (String)bestellungBean.getProperty("fk_adresse_rechnung.ort");
                    final String rechnungStaat = (String)bestellungBean.getProperty(
                            "fk_adresse_rechnung.staat");

                    final String lieferFirma = (String)bestellungBean.getProperty("fk_adresse_versand.firma");
                    final String lieferVorname = (String)bestellungBean.getProperty(
                            "fk_adresse_versand.vorname");
                    final String lieferName = (String)bestellungBean.getProperty("fk_adresse_versand.name");
                    final String lieferStrasse = (String)bestellungBean.getProperty(
                            "fk_adresse_versand.strasse");
                    final String lieferHausnummer = (String)bestellungBean.getProperty(
                            "fk_adresse_versand.hausnummer");
                    final Integer lieferPlz = (Integer)bestellungBean.getProperty("fk_adresse_versand.plz");
                    final String lieferOrt = (String)bestellungBean.getProperty("fk_adresse_versand.ort");
                    final String lieferStaat = (String)bestellungBean.getProperty("fk_adresse_versand.staat");

                    final Map<String, Object> form = new HashMap();
                    form.put("Antragsteller.Daten.Vorgang", transid);
                    form.put(
                        "Antragsteller.Daten.Flurstueckskennzeichen",
                        (String)bestellungBean.getProperty("landparcelcode"));
                    form.put("Antragsteller.Daten.betrag", gebuehr);

                    form.put("Antragsteller.Daten.Email bei Postversand.E-Mailadresse.E-Mailadresse", email);
                    form.put("Antragsteller.Daten.AS_Adresse.AS_Adresse.Adresse.staat.staat", rechnungStaat);

                    form.put("Antragsteller.Daten.Firma", rechnungFirma);
                    form.put("Antragsteller.Daten.AS_Name1.AS_Name1.AS_Vorname", rechnungVorname);
                    form.put("Antragsteller.Daten.AS_Name1.AS_Name1.AS_Name", rechnungName);
                    form.put("Antragsteller.Daten.Email bei Download.E-Mailadresse", email);
                    form.put("Street", rechnungStrasse);
                    form.put("StreetNumber", rechnungHausnummer);
                    form.put("ZipCode", Integer.toString(rechnungPlz));
                    form.put("City", rechnungOrt);

                    form.put(
                        "Antragsteller.Daten.AS_Name1_Abweichende_Lieferanschrift.AS_Name1.AS_Vorname",
                        lieferVorname);
                    form.put(
                        "Antragsteller.Daten.AS_Name1_Abweichende_Lieferanschrift.AS_Name1.AS_Name",
                        lieferName);
                    form.put(
                        "Antragsteller.Daten.AS_Adresse_Abweichende_Lieferanschrift.AS_Adresse.Adresse.staat.staat",
                        lieferStaat);
                    form.put(
                        "Antragsteller.Daten.AS_Adresse_Abweichende_Lieferanschrift.AS_Adresse.Adresse.AS_Strasse",
                        lieferStrasse);
                    form.put(
                        "Antragsteller.Daten.AS_Adresse_Abweichende_Lieferanschrift.AS_Adresse.Adresse.AS_Hausnummer",
                        lieferHausnummer);
                    form.put(
                        "Antragsteller.Daten.AS_Adresse_Abweichende_Lieferanschrift.AS_Adresse.Adresse.AS_PLZ",
                        lieferPlz);
                    form.put(
                        "Antragsteller.Daten.AS_Adresse_Abweichende_Lieferanschrift.AS_Adresse.Adresse.AS_Ort",
                        lieferOrt);

                    final HashMap<String, String> headerMap = new HashMap<>();
                    headerMap.put("Content-Type", "application/x-www-form-urlencoded");
                    final InputStream in =
                        new SimpleHttpAccessHandler().doRequest(
                            new URL(FormSolutionsProperties.getInstance().getUrlCreateCacheid()),
                            new StringReader(urlEncodeUTF8(form)),
                            AccessHandler.ACCESS_METHODS.POST_REQUEST,
                            headerMap);
                    final String cacheID = IOUtils.toString(in, "UTF-8");
                    final String redirectionLink = String.format(FormSolutionsProperties.getInstance()
                                    .getRedirectionFormat(),
                            cacheID);

                    bestellungBean.setProperty("request_url", redirectionLink);
                    getMetaService().updateMetaObject(
                        getUser(),
                        bestellungBean.getMetaObject(),
                        getConnectionContext());
                    return redirectionLink;
                }
            } catch (final Exception ex) {
                LOG.error(ex, ex);
                return null;
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

    @Override
    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }
}
