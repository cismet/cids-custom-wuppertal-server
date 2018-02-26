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
package de.cismet.cids.custom.utils.vermessungsunterlagen;

import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.middleware.types.LightweightMetaObject;
import Sirius.server.middleware.types.MetaClass;
import Sirius.server.middleware.types.MetaObjectNode;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import de.cismet.cids.custom.utils.vermessungsunterlagen.exceptions.VermessungsunterlagenException;
import de.cismet.cids.custom.utils.vermessungsunterlagen.exceptions.VermessungsunterlagenValidatorException;
import de.cismet.cids.custom.wunda_blau.search.server.AlbFlurstueckKickerLightweightSearch;
import de.cismet.cids.custom.wunda_blau.search.server.BufferingGeosearch;
import de.cismet.cids.custom.wunda_blau.search.server.CidsAlkisSearchStatement;
import de.cismet.cids.custom.wunda_blau.search.server.KundeByVermessungsStellenNummerSearch;
import de.cismet.cids.custom.wunda_blau.search.server.VermessungFlurstueckKickerLightweightSearch;

import de.cismet.cids.dynamics.CidsBean;
import de.cismet.cids.server.connectioncontext.ServerConnectionContext;

import de.cismet.cids.server.search.CidsServerSearch;
import de.cismet.cids.server.search.SearchException;
import de.cismet.cids.server.connectioncontext.ServerConnectionContextProvider;

/**
 * DOCUMENT ME!
 *
 * @version  $Revision$, $Date$
 */
public class VermessungsunterlagenValidator implements ServerConnectionContextProvider {

    //~ Static fields/initializers ---------------------------------------------

    public static final String CONTACT = " E-Mail: geodatenzentrum@stadt.wuppertal.de  Tel.: +49 202 563 5399 ";
    public static final int MAX_PNR_PRO_KM = 100;
    public static final int MAX_SAUM = 999;

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Error {

        //~ Enum constants -----------------------------------------------------

        NO_GESCHAEFTSBUCHNUMMER, NO_ANTRAGSFLURSTUECK, WRONG_ANTRAGSFLURSTUECK, UNKNOWN_ANTRAGSFLURSTUECK,
        UNSUFFICENT_PNR, WRONG_PNR, NO_SAUM, WRONG_SAUM, NO_ART, WRONG_GEBIET
    }

    //~ Instance fields --------------------------------------------------------

    @Getter private final Collection<CidsBean> flurstuecke = new ArrayList<>();
    @Getter private boolean vermessungsstelleKnown = false;
    @Getter private final VermessungsunterlagenHelper helper;
    @Getter private boolean ignoreError = false;
    @Getter private boolean pnrNotZero = false;
    @Getter private boolean geometryFromFlurstuecke = true;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new VermessungsunterlagenValidator object.
     *
     * @param  helper  DOCUMENT ME!
     */
    public VermessungsunterlagenValidator(final VermessungsunterlagenHelper helper) {
        this.helper = helper;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param   anfrageBean  DOCUMENT ME!
     *
     * @return  if validation passed returns {@link #ISVALID}. If validation failed returns error message for enduser in
     *          plain text
     *
     * @throws  VermessungsunterlagenException           DOCUMENT ME!
     * @throws  VermessungsunterlagenValidatorException  DOCUMENT ME!
     */
    public boolean validateAndGetErrorMessage(final VermessungsunterlagenAnfrageBean anfrageBean)
            throws VermessungsunterlagenException {
        // Prüfung ob Benutzername des ÖBVIs in Wuppertal bekannt (registriert) ist
        vermessungsstelleKnown = isVermessungsstelleKnown(anfrageBean.getZulassungsnummerVermessungsstelle());

        // Validierung der Geschäftsbuchnummer
        // Geschäftsbuchnummer darf nicht leer sein. Keine weitere Prüfung auf Länge oder Inhalt.
        if ((anfrageBean.getGeschaeftsbuchnummer() == null)
                    || "".equals(anfrageBean.getGeschaeftsbuchnummer().trim())) {
            throw getExceptionByErrorCode(Error.NO_GESCHAEFTSBUCHNUMMER);
        }

        ignoreError = (anfrageBean.getAktenzeichenKatasteramt() != null)
                    && anfrageBean.getAktenzeichenKatasteramt().startsWith("[i_e]");

        // Validierung der Punktnummernreservierung
        if ((anfrageBean.getPunktnummernreservierungsArray() != null)
                    && (anfrageBean.getPunktnummernreservierungsArray().length > 0)) {
            for (final VermessungsunterlagenAnfrageBean.PunktnummernreservierungBean pnrOvject
                        : anfrageBean.getPunktnummernreservierungsArray()) {
                if ((pnrOvject.getAnzahlPunktnummern() == null) || (pnrOvject.getUtmKilometerQuadrat() == null)
                            || (pnrOvject.getAnzahlPunktnummern() == null)) {
                    throw getExceptionByErrorCode(Error.UNSUFFICENT_PNR);
                }
                if ((pnrOvject.getAnzahlPunktnummern() < 0) || (pnrOvject.getAnzahlPunktnummern() > MAX_PNR_PRO_KM)) {
                    // Die Anzahl der zu reservierenden Punkte muss größer, gleich 0 und kleiner einer maximalen
                    // Anzahl pro Kilometerquadrat sein
                    throw getExceptionByErrorCode(Error.WRONG_PNR);
                }
                if ((pnrOvject.getAnzahlPunktnummern() != null) && (pnrOvject.getAnzahlPunktnummern() > 0)) {
                    pnrNotZero = true;
                }
            }
        }

        // Validierung der "Art der Vermessung"
        if ((anfrageBean.getArtderVermessung() == null) || (anfrageBean.getArtderVermessung().length <= 0)) {
            throw getExceptionByErrorCode(Error.NO_ART);
        }

        // Wenn ausschließlich neue Punktnummern reserviert werden sollen, ist keine weitere Überprüfung notwendig.
        if (anfrageBean.getNurPunktnummernreservierung()) {
            return true;
        }

        // Validierung der übergebenen Flurstücke
        if ((anfrageBean.getAntragsflurstuecksArray() == null)
                    || (anfrageBean.getAntragsflurstuecksArray().length <= 0)) {
            // es wurde kein Flurstück übergeben
            throw getExceptionByErrorCode(Error.NO_ANTRAGSFLURSTUECK);
        }

        final Collection<VermessungsunterlagenAnfrageBean.AntragsflurstueckBean> valideFlurstuecke = new ArrayList<>();
        final Collection<VermessungsunterlagenAnfrageBean.AntragsflurstueckBean> wuppFlurstuecke = new ArrayList<>();

        for (final VermessungsunterlagenAnfrageBean.AntragsflurstueckBean antragsFlurstueck
                    : anfrageBean.getAntragsflurstuecksArray()) {
            if (isFlurstueckValide(antragsFlurstueck)) {
                valideFlurstuecke.add(antragsFlurstueck);
            }
            if (isWuppGemarkung(antragsFlurstueck)) {
                wuppFlurstuecke.add(antragsFlurstueck);
            }
        }

        if (valideFlurstuecke.isEmpty()) {
            throw getExceptionByErrorCode(Error.NO_ANTRAGSFLURSTUECK);
        }

        geometryFromFlurstuecke = !wuppFlurstuecke.isEmpty();

        // keine wuppertale Flurstücke
        if (wuppFlurstuecke.isEmpty()) {
            final Polygon[] polygonArray = anfrageBean.getAnfragepolygonArray();
            // Validierung des Vermessungsgebiets
            if ((polygonArray == null) || (polygonArray.length <= 0) || (polygonArray[0] == null)) {
                // es wurde kein Flurstück übergeben
                throw getExceptionByErrorCode(Error.WRONG_GEBIET);
            }
            final Polygon polygon = polygonArray[0];

            try {
                final Geometry geom = polygon.getGeometryN(0);
                geom.setSRID(VermessungsunterlagenHelper.SRID);
                // TODO geom verschneiden zum suchen von flurstücken
                final Collection<CidsBean> flurstuecke = searchFlurstuecke(geom);
                for (final CidsBean flurstueck : flurstuecke) {
                    final String[] alkisParts = ((String)flurstueck.getProperty("alkis_id")).split("-");
                    final VermessungsunterlagenAnfrageBean.AntragsflurstueckBean wuppFlurstueck =
                        new VermessungsunterlagenAnfrageBean.AntragsflurstueckBean();
                    wuppFlurstueck.setGemarkungsID(alkisParts[0]);
                    wuppFlurstueck.setFlurID(alkisParts[1]);
                    wuppFlurstueck.setFlurstuecksID(alkisParts[2]);
                    wuppFlurstuecke.add(wuppFlurstueck);
                }
            } catch (Exception ex) {
                throw new VermessungsunterlagenValidatorException(
                    "Fehler beim laden der Flurstücke für das Vermessungsgebiet.",
                    ex);
            }
        }

        // jedes einzelne Flurstück Überprüfen
        for (final VermessungsunterlagenAnfrageBean.AntragsflurstueckBean wuppFlurstueck : wuppFlurstuecke) {
            // gemarkung flur flurstueck darf nicht leer sein
            if (!isFlurstueckValide(wuppFlurstueck)) {
                throw getExceptionByErrorCode(Error.WRONG_ANTRAGSFLURSTUECK);
            }

            final String alkisId = getAlkisId(wuppFlurstueck);
            final Collection<CidsBean> flurstuecke = getWuppFlurstuecke(alkisId);
            final boolean isWuppFlurstueckExisting = (flurstuecke != null) && !flurstuecke.isEmpty();

            if (!isWuppFlurstueckExisting) {
                // Jedes Flurstück muss auffindbar sein
                throw getExceptionByErrorCode(Error.UNKNOWN_ANTRAGSFLURSTUECK);
            }

            this.flurstuecke.addAll(flurstuecke);
        }

        // Validierung des Saums
        if ((anfrageBean.getSaumAPSuche() == null) || "".equals(anfrageBean.getSaumAPSuche().trim())) {
            throw getExceptionByErrorCode(Error.NO_SAUM);
        }
        // BAD CODE WARNING
        int iSaum = -1;
        try {
            if (!anfrageBean.getSaumAPSuche().contains("e") && !anfrageBean.getSaumAPSuche().contains("E")) {
                // Saum muss eine Zahl sein
                iSaum = Integer.parseInt(anfrageBean.getSaumAPSuche());
            }
        } catch (final Exception e) {
        }
        // Saum muss ganze zahl zwischen 0 und MAX_SAUM sein
        if ((iSaum < 0) || (iSaum > MAX_SAUM)) {
            throw getExceptionByErrorCode(Error.WRONG_SAUM);
        }
        return true;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   fs  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private boolean isFlurstueckValide(final VermessungsunterlagenAnfrageBean.AntragsflurstueckBean fs) {
        if (fs == null) {
            return false;
        }
        final String gemarkung = fs.getGemarkungsID();
        final String flur = fs.getFlurID();
        final String zaehlernenner = fs.getFlurstuecksID();
        return (gemarkung != null) && !gemarkung.trim().isEmpty()
                    && (flur != null) && !flur.trim().isEmpty()
                    && (zaehlernenner != null) && !zaehlernenner.trim().isEmpty();
    }

    /**
     * Get Errormessage by ErrorCode.
     *
     * @param   code  DOCUMENT ME!
     *
     * @return  Error message plain text
     */
    private static VermessungsunterlagenException getExceptionByErrorCode(final Error code) {
        final String message;
        switch (code) {
            // leere Geschäftsbuchnummer
            case NO_GESCHAEFTSBUCHNUMMER: {
                message =
                    "Die angegebene Geschäftsbuchnummer ist leer. Bitte geben Sie eine gültige Geschäftsbuchnummer an.";
            }
            break;
            // gar kein Flurstück
            case NO_ANTRAGSFLURSTUECK: {
                message =
                    "Es wurde kein gültiges Antragsflurstück übergeben. Geben Sie mindestens ein gültiges Flurstück an.";
            }
            break;
            // Mindestens ein unvollständiges
            case WRONG_ANTRAGSFLURSTUECK: {
                message =
                    "Die Anfrage enthält mindestens eine unvollständige Gemarkung-, Flur- oder Flurstückseingabe.";
            }
            break;
            // Mindestens ein unvollständiges
            case UNKNOWN_ANTRAGSFLURSTUECK: {
                message =
                    "Die Anfrage enthält mindestens eine unbekannte oder vor der ALKIS Einführung historisierte Flurstücksnummer.";
            }
            break;
            // negative oder sehr große Anzahl an Punkten reserviert
            case WRONG_PNR: {
                message =
                    "Die Anzahl der zu reservierenden Punkte muss pro Kilometerquadrat muss größer oder gleich 0 und kleiner als "
                            + MAX_PNR_PRO_KM
                            + " sein.";
            }
            break;
            // kein Saum angegeben
            case NO_SAUM: {
                message = "Es wurde kein gültiger Saum angegeben.";
            }
            break;
            // ungueltiger Saum angegeben
            case WRONG_SAUM: {
                message = "Der angegebene Saum ist ungültig. Nur ganzzahlige Eingaben zwischen 0 und "
                            + MAX_SAUM
                            + " sind gültig.";
            }
            break;
            // keine Art der Vermessung angegeben
            case NO_ART: {
                message = "Es wurde keine oder keine gültige Art der Vermessung angegeben.";
            }
            break;
            // negative oder sehr große Anzahl an Punkten reserviert
            case UNSUFFICENT_PNR: {
                message = "Die Angaben zur Punktnummernreservierung sind unvollständig.";
            }
            break;
            // negative oder sehr große Anzahl an Punkten reserviert
            case WRONG_GEBIET: {
                message = "Es wurde kein oder kein gültiges Vermessungsgebiet angegeben.";
            }
            break;
            default: {
                message = "";
            }
        }
        return new VermessungsunterlagenValidatorException(message);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   vermessungsstelle  usernameVermPortal DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  VermessungsunterlagenException  DOCUMENT ME!
     */
    private boolean isVermessungsstelleKnown(final String vermessungsstelle) throws VermessungsunterlagenException {
        if ("053290".equals(vermessungsstelle)) {
            return true;
        } else {
            try {
                final CidsServerSearch search = new KundeByVermessungsStellenNummerSearch(
                        vermessungsstelle.startsWith("05") ? vermessungsstelle.substring(2) : vermessungsstelle);
                helper.performSearch(search);
                final Collection res = search.performServerSearch();
                return ((res != null) && !res.isEmpty());
            } catch (final SearchException ex) {
                return false;
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   flurstueckBean  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private boolean isWuppGemarkung(final VermessungsunterlagenAnfrageBean.AntragsflurstueckBean flurstueckBean) {
        if ((flurstueckBean == null) || (flurstueckBean.getGemarkungsID() == null)) {
            return false;
        }

        try {
            final int gemarkungId = Integer.parseInt(flurstueckBean.getGemarkungsID().trim().substring(2));
            if (gemarkungId == 0) {
                return false;
            }

            final VermessungFlurstueckKickerLightweightSearch search =
                new VermessungFlurstueckKickerLightweightSearch();
            search.setSearchFor(VermessungFlurstueckKickerLightweightSearch.SearchFor.GEMARKUNG);
            search.setGemarkungsnummer(Integer.toString(gemarkungId));
            search.setRepresentationFields(new String[] {});
            final Collection<LightweightMetaObject> lwmos = helper.performSearch(search);
            return !lwmos.isEmpty();
        } catch (final Exception ex) {
            return false;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   flurstueckBean  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String getAlkisId(final VermessungsunterlagenAnfrageBean.AntragsflurstueckBean flurstueckBean) {
        if (flurstueckBean == null) {
            return null;
        }
        final String alkisId;

        try {
            final String zaehlernenner = flurstueckBean.getFlurstuecksID();
            final String zaehler;
            final String nenner;
            if (zaehlernenner.contains("/")) {
                final String[] split = zaehlernenner.split("/");
                if (split.length != 2) {
                    return null;
                }
                zaehler = split[0];
                nenner = split[1];
            } else {
                zaehler = zaehlernenner;
                nenner = "0";
            }
            alkisId = toAlkisId(
                    flurstueckBean.getGemarkungsID(),
                    Integer.valueOf(flurstueckBean.getFlurID()),
                    Integer.valueOf(zaehler),
                    Integer.valueOf(nenner));
        } catch (final Exception ex) {
            return null;
        }
        return alkisId;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   alkisId  flurstueckBean DOCUMENT ME!
     *
     * @return  true if WuNDa contains a flurstueck defined by gemarkung, flur, flurstuecksnummer. Also true if the
     *          containing flurstueck ist historic!
     *
     * @throws  VermessungsunterlagenException           Exception DOCUMENT ME!
     * @throws  VermessungsunterlagenValidatorException  DOCUMENT ME!
     */
    private Collection<CidsBean> getWuppFlurstuecke(final String alkisId) throws VermessungsunterlagenException {
        if (alkisId == null) {
            return null;
        }
        try {
            final Collection<CidsBean> flurstuecke = new ArrayList<>();
            final CidsBean fsCidsBean = searchFlurstueck(alkisId);
            if (fsCidsBean != null) {
                for (final CidsBean aktuell : getAktuelle(fsCidsBean)) {
                    final CidsAlkisSearchStatement alkisSearch = new CidsAlkisSearchStatement(
                            CidsAlkisSearchStatement.Resulttyp.FLURSTUECK,
                            CidsAlkisSearchStatement.SucheUeber.FLURSTUECKSNUMMER,
                            (String)aktuell.getProperty("alkis_id"),
                            null);

                    final Collection<MetaObjectNode> mons = helper.performSearch(alkisSearch);
                    for (final MetaObjectNode mon : mons) {
                        final CidsBean alkisBean = helper.loadCidsBean(mon);
                        flurstuecke.add(alkisBean);
                    }
                }
            }
            return flurstuecke;
        } catch (final Exception ex) {
            throw new VermessungsunterlagenValidatorException("Fehler beim laden des Flurstuecks: " + alkisId, ex);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   fsCidsBean  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private Collection<CidsBean> getAktuelle(final CidsBean fsCidsBean) throws Exception {
        final Collection<CidsBean> aktuelle = new ArrayList<CidsBean>();
        if (fsCidsBean != null) {
            if (fsCidsBean.getProperty("historisch") == null) {
                aktuelle.add(fsCidsBean);
            } else {
                for (final CidsBean nachfolger : getNachfolger(fsCidsBean)) {
                    aktuelle.addAll(getAktuelle(nachfolger));
                }
            }
        }
        return aktuelle;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   flurstueck  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private Collection<CidsBean> getNachfolger(final CidsBean flurstueck) throws Exception {
        final List<CidsBean> nachfolgerBeans = new ArrayList<CidsBean>();
        final String query = "SELECT flurstueckskennzeichen_neu FROM lookup_alkis_ffn "
                    + "WHERE ffn LIKE '"
                    + (String)flurstueck.getProperty("fortfuehrungsnummer")
                    + "' "
                    + "AND flurstueckskennzeichen_alt LIKE '"
                    + ((String)flurstueck.getProperty("alkis_id")).replace("-", "")
                    + "_%';";

        final MetaService metaService = helper.getMetaService();
        for (final ArrayList fields : metaService.performCustomSearch(query, getServerConnectionContext())) {
            final String kennzeichen = (String)fields.get(0);
            final CidsBean nachfolgerBean = searchFlurstueck(toAlkisId(
                        kennzeichen.substring(0, 6),
                        Integer.valueOf(kennzeichen.substring(6, 9)),
                        Integer.valueOf(kennzeichen.substring(9, 14)),
                        Integer.valueOf(kennzeichen.substring(14).replaceAll("_", "0"))));
            if (nachfolgerBean != null) {
                nachfolgerBeans.add(nachfolgerBean);
            }
        }
        return nachfolgerBeans;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   gemarkung  DOCUMENT ME!
     * @param   flur       DOCUMENT ME!
     * @param   zaehler    DOCUMENT ME!
     * @param   nenner     DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String toAlkisId(final String gemarkung, final int flur, final int zaehler, final int nenner) {
        final String nf = String.format("%04d", nenner);
        return gemarkung
                    + "-"
                    + String.format("%03d", flur)
                    + "-"
                    + String.format("%05d", zaehler)
                    + ((!nf.equals("0000")) ? ("/" + nf) : "");
    }

    /**
     * DOCUMENT ME!
     *
     * @param   geom  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private Collection<CidsBean> searchFlurstuecke(final Geometry geom) throws Exception {
        final BufferingGeosearch search = new BufferingGeosearch();
        search.setGeometry(geom);
        final MetaClass mc = CidsBean.getMetaClassFromTableName("WUNDA_BLAU", "alkis_landparcel");
        search.setValidClasses(Arrays.asList(mc));
        final Collection<MetaObjectNode> mons = helper.performSearch(search);

        final Collection<CidsBean> flurstuecke = new ArrayList<>();
        for (final MetaObjectNode mon : mons) {
            flurstuecke.add(helper.loadCidsBean(mon));
        }
        return flurstuecke;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   alkisId  gemarkung DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private CidsBean searchFlurstueck(final String alkisId) throws Exception {
        final String zaehlernenner = alkisId.substring(11);
        final String zaehler;
        final String nenner;
        if (zaehlernenner.contains("/")) {
            final String[] split = zaehlernenner.split("/");
            zaehler = split[0];
            nenner = split[1];
        } else {
            zaehler = zaehlernenner;
            nenner = null;
        }

        final String[] parts = VermessungsunterlagenUtils.createFlurstueckParts(alkisId.substring(0, 6),
                alkisId.substring(7, 10),
                zaehler,
                nenner);
        if (parts == null) {
            return null;
        }

        final AlbFlurstueckKickerLightweightSearch search = new AlbFlurstueckKickerLightweightSearch();
        search.setSearchFor(AlbFlurstueckKickerLightweightSearch.SearchFor.FLURSTUECK);
        search.setGemarkungsnummer(parts[0]);
        search.setFlur(parts[1]);
        search.setZaehler(parts[2]);
        search.setNenner(parts[3]);
        search.setRepresentationFields(new String[] { "id", "gemarkung", "flur", "zaehler", "nenner" });
        final Collection<LightweightMetaObject> result = helper.performSearch(search);
        if ((result != null) && !result.isEmpty()) {
            final CidsBean cidsBean = helper.loadCidsBean(result.iterator().next());
            final CidsBean fsCidsBean = (CidsBean)cidsBean.getProperty("fs_referenz");
            return fsCidsBean;
        } else {
            return null;
        }
    }
    
    @Override
    public ServerConnectionContext getServerConnectionContext() {
        return ServerConnectionContext.create(VermessungsunterlagenValidator.class.getSimpleName());
    }
    
}
