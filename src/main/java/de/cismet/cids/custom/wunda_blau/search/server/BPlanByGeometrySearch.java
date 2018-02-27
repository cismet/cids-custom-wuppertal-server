/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.wunda_blau.search.server;

import Sirius.server.middleware.interfaces.domainserver.MetaService;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import de.cismet.cids.server.connectioncontext.ServerConnectionContext;
import de.cismet.cids.server.connectioncontext.ServerConnectionContextProvider;
import de.cismet.cids.server.search.AbstractCidsServerSearch;
import de.cismet.cids.server.search.SearchException;

/**
 * Search the BPlan of a given geometry.
 *
 * @author   Thorsten Herter
 * @version  $Revision$, $Date$
 */
public class BPlanByGeometrySearch extends AbstractCidsServerSearch implements ServerConnectionContextProvider {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(BPlanByGeometrySearch.class);
    private static final String DOMAIN = "WUNDA_BLAU";
    private static final String QUERY_BPLAN = "select nummer || ' ' || name from bplan_verfahren "
                + "join geom on (geometrie = geom.id) "
                + "where st_intersects(geo_field, st_setSrid('%s', 25832))";
//    private static final String QUERY_BPLAN = "select verfahren || ' ' || name from bplan_plan join geom on (geometrie = geom.id) where st_intersects(geo_field, st_setSrid('%s', 25832))";

    //~ Instance fields --------------------------------------------------------

    private String geom;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new LightweightMetaObjectsByQuerySearch object.
     *
     * @param  geom  kompensationId DOCUMENT ME!
     */
    public BPlanByGeometrySearch(final String geom) {
        this.geom = geom;
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public Collection performServerSearch() throws SearchException {
        try {
            final MetaService metaService = (MetaService)this.getActiveLocalServers().get(DOMAIN);
            if (metaService != null) {
                final ArrayList<ArrayList> bPlanList = metaService.performCustomSearch(String.format(
                            QUERY_BPLAN,
                            geom),
                        getServerConnectionContext());

                String bPlanCs = null;

                if ((bPlanList != null) && (bPlanList.size() > 0)) {
                    for (final ArrayList resultPart : bPlanList) {
                        if (bPlanCs != null) {
                            bPlanCs += ", ";
                            bPlanCs += resultPart.get(0);
                        } else {
                            bPlanCs = (String)resultPart.get(0);
                        }
                    }

                    return Arrays.asList(bPlanCs);
                }
            } else {
                LOG.error("active local server not found"); // NOI18N
            }

            return null;
        } catch (final Exception ex) {
            throw new SearchException("error while loading verfahren objects", ex);
        }
    }

    @Override
    public ServerConnectionContext getServerConnectionContext() {
        return ServerConnectionContext.create(getClass().getSimpleName());
    }
}
