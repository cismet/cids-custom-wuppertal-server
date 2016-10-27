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
     * @param  produktTyp        DOCUMENT ME!
     * @param  alkisObjectTyp    DOCUMENT ME!
     * @param  alkisDownloadTyp  DOCUMENT ME!
     * @param  alkisCodes        DOCUMENT ME!
     * @param  billingId         DOCUMENT ME!
     */
    public BerechtigungspruefungAlkisDownloadInfo(@JsonProperty("produktTyp") final String produktTyp,
            @JsonProperty("alkisObjectTyp") final AlkisObjektTyp alkisObjectTyp,
            @JsonProperty("alkisDownloadTyp") final AlkisDownloadTyp alkisDownloadTyp,
            @JsonProperty("alkisCodes") final List<String> alkisCodes,
            @JsonProperty("billingId") final Integer billingId) {
        this(alkisObjectTyp, alkisDownloadTyp, alkisCodes, billingId);
    }

    /**
     * Creates a new BerechtigungspruefungAlkisDownloadInfo object.
     *
     * @param  alkisObjectTyp    DOCUMENT ME!
     * @param  alkisDownloadTyp  DOCUMENT ME!
     * @param  alkisCodes        DOCUMENT ME!
     * @param  billingId         DOCUMENT ME!
     */
    protected BerechtigungspruefungAlkisDownloadInfo(
            final AlkisObjektTyp alkisObjectTyp,
            final AlkisDownloadTyp alkisDownloadTyp,
            final List<String> alkisCodes,
            final Integer billingId) {
        super(PRODUKT_TYP, billingId);
        this.alkisObjectTyp = alkisObjectTyp;
        this.alkisDownloadTyp = alkisDownloadTyp;
        this.alkisCodes = alkisCodes;
    }
}
