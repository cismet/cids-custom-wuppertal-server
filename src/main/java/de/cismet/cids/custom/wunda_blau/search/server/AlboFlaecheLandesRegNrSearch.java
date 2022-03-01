/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.wunda_blau.search.server;

import Sirius.server.middleware.interfaces.domainserver.MetaService;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Collection;

import de.cismet.cids.server.search.AbstractCidsServerSearch;

import de.cismet.connectioncontext.ConnectionContext;
import de.cismet.connectioncontext.ConnectionContextStore;

/**
 * DOCUMENT ME!
 *
 * @author   stefan
 * @version  $Revision$, $Date$
 */
public class AlboFlaecheLandesRegNrSearch extends AbstractCidsServerSearch implements ConnectionContextStore {

    //~ Static fields/initializers ---------------------------------------------

    private static final String QUERY_AREA =
        "SELECT landreg from baublock where st_intersects(geom, '%1$s'::geometry) order by st_area(st_intersection(geom, '%1$s'::geometry)) desc limit 1";
    private static final String QUERY_LFD_NR =
        "select laufende_nummer from albo_flaeche where landesregistriernummer = '%1s'";

    //~ Instance fields --------------------------------------------------------

    private final String geometryAsText;

    private ConnectionContext connectionContext = ConnectionContext.createDummy();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new Alb_BaulastChecker object.
     *
     * @param  geometryAsText  blattnummer DOCUMENT ME!
     */
    public AlboFlaecheLandesRegNrSearch(final String geometryAsText) {
        this.geometryAsText = geometryAsText;
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public void initWithConnectionContext(final ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }

    @Override
    public Collection performServerSearch() {
        final MetaService ms = (MetaService)getActiveLocalServers().get("WUNDA_BLAU");
        if (ms != null) {
            try {
                final ArrayList<ArrayList> landReg = ms.performCustomSearch(String.format(QUERY_AREA, geometryAsText),
                        getConnectionContext());

                if ((landReg != null) && (landReg.size() > 0) && (landReg.get(0) != null) && (landReg.get(0).size() > 0)
                            && (landReg.get(0).get(0) != null)) {
                    final String currentLandReg = String.valueOf(landReg.get(0).get(0));

                    final ArrayList<ArrayList> nr = ms.performCustomSearch(String.format(QUERY_LFD_NR, currentLandReg),
                            getConnectionContext());
                    int maxNumber = 0;

                    if ((nr != null) && (nr.size() > 0)) {
                        for (int i = 0; i < nr.size(); ++i) {
                            if ((nr.get(i) != null) && (nr.size() > 0)) {
                                try {
                                    final int noToCheck = Integer.parseInt(String.valueOf(nr.get(i).get(0)));

                                    if (maxNumber < noToCheck) {
                                        maxNumber = noToCheck;
                                    }
                                } catch (NumberFormatException e) {
                                    // nothing to do
                                }
                            }
                        }
                    }

                    final ArrayList<ArrayList<String>> resultList = new ArrayList<>();
                    final ArrayList<String> numberList = new ArrayList<>();

                    numberList.add(currentLandReg);
                    numberList.add(String.format("%04d", (maxNumber + 1)));

                    resultList.add(numberList);

                    return resultList;
                }

                return null;
            } catch (RemoteException ex) {
            }
        }

        return null;
    }

    @Override
    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }
}
