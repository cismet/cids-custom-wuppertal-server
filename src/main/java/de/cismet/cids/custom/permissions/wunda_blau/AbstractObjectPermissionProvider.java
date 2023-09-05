/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cismet.cids.custom.permissions.wunda_blau;

import Sirius.server.middleware.interfaces.domainserver.DomainServerCallServerService;
import Sirius.server.middleware.types.MetaObject;
import Sirius.server.newuser.User;
import Sirius.server.newuser.UserGroup;

import java.sql.Timestamp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import de.cismet.cids.dynamics.AbstractCustomBeanPermissionProvider;
import de.cismet.cids.dynamics.CidsBean;

import de.cismet.connectioncontext.ConnectionContext;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public abstract class AbstractObjectPermissionProvider extends AbstractCustomBeanPermissionProvider {

    //~ Static fields/initializers ---------------------------------------------

    private static final String PROPERTY__READ = "read";
    private static final String PROPERTY__WRITE = "write";

    private static final String QUERY_TEMPLATE = ""
                + "SELECT 'cs_objectpermissions', id "
                + "FROM cs_objectpermissions "
                + "WHERE class_key ILIKE '%1$s' "
                + "AND (object_id IS NULL OR object_id = %2$d) "
                + "AND (user_name LIKE %3$s OR group_name = ANY(VALUES %4$s)) "
                + "AND (ts_start IS NULL OR ts_start < %5$s) "
                + "AND (ts_end IS NULL OR ts_end > %5$s);";

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    private enum PermissionType {

        //~ Enum constants -----------------------------------------------------

        READ, WRITE
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public boolean getCustomReadPermissionDecisionforUser(final User user, final ConnectionContext connectionContext) {
        final CidsBean cidsBean = getCidsBean();
        if ((cidsBean == null) || (user == null)) {
            return false;
        } else {
            final Timestamp now = new Timestamp(new Date().getTime());
            final Collection<CidsBean> permissionBeans = getObjectPermissionBeans(
                    user,
                    cidsBean,
                    now,
                    connectionContext);
            if (permissionBeans != null) {
                for (final CidsBean permissionBean : permissionBeans) {
                    if (checkPermission(permissionBean, user, PermissionType.READ)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    @Override
    public boolean getCustomWritePermissionDecisionforUser(final User user, final ConnectionContext connectionContext) {
        final CidsBean cidsBean = getCidsBean();
        if ((cidsBean == null) || (user == null)) {
            return false;
        } else {
            final Timestamp now = new Timestamp(new Date().getTime());
            final Collection<CidsBean> permissionBeans = getObjectPermissionBeans(
                    user,
                    cidsBean,
                    now,
                    connectionContext);
            if (permissionBeans != null) {
                for (final CidsBean permissionBean : permissionBeans) {
                    if (checkPermission(permissionBean, user, PermissionType.WRITE)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   user               DOCUMENT ME!
     * @param   cidsBean           DOCUMENT ME!
     * @param   now                DOCUMENT ME!
     * @param   connectionContext  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static Collection<CidsBean> getObjectPermissionBeans(final User user,
            final CidsBean cidsBean,
            final Timestamp now,
            final ConnectionContext connectionContext) {
        if ((user == null) || (cidsBean == null)) {
            return null;
        }
        final String classKey = cidsBean.getMetaObject().getMetaClass().getTableName();
        final int objectId = cidsBean.getMetaObject().getId();
        final String userName = user.getName();
        final Collection<String> groupNames = new ArrayList<>();
        for (final UserGroup group : user.getPotentialUserGroups()) {
            if (group != null) {
                groupNames.add(String.format("('%s')", group.getName()));
            }
        }
        final String query = String.format(
                QUERY_TEMPLATE,
                classKey,
                objectId,
                String.format("'%s'", userName),
                String.join(", ", groupNames),
                String.format("'%s'", now.toString()));
        try {
            final Collection<CidsBean> permissionBeans = new ArrayList<>();
            for (final MetaObject metaObject
                        : DomainServerCallServerService.getCallServerServiceInstance().getMetaObject(
                            user,
                            query,
                            connectionContext)) {
                permissionBeans.add(metaObject.getBean());
            }
            return permissionBeans;
        } catch (final Exception ex) {
            return null;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   permissionBean  DOCUMENT ME!
     * @param   user            DOCUMENT ME!
     * @param   permissionType  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static boolean checkPermission(final CidsBean permissionBean,
            final User user,
            final PermissionType permissionType) {
        if ((permissionBean != null) && (user != null)) {
            final Boolean permission = (Boolean)permissionBean.getProperty(PermissionType.READ.equals(permissionType)
                        ? PROPERTY__READ : PROPERTY__WRITE);
            if (Boolean.TRUE.equals(permission)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean getCustomWritePermissionDecisionforUser(final User u) {
        return getCustomWritePermissionDecisionforUser(u, ConnectionContext.createDeprecated());
    }

    @Override
    public boolean getCustomReadPermissionDecisionforUser(final User u) {
        return getCustomReadPermissionDecisionforUser(u, ConnectionContext.createDeprecated());
    }
}
