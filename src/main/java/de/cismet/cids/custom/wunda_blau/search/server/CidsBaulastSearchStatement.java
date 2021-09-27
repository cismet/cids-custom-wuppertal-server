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

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
public class CidsBaulastSearchStatement extends AbstractCidsServerSearch implements MetaObjectNodeServerSearch,
    ConnectionContextStore {

    //~ Static fields/initializers ---------------------------------------------

    /** LOGGER. */
    private static final transient Logger LOG = Logger.getLogger(CidsBaulastSearchStatement.class);
    private static final String INTERSECTS_BUFFER = SearchProperties.getInstance().getIntersectsBuffer();

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Result {

        //~ Enum constants -----------------------------------------------------

        BAULAST, BAULASTBLATT
    }

    //~ Instance fields --------------------------------------------------------

    //
    private String blattnummer;
    //
    private Result result;
    //
    private boolean gueltig;
    private boolean ungueltig;
    //
    private boolean belastet;
    private boolean beguenstigt;
    //
    private Geometry geometry;
    //
    private List<FlurstueckInfo> flurstuecke;
    //
    private String art;
    private final int baulastClassID;
    private final int baulastblattClassID;
    private String blattnummerquerypart = "";
    private String gueltigquerypart = "";
    private String ungueltigquerypart = "";
    private String geoquerypart = "";
    private String fsquerypart = "";
    private String artquerypart = "";

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new CidsBaulastSearchStatement object.
     *
     * @param  searchInfo           DOCUMENT ME!
     * @param  baulastClassID       DOCUMENT ME!
     * @param  baulastblattClassID  DOCUMENT ME!
     */
    public CidsBaulastSearchStatement(final BaulastSearchInfo searchInfo,
            final int baulastClassID,
            final int baulastblattClassID) {
        this.baulastClassID = baulastClassID;
        this.baulastblattClassID = baulastblattClassID;
        this.result = searchInfo.getResult();
        this.blattnummer = searchInfo.getBlattnummer();
        if (blattnummer != null) {
            blattnummer = StringEscapeUtils.escapeSql(blattnummer);
        }
        this.gueltig = searchInfo.isGueltig();
        this.ungueltig = searchInfo.isUngueltig();
        this.beguenstigt = searchInfo.isBeguenstigt();
        this.belastet = searchInfo.isBelastet();
        this.art = searchInfo.getArt();
        if (art != null) {
            art = StringEscapeUtils.escapeSql(art);
        }
        this.geometry = searchInfo.getGeometry();
        this.flurstuecke = searchInfo.getFlurstuecke();

        if ((blattnummer != null) && (blattnummer.length() > 0)) {
            // ^             beginning of line
            // [0]*          preceded by any amount of 0s
            // [[:alpha:]]?  possibly followed by one letter (issue 2156)
            // $             end of line
            blattnummerquerypart = " and l.blattnummer ~* '^[0]*" + blattnummer + "[[:alpha:]]?$'";
        }

        if (!(gueltig && ungueltig)) {
            if (!gueltig && !ungueltig) {
                gueltigquerypart = " and false";
            } else {
                if (gueltig) {
                    gueltigquerypart = " and loeschungsdatum is null and geschlossen_am is null";
                } else if (ungueltig) {
                    ungueltigquerypart = " and (loeschungsdatum is not null or geschlossen_am is not null)";
                }
            }
        }

        if ((geometry != null) && !geometry.isEmpty()) {
            final String geomString = PostGisGeometryFactory.getPostGisCompliantDbString(geometry);
            if ((geometry instanceof Polygon) || (geometry instanceof MultiPolygon)) { // with buffer for geostring
                geoquerypart = " and g.geo_field && st_GeometryFromText('" + geomString
                            + "',25832) and st_intersects(st_buffer(g.geo_field, " + INTERSECTS_BUFFER
                            + "),st_buffer(st_GeometryFromText('"
                            + geomString
                            + "',25832), " + INTERSECTS_BUFFER + "))";
            } else {
                geoquerypart = " and g.geo_field && st_GeometryFromText('" + geomString
                            + "',25832) and st_intersects(st_buffer(g.geo_field, " + INTERSECTS_BUFFER
                            + "),st_GeometryFromText('" + geomString
                            + "',25832))";
            }
        }

        if ((art != null) && (art.length() > 0)) {
            artquerypart = " and l.id = la.baulast_reference and la.baulast_art = a.id and a.baulast_art = '" + art
                        + "'";
        }

        fsquerypart = getSqlByFlurstuecksInfo(flurstuecke);
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public void initWithConnectionContext(final ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }

    @Override
    public Collection<MetaObjectNode> performServerSearch() {
        try {
            final String primary = getPrimaryQuery();
            final String secondary = getSecondaryQuery();
            final MetaService ms = (MetaService)getActiveLocalServers().get("WUNDA_BLAU");
            final List<ArrayList> primaryResultList = ms.performCustomSearch(primary, getConnectionContext());

            final List<MetaObjectNode> aln = new ArrayList<MetaObjectNode>();
            for (final ArrayList al : primaryResultList) {
                final int cid = (Integer)al.get(0);
                final int oid = (Integer)al.get(1);
                final MetaObjectNode mon = new MetaObjectNode("WUNDA_BLAU", oid, cid, (String)al.get(2), null, null); // TODO: Check4CashedGeomAndLightweightJson

                aln.add(mon);
            }

            if ((flurstuecke != null) && (flurstuecke.size() > 0)) {
                final List<ArrayList> secondaryResultList = ms.performCustomSearch(
                        secondary,
                        getConnectionContext());
                for (final ArrayList al : secondaryResultList) {
                    final int cid = (Integer)al.get(0);
                    final int oid = (Integer)al.get(1);
                    final MetaObjectNode mon = new MetaObjectNode(
                            "WUNDA_BLAU",
                            oid,
                            cid,
                            "indirekt: "
                                    + (String)al.get(2),
                            null,
                            null); // TODO: Check4CashedGeomAndLightweightJson

                    aln.add(mon);
                }
            }
            return aln;
        } catch (Exception e) {
            LOG.error("Problem der Baulastensuche", e);
            throw new RuntimeException("Problem der Baulastensuche", e);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   fis  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String getSqlByFlurstuecksInfo(final Collection<FlurstueckInfo> fis) {
        assert (fis != null);
        String queryPart = "";
        if (fis.size() > 0) {
            queryPart += " AND ( ";
            for (final FlurstueckInfo fi : fis) {
                queryPart += getSqlByFlurstuecksInfo(fi);
                queryPart += " or ";
            }
            queryPart = queryPart.substring(0, queryPart.length() - 4); // letztes " or " wieder entfernen
            queryPart += " ) ";
        }
        return queryPart;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   fi  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String getSqlByFlurstuecksInfo(final FlurstueckInfo fi) {
        return " ( k.gemarkung = '" + fi.gemarkung + "' and k.flur = '"
                    + StringEscapeUtils.escapeSql(fi.flur) + "' and k.zaehler = '"
                    + StringEscapeUtils.escapeSql(fi.zaehler) + "' and k.nenner = '"
                    + StringEscapeUtils.escapeSql(fi.nenner) + "' ) ";
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String getBelastetBeguenstigtSubselect() {
        if (belastet || beguenstigt) {
            String subselect = "";
            subselect += " (";
            if (beguenstigt) {
                subselect += " select * from alb_baulast_flurstuecke_beguenstigt";
                if (belastet) {
                    subselect += " UNION";
                }
            }
            if (belastet) {
                subselect += " select * from alb_baulast_flurstuecke_belastet";
            }
            subselect += " ) as fsj";
            return subselect;
        } else {
            return " (SELECT * FROM   alb_baulast_flurstuecke_beguenstigt where true=false) AS fsj";
        }
    }

    /**
     * Creates the primary query. A join is made over several tables. To make this join two cases have to be
     * distinguished.<br>
     * 1) Filter for a geometry. In this case more tables have to be joined as the geometry of a landparcel has to be
     * fetched.<br>
     * 2) No geometry involved. In this case fewer tables have to be joined because there are buchungsblaetter without a
     * landparcel. Thus a join with the table flurstueck and geom leads to a wrong result as the join fails.
     *
     * @return  DOCUMENT ME!
     */
    private String getPrimaryQuery() {
        String query = "";

        final String queryBlattPrefix = ""
                    + "SELECT " + baulastblattClassID + "  AS class_id, "
                    + "\n       b.id AS object_id, "
                    + "\n       b.blattnummer "
                    + "\nFROM   alb_baulastblatt b "
                    + "\nWHERE  b.blattnummer IN (SELECT blattnummer "
                    + "\n                         FROM "
                    + "\n       (";
        String queryMid = ""
                    + "\nSELECT " + baulastClassID + "  AS class_id, "
                    + "\n               l.id AS object_id, "
                    + "\n               l.blattnummer|| '/' || case when l.laufende_nummer is not null then l.laufende_nummer else 'keine laufende Nummer' end, "
                    + "\n               l.blattnummer , "
                    + "\n               l.laufende_nummer "
                    + "\n        FROM   alb_baulast l "
                    + "\n               left outer join alb_baulast_baulastarten la on (l.id = la.baulast_reference) "
                    + "\n               left outer join alb_baulast_art a on (la.baulast_art = a.id),"
                    + "\n" + getBelastetBeguenstigtSubselect()
                    + "\n               , "
                    + "\n               alb_flurstueck_kicker k";
        if ((geometry != null) && !geometry.isEmpty()) {
            queryMid += ", "
                        + "\n               flurstueck f, "
                        + "\n               geom g ";
        }
        queryMid += ""
                    + "\n        WHERE  1 = 1 "
                    + "\n               AND l.id = fsj.baulast_reference "
                    + "\n               AND fsj.flurstueck = k.id ";
        if ((geometry != null) && !geometry.isEmpty()) {
            queryMid += ""
                        + "\n               AND k.fs_referenz = f.id "
                        + "\n               AND f.umschreibendes_rechteck = g.id ";
        }
        queryMid += ""
                    + "\n               "
                    + blattnummerquerypart; // --  AND l.blattnummer LIKE '^[0]*4711[[:alpha:]]?$'  "
        if ((geometry != null) && !geometry.isEmpty()) {
            queryMid += "\n               "
                        + geoquerypart;
        }
        queryMid += "\n               "
                    + artquerypart          // -- AND a.baulast_art = 'Wertsteigerungsverzicht' "
                    + "\n               "
                    + ungueltigquerypart
                    + "\n               "
                    + gueltigquerypart
                    + "\n               "
                    + fsquerypart;
        final String queryBlattPostfix = ""
                    + "\n) AS x "
                    + "\n                        ) "
                    + "\nGROUP  BY b.blattnummer, "
                    + "\n          class_id, "
                    + "\n          object_id "
                    + "\nORDER  BY b.blattnummer ";
        final String queryBaulastPostfix = ""
                    + "\n group by blattnummer, laufende_nummer, class_id, object_id"
                    + "\n order by blattnummer, laufende_nummer";

        if (result == Result.BAULASTBLATT) {
            query = queryBlattPrefix
                        + queryMid
                        + queryBlattPostfix;
        } else {
            query = queryMid
                        + queryBaulastPostfix;
        }
        return query;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String getSecondaryQuery() {
        String query = "";
        final String queryBlattPrefix = ""
                    + "SELECT " + baulastblattClassID + "        AS class_id, "
                    + "\n       b.id       AS object_id, "
                    + "\n       b.blattnummer "
                    + "\nFROM   alb_baulastblatt b "
                    + "\nWHERE  b.blattnummer IN (SELECT blattnummer "
                    + "\n                         FROM (";
        final String queryMid = ""
                    + "\n       SELECT " + baulastClassID + "  AS class_id, "
                    + "\n               l.id AS object_id, "
                    + "\n               l.blattnummer|| '/' || case when l.laufende_nummer is not null then l.laufende_nummer else 'keine laufende Nummer' end, "
                    + "\n               l.blattnummer, "
                    + "\n               l.laufende_nummer "
                    + "\n        FROM   alb_baulast l "
                    + "\n               left outer join alb_baulast_baulastarten la on (l.id = la.baulast_reference) "
                    + "\n               left outer join alb_baulast_art a on (la.baulast_art = a.id),"
                    + "\n               alb_flurstueck_kicker k, "
                    + "\n               flurstueck f, "
                    + "\n" + getBelastetBeguenstigtSubselect()
                    + "\n               , "
                    + "\n              (SELECT f.gemarkungs_nr gemarkung, "
                    + "\n                      f.flur          flur, "
                    + "\n                      f.fstnr_z       zaehler, "
                    + "\n                      f.fstnr_n       nenner "
                    + "\n               FROM   alb_flurstueck_kicker k, "
                    + "\n                      flurstueck f, "
                    + "\n                      geom g, "
                    + "\n                      (SELECT f.id fid, "
                    + "\n                              k.id kid, "
                    + "\n                              geo_field "
                    + "\n                       FROM   alb_flurstueck_kicker k "
                    + "\n                              LEFT OUTER JOIN (SELECT "
                    + "\n                              flurstueck "
                    + "\n                                               FROM "
                    + "\n                              alb_baulast_flurstuecke_beguenstigt "
                    + "\n                                               UNION "
                    + "\n                                               SELECT flurstueck "
                    + "\n                                               FROM "
                    + "\n                              alb_baulast_flurstuecke_belastet "
                    + "\n                                              ) AS x "
                    + "\n                                ON ( x.flurstueck = k.id ), "
                    + "\n                              flurstueck f, "
                    + "\n                              geom g "
                    + "\n                       WHERE  x.flurstueck IS NULL "
                    + "\n                              AND k.fs_referenz = f.id "
                    + "\n                              AND f.umschreibendes_rechteck = g.id "
                    + "\n                              " + fsquerypart
                    + "\n                                                     ) AS y "
                    + "\n               WHERE  k.fs_referenz = f.id "
                    + "\n                      AND f.umschreibendes_rechteck = g.id "
                    + "\n                      AND CASE WHEN NOT st_isEmpty(y.geo_field) AND NOT st_isEmpty(g.geo_field) THEN y.geo_field && g.geo_field ELSE FALSE END "
                    + "\n                      AND CASE WHEN NOT st_isEmpty(y.geo_field) AND NOT st_isEmpty(g.geo_field) AND NOT st_isEmpty(St_buffer(y.geo_field,"
                    + INTERSECTS_BUFFER + ")) AND NOT st_isEmpty(St_buffer(g.geo_field, " + INTERSECTS_BUFFER
                    + ")) THEN st_Intersects(St_buffer(y.geo_field, " + INTERSECTS_BUFFER + "), St_buffer(g.geo_field, "
                    + INTERSECTS_BUFFER + ")) ELSE FALSE END "
                    + "\n                      AND NOT y.fid = f.id "
                    + "\n                      AND CASE WHEN NOT st_isEmpty(y.geo_field) AND NOT st_isEmpty(g.geo_field) AND NOT st_isEmpty(st_Buffer(y.geo_field, -0.005)) AND NOT st_isEmpty(St_buffer(g.geo_field, "
                    + INTERSECTS_BUFFER
                    + ")) THEN st_Intersects(st_Buffer(y.geo_field, -0.005), St_buffer(g.geo_field, "
                    + INTERSECTS_BUFFER + ")) ELSE FALSE END) AS indirekt "
                    + "\n                                 WHERE  1 = 1 "
                    + "\n                                        AND l.id = fsj.baulast_reference "
                    + "\n                                        AND fsj.flurstueck = k.id "
                    + "\n                                        AND k.fs_referenz = f.id "
                    + "\n                                        AND f.gemarkungs_nr = indirekt.gemarkung "
                    + "\n                                        AND f.flur = indirekt.flur "
                    + "\n                                        AND f.fstnr_z = indirekt.zaehler "
                    + "\n                                        AND f.fstnr_n = indirekt.nenner "
                    + "\n                                      "
                    + blattnummerquerypart // --  AND l.blattnummer LIKE '^[0]*4711[[:alpha:]]?$'  "
                    + "\n                                      "
                    + artquerypart         // -- AND a.baulast_art = 'Wertsteigerungsverzicht' "
                    + "\n                                      " + ungueltigquerypart
                    + "\n                                      " + gueltigquerypart;
        final String queryBlattPostfix = ""
                    + "\n)AS x) "
                    + "\nGROUP  BY b.blattnummer, "
                    + "\n          class_id, "
                    + "\n          object_id "
                    + "\nORDER  BY b.blattnummer";
        final String queryBaulastPostfix = ""
                    + "\n group by blattnummer, laufende_nummer, class_id, object_id"
                    + "\n order by blattnummer, laufende_nummer";

        if (result == Result.BAULASTBLATT) {
            query = queryBlattPrefix + queryMid + queryBlattPostfix;
        } else {
            query = queryMid + queryBaulastPostfix;
        }
        return query;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  args  DOCUMENT ME!
     */
    public static void main(final String[] args) {
        final BaulastSearchInfo bsi = new BaulastSearchInfo();
        bsi.setResult(Result.BAULASTBLATT);
//        bsi.getFlurstuecke().add(new FlurstueckInfo(3135, "252", "576", "0"));
        // bsi.getFlurstuecke().add(new FlurstueckInfo(3279, "012", "1975", "402"));
        bsi.setBlattnummer("9724");

        final CidsBaulastSearchStatement css = new CidsBaulastSearchStatement(bsi, 177, 182);
        System.out.println(css.getPrimaryQuery());
    }

    @Override
    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }
}
