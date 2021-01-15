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
import java.util.List;

import de.cismet.cids.server.search.AbstractCidsServerSearch;
import de.cismet.cids.server.search.MetaObjectNodeServerSearch;

import de.cismet.connectioncontext.ConnectionContext;

/**
 * DOCUMENT ME!
 *
 * @version  $Revision$, $Date$
 */
public class AlboVorgangSearch extends AbstractCidsServerSearch implements MetaObjectNodeServerSearch {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient Logger LOG = Logger.getLogger(AlboVorgangSearch.class);

    private static final String QUERY_TEMPLATE = "SELECT "
                + "(SELECT c.id FROM cs_class c WHERE table_name ILIKE 'albo_vorgang') AS class_id, albo_vorgang.id, albo_vorgang.schluessel AS name "
                + "FROM albo_vorgang "
                + "%s "
                + "%s";

    //~ Instance fields --------------------------------------------------------

    @Setter @Getter private Integer flaecheId;
    @Setter @Getter private Boolean loeschen = Boolean.FALSE;

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new AlboVorgangSearch object.
     */
    public AlboVorgangSearch() {
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
            final List<String> leftJoins = new ArrayList<>();
            final List<String> wheres = new ArrayList<>();
            if (getFlaecheId() != null) {
                leftJoins.add(
                    "albo_vorgang_flaeche ON albo_vorgang.arr_flaechen = albo_vorgang_flaeche.vorgang_reference");
                wheres.add(String.format("albo_vorgang_flaeche.fk_flaeche = %d", getFlaecheId()));
            }
            if (getLoeschen() != null) {
                wheres.add(String.format("albo_vorgang.loeschen = %s", getLoeschen() ? "TRUE" : "FALSE"));
            }

            final String leftJoin = (!leftJoins.isEmpty())
                ? String.format("LEFT JOIN %s", String.join(" LEFT JOIN ", leftJoins)) : "";
            final String where = (!wheres.isEmpty()) ? String.format("WHERE %s", String.join(" AND ", wheres)) : "";
            final String query = String.format(QUERY_TEMPLATE, leftJoin, where);

            final MetaService ms = (MetaService)getActiveLocalServers().get("WUNDA_BLAU");
            final List<MetaObjectNode> mons = new ArrayList<>();
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
            LOG.error("error while searching for albo_vorgang", ex);
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
