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
public class BaumAnsprechpartnerDeletionProvider extends AbstractCustomDeletionProvider {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(BaumAnsprechpartnerDeletionProvider.class);
    public static final String TABLE_NAME = "baum_ansprechpartner";
    public static final String FIELD__ID = "id";
    public static final String FIELD__FK = "fk_ansprechpartner";
    public static final String TABLE_NAME_SEARCH = "baum_meldung_ansprechpartner";
    public String DELETE_TEXT = "Dieser Ansprechpartner kann nicht gelöscht werden, da dieser verwendet wird.";
    public boolean notToDelete = false;

    //~ Methods ----------------------------------------------------------------

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public boolean isMatching(final User user, final MetaObject metaObject) {
        if (metaObject != null) {
            final CidsBean ansprechpartnerBean = metaObject.getBean();
            final Integer ansprechpartner_id = (Integer) ansprechpartnerBean.getProperty(FIELD__ID);
            
            final String queryArtInErsatz = String.format(
                        "SELECT * FROM %s WHERE %s = %d;",
                        TABLE_NAME_SEARCH, FIELD__FK, ansprechpartner_id); 
            try {
                ArrayList<ArrayList>artArrayE = getMetaService().performCustomSearch(queryArtInErsatz, getConnectionContext());
                if (artArrayE.size() < 1) {
                    return false; // kein true sonst läuft jede Klasse durch
                }else {
                    notToDelete = true;
                }
                        
            } catch (RemoteException ex) {
                LOG.error("Cannot delete Ap object", ex);
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
