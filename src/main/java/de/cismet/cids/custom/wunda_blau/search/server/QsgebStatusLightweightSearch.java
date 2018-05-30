/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.wunda_blau.search.server;

import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.middleware.types.MetaClass;

import lombok.Getter;
import lombok.Setter;

import org.apache.log4j.Logger;

import org.openide.util.lookup.ServiceProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.server.search.AbstractCidsServerSearch;
import de.cismet.cids.server.search.SearchException;

import de.cismet.cidsx.base.types.Type;

import de.cismet.cidsx.server.api.types.SearchInfo;
import de.cismet.cidsx.server.api.types.SearchParameterInfo;
import de.cismet.cidsx.server.search.RestApiCidsServerSearch;
import de.cismet.cidsx.server.search.builtin.legacy.LightweightMetaObjectsSearch;

import de.cismet.connectioncontext.ConnectionContext;
import de.cismet.connectioncontext.ConnectionContextStore;

/**
 * Builtin Legacy Search to delegate the operation getLightweightMetaObjectsByQuery to the cids Pure REST Search API.
 *
 * @author   sandra
 * @version  $1.0$, $31.05.2018$
 */
@ServiceProvider(service = RestApiCidsServerSearch.class)
public class QsgebStatusLightweightSearch extends AbstractCidsServerSearch implements RestApiCidsServerSearch,
    LightweightMetaObjectsSearch,
    ConnectionContextStore {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(QsgebStatusLightweightSearch.class);

    private static final String TABLE_QSGEB_STATUS = "qsgeb_status"; 
    public static final String FIELD__STATUS_ID = "status.id";

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum SearchFor {

        //~ Enum constants -----------------------------------------------------

        STATUS
    }

    //~ Instance fields --------------------------------------------------------

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    @Getter private final SearchInfo searchInfo;
    @Getter @Setter private SearchFor searchFor;
    @Getter @Setter private Integer statusId;
    @Getter @Setter private String representationPattern;
    @Getter @Setter private String[] representationFields;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new LightweightMetaObjectsByQuerySearch object.
     */
    public QsgebStatusLightweightSearch() {
        this.searchInfo = new SearchInfo(
                this.getClass().getName(),
                this.getClass().getSimpleName(),
                "Builtin Legacy Search to delegate the operation getLightweightMetaObjectsByQuery to the cids Pure REST Search API.",
                Arrays.asList(
                    new SearchParameterInfo[] {
                        new MySearchParameterInfo(FIELD__STATUS_ID, Type.INTEGER),
                        new MySearchParameterInfo("searchFor", Type.UNDEFINED),
                        new MySearchParameterInfo("representationPattern", Type.STRING, true),
                        new MySearchParameterInfo("representationFields", Type.STRING, true)
                    }),
                new MySearchParameterInfo("return", Type.ENTITY_REFERENCE, true));
    }

    /**
     * Creates a new StatusArtLightweightSearch object.
     *
     * @param  searchFor              DOCUMENT ME!
     * @param  representationPattern  DOCUMENT ME!
     * @param  representationFields   DOCUMENT ME!
     */
    public QsgebStatusLightweightSearch(final SearchFor searchFor,
            final String representationPattern,
            final String[] representationFields) {
        this();
        setSearchFor(searchFor);
        setRepresentationPattern(representationPattern);
        setRepresentationFields(representationFields);
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public void initWithConnectionContext(final ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }

    @Override
    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }

    @Override
    public Collection performServerSearch() throws SearchException {
        final SearchFor searchFor = getSearchFor();
        final Integer statusId = getStatusId();

        if (searchFor == null) {
            throw new SearchException("searchFor has to be set");
        }

        final MetaService metaService = (MetaService)this.getActiveLocalServers().get("WUNDA_BLAU");
        if (metaService == null) {
            final String message = "Lightweight Meta Objects By Query Search "
                        + "could not connect ot MetaService @domain 'WUNDA_BLAU'";
            LOG.error(message);
            throw new SearchException(message);
        }

        final Collection<String> conditions = new ArrayList<>();
        if (statusId != null) {
            //conditions.add(String.format("status = %d", statusId));
            switch (statusId){
                case 0:
                    conditions.add(String.format("id = %d", 0));
                    conditions.add(String.format("id = %d", 1));
                    conditions.add(String.format("id = %d", 4));
                case 1:
                    conditions.add(String.format("id = %d", 1));
                    conditions.add(String.format("id = %d", 2));
                case 2:
                    conditions.add(String.format("id = %d", 2));
                    conditions.add(String.format("id = %d", 3));
                default://andere Fälle treten momentan nicht auf, da dann cbStatus.setEnabled(false);
            }
        }

        final String table;
        switch (searchFor) {
            case STATUS: {
                table = TABLE_QSGEB_STATUS;
            }
            break;
            default: {
                // should be unreachable, because searchFor was already checked for null above
                throw new SearchException("searchFor has to be set");
            }
        }

        final String query = "SELECT id, name FROM " + table 
                    + (conditions.isEmpty() ? "" : (" WHERE " + String.join(" OR ", conditions)))
                    + " ORDER BY id";
        
        try {
            final MetaClass mc = CidsBean.getMetaClassFromTableName("WUNDA_BLAU", table, getConnectionContext());
            if (getRepresentationPattern() != null) {
                return Arrays.asList(metaService.getLightweightMetaObjectsByQuery(
                            mc.getID(),
                            getUser(),
                            query,
                            getRepresentationFields(),
                            getRepresentationPattern(),
                            connectionContext));
            } else {
                return Arrays.asList(metaService.getLightweightMetaObjectsByQuery(
                            mc.getID(),
                            getUser(),
                            query,
                            getRepresentationFields(),
                            connectionContext));
            }
        } catch (final Exception ex) {
            throw new SearchException("error while loading lwmos", ex);
        }
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
