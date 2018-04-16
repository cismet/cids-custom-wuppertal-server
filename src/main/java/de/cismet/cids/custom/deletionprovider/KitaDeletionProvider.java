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
import Sirius.server.middleware.types.MetaObject;
import Sirius.server.newuser.User;

import org.apache.log4j.Logger;

import org.openide.util.lookup.ServiceProvider;

import java.sql.Timestamp;

import java.util.Collection;
import java.util.Date;

import de.cismet.cids.dynamics.CidsBean;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
@ServiceProvider(service = CustomDeletionProvider.class)
public class KitaDeletionProvider extends AbstractCustomDeletionProvider {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(KitaDeletionProvider.class);
    public static final String TABLE_NAME = "infra_kita";
    public static final String FIELD__VERSION_KITA = "version_kita";
    public static final String FIELD__VERSIONNR = "versionnr";
    public static final String FIELD__ONLINE_STELLEN = "online_stellen";
    public static final String FIELD__ENDLIFESPANVERSION = "endlifespanversion";

    //~ Methods ----------------------------------------------------------------

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public boolean isMatching(final User user, final MetaObject metaObject) {
        if (metaObject != null) {
            final CidsBean kitaBean = metaObject.getBean();

            if ((kitaBean.getProperty(FIELD__ONLINE_STELLEN) == null)
                        || Boolean.FALSE.equals(kitaBean.getProperty(FIELD__ONLINE_STELLEN))) {
                return false; // kein true sonst läuft jede Klasse durch
            }
        }
        return super.isMatching(user, metaObject);
    }

    @Override
    public void customDeleteMetaObject(final User user, final MetaObject metaObject) throws Exception {
        if (metaObject != null) {
            final CidsBean kitaBean = metaObject.getBean();

            // finde aktuelle Version (höchste Versions-Nummer)
            CidsBean hoechsteVersionBean = null;
            final Collection<CidsBean> versionBeans = kitaBean.getBeanCollectionProperty(FIELD__VERSION_KITA);
            if (versionBeans != null) {
                for (final CidsBean versionBean : kitaBean.getBeanCollectionProperty(FIELD__VERSION_KITA)) {
                    final int hoechsteVersionNr =
                        ((hoechsteVersionBean != null) && (hoechsteVersionBean.getProperty(FIELD__VERSIONNR) != null))
                        ? (Integer)hoechsteVersionBean.getProperty(FIELD__VERSIONNR) : Integer.MIN_VALUE;
                    final int versionNr = ((versionBean != null) && (versionBean.getProperty(FIELD__VERSIONNR) != null))
                        ? (Integer)versionBean.getProperty(FIELD__VERSIONNR) : Integer.MIN_VALUE;
                    if (versionNr > hoechsteVersionNr) {
                        hoechsteVersionBean = versionBean;
                    }
                }
            }

            // Löschen = online_stellen auf false setzen und endlifespanversion der aktuellen Version auf jetzt setzen
            if (hoechsteVersionBean != null) {
                final Timestamp timestamp = new Timestamp(new Date().getTime());
                try {
                    hoechsteVersionBean.setProperty(FIELD__ENDLIFESPANVERSION, timestamp);
                    kitaBean.setProperty(FIELD__ONLINE_STELLEN, false);
                    getMetaService().updateMetaObject(user, metaObject, getConnectionContext());
                } catch (Exception ex) {
                    LOG.error("could not custom-delete kita: " + metaObject.getDebugString(), ex);
                }
            } else {
                throw new Exception("Aktuelle Version der Kita konnte nicht ermittelt werden.\n"
                            + metaObject.getDebugString());
            }
        }
    }
}
