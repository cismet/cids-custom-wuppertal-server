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

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class VermessungsunterlagenUtils {

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param   gemarkung  flurstueck DOCUMENT ME!
     * @param   flur       DOCUMENT ME!
     * @param   zaehler    DOCUMENT ME!
     * @param   nenner     DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static String[] createFlurstueckParts(final String gemarkung,
            final String flur,
            final String zaehler,
            final String nenner) {
        try {
            final String formattedGemarkung = gemarkung.startsWith("05") ? gemarkung.substring(2) : gemarkung;
            final String formattedFlur = String.format("%03d", Integer.parseInt(flur));
            final String formattedZahler = Integer.valueOf(zaehler).toString();
            final String formattedNenner = (nenner != null) ? Integer.valueOf(nenner).toString() : "0";
            return new String[] { formattedGemarkung, formattedFlur, formattedZahler, formattedNenner };
        } catch (final Exception ex) {
            return null;
        }
    }
}
