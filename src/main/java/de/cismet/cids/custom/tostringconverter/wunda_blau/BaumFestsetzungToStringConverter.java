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
package de.cismet.cids.custom.tostringconverter.wunda_blau;

import java.text.SimpleDateFormat;

import de.cismet.cids.tools.CustomToStringConverter;

/**
 * DOCUMENT ME!
 *
 * @author   sandra
 * @version  $Revision$, $Date$
 */
public class BaumFestsetzungToStringConverter extends CustomToStringConverter {

    //~ Static fields/initializers ---------------------------------------------

    public static final String FIELD__ID = "id";                                                  // baum_festsetzung
    public static final String FIELD__ART = "fk_art.name";                                        // baum_festsetzung
    public static final String FIELD__SCHADEN_ID = "fk_schaden.id";                               // baum_schaden
    public static final String FIELD__SCHADEN_ART = "fk_schaden.fk_art.name";                     // baum_schaden
    public static final String FIELD__MELDUNG_DATUM = "fk_schaden.fk_meldung.datum";              // baum_meldung
    public static final String FIELD__GEBIET_AZ = "fk_schaden.fk_meldung.fk_gebiet.aktenzeichen"; // baum_gebiet

    //~ Methods ----------------------------------------------------------------

    @Override
    public String createString() {
        final Integer myid = cidsBean.getPrimaryKeyValue();
        if (myid < 0) {
            return "--";
        } else {
            final SimpleDateFormat formatTag = new SimpleDateFormat("dd.MM.yy");
            return String.format(
                    "G: %s - M: %s - S: %s, %s - F:%s, %s",
                    cidsBean.getProperty(FIELD__GEBIET_AZ),
                    formatTag.format(
                        cidsBean.getProperty(FIELD__MELDUNG_DATUM)),
                    cidsBean.getProperty(FIELD__SCHADEN_ID),
                    cidsBean.getProperty(FIELD__SCHADEN_ART),
                    cidsBean.getProperty(FIELD__ID),
                    cidsBean.getProperty(FIELD__ART));
        }
    }
}
