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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class BerechtigungspruefungDownloadInfo {

    //~ Instance fields --------------------------------------------------------

    @JsonProperty private final String produktTyp;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new BerechtigungspruefungFreigabeInfo object.
     *
     * @param  produktTyp  DOCUMENT ME!
     */
    public BerechtigungspruefungDownloadInfo(@JsonProperty("produktTyp") final String produktTyp) {
        this.produktTyp = produktTyp;
    }
}
