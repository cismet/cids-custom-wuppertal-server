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

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

import java.util.List;

import de.cismet.cids.custom.utils.berechtigungspruefung.BerechtigungspruefungBillingDownloadInfo;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
@Getter
public class BerechtigungspruefungAlkisDownloadInfo extends BerechtigungspruefungBillingDownloadInfo {

    //~ Static fields/initializers ---------------------------------------------

    public static String PRODUKT_TYP = "katasterauszug";

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum AlkisObjektTyp {

        //~ Enum constants -----------------------------------------------------

        FLURSTUECKE, BUCHUNGSBLAETTER
    }
    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum AlkisDownloadTyp {

        //~ Enum constants -----------------------------------------------------

        EINZELNACHWEIS, KARTE
    }

    //~ Instance fields --------------------------------------------------------

    @JsonProperty private final AlkisObjektTyp alkisObjectTyp;
    @JsonProperty private final AlkisDownloadTyp alkisDownloadTyp;
    @JsonProperty private final List<String> alkisCodes;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new BerechtigungspruefungAlkisDownloadInfo object.
     *
     * @param  produktTyp          DOCUMENT ME!
     * @param  auftragsnummer      DOCUMENT ME!
     * @param  produktbezeichnung  DOCUMENT ME!
     * @param  billingId           DOCUMENT ME!
     * @param  alkisObjectTyp      DOCUMENT ME!
     * @param  alkisDownloadTyp    DOCUMENT ME!
     * @param  alkisCodes          DOCUMENT ME!
     */
    public BerechtigungspruefungAlkisDownloadInfo(@JsonProperty("produktTyp") final String produktTyp,
            @JsonProperty("auftragsnummer") final String auftragsnummer,
            @JsonProperty("produktbezeichnung") final String produktbezeichnung,
            @JsonProperty("billingId") final Integer billingId,
            @JsonProperty("alkisObjectTyp") final AlkisObjektTyp alkisObjectTyp,
            @JsonProperty("alkisDownloadTyp") final AlkisDownloadTyp alkisDownloadTyp,
            @JsonProperty("alkisCodes") final List<String> alkisCodes) {
        super(produktTyp, auftragsnummer, produktbezeichnung, billingId);
        this.alkisObjectTyp = alkisObjectTyp;
        this.alkisDownloadTyp = alkisDownloadTyp;
        this.alkisCodes = alkisCodes;
    }

    /**
     * Creates a new BerechtigungspruefungAlkisDownloadInfo object.
     *
     * @param  auftragsnummer      DOCUMENT ME!
     * @param  produktbezeichnung  DOCUMENT ME!
     * @param  billingId           DOCUMENT ME!
     * @param  alkisObjectTyp      DOCUMENT ME!
     * @param  alkisDownloadTyp    DOCUMENT ME!
     * @param  alkisCodes          DOCUMENT ME!
     */
    protected BerechtigungspruefungAlkisDownloadInfo(
            final String auftragsnummer,
            final String produktbezeichnung,
            final Integer billingId,
            final AlkisObjektTyp alkisObjectTyp,
            final AlkisDownloadTyp alkisDownloadTyp,
            final List<String> alkisCodes) {
        this(PRODUKT_TYP, auftragsnummer, produktbezeichnung, billingId, alkisObjectTyp, alkisDownloadTyp, alkisCodes);
    }
}
