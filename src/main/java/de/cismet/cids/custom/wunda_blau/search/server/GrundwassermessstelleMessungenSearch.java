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

import org.apache.log4j.Logger;

import java.io.StringReader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import de.cismet.cids.custom.utils.WundaBlauServerResources;

import de.cismet.cids.server.actions.GetServerResourceServerAction;
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
public class GrundwassermessstelleMessungenSearch extends AbstractCidsServerSearch implements RestApiCidsServerSearch,
    ConnectionContextStore {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient Logger LOG = Logger.getLogger(GrundwassermessstelleMessungenSearch.class);

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum SearchBy {

        //~ Enum constants -----------------------------------------------------

        MESSSTELLE
    }

    //~ Instance fields --------------------------------------------------------

    private final SearchBy searchBy;
    private Integer messstelleId = null;
    private final SearchInfo searchInfo;

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new CidsAlkisSearchStatement object.
     *
     * @param  messstelleId  DOCUMENT ME!
     */
    public GrundwassermessstelleMessungenSearch(final int messstelleId) {
        this(SearchBy.MESSSTELLE);
        this.messstelleId = messstelleId;
    }

    /**
     * Creates a new GrundwassermessstelleMessungenSearchStatement object.
     *
     * @param  searchBy  DOCUMENT ME!
     */
    private GrundwassermessstelleMessungenSearch(final SearchBy searchBy) {
        this.searchBy = searchBy;
        this.searchInfo = new SearchInfo(
                this.getClass().getName(),
                this.getClass().getSimpleName(),
                "Builtin Legacy Search to delegate the operation GrundwassermessstelleMessungenSearchStatement to the cids Pure REST Search API.",
                Arrays.asList(
                    new SearchParameterInfo[] {
                        new MySearchParameterInfo("messungId", Type.INTEGER),
                        new MySearchParameterInfo("searchBy", Type.UNDEFINED),
                    }),
                new MySearchParameterInfo("return", Type.ENTITY_REFERENCE, true));
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public SearchInfo getSearchInfo() {
        return searchInfo;
    }

    @Override
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

            String query = null;

            switch (searchBy) {
                case MESSSTELLE: {
                    query = "SELECT \n"
                                + "	(SELECT id FROM cs_class WHERE table_name ILIKE 'grundwassermessstelle_messung') AS class_id, "
                                + "	grundwassermessstelle_messung.id AS object_id, "
                                + "	grundwassermessstelle_messung.schluessel AS object_name "
                                + "FROM grundwassermessstelle_messung "
                                + "WHERE grundwassermessstelle_messung.messstelle_id = " + String.valueOf(messstelleId);
                }
                break;
            }

            if (query != null) {
                final MetaService ms = (MetaService)getActiveLocalServers().get("WUNDA_BLAU");

                final List<ArrayList> resultList = ms.performCustomSearch(query, getConnectionContext());
                for (final ArrayList al : resultList) {
                    final int cid = (Integer)al.get(0);
                    final int oid = (Integer)al.get(1);
                    final String name = (String)al.get(2);
                    final MetaObjectNode mon = new MetaObjectNode("WUNDA_BLAU", oid, cid, name, null, null);

                    result.add(mon);
                }
            }
            return result;
        } catch (final Exception ex) {
            LOG.error("error while searching for messungen", ex);
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
    private class MySearchParameterInfo extends SearchParameterInfo {

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
