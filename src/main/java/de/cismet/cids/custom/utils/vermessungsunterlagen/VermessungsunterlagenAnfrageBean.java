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

    //~ Static fields/initializers ---------------------------------------------

    private static final String NEW_PORTAL_VERSION = "2.1.0";

    //~ Instance fields --------------------------------------------------------

    private String aktenzeichenKatasteramt;
    private Boolean anonymousOrder;
    private Polygon[] anfragepolygonArray;
    private VermessungsunterlagenAnfrageBean.AntragsflurstueckBean[] antragsflurstuecksArray;
    private String[] artderVermessung;
    private String geschaeftsbuchnummer;
    private String katasteramtAuftragsnummer;
    private String katasteramtsId;
    private Boolean mitAPBeschreibungen;
    private Boolean mitAPKarten;
    private Boolean mitAPUebersichten;
    private Boolean mitNIVPBeschreibungen;
    private Boolean mitNIVPUebersichten;
    private Boolean mitAlkisBestandsdatenmitEigentuemerinfo;
    private Boolean mitAlkisBestandsdatennurPunkte;
    private Boolean mitAlkisBestandsdatenohneEigentuemerinfo;
    private Boolean mitGrenzniederschriften;
    private Boolean mitPunktnummernreservierung;
    private Boolean mitRisse;
    private String nameVermessungsstelle;
    private VermessungsunterlagenAnfrageBean.PunktnummernreservierungBean[] punktnummernreservierungsArray;
    private String saumAPSuche;
    private String zulassungsnummerVermessungsstelle;
    private Boolean test;
    private String portalVersion;

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public boolean isNewPortalVersion() {
        return NEW_PORTAL_VERSION.equals(getPortalVersion());
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Boolean getNurPunktnummernreservierung() {
        return NEW_PORTAL_VERSION.equals(getPortalVersion()) ? null : (!isMitAPBeschreibungen());
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public boolean isMitAPBeschreibungen() {
        return Boolean.TRUE.equals(getMitAPBeschreibungen());
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public boolean isMitAPKarten() {
        return Boolean.TRUE.equals(getMitAPKarten());
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public boolean isMitAPUebersichten() {
        return Boolean.TRUE.equals(getMitAPUebersichten());
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public boolean isMitNIVPBeschreibungen() {
        return Boolean.TRUE.equals(getMitNIVPBeschreibungen());
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public boolean isMitNIVPUebersichten() {
        return Boolean.TRUE.equals(getMitNIVPUebersichten());
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public boolean isMitAlkisBestandsdatenmitEigentuemerinfo() {
        return Boolean.TRUE.equals(getMitAlkisBestandsdatenmitEigentuemerinfo());
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public boolean isMitAlkisBestandsdatennurPunkte() {
        return Boolean.TRUE.equals(getMitAlkisBestandsdatennurPunkte());
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public boolean isMitAlkisBestandsdatenohneEigentuemerinfo() {
        return Boolean.TRUE.equals(getMitAlkisBestandsdatenohneEigentuemerinfo());
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public boolean isMitGrenzniederschriften() {
        return Boolean.TRUE.equals(getMitGrenzniederschriften());
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public boolean isMitPunktnummernreservierung() {
        return Boolean.TRUE.equals(getMitPunktnummernreservierung());
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public boolean isMitRisse() {
        return Boolean.TRUE.equals(getMitRisse());
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public boolean isTest() {
        return Boolean.TRUE.equals(getTest());
    }

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
