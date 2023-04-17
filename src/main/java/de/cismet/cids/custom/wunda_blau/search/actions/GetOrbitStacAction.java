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

import Sirius.server.newuser.User;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.socket.client.IO;
import io.socket.client.Socket;

import org.apache.log4j.Logger;

import java.util.Properties;

import de.cismet.cids.custom.utils.WundaBlauServerResources;
import de.cismet.cids.custom.wunda_blau.search.actions.orbit.OrbitStacTools;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;
import de.cismet.cids.server.actions.UserAwareServerAction;

import de.cismet.cids.utils.serverresources.ServerResourcesLoader;

/**
 * DOCUMENT ME!
 *
 * @author   hell
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class GetOrbitStacAction implements ServerAction, UserAwareServerAction {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(StamperServerAction.class);

    public static final String TASK_NAME = "getOrbitStac";

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum PARAMETER_TYPE {

        //~ Enum constants -----------------------------------------------------

        IP, STAC_OPTIONS
    }

    //~ Instance fields --------------------------------------------------------

    private User user;

    //~ Methods ----------------------------------------------------------------

    @Override
    public Object execute(final Object body, final ServerActionParameter... saps) {
        String ipAddress = null;
        String stacOptions = null;
        final ObjectMapper mapper = new ObjectMapper();
        final String stac = OrbitStacTools.getInstance().createStac(user.getName(), ipAddress, stacOptions);

        try {
            final Properties settings = ServerResourcesLoader.getInstance()
                        .loadProperties(WundaBlauServerResources.ORBIT_SETTINGS_PROPERTIES.getValue());
            final String OPEN_CHANNELS_SECRET = settings.getProperty("openChannelsSecret");
            final String SOCKET_BROADCASTER = settings.getProperty("socketBroadcaster");
            final int OPEN_CHANNEL_TIMEOUT = Integer.parseInt(settings.getProperty("openChannelTimeout"));

            for (final ServerActionParameter sap : saps) {
                if (sap.getKey().equals(PARAMETER_TYPE.IP.toString())) {
                    ipAddress = (String)sap.getValue();
                } else if (sap.getKey().equals(PARAMETER_TYPE.STAC_OPTIONS.toString())) {
                    stacOptions = (String)sap.getValue();
                }
            }

            final String socketChannelId = OrbitStacTools.getInstance().getEntry(stac).getSocketChannelId();

            openChannels(socketChannelId, OPEN_CHANNELS_SECRET, OPEN_CHANNEL_TIMEOUT);
            return "{\"stac\":\"" + stac + "\",\"socketChannelId\":\"" + socketChannelId + "\"}";
        } catch (Exception e) {
            // no Connection to the broadcaster possible don't return a socketchanellid
            LOG.error("Error during GetOrbitStacAction. Will send STAC only. (No Socket-Functionality available.)", e);
            return "{\"stac\":\"" + stac + "\"}";
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   socketChannelId       DOCUMENT ME!
     * @param   OPEN_CHANNELS_SECRET  DOCUMENT ME!
     * @param   OPEN_CHANNEL_TIMEOUT  DOCUMENT ME!
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private static void openChannels(final String socketChannelId,
            final String OPEN_CHANNELS_SECRET,
            final int OPEN_CHANNEL_TIMEOUT) throws Exception {
        final ObjectMapper mapper = new ObjectMapper();

        final OpenChannelInfo info = new OpenChannelInfo();
        final String[] channels = new String[] { "toOrbit:" + socketChannelId, "fromOrbit:" + socketChannelId };
        info.setSecret(OPEN_CHANNELS_SECRET);
        info.setChannels(channels);
        info.setTimeoutS(OPEN_CHANNEL_TIMEOUT);
        LOG.debug("try to open channels for " + socketChannelId);
        final Socket socket = SocketIOSocketProvider.getInstance().getSocket();
        socket.emit("open", mapper.writeValueAsString(info));
    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
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

/**
 * DOCUMENT ME!
 *
 * @version  $Revision$, $Date$
 */
class OpenChannelInfo {

    //~ Instance fields --------------------------------------------------------

    String secret;
    String[] channels;
    int timeoutS = 10;

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getSecret() {
        return secret;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  secret  DOCUMENT ME!
     */
    public void setSecret(final String secret) {
        this.secret = secret;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String[] getChannels() {
        return channels;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  channels  DOCUMENT ME!
     */
    public void setChannels(final String[] channels) {
        this.channels = channels;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public int getTimeoutS() {
        return timeoutS;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  timeoutS  DOCUMENT ME!
     */
    public void setTimeoutS(final int timeoutS) {
        this.timeoutS = timeoutS;
    }
}

/**
 * DOCUMENT ME!
 *
 * @version  $Revision$, $Date$
 */
class SocketIOSocketProvider {

    //~ Instance fields --------------------------------------------------------

    Socket socket;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new SocketIOSocketProvider object.
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private SocketIOSocketProvider() throws Exception {
        final IO.Options opts = new IO.Options();
        opts.transports = new String[] { "websocket" };
        final Properties settings = ServerResourcesLoader.getInstance()
                    .loadProperties(WundaBlauServerResources.ORBIT_SETTINGS_PROPERTIES.getValue());
        final String SOCKET_BROADCASTER = settings.getProperty("socketBroadcaster");
        socket = IO.socket(SOCKET_BROADCASTER).connect();
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static SocketIOSocketProvider getInstance() {
        return LazyInitializer.INSTANCE;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Socket getSocket() {
        return socket;
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    private static final class LazyInitializer {

        //~ Static fields/initializers -----------------------------------------

        private static final SocketIOSocketProvider INSTANCE;

        static {
            try {
                INSTANCE = new SocketIOSocketProvider();
            } catch (final Exception ex) {
                throw new RuntimeException("Exception while initializing ServerStamperUtils", ex);
            }
        }
    }
}
