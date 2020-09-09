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
import org.openide.util.NbBundle;

/**
 * DOCUMENT ME!
 *
 * @author   sandra
 * @version  $Revision$, $Date$
 */
@ServiceProvider(service = CustomDeletionProvider.class)
public class StrAdrStrasseDeletionProvider extends AbstractCustomDeletionProvider {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(StrAdrStrasseDeletionProvider.class);
    public static final String TABLE_NAME = "str_adr_strasse";
    public static final String FIELD__KEY = "strasse";
    private static final String amtlStrGrenze = "04000";
    private static final String DELETE_KLEINER = "Diese Straße darf nicht gelöscht werden, sie muss historisiert werden.";

    //~ Methods ----------------------------------------------------------------

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public boolean isMatching(final User user, final MetaObject metaObject) {
        if (metaObject != null) {
            final CidsBean strBean = metaObject.getBean();

            if (strBean.getProperty(FIELD__KEY).toString().compareTo(amtlStrGrenze) > 0) {
                return false; // kein true sonst läuft jede Klasse durch
            }
        }
        return super.isMatching(user, metaObject);
    }

    @Override
    public void customDeleteMetaObject(final User user, final MetaObject metaObject) throws Exception {
        if (metaObject != null) {
            final CidsBean strBean = metaObject.getBean();
            final String strasse = strBean.getProperty(FIELD__KEY).toString();

            // finde amtliche (historische) Straßen
            
            if (strasse.compareTo(amtlStrGrenze) < 0) {
                throw new DeletionProviderClientException(
                        DELETE_KLEINER);
             
            }
        }
    }
}
