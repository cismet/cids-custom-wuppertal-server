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

import de.aedsicad.aaaweb.rest.client.ApiException;
import de.aedsicad.aaaweb.rest.model.Buchungsblatt;
import de.aedsicad.aaaweb.rest.model.Point;

import java.util.Arrays;
import java.util.List;

import de.cismet.cids.custom.utils.alkis.AlkisAccessProvider;
import de.cismet.cids.custom.utils.alkis.AlkisRestConf;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;

/**
 * DOCUMENT ME!
 *
 * @author   thorsten
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class AlkisRestAction implements ServerAction {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            AlkisRestAction.class);

    public static final String TASKNAME = "alkisRestTunnelAction";

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
            final String token = getAlkisAccessProvider().login();
            final String configuration = getAlkisAccessProvider().getAlkisRestConf().getConfiguration();
            if (body.toString().equals(RETURN_VALUE.POINT.toString())) {
                // POINT
                try {
                    final String pointCode = params[0].getValue().toString();
                    final Point point = getAlkisAccessProvider().getAlkisInfoService()
                                .getPoint(
                                    token,
                                    configuration,
                                    pointCode);
                    return point;
                } catch (final ApiException remoteException) {
                    LOG.error("Error in ServerAlkisRestAction", remoteException);
                    throw new RuntimeException("Error in ServerAlkisRestAction", remoteException);
                }
            } else {
                // BUCHUNGSBLATT
                try {
                    final List<String> buchungsblattCode = Arrays.asList(fixBuchungslattCode(
                                params[0].getValue().toString()));
                    final boolean aWithLandParcels = true;
                    final String operationName = null;
                    final String operationArgument = null;
                    final String orderNumberInfo = null; // TODO ???
                    final List<Buchungsblatt> buchungsblaetter = getAlkisAccessProvider().getAlkisInfoService()
                                .getBuchungsblaetter(
                                    buchungsblattCode,
                                    token,
                                    configuration,
                                    aWithLandParcels,
                                    operationName,
                                    operationArgument,
                                    orderNumberInfo);
                    return buchungsblaetter.iterator().next();
                } catch (final ApiException remoteException) {
                    LOG.error("Error in ServerAlkisRestAction", remoteException);
                    throw new RuntimeException("Error in ServerAlkisRestAction", remoteException);
                }
            }
        } finally {
            getAlkisAccessProvider().logout();
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   buchungsblattCode  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static String fixBuchungslattCode(final String buchungsblattCode) {
        if (buchungsblattCode != null) {
            final StringBuffer buchungsblattCodeSB = new StringBuffer(buchungsblattCode);
            // Fix SICAD-API-strangeness...
            while (buchungsblattCodeSB.length() < 14) {
                buchungsblattCodeSB.append(" ");
            }
            return buchungsblattCodeSB.toString();
        } else {
            return "";
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
    private static AlkisAccessProvider getAlkisAccessProvider() {
        return LazyInitialiser.ALKIS_ACCESS_PROVIDER;
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    private static final class LazyInitialiser {

        //~ Static fields/initializers -----------------------------------------

        private static final AlkisAccessProvider ALKIS_ACCESS_PROVIDER = new AlkisAccessProvider(AlkisRestConf
                        .getInstance());

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new LazyInitialiser object.
         */
        private LazyInitialiser() {
        }
    }
}
