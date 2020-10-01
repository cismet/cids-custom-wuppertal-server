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

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.cismet.cids.server.search.AbstractCidsServerSearch;

import de.cismet.cidsx.base.types.Type;

import de.cismet.cidsx.server.api.types.SearchInfo;
import de.cismet.cidsx.server.api.types.SearchParameterInfo;
import de.cismet.cidsx.server.search.RestApiCidsServerSearch;

import de.cismet.connectioncontext.ConnectionContext;
import de.cismet.connectioncontext.ConnectionContextStore;

/**
 * DOCUMENT ME!
 *
 * @version  $Revision$, $Date$
 */
public class ObjectsPermissionsSearch extends AbstractCidsServerSearch implements RestApiCidsServerSearch,
    ConnectionContextStore {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient Logger LOG = Logger.getLogger(ObjectsPermissionsSearch.class);
    private static final String CLASSNAME__OBJECTPERMISSIONS = "cs_objectpermissions";
    private static final String QUERY_TEMPLATE = ""
                + "SELECT (SELECT id from cs_class WHERE table_name ILIKE '%1$s'), id "
                + "FROM %1$s "
                + "WHERE class_id = %2$d"
                + "AND (object_id IS NULL OR object_id = %3$d)";
    private static final SearchInfo SEARCH_INFO = createSearchInfo();

    //~ Instance fields --------------------------------------------------------

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    private final Collection<MetaObjectNode> objectMons;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new ObjectsPermissionsSearch object.
     *
     * @param  objectMons  DOCUMENT ME!
     */
    public ObjectsPermissionsSearch(final Collection<MetaObjectNode> objectMons) {
        this.objectMons = objectMons;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static SearchInfo createSearchInfo() {
        return new SearchInfo(
                ObjectsPermissionsSearch.class.getName(),
                ObjectsPermissionsSearch.class.getSimpleName(),
                "Builtin Legacy Search to delegate the operation ObjectsPermissionsSearch to the cids Pure REST Search API.",
                Arrays.asList(
                    new SearchParameterInfo[] {
                        new MySearchParameterInfo("objectMons", Type.ENTITY_REFERENCE),
                        new MySearchParameterInfo("searchBy", Type.UNDEFINED),
                    }),
                new MySearchParameterInfo("return", Type.ENTITY_REFERENCE, true));
    }

    @Override
    public SearchInfo getSearchInfo() {
        return SEARCH_INFO;
    }

    @Override
    public void initWithConnectionContext(final ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Collection<MetaObjectNode> getObjectMons() {
        return objectMons;
    }

    @Override
    public Collection<MetaObjectNode> performServerSearch() {
        try {
            final List<MetaObjectNode> result = new ArrayList<>();

            final Collection<MetaObjectNode> objectMonsAllDomains = getObjectMons();

            // seperating MONS by their domain
            final Map<String, Collection<MetaObjectNode>> objectMonsPerDomain = new HashMap<>();
            if (objectMonsAllDomains != null) {
                for (final MetaObjectNode objectMon : objectMonsAllDomains) {
                    if (objectMon != null) {
                        final String domain = objectMon.getDomain();
                        final Collection<MetaObjectNode> objectMonsSingleDomain;
                        if (objectMonsPerDomain.containsKey(domain)) {
                            objectMonsSingleDomain = objectMonsPerDomain.get(domain);
                        } else {
                            objectMonsSingleDomain = new ArrayList<>();
                            objectMonsPerDomain.put(domain, objectMonsSingleDomain);
                        }
                        objectMonsSingleDomain.add(objectMon);
                    }
                }
            }

            // searching objectspermissions for each domain
            for (final String domain : objectMonsPerDomain.keySet()) {
                final Collection<MetaObjectNode> objectMonsSingleDomain = objectMonsPerDomain.get(domain);
                final Collection<String> subQuery = new ArrayList<>();
                if (objectMonsSingleDomain != null) {
                    for (final MetaObjectNode objectMon : objectMonsSingleDomain) {
                        if (objectMon != null) {
                            subQuery.add(String.format(
                                    QUERY_TEMPLATE,
                                    CLASSNAME__OBJECTPERMISSIONS,
                                    objectMon.getClassId(),
                                    objectMon.getObjectId()));
                        }
                    }
                }

                final String query = String.format("%s;", String.join(" UNION ", subQuery));
                final MetaService metaService = (MetaService)getActiveLocalServers().get(domain);

                final List<ArrayList> resultList = metaService.performCustomSearch(query, getConnectionContext());
                for (final ArrayList al : resultList) {
                    final int cid = (Integer)al.get(0);
                    final int oid = (Integer)al.get(1);
                    final MetaObjectNode mon = new MetaObjectNode(domain, oid, cid);

                    result.add(mon);
                }
            }
            return result;
        } catch (final Exception ex) {
            LOG.error("error while searching for objects permissions", ex);
            throw new RuntimeException(ex);
        }
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
    private static class MySearchParameterInfo extends SearchParameterInfo {

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new MySearchParameterInfo object.
         *
         * @param  key   DOCUMENT ME!
         * @param  type  DOCUMENT ME!
         */
        private MySearchParameterInfo(final String key, final Type type) {
            this(key, type, null);
        }
        /**
         * Creates a new MySearchParameterInfo object.
         *
         * @param  key    DOCUMENT ME!
         * @param  type   DOCUMENT ME!
         * @param  array  DOCUMENT ME!
         */
        private MySearchParameterInfo(final String key, final Type type, final Boolean array) {
            super.setKey(key);
            super.setType(type);
            if (array != null) {
                super.setArray(array);
            }
        }
    }
}
