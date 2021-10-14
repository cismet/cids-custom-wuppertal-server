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

import com.vividsolutions.jts.io.ParseException;

import lombok.Getter;
import lombok.Setter;

import org.openide.util.lookup.ServiceProvider;

import java.util.Arrays;

import de.cismet.cidsx.server.api.types.SearchInfo;
import de.cismet.cidsx.server.api.types.SearchParameterInfo;
import de.cismet.cidsx.server.search.RestApiCidsServerSearch;

/**
 * DOCUMENT ME!
 *
 * @author   sandra
 * @version  $Revision$, $Date$
 */
@ServiceProvider(service = RestApiCidsServerSearch.class)
public class BaumSchadenLightweightSearch extends AbstractMonToLwmoSearch {

    //~ Instance fields --------------------------------------------------------

    @Getter @Setter private BaumSchadenSearch monSearch;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new BaumSchadenLightweightSearch object.
     */
    public BaumSchadenLightweightSearch() {
        this(new BaumSchadenSearch());
    }

    /**
     * Creates a new BaumMSchadenLightweightSearch object.
     *
     * @param  monSearch  DOCUMENT ME!
     */
    public BaumSchadenLightweightSearch(final BaumSchadenSearch monSearch) {
        super(createSearchInfo(), "baum_schaden", "WUNDA_BLAU");
        this.monSearch = monSearch;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param   args  DOCUMENT ME!
     *
     * @throws  ParseException  DOCUMENT ME!
     */
    public static void main(final String[] args) throws ParseException {
        final BaumSchadenLightweightSearch bmlw = new BaumSchadenLightweightSearch();
    }
    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static SearchInfo createSearchInfo() {
        return new SearchInfo(BaumSchadenLightweightSearch.class.getName(),
                BaumSchadenLightweightSearch.class.getSimpleName(),
                "BaumSchadenLS",
                Arrays.asList(new SearchParameterInfo[0]),
                new SearchParameterInfo());
    }
}
