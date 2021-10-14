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
public class BaumHauptartDeletionProvider extends AbstractCustomDeletionProvider {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(BaumHauptartDeletionProvider.class);
    private static final String TABLE_NAME = "baum_hauptart";
    private static final String FIELD__ID = "id";
    private static final String FIELD__FK = "fk_hauptart";
    private static final String TABLE_NAME_SEARCH = "baum_art";
    private static final String DELETE_TEXT = "Diese Hauptart kann nicht gelöscht werden, da diese verwendet wird.";

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

        final CidsBean hauptartBean = metaObject.getBean();
        final Integer hauptart_id = (Integer)hauptartBean.getProperty(FIELD__ID);

        final String queryHauptartInArt = String.format(
                "SELECT * FROM %s WHERE %s = %d;",
                TABLE_NAME_SEARCH,
                FIELD__FK,
                hauptart_id);
        try {
            final ArrayList<ArrayList> hauptartArray = getMetaService().performCustomSearch(
                    queryHauptartInArt,
                    getConnectionContext());
            if (!hauptartArray.isEmpty()) {
                return true;
            }
        } catch (RemoteException ex) {
            LOG.error("Cannot delete hauptart object", ex);
        }
        return false;
    }

    @Override
    public boolean customDeleteMetaObject(final User user, final MetaObject metaObject) throws Exception {
        // darf nicht geloescht werden
        throw new DeletionProviderClientException(DELETE_TEXT);
    }

    @Override
    public String getDomain() {
        return "WUNDA_BLAU";
    }
}
