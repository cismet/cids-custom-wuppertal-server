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
package de.cismet.cids.custom.utils.formsolutions;

import lombok.Getter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
@XmlRootElement(name = "form")
@XmlAccessorType(XmlAccessType.FIELD)
@Getter
public class FormSolutionsBestellung {

    //~ Instance fields --------------------------------------------------------

    @XmlElement(name = "transId")
    private String transId;

    @XmlElement(name = "fileUrl")
    private String fileUrl;

    @XmlElement(name = "Auswahl_über")
    private String auswahlUeber;

    @XmlElement(name = "Stadt")
    private String stadt;

    @XmlElement(name = "Straße")
    private String strasse;

    @XmlElement(name = "Hausnummer")
    private String hausnummer;

    @XmlElement(name = "Flurstückskennzeichen")
    private String flurstueckskennzeichen;

    @XmlElement(name = "NS-Ausdehnung")
    private String nsAusdehnung;

    @XmlElement(name = "WO-Ausdehnung")
    private String woAusdehnung;

    @XmlElement(name = "GeoLocation1_latitude")
    private String geoLocation1Latitude;

    @XmlElement(name = "GeoLocation1_longitude")
    private String geoLocation1Longitude;

    @XmlElement(name = "GeoLocation2_latitude")
    private String geoLocation2Latitude;

    @XmlElement(name = "GeoLocation2_longitude")
    private String geoLocation2Longitude;

    @XmlElement(name = "Gemarkung")
    private String gemarkung;

    @XmlElement(name = "Gemarkungsnummer")
    private String gemarkungsnummer;

    @XmlElement(name = "Flurnummer")
    private String flurnummer;

    @XmlElement(name = "Flurstück")
    private String flurstueck;

    @XmlElement(name = "Ausgewählte_Flurstücke")
    private String ausgewaehlteFlurstuecke;

    @XmlElement(name = "Flurstückskennzeichen.1")
    private String flurstueckskennzeichen1;

    @XmlElement(name = "Ausgewählte_Buchungsblätter")
    private String ausgewaehlteBuchungsblaetter;

    @XmlElement(name = "Buchungsblattkennzeichen")
    private String buchungsblattkennzeichen;

    @XmlElement(name = "NS-Ausdehnung.1")
    private String nsAusdehnung1;

    @XmlElement(name = "WO-Ausdehnung.1")
    private String woAusdehnung1;

    @XmlElement(name = "Auswahl")
    private String auswahl;

    @XmlElement(name = "Format")
    private String format;

    @XmlElement(name = "Ausrichtung")
    private String ausrichtung;

    @XmlElement(name = "Maßstab")
    private String massstab;

    @XmlElement(name = "Bezugsweg")
    private String bezugsweg;

    @XmlElement(name = "Farbausprägung")
    private String farbauspraegung;

    @XmlElement(name = "Nord-Süd")
    private String nordSued;

    @XmlElement(name = "West-Ost")
    private String westOst;

    @XmlElement(name = "betrag")
    private String betrag;

    @XmlElement(name = "betragDL")
    private String betragDL;

    @XmlElement(name = "Firma")
    private String firma;

    @XmlElement(name = "AS_Name")
    private String asName;

    @XmlElement(name = "AS_Vorname")
    private String asVorname;

    @XmlElement(name = "staat")
    private String staat;

    @XmlElement(name = "AS_PLZ")
    private String asPlz;

    @XmlElement(name = "AS_Ort")
    private String asOrt;

    @XmlElement(name = "AS_Bundesland")
    private String asBundesland;

    @XmlElement(name = "AS_Strasse")
    private String asStrasse;

    @XmlElement(name = "AS_Hausnummer")
    private String asHausnummer;

    @XmlElement(name = "Alt_Adresse")
    private String altAdresse;

    @XmlElement(name = "Die_Rechnungsanschrift_ist_auch_die_Lieferanschrift")
    private String dieRechnungsanschriftAuchDieLieferanschrift;

    @XmlElement(name = "RechnungsanschriftistLieferanschrift")
    private String rechnungsanschriftistLieferanschrift;

    @XmlElement(name = "Firma.1")
    private String firma1;

    @XmlElement(name = "Firma_Abweichende_Lieferanschrift")
    private String firmaAbweichendeLieferanschrift;

    @XmlElement(name = "AS_Name.1")
    private String asName1;

    @XmlElement(name = "AS_Vorname.1")
    private String asVorname1;

    @XmlElement(name = "staat.1")
    private String staat1;

    @XmlElement(name = "AS_PLZ.1")
    private String asPlz1;

    @XmlElement(name = "AS_Ort.1")
    private String asOrt1;

    @XmlElement(name = "AS_Bundesland.1")
    private String asBundesland1;

    @XmlElement(name = "AS_Strasse.1")
    private String asStrasse1;

    @XmlElement(name = "AS_Hausnummer.1")
    private String asHausnummer1;

    @XmlElement(name = "Alt_Adresse.1")
    private String altAdresse1;

    @XmlElement(name = "Alt_Adresse_Abweichende_Lieferanschrift")
    private String altAdresseAbweichendeLieferanschrift;

    @XmlElement(name = "E-Mailadresse")
    private String eMailadresse;

    @XmlElement(name = "Gutschein")
    private String gutschein;

    @XmlElement(name = "Gutscheincode")
    private String gutscheinCode;

    @XmlElement(name = "Berechtigungsgrund")
    private String berechtigungsgrund;

    @XmlElement(name = "sonstiger_Berechtigungsgrund")
    private String berechtigungsgrund_sonstiger;

    @XmlElement(name = "Begründungstext")
    private String begruendungstext;

    @XmlElement(name = "Nachweis")
    private String nachweis;
}
