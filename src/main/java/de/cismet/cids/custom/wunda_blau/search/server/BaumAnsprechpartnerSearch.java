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
 * @version  $Revision$, $Date$
 */
public class BaumAnsprechpartnerSearch extends AbstractCidsServerSearch implements MetaObjectNodeServerSearch {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient Logger LOG = Logger.getLogger(BaumAnsprechpartnerSearch.class);

    public static final String TABLE_NAME = "baum_ansprechpartner";
    public static final String FIELD__ID = "id";
    public static final String FIELD__NAME = "name";
    public static final String FIELD__MAIL = "mail";
    public static final String FIELD__BEMERKUNG = "bemerkung";

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String QUERY_TEMPLATE = "SELECT "
                + "  (SELECT c.id FROM cs_class c WHERE table_name ILIKE '" + TABLE_NAME + "') AS class_id, "
                + TABLE_NAME + "." + FIELD__ID + ", "
                + TABLE_NAME + "." + FIELD__NAME + " || ' -- ' || "
                + "COALESCE(" + TABLE_NAME + "." + FIELD__MAIL + ", 'k.A.')" + " || ' -- ' || "
                + "COALESCE(" + TABLE_NAME + "." + FIELD__BEMERKUNG + ", 'k.A.')" + " as name"
                + " FROM " + TABLE_NAME
                + " ORDER BY " + TABLE_NAME + "." + FIELD__NAME;

    //~ Instance fields --------------------------------------------------------

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    @Setter @Getter private Integer ansprechpartnerId;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new BaumAnsprechpartnerSearch object.
     */
    public BaumAnsprechpartnerSearch() {
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
            if (getAnsprechpartnerId() != null) {
                wheres.add(String.format("baum_ansprechpartner.id = %d", getAnsprechpartnerId()));
            }

            final String leftJoin = (!leftJoins.isEmpty())
                ? String.format("LEFT JOIN %s", String.join(" LEFT JOIN ", leftJoins)) : "";
            final String where = (!wheres.isEmpty()) ? String.format("WHERE %s", String.join(" AND ", wheres)) : "";
            final String query = String.format(QUERY_TEMPLATE, leftJoin, where);
            LOG.info(query);
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
        } catch (final RemoteException ex) {
            LOG.error("error while searching for baum_ansprechpartner", ex);
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
