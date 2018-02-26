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
 * Search the Gemeinde of a given geometry.
 *
 * @author   Thorsten Herter
 * @version  $Revision$, $Date$
 */
public class GemeindeByGeometrySearch extends AbstractCidsServerSearch implements ServerConnectionContextProvider {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(GemeindeByGeometrySearch.class);
    private static final String DOMAIN = "WUNDA_BLAU";
    private static final String QUERY_GEMEINDE = "select distinct g.name from flurstueck f "
                + "join gemarkung g on (g.gemarkungsnummer = f.gemarkungs_nr) "
                + "join geom on (umschreibendes_rechteck = geom.id) "
                + "where st_intersects(geo_field,  st_setSrid('%s', 25832))";

    //~ Instance fields --------------------------------------------------------

    private String geom;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new LightweightMetaObjectsByQuerySearch object.
     *
     * @param  geom  kompensationId DOCUMENT ME!
     */
    public GemeindeByGeometrySearch(final String geom) {
        this.geom = geom;
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public Collection performServerSearch() throws SearchException {
        try {
            final MetaService metaService = (MetaService)this.getActiveLocalServers().get(DOMAIN);
            if (metaService != null) {
                final ArrayList<ArrayList> gemeindeList = metaService.performCustomSearch(String.format(
                            QUERY_GEMEINDE,
                            geom),
                        getServerConnectionContext());

                String gemeindeCs = null;

                if ((gemeindeList != null) && (gemeindeList.size() > 0)) {
                    for (final ArrayList resultPart : gemeindeList) {
                        if (gemeindeCs != null) {
                            gemeindeCs += ", ";
                            gemeindeCs += resultPart.get(0);
                        } else {
                            gemeindeCs = (String)resultPart.get(0);
                        }
                    }

                    return Arrays.asList(gemeindeCs);
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
        return ServerConnectionContext.create(GemeindeByGeometrySearch.class.getSimpleName());
    }
}
