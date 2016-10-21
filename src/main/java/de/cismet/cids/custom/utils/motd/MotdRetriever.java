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
package de.cismet.cids.custom.utils.motd;

import org.apache.commons.io.IOUtils;

import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

import java.net.URL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import de.cismet.cids.custom.utils.WundaBlauServerResources;

import de.cismet.cids.utils.serverresources.ServerResourcesLoader;

import de.cismet.commons.security.AccessHandler;
import de.cismet.commons.security.handler.SimpleHttpAccessHandler;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class MotdRetriever {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(MotdRetriever.class);

    private static MotdRetriever INSTANCE;

    //~ Instance fields --------------------------------------------------------

    private final SimpleHttpAccessHandler httpHandler = new SimpleHttpAccessHandler();
    private final Collection<MotdRetrieverListener> listeners = new ArrayList<MotdRetrieverListener>();
    private final MotdRetrieverListenerHandler listenerHandler = new MotdRetrieverListenerHandler();
    private final Timer timer = new Timer();
    private String domain;
    private String motd = null;
    private String motd_extern = null;
    private String totd = null;
    private String totd_extern = null;
    private boolean running;
    private String motd_url;
    private String motd_extern_url;
    private Integer retrieveRate;
    private String noMessage;
    private Map<String, Boolean> retrieveSuccessfulMap = new HashMap<String, Boolean>();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new MotdRetriever object.
     */
    private MotdRetriever() {
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param   domain  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  Exception                 java.lang.Exception
     * @throws  IllegalArgumentException  DOCUMENT ME!
     * @throws  IllegalStateException     DOCUMENT ME!
     */
    public boolean init(final String domain) throws Exception {
        if (domain == null) {
            throw new IllegalArgumentException("Domain darf nicht null sein !");
        }
        if (this.domain != null) {
            throw new IllegalStateException("MotdRetriever wurde bereits initialisiert !");
        }
        try {
            final Properties serviceProperties;
            if (domain.equalsIgnoreCase("wunda_blau")) {
                serviceProperties = ServerResourcesLoader.getInstance()
                            .loadPropertiesResource(WundaBlauServerResources.MOTD_WUNDA_BLAU_PROPERTIES.getValue());
            } else if (domain.equalsIgnoreCase("verdis_grundis")) {
                serviceProperties = ServerResourcesLoader.getInstance()
                            .loadPropertiesResource(WundaBlauServerResources.MOTD_VERDIS_GRUNDIS_PROPERTIES.getValue());
            } else if (domain.equalsIgnoreCase("lagis")) {
                serviceProperties = ServerResourcesLoader.getInstance()
                            .loadPropertiesResource(WundaBlauServerResources.MOTD_LAGIS_PROPERTIES.getValue());
            } else if (domain.equalsIgnoreCase("belis2")) {
                serviceProperties = ServerResourcesLoader.getInstance()
                            .loadPropertiesResource(WundaBlauServerResources.MOTD_BELIS2_PROPERTIES.getValue());
            } else {
                return false;
            }

            motd_url = serviceProperties.getProperty("MOTD_URL");
            motd_extern_url = serviceProperties.getProperty("MOTD_EXTERN_URL");
            retrieveRate = Integer.parseInt(serviceProperties.getProperty("RETRIEVE_RATE_IN_MS"));
            noMessage = serviceProperties.getProperty("NO_MESSAGE");

            this.domain = domain;
            return true;
        } catch (final Exception ex) {
            throw new Exception(
                "Fehler beim Initialisieren des MotdRetrievers. Es werden keine aktuellen Meldungen verteilt !",
                ex);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static MotdRetriever getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new MotdRetriever();
        }
        return INSTANCE;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private SimpleHttpAccessHandler getHttpAccessHandler() {
        return httpHandler;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   extern  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getTotd(final boolean extern) {
        return totd;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  newTotd  DOCUMENT ME!
     * @param  extern   DOCUMENT ME!
     */
    private void setTotd(final String newTotd, final boolean extern) {
        final String old = extern ? this.totd_extern : this.totd;
        final boolean changed;
        if (newTotd != null) {
            changed = !newTotd.equals(old);
        } else {
            changed = old != null;
        }

        if (changed) {
            if (extern) {
                this.totd_extern = newTotd;
            } else {
                this.totd = newTotd;
            }

            listenerHandler.totdChanged(new MotdRetrieverListenerEvent(
                    MotdRetrieverListenerEvent.TYPE_TOTD_CHANGED,
                    newTotd,
                    extern,
                    this));
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   extern  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getMotd(final boolean extern) {
        return extern ? motd_extern : motd;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  newMotd  DOCUMENT ME!
     * @param  extern   DOCUMENT ME!
     */
    private void setMotd(final String newMotd, final boolean extern) {
        final String old = extern ? this.motd_extern : this.motd;
        final boolean changed;
        if (newMotd != null) {
            changed = !newMotd.equals(old);
        } else {
            changed = old != null;
        }

        if (changed) {
            if (extern) {
                this.motd_extern = newMotd;
            } else {
                this.motd = newMotd;
            }

            listenerHandler.motdChanged(new MotdRetrieverListenerEvent(
                    MotdRetrieverListenerEvent.TYPE_MOTD_CHANGED,
                    newMotd,
                    extern,
                    this));
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @throws  IllegalStateException  DOCUMENT ME!
     */
    public void start() {
        if (domain == null) {
            throw new IllegalStateException("MotdRetriever wurde nicht initialisiert !");
        }

        synchronized (timer) {
            if (!running) {
                startTimer(retrieveRate);
            }
        }
    }

    /**
     * DOCUMENT ME!
     */
    public void stop() {
        synchronized (timer) {
            if (running) {
                timer.cancel();
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param  scheduleMs  DOCUMENT ME!
     */
    private void startTimer(final int scheduleMs) {
        running = true;
        synchronized (timer) {
            timer.schedule(new RetrieveTimerTask(scheduleMs), scheduleMs);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   listener  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public boolean addMotdRetrieverListener(final MotdRetrieverListener listener) {
        return listeners.add(listener);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   listener  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public boolean removeMotdRetrieverListener(final MotdRetrieverListener listener) {
        return listeners.remove(listener);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   motd  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String extractTitle(final String motd) {
        if (motd == null) {
            return null;
        } else {
            final Elements elements = Jsoup.parse(motd).select("span.totd");
            if ((elements != null) && !elements.isEmpty() && (elements.get(0) != null)) {
                return elements.get(0).text();
            } else {
                return null;
            }
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    class RetrieveTimerTask extends TimerTask {

        //~ Instance fields ----------------------------------------------------

        private final int intervall;

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new RetrieveTimerTask object.
         *
         * @param  intervall  DOCUMENT ME!
         */
        public RetrieveTimerTask(final int intervall) {
            this.intervall = intervall;
        }

        //~ Methods ------------------------------------------------------------

        /**
         * DOCUMENT ME!
         *
         * @param   motd_url  DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         */
        private String retrieveMotd(final String motd_url) {
            InputStream inputStream = null;
            try {
                inputStream = getHttpAccessHandler().doRequest(new URL(motd_url),
                        new StringReader(""),
                        AccessHandler.ACCESS_METHODS.GET_REQUEST);
                final String motd = IOUtils.toString(inputStream, "ISO-8859-1");
                retrieveSuccessfulMap.put(motd_url, true);
                return motd;
            } catch (final Exception ex) {
                if (!retrieveSuccessfulMap.containsKey(motd_url)
                            || Boolean.TRUE.equals(retrieveSuccessfulMap.get(motd_url))) {
                    LOG.warn("couldnt get the MOTD from " + motd_url, ex);
                } else {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("couldnt get the MOTD from " + motd_url, ex);
                    }
                }
                retrieveSuccessfulMap.put(motd_url, false);
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException ex) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("couldnt close the inputstream", ex);
                        }
                    }
                }
            }
            return null;
        }

        /**
         * DOCUMENT ME!
         *
         * @param  extern  DOCUMENT ME!
         */
        private void processMotd(final boolean extern) {
            final String newMotd = retrieveMotd(extern ? motd_extern_url : motd_url);
            if (newMotd != null) {
                if (newMotd.equals(noMessage)) {
                    setMotd(null, extern);
                    setTotd(null, extern);
                } else {
                    setMotd(newMotd, extern);
                    setTotd(extractTitle(newMotd), extern);
                }
            }
        }

        @Override
        public void run() {
            try {
                processMotd(false); // intern
                processMotd(true);  // extern
            } catch (final Exception ex) {
                LOG.warn("couldnt retrieve motd", ex);
            } finally {
                synchronized (timer) {
                    startTimer(intervall);
                }
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    class MotdRetrieverListenerHandler implements MotdRetrieverListener {

        //~ Methods ------------------------------------------------------------

        @Override
        public void motdChanged(final MotdRetrieverListenerEvent event) {
            for (final MotdRetrieverListener listener : listeners) {
                listener.motdChanged(event);
            }
        }

        @Override
        public void totdChanged(final MotdRetrieverListenerEvent event) {
            for (final MotdRetrieverListener listener : listeners) {
                listener.totdChanged(event);
            }
        }
    }
}
