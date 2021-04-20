/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.deletionprovider;

import Sirius.server.localserver.object.AbstractCustomDeletionProvider;
import Sirius.server.localserver.object.CustomDeletionProvider;
import Sirius.server.localserver.object.DeletionProviderClientException;
import Sirius.server.middleware.types.MetaObject;
import Sirius.server.newuser.User;

import org.apache.log4j.Logger;

import org.openide.util.lookup.ServiceProvider;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * DOCUMENT ME!
 *
 * @author   sandra
 * @version  $Revision$, $Date$
 */
@ServiceProvider(service = CustomDeletionProvider.class)
public class PfSchluesseltabelleDeletionProvider extends AbstractCustomDeletionProvider {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(PfSchluesseltabelleDeletionProvider.class);
    public static final String TABLE_NAME = "pf_schluesseltabelle";
    private static final String STILL_USED =
        "Dieser Eintrag darf nicht gelöscht werden, da er noch referenziert wird verwendet wird.";
    private static final String SYSTEM_TABLE = "Die Einträge zu den Schlüsseltabellen dürfen nicht gelöscht werden.";

    private static final String TABLENAMES_TEMPLATE = "SELECT DISTINCT table_name FROM %s;";

    private static final String QUERY_TEMPLATE = "SELECT * FROM selexecute(("
                + "  SELECT '"
                + "SELECT "
                + "  sub.table_name AS table_name, "
                + "  count(sub.object_id) AS num_of "
                + "FROM ("
                + "  ' || array_to_string(array_agg('SELECT ' || sub.field_name || '::bigint AS object_id, ''' || sub.table_name || '''::text AS table_name FROM ' || sub.table_name), ' UNION ') || ' "
                + ") AS sub "
                + "WHERE sub.object_id = ' || %2$d || ' "
                + "GROUP BY sub.table_name ' AS execution_query "
                + "  FROM ("
                + "    SELECT DISTINCT class.table_name, attr.field_name "
                + "    FROM "
                + "      cs_class AS class, "
                + "      cs_attr AS attr "
                + "    WHERE "
                + "      attr.class_id = class.id "
                + "      AND attr.foreign_key_references_to = %1$d "
                + "  ) AS sub "
                + ")) AS (table_name text, num_of bigint);";

    //~ Instance fields --------------------------------------------------------

    private final Set<String> matchingTableNames = new HashSet<>();
    private boolean initalized = false;

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public final void refreshMatchingTableNames() throws Exception {
        final String query = String.format(TABLENAMES_TEMPLATE, TABLE_NAME);
        final List result = getMetaService().performCustomSearch(query, getConnectionContext());
        for (final List row : (List<List>)result) {
            if (row != null) {
                final String tableName = (String)row.get(0);
                matchingTableNames.add(tableName);
            }
        }
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public boolean isMatching(final User user, final MetaObject metaObject) {
        if (!initalized) {
            synchronized (matchingTableNames) {
                try {
                    refreshMatchingTableNames();
                } catch (final Throwable ex) {
                    LOG.error(ex, ex);
                } finally {
                    initalized = true;
                }
            }
        }

        return (metaObject != null) && (metaObject.getMetaClass() != null)
                    && (metaObject.getMetaClass().getTableName() != null)
                    && matchingTableNames.contains(metaObject.getMetaClass().getTableName());
    }

    @Override
    public boolean customDeleteMetaObject(final User user, final MetaObject metaObject) throws Exception {
        if (metaObject != null) {
            if (metaObject.getMetaClass().getTableName().equalsIgnoreCase(TABLE_NAME)) {
                throw new DeletionProviderClientException(SYSTEM_TABLE);
            }
            final int objectId = metaObject.getId();
            final int classId = metaObject.getClassID();
            final String query = String.format(QUERY_TEMPLATE, classId, objectId);
            final Collection result = getMetaService().performCustomSearch(query, getConnectionContext());
            if (result.size() > 0) {
                throw new DeletionProviderClientException(STILL_USED);
            }
        }
        return false;
    }
}
