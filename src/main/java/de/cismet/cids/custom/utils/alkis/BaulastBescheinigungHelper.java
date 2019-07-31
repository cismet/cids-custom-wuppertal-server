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
package de.cismet.cids.custom.utils.alkis;

import Sirius.server.middleware.impls.domainserver.DomainServerImpl;
import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.middleware.types.MetaClass;
import Sirius.server.middleware.types.MetaObject;
import Sirius.server.middleware.types.MetaObjectNode;
import Sirius.server.newuser.User;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.aedsicad.aaaweb.service.util.Buchungsblatt;
import de.aedsicad.aaaweb.service.util.Buchungsstelle;
import de.aedsicad.aaaweb.service.util.LandParcel;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.net.URL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import de.cismet.cids.custom.utils.berechtigungspruefung.DownloadInfoFactory;
import de.cismet.cids.custom.utils.berechtigungspruefung.baulastbescheinigung.BerechtigungspruefungBescheinigungBaulastInfo;
import de.cismet.cids.custom.utils.berechtigungspruefung.baulastbescheinigung.BerechtigungspruefungBescheinigungDownloadInfo;
import de.cismet.cids.custom.utils.berechtigungspruefung.baulastbescheinigung.BerechtigungspruefungBescheinigungFlurstueckInfo;
import de.cismet.cids.custom.utils.berechtigungspruefung.baulastbescheinigung.BerechtigungspruefungBescheinigungGruppeInfo;
import de.cismet.cids.custom.utils.berechtigungspruefung.baulastbescheinigung.BerechtigungspruefungBescheinigungInfo;
import de.cismet.cids.custom.wunda_blau.search.actions.BaulastBescheinigungReportServerAction;
import de.cismet.cids.custom.wunda_blau.search.actions.BaulastenReportServerAction;
import de.cismet.cids.custom.wunda_blau.search.actions.ServerAlkisSoapAction;
import de.cismet.cids.custom.wunda_blau.search.server.BaulastSearchInfo;
import de.cismet.cids.custom.wunda_blau.search.server.CidsBaulastSearchStatement;
import de.cismet.cids.custom.wunda_blau.search.server.FlurstueckInfo;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.server.actions.ServerActionParameter;
import de.cismet.cids.server.search.CidsServerSearch;

import de.cismet.commons.security.handler.SimpleHttpAccessHandler;

import de.cismet.connectioncontext.ConnectionContext;
import de.cismet.connectioncontext.ConnectionContextStore;
import java.io.File;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class BaulastBescheinigungHelper {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(BaulastBescheinigungHelper.class);
    private static final Map<String, MetaClass> METACLASS_CACHE = new HashMap();

    //~ Instance fields --------------------------------------------------------

    private final ConnectionContext connectionContext;
    private final MetaService metaService;
    private final User user;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new BaulastBescheinigungHelper object.
     *
     * @param  user               DOCUMENT ME!
     * @param  metaService        DOCUMENT ME!
     * @param  connectionContext  DOCUMENT ME!
     */
    public BaulastBescheinigungHelper(final User user,
            final MetaService metaService,
            final ConnectionContext connectionContext) {
        this.user = user;
        this.metaService = metaService;
        this.connectionContext = connectionContext;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param   flurstuecke                           DOCUMENT ME!
     * @param   flurstueckeToBaulastenBelastetMap     DOCUMENT ME!
     * @param   flurstueckeToBaulastenBeguenstigtMap  DOCUMENT ME!
     * @param   protocolBuffer                        DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public void fillFlurstueckeToBaulastenMaps(final Collection<CidsBean> flurstuecke,
            final Map<CidsBean, Collection<CidsBean>> flurstueckeToBaulastenBelastetMap,
            final Map<CidsBean, Collection<CidsBean>> flurstueckeToBaulastenBeguenstigtMap,
            final ProtocolBuffer protocolBuffer) throws Exception {
        protocolBuffer.appendLine("\n===");

        // belastete Baulasten pro Flurstück
        flurstueckeToBaulastenBelastetMap.putAll(createFlurstueckeToBaulastenMap(flurstuecke, true, protocolBuffer));

        // begünstigte Baulasten pro Flurstück
        flurstueckeToBaulastenBeguenstigtMap.putAll(createFlurstueckeToBaulastenMap(
                flurstuecke,
                false,
                protocolBuffer));
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
     * @return  DOCUMENT ME!
     */
    public User getUser() {
        return user;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   serverSearch  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    protected Collection executeSearch(final CidsServerSearch serverSearch) throws Exception {
        final Map localServers = new HashMap<>();
        localServers.put("WUNDA_BLAU", getMetaService());
        serverSearch.setActiveLocalServers(localServers);
        serverSearch.setUser(getUser());
        if (serverSearch instanceof ConnectionContextStore) {
            ((ConnectionContextStore)serverSearch).initWithConnectionContext(getConnectionContext());
        }
        return serverSearch.performServerSearch();
    }

    /**
     * DOCUMENT ME!
     *
     * @param   oid  DOCUMENT ME!
     * @param   cid  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    protected MetaObject getMetaObject(final int oid, final int cid) throws Exception {
        return DomainServerImpl.getServerInstance().getMetaObject(getUser(), oid, cid, getConnectionContext());
    }
    /**
     * DOCUMENT ME!
     *
     * @param   query  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    protected MetaObject[] getMetaObjects(final String query) throws Exception {
        return DomainServerImpl.getServerInstance().getMetaObject(getUser(), query, getConnectionContext());
    }

    /**
     * DOCUMENT ME!
     *
     * @param   tableName  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    protected MetaClass getMetaClass(final String tableName) {
        if (!METACLASS_CACHE.containsKey(tableName)) {
            MetaClass mc = null;
            try {
                mc = CidsBean.getMetaClassFromTableName("WUNDA_BLAU", tableName, connectionContext);
            } catch (final Exception ex) {
                LOG.error("could not get metaclass of " + tableName, ex);
            }
            METACLASS_CACHE.put(tableName, mc);
        }
        return METACLASS_CACHE.get(tableName);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   flurstuecke     DOCUMENT ME!
     * @param   belastet        DOCUMENT ME!
     * @param   protocolBuffer  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private Map<CidsBean, Set<CidsBean>> createFlurstueckeToBaulastenMap(final Collection<CidsBean> flurstuecke,
            final boolean belastet,
            final ProtocolBuffer protocolBuffer) throws Exception {
        final String queryBeguenstigt = "SELECT %d, alb_baulast.%s \n"
                    + "FROM alb_baulast_flurstuecke_beguenstigt, alb_baulast, alb_flurstueck_kicker, flurstueck \n"
                    + "WHERE alb_baulast.id = alb_baulast_flurstuecke_beguenstigt.baulast_reference \n"
                    + "AND alb_baulast_flurstuecke_beguenstigt.flurstueck = alb_flurstueck_kicker.id \n"
                    + "AND alb_flurstueck_kicker.fs_referenz = flurstueck.id \n"
                    + "AND flurstueck.alkis_id ilike '%s' \n"
                    + "AND alb_baulast.geschlossen_am is null AND alb_baulast.loeschungsdatum is null";

        final String queryBelastet = "SELECT %d, alb_baulast.%s \n"
                    + "FROM alb_baulast_flurstuecke_belastet, alb_baulast, alb_flurstueck_kicker, flurstueck \n"
                    + "WHERE alb_baulast.id = alb_baulast_flurstuecke_belastet.baulast_reference \n"
                    + "AND alb_baulast_flurstuecke_belastet.flurstueck = alb_flurstueck_kicker.id \n"
                    + "AND alb_flurstueck_kicker.fs_referenz = flurstueck.id \n"
                    + "AND flurstueck.alkis_id ilike '%s' \n"
                    + "AND alb_baulast.geschlossen_am is null AND alb_baulast.loeschungsdatum is null";

        final MetaClass mcBaulast = getMetaClass("alb_baulast");

        final String query = belastet ? queryBelastet : queryBeguenstigt;

        protocolBuffer.appendLine("\nSuche der " + ((belastet) ? "belastenden" : "begünstigenden") + " Baulasten von:");
        final Map<CidsBean, Set<CidsBean>> flurstueckeToBaulastenMap = new HashMap<>();
        for (final CidsBean flurstueck : flurstuecke) {
            protocolBuffer.appendLine(" * Flurstück: " + flurstueck + " ...");
            final Set<CidsBean> baulasten = new HashSet<>();
            try {
                final BaulastSearchInfo searchInfo = new BaulastSearchInfo();
                final Integer gemarkung = Integer.parseInt(((String)flurstueck.getProperty("alkis_id")).substring(
                            2,
                            6));
                final String flur = (String)flurstueck.getProperty("flur");
                final String zaehler = Integer.toString(Integer.parseInt(
                            (String)flurstueck.getProperty("fstck_zaehler")));
                final String nenner = (flurstueck.getProperty("fstck_nenner") == null)
                    ? "0" : Integer.toString(Integer.parseInt((String)flurstueck.getProperty("fstck_nenner")));

                final FlurstueckInfo fsi = new FlurstueckInfo(gemarkung, flur, zaehler, nenner);
                searchInfo.setFlurstuecke(Arrays.asList(fsi));
                searchInfo.setResult(CidsBaulastSearchStatement.Result.BAULAST);
                searchInfo.setBelastet(belastet);
                searchInfo.setBeguenstigt(!belastet);
                searchInfo.setBlattnummer("");
                searchInfo.setArt("");
                final CidsBaulastSearchStatement search = new CidsBaulastSearchStatement(
                        searchInfo,
                        mcBaulast.getId(),
                        -1);

                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }
                final Collection<MetaObjectNode> mons = executeSearch(search);
                for (final MetaObjectNode mon : mons) {
                    final MetaObject mo = getMetaObject(mon.getObjectId(), mon.getClassId());
                    if ((mo.getBean() != null) && (mo.getBean() != null)
                                && (mo.getBean().getProperty("loeschungsdatum") != null)) {
                        continue;
                    }
                    if (mon.getName().startsWith("indirekt: ")) {
                        throw new BaBeException(
                            "Zu den angegebenen Flurstücken kann aktuell keine Baulastauskunft erteilt werden, da sich einige der enthaltenen Baulasten im Bearbeitungszugriff befinden.");
                    }
                }

                final String alkisId = (String)flurstueck.getProperty("alkis_id");

                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }
                final MetaObject[] mos = getMetaObjects(String.format(
                            query,
                            mcBaulast.getID(),
                            mcBaulast.getPrimaryKey(),
                            alkisId));
                for (final MetaObject mo : mos) {
                    final CidsBean baulast = mo.getBean();
                    final Boolean geprueft = (Boolean)baulast.getProperty("geprueft");
                    if ((geprueft == null) || (geprueft == false)) {
                        throw new BaBeException(
                            "Zu den angegebenen Flurstücken kann aktuell keine Baulastauskunft erteilt werden, da sich einige der enthaltenen Baulasten im Bearbeitungszugriff befinden.");
                    }
                    protocolBuffer.appendLine("   => Baulast: " + baulast);
                    baulasten.add(baulast);
                }
                flurstueckeToBaulastenMap.put(flurstueck, baulasten);
            } catch (final Exception ex) {
                LOG.fatal(ex, ex);
            }
        }
        return flurstueckeToBaulastenMap;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   bescheinigungInfo  grundstueckeToFlurstueckeMap flurstuecke flurstueckeToBaulastengrundstueckMap
     *                             DOCUMENT ME!
     * @param   protocolBuffer     DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public HashMap<String, Integer> createBilling(final BerechtigungspruefungBescheinigungInfo bescheinigungInfo,
            final ProtocolBuffer protocolBuffer) {
        final List<String> keys = new ArrayList<>();
        for (final BerechtigungspruefungBescheinigungGruppeInfo gruppeInfo
                    : bescheinigungInfo.getBescheinigungsgruppen()) {
            keys.add(gruppeInfo.getName());
        }
        Collections.sort(keys);

        final int anzahlGrundstuecke = bescheinigungInfo.getBescheinigungsgruppen().size();
        if (anzahlGrundstuecke == 1) {
            protocolBuffer.appendLine("\n===\n\nBescheinigungsart des Grundstücks:");
        } else {
            protocolBuffer.appendLine("\n===\n\nBescheinigungsarten der " + anzahlGrundstuecke
                        + " ermittelten Grundstücke:");
        }

        int anzahlNegativ = 0;
        int anzahlPositiv1 = 0;
        int anzahlPositiv2 = 0;
        int anzahlPositiv3 = 0;

        for (final BerechtigungspruefungBescheinigungGruppeInfo gruppeInfo
                    : bescheinigungInfo.getBescheinigungsgruppen()) {
            final Set<BerechtigungspruefungBescheinigungBaulastInfo> baulastInfos = new HashSet<>();
            baulastInfos.addAll(gruppeInfo.getBaulastenBelastet());
            baulastInfos.addAll(gruppeInfo.getBaulastenBeguenstigt());

            final StringBuffer sb = new StringBuffer();
            boolean first = true;
            for (final BerechtigungspruefungBescheinigungBaulastInfo baulastInfo : baulastInfos) {
                if (first) {
                    first = false;
                } else {
                    sb.append(", ");
                }
                sb.append(baulastInfo);
            }

            final String baulastenString = sb.toString();
            final int numOfBaulasten = gruppeInfo.getBaulastenBelastet().size()
                        + gruppeInfo.getBaulastenBeguenstigt().size();
            switch (numOfBaulasten) {
                case 0: {
                    protocolBuffer.appendLine(" * Grundstück " + gruppeInfo.getName()
                                + " => Negativ-Bescheinigung");
                    anzahlNegativ++;
                }
                break;
                case 1: {
                    protocolBuffer.appendLine(" * Grundstück " + gruppeInfo.getName()
                                + " => Positiv-Bescheinigung für eine Baulast (" + baulastenString + ")");
                    anzahlPositiv1++;
                }
                break;
                case 2: {
                    protocolBuffer.appendLine(" * Grundstück " + gruppeInfo.getName()
                                + " => Positiv-Bescheinigung für zwei Baulasten (" + baulastenString + ")");
                    anzahlPositiv2++;
                }
                break;
                default: {
                    protocolBuffer.appendLine(" * Grundstück " + gruppeInfo.getName()
                                + " => Positiv-Bescheinigung für drei oder mehr Baulasten (" + baulastenString + ")");
                    anzahlPositiv3++;
                }
                break;
            }
        }

        final HashMap<String, Integer> prodAmounts = new HashMap<>();
        if (anzahlNegativ > 0) {
            if (anzahlNegativ > 10) {
                prodAmounts.put("ea_blab_neg_ab_10", 1);
            } else {
                prodAmounts.put("ea_blab_neg", anzahlNegativ);
            }
        }
        if (anzahlPositiv1 > 0) {
            prodAmounts.put("ea_blab_pos_1", anzahlPositiv1);
        }
        if (anzahlPositiv2 > 0) {
            prodAmounts.put("ea_blab_pos_2", anzahlPositiv2);
        }
        if (anzahlPositiv3 > 0) {
            prodAmounts.put("ea_blab_pos_3", anzahlPositiv3);
        }
        return prodAmounts;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   flurstueckeToGrundstueckeMap          flurstuecke DOCUMENT ME!
     * @param   flurstueckeToBaulastenBeguenstigtMap  DOCUMENT ME!
     * @param   flurstueckeToBaulastenBelastetMap     DOCUMENT ME!
     * @param   protocolBuffer                        DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Set<BerechtigungspruefungBescheinigungGruppeInfo> createBescheinigungsGruppen(
            final Map<CidsBean, Collection<String>> flurstueckeToGrundstueckeMap,
            final Map<CidsBean, Collection<CidsBean>> flurstueckeToBaulastenBeguenstigtMap,
            final Map<CidsBean, Collection<CidsBean>> flurstueckeToBaulastenBelastetMap,
            final ProtocolBuffer protocolBuffer) {
        final Map<String, BerechtigungspruefungBescheinigungGruppeInfo> gruppeMap = new HashMap<>();

        final List<CidsBean> flurstuecke = new ArrayList<>(flurstueckeToGrundstueckeMap.keySet());
        Collections.sort(flurstuecke, new Comparator<CidsBean>() {

                @Override
                public int compare(final CidsBean o1, final CidsBean o2) {
                    final String s1 = (o1 == null) ? "" : (String)o1.getProperty("alkis_id");
                    final String s2 = (o2 == null) ? "" : (String)o2.getProperty("alkis_id");
                    return s1.compareTo(s2);
                }
            });

        for (final CidsBean flurstueck : flurstuecke) {
            final Collection<CidsBean> baulastenBeguenstigt = flurstueckeToBaulastenBeguenstigtMap.get(flurstueck);
            final Collection<CidsBean> baulastenBelastet = flurstueckeToBaulastenBelastetMap.get(flurstueck);
            final BerechtigungspruefungBescheinigungGruppeInfo newGruppe = DownloadInfoFactory
                        .createBerechtigungspruefungBescheinigungGruppeInfo(
                            flurstueckeToGrundstueckeMap.get(flurstueck).iterator().next(),
                            flurstueckeToGrundstueckeMap,
                            baulastenBeguenstigt,
                            baulastenBelastet);
            final String gruppeKey = newGruppe.toString();
            if (!gruppeMap.containsKey(gruppeKey)) {
                gruppeMap.put(gruppeKey, newGruppe);
            }
            final BerechtigungspruefungBescheinigungGruppeInfo gruppe = gruppeMap.get(gruppeKey);
            gruppe.getFlurstuecke()
                    .add(DownloadInfoFactory.createBerechtigungspruefungBescheinigungFlurstueckInfo(
                            flurstueck,
                            flurstueckeToGrundstueckeMap.get(flurstueck)));
        }

        final Set<BerechtigungspruefungBescheinigungGruppeInfo> bescheinigungsgruppen = new HashSet<>(
                gruppeMap.values());

        protocolBuffer.appendLine("\n===\n\nAnzahl Bescheinigungsgruppen: " + bescheinigungsgruppen.size());
        for (final BerechtigungspruefungBescheinigungGruppeInfo gruppe : bescheinigungsgruppen) {
            protocolBuffer.appendLine(" * " + gruppe.toString());
        }
        return bescheinigungsgruppen;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   flurstuecke                   DOCUMENT ME!
     * @param   grundstueckeToFlurstueckeMap  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Map<CidsBean, Collection<String>> createFlurstueckeToGrundstueckeMap(
            final Collection<CidsBean> flurstuecke,
            final Map<String, Collection<CidsBean>> grundstueckeToFlurstueckeMap) {
        final HashMap<CidsBean, Collection<String>> flurstueckeToGrundstueckeMap = new HashMap<>();

        for (final String grundstueck : grundstueckeToFlurstueckeMap.keySet()) {
            final Collection<CidsBean> gruFlu = grundstueckeToFlurstueckeMap.get(grundstueck);
            for (final CidsBean flurstueck : flurstuecke) {
                if (gruFlu.contains(flurstueck)) {
                    if (!flurstueckeToGrundstueckeMap.containsKey(flurstueck)) {
                        flurstueckeToGrundstueckeMap.put(flurstueck, new HashSet<String>());
                    }
                    final Collection<String> grundstuecke = flurstueckeToGrundstueckeMap.get(flurstueck);
                    grundstuecke.add(grundstueck);
                }
            }
        }
        return flurstueckeToGrundstueckeMap;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   flurstuecke     DOCUMENT ME!
     * @param   protocolBuffer  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public void prepareFlurstuecke(final List<CidsBean> flurstuecke, final ProtocolBuffer protocolBuffer)
            throws Exception {
        protocolBuffer.appendLine("Baulastbescheinigungs-Protokoll für "
                    + ((flurstuecke.size() == 1) ? "folgendes Flurstück" : "folgende Flurstücke") + ":");

        Collections.sort(flurstuecke, new Comparator<CidsBean>() {

                @Override
                public int compare(final CidsBean o1, final CidsBean o2) {
                    final String s1 = (o1 == null) ? "" : (String)o1.getProperty("alkis_id");
                    final String s2 = (o2 == null) ? "" : (String)o2.getProperty("alkis_id");
                    return s1.compareTo(s2);
                }
            });

        for (final CidsBean flurstueck : flurstuecke) {
            protocolBuffer.appendLine(" * " + flurstueck);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   flurstuecke     DOCUMENT ME!
     * @param   protocolBuffer  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception             DOCUMENT ME!
     * @throws  InterruptedException  DOCUMENT ME!
     */
    public Map<String, Collection<CidsBean>> createGrundstueckeToFlurstueckeMap(
            final Collection<CidsBean> flurstuecke,
            final ProtocolBuffer protocolBuffer) throws Exception {
        protocolBuffer.appendLine("\n===\n\nZuordnung der Flurstücke zu Grundstücken...");

        final Map<String, Collection<CidsBean>> grundstueckeToFlurstueckeMap = new HashMap<>();

        for (final CidsBean flurstueckBean : flurstuecke) {
            final List<CidsBean> buchungsblaetter = new ArrayList<>(flurstueckBean.getBeanCollectionProperty(
                        "buchungsblaetter"));
            if (buchungsblaetter.size() == 1) {
                protocolBuffer.appendLine("Flurstück: " + flurstueckBean + " (1 Buchungsblatt):");
            } else {
                protocolBuffer.appendLine("Flurstück: " + flurstueckBean + " (" + buchungsblaetter.size()
                            + " Buchungsblätter):");
            }
            Collections.sort(buchungsblaetter, new Comparator<CidsBean>() {

                    @Override
                    public int compare(final CidsBean o1, final CidsBean o2) {
                        final String s1 = (o1 == null) ? "" : (String)o1.getProperty("buchungsblattcode");
                        final String s2 = (o2 == null) ? "" : (String)o2.getProperty("buchungsblattcode");
                        return s1.compareTo(s2);
                    }
                });

//            boolean teileigentumAlreadyCounted = false;
            boolean grundstueckFound = false;
            for (final CidsBean buchungsblattBean : buchungsblaetter) {
                if (grundstueckFound) {
                    break; // we are done
                }
                if (buchungsblattBean != null) {
                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedException();
                    }
                    protocolBuffer.appendLine(" * analysiere Buchungsblatt " + buchungsblattBean + " ..");
                    final Buchungsblatt buchungsblatt = getBuchungsblatt(buchungsblattBean);

                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedException();
                    }
                    final List<Buchungsstelle> buchungsstellen = Arrays.asList(buchungsblatt.getBuchungsstellen());
                    Collections.sort(buchungsstellen, new Comparator<Buchungsstelle>() {

                            @Override
                            public int compare(final Buchungsstelle o1, final Buchungsstelle o2) {
                                final String s1 = (o1 == null) ? "" : o1.getSequentialNumber();
                                final String s2 = (o2 == null) ? "" : o2.getSequentialNumber();
                                return s1.compareTo(s2);
                            }
                        });

                    for (final Buchungsstelle buchungsstelle : buchungsstellen) {
                        if (grundstueckFound) {
                            break; // we are done
                        }
                        if (Thread.currentThread().isInterrupted()) {
                            throw new InterruptedException();
                        }
                        boolean flurstueckPartOfStelle = false;
                        final LandParcel[] landparcels = AlkisProducts.getLandparcelFromBuchungsstelle(
                                buchungsstelle);
                        if (landparcels != null) {
                            for (final LandParcel lp : landparcels) {
                                if (((String)flurstueckBean.getProperty("alkis_id")).equals(
                                                lp.getLandParcelCode())) {
                                    flurstueckPartOfStelle = true;
                                    break;
                                }
                            }
                        }
                        if (flurstueckPartOfStelle) {
                            final String[] bbc = buchungsblatt.getBuchungsblattCode().split("-");
                            final String gemarkungsnummer = bbc[0].substring(2).trim();
                            final String buchungsblattnummer = bbc[1].trim();
                            final MetaClass mcGemarkung = getMetaClass("gemarkung");

                            final String pruefungQuery = "SELECT " + mcGemarkung.getID()
                                        + ", " + mcGemarkung.getTableName() + "." + mcGemarkung.getPrimaryKey() + " "
                                        + "FROM " + mcGemarkung.getTableName() + " "
                                        + "WHERE " + mcGemarkung.getTableName() + ".gemarkungsnummer = "
                                        + Integer.parseInt(gemarkungsnummer) + " "
                                        + "LIMIT 1;";
                            final MetaObject[] mos = getMetaObjects(pruefungQuery);

                            final String key;
                            if ((mos != null) && (mos.length > 0)) {
                                final CidsBean gemarkung = mos[0].getBean();
                                key = gemarkung.getProperty("name") + " "
                                            + Integer.parseInt(buchungsblattnummer.substring(0, 5))
                                            + buchungsblattnummer.substring(5) + " / "
                                            + Integer.parseInt(buchungsstelle.getSequentialNumber());
                            } else {
                                key = "[" + gemarkungsnummer + "] "
                                            + Integer.parseInt(buchungsblattnummer.substring(0, 5))
                                            + buchungsblattnummer.substring(5) + " / "
                                            + Integer.parseInt(buchungsstelle.getSequentialNumber());
                            }

                            final String buchungsart = buchungsstelle.getBuchungsart();
                            if ("Erbbaurecht".equals(buchungsart)) {
                                protocolBuffer.appendLine("   -> ignoriere \"" + key + "\" aufgrund der Buchungsart ("
                                            + buchungsart + ")");
                                continue;
                            }

                            if (!grundstueckeToFlurstueckeMap.containsKey(key)) {
                                grundstueckeToFlurstueckeMap.put(key, new HashSet<CidsBean>());
                            }

                            final String buchungsartSuffix = "Grundstück".equals(buchungsart)
                                ? "" : (" (" + buchungsart + ")");
                            protocolBuffer.appendLine("   => füge Flurstück " + flurstueckBean + " zu Grundstück \""
                                        + key + "\" hinzu" + buchungsartSuffix);
                            grundstueckeToFlurstueckeMap.get(key).add(flurstueckBean);
                            grundstueckFound = true;
                        }
                    }
                }
            }
        }

        return grundstueckeToFlurstueckeMap;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   bescheinigungsgruppen  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static List<BerechtigungspruefungBescheinigungGruppeInfo> getSortedBescheinigungsGruppen(
            final Collection<BerechtigungspruefungBescheinigungGruppeInfo> bescheinigungsgruppen) {
        final List<BerechtigungspruefungBescheinigungGruppeInfo> sortedBescheinigungsGruppen = new ArrayList<>(
                bescheinigungsgruppen);
        Collections.sort(
            sortedBescheinigungsGruppen,
            new Comparator<BerechtigungspruefungBescheinigungGruppeInfo>() {

                @Override
                public int compare(final BerechtigungspruefungBescheinigungGruppeInfo o1,
                        final BerechtigungspruefungBescheinigungGruppeInfo o2) {
                    final String alkisId1 = o1.getFlurstuecke().iterator().next().getAlkisId();
                    final String alkisId2 = o2.getFlurstuecke().iterator().next().getAlkisId();
                    return alkisId1.compareTo(alkisId2);
                }
            });
        return sortedBescheinigungsGruppen;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   info  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private CidsBean loadBaulast(final BerechtigungspruefungBescheinigungBaulastInfo info) throws Exception {
        final String query = "SELECT %d, id "
                    + "FROM alb_baulast "
                    + "WHERE blattnummer ILIKE '%s' "
                    + "AND laufende_nummer ILIKE '%s'";
        final MetaClass mcBaulast = getMetaClass("alb_baulast");
        final MetaObject[] mos = getMetaObjects(String.format(
                    query,
                    mcBaulast.getID(),
                    info.getBlattnummer(),
                    info.getLaufende_nummer()));
        if ((mos != null) && (mos.length > 0)) {
            return mos[0].getBean();
        } else {
            return null;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   protocol  DOCUMENT ME!
     * @param   zipOut    DOCUMENT ME!
     *
     * @throws  IOException  DOCUMENT ME!
     */
    public void writeProcotol(final String protocol, final ZipOutputStream zipOut) throws IOException {
        writeToZip("baulastbescheinigung_protokoll.txt", IOUtils.toInputStream(protocol, "UTF-8"), zipOut);
    }

    /**
     * DOCUMENT ME!
     *
     * @param  downloadInfo  DOCUMENT ME!
     * @param  transId       DOCUMENT ME!
     */
    public void writeFullBescheinigung(final BerechtigungspruefungBescheinigungDownloadInfo downloadInfo, final File file) {
        try(final ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(file))) {
            writeProcotol(downloadInfo.getProtokoll(), zipOut);
            if (downloadInfo.getBescheinigungsInfo() != null) {
                final Set<CidsBean> allBaulasten = new HashSet<>();
                int number = 0;
                for (final BerechtigungspruefungBescheinigungGruppeInfo bescheinigungsGruppeInfo
                            : getSortedBescheinigungsGruppen(
                                downloadInfo.getBescheinigungsInfo().getBescheinigungsgruppen())) {
                    writeBescheinigungReport(
                        bescheinigungsGruppeInfo,
                        downloadInfo.getAuftragsnummer(),
                        downloadInfo.getAuftragsnummer(),
                        downloadInfo.getProduktbezeichnung(),
                        ++number,
                        zipOut);

                    for (final BerechtigungspruefungBescheinigungBaulastInfo baulastInfo
                                : bescheinigungsGruppeInfo.getBaulastenBelastet()) {
                        allBaulasten.add(loadBaulast(baulastInfo));
                    }
                    for (final BerechtigungspruefungBescheinigungBaulastInfo baulastInfo
                                : bescheinigungsGruppeInfo.getBaulastenBeguenstigt()) {
                        allBaulasten.add(loadBaulast(baulastInfo));
                    }
                }

                if (!allBaulasten.isEmpty()) {
                    writeBaulastenReports(
                        BaulastenReportGenerator.Type.TEXTBLATT_PLAN_RASTER,
                        allBaulasten,
                        downloadInfo.getAuftragsnummer(),
                        downloadInfo.getProduktbezeichnung(),
                        zipOut);
                    writeAdditionalFiles(allBaulasten, zipOut);
                }
            }
            zipOut.close();
        } catch (final Exception ex) {
            LOG.fatal(ex, ex);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   fileName  DOCUMENT ME!
     * @param   in        DOCUMENT ME!
     * @param   zipOut    DOCUMENT ME!
     *
     * @throws  IOException  DOCUMENT ME!
     */
    private void writeToZip(final String fileName, final InputStream in, final ZipOutputStream zipOut)
            throws IOException {
        final byte[] buf = new byte[1024];
        int len;
        zipOut.putNextEntry(new ZipEntry(fileName));
        while ((len = in.read(buf)) > 0) {
            zipOut.write(buf, 0, len);
        }
        zipOut.flush();
    }

    /**
     * DOCUMENT ME!
     *
     * @param   baulasten  DOCUMENT ME!
     * @param   zipOut     DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private void writeAdditionalFiles(final Collection<CidsBean> baulasten, final ZipOutputStream zipOut)
            throws Exception {
        for (final URL url : BaulastenPictureFinder.getInstance().findAdditionalFiles(baulasten)) {
            writeToZip(url.getFile().substring(url.getFile().lastIndexOf('/') + 1),
                new SimpleHttpAccessHandler().doRequest(url),
                zipOut);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   bescheinigungsGruppeInfo  DOCUMENT ME!
     * @param   auftragsNummer            DOCUMENT ME!
     * @param   projectName               DOCUMENT ME!
     * @param   anfrageSchluessel         DOCUMENT ME!
     * @param   number                    DOCUMENT ME!
     * @param   zipOut                    DOCUMENT ME!
     *
     * @throws  JsonProcessingException  DOCUMENT ME!
     * @throws  IOException              DOCUMENT ME!
     */
    private void writeBescheinigungReport(final BerechtigungspruefungBescheinigungGruppeInfo bescheinigungsGruppeInfo,
            final String auftragsNummer,
            final String projectName,
            final String anfrageSchluessel,
            final int number,
            final ZipOutputStream zipOut) throws JsonProcessingException, IOException {
        final Collection<BerechtigungspruefungBescheinigungFlurstueckInfo> fls =
            bescheinigungsGruppeInfo.getFlurstuecke();
        final ServerActionParameter[] saps = new ServerActionParameter[] {
                new ServerActionParameter<>(
                    BaulastBescheinigungReportServerAction.Parameter.BESCHEINIGUNGGRUPPE_INFO.toString(),
                    new ObjectMapper().writeValueAsString(bescheinigungsGruppeInfo)),
                new ServerActionParameter<>(
                    BaulastBescheinigungReportServerAction.Parameter.FABRICATION_DATE.toString(),
                    new Date().getTime()),
                /*new ServerActionParameter<>(
                 *  BaulastBescheinigungReportServerAction.Parameter.FERTIGUNGS_VERMERK.toString(), ""),*/
                new ServerActionParameter<>(
                    BaulastBescheinigungReportServerAction.Parameter.JOB_NUMBER.toString(),
                    auftragsNummer),
                new ServerActionParameter<>(
                    BaulastBescheinigungReportServerAction.Parameter.PROJECT_NAME.toString(),
                    projectName),
                new ServerActionParameter<>(
                    BaulastBescheinigungReportServerAction.Parameter.ANFRAGE_SCHLUESSEL.toString(),
                    anfrageSchluessel),
            };

        final BaulastBescheinigungReportServerAction serverAction = new BaulastBescheinigungReportServerAction();
        serverAction.setMetaService(getMetaService());
        serverAction.setUser(getUser());
        serverAction.initWithConnectionContext(getConnectionContext());

        writeToZip("bescheinigung_" + fls.iterator().next().getAlkisId().replace("/", "--")
                    + ((fls.size() > 1) ? ".ua" : "")
                    + "_" + number,
            new ByteArrayInputStream((byte[])serverAction.execute(null, saps)),
            zipOut);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   type               DOCUMENT ME!
     * @param   selectedBaulasten  DOCUMENT ME!
     * @param   jobNumber          DOCUMENT ME!
     * @param   projectName        DOCUMENT ME!
     * @param   zipOut             DOCUMENT ME!
     *
     * @throws  IOException  DOCUMENT ME!
     */
    private void writeBaulastenReports(final BaulastenReportGenerator.Type type,
            final Collection<CidsBean> selectedBaulasten,
            final String jobNumber,
            final String projectName,
            final ZipOutputStream zipOut) throws IOException {
        final Collection<MetaObjectNode> mons = new ArrayList<>();
        for (final CidsBean baulastBean : selectedBaulasten) {
            mons.add(new MetaObjectNode(baulastBean));
        }
        final ServerActionParameter[] saps = new ServerActionParameter[] {
                new ServerActionParameter<>(
                    BaulastenReportServerAction.Parameter.BAULASTEN_MONS.toString(),
                    mons),
                /*new ServerActionParameter<>(
                 *  BaulastenReportServerAction.Parameter.FERTIGUNGS_VERMERK.toString(), ""),*/
                new ServerActionParameter<>(
                    BaulastenReportServerAction.Parameter.JOB_NUMBER.toString(),
                    jobNumber),
                new ServerActionParameter<>(
                    BaulastenReportServerAction.Parameter.PROJECT_NAME.toString(),
                    projectName),
                new ServerActionParameter<>(
                    BaulastenReportServerAction.Parameter.TYPE.toString(),
                    type),
            };

        final BaulastenReportServerAction serverAction = new BaulastenReportServerAction();
        serverAction.setMetaService(getMetaService());
        serverAction.setUser(getUser());
        serverAction.initWithConnectionContext(getConnectionContext());

        writeToZip("baulasten.pdf", new ByteArrayInputStream((byte[])serverAction.execute(null, saps)), zipOut);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   buchungsblattBean  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    protected Buchungsblatt getBuchungsblatt(final CidsBean buchungsblattBean) throws Exception {
        final String buchungsblattcode = String.valueOf(buchungsblattBean.getProperty("buchungsblattcode"));
        if ((buchungsblattcode != null) && (buchungsblattcode.length() > 5)) {
            final ServerActionParameter buchungsblattCodeSAP = new ServerActionParameter<>(
                    ServerAlkisSoapAction.RETURN_VALUE.BUCHUNGSBLATT.toString(),
                    buchungsblattcode);
            final Object body = ServerAlkisSoapAction.RETURN_VALUE.BUCHUNGSBLATT;

            return (Buchungsblatt)new ServerAlkisSoapAction().execute(body, buchungsblattCodeSAP);
        }
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   auftragsnummer      DOCUMENT ME!
     * @param   produktBezeichnung  DOCUMENT ME!
     * @param   flurstuecke         DOCUMENT ME!
     * @param   protocolBuffer      DOCUMENT ME!
     * @param   statusHolder        DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public BerechtigungspruefungBescheinigungDownloadInfo calculateDownloadInfo(final String auftragsnummer,
            final String produktBezeichnung,
            final List<CidsBean> flurstuecke,
            final BaulastBescheinigungHelper.ProtocolBuffer protocolBuffer,
            final BaulastBescheinigungHelper.StatusHolder statusHolder) throws Exception {
        statusHolder.setMessage("Bescheinigung wird vorbereitet...");
        prepareFlurstuecke(flurstuecke, protocolBuffer);

        statusHolder.setMessage("Buchungsblätter werden analysiert...");
        final Map<String, Collection<CidsBean>> grundstueckeToFlurstueckeMap = createGrundstueckeToFlurstueckeMap(
                flurstuecke,
                protocolBuffer);

        statusHolder.setMessage("Baulasten werden gesucht...");
        final Map<CidsBean, Collection<CidsBean>> flurstueckeToBaulastenBelastetMap = new HashMap<>();
        final Map<CidsBean, Collection<CidsBean>> flurstueckeToBaulastenBeguenstigtMap = new HashMap<>();
        fillFlurstueckeToBaulastenMaps(
            flurstuecke,
            flurstueckeToBaulastenBelastetMap,
            flurstueckeToBaulastenBeguenstigtMap,
            protocolBuffer);

        statusHolder.setMessage("Bescheinigungsgruppen werden identifiziert...");
        final Collection<BerechtigungspruefungBescheinigungGruppeInfo> bescheinigungsgruppen =
            createBescheinigungsGruppen(
                createFlurstueckeToGrundstueckeMap(
                    flurstuecke,
                    grundstueckeToFlurstueckeMap),
                flurstueckeToBaulastenBeguenstigtMap,
                flurstueckeToBaulastenBelastetMap,
                protocolBuffer);

        statusHolder.setMessage("Gebühr wird berechnet...");
        final BerechtigungspruefungBescheinigungInfo bescheinigungInfo = new BerechtigungspruefungBescheinigungInfo(
                new Date(),
                new HashSet<>(bescheinigungsgruppen));
        final HashMap<String, Integer> prodAmounts = createBilling(bescheinigungInfo, protocolBuffer);

        return new BerechtigungspruefungBescheinigungDownloadInfo(
                auftragsnummer,
                produktBezeichnung,
                protocolBuffer.toString(),
                bescheinigungInfo,
                prodAmounts);
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public static class BaBeException extends Exception {

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new BaBeException object.
         *
         * @param  message  DOCUMENT ME!
         */
        public BaBeException(final String message) {
            super(message);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public static class StatusHolder {

        //~ Instance fields ----------------------------------------------------

        private String message;

        //~ Methods ------------------------------------------------------------

        /**
         * DOCUMENT ME!
         *
         * @param  message  DOCUMENT ME!
         */
        public void setMessage(final String message) {
            this.message = message;
        }

        /**
         * DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         */
        public String getMessage() {
            return message;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public static class ProtocolBuffer {

        //~ Instance fields ----------------------------------------------------

        private final StringBuffer buffer = new StringBuffer();

        //~ Methods ------------------------------------------------------------

        /**
         * DOCUMENT ME!
         *
         * @param   string  DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         */
        public BaulastBescheinigungHelper.ProtocolBuffer appendLine(final String string) {
            buffer.append(string).append("\n");
            return this;
        }

        @Override
        public String toString() {
            return buffer.toString();
        }
    }
}
