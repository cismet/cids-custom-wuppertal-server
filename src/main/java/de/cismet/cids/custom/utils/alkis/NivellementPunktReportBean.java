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
public class NivellementPunktReportBean {

    //~ Instance fields --------------------------------------------------------

    private final Collection<CidsBean> nivellementPunkte;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new NivellementPunktReportBean object.
     *
     * @param  nivellementPunkte  DOCUMENT ME!
     */
    public NivellementPunktReportBean(final Collection<CidsBean> nivellementPunkte) {
        this.nivellementPunkte = nivellementPunkte;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Collection<CidsBean> getNivellementPunkte() {
        return nivellementPunkte;
    }
}
