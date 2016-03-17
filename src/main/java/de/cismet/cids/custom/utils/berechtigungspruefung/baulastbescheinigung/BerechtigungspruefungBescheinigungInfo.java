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

import java.util.Date;
import java.util.Set;

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
public class BerechtigungspruefungBescheinigungInfo {

    //~ Instance fields --------------------------------------------------------

    @JsonProperty private final Date datum;
    @JsonProperty private final Set<BerechtigungspruefungBescheinigungGruppeInfo> bescheinigungsgruppen;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new BescheinigungsInfo object.
     *
     * @param  datum                  DOCUMENT ME!
     * @param  bescheinigungsgruppen  DOCUMENT ME!
     */
    public BerechtigungspruefungBescheinigungInfo(@JsonProperty("datum") final Date datum,
            @JsonProperty("bescheinigungsgruppen") final Set<BerechtigungspruefungBescheinigungGruppeInfo> bescheinigungsgruppen) {
        this.datum = datum;
        this.bescheinigungsgruppen = bescheinigungsgruppen;
    }
}
