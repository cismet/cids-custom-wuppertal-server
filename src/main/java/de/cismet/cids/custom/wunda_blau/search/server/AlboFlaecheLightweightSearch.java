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
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
@ServiceProvider(service = RestApiCidsServerSearch.class)
public class AlboFlaecheLightweightSearch extends AbstractMonToLwmoSearch {

    //~ Instance fields --------------------------------------------------------

    @Getter @Setter private AlboFlaecheSearch monSearch;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new AlboFlaecheLightweightSearch object.
     */
    public AlboFlaecheLightweightSearch() {
        this(new AlboFlaecheSearch(false));
    }

    /**
     * Creates a new AlboFlaecheLightweightSearch object.
     *
     * @param  monSearch  DOCUMENT ME!
     */
    public AlboFlaecheLightweightSearch(final AlboFlaecheSearch monSearch) {
        super(createSearchInfo(), "albo_vorgang", "WUNDA_BLAU");
        this.monSearch = monSearch;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static SearchInfo createSearchInfo() {
        final SearchInfo searchInfo = new SearchInfo(AlboVorgangLightweightSearch.class.getName(),
                AlboVorgangLightweightSearch.class.getSimpleName(),
                "TODO",
                Arrays.asList(new SearchParameterInfo[0]),
                new SearchParameterInfo());
        return searchInfo;
    }
}
