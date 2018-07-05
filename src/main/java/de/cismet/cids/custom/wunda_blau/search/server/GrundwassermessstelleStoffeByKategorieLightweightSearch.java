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
 * DOCUMENT ME!
 *
 * @version  $Revision$, $Date$
 */
@ServiceProvider(service = RestApiCidsServerSearch.class)
public class GrundwassermessstelleStoffeByKategorieLightweightSearch extends AbstractCidsServerSearch
        implements RestApiCidsServerSearch,
            LightweightMetaObjectsSearch,
            ConnectionContextStore {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(GrundwassermessstelleStoffeByKategorieLightweightSearch.class);

    private static final String TABLE__GRUNDWASSERMESSSTELLE_STOFF = "grundwassermessstelle_stoff";
    private static final String TABLE__GRUNDWASSERMESSSTELLE_KATEGORIE_STOFFE =
        "grundwassermessstelle_kategorie_stoffe";

    //~ Instance fields --------------------------------------------------------

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    @Getter private final SearchInfo searchInfo;
    @Getter @Setter private Integer kategorieId;
    @Getter @Setter private String representationPattern;
    @Getter @Setter private String[] representationFields;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new GrundwassermessstelleStoffeByCategoryLightweightSearch object.
     */
    public GrundwassermessstelleStoffeByKategorieLightweightSearch() {
        this.searchInfo = new SearchInfo(
                this.getClass().getName(),
                this.getClass().getSimpleName(),
                "Builtin Legacy Search to delegate the operation GrundwassermessstelleStoffeByCategoryLightweightSearch to the cids Pure REST Search API.",
                Arrays.asList(
                    new SearchParameterInfo[] {
                        new MySearchParameterInfo("materialId", Type.INTEGER),
                        new MySearchParameterInfo("typId", Type.INTEGER),
                        new MySearchParameterInfo("representationPattern", Type.STRING, true),
                        new MySearchParameterInfo("representationFields", Type.STRING, true)
                    }),
                new MySearchParameterInfo("return", Type.ENTITY_REFERENCE, true));
    }

    /**
     * Creates a new GrundwassermessstelleStoffeByCategoryLightweightSearch object.
     *
     * @param  representationPattern  DOCUMENT ME!
     * @param  representationFields   DOCUMENT ME!
     */
    public GrundwassermessstelleStoffeByKategorieLightweightSearch(
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
        final Integer kategorieId = getKategorieId();

        final MetaService metaService = (MetaService)this.getActiveLocalServers().get("WUNDA_BLAU");
        if (metaService == null) {
            final String message = "Lightweight Meta Objects By Query Search "
                        + "could not connect ot MetaService @domain 'WUNDA_BLAU'";
            LOG.error(message);
            throw new SearchException(message);
        }

        final Collection<String> conditions = new ArrayList<>();
        if (kategorieId != null) {
            conditions.add(String.format(
                    TABLE__GRUNDWASSERMESSSTELLE_KATEGORIE_STOFFE
                            + ".kategorie_reference = %d",
                    kategorieId));
        }

        final String query = "SELECT " + TABLE__GRUNDWASSERMESSSTELLE_STOFF + ".id, "
                    + TABLE__GRUNDWASSERMESSSTELLE_STOFF + ".name FROM " + TABLE__GRUNDWASSERMESSSTELLE_STOFF
                    + " LEFT JOIN "
                    + TABLE__GRUNDWASSERMESSSTELLE_KATEGORIE_STOFFE + " ON "
                    + TABLE__GRUNDWASSERMESSSTELLE_KATEGORIE_STOFFE + ".stoff = " + TABLE__GRUNDWASSERMESSSTELLE_STOFF
                    + ".id"
                    + (conditions.isEmpty() ? "" : (" wHERE " + String.join(" AND ", conditions)));
        try {
            final MetaClass mc = CidsBean.getMetaClassFromTableName(
                    "WUNDA_BLAU",
                    TABLE__GRUNDWASSERMESSSTELLE_STOFF,
                    getConnectionContext());
            if (getRepresentationPattern() != null) {
                return Arrays.asList(metaService.getLightweightMetaObjectsByQuery(
                            mc.getID(),
                            getUser(),
                            query,
                            getRepresentationFields(),
                            getRepresentationPattern()));
            } else {
                return Arrays.asList(metaService.getLightweightMetaObjectsByQuery(
                            mc.getID(),
                            getUser(),
                            query,
                            getRepresentationFields()));
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
