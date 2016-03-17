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

/**
 * DOCUMENT ME!
 *
 * @version  $Revision$, $Date$
 */
@Getter
@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE
)
public class BerechtigungspruefungBescheinigungBaulastInfo {

    //~ Instance fields --------------------------------------------------------

    @JsonProperty private final String blattnummer;
    @JsonProperty private final String laufende_nummer;
    @JsonProperty private final String arten;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new BaulastInfo object.
     *
     * @param  blattnummer      DOCUMENT ME!
     * @param  laufende_nummer  DOCUMENT ME!
     * @param  arten            DOCUMENT ME!
     */
    public BerechtigungspruefungBescheinigungBaulastInfo(@JsonProperty("blattnummer") final String blattnummer,
            @JsonProperty("laufende_nummer") final String laufende_nummer,
            @JsonProperty("arten") final String arten) {
        this.blattnummer = blattnummer;
        this.laufende_nummer = laufende_nummer;
        this.arten = arten;
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public String toString() {
        return ((blattnummer != null) ? blattnummer : "kein Baulastbatt") + "/" + ((laufende_nummer != null) ? laufende_nummer : "keine laufende Nummer");
    }
}
