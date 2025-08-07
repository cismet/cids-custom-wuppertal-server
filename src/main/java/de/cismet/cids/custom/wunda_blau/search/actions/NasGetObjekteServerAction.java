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

import org.apache.log4j.Logger;

import org.openide.util.Exceptions;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.Properties;

import de.cismet.cids.custom.utils.WundaBlauServerResources;
import de.cismet.cids.custom.wunda_blau.search.server.CidsMauernSearchStatement;

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
 * @author   therter
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class NasGetObjekteServerAction implements ServerAction, MetaServiceStore, ConnectionContextStore {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(CidsMauernSearchStatement.class);
    public static final String TASK_NAME = "nasGetObjekte";

    private static final String FLURSTUECK_STMT =
        "select gml_id from ax_flurstueck where st_intersects(wkb_geometry,<geom>)";

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

        FLURSTUECKE
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
    public NasGetObjekteServerAction() {
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

        if (initError) {
            LOG.warn(
                "NasZaehlObjekteSearch initialisation error. An error during reading fme_db_con properties occured.");
            return null;
        }

        try {
            if (null != searchType) {
                switch (searchType) {
                    case FLURSTUECKE: {
                        return getFlurstueckObjects(geometry);
                    }
                    default: {
                    }
                }
            }
            return null;
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
    private String getFlurstueckObjects(final Geometry geometry) throws SearchException {
        return getObjects(FLURSTUECK_STMT, geometry);
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
    private synchronized String getObjects(final String statement, final Geometry geometry) throws SearchException {
        Statement st = null;
        try {
            if ((fmeConn == null) || fmeConn.isClosed()) {
                initConnection();
            }
            final String geostring = PostGisGeometryFactory.getPostGisCompliantDbString(geometry);
            final String finalStatement = statement.replace(
                    "<geom>",
                    "st_buffer(st_GeometryFromText('"
                            + geostring
                            + "'), 0.000001)");

            st = fmeConn.createStatement();

            if ((st != null) && !fmeConn.isClosed() && !st.isClosed()) {
                st.execute(finalStatement);

                if (LOG.isDebugEnabled()) {
                    LOG.debug("query: " + finalStatement);
                }

                final ResultSet rs = st.getResultSet();
                StringBuffer result = null;

                while (rs.next()) {
                    if (result == null) {
                        result = new StringBuffer(rs.getString("gml_id"));
                    } else {
                        result.append(",").append(rs.getString("gml_id"));
                    }
                }

                return ((result == null) ? null : result.toString());
            }
            return null;
        } catch (SQLException ex) {
            LOG.error("Error during NasGetobjekteSearch", ex);
            throw new SearchException("Error during NasGetobjekteSearch");
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
