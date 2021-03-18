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

import java.sql.Statement;

import java.util.Arrays;
import java.util.Collection;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.trigger.AbstractDBAwareCidsTrigger;
import de.cismet.cids.trigger.CidsTrigger;
import de.cismet.cids.trigger.CidsTriggerKey;

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

    private static final String ORDER_BY_FIELD = "order_by";
    private static final String UPDATE_ORDER_TEMPLATE = "UPDATE %1$s AS outter "
                + "SET %2$s = sub.row_number "
                + "FROM (SELECT id, row_number() over(ORDER BY %2$s, id != %3$d) AS row_number FROM %1$s) AS sub "
                + "WHERE sub.id = outter.id;";

    private static final Collection<String> TABLE_NAMES = Arrays.asList(
            new String[] {
                "PF_AEUSSERE_ERSCHLIESSUNG",
                "PF_AKTIVIERBARKEIT",
                "PF_AUSRICHTUNG",
                "PF_BAULUECKENART",
                "PF_BEBAUUNG",
                "PF_BRACHFLAECHE",
                "PF_EIGENTUEMER",
                "PF_EMPFOHLENE_NUTZUNG",
                "PF_ENTWICKLUNGSAUSSICHTEN",
                "PF_ENTWICKLUNGSSTAND",
                "PF_HANDLUNGSDRUCK",
                "PF_HANDLUNGSPRIORITAET",
                "PF_LAGEBEWERTUNG_VERKEHR",
                "PF_NACHFOLGENUTZUNG",
                "PF_NAEHE_ZU",
                "PF_NUTZUNG",
                "PF_OEPNV",
                "PF_POTENZIALART",
                "PF_RESTRIKTION",
                "PF_REVITALISIERUNG",
                "PF_SIEDLUNGSRAEUMLICHE_LAGE",
                "PF_TOPOGRAFIE",
                "PF_VERFUEGBARKEIT",
                "PF_VEROEFFENTLICHKEITSSTATUS",
                "PF_VERWERTBARKEIT",
                "PF_WOHNEINHEITEN"
            });

    //~ Methods ----------------------------------------------------------------

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
                    && TABLE_NAMES.contains(cidsBean.getMetaObject().getMetaClass().getTableName())) {
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
                    UPDATE_ORDER_TEMPLATE,
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
        return new CidsTriggerKey("WUNDA_BLAU", CidsTriggerKey.ALL);
    }

    @Override
    public int compareTo(final CidsTrigger cidsTrigger) {
        return 0;
    }
}
