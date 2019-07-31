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

import lombok.Getter;
import lombok.Setter;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.cismet.cids.server.search.AbstractCidsServerSearch;
import de.cismet.cids.server.search.MetaObjectNodeServerSearch;

import de.cismet.connectioncontext.ConnectionContext;

/**
 * DOCUMENT ME!
 *
 * @version  $Revision$, $Date$
 */
public class FormSolutionsBestellungSearch extends AbstractCidsServerSearch implements MetaObjectNodeServerSearch {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient Logger LOG = Logger.getLogger(FormSolutionsBestellungSearch.class);

    //~ Instance fields --------------------------------------------------------

    @Setter @Getter private String berechtigungspruefungSchluessel;

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new FormSolutionsBestellungSearch object.
     */
    public FormSolutionsBestellungSearch() {
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

    @Override
    public Collection<MetaObjectNode> performServerSearch() {
        try {
            final List<MetaObjectNode> result = new ArrayList<>();

            final Map<String, Object> filter = new HashMap<>();
            filter.put("berechtigungspruefung.schluessel", berechtigungspruefungSchluessel);
            final Collection<String> filterStrings = new ArrayList<>();
            for (final Map.Entry<String, Object> entry : filter.entrySet()) {
                if (entry.getValue() instanceof String) {
                    filterStrings.add(entry.getKey() + " ILIKE '" + entry.getValue() + "'");
                }
            }
            final String query =
                "SELECT (SELECT c.id FROM cs_class c WHERE table_name ilike 'fs_bestellung') AS class_id, fs_bestellung.id, max(fs_bestellung.transid) AS name "
                        + "FROM fs_bestellung LEFT JOIN berechtigungspruefung ON fs_bestellung.berechtigungspruefung = berechtigungspruefung.id "
                        + "WHERE "
                        + String.join(" AND ", filterStrings)
                        + " "
                        + "GROUP BY (fs_bestellung.id);";

            final MetaService ms = (MetaService)getActiveLocalServers().get("WUNDA_BLAU");

            final List<ArrayList> resultList = ms.performCustomSearch(query, getConnectionContext());
            for (final ArrayList al : resultList) {
                final int cid = (Integer)al.get(0);
                final int oid = (Integer)al.get(1);
                final String name = (String)al.get(2);
                final MetaObjectNode mon = new MetaObjectNode("WUNDA_BLAU", oid, cid, name, null, null);

                result.add(mon);
            }
            return result;
        } catch (final Exception ex) {
            LOG.error("error while searching for messungen", ex);
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
}
