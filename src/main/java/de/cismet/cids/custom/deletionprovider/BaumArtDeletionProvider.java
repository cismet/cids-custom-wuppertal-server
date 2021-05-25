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
import org.openide.util.Exceptions;

/**
 * DOCUMENT ME!
 *
 * @author   sandra
 * @version  $Revision$, $Date$
 */
@ServiceProvider(service = CustomDeletionProvider.class)
public class BaumArtDeletionProvider extends AbstractCustomDeletionProvider {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(BaumArtDeletionProvider.class);
    public static final String TABLE_NAME = "baum_art";
    public static final String FIELD__ID = "id";
    public static final String FIELD__FK = "fk_art";
    public static final String TABLE_NAME_SEARCH_S = "baum_schaden";
    public static final String TABLE_NAME_SEARCH_E = "baum_ersatz";
    public static final String DELETE_TEXT = "Diese Art kann nicht gelöscht werden, da diese verwendet wird.";
    public boolean notToDelete = false;

    //~ Methods ----------------------------------------------------------------

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public boolean isMatching(final User user, final MetaObject metaObject) {
        if (metaObject != null) {
            final CidsBean artBean = metaObject.getBean();
            final Integer art_id = (Integer) artBean.getProperty(FIELD__ID);
            
            final String queryArtInSchaden = ""
                        + "SELECT * "
                        + "FROM "
                        + TABLE_NAME_SEARCH_S
                        + " WHERE "
                        + FIELD__FK
                        + " = "
                        + art_id
                        + ";"; 
            final String queryArtInErsatz = ""
                        + "SELECT * "
                        + "FROM "
                        + TABLE_NAME_SEARCH_E
                        + " WHERE "
                        + FIELD__FK
                        + " = "
                        + art_id
                        + ";"; 
            try {
                ArrayList<ArrayList>artArrayS = getMetaService().performCustomSearch(queryArtInSchaden, getConnectionContext());
                if (artArrayS.size() < 1) {
                    ArrayList<ArrayList>artArrayE = getMetaService().performCustomSearch(queryArtInErsatz, getConnectionContext());
                    if (artArrayE.size() < 1) {
                        return false; // kein true sonst läuft jede Klasse durch
                    }else {
                        notToDelete = true;
                    }
                }else {
                    notToDelete = true;
                }
            } catch (RemoteException ex) {
                Exceptions.printStackTrace(ex);
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
