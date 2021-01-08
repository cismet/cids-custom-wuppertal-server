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
public class BaumMeldungLightweightSearch extends AbstractMonToLwmoSearch {

    //~ Instance fields --------------------------------------------------------

    @Getter @Setter private BaumMeldungSearch monSearch;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new BaumMeldungLightweightSearch object.
     */
    public BaumMeldungLightweightSearch() {
        this(new BaumMeldungSearch());
    }

    /**
     * Creates a new BaumMeldungLightweightSearch object.
     *
     * @param  monSearch  DOCUMENT ME!
     */
    public BaumMeldungLightweightSearch(final BaumMeldungSearch monSearch) {
        super(createSearchInfo(), "baum_meldung", "WUNDA_BLAU");
        this.monSearch = monSearch;
    }

    //~ Methods ----------------------------------------------------------------
    public static void main(final String[] args)throws ParseException{
        BaumMeldungLightweightSearch bmlw = new BaumMeldungLightweightSearch();
        System.out.println(bmlw.getMonSearch());
    }
    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static SearchInfo createSearchInfo() {
        return new SearchInfo(BaumMeldungLightweightSearch.class.getName(),
                BaumMeldungLightweightSearch.class.getSimpleName(),
                "TODO",
                Arrays.asList(new SearchParameterInfo[0]),
                new SearchParameterInfo());
    }
}
