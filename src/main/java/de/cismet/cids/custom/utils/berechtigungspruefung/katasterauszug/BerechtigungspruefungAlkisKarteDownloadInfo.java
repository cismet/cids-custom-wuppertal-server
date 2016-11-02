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
     */
    public BerechtigungspruefungAlkisKarteDownloadInfo(
            final AlkisObjektTyp alkisObjectTyp,
            final List<String> alkisCodes) {
        this(PRODUKT_TYP, null, null, null, alkisObjectTyp, AlkisDownloadTyp.KARTE, alkisCodes);
    }

    /**
     * Creates a new BerechtigungspruefungAlkisKarteDownloadInfo object.
     *
     * @param  produktTyp          DOCUMENT ME!
     * @param  auftragsnummer      DOCUMENT ME!
     * @param  produktbezeichnung  DOCUMENT ME!
     * @param  billingId           DOCUMENT ME!
     * @param  alkisObjectTyp      DOCUMENT ME!
     * @param  alkisDownloadTyp    DOCUMENT ME!
     * @param  alkisCodes          DOCUMENT ME!
     */
    public BerechtigungspruefungAlkisKarteDownloadInfo(@JsonProperty("produktTyp") final String produktTyp,
            @JsonProperty("auftragsnummer") final String auftragsnummer,
            @JsonProperty("produktbezeichnung") final String produktbezeichnung,
            @JsonProperty("billingId") final Integer billingId,
            @JsonProperty("alkisObjectTyp") final AlkisObjektTyp alkisObjectTyp,
            @JsonProperty("alkisDownloadTyp") final AlkisDownloadTyp alkisDownloadTyp,
            @JsonProperty("alkisCodes") final List<String> alkisCodes) {
        super(
            produktTyp,
            auftragsnummer,
            produktbezeichnung,
            billingId,
            alkisObjectTyp,
            alkisDownloadTyp,
            alkisCodes);
    }
}
