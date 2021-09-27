/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.wunda_blau.search.server;

import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.middleware.types.MetaObjectNode;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import de.cismet.cids.server.search.AbstractCidsServerSearch;
import de.cismet.cids.server.search.MetaObjectNodeServerSearch;

import de.cismet.cismap.commons.jtsgeometryfactories.PostGisGeometryFactory;

import de.cismet.connectioncontext.ConnectionContext;
import de.cismet.connectioncontext.ConnectionContextStore;

/**
 * DOCUMENT ME!
 *
 * @author   jweintraut
 * @version  $Revision$, $Date$
 */
public class CidsVermessungRissSearchStatement extends AbstractCidsServerSearch implements MetaObjectNodeServerSearch,
    ConnectionContextStore {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(CidsVermessungRissSearchStatement.class);

    private static final String DOMAIN = "WUNDA_BLAU";
    private static final String CIDSCLASS = "vermessung_riss";

    private static final String INTERSECTS_BUFFER = SearchProperties.getInstance().getIntersectsBuffer();

    private static final String SQL = "SELECT"
                + " DISTINCT (SELECT c.id FROM cs_class c WHERE table_name ilike '" + CIDSCLASS + "') as class_id,"
                + " vr.id,"
                + " vr.schluessel||' - '||vg.name||' - '||vr.flur||' - '||vr.blatt as name"
                + " FROM <fromClause>"
                + " <whereClause>"
                + " ORDER BY name";
    private static final String FROM = CIDSCLASS + " vr JOIN vermessung_gemarkung vg ON vr.gemarkung = vg.id";
    private static final String JOIN_KICKER =
        " JOIN vermessung_riss_flurstuecksvermessung vrf ON vr.id = vrf.vermessung_riss_reference"
                + " JOIN vermessung_flurstuecksvermessung vf ON vrf.flurstuecksvermessung = vf.id"
                + " JOIN vermessung_flurstueck_kicker vfk ON vf.flurstueck = vfk.id";
    private static final String JOIN_GEOM = " JOIN geom g ON vr.geometrie = g.id";

    public static final String FLURSTUECK_GEMARKUNG = "gemarkung";
    public static final String FLURSTUECK_FLUR = "flur";
    public static final String FLURSTUECK_ZAEHLER = "zaehler";
    public static final String FLURSTUECK_NENNER = "nenner";
    public static final String FLURSTUECK_VERAENDERUNGSART = "veraenderungsart";

    //~ Instance fields --------------------------------------------------------

    private String schluessel;
    private String gemarkung;
    private String flur;
    private String blatt;
    private Collection<String> schluesselCollection;
    private Geometry geometry;
    private Collection<Map<String, String>> flurstuecke;

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new CidsVermessungRissSearchStatement object.
     *
     * @param  schluessel            DOCUMENT ME!
     * @param  gemarkung             DOCUMENT ME!
     * @param  flur                  DOCUMENT ME!
     * @param  blatt                 DOCUMENT ME!
     * @param  schluesselCollection  DOCUMENT ME!
     * @param  geometry              DOCUMENT ME!
     * @param  flurstuecke           DOCUMENT ME!
     */
    public CidsVermessungRissSearchStatement(final String schluessel,
            final String gemarkung,
            final String flur,
            final String blatt,
            final Collection<String> schluesselCollection,
            final Geometry geometry,
            final Collection<Map<String, String>> flurstuecke) {
        this.schluessel = schluessel;
        this.gemarkung = gemarkung;
        this.flur = flur;
        this.blatt = blatt;
        this.schluesselCollection = schluesselCollection;
        this.geometry = geometry;
        this.flurstuecke = flurstuecke;
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public void initWithConnectionContext(final ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }

    @Override
    public Collection<MetaObjectNode> performServerSearch() {
        try {
            final ArrayList result = new ArrayList();

            if (((schluessel == null) || (schluessel.trim().length() <= 0))
                        && (gemarkung == null)
                        && ((flur == null) || (flur.trim().length() <= 0))
                        && ((blatt == null) || (blatt.trim().length() <= 0))
                        && ((schluesselCollection == null) || schluesselCollection.isEmpty())
                        && (geometry == null)
                        && ((flurstuecke == null) || flurstuecke.isEmpty())) {
                LOG.warn("No filters provided. Cancel search.");
                return result;
            }

            final StringBuilder sqlBuilder = new StringBuilder();

            final MetaService metaService = (MetaService)getActiveLocalServers().get(DOMAIN);
            if (metaService == null) {
                LOG.error("Could not retrieve MetaService '" + DOMAIN + "'.");
                return result;
            }

            sqlBuilder.append(SQL.replace("<fromClause>", generateFromClause()).replace(
                    "<whereClause>",
                    generateWhereClause()));

            final ArrayList<ArrayList> resultset;
            if (LOG.isDebugEnabled()) {
                LOG.debug("Executing SQL statement '" + sqlBuilder.toString() + "'.");
            }
            resultset = metaService.performCustomSearch(sqlBuilder.toString(), getConnectionContext());

            for (final ArrayList measurementPoint : resultset) {
                final int classID = (Integer)measurementPoint.get(0);
                final int objectID = (Integer)measurementPoint.get(1);
                final String name = (String)measurementPoint.get(2);

                final MetaObjectNode node = new MetaObjectNode(DOMAIN, objectID, classID, name, null, null); // TODO: Check4CashedGeomAndLightweightJson

                result.add(node);
            }

            return result;
        } catch (final Exception e) {
            LOG.error("Problem", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String generateFromClause() {
        final StringBuilder result = new StringBuilder(FROM);

        if ((geometry != null) && !geometry.isEmpty()) {
            result.append(JOIN_GEOM);
        }

        if ((flurstuecke != null) && !flurstuecke.isEmpty()) {
            result.append(JOIN_KICKER);
        }

        return result.toString();
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String generateWhereClause() {
        final StringBuilder result = new StringBuilder();
        String conjunction = "WHERE ";

        if ((schluessel != null) && (schluessel.trim().length() > 0)) {
            result.append(conjunction);

            result.append("vr.schluessel LIKE \'");
            result.append(schluessel);
            result.append('\'');

            conjunction = " AND ";
        }

        if ((gemarkung != null) && (gemarkung.trim().length() > 0)) {
            result.append(conjunction);

            result.append("vr.gemarkung::text LIKE \'");
            result.append(gemarkung);
            result.append('\'');

            conjunction = " AND ";
        }

        if ((flur != null) && (flur.trim().length() > 0)) {
            result.append(conjunction);

            result.append("vr.flur LIKE \'");
            result.append(flur);
            result.append('\'');

            conjunction = " AND ";
        }

        if ((blatt != null) && (blatt.trim().length() > 0)) {
            result.append(conjunction);

            result.append("vr.blatt LIKE \'");
            result.append(blatt);
            result.append('\'');

            conjunction = " AND ";
        }

        if ((schluesselCollection != null) && !schluesselCollection.isEmpty()) {
            result.append(conjunction);

            result.append("vr.schluessel IN (");
            final Iterator<String> schluessel = schluesselCollection.iterator();
            while (schluessel.hasNext()) {
                result.append('\'');
                result.append(schluessel.next());
                result.append('\'');

                if (schluessel.hasNext()) {
                    result.append(',');
                }
            }
            result.append(')');

            conjunction = " AND ";
        }

        if (geometry != null) {
            final String geomString = PostGisGeometryFactory.getPostGisCompliantDbString(geometry);

            result.append(conjunction);
            conjunction = " AND ";

            result.append("g.geo_field && st_GeometryFromText('").append(geomString).append("')");

            result.append(conjunction);

            if ((geometry instanceof Polygon) || (geometry instanceof MultiPolygon)) { // with buffer for searchGeometry
                result.append("st_intersects(st_buffer(g.geo_field, ")
                        .append(INTERSECTS_BUFFER)
                        .append("), st_buffer(st_GeometryFromText('")
                        .append(geomString)
                        .append("'), ")
                        .append(INTERSECTS_BUFFER)
                        .append("))");
            } else {
                result.append("st_intersects(st_buffer(g.geo_field, ")
                        .append(INTERSECTS_BUFFER)
                        .append("), st_GeometryFromText('")
                        .append(geomString)
                        .append("'))");
            }
        }

        if ((flurstuecke != null) && !flurstuecke.isEmpty()) {
            result.append(conjunction);
            result.append('(');

            boolean firstFlurstueck = true;

            final Iterator<Map<String, String>> flurstueckIterator = flurstuecke.iterator();
            while (flurstueckIterator.hasNext()) {
                final Map<String, String> flurstueck = flurstueckIterator.next();
                final String gemarkung = flurstueck.get(FLURSTUECK_GEMARKUNG);
                final String flur = flurstueck.get(FLURSTUECK_FLUR);
                final String zaehler = flurstueck.get(FLURSTUECK_ZAEHLER);
                final String nenner = flurstueck.get(FLURSTUECK_NENNER);
                final String veraenderungsart = flurstueck.get(FLURSTUECK_VERAENDERUNGSART);

                final StringBuilder flurstueckBuilder = new StringBuilder();
                String flurstueckConjunction = "";

                if ((gemarkung != null) && (gemarkung.trim().length() > 0)) {
                    flurstueckBuilder.append("vfk.gemarkung::text LIKE \'");
                    flurstueckBuilder.append(gemarkung);
                    flurstueckBuilder.append('\'');

                    flurstueckConjunction = " AND ";
                }

                if ((flur != null) && (flur.trim().length() > 0)) {
                    flurstueckBuilder.append(flurstueckConjunction);

                    flurstueckBuilder.append("vfk.flur LIKE \'");
                    flurstueckBuilder.append(flur);
                    flurstueckBuilder.append('\'');

                    flurstueckConjunction = " AND ";
                }

                if ((zaehler != null) && (zaehler.trim().length() > 0)) {
                    flurstueckBuilder.append(flurstueckConjunction);

                    flurstueckBuilder.append("vfk.zaehler LIKE \'");
                    flurstueckBuilder.append(zaehler);
                    flurstueckBuilder.append('\'');

                    flurstueckConjunction = " AND ";
                }

                if ((nenner != null) && (nenner.trim().length() > 0)) {
                    flurstueckBuilder.append(flurstueckConjunction);

                    flurstueckBuilder.append("vfk.nenner LIKE \'");
                    flurstueckBuilder.append(nenner);
                    flurstueckBuilder.append('\'');

                    flurstueckConjunction = " AND ";
                }

                if ((veraenderungsart != null) && (veraenderungsart.trim().length() > 0)) {
                    flurstueckBuilder.append(flurstueckConjunction);

                    flurstueckBuilder.append("vf.veraenderungsart::text LIKE \'");
                    flurstueckBuilder.append(veraenderungsart);
                    flurstueckBuilder.append('\'');
                }

                if (flurstueckBuilder.length() > 0) {
                    if (!firstFlurstueck) {
                        result.append(" OR ");
                    } else {
                        firstFlurstueck = false;
                    }

                    result.append('(');
                    result.append(flurstueckBuilder);
                    result.append(')');
                }
            }

            result.append(')');
        }

        return result.toString();
    }

    @Override
    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }
}
