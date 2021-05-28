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

import lombok.Getter;
import lombok.Setter;

import de.cismet.cismap.commons.jtsgeometryfactories.PostGisGeometryFactory;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public abstract class RestApiMonGeometrySearch extends RestApiMonSearch implements GeometrySearch {

    //~ Instance fields --------------------------------------------------------

    @Getter @Setter private Geometry geometry;
    @Getter @Setter private Double buffer;
    @Getter @Setter private Double cutoff;

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    protected String getGeomCondition() {
        final Geometry geometry = getGeometry();
        if (geometry != null) {
            final String geomStringFromText = String.format(
                    "GeometryFromText('%s')",
                    PostGisGeometryFactory.getPostGisCompliantDbString(geometry));
            return String.format(
                    "(geom.geo_field && %s AND intersects(%s, geo_field))",
                    geomStringFromText,
                    ((getBuffer() != null) ? String.format("st_buffer(%s, %f)", geomStringFromText, getBuffer())
                                           : geomStringFromText));
        } else {
            return null;
        }
    }
}
