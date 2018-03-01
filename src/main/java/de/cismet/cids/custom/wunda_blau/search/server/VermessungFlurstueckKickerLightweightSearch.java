/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.wunda_blau.search.server;

import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.middleware.types.MetaClass;

import lombok.Getter;
import lombok.Setter;

import org.apache.log4j.Logger;

import org.openide.util.lookup.ServiceProvider;

import java.rmi.RemoteException;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.server.connectioncontext.ServerConnectionContext;
import de.cismet.cids.server.connectioncontext.ServerConnectionContextProvider;
import de.cismet.cids.server.search.AbstractCidsServerSearch;
import de.cismet.cids.server.search.SearchException;

import de.cismet.cidsx.base.types.Type;

import de.cismet.cidsx.server.api.types.SearchInfo;
import de.cismet.cidsx.server.api.types.SearchParameterInfo;
import de.cismet.cidsx.server.search.RestApiCidsServerSearch;
import de.cismet.cidsx.server.search.builtin.legacy.LightweightMetaObjectsSearch;

/**
 * Builtin Legacy Search to delegate the operation getLightweightMetaObjectsByQuery to the cids Pure REST Search API.
 *
 * @author   Pascal Dihé
 * @version  $Revision$, $Date$
 */
@ServiceProvider(service = RestApiCidsServerSearch.class)
public class VermessungFlurstueckKickerLightweightSearch extends AbstractCidsServerSearch
        implements RestApiCidsServerSearch,
            LightweightMetaObjectsSearch,
            ServerConnectionContextProvider {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(VermessungFlurstueckKickerLightweightSearch.class);

    public static final String FLURSTUECK_KICKER_TABLE_NAME_VIEW = "vermessung_flurstueck_kicker";
    public static final String FLURSTUECK_KICKER_TABLE_NAME = "vermessung_flurstueck_kicker";

    private static final String FLURSTUECK_GEMARKUNG = "gemarkung";
    private static final String FLURSTUECK_FLUR = "flur";
    private static final String FLURSTUECK_ZAEHLER = "zaehler";
    private static final String FLURSTUECK_NENNER = "nenner";
    private static final String VERMESSUNG_GEMARKUNG_TABLE_NAME = "vermessung_gemarkung";
    private static final String VERMESSUNG_GEMARKUNG_ID = "id";
    private static final String VERMESSUNG_GEMARKUNG_NAME = "name";

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum SearchFor {

        //~ Enum constants -----------------------------------------------------

        ALLE_FLUSTUECKE, FLURSTUECKE, ALLE_GEMARKUNGEN, GEMARKUNG, FLURE, ZAEHLER_NENNER
    }

    //~ Instance fields --------------------------------------------------------

    @Getter private final SearchInfo searchInfo;
    @Getter @Setter private SearchFor searchFor;
    @Getter @Setter private String gemarkungsnummer;
    @Getter @Setter private String flur;
    @Getter @Setter private String zaehler;
    @Getter @Setter private String nenner;
    @Getter @Setter private String[] representationFields;
    @Getter @Setter private String representationPattern;

    private ServerConnectionContext serverConnectionContext = ServerConnectionContext.create(getClass()
                    .getSimpleName());

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new LightweightMetaObjectsByQuerySearch object.
     */
    public VermessungFlurstueckKickerLightweightSearch() {
        searchInfo = new SearchInfo();
        searchInfo.setKey(this.getClass().getName());
        searchInfo.setName(this.getClass().getSimpleName());
        searchInfo.setDescription(
            "Builtin Legacy Search to delegate the operation getLightweightMetaObjectsByQuery to the cids Pure REST Search API.");

        final List<SearchParameterInfo> parameterDescription = new LinkedList<SearchParameterInfo>();
        SearchParameterInfo searchParameterInfo;

        searchParameterInfo = new SearchParameterInfo();
        searchParameterInfo.setKey("gemarkungsnummer");
        searchParameterInfo.setType(Type.STRING);
        parameterDescription.add(searchParameterInfo);

        searchParameterInfo = new SearchParameterInfo();
        searchParameterInfo.setKey("flur");
        searchParameterInfo.setType(Type.STRING);
        parameterDescription.add(searchParameterInfo);

        searchParameterInfo = new SearchParameterInfo();
        searchParameterInfo.setKey("zaehler");
        searchParameterInfo.setType(Type.STRING);
        parameterDescription.add(searchParameterInfo);

        searchParameterInfo = new SearchParameterInfo();
        searchParameterInfo.setKey("nenner");
        searchParameterInfo.setType(Type.STRING);
        parameterDescription.add(searchParameterInfo);

        searchParameterInfo = new SearchParameterInfo();
        searchParameterInfo.setKey("searchFor");
        searchParameterInfo.setType(Type.UNDEFINED);
        parameterDescription.add(searchParameterInfo);

        searchParameterInfo = new SearchParameterInfo();
        searchParameterInfo.setKey("representationFields");
        searchParameterInfo.setType(Type.STRING);
        searchParameterInfo.setArray(true);
        parameterDescription.add(searchParameterInfo);

        searchParameterInfo = new SearchParameterInfo();
        searchParameterInfo.setKey("representationPattern");
        searchParameterInfo.setType(Type.STRING);
        parameterDescription.add(searchParameterInfo);

        searchInfo.setParameterDescription(parameterDescription);

        final SearchParameterInfo resultParameterInfo = new SearchParameterInfo();
        resultParameterInfo.setKey("return");
        resultParameterInfo.setArray(true);
        resultParameterInfo.setType(Type.ENTITY_REFERENCE);
        searchInfo.setResultDescription(resultParameterInfo);
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public Collection performServerSearch() throws SearchException {
        if (searchFor != null) {
            final MetaService metaService = (MetaService)this.getActiveLocalServers().get("WUNDA_BLAU");
            if (metaService == null) {
                final String message = "Lightweight Meta Objects By Query Search "
                            + "could not connect ot MetaService @domain 'WUNDA_BLAU'";
                LOG.error(message);
                throw new SearchException(message);
            }
            final String query;
            final MetaClass flurstueckKickerMetaClass;
            final MetaClass vermessungsGemarkungenMetaClass;

            try {
                flurstueckKickerMetaClass = CidsBean.getMetaClassFromTableName(
                        "WUNDA_BLAU",
                        FLURSTUECK_KICKER_TABLE_NAME);
                vermessungsGemarkungenMetaClass = CidsBean.getMetaClassFromTableName(
                        "WUNDA_BLAU",
                        VERMESSUNG_GEMARKUNG_TABLE_NAME);
            } catch (final Exception ex) {
                throw new SearchException("error while loadomg metaclass", ex);
            }
            final MetaClass metaClassToUse;
            switch (searchFor) {
                case ALLE_FLUSTUECKE: {
                    metaClassToUse = flurstueckKickerMetaClass;
                    query = "select id,"
                                + FLURSTUECK_GEMARKUNG + ","
                                + FLURSTUECK_FLUR + ","
                                + FLURSTUECK_ZAEHLER + ","
                                + FLURSTUECK_NENNER
                                + " from "
                                // FLURSTUECK_KICKER_TABLE_NAME_VIEW +
                                + FLURSTUECK_KICKER_TABLE_NAME
                                + " order by "
                                + FLURSTUECK_GEMARKUNG + ", "
                                + FLURSTUECK_FLUR + ", "
                                + FLURSTUECK_ZAEHLER + ", "
                                + FLURSTUECK_NENNER;
                }
                break;
                case FLURSTUECKE: {
                    metaClassToUse = flurstueckKickerMetaClass;
                    if (getGemarkungsnummer() == null) {
                        throw new SearchException("gemarkung has to be set");
                    }
                    if (getFlur() == null) {
                        throw new SearchException("flur has to be set");
                    }
                    if (getZaehler() == null) {
                        throw new SearchException("zaehler has to be set");
                    }
                    if (getNenner() == null) {
                        throw new SearchException("nenner has to be set");
                    }
                    query = "select id, "
                                + FLURSTUECK_GEMARKUNG
                                + ","
                                + FLURSTUECK_FLUR
                                + ","
                                + FLURSTUECK_ZAEHLER
                                + ","
                                + FLURSTUECK_NENNER
                                + " from "
                                // + FLURSTUECK_KICKER_TABLE_NAME_VIEW
                                + FLURSTUECK_KICKER_TABLE_NAME
                                + " where "
                                + FLURSTUECK_GEMARKUNG
                                + " = "
                                + getGemarkungsnummer()
                                + " and "
                                + FLURSTUECK_FLUR
                                + " = '"
                                + getFlur()
                                + "' and "
                                + FLURSTUECK_ZAEHLER
                                + " = '"
                                + getZaehler()
                                + "' and "
                                + FLURSTUECK_NENNER
                                + " = '"
                                + getNenner()
                                + "'";
                }
                break;
                case ALLE_GEMARKUNGEN: {
                    metaClassToUse = vermessungsGemarkungenMetaClass;
                    query = "select  " + "f."
                                + FLURSTUECK_GEMARKUNG
                                + " as id, "
                                + "f."
                                + FLURSTUECK_GEMARKUNG
                                + ", min("
                                // + GEMARKUNG_NAME
                                + "g."
                                + VERMESSUNG_GEMARKUNG_NAME
                                + ") as "
                                // + GEMARKUNG_NAME
                                + VERMESSUNG_GEMARKUNG_NAME
                                + " from "
                                // + FLURSTUECK_KICKER_TABLE_NAME_VIEW
                                + FLURSTUECK_KICKER_TABLE_NAME
                                + " f join "
                                // + GEMARKUNG_TABLE_NAME
                                + VERMESSUNG_GEMARKUNG_TABLE_NAME
                                + " g on "
                                + "f."
                                + FLURSTUECK_GEMARKUNG
                                + " = "
                                + "g."
                                + VERMESSUNG_GEMARKUNG_ID
                                + " group by "
                                + "f."
                                + FLURSTUECK_GEMARKUNG
                                + " order by "
                                + "f."
                                + FLURSTUECK_GEMARKUNG;
                }
                break;
                case GEMARKUNG: {
                    metaClassToUse = vermessungsGemarkungenMetaClass;
                    if (getGemarkungsnummer() == null) {
                        throw new SearchException("gemarkung has to be set");
                    }
                    query = "select *"
                                + " from "
                                // + FLURSTUECK_KICKER_TABLE_NAME_VIEW
                                + VERMESSUNG_GEMARKUNG_TABLE_NAME
                                + " where "
                                + "id = "
                                + getGemarkungsnummer();
                }
                break;
                case FLURE: {
                    metaClassToUse = flurstueckKickerMetaClass;
                    if (getGemarkungsnummer() == null) {
                        throw new SearchException("gemarkung has to be set");
                    }
                    query = "select min(id) as id, "
                                + FLURSTUECK_FLUR
                                + " from "
                                // + FLURSTUECK_KICKER_TABLE_NAME_VIEW
                                + FLURSTUECK_KICKER_TABLE_NAME
                                + " where "
                                + FLURSTUECK_GEMARKUNG
                                + " = "
                                + getGemarkungsnummer()
                                + " group by "
                                + FLURSTUECK_FLUR
                                + " order by "
                                + FLURSTUECK_FLUR;
                }
                break;
                case ZAEHLER_NENNER: {
                    metaClassToUse = flurstueckKickerMetaClass;
                    if (getGemarkungsnummer() == null) {
                        throw new SearchException("gemarkung has to be set");
                    }
                    if (getFlur() == null) {
                        throw new SearchException("flur has to be set");
                    }
                    query = "select min(id) as id, "
                                + FLURSTUECK_ZAEHLER
                                + ", "
                                + FLURSTUECK_NENNER
                                + ", "
                                + FLURSTUECK_FLUR
                                + ", "
                                + FLURSTUECK_GEMARKUNG
                                + " from "
                                // + FLURSTUECK_KICKER_TABLE_NAME_VIEW
                                + FLURSTUECK_KICKER_TABLE_NAME
                                + " where "
                                + FLURSTUECK_GEMARKUNG
                                + " = "
                                + getGemarkungsnummer()
                                + " and "
                                + FLURSTUECK_FLUR
                                + " = '"
                                + getFlur()
                                + "' group by "
                                + FLURSTUECK_ZAEHLER
                                + ", "
                                + FLURSTUECK_NENNER
                                + ", "
                                + FLURSTUECK_FLUR
                                + ", "
                                + FLURSTUECK_GEMARKUNG;
                }
                break;
                default: {
                    metaClassToUse = flurstueckKickerMetaClass;
                    query = null;
                }
            }
            try {
                if (getRepresentationPattern() != null) {
                    return Arrays.asList(metaService.getLightweightMetaObjectsByQuery(
                                metaClassToUse.getID(),
                                getUser(),
                                query,
                                getRepresentationFields(),
                                getRepresentationPattern(),
                                getServerConnectionContext()));
                } else {
                    return Arrays.asList(metaService.getLightweightMetaObjectsByQuery(
                                metaClassToUse.getID(),
                                getUser(),
                                query,
                                getRepresentationFields(),
                                getServerConnectionContext()));
                }
            } catch (final RemoteException ex) {
                throw new SearchException("error while loading lwmos (MetaClass:+" + metaClassToUse.getID() + ", Query:"
                            + query + ")",
                    ex);
            }
        } else {
            throw new SearchException("searchFor has to be set");
        }
    }

    @Override
    public ServerConnectionContext getServerConnectionContext() {
        return serverConnectionContext;
    }

    @Override
    public void setServerConnectionContext(final ServerConnectionContext serverConnectionContext) {
        this.serverConnectionContext = serverConnectionContext;
    }
}
