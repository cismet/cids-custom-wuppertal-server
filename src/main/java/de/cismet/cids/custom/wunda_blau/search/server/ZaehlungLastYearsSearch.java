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

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;

import org.apache.log4j.Logger;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.cismet.cids.server.search.AbstractCidsServerSearch;
import de.cismet.cids.server.search.MetaObjectNodeServerSearch;

import de.cismet.connectioncontext.ConnectionContext;

/**
 * DOCUMENT ME!
 *
 * @author   Sandra
 * @version  $Revision$, $Date$
 */
public class ZaehlungLastYearsSearch extends AbstractCidsServerSearch implements MetaObjectNodeServerSearch {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient Logger LOG = Logger.getLogger(BaumSchadenSearch.class);

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    //~ Instance fields --------------------------------------------------------

    private final String QUERY_TEMPLATE = String.format("SELECT "
                    + "  (SELECT c.id FROM cs_class c WHERE table_name ILIKE 'zaehlung') AS class_id, "
                    + " z.id \n"
                    + "from zaehlung z\n"
                    + "left join zaehlung_ref r on z.id = r.zaehlung_ref\n");

    private ConnectionContext connectionContext = ConnectionContext.create(
            ConnectionContext.Category.STATIC,
            ZaehlungLastYearsSearch.class.getSimpleName());

    @Setter @Getter private Integer standortId;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new ZaehlungLastYearsSearch object.
     */
    public ZaehlungLastYearsSearch() {
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

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  RuntimeException  DOCUMENT ME!
     */
    @Override
    public Collection<MetaObjectNode> performServerSearch() {
        try {
            final List<String> leftJoins = new ArrayList<>();
            final List<String> wheres = new ArrayList<>();
            if (getStandortId() != null) {
                wheres.add(String.format("standpunkt_ref= %d", getStandortId()));
                wheres.add(String.format(
                        "date_part('year'::text, datum) in (\n"
                                + "select date_part('year'::text, datum)\n"
                                + "from zaehlung z\n"
                                + "left join zaehlung_ref r on z.id = r.zaehlung_ref\n"
                                + "where standpunkt_ref= %d \n"
                                + "group by date_part('year'::text, datum)\n"
                                + "order by date_part('year'::text, datum) desc\n"
                                + "fetch first 2 row only)",
                        getStandortId()));
            }

            final String leftJoin = (!leftJoins.isEmpty())
                ? String.format("LEFT JOIN %s", String.join(" LEFT JOIN ", leftJoins)) : "";
            final String where = (!wheres.isEmpty()) ? String.format("WHERE %s", String.join(" AND ", wheres)) : "";
            final String query = QUERY_TEMPLATE + where;
            LOG.info(query);
            final MetaService ms = (MetaService)getActiveLocalServers().get("WUNDA_BLAU");
            final List<MetaObjectNode> mons = new ArrayList<>();
            final List<ArrayList> resultList = ms.performCustomSearch(query, getConnectionContext());
            for (final ArrayList al : resultList) {
                final int cid = (Integer)al.get(0);
                final int oid = (Integer)al.get(1);
                final String name = String.valueOf(al.get(1));
                final MetaObjectNode mon = new MetaObjectNode("WUNDA_BLAU", oid, cid, name, null, null);

                mons.add(mon);
            }
            return mons;
        } catch (final RemoteException ex) {
            LOG.error("error while searching for zaehlung", ex);
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
