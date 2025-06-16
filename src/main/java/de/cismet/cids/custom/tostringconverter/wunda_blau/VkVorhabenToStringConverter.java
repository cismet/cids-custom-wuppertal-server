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
public class VkVorhabenToStringConverter extends CustomToStringConverter {

    //~ Static fields/initializers ---------------------------------------------

    public static final String FIELD__TITEL = "titel";     // vk_vorhaben
    public static final String FIELD__ID = "id";           // vk_vorhaben

    //~ Methods ----------------------------------------------------------------

    @Override
    public String createString() {
        final Integer myid = cidsBean.getPrimaryKeyValue();
        if (myid < 0) {
            return "ein neues Vorhaben anlegen...";
        } else {
            return String.format(
                    "%s (%s)",
                    String.valueOf(cidsBean.getProperty(FIELD__TITEL)),
                    String.valueOf(cidsBean.getProperty(FIELD__ID)));
        }
    }
}
