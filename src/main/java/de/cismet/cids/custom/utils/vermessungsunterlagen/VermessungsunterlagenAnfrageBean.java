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
package de.cismet.cids.custom.utils.vermessungsunterlagen;

import com.vividsolutions.jts.geom.Polygon;

import lombok.Getter;
import lombok.Setter;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
@Getter
@Setter
public class VermessungsunterlagenAnfrageBean {

    //~ Instance fields --------------------------------------------------------

    private String aktenzeichenKatasteramt;
    private Boolean anonymousOrder;
    private Polygon[] antragsPolygone;
    private VermessungsunterlagenAnfrageBean.AntragsflurstueckBean[] antragsflurstuecke;
    private String[] artderVermessung;
    private String geschaeftsbuchnummer;
    private String katasteramtAuftragsnummer;
    private Boolean mitAPBeschreibungen;
    private Boolean mitAPKarten;
    private Boolean mitAPUebersichten;
    private Boolean mitAlkisBestandsdatenmitEigentuemerinfo;
    private Boolean mitAlkisBestandsdatennurPunkte;
    private Boolean mitAlkisBestandsdatenohneEigentuemerinfo;
    private Boolean mitGrenzniederschriften;
    private Boolean mitPunktnummernreservierung;
    private Boolean mitRisse;
    private String katasteramtsId;
    private String nameVermessungsstelle;
    private Boolean _nurPunktnummernreservierung;
    private VermessungsunterlagenAnfrageBean.PunktnummernreservierungBean[] punktnummernreservierungen;
    private String saumAPSuche;
    private String zulassungsnummerVermessungsstelle;
    private Boolean test;
    private String portalVersion;

    //~ Inner Classes ----------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @Setter
    public static class AntragsflurstueckBean {

        //~ Instance fields ----------------------------------------------------

        private String flurID;
        private String flurstuecksID;
        private String gemarkungsID;
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @Setter
    public static class PunktnummernreservierungBean {

        //~ Instance fields ----------------------------------------------------

        private Integer anzahlPunktnummern;
        private String katasteramtsID;
        private String utmKilometerQuadrat;
    }
}
