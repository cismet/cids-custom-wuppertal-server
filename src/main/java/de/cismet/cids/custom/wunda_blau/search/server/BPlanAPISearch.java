/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.wunda_blau.search.server;

import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.sql.PreparableStatement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import lombok.Getter;
import lombok.Setter;

import org.apache.log4j.Logger;

import org.openide.util.lookup.ServiceProvider;

import java.io.Serializable;

import java.sql.Types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import de.cismet.cids.server.search.AbstractCidsServerSearch;
import de.cismet.cids.server.search.SearchException;

import de.cismet.cidsx.base.types.Type;

import de.cismet.cidsx.server.api.types.SearchInfo;
import de.cismet.cidsx.server.api.types.SearchParameterInfo;
import de.cismet.cidsx.server.search.RestApiCidsServerSearch;

/**
 * Search to Retrieve the BPlan-Objects to the cids Pure REST Search API.
 *
 * @author   Thorsten Hell
 * @version  $Revision$, $Date$
 */
@ServiceProvider(service = RestApiCidsServerSearch.class)
public class BPlanAPISearch extends AbstractCidsServerSearch implements RestApiCidsServerSearch {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(BPlanAPISearch.class);

    private static final PreparableStatement QUERY = new PreparableStatement(
            "select * from bplanapisearch(?,?)",
            Types.VARCHAR,
            Types.VARCHAR);
    private static final String DOMAIN = "WUNDA_BLAU";

    //~ Instance fields --------------------------------------------------------

    @Getter private final SearchInfo searchInfo;
    @Getter @Setter private String wktString;
    @Getter @Setter private String status;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new LightweightMetaObjectsByQuerySearch object.
     */
    public BPlanAPISearch() {
        searchInfo = new SearchInfo();
        searchInfo.setKey(this.getClass().getSimpleName());
        searchInfo.setName(this.getClass().getSimpleName());
        searchInfo.setDescription("BPlan Search to use in Geoportal 3");

        final List<SearchParameterInfo> parameterDescription = new LinkedList<SearchParameterInfo>();
        searchInfo.setParameterDescription(parameterDescription);

        final SearchParameterInfo wktStringParameterInfo;
        wktStringParameterInfo = new SearchParameterInfo();
        wktStringParameterInfo.setKey("wktString");
        wktStringParameterInfo.setType(Type.STRING);
        parameterDescription.add(wktStringParameterInfo);

        final SearchParameterInfo statusParameterInfo;
        statusParameterInfo = new SearchParameterInfo();
        statusParameterInfo.setKey("status");
        statusParameterInfo.setType(Type.STRING);
        parameterDescription.add(statusParameterInfo);

        searchInfo.setParameterDescription(parameterDescription);

        final SearchParameterInfo resultParameterInfo = new SearchParameterInfo();
        resultParameterInfo.setKey("return");
        resultParameterInfo.setArray(true);
        resultParameterInfo.setType(Type.JAVA_CLASS);
        resultParameterInfo.setAdditionalTypeInfo(" java.util.ArrayList<" + Chaos.class.getCanonicalName() + ">");
        searchInfo.setResultDescription(resultParameterInfo);
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public Collection performServerSearch() throws SearchException {
        try {
            final MetaService metaService = (MetaService)this.getActiveLocalServers().get(DOMAIN);
            if (status == null) {
                status = "";
            }
            if (metaService != null) {
                // "select * from bplanapisearch('" + wktString + "','" + status + "')";
                QUERY.setObjects(wktString, status);
                final ArrayList<ArrayList> results = metaService.performCustomSearch(QUERY);
                return results;
            } else {
                LOG.error("active local server not found"); // NOI18N
            }

            return null;
        } catch (final Exception ex) {
            throw new SearchException("error while loading verfahren objects", ex);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   args  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    public static void main(final String[] args) throws Exception {
        System.out.println(BPlanAPISearch.class.getSimpleName());
        final ObjectMapper objectMapper = new ObjectMapper();
        final Chaos c = new Chaos(4711, "EIJO");
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        System.out.println(objectMapper.writeValueAsString(c));
    }
}

/**
 * DOCUMENT ME!
 *
 * @version  $Revision$, $Date$
 */
@XmlRootElement
class Chaos implements Serializable {

    //~ Instance fields --------------------------------------------------------

    @Getter @Setter private int id;
    @Getter @Setter private String result;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new Chaos object.
     *
     * @param  id      DOCUMENT ME!
     * @param  result  DOCUMENT ME!
     */
    public Chaos(final int id, final String result) {
        this.id = id;
        this.result = result;
    }
}

//DROP TYPE bplanAPISearchResult CASCADE;
//
//CREATE TYPE bplanAPISearchResult AS (
//    nummer text,
//    name text,
//    status text,
//    plaene_rk text,
//    plaene_nrk text,
//    docs text,
//    geojson text);
//
//
//CREATE or REPLACE FUNCTION bplanAPISearch(in wkt_string text, in status text DEFAULT '',in srid int DEFAULT 25832) RETURNS SETOF bplanAPISearchResult
//    AS $$
//        select
//            outerBPP.verfahren as nummer,
//            (select name from bplan_verfahren where nummer=verfahren) as name,
//            (select array_to_string(array_agg(distinct innerBPP.status),',') from bplan_plan innerBPP, geom where geom.id=innerBPP.geometrie and innerBPP.verfahren=outerBPP.verfahren and geom.geo_field=outerGeom.geo_field) as status,
//            (select '['||array_to_string(array_agg('{ "file": "B'||nummer||'.pdf", "url":"http://www.wuppertal.de/geoportal/bplaene/rechtswirksam/B'||nummer||'.pdf"}'), ',')||']' from bplan_plan, geom where geom.id=bplan_plan.geometrie and bplan_plan.verfahren=outerBPP.verfahren and geom.geo_field=outerGeom.geo_field and bplan_plan.status like 'r%') as plaene_rk,
//            (select '['||array_to_string(array_agg('{ "file": "B'||regexp_replace(nummer, E'\\*$', '')||'.pdf", "url":"http://www.wuppertal.de/geoportal/bplaene/verfahren/B'||regexp_replace(nummer, E'\\*$', '')||'.pdf"}'), ',')||']' from bplan_plan, geom where geom.id=bplan_plan.geometrie and bplan_plan.verfahren=outerBPP.verfahren and geom.geo_field=outerGeom.geo_field and bplan_plan.status like 'n%') as plaene_nrk,
//            (select '['||array_to_string(array_agg('{ "file": "'||dateiname||'.pdf", "url":"'||pfad||'"}'), ',')||']' from bpl_dokumente where bpl_dokumente.plannummer=outerBPP.verfahren) as docs,
//            st_asgeojson(geo_field)
//        from bplan_plan outerBPP, geom outerGeom where outerGeom.id=outerBPP.geometrie
//
//        and status ilike $2||'%'
//        and  st_intersects(geo_field,st_GeomFromEWKT('SRID='||$3||';'||$1))
//        group by verfahren, outerGeom.geo_field
//    order by  st_distance(st_centroid(st_GeomFromEWKT('SRID='||$3||';'||$1)) , st_centroid(geo_field)) ASC
//    $$
//LANGUAGE SQL STABLE;
//
//
// -- Example
//select * from bplanapisearch('POLYGON((373868.48515255994 5681973.08598268,374972.0447490652 5681973.08598268,374972.0447490652 5681363.978413516,373868.48515255994 5681363.978413516,373868.48515255994 5681973.08598268))')
