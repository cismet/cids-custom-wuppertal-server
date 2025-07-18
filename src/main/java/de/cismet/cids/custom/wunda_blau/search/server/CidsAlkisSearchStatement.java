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
import Sirius.server.search.SearchRuntimeException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.vividsolutions.jts.geom.Geometry;

import de.aedsicad.aaaweb.rest.api.AlkisSucheApi;
import de.aedsicad.aaaweb.rest.client.ApiException;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.cismet.cids.custom.utils.alkis.AlkisAccessProvider;
import de.cismet.cids.custom.utils.alkis.AlkisRestConf;

import de.cismet.cids.server.search.AbstractCidsServerSearch;
import de.cismet.cids.server.search.MetaObjectNodeServerSearch;

import de.cismet.cismap.commons.jtsgeometryfactories.PostGisGeometryFactory;

import de.cismet.connectioncontext.ConnectionContext;
import de.cismet.connectioncontext.ConnectionContextStore;

/**
 * DOCUMENT ME!
 *
 * @author   stefan
 * @version  $Revision$, $Date$
 */
public class CidsAlkisSearchStatement extends AbstractCidsServerSearch implements MetaObjectNodeServerSearch,
    ConnectionContextStore {

    //~ Static fields/initializers ---------------------------------------------

    /** LOGGER. */
    private static final transient Logger LOG = Logger.getLogger(CidsAlkisSearchStatement.class);
    private static final String INTERSECTS_BUFFER = SearchProperties.getInstance().getIntersectsBuffer();
    public static String WILDCARD = "%";
    private static final int TIMEOUT = 100000;

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Resulttyp {

        //~ Enum constants -----------------------------------------------------

        FLURSTUECK, BUCHUNGSBLATT
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum SucheUeber {

        //~ Enum constants -----------------------------------------------------

        FLURSTUECKSNUMMER, BUCHUNGSBLATTNUMMER, EIGENTUEMER
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Personentyp {

        //~ Enum constants -----------------------------------------------------

        MANN, FRAU, FIRMA
    }

    //~ Instance fields --------------------------------------------------------

    private Resulttyp resulttyp = Resulttyp.FLURSTUECK;
    private String name;
    private String vorname;
    private String geburtsname;
    private String geburtstag;
    private Personentyp ptyp = null;
    private String flurstuecksnummer = null;
    private String buchungsblattnummer = null;
    private SucheUeber ueber = null;
    private Geometry geometry = null;
    private boolean useWildcardForBuchungsblattsearch = true;

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new CidsAlkisSearchStatement object.
     *
     * @param  resulttyp                               DOCUMENT ME!
     * @param  ueber                                   DOCUMENT ME!
     * @param  flurstuecksnummerOrBuchungsblattnummer  DOCUMENT ME!
     * @param  geometry                                DOCUMENT ME!
     */
    public CidsAlkisSearchStatement(final Resulttyp resulttyp,
            final SucheUeber ueber,
            final String flurstuecksnummerOrBuchungsblattnummer,
            final Geometry geometry) {
        this(resulttyp, ueber, flurstuecksnummerOrBuchungsblattnummer, geometry, true);
    }

    /**
     * Creates a new CidsAlkisSearchStatement object.
     *
     * @param  resulttyp                               DOCUMENT ME!
     * @param  ueber                                   DOCUMENT ME!
     * @param  flurstuecksnummerOrBuchungsblattnummer  DOCUMENT ME!
     * @param  geometry                                DOCUMENT ME!
     * @param  useWildcardForBuchungsblattsearch       DOCUMENT ME!
     */
    public CidsAlkisSearchStatement(final Resulttyp resulttyp,
            final SucheUeber ueber,
            final String flurstuecksnummerOrBuchungsblattnummer,
            final Geometry geometry,
            final boolean useWildcardForBuchungsblattsearch) {
        this.resulttyp = resulttyp;
        this.ueber = ueber;
        if (ueber == SucheUeber.FLURSTUECKSNUMMER) {
            flurstuecksnummer = flurstuecksnummerOrBuchungsblattnummer;
        } else if (ueber == SucheUeber.BUCHUNGSBLATTNUMMER) {
            buchungsblattnummer = flurstuecksnummerOrBuchungsblattnummer;
        }
        this.geometry = geometry;
        this.useWildcardForBuchungsblattsearch = useWildcardForBuchungsblattsearch;
    }

    /**
     * Creates a new CidsBaulastSearchStatement object.
     *
     * @param  resulttyp    searchInfo DOCUMENT ME!
     * @param  name         DOCUMENT ME!
     * @param  vorname      DOCUMENT ME!
     * @param  geburtsname  DOCUMENT ME!
     * @param  geburtstag   DOCUMENT ME!
     * @param  ptyp         DOCUMENT ME!
     * @param  g            DOCUMENT ME!
     */
    public CidsAlkisSearchStatement(final Resulttyp resulttyp,
            final String name,
            final String vorname,
            final String geburtsname,
            final String geburtstag,
            final Personentyp ptyp,
            final Geometry g) {
        this.resulttyp = resulttyp;
        this.ueber = SucheUeber.EIGENTUEMER;
        String lengthTest = name;
        this.name = (lengthTest.length() > 0) ? lengthTest : null;
        lengthTest = vorname;
        this.vorname = (lengthTest.length() > 0) ? lengthTest : null;
        lengthTest = geburtsname;
        this.geburtsname = (lengthTest.length() > 0) ? lengthTest : null;
        lengthTest = geburtstag;
        this.geburtstag = (lengthTest.length() > 0) ? lengthTest : null;
        this.ptyp = ptyp;
        geometry = g;
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public void initWithConnectionContext(final ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }

    @Override
    public Collection<MetaObjectNode> performServerSearch() {
        try {
            final List<MetaObjectNode> result = new ArrayList<>();
            final AlkisAccessProvider accessProvider = new AlkisAccessProvider(AlkisRestConf.loadFromDomainServer(
                        getUser(),
                        (ActionService)getActiveLocalServers().get("WUNDA_BLAU"),
                        getConnectionContext()));
            final AlkisSucheApi searchService = accessProvider.getAlkisSearchService();

            String query = null;

            switch (ueber) {
                case EIGENTUEMER: {
                    String salutation = null;
                    if (ptyp == Personentyp.MANN) {
                        salutation = "2000"; // NOI18N
                    } else if (ptyp == Personentyp.FRAU) {
                        salutation = "1000"; // NOI18N
                    } else if (ptyp == Personentyp.FIRMA) {
                        salutation = "3000"; // NOI18N
                    }
                    final String aToken = accessProvider.login();
                    final List<String> ownersIds = searchService.searchOwnersWithAttributes(
                            aToken,
                            accessProvider.getAlkisRestConf().getConfiguration(),
                            salutation,
                            vorname,
                            name,
                            geburtsname,
                            geburtstag,
                            null,
                            null,
                            TIMEOUT);
                    accessProvider.logout(aToken);

                    if (ownersIds != null) {
                        final StringBuilder whereClauseBuilder = new StringBuilder(ownersIds.size() * 20);
                        for (final String oid : ownersIds) {
                            if (whereClauseBuilder.length() > 0) {
                                whereClauseBuilder.append(',');
                            }
                            whereClauseBuilder.append('\'').append(StringEscapeUtils.escapeSql(oid)).append('\'');
                        }
                        if (resulttyp == Resulttyp.FLURSTUECK) {
                            query =
                                "select distinct (select id from cs_class where table_name ilike 'alkis_landparcel') as class_id, lp.id as object_id, lp.alkis_id from alkis_landparcel lp,alkis_flurstueck_to_buchungsblaetter jt,alkis_buchungsblatt bb,ownerofbb where lp.buchungsblaetter=jt.flurstueck_reference and jt.buchungsblatt=bb.id and bb.buchungsblattcode = ownerofbb.bb and ownerofbb.ownerid in ("
                                        + whereClauseBuilder
                                        + ")";
                        } else {
                            query =
                                "select distinct (select id from cs_class where table_name ilike 'alkis_buchungsblatt') as class_id, jt.buchungsblatt as object_id,bb.buchungsblattcode from alkis_landparcel lp,alkis_flurstueck_to_buchungsblaetter jt,alkis_buchungsblatt bb,ownerofbb where lp.buchungsblaetter=jt.flurstueck_reference and jt.buchungsblatt=bb.id and bb.buchungsblattcode = ownerofbb.bb and ownerofbb.ownerid in ("
                                        + whereClauseBuilder
                                        + ")";
                        }
                        break;
                    }
                    break;
                }
                case BUCHUNGSBLATTNUMMER: {
                    if (resulttyp == Resulttyp.FLURSTUECK) {
                        if (useWildcardForBuchungsblattsearch) {
                            query =
                                "select distinct (select id from cs_class where table_name ilike 'alkis_landparcel') as class_id, lp.id as object_id, lp.alkis_id from alkis_landparcel lp,alkis_flurstueck_to_buchungsblaetter jt,alkis_buchungsblatt bb where lp.buchungsblaetter=jt.flurstueck_reference and jt.buchungsblatt=bb.id and bb.buchungsblattcode ilike '"
                                        + buchungsblattnummer
                                        + WILDCARD
                                        + "'";
                        } else {
                            query =
                                "select distinct (select id from cs_class where table_name ilike 'alkis_landparcel') as class_id, lp.id as object_id, lp.alkis_id from alkis_landparcel lp,alkis_flurstueck_to_buchungsblaetter jt,alkis_buchungsblatt bb where lp.buchungsblaetter=jt.flurstueck_reference and jt.buchungsblatt=bb.id and bb.buchungsblattcode ilike '"
                                        + buchungsblattnummer
                                        + "'";
                        }
                    } else {
                        if (useWildcardForBuchungsblattsearch) {
                            query =
                                "select distinct (select id from cs_class where table_name ilike 'alkis_buchungsblatt') as class_id, jt.buchungsblatt as object_id,bb.buchungsblattcode from alkis_landparcel lp,alkis_flurstueck_to_buchungsblaetter jt,alkis_buchungsblatt bb where lp.buchungsblaetter=jt.flurstueck_reference and jt.buchungsblatt=bb.id and bb.buchungsblattcode ilike '"
                                        + buchungsblattnummer
                                        + WILDCARD
                                        + "'";
                        } else {
                            query =
                                "select distinct (select id from cs_class where table_name ilike 'alkis_buchungsblatt') as class_id, jt.buchungsblatt as object_id,bb.buchungsblattcode from alkis_landparcel lp,alkis_flurstueck_to_buchungsblaetter jt,alkis_buchungsblatt bb where lp.buchungsblaetter=jt.flurstueck_reference and jt.buchungsblatt=bb.id and bb.buchungsblattcode ilike '"
                                        + buchungsblattnummer
                                        + "'";
                        }
                    }
                    break;
                }

                case FLURSTUECKSNUMMER: {
                    String flurstueckClause;
                    if (flurstuecksnummer.endsWith("/%")) {
                        flurstueckClause = "(lp.alkis_id ilike '"
                                    + flurstuecksnummer
                                    + "' or lp.alkis_id ilike '"
                                    + flurstuecksnummer.substring(0, flurstuecksnummer.length() - 2)
                                    + "')";
                    } else {
                        flurstueckClause = "lp.alkis_id ilike '"
                                    + flurstuecksnummer
                                    + "'";
                    }
                    if (resulttyp == Resulttyp.FLURSTUECK) {
                        query =
                            "select distinct (select id from cs_class where table_name ilike 'alkis_landparcel') as class_id, lp.id as object_id, lp.alkis_id from alkis_landparcel lp where "
                                    + flurstueckClause;
                    } else {
                        query =
                            "select distinct (select id from cs_class where table_name ilike 'alkis_buchungsblatt') as class_id, jt.buchungsblatt as object_id,bb.buchungsblattcode from  alkis_landparcel lp,alkis_flurstueck_to_buchungsblaetter jt,alkis_buchungsblatt bb where lp.buchungsblaetter=jt.flurstueck_reference and jt.buchungsblatt=bb.id and "
                                    + flurstueckClause;
                    }
                    break;
                }
            }
            if ((geometry != null) && (query != null)) {
                final String geostring = PostGisGeometryFactory.getPostGisCompliantDbString(geometry);
                query += " and st_intersects("
                            + "geometrie"
                            + ","
                            + "st_buffer(st_GeomFromEWKT('"
                            + geostring
                            + "'), "
                            + INTERSECTS_BUFFER
                            + "))";
            }

            if (LOG.isInfoEnabled()) {
                LOG.info("Search:\n" + query);
            }

            if (query != null) {
                final MetaService ms = (MetaService)getActiveLocalServers().get("WUNDA_BLAU");

                final List<ArrayList> resultList = ms.performCustomSearch(query, getConnectionContext());
                for (final ArrayList al : resultList) {
                    final int cid = (Integer)al.get(0);
                    final int oid = (Integer)al.get(1);
                    final String nodename = (String)al.get(2);
                    final MetaObjectNode mon = new MetaObjectNode("WUNDA_BLAU", oid, cid, nodename, null, null); // TODO: Check4CashedGeomAndLightweightJson

                    result.add(mon);
                }
            }
            return result;
        } catch (final ApiException e) {
            if (e.getResponseBody() != null) {
                LOG.error("Problem" + e.getResponseBody(), e);
                String message = e.getResponseBody();

                try {
                    final ObjectMapper map = new ObjectMapper();
                    final JsonNode node = map.readTree(message);
                    JsonNode tmpMessage = node.get("error");

                    if ((tmpMessage != null) && (tmpMessage.get("message") != null)) {
                        tmpMessage = tmpMessage.get("message");
                        message = "<html>"
                                    + tmpMessage.textValue();
                    }
                } catch (Exception ex) {
                    LOG.error("Cannot parse exception", ex);
                }

                throw new SearchRuntimeException(message);
            } else {
                LOG.error("Problem", e);
                throw new RuntimeException(e);
            }
        } catch (final Exception e) {
            LOG.error("Problem", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }
}
