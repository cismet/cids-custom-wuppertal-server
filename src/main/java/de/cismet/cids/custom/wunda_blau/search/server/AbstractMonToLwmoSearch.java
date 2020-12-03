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

import Sirius.server.middleware.types.LightweightMetaObject;
import Sirius.server.middleware.types.MetaObjectNode;
import Sirius.server.middleware.types.StringPatternFormater;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.cismet.cids.server.search.AbstractCidsServerSearch;
import de.cismet.cids.server.search.MetaObjectNodeServerSearch;
import de.cismet.cids.server.search.SearchException;

import de.cismet.cidsx.server.api.types.SearchInfo;
import de.cismet.cidsx.server.search.RestApiCidsServerSearch;
import de.cismet.cidsx.server.search.builtin.legacy.LightweightMetaObjectsSearch;

import de.cismet.connectioncontext.ConnectionContext;
import de.cismet.connectioncontext.ConnectionContextStore;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public abstract class AbstractMonToLwmoSearch extends AbstractCidsServerSearch implements RestApiCidsServerSearch,
    LightweightMetaObjectsSearch,
    ConnectionContextStore {

    //~ Instance fields --------------------------------------------------------

    @Getter private final SearchInfo searchInfo;
    @Getter private final String tableName;
    @Getter private final String domain;

    @Getter @Setter private String representationPattern = "%s";
    @Getter @Setter private String[] representationFields = new String[] { "name" };
    @Getter private ConnectionContext connectionContext = ConnectionContext.createDummy();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new AbstractMonToLwmoSearch object.
     *
     * @param  searchInfo  DOCUMENT ME!
     * @param  tableName   DOCUMENT ME!
     * @param  domain      DOCUMENT ME!
     */
    protected AbstractMonToLwmoSearch(final SearchInfo searchInfo, final String tableName, final String domain) {
        this.searchInfo = searchInfo;
        this.tableName = tableName;
        this.domain = domain;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public abstract MetaObjectNodeServerSearch getMonSearch();

    @Override
    public Collection performServerSearch() throws SearchException {
        final MetaObjectNodeServerSearch monSearch = getMonSearch();
        if (monSearch == null) {
            throw new SearchException("monSearch has to be set");
        }
        monSearch.setUser(getUser());
        monSearch.setActiveLocalServers(getActiveLocalServers());
        if (monSearch instanceof ConnectionContextStore) {
            ((ConnectionContextStore)monSearch).initWithConnectionContext(getConnectionContext());
        }
        final List<LightweightMetaObject> lwmos = new ArrayList<>();
        for (final MetaObjectNode mon : monSearch.performServerSearch()) {
            final Map<String, java.lang.Object> attributeMap = new HashMap<>();
            attributeMap.put("name", mon.getName());
            final LightweightMetaObject lwmo = new LightweightMetaObject(mon.getClassId(),
                    mon.getObjectId(),
                    mon.getDomain(),
                    getUser(),
                    attributeMap,
                    new StringPatternFormater(getRepresentationPattern(), getRepresentationFields()));
            lwmos.add(lwmo);
        }
        return lwmos;
    }

    @Override
    public void initWithConnectionContext(final ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }
}
