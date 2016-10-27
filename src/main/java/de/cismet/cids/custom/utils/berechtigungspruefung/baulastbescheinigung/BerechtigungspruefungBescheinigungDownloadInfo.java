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
package de.cismet.cids.custom.utils.berechtigungspruefung.baulastbescheinigung;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

import de.cismet.cids.custom.utils.berechtigungspruefung.BerechtigungspruefungBillingDownloadInfo;

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
public class BerechtigungspruefungBescheinigungDownloadInfo extends BerechtigungspruefungBillingDownloadInfo {

    //~ Static fields/initializers ---------------------------------------------

    public static String PRODUKT_TYP = "baulastbescheinigung";

    //~ Instance fields --------------------------------------------------------

    @JsonProperty private final String jobname;
    @JsonProperty private final String produktbezeichnung;
    @JsonProperty private final String auftragsnummer;
    @JsonProperty private final String protokoll;
    @JsonProperty private final BerechtigungspruefungBescheinigungInfo bescheinigungsInfo;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new BerechtigungspruefungBescheinigungDownloadInfo object.
     *
     * @param  jobname             DOCUMENT ME!
     * @param  produktbezeichnung  DOCUMENT ME!
     * @param  auftragsnummer      DOCUMENT ME!
     * @param  protokoll           DOCUMENT ME!
     * @param  bescheinigungsInfo  DOCUMENT ME!
     * @param  billingId           DOCUMENT ME!
     */
    public BerechtigungspruefungBescheinigungDownloadInfo(
            final String jobname,
            final String produktbezeichnung,
            final String auftragsnummer,
            final String protokoll,
            final BerechtigungspruefungBescheinigungInfo bescheinigungsInfo,
            final Integer billingId) {
        this(PRODUKT_TYP, jobname, produktbezeichnung, auftragsnummer, protokoll, bescheinigungsInfo, billingId);
    }

    /**
     * Creates a new BescheinigungsDownloadInfo object.
     *
     * @param  produktTyp          DOCUMENT ME!
     * @param  jobname             DOCUMENT ME!
     * @param  produktbezeichnung  DOCUMENT ME!
     * @param  auftragsnummer      DOCUMENT ME!
     * @param  protokoll           DOCUMENT ME!
     * @param  bescheinigungsInfo  DOCUMENT ME!
     * @param  billingId           DOCUMENT ME!
     */
    public BerechtigungspruefungBescheinigungDownloadInfo(@JsonProperty("produktTyp") final String produktTyp,
            @JsonProperty("jobname") final String jobname,
            @JsonProperty("produktbezeichnung") final String produktbezeichnung,
            @JsonProperty("auftragsnummer") final String auftragsnummer,
            @JsonProperty("protokoll") final String protokoll,
            @JsonProperty("bescheinigungsInfo") final BerechtigungspruefungBescheinigungInfo bescheinigungsInfo,
            @JsonProperty("billingId") final Integer billingId) {
        super(PRODUKT_TYP, billingId);
        this.jobname = jobname;
        this.produktbezeichnung = produktbezeichnung;
        this.auftragsnummer = auftragsnummer;
        this.protokoll = protokoll;
        this.bescheinigungsInfo = bescheinigungsInfo;
    }
}
