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
public class BaumSchadenSearch extends AbstractCidsServerSearch implements MetaObjectNodeServerSearch {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient Logger LOG = Logger.getLogger(BaumSchadenSearch.class);

    public static final String TABLE_NAME_MELDUNG = "baum_meldung";
    public static final String TABLE_NAME_SCHADEN = "baum_schaden";
    public static final String TABLE_NAME_GEBIET = "baum_gebiet";
    public static final String TABLE_NAME_ART = "baum_art";
    public static final String TABLE_NAME_HAUPTART = "baum_hauptart";
    public static final String FIELD__SCHADEN_ID = "id";          // baum_schaden
    public static final String FIELD__SCHADEN_FK = "fk_meldung";  // baum_schaden
    public static final String FIELD__GEBIET_AZ = "aktenzeichen"; // baum_gebiet
    public static final String FIELD__GEBIET_ID = "id";           // baum_gebiet
    public static final String FIELD__MELDUNG_DATUM = "datum";    // baum_meldung
    public static final String FIELD__SCHADEN_ART = "fk_art";     // baum_schaden
    public static final String FIELD__MELDUNG_FK = "fk_gebiet";   // baum_ersatz
    public static final String FIELD__MELDUNG_ID = "id";          // baum_meldung
    public static final String FIELD__ART_NAME = "name";          // baum_art
    public static final String FIELD__ART_ID = "id";              // baum_art
    public static final String FIELD__ART_FK = "fk_hauptart";     // baum_art
    public static final String FIELD__HAUPTART_NAME = "name";     // baum_hauptart
    public static final String FIELD__HAUPTART_ID = "id";         // baum_hauptart

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String QUERY_TEMPLATE = "SELECT "
                + "  (SELECT c.id FROM cs_class c WHERE table_name ILIKE '" + TABLE_NAME_SCHADEN + "') AS class_id, "
                + TABLE_NAME_SCHADEN + "." + FIELD__SCHADEN_ID + ", "
                + TABLE_NAME_GEBIET + "." + FIELD__GEBIET_AZ // + ", "
                + " || '---' || to_char(" + TABLE_NAME_MELDUNG + "." + FIELD__MELDUNG_DATUM + ", 'DD.MM.YY')"
                + " || '---' || " + TABLE_NAME_SCHADEN + "." + FIELD__SCHADEN_ID
                + " || '(' || " + "COALESCE(" + TABLE_NAME_HAUPTART + "." + FIELD__HAUPTART_NAME
                + ", ' '::character varying)"
                + " || '-' || " + "COALESCE(" + TABLE_NAME_ART + "." + FIELD__ART_NAME + ", ' '::character varying)"
                + " || ')'" + " AS zuord_name"
                // + "'[' || " + TABLE_NAME_MELDUNG + "."  + FIELD__MELDUNG_DATUM + " || ']' AS datum"
                + " FROM " + TABLE_NAME_SCHADEN
                + " LEFT JOIN " + TABLE_NAME_MELDUNG
                + " ON " + TABLE_NAME_MELDUNG + "." + FIELD__MELDUNG_ID + " = "
                + TABLE_NAME_SCHADEN + "." + FIELD__SCHADEN_FK
                + " LEFT JOIN " + TABLE_NAME_GEBIET
                + " ON " + TABLE_NAME_MELDUNG + "." + FIELD__MELDUNG_FK + " = "
                + TABLE_NAME_GEBIET + "." + FIELD__GEBIET_ID
                + " LEFT JOIN " + TABLE_NAME_ART
                + " ON " + TABLE_NAME_SCHADEN + "." + FIELD__SCHADEN_ART + " = "
                + TABLE_NAME_ART + "." + FIELD__ART_ID
                + " LEFT JOIN " + TABLE_NAME_HAUPTART
                + " ON " + TABLE_NAME_ART + "." + FIELD__ART_FK + " = "
                + TABLE_NAME_HAUPTART + "." + FIELD__HAUPTART_ID
                + " ORDER BY " + TABLE_NAME_GEBIET + "." + FIELD__GEBIET_AZ + ", " + TABLE_NAME_MELDUNG + "."
                + FIELD__MELDUNG_DATUM;

    //~ Instance fields --------------------------------------------------------

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    @Setter @Getter private Integer ersatzId;
    @Setter @Getter private Integer ersatzFKSchaden;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new BaumSchadenSearch object.
     */
    public BaumSchadenSearch() {
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
            if (getErsatzId() != null) {
                wheres.add(String.format("baum_schaden.id = %d", getErsatzFKSchaden()));
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
            LOG.error("error while searching for baum_schaden", ex);
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
