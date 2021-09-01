/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.wunda_blau.search.server;

import Sirius.server.middleware.interfaces.domainserver.ActionService;
import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.middleware.types.MetaObjectNode;

import com.vividsolutions.jts.geom.Geometry;

import lombok.AllArgsConstructor;
import lombok.Getter;

import org.apache.log4j.Logger;

import java.io.Serializable;
import java.io.StringReader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import de.cismet.cids.custom.utils.WundaBlauServerResources;

import de.cismet.cids.server.actions.GetServerResourceServerAction;
import de.cismet.cids.server.search.AbstractCidsServerSearch;
import de.cismet.cids.server.search.MetaObjectNodeServerSearch;

import de.cismet.cismap.commons.jtsgeometryfactories.PostGisGeometryFactory;

import de.cismet.connectioncontext.ConnectionContext;

/**
 * DOCUMENT ME!
 *
 * @version  $Revision$, $Date$
 */
public class CidsGrundwassermessstelleSearch extends AbstractCidsServerSearch implements MetaObjectNodeServerSearch {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient Logger LOG = Logger.getLogger(CidsGrundwassermessstelleSearch.class);
    private static final String INTERSECTS_BUFFER = SearchProperties.getInstance().getIntersectsBuffer();

    //~ Instance fields --------------------------------------------------------

    private final Geometry geom;
    private final Date messungVon;
    private final Date messungBis;
    private final Collection<StoffFilter> wertePairs;

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new GrundwassermessstelleMessungenSearchStatement object.
     *
     * @param  geom        DOCUMENT ME!
     * @param  messungVon  searchBy DOCUMENT ME!
     * @param  messungBis  DOCUMENT ME!
     * @param  wertePairs  DOCUMENT ME!
     */
    public CidsGrundwassermessstelleSearch(
            final Geometry geom,
            final Date messungVon,
            final Date messungBis,
            final Collection<StoffFilter> wertePairs) {
        this.geom = geom;
        this.messungVon = messungVon;
        this.messungBis = messungBis;
        this.wertePairs = wertePairs;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param  connectionContext  DOCUMENT ME!
     */
    public void initWithConnectionContext(final ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }

    @Override
    public Collection<MetaObjectNode> performServerSearch() {
        try {
            final List<MetaObjectNode> result = new ArrayList<>();
            final Properties properties = new Properties();
            final ActionService as = (ActionService)getActiveLocalServers().get("WUNDA_BLAU");
            properties.load(new StringReader(
                    (String)as.executeTask(
                        getUser(),
                        GetServerResourceServerAction.TASK_NAME,
                        WundaBlauServerResources.ALKIS_CONF.getValue(),
                        getConnectionContext())));

            final Collection<String> conditions = new ArrayList<>();
            final String messungVonQuery = "grundwassermessstelle_messung.datum >= '" + messungVon + "'";
            final String messungBisQuery = "grundwassermessstelle_messung.datum <= '" + messungBis + "'";
            if ((messungVon != null) && (messungBis != null)) {
                conditions.add("(" + messungVonQuery + "AND " + messungBisQuery + ")");
            } else if (messungBis != null) {
                conditions.add("(" + messungBisQuery + ")");
            } else if (messungVon != null) {
                conditions.add("(" + messungVonQuery + ")");
            }

            if (wertePairs != null) {
                for (final StoffFilter wertPair : wertePairs) {
                    if (wertPair.getStoffSchuessel() != null) {
                        final String schluesselQuery = "grundwassermessstelle_messwert.stoff_schluessel = '"
                                    + wertPair.getStoffSchuessel() + "'";
                        final String messwertVonQuery = "abs(grundwassermessstelle_messwert.wert) >= "
                                    + wertPair.getVonValue();
                        final String messwertBisQuery = "abs(grundwassermessstelle_messwert.wert) <= "
                                    + wertPair.getBisValue();
                        if ((wertPair.getVonValue() != null) && (wertPair.getBisValue() != null)) {
                            conditions.add("(" + schluesselQuery + " AND " + messwertVonQuery + " AND "
                                        + messwertBisQuery + ")");
                        } else if (wertPair.getBisValue() != null) {
                            conditions.add("(" + schluesselQuery + " AND " + messwertBisQuery + ")");
                        } else if (wertPair.getVonValue() != null) {
                            conditions.add("(" + schluesselQuery + " AND " + messwertVonQuery + ")");
                        }
                    }
                }
            }

            final String geomJoin;
            if (geom != null) {
                geomJoin = " LEFT OUTER JOIN geom ON grundwassermessstelle.geometrie = geom.id";
            } else {
                geomJoin = "";
            }

            final String geomWhere;
            if (geom != null) {
                final String geostring = PostGisGeometryFactory.getPostGisCompliantDbString(geom);
                geomWhere = " AND (geom.geo_field && st_GeometryFromText('" + geostring + "') AND st_intersects("
                            + "st_buffer(geo_field, " + INTERSECTS_BUFFER + "),"
                            + "st_GeometryFromText('"
                            + geostring
                            + "')))";
            } else {
                geomWhere = "";
            }
            final String query =
                "SELECT (SELECT c.id FROM cs_class c WHERE table_name ilike 'grundwassermessstelle') AS class_id, grundwassermessstelle.id, max(grundwassermessstelle.name) AS name "
                        + "FROM grundwassermessstelle_messwert "
                        + "LEFT JOIN grundwassermessstelle_messung ON grundwassermessstelle_messwert.messung = grundwassermessstelle_messung.id "
                        + "LEFT JOIN grundwassermessstelle ON grundwassermessstelle_messung.messstelle_id = grundwassermessstelle.id "
                        + geomJoin
                        + (conditions.isEmpty() ? (" WHERE TRUE" + geomWhere)
                                                : (" wHERE " + String.join(" AND ", conditions) + geomWhere))
                        + " "
                        + "GROUP BY (grundwassermessstelle.id)";

            final MetaService ms = (MetaService)getActiveLocalServers().get("WUNDA_BLAU");

            final List<ArrayList> resultList = ms.performCustomSearch(query, getConnectionContext());
            for (final ArrayList al : resultList) {
                final int cid = (Integer)al.get(0);
                final int oid = (Integer)al.get(1);
                final String name = (String)al.get(2);
                final MetaObjectNode mon = new MetaObjectNode("WUNDA_BLAU", oid, cid, name, null, null);

                result.add(mon);
            }
            return result;
        } catch (final Exception ex) {
            LOG.error("error while searching for messungen", ex);
            throw new RuntimeException(ex);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @AllArgsConstructor
    public static class StoffFilter implements Serializable {

        //~ Instance fields ----------------------------------------------------

        private final String stoffSchuessel;
        private final Double vonValue;
        private final Double bisValue;
    }
}
