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

import de.cismet.cids.dynamics.CidsBean;
import java.rmi.RemoteException;
import java.util.ArrayList;

/**
 * DOCUMENT ME!
 *
 * @author   sandra
 * @version  $Revision$, $Date$
 */
@ServiceProvider(service = CustomDeletionProvider.class)
public class BaumKroneDeletionProvider extends AbstractCustomDeletionProvider {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(BaumKroneDeletionProvider.class);
    public static final String TABLE_NAME = "baum_krone";
    public static final String FIELD__ID = "id";
    public static final String FIELD__FK = "fk_krone";
    public static final String TABLE_NAME_SEARCH_S = "baum_schaden_krone";
    public String DELETE_TEXT = 
            "Dieser Kronenschaden kann nicht gelöscht werden, da dieser verwendet wird.";
    public boolean notToDelete = false;

    //~ Methods ----------------------------------------------------------------

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public boolean isMatching(final User user, final MetaObject metaObject) {
        if (metaObject != null) {
            final CidsBean schadenBean = metaObject.getBean();
            final Integer schaden_id = (Integer) schadenBean.getProperty(FIELD__ID);
            
            final String queryKroneInSchaden = String.format(
                        "SELECT * FROM %s WHERE %s = %d;",
                        TABLE_NAME_SEARCH_S, FIELD__FK, schaden_id); 
            try {
                ArrayList<ArrayList>artArrayS = getMetaService().performCustomSearch(
                        queryKroneInSchaden, 
                        getConnectionContext());
                if (artArrayS.size() < 1) {
                    return false; // kein true sonst läuft jede Klasse durch
                }else {
                    notToDelete = true;
                }
            } catch (RemoteException ex) {
                LOG.error("Cannot delete krone object", ex);
            }
        }
        return super.isMatching(user, metaObject);
    }

    @Override
    public boolean customDeleteMetaObject(final User user, final MetaObject metaObject) throws Exception {
        if (metaObject != null) {
            // darf nicht geloescht werden
            
            if (notToDelete) {
                throw new DeletionProviderClientException(
                        DELETE_TEXT);
            }
        }
        return false;
    }
}
