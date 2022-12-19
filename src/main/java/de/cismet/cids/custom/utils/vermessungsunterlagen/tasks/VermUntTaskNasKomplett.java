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
package de.cismet.cids.custom.utils.vermessungsunterlagen.tasks;

import com.vividsolutions.jts.geom.Geometry;

import de.cismet.cids.custom.utils.vermessungsunterlagen.VermessungsunterlagenHandler;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class VermUntTaskNasKomplett extends VermUntTaskNas {

    //~ Static fields/initializers ---------------------------------------------

    public static final String TYPE = "NAS_Komplett";

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new VermUntTaskNasKomplett object.
     *
     * @param  jobKey     DOCUMENT ME!
     * @param  requestId  DOCUMENT ME!
     * @param  geometry   DOCUMENT ME!
     */
    public VermUntTaskNasKomplett(final String jobKey,
            final String requestId,
            final Geometry geometry) {
        super(TYPE, jobKey, requestId, geometry, VermessungsunterlagenHandler.NAS_PRODUCT_KOMPLETT);
    }
}
