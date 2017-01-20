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
package de.cismet.cids.custom.utils.alkis;

import java.util.Collection;

import de.cismet.cids.dynamics.CidsBean;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class AlkisPointReportBean {

    //~ Instance fields --------------------------------------------------------

    private final Collection<CidsBean> alkisPunkte;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new AlkisPointReportBean object.
     *
     * @param  alkisPunkte  DOCUMENT ME!
     */
    public AlkisPointReportBean(final Collection<CidsBean> alkisPunkte) {
        this.alkisPunkte = alkisPunkte;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Collection<CidsBean> getAlkisPunkte() {
        return alkisPunkte;
    }
}
