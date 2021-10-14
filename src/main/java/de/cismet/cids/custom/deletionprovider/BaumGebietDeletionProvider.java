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
public class BaumGebietDeletionProvider extends AbstractCustomDeletionProvider
implements ConnectionContextStore{

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(BaumGebietDeletionProvider.class);
    public static final String TABLE_NAME = "baum_gebiet";
    public static final String FIELD__ID = "id";
    public static final String FIELD__FK = "fk_gebiet";
    public static final String TABLE_NAME_SEARCH = "baum_meldung";
    public String DELETE_TEXT = 
            "Dieses Gebiet kann nicht gelöscht werden, da dieses mindestens eine Meldung hat.";
    public boolean notToDelete = false;
    private ConnectionContext connectionContext = ConnectionContext.createDummy();

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
            final CidsBean gebietBean = metaObject.getBean();
            final Integer gebiet_id = (Integer) gebietBean.getProperty(FIELD__ID);
            
            notToDelete = false;
            if (checkChildObject(FIELD__FK, gebiet_id, TABLE_NAME_SEARCH, user)) {
                notToDelete = true;
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
            LOG.error("Cannot delete Gebiet object", ex);
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
