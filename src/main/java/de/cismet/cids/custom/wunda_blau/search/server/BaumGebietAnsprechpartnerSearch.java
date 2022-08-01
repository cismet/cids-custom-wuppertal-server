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

import com.vividsolutions.jts.geom.Geometry;

import lombok.Getter;
import lombok.Setter;

import org.apache.log4j.Logger;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.cismet.cids.server.search.AbstractCidsServerSearch;
import de.cismet.cids.server.search.MetaObjectNodeServerSearch;

import de.cismet.cismap.commons.jtsgeometryfactories.PostGisGeometryFactory;

import de.cismet.connectioncontext.ConnectionContext;

/**
 * DOCUMENT ME!
 *
 * @version  $Revision$, $Date$
 */
public class BaumGebietAnsprechpartnerSearch extends AbstractCidsServerSearch implements MetaObjectNodeServerSearch {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient Logger LOG = Logger.getLogger(BaumGebietAnsprechpartnerSearch.class);

    public static final String TABLE_NAME = "baum_ansprechpartner";
    public static final String TABLE_MELDUNG = "baum_meldung";
    public static final String TABLE_GEBIET = "baum_gebiet";
    public static final String TABLE_MELDUNG_AP = "baum_meldung_ansprechpartner";
    public static final String TABLE_ORTSTERMIN_AP = "baum_ortstermin_ansprechpartner";
    public static final String TABLE_ORTSTERMIN = "baum_ortstermin";
    public static final String FIELD__ID = "id";
    public static final String FIELD__NAME = "name";                              // baum_ortstermin
    public static final String FIELD__MAIL = "mail";                              // baum_ortstermin
    public static final String FIELD__STRASSE = "strasse";                        // baum_ortstermin
    public static final String FIELD__ORT = "ort";                                // baum_ortstermin
    public static final String FIELD__BEMERKUNG = "bemerkung";
    public static final String FIELD__FK_AP = "fk_ansprechpartner";               // baum_meldung/ot_ansprechpartner
    public static final String FIELD__FK_GEBIET = "fk_gebiet";                    // baum_meldung
    public static final String FIELD__FK_MELDUNG = "fk_meldung";                  // baum_ortstermin
    public static final String FIELD__ARR_AP = "arr_ansprechpartner";             // baum_meldung
    public static final String FIELD__REFERENCE_M = "baum_meldung_reference";     // baum_meldung_ansprechpartner
    public static final String FIELD__REFERENCE_OT = "baum_ortstermin_reference"; // baum_ortstermin_ansprechpartner
    public static final String FIELD__AZ = "aktenzeichen";                        // baum_gebiet

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String INTERSECTS_BUFFER = SearchProperties.getInstance().getIntersectsBuffer();

    private static final String QUERY_TEMPLATE = "WITH APs AS ( "
                + "SELECT DISTINCT "
                + "g." + FIELD__ID + ", "
                + "g." + FIELD__AZ + ", "
                + "a." + FIELD__NAME + ", "
                + "a." + FIELD__BEMERKUNG + ", "
                + "a." + FIELD__MAIL + ", "
                + "a." + FIELD__ORT + ", "
                + "a." + FIELD__STRASSE
                + " FROM " + TABLE_MELDUNG_AP + " ma"
                + " LEFT JOIN " + TABLE_MELDUNG + " m ON m." + FIELD__ARR_AP + " = "
                + " ma." + FIELD__REFERENCE_M
                + " LEFT JOIN " + TABLE_GEBIET + " g ON g." + FIELD__ID + " = "
                + " m." + FIELD__FK_GEBIET
                + " LEFT JOIN " + TABLE_NAME + " a ON a." + FIELD__ID + " = "
                + "ma." + FIELD__FK_AP
                + " UNION "
                + "SELECT DISTINCT "
                + "g." + FIELD__ID + ", "
                + "g." + FIELD__AZ + ", "
                + "a." + FIELD__NAME + ", "
                + "a." + FIELD__BEMERKUNG + ", "
                + "a." + FIELD__MAIL + ", "
                + "a." + FIELD__ORT + ", "
                + "a." + FIELD__STRASSE
                + " FROM " + TABLE_ORTSTERMIN_AP + " oa"
                + " LEFT JOIN " + TABLE_ORTSTERMIN + " o ON o." + FIELD__ARR_AP + " = "
                + " o." + FIELD__FK_MELDUNG
                + " LEFT JOIN " + TABLE_MELDUNG + " m ON m." + FIELD__ID + " = "
                + " oa." + FIELD__REFERENCE_OT
                + " LEFT JOIN " + TABLE_GEBIET + " g ON g." + FIELD__ID + " = "
                + " m." + FIELD__FK_GEBIET
                + " LEFT JOIN " + TABLE_NAME + " a ON a." + FIELD__ID + " = "
                + "oa." + FIELD__FK_AP + ") "
                + " SELECT DISTINCT "
                + "  (SELECT c.id FROM cs_class c WHERE table_name ILIKE '" + TABLE_GEBIET + "') AS class_id, "
                + FIELD__ID + ", "
                + FIELD__AZ
                + " FROM APs ";

    //~ Instance fields --------------------------------------------------------

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    @Setter @Getter private String ansprechpartnerName;
    @Setter @Getter private String ansprechpartnerStrasse;
    @Setter @Getter private String ansprechpartnerMail;
    @Setter @Getter private String ansprechpartnerBemerkung;
    @Setter @Getter private String ansprechpartnerOrt;
    @Setter @Getter private Geometry geom = null;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new BaumAnsprechpartnerSearch object.
     */
    public BaumGebietAnsprechpartnerSearch() {
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

            if ((getAnsprechpartnerName() != null) && !getAnsprechpartnerName().trim().equals("")) {
                wheres.add(String.format("name ILIKE '%%%s%%'", getAnsprechpartnerName()));
            }

            if ((getAnsprechpartnerOrt() != null) && !getAnsprechpartnerOrt().trim().equals("")) {
                wheres.add(String.format("ort ILIKE '%%%s%%'", getAnsprechpartnerOrt()));
            }

            if ((getAnsprechpartnerBemerkung() != null) && !getAnsprechpartnerBemerkung().trim().equals("")) {
                wheres.add(String.format("bemerkung ILIKE '%%%s%%'", getAnsprechpartnerBemerkung()));
            }

            if ((getAnsprechpartnerMail() != null) && !getAnsprechpartnerMail().trim().equals("")) {
                wheres.add(String.format("mail ILIKE '%%%s%%'", getAnsprechpartnerMail()));
            }

            if ((getAnsprechpartnerStrasse() != null) && !getAnsprechpartnerStrasse().trim().equals("")) {
                wheres.add(String.format("strasse ILIKE '%%%s%%'", getAnsprechpartnerStrasse()));
            }

            if (geom != null) {
                final String geomString = PostGisGeometryFactory.getPostGisCompliantDbString(geom);
                wheres.add("(geom.geo_field && st_GeometryFromText('" + geomString + "') AND st_intersects("
                            + "st_buffer(geo_field, " + INTERSECTS_BUFFER + "),"
                            + "st_GeometryFromText('"
                            + geomString
                            + "')))");
                leftJoins.add("geom ON g.fk_geom = geom.id");
            }

            final String leftJoin = (!leftJoins.isEmpty())
                ? String.format("LEFT JOIN %s", String.join(" LEFT JOIN ", leftJoins)) : "";
            final String where = (!wheres.isEmpty()) ? String.format("WHERE %s", String.join(" AND ", wheres)) : "";
            final String query = String.format("%s %s %s", QUERY_TEMPLATE, leftJoin, where);

            if (LOG.isDebugEnabled()) {
                LOG.debug(query);
            }

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
            LOG.error("error while searching for ansprechpartner in baum_gebiet", ex);
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
