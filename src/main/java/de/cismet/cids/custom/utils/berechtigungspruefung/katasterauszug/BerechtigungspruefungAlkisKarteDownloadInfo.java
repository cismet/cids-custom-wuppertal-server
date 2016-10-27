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
package de.cismet.cids.custom.utils.berechtigungspruefung.katasterauszug;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

import java.util.List;

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
public class BerechtigungspruefungAlkisKarteDownloadInfo extends BerechtigungspruefungAlkisDownloadInfo {

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new BerechtigungspruefungBescheinigungDownloadInfo object.
     *
     * @param  alkisObjectTyp  produktbezeichnung DOCUMENT ME!
     * @param  alkisCodes      auftragsnummer DOCUMENT ME!
     * @param  billingId       DOCUMENT ME!
     */
    public BerechtigungspruefungAlkisKarteDownloadInfo(
            final AlkisObjektTyp alkisObjectTyp,
            final List<String> alkisCodes,
            final Integer billingId) {
        super(PRODUKT_TYP, alkisObjectTyp, AlkisDownloadTyp.KARTE, alkisCodes, billingId);
    }

    /**
     * Creates a new BerechtigungspruefungAlkisKarteDownloadInfo object.
     *
     * @param  produktTyp        DOCUMENT ME!
     * @param  alkisObjectTyp    DOCUMENT ME!
     * @param  alkisDownloadTyp  DOCUMENT ME!
     * @param  alkisCodes        DOCUMENT ME!
     * @param  billingId         DOCUMENT ME!
     */
    public BerechtigungspruefungAlkisKarteDownloadInfo(@JsonProperty("produktTyp") final String produktTyp,
            @JsonProperty("alkisObjectTyp") final AlkisObjektTyp alkisObjectTyp,
            @JsonProperty("alkisDownloadTyp") final AlkisDownloadTyp alkisDownloadTyp,
            @JsonProperty("alkisCodes") final List<String> alkisCodes,
            @JsonProperty("billingId") final Integer billingId) {
        this(alkisObjectTyp, alkisCodes, billingId);
    }
}
