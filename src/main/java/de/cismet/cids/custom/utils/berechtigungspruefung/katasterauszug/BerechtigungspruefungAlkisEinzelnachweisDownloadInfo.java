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
        this(alkisObjectTyp, alkisProdukt, null, alkisCodes);
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
        super(PRODUKT_TYP, alkisObjectTyp, AlkisDownloadTyp.EINZELNACHWEIS, alkisCodes);

        this.alkisProdukt = alkisProdukt;
        this.date = stichtag;
    }

    /**
     * Creates a new BerechtigungspruefungAlkisEinzelnachweisDownloadInfo object.
     *
     * @param  produktTyp        DOCUMENT ME!
     * @param  alkisObjectTyp    DOCUMENT ME!
     * @param  alkisDownloadTyp  DOCUMENT ME!
     * @param  alkisProdukt      DOCUMENT ME!
     * @param  stichtag          DOCUMENT ME!
     * @param  alkisCodes        DOCUMENT ME!
     */
    public BerechtigungspruefungAlkisEinzelnachweisDownloadInfo(@JsonProperty("produktTyp") final String produktTyp,
            @JsonProperty("alkisObjectTyp") final AlkisObjektTyp alkisObjectTyp,
            @JsonProperty("alkisDownloadTyp") final AlkisDownloadTyp alkisDownloadTyp,
            @JsonProperty("alkisProdukt") final String alkisProdukt,
            @JsonProperty("stichtag") final Date stichtag,
            @JsonProperty("alkisCodes") final List<String> alkisCodes) {
        this(alkisObjectTyp, alkisProdukt, stichtag, alkisCodes);
    }
}
