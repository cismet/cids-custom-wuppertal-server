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
import Sirius.server.middleware.types.MetaObjectNode;
import de.cismet.cids.dynamics.CidsBean;

import lombok.Getter;
import lombok.Setter;

import org.apache.log4j.Logger;

import org.openide.util.lookup.ServiceProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import de.cismet.cids.server.search.AbstractCidsServerSearch;
import de.cismet.cids.server.search.SearchException;

import de.cismet.cidsx.base.types.Type;

import de.cismet.cidsx.server.api.types.SearchInfo;
import de.cismet.cidsx.server.api.types.SearchParameterInfo;
import de.cismet.cidsx.server.search.RestApiCidsServerSearch;
import de.cismet.cidsx.server.search.builtin.legacy.LightweightMetaObjectsSearch;

import de.cismet.connectioncontext.ConnectionContext;
import de.cismet.connectioncontext.ConnectionContextStore;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Builtin Legacy Search to delegate the operation getLightweightMetaObjectsByQuery to the cids Pure REST Search API.
 *
 * @author   Sandra Simmert
 * @version  $Revision$, $Date$
 */
@ServiceProvider(service = RestApiCidsServerSearch.class)
public class BaumArtLightweightSearch extends AbstractCidsServerSearch implements RestApiCidsServerSearch,
    LightweightMetaObjectsSearch,
    ConnectionContextStore {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(BaumArtLightweightSearch.class);

    private static final String TABLE__SORTE = "baum_sorte";

    //~ Enums ------------------------------------------------------------------


    //~ Instance fields --------------------------------------------------------

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    @Getter private final SearchInfo searchInfo;
    @Getter @Setter private Integer artId;
    @Getter @Setter private String representationPattern;
    @Getter @Setter private String[] representationFields;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new LightweightMetaObjectsByQuerySearch object.
     */
    public BaumArtLightweightSearch() {
        this.searchInfo = new SearchInfo(
                this.getClass().getName(),
                this.getClass().getSimpleName(),
                "Builtin Legacy Search to delegate the operation getLightweightMetaObjectsByQuery to the cids Pure REST Search API.",
                Arrays.asList(
                    new SearchParameterInfo[] {
                        new MySearchParameterInfo("artId", Type.INTEGER),
                        new MySearchParameterInfo("representationPattern", Type.STRING, true),
                        new MySearchParameterInfo("representationFields", Type.STRING, true)
                    }),
                new MySearchParameterInfo("return", Type.ENTITY_REFERENCE, true));
    }

    /**
     * Creates a new BaumArtLightweightSearch object.
     *
     * @param  representationPattern  DOCUMENT ME!
     * @param  representationFields   DOCUMENT ME!
     */
    public BaumArtLightweightSearch(final String representationPattern,
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
        final Integer artId = getArtId();

        final MetaService metaService = (MetaService)this.getActiveLocalServers().get("WUNDA_BLAU");
        if (metaService == null) {
            final String message = "Lightweight Meta Objects By Query Search "
                        + "could not connect ot MetaService @domain 'WUNDA_BLAU'";
            LOG.error(message);
            throw new SearchException(message);
        }

        final Collection<String> conditions = new ArrayList<>();
        if (artId != null) {
            conditions.add(String.format("fk_art = %d", artId));
        }

        

        final String query = "SELECT (SELECT c.id FROM cs_class c WHERE table_name ILIKE '" + TABLE__SORTE + "') AS class_id, id, name FROM " + TABLE__SORTE
                    + (conditions.isEmpty() ? "" : (" WHERE " + String.join(" AND ", conditions)));
        try {
            final MetaClass mc = CidsBean.getMetaClassFromTableName(
                    "WUNDA_BLAU",
                    TABLE__SORTE,
                    getConnectionContext());
            if (getRepresentationPattern() != null) {
                return Arrays.asList(metaService.getLightweightMetaObjectsByQuery(
                            mc.getID(),
                            getUser(),
                            query,
                            getRepresentationFields(),
                            getRepresentationPattern(),
                            getConnectionContext()));
            } else {
                return Arrays.asList(metaService.getLightweightMetaObjectsByQuery(
                            mc.getID(),
                            getUser(),
                            query,
                            getRepresentationFields(),
                            getConnectionContext()));
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
