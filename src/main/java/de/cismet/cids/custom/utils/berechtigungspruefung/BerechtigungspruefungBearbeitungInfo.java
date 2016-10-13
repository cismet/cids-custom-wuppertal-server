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
package de.cismet.cids.custom.utils.berechtigungspruefung;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

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
public class BerechtigungspruefungBearbeitungInfo {

    //~ Instance fields --------------------------------------------------------

    @JsonProperty private final String schluessel;
    @JsonProperty private final String pruefer;
    @JsonProperty private final Boolean status;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new BerechtigungspruefungInfo object.
     *
     * @param  schluessel  kommentar DOCUMENT ME!
     * @param  pruefer     freigegeben DOCUMENT ME!
     * @param  status      berechtigungspruefungDownloadInfo DOCUMENT ME!
     */
    public BerechtigungspruefungBearbeitungInfo(@JsonProperty("schluessel") final String schluessel,
            @JsonProperty("pruefer") final String pruefer,
            @JsonProperty("bearbeitung") final Boolean status) {
        this.schluessel = schluessel;
        this.pruefer = pruefer;
        this.status = status;
    }
}
