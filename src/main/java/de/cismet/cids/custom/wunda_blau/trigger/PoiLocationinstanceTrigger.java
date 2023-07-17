/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.wunda_blau.trigger;

import Sirius.server.newuser.User;
import Sirius.server.sql.DBConnection;

import org.openide.util.lookup.ServiceProvider;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.trigger.AbstractDBAwareCidsTrigger;
import de.cismet.cids.trigger.CidsTrigger;
import de.cismet.cids.trigger.CidsTriggerKey;

import de.cismet.connectioncontext.AbstractConnectionContext;
import de.cismet.connectioncontext.ConnectionContext;
import de.cismet.connectioncontext.ConnectionContextProvider;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * DOCUMENT ME!
 *
 * @author   sandra
 * @version  $Revision$, $Date$
 */
@ServiceProvider(service = CidsTrigger.class)
public class PoiLocationinstanceTrigger extends AbstractDBAwareCidsTrigger implements ConnectionContextProvider {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(PoiLocationinstanceTrigger.class);

    private static final String FIELD__ARR = "alternativegeographicidentifier"; //poi_locationinstance
    private static final String FIELD__ID = "id";                               //poi_alternativegeographicidentifier
    private static final String DOMAIN = "WUNDA_BLAU";
    private static final String TABLE_POI = "poi_locationinstance";
    private static final String TABLE_NAMEN = "poi_alternativegeographicidentifier";

    //~ Instance fields --------------------------------------------------------

    private final ConnectionContext connectionContext = ConnectionContext.create(AbstractConnectionContext.Category.OTHER,
            PoiLocationinstanceTrigger.class.getCanonicalName());

    //~ Methods ----------------------------------------------------------------

    @Override
    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }

    @Override
    public void afterInsert(final CidsBean cidsBean, final User user) {
    }

    @Override
    public void afterDelete(final CidsBean cidsBean, final User user) {
    }

    @Override
    public void afterUpdate(final CidsBean cidsBean, final User user) {
    }

    @Override
    public void beforeInsert(final CidsBean cidsBean, final User user) {
    }

    @Override
    public void beforeUpdate(final CidsBean cidsBean, final User user) {
    }

    @Override
    public void beforeDelete(final CidsBean cidsBean, final User user) {
    }

    @Override
    public void afterCommittedInsert(final CidsBean cidsBean, final User user) {
        
    }

    @Override
    public void afterCommittedUpdate(final CidsBean cidsBean, final User user) {
    }

    @Override
    public void afterCommittedDelete(final CidsBean cidsBean, final User user) {
        final Collection<CidsBean> anCollection = cidsBean.getBeanCollectionProperty(FIELD__ARR);
        
        final List<String> ids_an = new ArrayList<>();
        for (final CidsBean anBean : anCollection) {
            if (anBean.getProperty(FIELD__ID) != null) {
                ids_an.add(anBean.getProperty(FIELD__ID).toString());
            }
        }
        final String ids = (!ids_an.isEmpty()) ? String.format("%s", String.join(", ", ids_an)) : "";
        //Statement
        final String query = String.format("DELETE FROM %s WHERE %s IN (%s);",
                                    TABLE_NAMEN,
                                    FIELD__ID,
                                    ids);
        if (LOG.isDebugEnabled()) {
            LOG.debug(query);
        }
        Statement statement = null;
        try {
            statement = getDbServer().getConnectionPool().getConnection().createStatement();
            statement.executeUpdate(query);
        } catch (final SQLException ex) {
            LOG.error(ex, ex);
        } finally {
            DBConnection.closeStatements(statement);
        }
        
    }

    @Override
    public CidsTriggerKey getTriggerKey() {
        return new CidsTriggerKey(DOMAIN, TABLE_POI);
    }

    @Override
    public int compareTo(final CidsTrigger cidsTrigger) {
        return 0;
    }
    
}
