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

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
@Getter
public class BerechtigungspruefungBillingDownloadInfo extends BerechtigungspruefungDownloadInfo {

    //~ Instance fields --------------------------------------------------------

    @JsonProperty private final Integer billingId;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new BerechtigungspruefungFreigabeInfo object.
     *
     * @param  produktTyp  DOCUMENT ME!
     * @param  billingId   DOCUMENT ME!
     */
    public BerechtigungspruefungBillingDownloadInfo(@JsonProperty("produktTyp") final String produktTyp,
            @JsonProperty("billingId") final Integer billingId) {
        super(produktTyp);
        this.billingId = billingId;
    }
}
