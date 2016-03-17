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
 * @param    <D>
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
public class BerechtigungspruefungFreigabeInfo<D extends BerechtigungspruefungDownloadInfo> {

    //~ Instance fields --------------------------------------------------------

    @JsonProperty private final String kommentar;
    @JsonProperty private final Boolean freigegeben;
    @JsonProperty private final D berechtigungspruefungDownloadInfo;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new BerechtigungspruefungInfo object.
     *
     * @param  kommentar                          DOCUMENT ME!
     * @param  freigegeben                        DOCUMENT ME!
     * @param  berechtigungspruefungDownloadInfo  DOCUMENT ME!
     */
    public BerechtigungspruefungFreigabeInfo(@JsonProperty("kommentar") final String kommentar,
            @JsonProperty("freigegeben") final Boolean freigegeben,
            @JsonProperty("berechtigungspruefungDownloadInfo") final D berechtigungspruefungDownloadInfo) {
        this.kommentar = kommentar;
        this.freigegeben = freigegeben;
        this.berechtigungspruefungDownloadInfo = berechtigungspruefungDownloadInfo;
    }
}
