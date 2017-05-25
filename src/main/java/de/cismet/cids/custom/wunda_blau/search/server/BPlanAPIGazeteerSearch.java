/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cismet.cids.custom.wunda_blau.search.server;

import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.sql.PreparableStatement;

import lombok.Getter;
import lombok.Setter;

import org.apache.log4j.Logger;

import org.openide.util.lookup.ServiceProvider;

import java.io.Serializable;

import java.math.BigDecimal;

import java.sql.Types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import de.cismet.cids.server.search.AbstractCidsServerSearch;
import de.cismet.cids.server.search.SearchException;

import de.cismet.cidsx.base.types.Type;

import de.cismet.cidsx.server.api.types.SearchInfo;
import de.cismet.cidsx.server.api.types.SearchParameterInfo;
import de.cismet.cidsx.server.search.RestApiCidsServerSearch;

/**
 * DOCUMENT ME!
 *
 * @author   thorsten
 * @version  $Revision$, $Date$
 */
@ServiceProvider(service = RestApiCidsServerSearch.class)
public class BPlanAPIGazeteerSearch extends AbstractCidsServerSearch implements RestApiCidsServerSearch {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(BPlanAPIGazeteerSearch.class);

    private static final PreparableStatement QUERY = new PreparableStatement(
            "select * from bplanGazeteerAPISearch(?)",
            Types.VARCHAR);
    private static final String DOMAIN = "WUNDA_BLAU";

    //~ Instance fields --------------------------------------------------------

    @Getter private final SearchInfo searchInfo;
    @Getter @Setter private String input;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new BPlanAPIGazeteerSearch object.
     */
    public BPlanAPIGazeteerSearch() {
        searchInfo = new SearchInfo();
        searchInfo.setKey(this.getClass().getSimpleName());
        searchInfo.setName(this.getClass().getSimpleName());
        searchInfo.setDescription("BPlan Gazeteer Search to use in Geoportal 3");

        final List<SearchParameterInfo> parameterDescription = new LinkedList<SearchParameterInfo>();
        searchInfo.setParameterDescription(parameterDescription);

        final SearchParameterInfo wktStringParameterInfo;
        wktStringParameterInfo = new SearchParameterInfo();
        wktStringParameterInfo.setKey("input");
        wktStringParameterInfo.setType(Type.STRING);
        parameterDescription.add(wktStringParameterInfo);

        searchInfo.setParameterDescription(parameterDescription);

        final SearchParameterInfo resultParameterInfo = new SearchParameterInfo();
        resultParameterInfo.setKey("return");
        resultParameterInfo.setArray(true);
        resultParameterInfo.setType(Type.JAVA_CLASS);
        searchInfo.setResultDescription(resultParameterInfo);
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public Collection performServerSearch() throws SearchException {
        try {
            final MetaService metaService = (MetaService)this.getActiveLocalServers().get(DOMAIN);

            if (metaService != null) {
                // "select * from bplanapisearch('" + wktString + "','" + status + "')";
                QUERY.setObjects(input);
                final ArrayList<ArrayList> results = metaService.performCustomSearch(QUERY);
                final ArrayList<GazzResult> ret = new ArrayList<GazzResult>(results.size());
                LOG.fatal(results);
                for (final ArrayList row : results) {
                    final String s = (String)row.get(0);
                    final String glyph = (String)row.get(1);
                    final double x = ((BigDecimal)row.get(2)).doubleValue();
                    final double y = ((BigDecimal)row.get(3)).doubleValue();
                    ret.add(new GazzResult(s, glyph, x, y));
                }
                return ret;
            } else {
                LOG.error("active local server not found"); // NOI18N
            }

            return null;
        } catch (final Exception ex) {
            throw new SearchException("error while loading gazetteer result objects", ex);
        }
    }
}

/**
 * DOCUMENT ME!
 *
 * @version  $Revision$, $Date$
 */
class GazzResult implements Serializable {

    //~ Instance fields --------------------------------------------------------

    @Getter @Setter private String string;
    @Getter @Setter private String glyphkey;
    @Getter @Setter private double x;
    @Getter @Setter private double y;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new GazzResult object.
     *
     * @param  string    DOCUMENT ME!
     * @param  glyphkey  DOCUMENT ME!
     * @param  x         DOCUMENT ME!
     * @param  y         DOCUMENT ME!
     */
    public GazzResult(final String string, final String glyphkey, final double x, final double y) {
        this.string = string;
        this.glyphkey = glyphkey;
        this.x = x;
        this.y = y;
    }
}

//
//DROP TYPE bplanGazeteerAPISearchResult CASCADE;
//
//CREATE TYPE bplanGazeteerAPISearchResult AS (
//    string text,
//    glyph text,
//    x numeric,
//    y numeric);
//
//CREATE or REPLACE FUNCTION bplanGazeteerAPISearch(in query text) RETURNS SETOF bplanGazeteerAPISearchResult
//    AS $$
//       --select '{"string":"'||string||'","x":'||x||',"y":'||y||'}'
//        select string,glyph,x,y
//            from (
//                select
//                    strasse.name || ' ' || adresse.hausnummer as string,
//                    'home' as glyph,
//                    round(st_x(geo_field)::numeric,
//                    2) as x,
//                    round(st_y(geo_field)::numeric,
//                    2) as y
//                from
//                    adresse,
//                    strasse,
//                    geom
//                where
//                    adresse.strasse=strasse.strassenschluessel
//                    and geom.id=adresse.umschreibendes_rechteck
//                union
//                select
//                    distinct '#'||bplan_plan.verfahren,
//                    'file' as glyph,
//                    round(st_x(st_centroid(geo_field))::numeric, 2) as x,
//                    round(st_y(st_centroid(geo_field))::numeric, 2) as x
//                from
//                    bplan_plan,
//                    geom
//                where
//                    geom.id=bplan_plan.geometrie
//            ) as gaz
//            where string ilike $1||'%'
//            order by string
//    $$
//LANGUAGE SQL STABLE;
//
//-- Example
//select * from bplanGazeteerAPISearch('#1000')
