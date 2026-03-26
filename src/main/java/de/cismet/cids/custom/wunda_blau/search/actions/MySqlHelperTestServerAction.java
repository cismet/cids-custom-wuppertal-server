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
package de.cismet.cids.custom.wunda_blau.search.actions;

import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.middleware.interfaces.domainserver.MetaServiceStore;
import Sirius.server.newuser.User;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;

import java.io.File;

import java.sql.SQLException;

import de.cismet.cids.custom.utils.AlboProperties;
import de.cismet.cids.custom.utils.formsolutions.FormSolutionsMySqlHelper;

import de.cismet.cids.server.actions.AbstractJumpPostgresToShapefileServerAction;
import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;
import de.cismet.cids.server.actions.UserAwareServerAction;

import de.cismet.connectioncontext.ConnectionContext;
import de.cismet.connectioncontext.ConnectionContextStore;

/**
 * DOCUMENT ME!
 *
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class MySqlHelperTestServerAction implements ServerAction, ConnectionContextStore {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            MySqlHelperTestServerAction.class);
    public static final String TASK_NAME = "mySqlHelperTest";
    private static final String SELECT_QUERY = "SELECT * FROM %s ORDER BY %s;";

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum PARAMETER_TYPE {

        //~ Enum constants -----------------------------------------------------

        TRANSID
    }

    //~ Instance fields --------------------------------------------------------

    private ConnectionContext cc = null;

    //~ Methods ----------------------------------------------------------------

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }

    @Override
    public void initWithConnectionContext(final ConnectionContext cc) {
        this.cc = cc;
    }

    @Override
    public ConnectionContext getConnectionContext() {
        return cc;
    }

//    @Override
//    public void setMetaService(MetaService sevice) {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//    }
//
//    @Override
//    public MetaService getMetaService() {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//    }

    @Override
    public Object execute(final Object body, final ServerActionParameter... params) {
        String transId = "";

        for (final ServerActionParameter sap : params) {
            if (sap.getKey().equals(PARAMETER_TYPE.TRANSID.toString())) {
                transId = (String)sap.getValue();
            }
        }

        try {
            LOG.error("try FormSolutionsMySqlHelper " + transId);
            final boolean res = FormSolutionsMySqlHelper.getInstance().checkConnection(transId);
            LOG.error("FormSolutionsMySqlHelper.getInstance().checkConnection: " + res);
        } catch (Exception e) {
            LOG.error("SQL Exception MySqlHelperTestServerAction", e);
        }

        return transId;
    }
}
