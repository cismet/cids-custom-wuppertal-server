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

import de.cismet.cids.tools.CustomToStringConverter;

/**
 * DOCUMENT ME!
 *
 * @author   sandra
 * @version  $Revision$, $Date$
 */

public class No2StandortToStringConverter extends CustomToStringConverter {

    //~ Static fields/initializers ---------------------------------------------

    public static final String FIELD__MP = "mp";                                // no2_standort
    public static final String FIELD__STRASSE = "strasse";                      // no2_standort
    public static final String FIELD__HNR = "hnr";                              // no2_standort

    //~ Methods ----------------------------------------------------------------

    @Override
    public String createString() {
        if (cidsBean.getProperty(FIELD__HNR) != null){
                return cidsBean.getProperty(FIELD__MP).toString() + "- " + cidsBean.getProperty(FIELD__STRASSE).toString() + " " + cidsBean.getProperty(FIELD__HNR);
            }else {
                return cidsBean.getProperty(FIELD__MP).toString() + "- " + cidsBean.getProperty(FIELD__STRASSE).toString();
            }
    }
}
