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

import de.aedsicad.aaaweb.service.alkis.info.ALKISInfoServices;
import de.aedsicad.aaaweb.service.util.Buchungsblatt;
import de.aedsicad.aaaweb.service.util.Point;

import java.rmi.RemoteException;

import de.cismet.cids.custom.utils.alkis.SOAPAccessProvider;
import de.cismet.cids.custom.utils.alkis.ServerAlkisConf;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;

/**
 * DOCUMENT ME!
 *
 * @author   thorsten
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class ServerAlkisSoapAction implements ServerAction {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            ServerAlkisSoapAction.class);

    public static final String TASKNAME = "alkisSoapTunnelAction";

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum RETURN_VALUE {

        //~ Enum constants -----------------------------------------------------

        POINT, BUCHUNGSBLATT
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public Object execute(final Object body, final ServerActionParameter... params) {
        if (!(body instanceof RETURN_VALUE)) {
            throw new IllegalArgumentException("Body has to be either POINT or BUCHUNGSBLATT");
        }

        try {
            final String aToken = getSOAPAccessProvider().login();
            if (body.toString().equals(RETURN_VALUE.POINT.toString())) {
                // POINT
                try {
                    final String pointCode = params[0].getValue().toString();
                    final Point point = getALKISInfoServices().getPoint(aToken, getSOAPAccessProvider().getService(), pointCode);
                    return point;
                } catch (RemoteException remoteException) {
                    LOG.error("Error in ServerAlkisSoapAction", remoteException);
                    throw new RuntimeException("Error in ServerAlkisSoapAction", remoteException);
                }
            } else {
                // BUCHUNGSBLATT
                try {
                    final String buchungsblattCode = params[0].getValue().toString();
                    final String[] uuids = getALKISInfoServices().translateBuchungsblattCodeIntoUUIds(
                            aToken,
                            getSOAPAccessProvider().getService(),
                            buchungsblattCode);
                    final Buchungsblatt[] buchungsblaetter = getALKISInfoServices().getBuchungsblaetter(
                            aToken,
                            getSOAPAccessProvider().getService(),
                            uuids,
                            true);
                    return buchungsblaetter[0];
                } catch (RemoteException remoteException) {
                    LOG.error("Error in ServerAlkisSoapAction", remoteException);
                    throw new RuntimeException("Error in ServerAlkisSoapAction", remoteException);
                }
            }
        } finally {
            getSOAPAccessProvider().logout();                    
        }
    }

    @Override
    public String getTaskName() {
        return TASKNAME;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static SOAPAccessProvider getSOAPAccessProvider() {
        return LazyInitialiser.INSTANCE;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static ALKISInfoServices getALKISInfoServices() {
        return getSOAPAccessProvider().getAlkisInfoService();
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    private static final class LazyInitialiser {

        //~ Static fields/initializers -----------------------------------------

        private static final SOAPAccessProvider INSTANCE = new SOAPAccessProvider(ServerAlkisConf.getInstance());

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new LazyInitialiser object.
         */
        private LazyInitialiser() {
        }
    }
}
