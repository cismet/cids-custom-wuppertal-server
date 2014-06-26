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
import Sirius.server.newuser.User;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.io.Serializable;

import java.rmi.RemoteException;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import de.cismet.cids.server.search.AbstractCidsServerSearch;
import de.cismet.cids.server.search.MetaObjectNodeServerSearch;
import de.cismet.cids.server.search.SearchException;

import de.cismet.cismap.commons.jtsgeometryfactories.PostGisGeometryFactory;

/**
 * DOCUMENT ME!
 *
 * @author   Gilles Baatz
 * @version  $Revision$, $Date$
 */
public class MetaObjectNodesStadtbildSerieSearchStatement extends AbstractCidsServerSearch
        implements MetaObjectNodeServerSearch {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(MetaObjectNodesStadtbildSerieSearchStatement.class);
    private static final String DOMAIN = "WUNDA_BLAU";

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Bildtyp {

        //~ Enum constants -----------------------------------------------------

        BODENNAH("2"), LUFTSCHRAEG("0"), LUFTSENK("1");

        //~ Instance fields ----------------------------------------------------

        private final String id;

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new Bildtyp object.
         *
         * @param  id  DOCUMENT ME!
         */
        private Bildtyp(final String id) {
            this.id = id;
        }
    }

    //~ Instance fields --------------------------------------------------------

    private Geometry geometryToSearchFor;

    private ArrayList<Bildtyp> bildtypen = new ArrayList<Bildtyp>();
    private ArrayList<Integer> suchwoerterIDs = new ArrayList<Integer>();
    private Interval interval;
    private Date from;
    private Date till;
    private String streetID;
    private String ortID;
    private String hausnummer;
    private String singleImageNumber;
    private final User user;
    private StringBuilder query;
    private final SimpleDateFormat postgresDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private ArrayList<ArrayList> resultset;
    private boolean preparationExecution = false;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new MetaObjectNodesStadtbildSerieSearchStatement object.
     *
     * @param  user  DOCUMENT ME!
     */
    public MetaObjectNodesStadtbildSerieSearchStatement(final User user) {
        this.user = user;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Collection prepareResultSetAndReturnItsSize() {
        final MetaService metaService = (MetaService)getActiveLocalServers().get(DOMAIN);
        final Collection<Integer> result = new ArrayList<Integer>();
        if (metaService != null) {
            try {
                generateQuery();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("The used query is: " + query.toString());
                }
                resultset = metaService.performCustomSearch(query.toString());

                result.add(resultset.size());
                return result;
            } catch (RemoteException ex) {
                LOG.error(ex.getMessage(), ex);
            }
        } else {
            LOG.error("active local server not found"); // NOI18N
        }
        result.clear();
        result.add(0);
        return result;
    }

    @Override
    public Collection<MetaObjectNode> performServerSearch() throws SearchException {
        if (isPreparationExecution()) {
            return prepareResultSetAndReturnItsSize();
        }

        final MetaService metaService = (MetaService)getActiveLocalServers().get(DOMAIN);
        if (metaService != null) {
            try {
                if (query == null) {
                    generateQuery();
                }
                if (LOG.isDebugEnabled()) {
                    LOG.debug("The used query is: " + query.toString());
                }

                if (resultset == null) {
                    resultset = metaService.performCustomSearch(query.toString());
                }

                final ArrayList result = new ArrayList();

                for (final ArrayList stadtbildserie : resultset) {
                    final int classID = (Integer)stadtbildserie.get(0);
                    final int objectID = (Integer)stadtbildserie.get(1);
                    final String name = (String)stadtbildserie.get(2);

                    final MetaObjectNode node = new MetaObjectNode(DOMAIN, objectID, classID, name);

                    result.add(node);
                }
                return result;
            } catch (RemoteException ex) {
                LOG.error(ex.getMessage(), ex);
            }
        } else {
            LOG.error("active local server not found"); // NOI18N
        }
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String generateQuery() {
        query = new StringBuilder();
        query.append("SELECT DISTINCT " + "(SELECT id "
                    + "                FROM    cs_class "
                    + "                WHERE   name ilike 'sb_stadtbildserie' "
                    + "                ), sbs.id, (select bildnummer from sb_stadtbild sb where sb.id = sbs.vorschaubild) ");
        query.append(" FROM sb_stadtbildserie sbs");
        if (StringUtils.isNotBlank(singleImageNumber) || (interval != null)) {
            query.append(" join sb_serie_bild_array as arr ");
            query.append(" on sbs.id = arr.sb_stadtbildserie_reference ");
            query.append(" JOIN sb_stadtbild AS sb ON sb.id = arr.stadtbild ");
        }

        if (geometryToSearchFor != null) {
            query.append(" join geom g ON sbs.geom = g.id ");
        }
        query.append(" WHERE ");
        query.append(" TRUE ");
        appendBildtyp();
        appendSuchworte();
        appendDates();
        appendStreetID();
        appendOrtID();
        appendHausnummer();
        appendSingleImageNumber();
        appendInterval();
        appendGeometry();
        return query.toString();
    }

    /**
     * DOCUMENT ME!
     */
    private void appendBildtyp() {
        if (!bildtypen.isEmpty()) {
            final StringBuilder bildtypListString = new StringBuilder(" and sbs.bildtyp in (");
            for (final Bildtyp typ : bildtypen) {
                bildtypListString.append(typ.id);
                bildtypListString.append(",");
            }
            // remove last comma
            bildtypListString.deleteCharAt(bildtypListString.length() - 1);
            bildtypListString.append(")");
            query.append(bildtypListString.toString());
        }
    }

    /**
     * DOCUMENT ME!
     */
    private void appendSuchworte() {
        if (!suchwoerterIDs.isEmpty()) {
            query.append(" and sbs.id IN (");
            String subquery = "select sb_stadtbild_reference from sb_stadtbild_suchwort_array b where b.sb_suchwort = "
                        + suchwoerterIDs.get(0);
            query.append(subquery);
            for (int i = 1; i < suchwoerterIDs.size(); i++) {
                query.append(" INTERSECT ");
                subquery = "select sb_stadtbild_reference from sb_stadtbild_suchwort_array b where b.sb_suchwort = "
                            + suchwoerterIDs.get(i);
                query.append(subquery);
            }
            query.append(" ) ");
        }
    }

    /**
     * DOCUMENT ME!
     */
    private void appendDates() {
        if ((from == null) && (till == null)) {
            // do nothing, time filters are ignored
        } else if (till == null) {                                                            // only from-date
                                                                                              // available get
                                                                                              // everything after
                                                                                              // that date
            query.append(" and date_trunc('day',aufnahmedatum) >= '");
            query.append(postgresDateFormat.format(from));
            query.append("' ");
        } else if (from == null) {                                                            // only till-date
                                                                                              // available get
                                                                                              // everything before
                                                                                              // that date
            query.append(" and date_trunc('day',aufnahmedatum) <= '");
            query.append(postgresDateFormat.format(till));
            query.append("' ");
        } else if (postgresDateFormat.format(till).equals(postgresDateFormat.format(from))) { // check they are the
                                                                                              // same day
            query.append(" and date_trunc('day',aufnahmedatum) = '");
            query.append(postgresDateFormat.format(from));
            query.append("' ");
        } else {                                                                              // create query for a
                                                                                              // time period
            query.append(" and date_trunc('day',aufnahmedatum) >= '");
            query.append(postgresDateFormat.format(from));
            query.append("' ");
            query.append(" and date_trunc('day',aufnahmedatum) <= '");
            query.append(postgresDateFormat.format(till));
            query.append("' ");
        }
    }

    /**
     * DOCUMENT ME!
     */
    private void appendStreetID() {
        if (StringUtils.isNotBlank(streetID)) {
            query.append(" and sbs.strasse = ").append(streetID).append(" ");
        }
    }

    /**
     * DOCUMENT ME!
     */
    private void appendOrtID() {
        if (StringUtils.isNotBlank(ortID)) {
            query.append(" and sbs.ort = ").append(ortID).append(" ");
        }
    }

    /**
     * DOCUMENT ME!
     */
    private void appendHausnummer() {
        if (StringUtils.isNotBlank(hausnummer)) {
            query.append("and sbs.hausnummer ilike '").append(hausnummer).append("' ");
        }
    }

    /**
     * DOCUMENT ME!
     */
    private void appendSingleImageNumber() {
        if (StringUtils.isNotBlank(singleImageNumber)) {
            query.append(" and sb.bildnummer ilike '").append(singleImageNumber).append("' ");
        }
    }

    /**
     * DOCUMENT ME!
     */
    private void appendInterval() {
        if (!interval.isEmpty()) {
            if (simpleInterval) {
                String imageNrFrom = interval.get(0);
                String imageNrTo = interval.get(1);
                String whereStatement;
                if (Character.isLetter(imageNrFrom.charAt(0))) {
                    final char firstLetter = imageNrFrom.charAt(0);
                    imageNrFrom = imageNrFrom.substring(1);
                    imageNrTo = imageNrTo.substring(1);
                    final int length = imageNrFrom.length();
                    whereStatement = String.format(
                            "and sb.bildnummer ~ '^%4$s\\\\d{%1$d}[a-z]?$' and %2$s <= substring(sb.bildnummer,2,%1$d)::bigint and substring(sb.bildnummer,2,%1$d)::bigint <= %3$s ",
                            length,
                            imageNrFrom,
                            imageNrTo,
                            firstLetter);
                } else {
                    final int length = imageNrFrom.length();
                    whereStatement = String.format(
                            "and sb.bildnummer ~ '^\\\\d{%1$d}[a-z]?$' and %2$s <= substring(sb.bildnummer,1,%1$d)::bigint and substring(sb.bildnummer,1,%1$d)::bigint <= %3$s ",
                            length,
                            imageNrFrom,
                            imageNrTo);
                }
                query.append(whereStatement);
            } else {
                query.append(" and sb.bildnummer IN ('").append(StringUtils.join(interval, "','")).append("') ");
            }
        }
    }

    /**
     * DOCUMENT ME!
     */
    private void appendGeometry() {
        if (geometryToSearchFor != null) {
            final String geostring = PostGisGeometryFactory.getPostGisCompliantDbString(geometryToSearchFor);
            query.append("and g.geo_field && GeometryFromText('").append(geostring).append("')");

            if ((geometryToSearchFor instanceof Polygon) || (geometryToSearchFor instanceof MultiPolygon)) { // with buffer for geostring
                query.append(" and intersects(" + "st_buffer(geo_field, 0.000001)," + "st_buffer(GeometryFromText('")
                        .append(geostring)
                        .append("'), 0.000001))");
            } else {                                                                                         // without buffer for
                // geostring
                query.append(" and intersects(" + "st_buffer(geo_field, 0.000001)," + "GeometryFromText('")
                        .append(geostring)
                        .append("'))");
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public ArrayList<Bildtyp> getBildtypen() {
        return bildtypen;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  bildtypen  DOCUMENT ME!
     */
    public void setBildtypen(final ArrayList<Bildtyp> bildtypen) {
        this.bildtypen = bildtypen;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public ArrayList<Integer> getSuchwoerterIDs() {
        return suchwoerterIDs;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  suchwoerterIDs  DOCUMENT ME!
     */
    public void setSuchwoerterIDs(final ArrayList<Integer> suchwoerterIDs) {
        this.suchwoerterIDs = suchwoerterIDs;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Date getFrom() {
        return from;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  from  DOCUMENT ME!
     */
    public void setFrom(final Date from) {
        this.from = from;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Date getTill() {
        return till;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  till  DOCUMENT ME!
     */
    public void setTill(final Date till) {
        this.till = till;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getStreetID() {
        return streetID;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  streetID  DOCUMENT ME!
     */
    public void setStreetID(final String streetID) {
        this.streetID = streetID;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getOrtID() {
        return ortID;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  ortID  DOCUMENT ME!
     */
    public void setOrtID(final String ortID) {
        this.ortID = ortID;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getHausnummer() {
        return hausnummer;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  hausnummer  DOCUMENT ME!
     */
    public void setHausnummer(final String hausnummer) {
        this.hausnummer = hausnummer;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getSingleImageNumber() {
        return singleImageNumber;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  singleImageNumber  DOCUMENT ME!
     */
    public void setSingleImageNumber(final String singleImageNumber) {
        this.singleImageNumber = singleImageNumber;
    }

    /**
     * This ServerSearch can be executed in two modes: a preparation mode and the normal mode. The default mode is the
     * normal mode, where MetaObjectNodes will be returned. In the preparation mode the amount of the found results will
     * be returned.
     *
     * @return  true: preparation mode. false: normal mode
     */
    public boolean isPreparationExecution() {
        return preparationExecution;
    }

    /**
     * This ServerSearch can be executed in two modes: a preparation mode and the normal mode. The default mode is the
     * normal mode, where MetaObjectNodes will be returned. In the preparation mode the amount of the found results will
     * be returned.
     *
     * @param  preparationExecution  true: preparation mode. false: normal mode
     */
    public void setPreparationExecution(final boolean preparationExecution) {
        this.preparationExecution = preparationExecution;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  geometryToSearchFor  DOCUMENT ME!
     */
    public void setGeometryToSearchFor(final Geometry geometryToSearchFor) {
        this.geometryToSearchFor = geometryToSearchFor;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Interval getInterval() {
        return interval;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  interval  DOCUMENT ME!
     */
    public void setInterval(final Interval interval) {
        this.interval = interval;
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public static class Interval implements Serializable {

        //~ Instance fields ----------------------------------------------------

        private final String intervalStart;
        private final String intervalEnd;
        private final ArrayList<String> additionalExactMatches;

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new Interval object.
         *
         * @param  intervalStart  DOCUMENT ME!
         * @param  intervalEnd    DOCUMENT ME!
         */
        public Interval(final String intervalStart, final String intervalEnd) {
            this(intervalStart, intervalEnd, null);
        }

        /**
         * Creates a new Interval object.
         *
         * @param  intervalStart           DOCUMENT ME!
         * @param  intervalEnd             DOCUMENT ME!
         * @param  additionalExactMatches  DOCUMENT ME!
         */
        public Interval(final String intervalStart,
                final String intervalEnd,
                final ArrayList<String> additionalExactMatches) {
            this.intervalStart = intervalStart;
            this.intervalEnd = intervalEnd;
            this.additionalExactMatches = additionalExactMatches;
        }

        //~ Methods ------------------------------------------------------------

        /**
         * DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         */
        public String getIntervalStart() {
            return intervalStart;
        }

        /**
         * DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         */
        public String getIntervalEnd() {
            return intervalEnd;
        }

        /**
         * DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         */
        public ArrayList<String> getAdditionalExactMatches() {
            return additionalExactMatches;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = (41 * hash) + ((this.intervalStart != null) ? this.intervalStart.hashCode() : 0);
            hash = (41 * hash) + ((this.intervalEnd != null) ? this.intervalEnd.hashCode() : 0);
            hash = (41 * hash) + ((this.additionalExactMatches != null) ? this.additionalExactMatches.hashCode() : 0);
            return hash;
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Interval other = (Interval)obj;
            if ((this.intervalStart == null) ? (other.intervalStart != null)
                                             : (!this.intervalStart.equals(other.intervalStart))) {
                return false;
            }
            if ((this.intervalEnd == null) ? (other.intervalEnd != null)
                                           : (!this.intervalEnd.equals(other.intervalEnd))) {
                return false;
            }
            if ((this.additionalExactMatches != other.additionalExactMatches)
                        && ((this.additionalExactMatches == null)
                            || !this.additionalExactMatches.equals(other.additionalExactMatches))) {
                return false;
            }
            return true;
        }
    }
}
