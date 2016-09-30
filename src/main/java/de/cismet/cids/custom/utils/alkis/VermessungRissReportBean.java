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
 * @version  $Revision$, $Date$
 */
public class VermessungRissReportBean {

    //~ Instance fields --------------------------------------------------------

    private Collection<CidsBean> vermessungsrisse;
    private Collection<VermessungRissImageReportBean> images;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new VermessungRissReportBean object.
     *
     * @param  vermessungsrisse  DOCUMENT ME!
     * @param  images            DOCUMENT ME!
     */
    public VermessungRissReportBean(final Collection<CidsBean> vermessungsrisse,
            final Collection<VermessungRissImageReportBean> images) {
        this.vermessungsrisse = vermessungsrisse;
        this.images = images;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Collection<CidsBean> getVermessungsrisse() {
        return vermessungsrisse;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Collection<VermessungRissImageReportBean> getImages() {
        return images;
    }
}
