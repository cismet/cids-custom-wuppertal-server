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
public class BaumAnsprechpartnerLightweightSearch extends AbstractMonToLwmoSearch {

    //~ Instance fields --------------------------------------------------------

    @Getter @Setter private BaumAnsprechpartnerSearch monSearch;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new BaumAnsprechpartnerLightweightSearch object.
     */
    public BaumAnsprechpartnerLightweightSearch() {
        this(new BaumAnsprechpartnerSearch());
    }

    /**
     * Creates a new BaumAnsprechpartnerLightweightSearch object.
     *
     * @param  monSearch  DOCUMENT ME!
     */
    public BaumAnsprechpartnerLightweightSearch(final BaumAnsprechpartnerSearch monSearch) {
        super(createSearchInfo(), "baum_ansprechpartner", "WUNDA_BLAU");
        this.monSearch = monSearch;
    }

    //~ Methods ----------------------------------------------------------------
    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static SearchInfo createSearchInfo() {
        return new SearchInfo(BaumAnsprechpartnerLightweightSearch.class.getName(),
                BaumAnsprechpartnerLightweightSearch.class.getSimpleName(),
                "BaumAnsprechpartnerLS",
                Arrays.asList(new SearchParameterInfo[0]),
                new SearchParameterInfo());
    }
}
