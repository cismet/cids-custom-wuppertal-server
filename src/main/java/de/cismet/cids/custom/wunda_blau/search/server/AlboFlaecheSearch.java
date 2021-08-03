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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.vividsolutions.jts.geom.Geometry;

import lombok.Getter;
import lombok.Setter;

import org.apache.log4j.Logger;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.cismet.cids.server.search.AbstractCidsServerSearch;
import de.cismet.cids.server.search.MetaObjectNodeServerSearch;

import de.cismet.cismap.commons.jtsgeometryfactories.PostGisGeometryFactory;

import de.cismet.connectioncontext.ConnectionContext;

/**
 * DOCUMENT ME!
 *
 * @version  $Revision$, $Date$
 */
public class AlboFlaecheSearch extends AbstractCidsServerSearch implements MetaObjectNodeServerSearch,
    StorableSearch<AlboFlaecheSearch.Configuration> {

    //~ Static fields/initializers ---------------------------------------------

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        final SimpleModule module = new SimpleModule();
        module.addDeserializer(ArtInfo.class, new ArtInfoDeserializer());
        OBJECT_MAPPER.registerModule(module);
    }

    private static final transient Logger LOG = Logger.getLogger(AlboFlaecheSearch.class);
    private static final String QUERY_TEMPLATE = "SELECT DISTINCT ON (flaeche.erhebungsnummer) "
                + "(SELECT c.id FROM cs_class c WHERE table_name ILIKE 'albo_flaeche') AS class_id, flaeche.id AS object_id, flaeche.erhebungsnummer || ' [' || art.schluessel || ']' AS name "
                + "FROM albo_flaeche AS flaeche "
                + "LEFT JOIN albo_flaechenart AS art ON flaeche.fk_art = art.id "
                + "%s "
                + "WHERE %s "
                + "ORDER BY flaeche.erhebungsnummer";

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum SearchMode {

        //~ Enum constants -----------------------------------------------------

        AND, OR,
    }

    //~ Instance fields --------------------------------------------------------

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    @Getter private Configuration configuration;
    @Getter @Setter private Geometry geometry;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new AlboFlaecheSearch object.
     */
    public AlboFlaecheSearch() {
        this(new Configuration());
    }

    /**
     * Creates a new AlboFlaecheSearch object.
     *
     * @param  searchInfo  DOCUMENT ME!
     */
    public AlboFlaecheSearch(final Configuration searchInfo) {
        this.configuration = searchInfo;
    }

    /**
     * Creates a new AlboFlaecheSearch object.
     *
     * @param   searchInfo  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public AlboFlaecheSearch(final String searchInfo) throws Exception {
        this((searchInfo != null) ? OBJECT_MAPPER.readValue(searchInfo, Configuration.class) : null);
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param  connectionContext  DOCUMENT ME!
     */
    public void initWithConnectionContext(final ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public AlboFlaecheLightweightSearch getLightweightSearch() {
        return new AlboFlaecheLightweightSearch(this);
    }

    @Override
    public void setConfiguration(final Configuration searchConfiguration) {
        this.configuration = searchConfiguration;
    }

    @Override
    public void setConfiguration(final Object searchConfiguration) {
        this.configuration = (searchConfiguration instanceof Configuration) ? (Configuration)searchConfiguration : null;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    @Override
    public String createQuery() {
        final Configuration configuration = getConfiguration();
        if (configuration != null) {
            final String buffer = SearchProperties.getInstance().getIntersectsBuffer();
            final List<String> leftJoins = new ArrayList<>();
            final Collection<String> wheres = new ArrayList<>();
            final Collection<String> wheresMain = new ArrayList<>();
            final Collection<String> wheresArt = new ArrayList<>();

            leftJoins.add("albo_vorgang_flaeche AS arr ON flaeche.id = arr.fk_flaeche");

            if ((configuration.getErhebungsNummer() != null) && !configuration.getErhebungsNummer().isEmpty()) {
                wheresMain.add(String.format(
                        "flaeche.erhebungsnummer ILIKE '%%%s%%'",
                        configuration.getErhebungsNummer()));
            }

            if ((configuration.getVorgangSchluessel() != null) && !configuration.getVorgangSchluessel().isEmpty()) {
                leftJoins.add("albo_vorgang AS vorgang ON vorgang.arr_flaechen = arr.vorgang_reference");
                wheresMain.add(String.format("vorgang.schluessel LIKE '%%%s%%'", configuration.getVorgangSchluessel()));
            }

            if ((configuration.getStatusSchluessel() != null) && !configuration.getStatusSchluessel().isEmpty()) {
                leftJoins.add("albo_flaechenstatus AS status ON status.id = flaeche.fk_status");
                wheresMain.add(String.format("status.schluessel LIKE '%s'", configuration.getStatusSchluessel()));
            }

            if ((configuration.getTypSchluessel() != null) && !configuration.getTypSchluessel().isEmpty()) {
                leftJoins.add("albo_flaechentyp AS typ ON typ.id = flaeche.fk_typ");
                wheresMain.add(String.format("typ.schluessel LIKE '%s'", configuration.getTypSchluessel()));
            }

            if ((configuration.getZuordnungSchluessel() != null) && !configuration.getZuordnungSchluessel().isEmpty()) {
                leftJoins.add("albo_flaechenzuordnung AS zuordnung ON zuordnung.id = flaeche.fk_zuordnung");
                wheresMain.add(String.format("zuordnung.schluessel LIKE '%s'", configuration.getZuordnungSchluessel()));
            }

            if (configuration.getUnterdrueckt() != null) {
                wheresMain.add(String.format(
                        "flaeche.loeschen = %s",
                        configuration.getUnterdrueckt() ? "TRUE" : "FALSE"));
            }

            if (configuration.getArtInfos() != null) {
                int artCount = 0;
                for (final ArtInfo artInfo : configuration.getArtInfos()) {
                    final String artSchluessel = (artInfo != null) ? artInfo.getFlaechenartSchluessel() : null;
                    final Collection<String> subAndWheres = new ArrayList<>();
                    if (artSchluessel != null) {
                        final String alias = String.format("%03d", artCount++);
                        leftJoins.add(String.format(
                                "albo_flaechenart AS art%1$s ON art%1$s.id = flaeche.fk_art",
                                alias));
                        subAndWheres.add(String.format("art%s.schluessel LIKE '%s'", alias, artSchluessel));
                        if (artInfo instanceof StandortInfo) {
                            final List<String> subLeftJoins = new ArrayList<>();
                            final String wirtschaftszweig = ((StandortInfo)artInfo).getWzSchluessel();
                            final Integer jahr = ((StandortInfo)artInfo).getJahr();
                            final Boolean jahrModus = ((StandortInfo)artInfo).getJahrModus();
                            final Integer dauer = ((StandortInfo)artInfo).getDauer();
                            final Boolean dauerModus = ((StandortInfo)artInfo).getDauerModus();
                            if (wirtschaftszweig != null) {
                                subLeftJoins.add(String.format(
                                        "albo_standort_wirtschaftszweig AS stwz%1$s ON stwz%1$s.standort_reference = standort%1$s.id",
                                        alias));
                                subLeftJoins.add(String.format(
                                        "albo_wirtschaftszweig AS wz%1$s ON stwz%1$s.fk_wirtschaftszweig = wz%1$s.id",
                                        alias));
                                subAndWheres.add(String.format("wz%s.schluessel LIKE '%s'", alias, wirtschaftszweig));
                            }
                            if (jahr != null) {
                                if (jahrModus == null) {                     // exakt jahr
                                    subAndWheres.add(String.format(
                                            "CASE WHEN standort%1$s.jahr_von IS NOT NULL THEN standort%1$s.jahr_von <= %2$d ELSE TRUE END AND CASE WHEN standort%1$s.jahr_bis IS NOT NULL THEN standort%1$s.jahr_bis >= %2$d ELSE TRUE END",
                                            alias,
                                            jahr));
                                } else if (Boolean.TRUE.equals(jahrModus)) { // nach jahr
                                    subAndWheres.add(String.format(
                                            "CASE WHEN standort%1$s.jahr_bis IS NOT NULL THEN standort%1$s.jahr_bis > %2$d ELSE TRUE END",
                                            alias,
                                            jahr));
                                } else {                                     // vor jahr
                                    subAndWheres.add(String.format(
                                            "CASE WHEN standort%1$s.jahr_von IS NOT NULL THEN standort%1$s.jahr_von < %2$d ELSE TRUE END",
                                            alias,
                                            jahr));
                                }
                            }

                            if (dauer != null) {
                                if (dauerModus == null) {                     // exakt dauer
                                    subAndWheres.add(String.format(
                                            "CASE "
                                                    + "WHEN standort%1$s.jahr_von IS NOT NULL AND standort%1$s.jahr_bis IS NOT NULL "
                                                    + "THEN standort%1$s.jahr_bis - standort%1$s.jahr_von = %2$d ELSE FALSE END",
                                            alias,
                                            dauer));
                                } else if (Boolean.TRUE.equals(dauerModus)) { // länger als dauer
                                    subAndWheres.add(String.format(
                                            "CASE "
                                                    + "WHEN standort%1$s.jahr_von IS NOT NULL AND standort%1$s.jahr_bis IS NOT NULL "
                                                    + "THEN standort%1$s.jahr_bis - standort%1$s.jahr_von >= %2$d ELSE FALSE END",
                                            alias,
                                            dauer));
                                } else {                                      // kürzer als dauer
                                    subAndWheres.add(String.format(
                                            "CASE "
                                                    + "WHEN standort%1$s.jahr_von IS NOT NULL AND standort%1$s.jahr_bis IS NOT NULL "
                                                    + "THEN standort%1$s.jahr_bis - standort%1$s.jahr_von < %2$d ELSE FALSE END",
                                            alias,
                                            dauer));
                                }
                            }

                            if (!subAndWheres.isEmpty()) {
                                leftJoins.add(String.format(
                                        "albo_standort AS standort%1$s ON standort%1$s.fk_flaeche = flaeche.id",
                                        alias));
                                leftJoins.addAll(subLeftJoins);
                            }
                        } else if (artInfo instanceof AltablagerungInfo) {
                            final List<String> subLeftJoins = new ArrayList<>();
                            final String stilllegung = ((AltablagerungInfo)artInfo).getStilllegungSchluessel();
                            final String erhebungsklasse = ((AltablagerungInfo)artInfo).getErhebungsklasseSchluessel();
                            final String verfuellkategorie = ((AltablagerungInfo)artInfo)
                                        .getVerfuellkategorieSchluessel();
                            if (stilllegung != null) {
                                subLeftJoins.add(String.format(
                                        "albo_stilllegung AS stilllegung%1$s ON altablagerung%1$s.fk_stilllegung = stilllegung%1$s.id",
                                        alias));
                                subAndWheres.add(String.format(
                                        "stilllegung%s.schluessel LIKE '%s'",
                                        alias,
                                        stilllegung));
                            }
                            if (erhebungsklasse != null) {
                                subLeftJoins.add(String.format(
                                        "albo_erhebungsklasse AS erhebungsklasse%1$s ON altablagerung%1$s.fk_erhebungsklasse = erhebungsklasse%1$s.id",
                                        alias));
                                subAndWheres.add(String.format(
                                        "erhebungsklasse%s.schluessel LIKE '%s'",
                                        alias,
                                        erhebungsklasse));
                            }
                            if (verfuellkategorie != null) {
                                subLeftJoins.add(String.format(
                                        "albo_verfuellkategorie AS verfuellkategorie%1$s ON altablagerung%1$s.fk_verfuellkategorie = verfuellkategorie%1$s.id",
                                        alias));
                                subAndWheres.add(String.format(
                                        "verfuellkategorie%s.schluessel LIKE '%s'",
                                        alias,
                                        verfuellkategorie));
                            }
                            if (!subAndWheres.isEmpty()) {
                                leftJoins.add(String.format(
                                        "albo_altablagerung AS altablagerung%1$s ON flaeche.fk_altablagerung = altablagerung%1$s.id",
                                        alias));
                                leftJoins.addAll(subLeftJoins);
                            }
                        } else if (artInfo instanceof MaterialaufbringungInfo) {
                            final List<String> subLeftJoins = new ArrayList<>();
                            final String materialaufbringungsart = ((MaterialaufbringungInfo)artInfo)
                                        .getMaterialaufbringungsartSchluessel();
                            if (materialaufbringungsart != null) {
                                subLeftJoins.add(String.format(
                                        "albo_materialaufbringungsart AS materialaufbringungsart%1$s ON materialaufbringung%1$s.fk_art = materialaufbringungsart%1$s.id",
                                        alias));
                                subAndWheres.add(String.format(
                                        "materialaufbringungsart%s.schluessel LIKE '%s'",
                                        alias,
                                        materialaufbringungsart));
                            }
                            if (!subAndWheres.isEmpty()) {
                                leftJoins.add(String.format(
                                        "albo_materialaufbringung AS materialaufbringung%1$s ON flaeche.fk_materialaufbringung = materialaufbringung%1$s.id",
                                        alias));
                                leftJoins.addAll(subLeftJoins);
                            }
                        }
                    }
                    if (!subAndWheres.isEmpty()) {
                        wheresArt.add(String.format("(%s)", String.join(" AND ", subAndWheres)));
                    }
                }
            }

            if (getGeometry() != null) {
                final String geomString = PostGisGeometryFactory.getPostGisCompliantDbString(getGeometry());
                leftJoins.add("geom ON flaeche.fk_geom = geom.id");
                wheres.add(String.format("geom.geo_field && GeometryFromText('%s')", geomString));
                wheres.add(String.format(
                        "intersects(st_buffer(geo_field, %s), GeometryFromText('%s'))",
                        buffer,
                        geomString));
            }

            if (!wheresMain.isEmpty()) {
                switch (configuration.getSearchModeMain()) {
                    case AND: {
                        wheres.add(String.format("(%s)", String.join(" AND ", wheresMain)));
                    }
                    break;
                    case OR: {
                        wheres.add(String.format("(%s)", String.join(" OR ", wheresMain)));
                    }
                }
            }

            if (!wheresArt.isEmpty()) {
                switch (configuration.getSearchModeArt()) {
                    case AND: {
                        wheres.add(String.format("(%s)", String.join(" AND ", wheresArt)));
                    }
                    break;
                    case OR: {
                        wheres.add(String.format("(%s)", String.join(" OR ", wheresArt)));
                    }
                }
            }

            final String leftJoin = (!leftJoins.isEmpty())
                ? String.format("LEFT JOIN %s", String.join(" LEFT JOIN ", leftJoins)) : "";
            final String where = (!wheres.isEmpty()) ? String.join(" AND ", wheres) : "TRUE";
            final String query = String.format(QUERY_TEMPLATE, leftJoin, where);
            return query;
        } else {
            return null;
        }
    }

    @Override
    public Collection<MetaObjectNode> performServerSearch() {
        try {
            final String query = createQuery();

            final List<MetaObjectNode> mons = new ArrayList<>();
            final MetaService ms = (MetaService)getActiveLocalServers().get("WUNDA_BLAU");

            final List<ArrayList> resultList = ms.performCustomSearch(query, getConnectionContext());
            for (final ArrayList al : resultList) {
                final int cid = (Integer)al.get(0);
                final int oid = (Integer)al.get(1);
                final String name = String.valueOf(al.get(2));
                final MetaObjectNode mon = new MetaObjectNode("WUNDA_BLAU", oid, cid, name, null, null);

                mons.add(mon);
            }
            return mons;
        } catch (final Exception ex) {
            LOG.error("error while searching for albo_flaeche", ex);
            throw new RuntimeException(ex);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @Setter
    @JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
    )
    public static class Configuration extends StorableSearch.Configuration {

        //~ Instance fields ----------------------------------------------------

        @JsonProperty private String vorgangSchluessel;
        @JsonProperty private String erhebungsNummer;
        @JsonProperty private String statusSchluessel;
        @JsonProperty private String typSchluessel;
        @JsonProperty private String zuordnungSchluessel;
        @JsonProperty private Boolean unterdrueckt = Boolean.FALSE;
        @JsonProperty private SearchMode searchModeMain = SearchMode.AND;
        @JsonProperty private SearchMode searchModeArt = SearchMode.AND;
        @JsonProperty private Collection<ArtInfo> artInfos;
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @Setter
    @JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
    )
    public abstract static class ArtInfo extends StorableSearch.Configuration {

        //~ Instance fields ----------------------------------------------------

        @JsonProperty private final String flaechenartSchluessel;

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new ArtInfo object.
         *
         * @param  flaechenartSchluessel  DOCUMENT ME!
         */
        protected ArtInfo(final String flaechenartSchluessel) {
            this.flaechenartSchluessel = flaechenartSchluessel;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @Setter
    @JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
    )
    public abstract static class StandortInfo extends ArtInfo {

        //~ Instance fields ----------------------------------------------------

        @JsonProperty private String wzSchluessel;
        @JsonProperty private Integer jahr;
        @JsonProperty private Boolean jahrModus;
        @JsonProperty private Integer dauer;
        @JsonProperty private Boolean dauerModus;

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new StandortInfo object.
         *
         * @param  artSchluessel  DOCUMENT ME!
         */
        public StandortInfo(final String artSchluessel) {
            super(artSchluessel);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @Setter
    @JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
    )
    public static class AltstandortInfo extends StandortInfo {

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new AltstandortInfo object.
         */
        public AltstandortInfo() {
            super("altstandort");
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @Setter
    @JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
    )
    public static class OhneVerdachtInfo extends ArtInfo {

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new OhneVerdachtInfo object.
         */
        public OhneVerdachtInfo() {
            super("ohne_verdacht");
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @Setter
    @JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
    )
    public static class SonstigeInfo extends ArtInfo {

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new SonstigeInfo object.
         */
        public SonstigeInfo() {
            super("sonstige");
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @Setter
    @JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
    )
    public static class StofflicheInfo extends ArtInfo {

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new SonstigeInfo object.
         */
        public StofflicheInfo() {
            super("stoffliche");
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @Setter
    @JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
    )
    public static class BodenaehnlichInfo extends ArtInfo {

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new SonstigeInfo object.
         */
        public BodenaehnlichInfo() {
            super("bodenaehnlich");
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @Setter
    @JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
    )
    public static class AbraumhaldeInfo extends ArtInfo {

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new SonstigeInfo object.
         */
        public AbraumhaldeInfo() {
            super("abraumhalde");
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @Setter
    @JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
    )
    public static class InBetriebInfo extends ArtInfo {

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new SonstigeInfo object.
         */
        public InBetriebInfo() {
            super("in_betrieb");
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @Setter
    @JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
    )
    public static class RclInfo extends ArtInfo {

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new SonstigeInfo object.
         */
        public RclInfo() {
            super("rcl");
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @Setter
    @JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
    )
    public static class BaugrundInfo extends ArtInfo {

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new SonstigeInfo object.
         */
        public BaugrundInfo() {
            super("baugrund");
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @Setter
    @JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
    )
    public static class SonstigesInfo extends ArtInfo {

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new SonstigeInfo object.
         */
        public SonstigesInfo() {
            super("sonstiges");
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @Setter
    @JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
    )
    public static class MaterialaufbringungInfo extends ArtInfo {

        //~ Instance fields ----------------------------------------------------

        @JsonProperty private String materialaufbringungsartSchluessel;

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new MaterialaufbringungInfo object.
         */
        public MaterialaufbringungInfo() {
            this(null);
        }

        /**
         * Creates a new MaterialaufbringungInfo object.
         *
         * @param  materialaufbringungsartSchluessel  DOCUMENT ME!
         */
        public MaterialaufbringungInfo(final String materialaufbringungsartSchluessel) {
            super("materialaufbringung");
            this.materialaufbringungsartSchluessel = materialaufbringungsartSchluessel;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @Setter
    @JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
    )
    public static class AltablagerungInfo extends ArtInfo {

        //~ Instance fields ----------------------------------------------------

        @JsonProperty private String stilllegungSchluessel;
        @JsonProperty private String verfuellkategorieSchluessel;
        @JsonProperty private String erhebungsklasseSchluessel;

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new AltablagerungInfo object.
         */
        public AltablagerungInfo() {
            super("altablagerung");
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    @Getter
    @Setter
    @JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
    )
    public static class BetriebsstandortInfo extends StandortInfo {

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new BetrienbsstandortInfo object.
         */
        public BetriebsstandortInfo() {
            super("betriebsstandort");
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public static class ArtInfoDeserializer extends StdDeserializer<ArtInfo> {

        //~ Instance fields ----------------------------------------------------

        private final ObjectMapper defaultMapper = new ObjectMapper();

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new CidsBeanJsonDeserializer object.
         */
        public ArtInfoDeserializer() {
            super(ArtInfoDeserializer.class);

            defaultMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public ArtInfo deserialize(final JsonParser jp, final DeserializationContext dc) throws IOException,
            JsonProcessingException {
            final ObjectNode on = jp.readValueAsTree();
            if (on.has("flaechenartSchluessel")) {
                final String flaechenartSchluessel = (String)defaultMapper.treeToValue(on.get("flaechenartSchluessel"),
                        String.class);
                switch (flaechenartSchluessel) {
                    case "betriebsstandort": {
                        return defaultMapper.treeToValue(on, BetriebsstandortInfo.class);
                    }
                    case "altstandort": {
                        return defaultMapper.treeToValue(on, AltstandortInfo.class);
                    }
                    case "altablagerung": {
                        return defaultMapper.treeToValue(on, AltablagerungInfo.class);
                    }
                    case "materialaufbringung": {
                        return defaultMapper.treeToValue(on, MaterialaufbringungInfo.class);
                    }
                    case "sonstige": {
                        return defaultMapper.treeToValue(on, SonstigeInfo.class);
                    }
                    case "ohne_verdacht": {
                        return defaultMapper.treeToValue(on, OhneVerdachtInfo.class);
                    }
                }
            }
            throw new RuntimeException("invalid ArtInfo");
        }
    }
}
