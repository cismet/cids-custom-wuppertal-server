/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.wunda_blau.trigger;

import Sirius.server.localserver.DBServer;
import Sirius.server.newuser.User;
import Sirius.server.sql.DBConnection;

import org.openide.util.lookup.ServiceProvider;

import java.sql.ResultSet;
import java.sql.Statement;

import java.util.Collection;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.trigger.AbstractDBAwareCidsTrigger;
import de.cismet.cids.trigger.CidsTrigger;
import de.cismet.cids.trigger.CidsTriggerKey;
import java.util.HashSet;

/**
 * DOCUMENT ME!
 *
 * @version  $Revision$, $Date$
 */
@ServiceProvider(service = CidsTrigger.class)
public class PfSchluesseltabelleTrigger extends AbstractDBAwareCidsTrigger {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            PfSchluesseltabelleTrigger.class);

    private static final String DOMAIN = "WUNDA_BLAU";
    private static final String TABLE = "pf_schluesseltabelle";
    private static final String ORDER_BY_FIELD = "order_by";
    private static final String TABLE_NAME_FIELD = "table_name";

    private static final String UPDATE_ORDER_QUERY_TEMPLATE = "UPDATE %1$s AS outter "
                + "SET %2$s = sub.row_number "
                + "FROM (SELECT id, row_number() over(ORDER BY %2$s, id != %3$d) AS row_number FROM %1$s) AS sub "
                + "WHERE sub.id = outter.id;";
    private static final String SELECT_TABLENAMES_QUERY_TEMPLATE = "SELECT %2$s FROM %1$s";

    //~ Instance fields --------------------------------------------------------

    private final Collection<String> triggeringTableNames = new HashSet<>();

    //~ Methods ----------------------------------------------------------------

    @Override
    public void setDbServer(final DBServer dbServer) {
        super.setDbServer(dbServer);

        Statement stmt = null;
        try {
            stmt = dbServer.getConnectionPool().getConnection().createStatement();

            final String query = String.format(
                    SELECT_TABLENAMES_QUERY_TEMPLATE,
                    TABLE,
                    TABLE_NAME_FIELD);
            final ResultSet resultSet = stmt.executeQuery(query);
            while (resultSet.next()) {
                getTriggeringTableNames().add(resultSet.getObject(TABLE_NAME_FIELD).toString());
            }
        } catch (final Throwable ex) {
            LOG.error(ex, ex);
        } finally {
            DBConnection.closeStatements(stmt);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public final Collection<String> getTriggeringTableNames() {
        return triggeringTableNames;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   cidsBean  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String identifyRelevantTableName(final CidsBean cidsBean) {
        if ((cidsBean != null)
                    && (cidsBean.getMetaObject() != null)
                    && (cidsBean.getMetaObject().getMetaClass() != null)
                    && getTriggeringTableNames().contains(cidsBean.getMetaObject().getMetaClass().getTableName())) {
            return cidsBean.getMetaObject().getMetaClass().getTableName();
        } else {
            return null;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param  cidsBean  DOCUMENT ME!
     */
    private void storeQuery(final CidsBean cidsBean) {
        final String tableName = identifyRelevantTableName(cidsBean);
        if (tableName != null) {
            final String query = String.format(
                    UPDATE_ORDER_QUERY_TEMPLATE,
                    tableName,
                    ORDER_BY_FIELD,
                    cidsBean.getMetaObject().getId());
            Statement stmt = null;
            try {
                stmt = getDbServer().getConnectionPool().getConnection().createStatement();
                stmt.executeUpdate(query);
            } catch (final Exception ex) {
                LOG.error(ex, ex);
            } finally {
                DBConnection.closeStatements(stmt);
            }
        }
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
        storeQuery(cidsBean);
    }

    @Override
    public void afterCommittedUpdate(final CidsBean cidsBean, final User user) {
        storeQuery(cidsBean);
    }

    @Override
    public void afterCommittedDelete(final CidsBean cidsBean, final User user) {
        storeQuery(cidsBean);
    }

    @Override
    public CidsTriggerKey getTriggerKey() {
        return new CidsTriggerKey(DOMAIN, CidsTriggerKey.ALL);
    }

    @Override
    public int compareTo(final CidsTrigger cidsTrigger) {
        return 0;
    }
}
