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

import Sirius.server.middleware.impls.domainserver.DomainServerImpl;
import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.middleware.interfaces.domainserver.MetaServiceStore;

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
import java.util.Properties;

import de.cismet.cids.custom.utils.WundaBlauServerResources;
import de.cismet.cids.custom.wunda_blau.search.server.CidsMauernSearchStatement;
import de.cismet.cids.custom.wunda_blau.search.server.SearchProperties;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;
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
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class NasZaehlObjekteServerAction implements ServerAction, MetaServiceStore, ConnectionContextStore {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(CidsMauernSearchStatement.class);
    public static final String TASK_NAME = "nasZaehlObjekte";

    private static final String INTERSECTS_BUFFER = SearchProperties.getInstance().getIntersectsBuffer();
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
                + " AND i.class_id IN ((select id from cs_Class where table_name ilike 'adresse'))"
                + " AND geo_field && st_GeometryFromText('<geom>')"
                + " AND st_intersects(st_buffer(geo_field, " + INTERSECTS_BUFFER
                + "),st_buffer(st_GeometryFromText('<geom>'), " + INTERSECTS_BUFFER + ")) ORDER BY 1,2,3";

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Parameter {

        //~ Enum constants -----------------------------------------------------

        GEOMETRY, SEARCH_TYPE
    }

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

    private Connection fmeConn = null;
    private final String url;
    private final String user;
    private final String pw;
    private final boolean initError;

    private MetaService metaService;

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new NasZaehlObjekteSearch object.
     */
    public NasZaehlObjekteServerAction() {
        boolean initError = false;
        String url = null;
        String user = null;
        String pw = null;

        try {
            if ((DomainServerImpl.getServerProperties() != null)
                        && "WUNDA_BLAU".equals(DomainServerImpl.getServerProperties().getServerName())) {
                final Properties serviceProperties = new Properties();
                serviceProperties.load(ServerResourcesLoader.getInstance().loadStringReader(
                        WundaBlauServerResources.FME_DB_CONN_PROPERTIES.getValue()));
                url = serviceProperties.getProperty("connection_url");
                user = serviceProperties.getProperty("connection_username");
                pw = serviceProperties.getProperty("connection_pw");
                initConnection();
            } else {
                initError = true;
            }
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
        this.initError = initError;
        this.url = url;
        this.user = user;
        this.pw = pw;
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public void initWithConnectionContext(final ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }

    @Override
    public MetaService getMetaService() {
        return metaService;
    }

    @Override
    public void setMetaService(final MetaService metaService) {
        this.metaService = metaService;
    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }

    @Override
    public Object execute(final Object body, final ServerActionParameter... params) {
        Geometry geometry = null;
        NasSearchType searchType = null;
        if (params != null) {
            for (final ServerActionParameter sap : params) {
                if (sap.getKey().equals(Parameter.SEARCH_TYPE.toString())) {
                    searchType = (NasSearchType)sap.getValue();
                } else if (sap.getKey().equals(Parameter.GEOMETRY.toString())) {
                    geometry = (Geometry)sap.getValue();
                }
            }
        }
        final ArrayList<Integer> resultList = new ArrayList<Integer>();
        if (initError) {
            LOG.warn(
                "NasZaehlObjekteSearch initialisation error. An error during reading fme_db_con properties occured.");
            return resultList;
        }

        try {
            if (null != searchType) {
                switch (searchType) {
                    case FLURSTUECKE: {
                        resultList.add(getFlurstueckObjectsCount(geometry));
                    }
                    break;
                    case GEBAEUDE: {
                        resultList.add(getGebaeudeObjectsCount(geometry));
                    }
                    break;
                    case ADRESSE: {
                        resultList.add(getAddressenCount(geometry));
                    }
                    break;
                    case BODENPUNKTE: {
                        resultList.add(getBodenpunkteObjectsCount(geometry));
                    }
                    break;
                    case DACHPUNKTE: {
                        resultList.add(getDachpunkteObjectsCount(geometry));
                    }
                    break;
                    default: {
                    }
                }
            }
            return resultList;
        } catch (final SearchException ex) {
            LOG.error(ex, ex);
            return null;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   geometry  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  SearchException  DOCUMENT ME!
     */
    private int getFlurstueckObjectsCount(final Geometry geometry) throws SearchException {
        return getObjectsCount(FLURSTUECK_STMT, geometry);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   geometry  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  SearchException  DOCUMENT ME!
     */
    private int getGebaeudeObjectsCount(final Geometry geometry) throws SearchException {
        return getObjectsCount(GEAEUDE_STMT, geometry);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   geometry  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  SearchException  DOCUMENT ME!
     */
    private int getBodenpunkteObjectsCount(final Geometry geometry) throws SearchException {
        return getObjectsCount(BODEN_PKT_STMT, geometry);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   geometry  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  SearchException  DOCUMENT ME!
     */
    private int getDachpunkteObjectsCount(final Geometry geometry) throws SearchException {
        return getObjectsCount(DACH_PKT_STMT, geometry);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   statement  DOCUMENT ME!
     * @param   geometry   DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  SearchException  DOCUMENT ME!
     */
    private synchronized int getObjectsCount(final String statement, final Geometry geometry) throws SearchException {
        Statement st = null;
        try {
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
     * @param   geometry  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  SearchException  DOCUMENT ME!
     */
    private int getAddressenCount(final Geometry geometry) throws SearchException {
        final MetaService ms = getMetaService();

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
    private void initConnection() throws SearchException {
        try {
            fmeConn = DriverManager.getConnection(url, user, pw);
        } catch (final SQLException ex) {
            throw new SearchException(
                "Error during NasZaehlObjekte search.Could not create db connection to fme_import database",
                ex);
        }
    }

    @Override
    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }
}
