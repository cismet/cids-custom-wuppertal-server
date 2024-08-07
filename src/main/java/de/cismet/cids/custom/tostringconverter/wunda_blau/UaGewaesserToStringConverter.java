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
public class UaGewaesserToStringConverter extends CustomToStringConverter {

    //~ Static fields/initializers ---------------------------------------------

    public static final String FIELD__NAME = "name";
    public static final String FIELD__WV = "wv";

    //~ Methods ----------------------------------------------------------------

    @Override
    public String createString() {
        final String gewaesser = String.valueOf(cidsBean.getProperty(FIELD__NAME));
        if (Boolean.TRUE.equals(cidsBean.getProperty(FIELD__WV))) {
            return String.format(
                    "%s - WV",
                    gewaesser);
        } else {
            return gewaesser;
        }
    }
}
