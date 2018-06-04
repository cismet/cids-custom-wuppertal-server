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

import de.cismet.cidsx.server.api.types.SearchInfo;
import de.cismet.cidsx.server.search.RestApiCidsServerSearch;
import de.cismet.cidsx.server.search.builtin.legacy.LightweightMetaObjectsSearch;

import de.cismet.connectioncontext.ConnectionContext;
import de.cismet.connectioncontext.ConnectionContextStore;

/**
 * Search for qsgeb by status.
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

    //~ Instance fields --------------------------------------------------------

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    @Getter private final SearchInfo searchInfo;
    @Getter @Setter private Integer statusId;
    @Getter @Setter private String representationPattern;
    @Getter @Setter private String[] representationFields;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new LightweightMetaObjectsByQuerySearch object for QsgebMarkerEditor.
     */
    public QsgebStatusLightweightSearch() {
        this.searchInfo = new SearchInfo(
                this.getClass().getName(),
                this.getClass().getSimpleName(),
                "Search qsgeb",
                null,
                null);
    }

    /**
     * Creates a new StatusArtLightweightSearch object.
     *
     * @param  representationPattern  DOCUMENT ME!
     * @param  representationFields   DOCUMENT ME!
     */
    public QsgebStatusLightweightSearch(
            final String representationPattern,
            final String[] representationFields) {
        this();
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
        final Integer statusId = getStatusId();
        final MetaService metaService = (MetaService)this.getActiveLocalServers().get("WUNDA_BLAU");

        if (metaService == null) {
            final String message = "Lightweight Meta Objects By Query Search "
                        + "could not connect ot MetaService @domain 'WUNDA_BLAU'";
            LOG.error(message);
            throw new SearchException(message);
        }

        final Collection<String> conditions = new ArrayList<>();
        if (statusId != null) {
            switch (statusId) {
                case 0: {
                    conditions.add(String.format("id = 0"));
                    conditions.add(String.format("id = 1"));
                    conditions.add(String.format("id = 4"));
                    break;
                }
                case 1: {
                    conditions.add(String.format("id = 1"));
                    conditions.add(String.format("id = 2"));
                    break;
                }
                case 2: {
                    conditions.add(String.format("id = 2"));
                    conditions.add(String.format("id = 3"));
                    break;
                }
                default: {
                    break;
                } // andere Fälle treten momentan nicht auf, da dann cbStatus.setEnabled(false);
            }
        }

        final String table = TABLE_QSGEB_STATUS;

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
}
