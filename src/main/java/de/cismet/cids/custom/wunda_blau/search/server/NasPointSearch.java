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
package de.cismet.cids.custom.wunda_blau.search.server;

import com.vividsolutions.jts.geom.Geometry;

import java.util.ArrayList;
import java.util.Collection;

/**
 * DOCUMENT ME!
 *
 * @author   daniel
 * @version  $Revision$, $Date$
 */
public class NasPointSearch extends CidsMeasurementPointSearchStatement {

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new NasPointSearch object.
     *
     * @param  pointcode   DOCUMENT ME!
     * @param  pointtypes  DOCUMENT ME!
     * @param  gst         DOCUMENT ME!
     * @param  geometry    DOCUMENT ME!
     */
    public NasPointSearch(final String pointcode,
            final Collection<Pointtype> pointtypes,
            final GST gst,
            final Geometry geometry) {
        super(pointcode, pointtypes, gst, geometry);
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public Collection performServerSearch() {
        final ArrayList<Integer> resultList = new ArrayList<Integer>();
        resultList.add(super.performServerSearch().size());
        return resultList;
    }
}
