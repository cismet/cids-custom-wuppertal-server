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
package de.cismet.cids.custom.wunda_blau.search.actions;

import Sirius.server.middleware.interfaces.domainserver.MetaService;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

import org.apache.log4j.Logger;

import org.openide.util.Exceptions;

import java.rmi.RemoteException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

import de.cismet.cids.custom.utils.WundaBlauServerResources;
import de.cismet.cids.custom.wunda_blau.search.server.CidsMauernSearchStatement;
import de.cismet.cids.custom.wunda_blau.search.server.SearchProperties;

import de.cismet.cids.server.search.AbstractCidsServerSearch;
import de.cismet.cids.server.search.SearchException;

import de.cismet.cids.utils.serverresources.ServerResourcesLoader;

import de.cismet.cismap.commons.jtsgeometryfactories.PostGisGeometryFactory;

import de.cismet.connectioncontext.ConnectionContext;
import de.cismet.connectioncontext.ConnectionContextStore;

/**
 * DOCUMENT ME!
 *
 * @author   daniel
 * @version  $Revision$, $Date$
 */
@Deprecated
public class NasZaehlObjekteSearch extends AbstractCidsServerSearch implements ConnectionContextStore {

    //~ Static fields/initializers ---------------------------------------------

    private static final String INTERSECTS_BUFFER = SearchProperties.getInstance().getIntersectsBuffer();
    private static final Logger LOG = Logger.getLogger(CidsMauernSearchStatement.class);
    private static final String FLURSTUECK_STMT =
        "select count(*) as Anzahl from ax_flurstueck where st_intersects(wkb_geometry,<geom>)";
    private static final String GEAEUDE_STMT =
        "select count(*) as Anzahl from ax_gebaeude where st_intersects(wkb_geometry,<geom>)";
    private static final String DACH_PKT_STMT =
        "SELECT count (*) FROM sic_regen_dachpg where st_intersects(wkb_geometry,<geom>)";
    private static final String BODEN_PKT_STMT =
        "SELECT count (*) FROM sic_regen_bodenpg where st_intersects(wkb_geometry,<geom>)";
    private static final String ADRESE_STMT = "SELECT DISTINCT i.class_id , i.object_id, s.stringrep"
                + " FROM geom g, cs_attr_object_derived i LEFT OUTER JOIN cs_cache s ON ( s.class_id =i.class_id AND s.object_id=i.object_id )"
                + " WHERE i.attr_class_id = ( SELECT cs_class.id FROM cs_class WHERE cs_class.table_name::text ILIKE 'GEOM'::text )"
                + " AND i.attr_object_id = g.id"
                + " AND i.class_id IN (6)"
                + " AND geo_field && st_GeometryFromText('<geom>')"
                + " AND st_intersects(st_buffer(geo_field, " + INTERSECTS_BUFFER
                + "),st_buffer(st_GeometryFromText('<geom>'), " + INTERSECTS_BUFFER + ")) ORDER BY 1,2,3";
    private static Connection fmeConn = null;
    private static String url;
    private static String user;
    private static String pw;
    private static boolean initError = false;

    static {
        try {
            final Properties serviceProperties = new Properties();
            serviceProperties.load(ServerResourcesLoader.getInstance().loadStringReader(
                    WundaBlauServerResources.FME_DB_CONN_PROPERTIES.getValue()));
            url = serviceProperties.getProperty("connection_url");
            user = serviceProperties.getProperty("connection_username");
            pw = serviceProperties.getProperty("connection_pw");
            initConnection();
        } catch (final SearchException ex) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("error during initialisation of fme db connection.", ex);
            }
        } catch (final Exception ex) {
            initError = true;
            LOG.warn(
                "error during initialisation of fme db connection. Could not read properties file. Search disabled",
                ex);
        }
    }

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum NasSearchType {

        //~ Enum constants -----------------------------------------------------

        FLURSTUECKE, GEBAEUDE, ADRESSE, DACHPUNKTE, BODENPUNKTE
    }

    //~ Instance fields --------------------------------------------------------

    final Geometry geometry;
    final NasZaehlObjekteSearch.NasSearchType searchType;

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new NasZaehlObjekteSearch object.
     *
     * @param  g     DOCUMENT ME!
     * @param  type  useCids DOCUMENT ME!
     */
    public NasZaehlObjekteSearch(final Geometry g, final NasZaehlObjekteSearch.NasSearchType type) {
        geometry = g;
        this.searchType = type;
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public void initWithConnectionContext(final ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  SearchException  DOCUMENT ME!
     */
    private int getFlurstueckObjectsCount() throws SearchException {
        return getObjectsCount(FLURSTUECK_STMT);
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  SearchException  DOCUMENT ME!
     */
    private int getGebaeudeObjectsCount() throws SearchException {
        return getObjectsCount(GEAEUDE_STMT);
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  SearchException  DOCUMENT ME!
     */
    private int getBodenpunkteObjectsCount() throws SearchException {
        return getObjectsCount(BODEN_PKT_STMT);
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  SearchException  DOCUMENT ME!
     */
    private int getDachpunkteObjectsCount() throws SearchException {
        return getObjectsCount(DACH_PKT_STMT);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   statement  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  SearchException  DOCUMENT ME!
     */
    private synchronized int getObjectsCount(final String statement) throws SearchException {
        Statement st = null;
        try {
//            initConnection();
            if ((fmeConn == null) || fmeConn.isClosed()) {
                initConnection();
            }
            st = fmeConn.createStatement();
            final StringBuilder sb = new StringBuilder();
            final String geostring = PostGisGeometryFactory.getPostGisCompliantDbString(geometry);
            if ((geometry instanceof Polygon) || (geometry instanceof MultiPolygon)) { // with buffer for geostring
                sb.append(statement.replace(
                        "<geom>",
                        "st_buffer(st_GeometryFromText('"
                                + geostring
                                + "'), 0.000001)"));
            }
            if ((st != null) && !fmeConn.isClosed() && !st.isClosed()) {
                st.execute(sb.toString());
                if (LOG.isDebugEnabled()) {
                    LOG.debug("query: " + sb.toString());
                }
                final ResultSet rs = st.getResultSet();
                rs.next();
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException ex) {
            LOG.error("Error during NasZaehlobjekteSearch", ex);
            throw new SearchException("Error during NasZaehlobjekteSearch");
        } finally {
            try {
                if (st != null) {
                    st.close();
                }
            } catch (SQLException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  SearchException  DOCUMENT ME!
     */
    private int getAddressenCount() throws SearchException {
        final MetaService ms = (MetaService)getActiveLocalServers().get("WUNDA_BLAU");

        if (ms != null) {
            try {
                final StringBuilder sb = new StringBuilder();
                final String geostring = PostGisGeometryFactory.getPostGisCompliantDbString(geometry);
                if ((geometry instanceof Polygon) || (geometry instanceof MultiPolygon)) { // with buffer for geostring
                    sb.append(ADRESE_STMT.replace(
                            "<geom>",
                            geostring));
                }
                if (LOG.isDebugEnabled()) {
                    LOG.debug("query: " + sb.toString());                                  // NOI18N
                }
                final ArrayList<ArrayList> lists = ms.performCustomSearch(sb.toString(), getConnectionContext());

                return lists.size();
            } catch (RemoteException ex) {
                LOG.error(ex.getMessage(), ex);
            }
        } else {
            LOG.error("active local server not found"); // NOI18N
        }
        return 0;
    }

    /**
     * DOCUMENT ME!
     *
     * @throws  SearchException  DOCUMENT ME!
     */
    private static void initConnection() throws SearchException {
        try {
            fmeConn = DriverManager.getConnection(url,
                    user, pw);
        } catch (SQLException ex) {
            throw new SearchException(
                "Error during NasZaehlObjekte search.Could not create db connection to fme_import database");
        }
    }

    @Override
    public Collection performServerSearch() throws SearchException {
        final ArrayList<Integer> resultList = new ArrayList<Integer>();
        if (initError) {
            LOG.warn(
                "NasZaehlObjekteSearch initialisation error. An error during reading fme_db_con properties occured.");
            return resultList;
        }

        if (searchType == NasZaehlObjekteSearch.NasSearchType.FLURSTUECKE) {
            resultList.add(getFlurstueckObjectsCount());
        } else if (searchType == NasZaehlObjekteSearch.NasSearchType.GEBAEUDE) {
            resultList.add(getGebaeudeObjectsCount());
        } else if (searchType == NasZaehlObjekteSearch.NasSearchType.ADRESSE) {
            resultList.add(getAddressenCount());
        } else if (searchType == NasZaehlObjekteSearch.NasSearchType.BODENPUNKTE) {
            resultList.add(getBodenpunkteObjectsCount());
        } else if (searchType == NasZaehlObjekteSearch.NasSearchType.DACHPUNKTE) {
            resultList.add(getDachpunkteObjectsCount());
        }
        return resultList;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  args  DOCUMENT ME!
     */
    public static void main(final String[] args) {
        final NasZaehlObjekteSearch search = new NasZaehlObjekteSearch(
                null,
                NasZaehlObjekteSearch.NasSearchType.GEBAEUDE);
//        try {
////            search.initConnection();
//        } catch (SearchException ex) {
//            Exceptions.printStackTrace(ex);
//        }
    }

    @Override
    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }
}
