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
import Sirius.server.middleware.types.MetaObjectNode;
import Sirius.server.newuser.User;
import de.cismet.cids.custom.wunda_blau.search.server.BaumChildLightweightSearch;

import org.apache.log4j.Logger;

import org.openide.util.lookup.ServiceProvider;

import de.cismet.cids.dynamics.CidsBean;
import de.cismet.cids.server.search.SearchException;
import de.cismet.connectioncontext.ConnectionContext;
import de.cismet.connectioncontext.ConnectionContextStore;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * DOCUMENT ME!
 *
 * @author   sandra
 * @version  $Revision$, $Date$
 */
@ServiceProvider(service = CustomDeletionProvider.class)
public class BaumMeldungDeletionProvider extends AbstractCustomDeletionProvider
implements ConnectionContextStore{

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(BaumMeldungDeletionProvider.class);
    public static final String TABLE_NAME = "baum_meldung";
    public static final String FIELD__ID = "id";
    public static final String FIELD__FK = "fk_meldung";
    public static final String TABLE_NAME_SEARCH_S = "baum_schaden";
    public static final String TABLE_NAME_SEARCH_O = "baum_ortstermin";
    public static String DELETE_TEXT = "Dieses Meldung kann nicht gelöscht werden, da diese mindestens ein Unterobjekt hat.";
    public static String DELETE_TEXT_SCHADEN = "Diese Meldung kann nicht gelöscht werden, da diese mindestens einen Schaden hat.";
    public static String DELETE_TEXT_ORT = "Diese Meldung kann nicht gelöscht werden, da diese mindestens einen Ortstermin hat.";
    public static boolean notToDelete = false;

    private User user;
    //private MetaService metaService;
    private ConnectionContext connectionContext = ConnectionContext.createDummy();

        //~ Enum constants -----------------------------------------------------

    
    //~ Methods ----------------------------------------------------------------

    @Override
    public void initWithConnectionContext(final ConnectionContext connectionContext) {
        this.connectionContext = connectionContext;
    }
    @Override
    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }
    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public boolean isMatching(final User user, final MetaObject metaObject) {
        if (metaObject != null) {
            final CidsBean meldungBean = metaObject.getBean();
            final Integer meldung_id = (Integer) meldungBean.getProperty(FIELD__ID);
            
            notToDelete = false;
            DELETE_TEXT = "Dieses Meldung kann nicht gelöscht werden, da diese mindestens ein Unterobjekt hat.";
            if (checkChildObject(FIELD__FK, meldung_id, TABLE_NAME_SEARCH_O, user)) {
                notToDelete = true;
                DELETE_TEXT = DELETE_TEXT_ORT;
            }else {
                if (checkChildObject(FIELD__FK, meldung_id,TABLE_NAME_SEARCH_S, user)) {
                    notToDelete = true;
                    DELETE_TEXT = DELETE_TEXT_SCHADEN;
                }
            }
        }
        return super.isMatching(user, metaObject);// kein true sonst läuft jede Klasse durch
        
    }
    
    public boolean checkChildObject(
                final String fkField,
                final int parentId,
                final String searchTable,
                final User user){
        final String[] childFields = {"id"};
        
        final BaumChildLightweightSearch search = new BaumChildLightweightSearch();
        final Map localServers = new HashMap<>();
        localServers.put("WUNDA_BLAU", getMetaService());
        search.setActiveLocalServers(localServers);
        search.setUser(user);
        search.initWithConnectionContext(connectionContext);
        search.setFkField(fkField);
        search.setParentId(parentId);
        search.setTable(searchTable);
        search.setRepresentationFields(childFields);
        try {
            final Collection<MetaObjectNode> mons = search.performServerSearch();
            if (!mons.isEmpty()){
                return true;//Kinder vorhanden
            }
        } catch (SearchException ex) {
            LOG.error("Cannot delete Meldung object", ex);
        }      
        
        return false;
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
