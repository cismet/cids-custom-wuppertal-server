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
import java.util.Timer;
import java.util.TimerTask;

import de.cismet.commons.security.AccessHandler;
import de.cismet.commons.security.handler.SimpleHttpAccessHandler;

import de.cismet.tools.PropertyReader;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class MotdRetriever {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(MotdRetriever.class);
    private static final String PROPERTIES_PATH = "/de/cismet/cids/custom/motd/";

    private static MotdRetriever INSTANCE;

    //~ Instance fields --------------------------------------------------------

    private final SimpleHttpAccessHandler httpHandler = new SimpleHttpAccessHandler();
    private final Collection<MotdRetrieverListener> listeners = new ArrayList<MotdRetrieverListener>();
    private final MotdRetrieverListenerHandler listenerHandler = new MotdRetrieverListenerHandler();
    private final Timer timer = new Timer();
    private String domain;
    private String motd = null;
    private String totd = null;
    private boolean running;
    private String motd_url;
    private Integer retrieveRate;
    private String noMessage;

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
     * @throws  Exception                 java.lang.Exception
     * @throws  IllegalArgumentException  DOCUMENT ME!
     * @throws  IllegalStateException     DOCUMENT ME!
     */
    public void init(final String domain) throws Exception {
        if (domain == null) {
            throw new IllegalArgumentException("Domain darf nicht null sein !");
        }
        if (this.domain != null) {
            throw new IllegalStateException("MotdRetriever wurde bereits initialisiert !");
        }
        try {
            final PropertyReader serviceProperties = new PropertyReader(PROPERTIES_PATH + domain.toLowerCase()
                            + ".properties");

            motd_url = serviceProperties.getProperty("MOTD_URL");
            retrieveRate = Integer.parseInt(serviceProperties.getProperty("RETRIEVE_RATE_IN_MS"));
            noMessage = serviceProperties.getProperty("NO_MESSAGE");

            this.domain = domain;
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
     * @return  DOCUMENT ME!
     */
    public String getTotd() {
        return totd;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  totd  DOCUMENT ME!
     */
    private void setTotd(final String totd) {
        final String old = this.totd;
        final boolean changed;
        if (totd != null) {
            changed = !totd.equals(old);
        } else {
            changed = old != null;
        }

        if (changed) {
            this.totd = totd;

            listenerHandler.totdChanged(new MotdRetrieverListenerEvent(
                    MotdRetrieverListenerEvent.TYPE_TOTD_CHANGED,
                    totd,
                    this));
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getMotd() {
        return motd;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  newMotd  DOCUMENT ME!
     */
    private void setMotd(final String newMotd) {
        final String old = this.motd;
        final boolean changed;
        if (newMotd != null) {
            changed = !newMotd.equals(old);
        } else {
            changed = old != null;
        }

        if (changed) {
            this.motd = newMotd;

            listenerHandler.motdChanged(new MotdRetrieverListenerEvent(
                    MotdRetrieverListenerEvent.TYPE_MOTD_CHANGED,
                    newMotd,
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
                startTimer(motd, retrieveRate);
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
     * @param  motd        DOCUMENT ME!
     * @param  scheduleMs  DOCUMENT ME!
     */
    private void startTimer(final String motd, final int scheduleMs) {
        running = true;
        synchronized (timer) {
            timer.schedule(new RetrieveTimerTask(motd, scheduleMs), scheduleMs);
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

        private final String motd;
        private final int intervall;

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new RetrieveTimerTask object.
         *
         * @param  motd       DOCUMENT ME!
         * @param  intervall  DOCUMENT ME!
         */
        public RetrieveTimerTask(final String motd, final int intervall) {
            this.motd = motd;
            this.intervall = intervall;
        }

        //~ Methods ------------------------------------------------------------

        /**
         * DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         */
        private String retrieveMotd() {
            InputStream inputStream = null;
            try {
                inputStream = getHttpAccessHandler().doRequest(new URL(motd_url),
                        new StringReader(""),
                        AccessHandler.ACCESS_METHODS.GET_REQUEST);
                return IOUtils.toString(inputStream, "ISO-8859-1");
            } catch (final Exception ex) {
                LOG.info("couldnt get the MOTD from " + motd_url, ex);
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException ex) {
                        LOG.warn("couldnt close the inputstream", ex);
                    }
                }
            }
            return null;
        }

        @Override
        public void run() {
            try {
                final String newMotd = retrieveMotd();
                if (newMotd != null) {
                    if (newMotd.equals(noMessage)) {
                        setMotd(null);
                        setTotd(null);
                    } else {
                        setMotd(newMotd);
                        setTotd(extractTitle(newMotd));
                    }
                }
            } catch (final Exception ex) {
                LOG.warn("couldnt retrieve motd", ex);
            } finally {
                synchronized (timer) {
                    startTimer(getMotd(), intervall);
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
