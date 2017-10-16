/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cismet.cids.custom.wunda_blau.search.server;

import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.middleware.types.MetaObjectNode;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import de.cismet.cids.server.connectioncontext.ConnectionContext;
import de.cismet.cids.server.connectioncontext.ConnectionContextProvider;

import org.apache.log4j.Logger;

import java.rmi.RemoteException;

import java.sql.Date;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.cismet.cids.server.search.AbstractCidsServerSearch;
import de.cismet.cids.server.search.MetaObjectNodeServerSearch;

import de.cismet.cismap.commons.jtsgeometryfactories.PostGisGeometryFactory;

/**
 * DOCUMENT ME!
 *
 * @author   mroncoroni
 * @version  $Revision$, $Date$
 */
public class CidsLandParcelSearchStatement extends AbstractCidsServerSearch implements MetaObjectNodeServerSearch, ConnectionContextProvider {

    //~ Static fields/initializers ---------------------------------------------

    /** LOGGER. */
    private static final transient Logger LOG = Logger.getLogger(CidsLandParcelSearchStatement.class);

    //~ Instance fields --------------------------------------------------------

    private boolean searchActualParcel;
    private boolean searchHistoricalParcel;
    private Date historicalFrom;
    private Date historicalTo;
    private Geometry geometry;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new CidsLandParcelSearchStatement object.
     *
     * @param  actualParcel  DOCUMENT ME!
     * @param  geometry      DOCUMENT ME!
     */
    public CidsLandParcelSearchStatement(final boolean actualParcel, final Geometry geometry) {
        this(actualParcel, false, null, null, geometry);
    }

    /**
     * Creates a new CidsLandParcelSearchStatement object.
     *
     * @param  actualParcel      DOCUMENT ME!
     * @param  historicalParcel  DOCUMENT ME!
     * @param  historicalFrom    DOCUMENT ME!
     * @param  historicalTo      DOCUMENT ME!
     * @param  geometry          DOCUMENT ME!
     */
    public CidsLandParcelSearchStatement(final boolean actualParcel,
            final boolean historicalParcel,
            final Date historicalFrom,
            final Date historicalTo,
            final Geometry geometry) {
        searchActualParcel = actualParcel;
        searchHistoricalParcel = historicalParcel;
        this.historicalFrom = historicalFrom;
        this.historicalTo = historicalTo;
        this.geometry = geometry;
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public Collection<MetaObjectNode> performServerSearch() {
        try {
            if ((searchActualParcel || searchHistoricalParcel) == false) {
                return new ArrayList<MetaObjectNode>();
            }

            if (searchHistoricalParcel && ((historicalFrom == null) || (historicalTo == null))) {
                return new ArrayList<MetaObjectNode>();
            }

            String query =
                "select distinct (select id from cs_class where table_name ilike 'flurstueck') as class_id, fl.id as object_id, fl.alkis_id from flurstueck fl, geom where geom.id = fl.umschreibendes_rechteck";
            if (searchActualParcel || searchHistoricalParcel) {
                query += " and ( ";
                if (searchActualParcel) {
                    query += "fl.historisch is null";
                }
                if (searchActualParcel && searchHistoricalParcel) {
                    query += " or ";
                }
                if (searchHistoricalParcel) {
                    query += "(fl.historisch between '" + historicalFrom + "' and '" + historicalTo + "')";
                }
                query += " )";
            }

            if (geometry != null) {
                final String geostring = PostGisGeometryFactory.getPostGisCompliantDbString(geometry);
                if ((geometry instanceof Polygon) || (geometry instanceof MultiPolygon)) {
                    query += " and geo_field &&\n"
                                + "st_buffer(\n"
                                + "GeometryFromText('" + geostring + "')\n"
                                + ", 0.000001)\n"
                                + "and intersects(geo_field,st_buffer(GeometryFromText('" + geostring
                                + "'), 0.000001))";
                } else {
                    query += " and geo_field &&\n"
                                + "st_buffer(\n"
                                + "GeometryFromText('" + geostring + "')\n"
                                + ", 0.000001)\n"
                                + "and intersects(geo_field, GeometryFromText('" + geostring + "'))";
                }
            }

            final List<MetaObjectNode> result = new ArrayList<MetaObjectNode>();
            final MetaService ms = (MetaService)getActiveLocalServers().get("WUNDA_BLAU");
            final ArrayList<ArrayList> searchResult = ms.performCustomSearch(query, getConnectionContext());
            for (final ArrayList al : searchResult) {
                final int cid = (Integer)al.get(0);
                final int oid = (Integer)al.get(1);
                final String nodename = (String)al.get(2);
                final MetaObjectNode mon = new MetaObjectNode("WUNDA_BLAU", oid, cid, nodename, null, null); // TODO: Check4CashedGeomAndLightweightJson

                result.add(mon);
            }
            return result;
        } catch (RemoteException ex) {
            LOG.error("Problem", ex);
            throw new RuntimeException(ex);
        }
    }
    
    @Override
    public ConnectionContext getConnectionContext() {
        return ConnectionContext.create(CidsLandParcelSearchStatement.class.getSimpleName());
    }                    
    
}
