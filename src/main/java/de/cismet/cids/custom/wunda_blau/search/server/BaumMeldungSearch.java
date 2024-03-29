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
public class BaumMeldungSearch extends AbstractCidsServerSearch implements MetaObjectNodeServerSearch {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient Logger LOG = Logger.getLogger(BaumMeldungSearch.class);

    public static final String TABLE_NAME = "baum_gebiet";
    public static final String TABLE_NAME_MELDUNG = "baum_meldung";
    public static final String FIELD__GEBIET_NAME = "name";
    public static final String FIELD__MELDUNG_ID = "id";
    public static final String FIELD__GEBIET_AZ = "aktenzeichen";
    public static final String FIELD__MELDUNG_DATUM = "datum";
    public static final String FIELD__MELDUNG_FK = "fk_gebiet";

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String QUERY_TEMPLATE = "SELECT "
                + "  (SELECT c.id FROM cs_class c WHERE table_name ILIKE '" + TABLE_NAME_MELDUNG + "') AS class_id, "
                + TABLE_NAME_MELDUNG + "." + FIELD__MELDUNG_ID + ", "
                + TABLE_NAME + "." + FIELD__GEBIET_AZ // + ", "
                + " || '---' || to_char(" + TABLE_NAME_MELDUNG + "." + FIELD__MELDUNG_DATUM
                + ", 'DD.MM.YY') AS aktenzeichen , "
                + TABLE_NAME + "." + FIELD__GEBIET_NAME + ", "
                + "'[' || " + TABLE_NAME_MELDUNG + "." + FIELD__MELDUNG_DATUM + " || ']' AS datum"
                + " FROM " + TABLE_NAME_MELDUNG
                + " LEFT JOIN " + TABLE_NAME
                + " ON " + TABLE_NAME_MELDUNG + "." + FIELD__MELDUNG_FK + " = "
                + TABLE_NAME + "." + FIELD__MELDUNG_ID
                + " ORDER BY " + TABLE_NAME + "." + FIELD__GEBIET_AZ + ", " + TABLE_NAME_MELDUNG + "."
                + FIELD__MELDUNG_DATUM;

    //~ Instance fields --------------------------------------------------------

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    @Setter @Getter private Integer ortsterminId;
    @Setter @Getter private Integer ortsterminFKMeldung;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new BaumMeldungSearch object.
     */
    public BaumMeldungSearch() {
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
            if (getOrtsterminId() != null) {
                wheres.add(String.format("baum_meldung.id = %d", getOrtsterminFKMeldung()));
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
            LOG.error("error while searching for baum_meldung", ex);
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
