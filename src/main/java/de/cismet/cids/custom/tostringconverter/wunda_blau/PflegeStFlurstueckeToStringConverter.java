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
public class PflegeStFlurstueckeToStringConverter extends CustomToStringConverter {
    public static final String FIELD__STRASSE = "strasse";
    public static final String FIELD__VON = "von";  
    public static final String FIELD__BIS = "bis";                               
    public static final String FIELD__ID = "id"; 

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String myString() {
        String myname;
        String von;
        String bis;
        von = String.valueOf(cidsBean.getProperty(FIELD__VON));
        bis = String.valueOf(cidsBean.getProperty(FIELD__BIS));
        if (("null".equals(von)) && ( "null".equals(bis))) {
            myname = String.valueOf(cidsBean.getProperty(FIELD__STRASSE));
        } else {
            if ("null".equals(von)) {
                myname = String.valueOf(cidsBean.getProperty(FIELD__STRASSE)) + " ("
                            + String.valueOf(cidsBean.getProperty(FIELD__BIS)) + ")";
            } else {
                if ( "null".equals(bis)) {
                    myname = String.valueOf(cidsBean.getProperty(FIELD__STRASSE)) + " ("
                                + String.valueOf(cidsBean.getProperty(FIELD__VON)) + ")";
                } else {
                    myname = String.valueOf(cidsBean.getProperty(FIELD__STRASSE)) + " ("
                                + String.valueOf(cidsBean.getProperty(FIELD__VON)) + "-"
                                + String.valueOf(cidsBean.getProperty(FIELD__BIS)) + ")";
                }
            }
        }

        return myname;
    }

    @Override
    public String createString() {
        /**
         *
         */

       final String myid = String.valueOf(cidsBean.getProperty(FIELD__ID));
        if ("-1".equals(myid)) {
            return "neue Pflegepfläche anlegen";
        }

        return myString();
    }
}
