/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.deletionprovider;

import Sirius.server.localserver.object.AbstractCustomDeletionProvider;
import Sirius.server.localserver.object.CustomDeletionProvider;
import Sirius.server.localserver.object.DeletionProviderClientException;
import Sirius.server.middleware.types.MetaObject;
import Sirius.server.newuser.User;

import org.apache.log4j.Logger;

import org.openide.util.lookup.ServiceProvider;

import java.rmi.RemoteException;

import java.util.ArrayList;

import de.cismet.cids.dynamics.CidsBean;

/**
 * DOCUMENT ME!
 *
 * @author   sandra
 * @version  $Revision$, $Date$
 */
@ServiceProvider(service = CustomDeletionProvider.class)
public class PoiLocationinstanceDeletionProvider extends AbstractCustomDeletionProvider {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(PoiLocationinstanceDeletionProvider.class);
    private static final String TABLE_NAME = "poi_locationinstance";
    private static final String FIELD__ID = "id";
    private static final String FIELD__FK = "fk_locationinstance";
    private static final String TABLE_NAME_SEARCH = "poi_zoomdefinition";
    private static final String DELETE_TEXT =
        "Dieser Poi kann nicht gelöscht werden, da dieser bei einer Zoomdefinition verwendet wird.";

    //~ Instance fields --------------------------------------------------------

    private String deleteText = "Dieser Poi kann nicht gelöscht werden.";

    //~ Methods ----------------------------------------------------------------

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public boolean isMatching(final User user, final MetaObject metaObject) {
        if (!super.isMatching(user, metaObject)) {
            return false;
        }

        final CidsBean poiBean = metaObject.getBean();
        final Integer poi_id = (Integer)poiBean.getProperty(FIELD__ID);

        final String query = String.format(
                "SELECT * FROM %s WHERE %s = %d;",
                TABLE_NAME_SEARCH,
                FIELD__FK,
                poi_id);

        try {
            final ArrayList<ArrayList> poiArray = getMetaService().performCustomSearch(
                    query,
                    getConnectionContext());
            if (!poiArray.isEmpty()) {
                deleteText = DELETE_TEXT;
                return true;
            }
        } catch (RemoteException ex) {
            LOG.error("Cannot delete poi object", ex);
        }
        return false;
    }

    @Override
    public boolean customDeleteMetaObject(final User user, final MetaObject metaObject) throws Exception {
        // darf nicht geloescht werden
        throw new DeletionProviderClientException(deleteText);
    }

    @Override
    public String getDomain() {
        return "WUNDA_BLAU";
    }
}
