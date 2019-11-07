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
package de.cismet.cids.custom.wunda_blau.search.actions;

import Sirius.server.middleware.impls.domainserver.DomainServerImpl;
import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.middleware.interfaces.domainserver.MetaServiceStore;
import Sirius.server.newuser.User;
import Sirius.server.sql.DBConnection;
import Sirius.server.sql.SQLTools;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.geom.util.AffineTransformation;
import com.vividsolutions.jts.io.WKTReader;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import org.apache.log4j.Logger;

import java.awt.Point;
import java.awt.Rectangle;

import java.net.URL;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import de.cismet.cids.custom.utils.ServerStadtbilderConf;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;
import de.cismet.cids.server.actions.UserAwareServerAction;

import de.cismet.commons.security.handler.SimpleHttpAccessHandler;

import de.cismet.connectioncontext.ConnectionContext;
import de.cismet.connectioncontext.ConnectionContextStore;

import de.cismet.tools.transformations.PointCoordinatePair;
import de.cismet.tools.transformations.TransformationTools;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */

@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class SchraegluftbilderForGeomSearchAction implements UserAwareServerAction,
    MetaServiceStore,
    ConnectionContextStore {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(SchraegluftbilderForGeomSearchAction.class);

    public static final String TASK_NAME = "SchraegluftbilderForGeomSearch";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static final String QUERY_TEMPLATE =
        "SELECT DISTINCT ON (extract(year from sb_stadtbildserie.aufnahmedatum), sb_blickrichtung.schluessel) "
                + "   sb_stadtbildserie.id AS id, "
                + "   extract(year from sb_stadtbildserie.aufnahmedatum) AS year, "
                + "   sb_blickrichtung.schluessel AS direction, "
                + "   sb_stadtbild.bildnummer AS number, "
                + "   st_distance(st_centroid(geom.geo_field), searchPoint.geo_field) AS distance, "
                + "   geom.geo_field AS geometry "
                + "FROM sb_stadtbildserie "
                + "   LEFT JOIN geom ON sb_stadtbildserie.geom = geom.id "
                + "   LEFT JOIN sb_blickrichtung ON sb_stadtbildserie.blickrichtung = sb_blickrichtung.id "
                + "   LEFT JOIN sb_serie_bild_array ON sb_serie_bild_array.sb_stadtbildserie_reference = sb_stadtbildserie.id "
                + "   LEFT JOIN sb_stadtbild ON sb_serie_bild_array.stadtbild = sb_stadtbild.id "
                + "   , (SELECT st_geomfromtext(?, ?) AS geo_field) AS searchPoint "
                + "WHERE "
                + "   sb_stadtbildserie.bildtyp = 3 "
                + "   AND sb_blickrichtung.schluessel = ANY(?) "
                + "   AND extract(year FROM sb_stadtbildserie.aufnahmedatum) = ANY(?) "
                + "   AND st_contains(geom.geo_field, searchPoint.geo_field) "
                + "ORDER BY "
                + "   extract(year FROM sb_stadtbildserie.aufnahmedatum) desc, "
                + "   sb_blickrichtung.schluessel, "
                + "   st_distance(st_centroid(geom.geo_field), searchPoint.geo_field)";

    private static final int SRID = 25832;

    public static String PARAM_DIRECTION = "DIRECTION";
    public static String PARAM_YEAR = "YEAR";

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Direction {

        //~ Enum constants -----------------------------------------------------

        NORTH, EAST, SOUTH, WEST
    }

    //~ Instance fields --------------------------------------------------------

    private MetaService metaService;
    private ConnectionContext connectionContext;
    private User user;

    //~ Methods ----------------------------------------------------------------

    @Override
    public Object execute(final Object body, final ServerActionParameter... saps) {
        final Geometry searchGeometry;
        if ((body instanceof String) || (body instanceof byte[])) {
            final String wktString = (body instanceof String) ? (String)body : new String((byte[])body);
            final int skIndex = wktString.indexOf(';');
            final String wkt;
            final int wktSrid;
            if (skIndex > 0) {
                final String sridKV = wktString.substring(0, skIndex);
                final int eqIndex = sridKV.indexOf('=');

                if (eqIndex > 0) {
                    wktSrid = Integer.parseInt(sridKV.substring(eqIndex + 1));
                    wkt = wktString.substring(skIndex + 1);
                } else {
                    wkt = wktString;
                    wktSrid = SRID;
                }
            } else {
                wkt = wktString;
                wktSrid = SRID;
            }

            try {
                if (wktSrid < 0) {
                    searchGeometry = new WKTReader().read(wkt);
                    searchGeometry.setSRID(wktSrid);
                } else {
                    final GeometryFactory geomFactory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING),
                            wktSrid);
                    searchGeometry = new WKTReader(geomFactory).read(wkt);
                    searchGeometry.setSRID(wktSrid);
                }
            } catch (final Exception ex) {
                LOG.error("could not parse or transform WKT String", ex);
                throw new IllegalArgumentException(ex);
            }
        } else if (body instanceof Geometry) {
            searchGeometry = (Geometry)body;
        } else {
            throw new IllegalArgumentException("no search geometry given");
        }

        final Geometry offsetSearchGeometry = searchGeometry; // AffineTransformation.translationInstance(0,
                                                              // -50).transform(searchGeometry);

        final Collection<String> directions = new ArrayList<>();
        if (saps != null) {
            for (final ServerActionParameter sap : saps) {
                if (PARAM_DIRECTION.equals(sap.getKey())) {
                    directions.add(Direction.valueOf((String)sap.getValue()).toString());
                }
            }
        }
        if (directions.isEmpty()) {
            directions.add(Direction.NORTH.toString());
            directions.add(Direction.EAST.toString());
            directions.add(Direction.SOUTH.toString());
            directions.add(Direction.WEST.toString());
        }

        final Collection<Integer> years = new ArrayList<>();
        if (saps != null) {
            for (final ServerActionParameter sap : saps) {
                if (PARAM_YEAR.equals(sap.getKey())) {
                    years.add((Integer)sap.getValue());
                }
            }
        }
        if (years.isEmpty()) {
            years.add(2018);
        }

        final Collection<Info> infos = new ArrayList<>();

        try {
            final Connection connection = DomainServerImpl.getServerInstance().getConnectionPool().getConnection();
            try {
                final PreparedStatement preparedStatement = connection.prepareStatement(QUERY_TEMPLATE);
                preparedStatement.setString(1, offsetSearchGeometry.toText());
                preparedStatement.setInt(2, offsetSearchGeometry.getSRID());
                preparedStatement.setArray(3, connection.createArrayOf("text", directions.toArray()));
                preparedStatement.setArray(4, connection.createArrayOf("integer", years.toArray()));
                final ResultSet resultSet = preparedStatement.executeQuery();
                while (resultSet.next()) {
                    final int id = resultSet.getInt(1);
                    final int year = resultSet.getInt(2);
                    final String direction = resultSet.getString(3);
                    final String name = resultSet.getString(4);
                    final double distance = resultSet.getDouble(5);
                    final Geometry geometry = SQLTools.getGeometryFromResultSetObject(resultSet.getObject(6));

                    final URL[] urls = ServerStadtbilderConf.getInstance()
                                .getHighresPictureUrls(name, 3, year, direction);

                    URL imgUrl = null;
                    for (final URL url : urls) {
                        if ((new SimpleHttpAccessHandler()).checkIfURLaccessible(url)) {
                            imgUrl = url;
                        }
                    }

                    final Coordinate center = geometry.getCentroid().getCoordinate();

                    final Coordinate[] coordinates = new Coordinate[4];
                    for (final Coordinate coordinate : Arrays.copyOf(geometry.getCoordinates(), 4)) {
                        if ((coordinate.x < center.x) && (coordinate.y < center.y)) {
                            coordinates[0] = coordinate;
                        } else if ((coordinate.x > center.x) && (coordinate.y < center.y)) {
                            coordinates[1] = coordinate;
                        } else if ((coordinate.x > center.x) && (coordinate.y > center.y)) {
                            coordinates[2] = coordinate;
                        } else if ((coordinate.x < center.x) && (coordinate.y > center.y)) {
                            coordinates[3] = coordinate;
                        }
                    }

                    final Rectangle rect = new Rectangle(0, 0, 1, 1);

                    final PointCoordinatePair[] pairs = new PointCoordinatePair[4];
                    pairs[0] = new PointCoordinatePair(new Point((int)rect.getMinX(), (int)rect.getMinY()),
                            coordinates[0]);
                    pairs[1] = new PointCoordinatePair(new Point((int)rect.getMaxX(), (int)rect.getMinY()),
                            coordinates[1]);
                    pairs[2] = new PointCoordinatePair(new Point((int)rect.getMaxX(), (int)rect.getMaxY()),
                            coordinates[2]);
                    pairs[3] = new PointCoordinatePair(new Point((int)rect.getMinX(), (int)rect.getMaxY()),
                            coordinates[3]);
                    final AffineTransformation transformation = TransformationTools.calculateAvgTransformation(
                            pairs);

                    final Point.Double point;

                    final Geometry transformedSearchGeometry = transformation.getInverse()
                                .transform(offsetSearchGeometry);

                    if (Direction.NORTH.toString().equals(direction)) {
                        point = new Point.Double(
                                transformedSearchGeometry.getCoordinate().x,
                                1
                                        - transformedSearchGeometry.getCoordinate().y);
                    } else if (Direction.WEST.toString().equals(direction)) {
                        point = new Point.Double(
                                transformedSearchGeometry.getCoordinate().y,
                                transformedSearchGeometry.getCoordinate().x);
                    } else if (Direction.SOUTH.toString().equals(direction)) {
                        point = new Point.Double(
                                1
                                        - transformedSearchGeometry.getCoordinate().x,
                                transformedSearchGeometry.getCoordinate().y);
                    } else if (Direction.EAST.toString().equals(direction)) {
                        point = new Point.Double(
                                1
                                        - transformedSearchGeometry.getCoordinate().y,
                                1
                                        - transformedSearchGeometry.getCoordinate().x);
                    } else {
                        point = null; // never happens
                    }

                    final Info info = new Info(
                            imgUrl,
                            direction,
                            year,
                            point);
                    infos.add(info);
                }
            } finally {
                DBConnection.closeConnections(connection);
            }

            final String json = OBJECT_MAPPER.writeValueAsString(infos);
            LOG.fatal(json);
            return json;
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }

    @Override
    public void setMetaService(final MetaService metaService) {
        this.metaService = metaService;
    }

    @Override
    public MetaService getMetaService() {
        return metaService;
    }

    @Override
    public User getUser() {
        return user;
    }

    @Override
    public void setUser(final User user) {
        this.user = user;
    }

    @Override
    public void initWithConnectionContext(final ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }

    @Override
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
    @Setter
    @AllArgsConstructor
    static class Info {

        //~ Instance fields ----------------------------------------------------

        private final URL url;
        private final String direction;
        private final Integer year;
        private final Point.Double point;
    }
}
