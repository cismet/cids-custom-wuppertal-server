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

import de.cismet.connectioncontext.ConnectionContext;
import de.cismet.connectioncontext.ConnectionContextStore;

/**
 * DOCUMENT ME!
 *
 * @author   Gilles Baatz
 * @version  $Revision$, $Date$
 */
public class MetaObjectNodesStadtbildSerieSearchStatement extends AbstractCidsServerSearch
        implements MetaObjectNodeServerSearch,
            ConnectionContextStore {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(MetaObjectNodesStadtbildSerieSearchStatement.class);
    private static final String DOMAIN = "WUNDA_BLAU";
    private static final String INTERSECTS_BUFFER = SearchProperties.getInstance().getIntersectsBuffer();

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Bildtyp {

        //~ Enum constants -----------------------------------------------------

        LUFTSCHRAEG(0), LUFTSENK(1), BODENNAH(2), REIHENSCHRAEG(3);

        //~ Instance fields ----------------------------------------------------

        private final int id;

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new Bildtyp object.
         *
         * @param  id  DOCUMENT ME!
         */
        private Bildtyp(final int id) {
            this.id = id;
        }

        //~ Methods ------------------------------------------------------------

        /**
         * DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         */
        public int getId() {
            return id;
        }
    }

    //~ Instance fields --------------------------------------------------------

    private Geometry geometryToSearchFor;

    private ArrayList<Bildtyp> bildtypen = new ArrayList<>();
    private ArrayList<Integer> suchwoerterIDs = new ArrayList<>();
    private ArrayList<Integer> nutzungseinschraenkungIDs = new ArrayList<>();
    private Interval interval;
    private Date from;
    private Date till;
    private String streetID;
    private String ortID;
    private String hausnummer;
    private String imageNumberRule;
    private StringBuilder query;
    private final SimpleDateFormat postgresDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private ArrayList<ArrayList> resultset;
    /**
     * If true the Stadtbildserie must have all selected Suchworte. If false the Stadtbildserie must have only one
     * suchwort.
     */
    private boolean hasAllSuchworte = true;
    /**
     * This ServerSearch can be executed in two modes: a preparation mode and the normal mode. The default mode is the
     * normal mode, where MetaObjectNodes will be returned. In the preparation mode the amount of the found results will
     * be returned.
     */
    private boolean preparationExecution = false;

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new MetaObjectNodesStadtbildSerieSearchStatement object.
     *
     * @param  user  DOCUMENT ME!
     */
    public MetaObjectNodesStadtbildSerieSearchStatement(final User user) {
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public void initWithConnectionContext(final ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Collection prepareResultSetAndReturnItsSize() {
        final MetaService metaService = (MetaService)getActiveLocalServers().get(DOMAIN);
        final Collection<Integer> result = new ArrayList<>();
        if (metaService != null) {
            try {
                generateQuery();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("The used query is: " + query.toString());
                }
                resultset = metaService.performCustomSearch(query.toString(), getConnectionContext());

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
                    resultset = metaService.performCustomSearch(query.toString(), getConnectionContext());
                }

                final ArrayList result = new ArrayList();

                for (final ArrayList stadtbildserie : resultset) {
                    final int classID = (Integer)stadtbildserie.get(0);
                    final int objectID = (Integer)stadtbildserie.get(1);
                    final String name = (String)stadtbildserie.get(2);

                    final MetaObjectNode node = new MetaObjectNode(DOMAIN, objectID, classID, name, null, null); // TODO: Check4CashedGeomAndLightweightJson

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
        if (StringUtils.isNotBlank(imageNumberRule) || (interval != null)) {
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
        appendNutzungseinschraenkungIDs();
        appendOrtID();
        appendHausnummer();
        appendImageNumberRule();
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
                bildtypListString.append(String.valueOf(typ.getId()));
                bildtypListString.append(",");
            }
            // remove last comma
            bildtypListString.deleteCharAt(bildtypListString.length() - 1);
            bildtypListString.append(")");
            query.append(bildtypListString.toString());
        }
    }

    /**
     * Creates an array with all the Stadtbildserie-Ids which have the right Suchworter, then it is checked if the
     * current Stadtbildserie-Id is in the array. The array can be created with an INTERSECT or an UNION. If it is
     * created with the INTERSECT then only the Stadtbildserien will be in the array which have all Suchworte. If the
     * UNION is chosen the array will contain all Stadtbildserien which have at least one suchwort.
     */
    private void appendSuchworte() {
        if (!suchwoerterIDs.isEmpty()) {
            query.append(" and sbs.id IN (");
            String subquery = "select sb_stadtbild_reference from sb_stadtbild_suchwort_array b where b.sb_suchwort = "
                        + suchwoerterIDs.get(0);
            query.append(subquery);
            for (int i = 1; i < suchwoerterIDs.size(); i++) {
                if (hasAllSuchworte) {
                    query.append(" INTERSECT ");
                } else {
                    query.append(" UNION ");
                }
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
    private void appendNutzungseinschraenkungIDs() {
        if ((nutzungseinschraenkungIDs != null) && !nutzungseinschraenkungIDs.isEmpty()) {
            query.append(" and sbs.nutzungseinschraenkung IN (")
                    .append(StringUtils.join(nutzungseinschraenkungIDs, ','))
                    .append(") ");
        } else {
            query.append(" and sbs.nutzungseinschraenkung IS NULL ");
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
    private void appendImageNumberRule() {
        if (StringUtils.isNotBlank(imageNumberRule)) {
            // alle Trennzeichen durch Leerzeichen ersetzen
            final String spaceSeparatedNumbers = imageNumberRule.trim().replaceAll("(\\s|;|,)+", " ");
            // mit Anführungszeichen klammern
            final String quotedNumbers = "'" + spaceSeparatedNumbers.trim().replaceAll(" ", "' '").trim() + "'";
            // split
            final String[] numbers = quotedNumbers.split(" ");

            final String sqlArray = "array[" + implode(numbers, ", ") + "]";
            query.append(" and sb.bildnummer ilike any (").append(sqlArray).append(") ");
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   stringArray  DOCUMENT ME!
     * @param   delimiter    DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static String implode(final Object[] stringArray, final String delimiter) {
        if (stringArray.length == 0) {
            return "";
        } else {
            final StringBuilder sb = new StringBuilder();
            sb.append(stringArray[0]);
            for (int index = 1; index < stringArray.length; index++) {
                sb.append(delimiter);
                sb.append(stringArray[index]);
            }
            return sb.toString();
        }
    }

    /**
     * If an Interval Object is present then the search looks for the bildnummern inside the Interval. An Interval
     * consists of two parts the simple interval and the exact matches. For the simple interval a statement is generated
     * that expresses: intervalStart &lt;= bildnummer &lt;= intervalEnd. Although the statement is more complicated than
     * that because a bildnummer can be e.g. N04711c. The exact matches is a list of bildnummern which should be found
     * independently from the simple interval.
     */
    private void appendInterval() {
        if (interval != null) {
            String logicalConnective = " and ";
            if ((interval.intervalStart != null) && (interval.intervalEnd != null)) {
                String imageNrFrom = interval.intervalStart;
                String imageNrTo = interval.intervalEnd;
                String whereStatement;
                if (Character.isLetter(imageNrFrom.charAt(0))) {
                    final char firstLetter = imageNrFrom.charAt(0);
                    imageNrFrom = imageNrFrom.substring(1);
                    imageNrTo = imageNrTo.substring(1);
                    final int length = imageNrFrom.length();
                    whereStatement = String.format(
                            " and sb.bildnummer ~ '^%4$s\\\\d{%1$d}[a-z]?$' and %2$s <= substring(sb.bildnummer,2,%1$d)::bigint and substring(sb.bildnummer,2,%1$d)::bigint <= %3$s ",
                            length,
                            imageNrFrom,
                            imageNrTo,
                            firstLetter);
                } else {
                    final int length = imageNrFrom.length();
                    whereStatement = String.format(
                            " and sb.bildnummer ~ '^\\\\d{%1$d}[a-z]?$' and %2$s <= substring(sb.bildnummer,1,%1$d)::bigint and substring(sb.bildnummer,1,%1$d)::bigint <= %3$s ",
                            length,
                            imageNrFrom,
                            imageNrTo);
                }
                query.append(whereStatement);
                logicalConnective = " or ";
            }
            if ((interval.additionalExactMatches != null) && !interval.additionalExactMatches.isEmpty()) {
                query.append(logicalConnective);
                query.append(" sb.bildnummer IN ('")
                        .append(StringUtils.join(interval.additionalExactMatches, "','"))
                        .append("') ");
            }
        }
    }

    /**
     * DOCUMENT ME!
     */
    private void appendGeometry() {
        if (geometryToSearchFor != null) {
            final String geostring = PostGisGeometryFactory.getPostGisCompliantDbString(geometryToSearchFor);
            query.append("and g.geo_field && st_GeometryFromText('").append(geostring).append("')");

            if ((geometryToSearchFor instanceof Polygon) || (geometryToSearchFor instanceof MultiPolygon)) { // with buffer for geostring
                query.append(" and st_intersects(" + "st_buffer(geo_field, " + INTERSECTS_BUFFER + "),"
                                + "st_buffer(st_GeometryFromText('")
                        .append(geostring)
                        .append("'), " + INTERSECTS_BUFFER + "))");
            } else {                                                                                         // without buffer for
                // geostring
                query.append(" and st_intersects(" + "st_buffer(geo_field, " + INTERSECTS_BUFFER + "),"
                                + "st_GeometryFromText('")
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
        return imageNumberRule;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  imageNumberRule  DOCUMENT ME!
     */
    public void setImageNumberRule(final String imageNumberRule) {
        this.imageNumberRule = imageNumberRule;
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

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public boolean isHasAllSuchworte() {
        return hasAllSuchworte;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  hasAllSuchworte  DOCUMENT ME!
     */
    public void setHasAllSuchworte(final boolean hasAllSuchworte) {
        this.hasAllSuchworte = hasAllSuchworte;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  nutzungseinschraenkungIDs  DOCUMENT ME!
     */
    public void setNutzungseinschraenkungIDs(final ArrayList<Integer> nutzungseinschraenkungIDs) {
        this.nutzungseinschraenkungIDs = nutzungseinschraenkungIDs;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public ArrayList<Integer> getNutzungseinschraenkungIDs() {
        return nutzungseinschraenkungIDs;
    }

    @Override
    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * Interval is used to search for bildnummern of a stadtbildserie. An Interval consists of two parts the simple
     * interval and the exact matches.
     *
     * <p>For the simple interval later on a statement will be generated that expresses: intervalStart &lt;= bildnummer
     * &lt;= intervalEnd. The exact matches is a list of bildnummern which should be found independently from the simple
     * interval.</p>
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
