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
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import lombok.Getter;
import lombok.Setter;

import org.apache.log4j.Logger;

import org.openide.util.lookup.ServiceProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.server.search.AbstractCidsServerSearch;
import de.cismet.cids.server.search.SearchException;

import de.cismet.cidsx.base.types.Type;

import de.cismet.cidsx.server.api.types.SearchInfo;
import de.cismet.cidsx.server.api.types.SearchParameterInfo;
import de.cismet.cidsx.server.search.RestApiCidsServerSearch;
import de.cismet.cidsx.server.search.builtin.legacy.LightweightMetaObjectsSearch;

import de.cismet.cismap.commons.jtsgeometryfactories.PostGisGeometryFactory;

import de.cismet.connectioncontext.ConnectionContext;
import de.cismet.connectioncontext.ConnectionContextStore;

/**
 * DOCUMENT ME!
 *
 * @version  $Revision$, $Date$
 */
@ServiceProvider(service = RestApiCidsServerSearch.class)
public class BaumGebietLightweightSearch extends AbstractCidsServerSearch implements RestApiCidsServerSearch,
    LightweightMetaObjectsSearch,
    ConnectionContextStore {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(BaumGebietLightweightSearch.class);

    public static final String TOSTRING_TEMPLATE = "%1$s (%2$s)";
    public static final String[] TOSTRING_FIELDS = { Subject.NAME.toString(), Subject.AKTENZEICHEN.toString() };
    public static final String TABLE_NAME = "baum_gebiet";
    public static final String TABLE_NAME_MELDUNG = "baum_meldung";
    public static final String FIELD__NAME = "name";
    public static final String FIELD__ID = "id";
    public static final String FIELD__AZ = "aktenzeichen";
    public static final String FIELD__MELDUNG_DATUM = "datum";
    public static final String FIELD__MELDUNG_FK = "fk_gebiet";

    

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
                return FIELD__NAME;
            }
        },
        AKTENZEICHEN {

            @Override
            public String toString() {
                return FIELD__AZ;
            }
        },
        DATUM{

            @Override
            public String toString() {
                return FIELD__MELDUNG_DATUM;
            }
        }
    }

    //~ Instance fields --------------------------------------------------------

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    @Getter private final SearchInfo searchInfo;
    @Getter @Setter private Subject subject = Subject.NAME;
    @Getter @Setter private String representationPattern;
    @Getter @Setter private String[] representationFields;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new LightweightMetaObjectsByQuerySearch object.
     */
    public BaumGebietLightweightSearch() {
        this(Subject.NAME, TOSTRING_TEMPLATE, TOSTRING_FIELDS);
    }

    /**
     * Creates a new BaumGebietLightweightSearch object.
     *
     * @param  subject                DOCUMENT ME!
     * @param  representationPattern  DOCUMENT ME!
     * @param  representationFields   DOCUMENT ME!
     */
    public BaumGebietLightweightSearch(
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
     * @param   subject     DOCUMENT ME!
     * @param   limitCount  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static String createUnionQuery(final Subject subject) {
        final List<String> queries = new ArrayList<>();
        /*
            queries.add(String.format(
                    "SELECT " + FIELD__ID + ", " + FIELD__AZ + ", " + FIELD__NAME + ", '('" + FIELD__MELDUNG_DATUM + "')'"
                    +" FROM (\n%s\n) AS query",
                    createQuery(subject, null, null)));
        
        return String.format("%s\nORDER BY " + FIELD__AZ + ", " + FIELD__NAME, String.join("\nUNION\n", queries));*/
        String query = "SELECT " + TABLE_NAME + "." + FIELD__ID + ", " 
                                 + TABLE_NAME + "." + FIELD__AZ + ", " 
                                 + TABLE_NAME + "." + FIELD__NAME + ", " 
                                 + "'[' || " + TABLE_NAME_MELDUNG + "."  + FIELD__MELDUNG_DATUM + " || ']' AS datum"
                    + " FROM " + TABLE_NAME_MELDUNG
                    + " LEFT JOIN " + TABLE_NAME 
                            + " ON " + TABLE_NAME_MELDUNG + "." + FIELD__MELDUNG_FK + " = " 
                            + TABLE_NAME + "." + FIELD__ID
                    + " ORDER BY " + TABLE_NAME + "." + FIELD__AZ + ", " + TABLE_NAME_MELDUNG + "."  + FIELD__MELDUNG_DATUM;
        return query;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   subject     DOCUMENT ME!
     * @param   limitCount  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static String createQuery(final Subject subject, final Geometry geom, final Integer limitCount) {
        final Collection<String> selectFields = new ArrayList<>();
        final List<String> leftJoins = new ArrayList<>();
        final List<String> whereConditions = new ArrayList<>();
        final List<String> groupBys = new ArrayList<>();
        final List<String> orderBys = new ArrayList<>();

        final String name = String.format("baum_gebiet.%s", subject.toString());
        selectFields.add("baum_gebiet.id");
        selectFields.add(String.format("%s AS name", name));
        selectFields.add("baum_gebiet.aktenzeichen");

        
        orderBys.add(String.format("baum_gebiet.%s", subject.toString()));

        final String select = "SELECT " + String.join(", ", selectFields);
        final String from = "FROM baum_gebiet"
                    + (leftJoins.isEmpty() ? "" : ("\nLEFT JOIN " + String.join("\nLEFT JOIN ", leftJoins)));
        final String where = whereConditions.isEmpty() ? "" : (" WHERE " + String.join(" AND ", whereConditions));
        final String groupBy = groupBys.isEmpty() ? "" : ("GROUP BY " + String.join(", ", groupBys));
        final String orderBy = orderBys.isEmpty() ? "" : ("ORDER BY " + String.join(", ", orderBys));
        final String limit = (limitCount != null) ? String.format("LIMIT %s", Integer.toString(limitCount)) : "";

        return String.format("%s\n%s\n%s\n%s\n%s\n%s", select, from, where, groupBy, orderBy, limit);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   args  DOCUMENT ME!
     *
     * @throws  ParseException  DOCUMENT ME!
     */
    public static void main(final String[] args) throws ParseException {
        System.out.println(BaumGebietLightweightSearch.createUnionQuery(Subject.NAME));
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

        final String query = createUnionQuery(subject);

        try {
            final MetaClass mc = CidsBean.getMetaClassFromTableName(
                    "WUNDA_BLAU",
                    TABLE_NAME,
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
