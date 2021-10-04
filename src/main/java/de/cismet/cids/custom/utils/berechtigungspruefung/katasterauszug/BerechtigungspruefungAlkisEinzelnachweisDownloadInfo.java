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

import java.util.Date;
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
public class BerechtigungspruefungAlkisEinzelnachweisDownloadInfo extends BerechtigungspruefungAlkisDownloadInfo {

    //~ Instance fields --------------------------------------------------------

    @JsonProperty private final String alkisProdukt;
    @JsonProperty private final Date date;
    @JsonProperty private final Integer amounts;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new BerechtigungspruefungBescheinigungDownloadInfo object.
     *
     * @param  alkisObjectTyp  produktbezeichnung DOCUMENT ME!
     * @param  alkisProdukt    auftragsnummer DOCUMENT ME!
     * @param  alkisCodes      protokoll DOCUMENT ME!
     */
    public BerechtigungspruefungAlkisEinzelnachweisDownloadInfo(
            final AlkisObjektTyp alkisObjectTyp,
            final String alkisProdukt,
            final List<String> alkisCodes) {
        this(
            PRODUKT_TYP,
            null,
            null,
            null,
            alkisObjectTyp,
            AlkisDownloadTyp.EINZELNACHWEIS,
            alkisCodes,
            alkisProdukt,
            null);
    }

    /**
     * Creates a new BerechtigungspruefungAlkisEinzelnachweisDownloadInfo object.
     *
     * @param  alkisObjectTyp  DOCUMENT ME!
     * @param  alkisProdukt    DOCUMENT ME!
     * @param  stichtag        DOCUMENT ME!
     * @param  alkisCodes      DOCUMENT ME!
     */
    public BerechtigungspruefungAlkisEinzelnachweisDownloadInfo(
            final AlkisObjektTyp alkisObjectTyp,
            final String alkisProdukt,
            final Date stichtag,
            final List<String> alkisCodes) {
        this(
            PRODUKT_TYP,
            null,
            null,
            null,
            alkisObjectTyp,
            AlkisDownloadTyp.EINZELNACHWEIS,
            alkisCodes,
            alkisProdukt,
            stichtag);
    }

    /**
     * Creates a new BerechtigungspruefungAlkisEinzelnachweisDownloadInfo object.
     *
     * @param  produktTyp          DOCUMENT ME!
     * @param  auftragsnummer      DOCUMENT ME!
     * @param  produktbezeichnung  DOCUMENT ME!
     * @param  billingId           DOCUMENT ME!
     * @param  alkisObjectTyp      DOCUMENT ME!
     * @param  alkisDownloadTyp    DOCUMENT ME!
     * @param  alkisCodes          DOCUMENT ME!
     * @param  alkisProdukt        DOCUMENT ME!
     * @param  stichtag            DOCUMENT ME!
     */
    public BerechtigungspruefungAlkisEinzelnachweisDownloadInfo(final String produktTyp,
            final String auftragsnummer,
            final String produktbezeichnung,
            final Integer billingId,
            final AlkisObjektTyp alkisObjectTyp,
            final AlkisDownloadTyp alkisDownloadTyp,
            final List<String> alkisCodes,
            final String alkisProdukt,
            final Date stichtag) {
        this(
            produktTyp,
            auftragsnummer,
            produktbezeichnung,
            billingId,
            alkisObjectTyp,
            alkisDownloadTyp,
            alkisCodes,
            alkisProdukt,
            stichtag,
            null);
    }

    /**
     * Creates a new BerechtigungspruefungAlkisEinzelnachweisDownloadInfo object.
     *
     * @param  produktTyp          DOCUMENT ME!
     * @param  auftragsnummer      DOCUMENT ME!
     * @param  produktbezeichnung  DOCUMENT ME!
     * @param  billingId           DOCUMENT ME!
     * @param  alkisObjectTyp      DOCUMENT ME!
     * @param  alkisDownloadTyp    DOCUMENT ME!
     * @param  alkisCodes          DOCUMENT ME!
     * @param  alkisProdukt        DOCUMENT ME!
     * @param  stichtag            DOCUMENT ME!
     * @param  amounts             DOCUMENT ME!
     */
    public BerechtigungspruefungAlkisEinzelnachweisDownloadInfo(@JsonProperty("produktTyp") final String produktTyp,
            @JsonProperty("auftragsnummer") final String auftragsnummer,
            @JsonProperty("produktbezeichnung") final String produktbezeichnung,
            @JsonProperty("billingId") final Integer billingId,
            @JsonProperty("alkisObjectTyp") final AlkisObjektTyp alkisObjectTyp,
            @JsonProperty("alkisDownloadTyp") final AlkisDownloadTyp alkisDownloadTyp,
            @JsonProperty("alkisCodes") final List<String> alkisCodes,
            @JsonProperty("alkisProdukt") final String alkisProdukt,
            @JsonProperty("stichtag") final Date stichtag,
            @JsonProperty("amounts") final Integer amounts) {
        super(produktTyp, auftragsnummer, produktbezeichnung, billingId, alkisObjectTyp, alkisDownloadTyp, alkisCodes);
        this.alkisProdukt = alkisProdukt;
        this.date = stichtag;
        this.amounts = amounts;
    }
}
