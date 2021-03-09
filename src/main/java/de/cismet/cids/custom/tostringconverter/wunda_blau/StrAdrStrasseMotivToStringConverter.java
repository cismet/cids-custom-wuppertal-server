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
 * @author   lat-lon
 * @version  $Revision$, $Date$
 */
public class StrAdrStrasseMotivToStringConverter extends CustomToStringConverter {

    //~ Static fields/initializers ---------------------------------------------

    public static final String FIELD__NAME = "name";
    public static final String FIELD__NUMMER = "nummer";

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String myString() {
        String mynummer = String.valueOf(cidsBean.getProperty(FIELD__NUMMER));
        final String myname;
        mynummer = mynummer.trim();
        switch (mynummer.length()) {
            case 2: {
                mynummer = mynummer + "      ";
                break;
            }
            case 3: {
                mynummer = mynummer + "    ";
                break;
            }
            case 4: {
                mynummer = mynummer + "  ";
                break;
            }
        }
        myname = String.valueOf(cidsBean.getProperty(FIELD__NAME));
        final String test = myname + "\u0009" + mynummer;
        return mynummer + "\u0009" + "\t" + (char)9 + "  " + myname;
    }

    @Override
    public String createString() {
        /**
         *
         */

        return myString();
    }
}
