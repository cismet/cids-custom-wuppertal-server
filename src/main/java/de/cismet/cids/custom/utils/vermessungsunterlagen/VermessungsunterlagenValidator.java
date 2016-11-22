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

import Sirius.server.middleware.types.MetaObjectNode;

import java.util.ArrayList;
import java.util.Collection;

import de.cismet.cids.custom.wunda_blau.search.server.CidsAlkisSearchStatement;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.server.search.CidsServerSearch;

/**
 * DOCUMENT ME!
 *
 * @version  $Revision$, $Date$
 */
public class VermessungsunterlagenValidator {

    //~ Static fields/initializers ---------------------------------------------

    public static final String CONTACT = " E-Mail: geodatenzentrum@stadt.wuppertal.de  Tel.: +49 202 563 5399 ";
    public static final int MAX_PNR_PRO_KM = 100;
    public static final int MAX_SAUM = 150;

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Error {

        //~ Enum constants -----------------------------------------------------

        UNKOWN_USER, NO_GESCHAEFTSBUCHNUMMER, NO_ANTRAGSFLURSTUECK, WRONG_ANTRAGSFLURSTUECK, UNKNOWN_ANTRAGSFLURSTUECK,
        UNSUFFICENT_PNR, WRONG_PNR, NO_SAUM, WRONG_SAUM, NO_ART, WRONG_GEBIET
    }

    //~ Instance fields --------------------------------------------------------

    private Collection<CidsBean> flurstuecke = new ArrayList<CidsBean>();

    private final VermessungsunterlagenHelper helper;

    private boolean ignoreError = false;

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
     * @return  DOCUMENT ME!
     */
    public Collection<CidsBean> getFlurstuecke() {
        return flurstuecke;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public boolean ignoreError() {
        return ignoreError;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   anfrageBean  DOCUMENT ME!
     *
     * @return  if validation passed returns {@link #ISVALID}. If validation failed returns error message for enduser in
     *          plain text
     *
     * @throws  VermessungsunterlagenException  DOCUMENT ME!
     */
    public boolean validateAndGetErrorMessage(final VermessungsunterlagenAnfrageBean anfrageBean)
            throws VermessungsunterlagenException {
        // Prüfung ob Benutzername des ÖBVIs in Wuppertal bekannt (registriert) ist
        if (!isUserNameVermPortalKnown(anfrageBean.getNameVermessungsstelle())) {
            throw getExceptionByErrorCode(Error.UNKOWN_USER);
        }

        // Validierung der Geschäftsbuchnummer
        // Geschäftsbuchnummer darf nicht leer sein. Keine weitere Prüfung auf Länge oder Inhalt.
        if ((anfrageBean.getGeschaeftsbuchnummer() == null)
                    || "".equals(anfrageBean.getGeschaeftsbuchnummer().trim())) {
            throw getExceptionByErrorCode(Error.NO_GESCHAEFTSBUCHNUMMER);
        }

        ignoreError = anfrageBean.getAktenzeichenKatasteramt().startsWith("[i_e]");

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

        // Validierung des Vermessungsgebiets
        if ((anfrageBean.getAnfragepolygonArray() == null) || (anfrageBean.getAnfragepolygonArray().length <= 0)
                    || (anfrageBean.getAnfragepolygonArray()[0] == null)) {
            // es wurde kein Flurstück übergeben
            throw getExceptionByErrorCode(Error.WRONG_GEBIET);
        }

        // Validierung der übergebenen Flurstücke
        if ((anfrageBean.getAntragsflurstuecksArray() == null)
                    || (anfrageBean.getAntragsflurstuecksArray().length <= 0)) {
            // es wurde kein Flurstück übergeben
            throw getExceptionByErrorCode(Error.NO_ANTRAGSFLURSTUECK);
        }

        // jedes einzelne Flurstück Überprüfen
        for (final VermessungsunterlagenAnfrageBean.AntragsflurstueckBean fs : anfrageBean.getAntragsflurstuecksArray()) {
            // gemarkung flur flurstueck darf nicht leer sein
            if (!isFlurstueckValide(fs)) {
                throw getExceptionByErrorCode(Error.WRONG_ANTRAGSFLURSTUECK);
            }
            // Jedes Flurstück muss auffindbar sein
            if (!existFlurstueck(fs)) {
                throw getExceptionByErrorCode(Error.UNKNOWN_ANTRAGSFLURSTUECK);
            }
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
        } catch (Exception e) {
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
    private VermessungsunterlagenException getExceptionByErrorCode(final Error code) {
        final String message;
        switch (code) {
            // Unbekannter ÖBVI
            case UNKOWN_USER: {
                message =
                    "Unbekannter Benutzername. Eine Ausstellung von Vermessungsunterlagen kann aus Sicherheitsgründen leider erst nach einer Registrierung beim Ressort für Vermessung, Liegenschaftskataster und Geodaten der Stadt Wuppertal erfolgen."
                            // Standard Postfix mit Kontaktdaten
                            + "\n Bitte wenden Sie sich an das Geodatenzentrum der Stadt Wuppertal "
                            + CONTACT
                            + ".";
            }
            break;
            // leere Geschäftsbuchnummer
            case NO_GESCHAEFTSBUCHNUMMER: {
                message =
                    "Die angegebene Geschäftsbuchnummer ist leer. Bitte geben Sie eine gültige Geschäftsbuchnummer an.";
            }
            break;
            // gar kein Flurstück
            case NO_ANTRAGSFLURSTUECK: {
                message = "Es wurde kein Antragsflurstück übergeben. Geben Sie mindestens ein gültiges Flurstück an.";
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
                    "Die Anfrage enthält mindestens ein unbekannte oder vor der ALKIS Einführung historisierte Flurstücksnummer.";
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
                message = "Der angegebene Saum ist ungültig. Nur ganzzahlig Eingaben zwischen 0 und "
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
        return new VermessungsunterlagenException(message);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   usernameVermPortal  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private boolean isUserNameVermPortalKnown(final String usernameVermPortal) {
        // TODO
        return true;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   flurstueckBean  DOCUMENT ME!
     *
     * @return  true if WuNDa contains a flurstueck defined by gemarkung, flur, flurstuecksnummer. Also true if the
     *          containing flurstueck ist historic!
     *
     * @throws  VermessungsunterlagenException  Exception DOCUMENT ME!
     */
    private boolean existFlurstueck(final VermessungsunterlagenAnfrageBean.AntragsflurstueckBean flurstueckBean)
            throws VermessungsunterlagenException {
        if (flurstueckBean == null) {
            return false;
        }
        final String fsKey;
        try {
            final String gemarkung = flurstueckBean.getGemarkungsID();
            final String flur = flurstueckBean.getFlurID();
            final String zaehlernenner = flurstueckBean.getFlurstuecksID();

            final String formattedZN;
            if (zaehlernenner.contains("/")) {
                final String[] split = zaehlernenner.split("/");
                if (split.length != 2) {
                    return false;
                }
                final String zaehler = String.format("%05d", Integer.parseInt(split[0]));
                final String nenner = String.format("%04d", Integer.parseInt(split[1]));
                formattedZN = zaehler
                            + "/"
                            + nenner;
            } else {
                formattedZN = String.format("%05d", Integer.parseInt(zaehlernenner));
            }
            fsKey = gemarkung
                        + "-"
                        + flur
                        + "-"
                        + formattedZN;
        } catch (final Exception ex) {
            return false;
        }

        final CidsServerSearch search = new CidsAlkisSearchStatement(
                CidsAlkisSearchStatement.Resulttyp.FLURSTUECK,
                CidsAlkisSearchStatement.SucheUeber.FLURSTUECKSNUMMER,
                fsKey,
                null);
        try {
            final Collection<MetaObjectNode> result = helper.performSearch(search);
            // TODO recursive search if Flurstueck is historic
            final boolean found = (result != null)
                        && !result.isEmpty();
            if (found) {
                final CidsBean cidsBean = helper.loadCidsBean(result.iterator().next());
                flurstuecke.add(cidsBean);
            }
            return found;
        } catch (final Exception ex) {
            throw new VermessungsunterlagenException("Fehler beim laden des Flurstuecks: " + fsKey, ex);
        }
    }
}
