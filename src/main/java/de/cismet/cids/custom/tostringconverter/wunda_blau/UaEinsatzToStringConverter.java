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
public class UaEinsatzToStringConverter extends CustomToStringConverter {

    //~ Static fields/initializers ---------------------------------------------

    public static final String FIELD__ID = "id";
    public static final String FIELD__AZ = "aktenzeichen";

    //~ Methods ----------------------------------------------------------------

    @Override
    public String createString() {
        final String id = String.valueOf(cidsBean.getProperty(FIELD__ID));
        final String az = String.valueOf(cidsBean.getProperty(FIELD__AZ));
        return String.format("%s (%s)", az, id);
    }
}
