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
package de.cismet.cids.custom.utils.berechtigungspruefung.baulastbescheinigung;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collection;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
@Getter
@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE
)
public class BerechtigungspruefungBescheinigungFlurstueckInfo {

    //~ Instance fields --------------------------------------------------------

    @JsonProperty private final String alkisId;
    @JsonProperty private final String gemarkung;
    @JsonProperty private final String flur;
    @JsonProperty private final String zaehler;
    @JsonProperty private final String nenner;
    @JsonProperty private final String lage;
    @JsonProperty private final Collection<String> grundstuecke = new ArrayList<String>();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new FlurstueckInfo object.
     *
     * @param  alkisId       DOCUMENT ME!
     * @param  gemarkung     DOCUMENT ME!
     * @param  flur          DOCUMENT ME!
     * @param  zaehler       DOCUMENT ME!
     * @param  nenner        DOCUMENT ME!
     * @param  lage          DOCUMENT ME!
     * @param  grundstuecke  DOCUMENT ME!
     */
    public BerechtigungspruefungBescheinigungFlurstueckInfo(@JsonProperty("alkisId") final String alkisId,
            @JsonProperty("gemarkung") final String gemarkung,
            @JsonProperty("flur") final String flur,
            @JsonProperty("zaehler") final String zaehler,
            @JsonProperty("nenner") final String nenner,
            @JsonProperty("lage") final String lage,
            @JsonProperty("grundstuecke") final Collection<String> grundstuecke) {
        this.alkisId = alkisId;
        this.gemarkung = gemarkung;
        this.flur = flur;
        this.zaehler = zaehler;
        this.nenner = nenner;
        this.lage = lage;
        if (grundstuecke != null) {
            this.grundstuecke.addAll(grundstuecke);
        }
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getNummer() {
        final String nummer = zaehler + ((nenner != null) ? ("/" + nenner) : "");
        return nummer;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getGrundstueckString() {
        if (grundstuecke.isEmpty()) {
            return "-";
        } else {
            final String grundstueck = grundstuecke.iterator().next();
            if (grundstuecke.size() == 1) {
                return grundstueck;
            } else {
                return grundstueck + " u.a.";
            }
        }
    }
}
