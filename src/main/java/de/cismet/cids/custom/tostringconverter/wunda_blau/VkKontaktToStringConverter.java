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
public class VkKontaktToStringConverter extends CustomToStringConverter {

    //~ Static fields/initializers ---------------------------------------------

    public static final String FIELD__MAIL = "mail"; // vk_kontakt
    public static final String FIELD__TEL = "telefon";      // vk_kontakt

    //~ Methods ----------------------------------------------------------------

    @Override
    public String createString() {
        final Integer myid = cidsBean.getPrimaryKeyValue();
        if (myid < 0) {
            return "eine neuen Kontakt anlegen...";
        } else {
            if (cidsBean.getProperty(FIELD__TEL) == null){
                return String.format("%s",
                        String.valueOf(cidsBean.getProperty(FIELD__MAIL)));
            } else {
                return String.format("%s (%s)",
                        String.valueOf(cidsBean.getProperty(FIELD__MAIL)),
                        String.valueOf(cidsBean.getProperty(FIELD__TEL)));
            }
        }
    }
}
