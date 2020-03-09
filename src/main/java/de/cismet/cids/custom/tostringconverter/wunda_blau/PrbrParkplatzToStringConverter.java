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
public class PrbrParkplatzToStringConverter extends CustomToStringConverter {

    //~ Static fields/initializers ---------------------------------------------

    public static final String FIELD__NAME = "name";

    //~ Methods ----------------------------------------------------------------

    @Override
    public String createString() {
        String myname;
        myname = String.valueOf(cidsBean.getProperty(FIELD__NAME));
        if ("null".equals(myname)) {
            myname = "Neue P+R bzw. B+R Anlage anlegen";
        }

        return myname;
    }
}
