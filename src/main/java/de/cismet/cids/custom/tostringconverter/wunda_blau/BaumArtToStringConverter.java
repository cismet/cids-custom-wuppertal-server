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
public class BaumArtToStringConverter extends CustomToStringConverter {

    //~ Static fields/initializers ---------------------------------------------

    public static final String FIELD__NAME = "name";                 // baum_Art
    public static final String FIELD__HAUPTART = "fk_hauptart.name"; // baum_Hauptart

    //~ Methods ----------------------------------------------------------------

    @Override
    public String createString() {
        final Integer myid = cidsBean.getPrimaryKeyValue();
        if (myid < 0) {
            return "Neue Art anlegen";
        } else {
            return String.format(
                    "%s - %s",
                    String.valueOf(cidsBean.getProperty(FIELD__HAUPTART)),
                    String.valueOf(cidsBean.getProperty(FIELD__NAME)));
        }
    }
}
