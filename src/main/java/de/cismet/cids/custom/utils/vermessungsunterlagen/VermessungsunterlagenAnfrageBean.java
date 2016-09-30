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
    private Polygon[] anfragepolygonArray;
    private AntragsflurstueckBean[] antragsflurstuecksArray;
    private String[] artderVermessung;
    private String geschaeftsbuchnummer;
    private String katasteramtAuftragsnummer;
    private String katasteramtsId;
    private Boolean mitGrenzniederschriften;
    private String nameVermessungsstelle;
    private Boolean nurPunktnummernreservierung;
    private PunktnummernreservierungBean[] punktnummernreservierungsArray;
    private String saumAPSuche;
    private String zulassungsnummerVermessungsstelle;

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
