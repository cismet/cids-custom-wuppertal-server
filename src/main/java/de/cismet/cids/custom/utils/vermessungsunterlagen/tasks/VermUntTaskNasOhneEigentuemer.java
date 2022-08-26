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

import Sirius.server.newuser.User;

import com.vividsolutions.jts.geom.Geometry;

import de.cismet.cids.custom.utils.vermessungsunterlagen.VermessungsunterlagenHelper;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class VermUntTaskNasOhneEigentuemer extends VermUntTaskNas {

    //~ Static fields/initializers ---------------------------------------------

    public static final String TYPE = "NAS_OhneEigentuemer";

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new VermUntTaskNasOhneEigentuemer object.
     *
     * @param  jobKey     DOCUMENT ME!
     * @param  user       DOCUMENT ME!
     * @param  requestId  DOCUMENT ME!
     * @param  geometry   DOCUMENT ME!
     */
    public VermUntTaskNasOhneEigentuemer(final String jobKey,
            final User user,
            final String requestId,
            final Geometry geometry) {
        super(TYPE, jobKey, user, requestId, geometry, VermessungsunterlagenHelper.NAS_PRODUCT_OHNE_EIGENTUEMER);
    }
}
