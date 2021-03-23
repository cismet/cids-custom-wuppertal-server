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

import com.vividsolutions.jts.geom.Geometry;

import lombok.Getter;
import lombok.Setter;

import org.apache.log4j.Logger;

import org.openide.util.lookup.ServiceProvider;

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
public class PfPotenzialflaechenHierarchicalLightweightSearch extends AbstractCidsServerSearch
        implements RestApiCidsServerSearch,
            LightweightMetaObjectsSearch,
            ConnectionContextStore {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(PfPotenzialflaechenHierarchicalLightweightSearch.class);

    public static final String TOSTRING_TEMPLATE = "%1$s (%2$s)";
    public static final String[] TOSTRING_FIELDS = { Subject.NAME.toString(), Subject.NUMMER.toString() };

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Subject {

        //~ Enum constants -----------------------------------------------------

        NAME {

            @Override
            public String toString() {
                return "name";
            }
        },
        NUMMER {

            @Override
            public String toString() {
                return "nummer";
            }
        }
    }

    //~ Instance fields --------------------------------------------------------

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    @Getter private final SearchInfo searchInfo;
    @Getter @Setter private Subject subject = Subject.NAME;
    @Getter @Setter private Geometry geom;
    @Getter @Setter private Integer sortDistanceLimit;
    @Getter @Setter private String representationPattern;
    @Getter @Setter private String[] representationFields;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new LightweightMetaObjectsByQuerySearch object.
     */
    public PfPotenzialflaechenHierarchicalLightweightSearch() {
        this(Subject.NAME, TOSTRING_TEMPLATE, TOSTRING_FIELDS);
    }

    /**
     * Creates a new PoiKategorienLightweightSearch object.
     *
     * @param  subject                DOCUMENT ME!
     * @param  representationPattern  DOCUMENT ME!
     * @param  representationFields   DOCUMENT ME!
     */
    public PfPotenzialflaechenHierarchicalLightweightSearch(
            final Subject subject,
            final String representationPattern,
            final String[] representationFields) {
        this.searchInfo = new SearchInfo(
                this.getClass().getName(),
                this.getClass().getSimpleName(),
                "Builtin Legacy Search to delegate the operation getLightweightMetaObjectsByQuery to the cids Pure REST Search API.",
                Arrays.asList(
                    new SearchParameterInfo[] {
                        new MySearchParameterInfo("representationPattern", Type.STRING, true),
                        new MySearchParameterInfo("representationFields", Type.STRING, true)
                    }),
                new MySearchParameterInfo("return", Type.ENTITY_REFERENCE, true));

        setRepresentationPattern(representationPattern);
        setRepresentationFields(representationFields);
        setSubject(subject);
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

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static String createQuery() {
        final String query = "with recursive list(id, name, fk_kategorie, nummer, text_kombi, ebene, level) as \n"
                    + "( \n"
                    + "select id, name, fk_kategorie, nummer, name as text_kombi, ebene, 1 as level \n"
                    + "from poi_kategorien \n"
                    + "union \n"
                    + "select a.id, a.name, a.fk_kategorie,  a.nummer,  \n"
                    + "list.text_kombi || ' - ' || a.name as text_kombi, \n"
                    + "a.ebene, list.level + 1 \n"
                    + "from poi_kategorien a \n"
                    + "join list on list.id = a.fk_kategorie \n"
                    + ") \n"
                    + "select l.id, l.name AS kat_name, l.nummer, l.text_kombi AS name \n"
                    + "from (select list.*, max(level) over (partition by id) as maxlevel from list) as l \n"
                    + "where l.id not in (select fk_kategorie from poi_kategorien where fk_kategorie is not null) \n"
                    + "and level = maxlevel";
        return query;
    }

    @Override
    public Collection performServerSearch() throws SearchException {
        final MetaService metaService = (MetaService)this.getActiveLocalServers().get("WUNDA_BLAU");
        if (metaService == null) {
            final String message = "Lightweight Meta Objects By Query Search "
                        + "could not connect ot MetaService @domain 'WUNDA_BLAU'";
            LOG.error(message);
            throw new SearchException(message);
        }

        final String query = createQuery();
        try {
            final MetaClass mc = CidsBean.getMetaClassFromTableName(
                    "WUNDA_BLAU",
                    "pf_potenzialflaeche",
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
