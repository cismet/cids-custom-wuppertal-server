/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cismet.cids.custom.wunda_blau.search.actions;

import Sirius.server.newuser.User;

import com.vividsolutions.jts.geom.GeometryCollection;

import de.cismet.cids.custom.utils.nas.NASProductGenerator;
import de.cismet.cids.custom.utils.nas.NasProduct;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;
import de.cismet.cids.server.actions.UserAwareServerAction;

/**
 * DOCUMENT ME!
 *
 * @author   daniel
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class NasDataQueryAction implements UserAwareServerAction {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            NasDataQueryAction.class);

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum METHOD_TYPE {

        //~ Enum constants -----------------------------------------------------

        ADD, GET, GET_ALL, CANCEL
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum PARAMETER_TYPE {

        //~ Enum constants -----------------------------------------------------

        TEMPLATE, GEOMETRY_COLLECTION, METHOD, ORDER_ID, REQUEST_ID, PROFIL_KENNUNG
    }

    //~ Instance fields --------------------------------------------------------

    private User user;

    //~ Methods ----------------------------------------------------------------

    @Override
    public Object execute(final Object body, final ServerActionParameter... params) {
        NasProduct nasProduct = null;
        GeometryCollection geoms = null;
        METHOD_TYPE method = null;
        String orderId = null;
        String requestId = null;
        String profilKennung = null;
        for (final ServerActionParameter sap : params) {
            if (sap.getKey().equals(PARAMETER_TYPE.TEMPLATE.toString())) {
                nasProduct = (NasProduct)sap.getValue();
            } else if (sap.getKey().equals(PARAMETER_TYPE.GEOMETRY_COLLECTION.toString())) {
                geoms = (GeometryCollection)sap.getValue();
            } else if (sap.getKey().equals(PARAMETER_TYPE.METHOD.toString())) {
                method = (METHOD_TYPE)sap.getValue();
            } else if (sap.getKey().equals(PARAMETER_TYPE.ORDER_ID.toString())) {
                orderId = (String)sap.getValue();
            } else if (sap.getKey().equals(PARAMETER_TYPE.REQUEST_ID.toString())) {
                requestId = (String)sap.getValue();
            } else if (sap.getKey().equals(PARAMETER_TYPE.PROFIL_KENNUNG.toString())) {
                profilKennung = (String)sap.getValue();
            }
        }
        if (method == METHOD_TYPE.ADD) {
            return NASProductGenerator.instance().executeAsynchQuery(nasProduct, geoms, user, requestId, profilKennung);
        } else if (method == METHOD_TYPE.GET) {
            if (orderId == null) {
                LOG.error("missing order id for get request");
                return null;
            }
            return NASProductGenerator.instance().getResultForOrder(orderId, user);
        } else if (method == METHOD_TYPE.GET_ALL) {
            return NASProductGenerator.instance().getUndeliveredOrders(user);
        } else if (method == METHOD_TYPE.CANCEL) {
            NASProductGenerator.instance().cancelOrder(orderId, user);
        }

        return null;
    }

    @Override
    public String getTaskName() {
        return "nasDataQuery";
    }

    @Override
    public User getUser() {
        return user;
    }

    @Override
    public void setUser(final User user) {
        this.user = user;
    }
}
