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

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class MotdRetriever {

    //~ Static fields/initializers ---------------------------------------------

    private static final String MOTD_URL = "http://wunda.wuppertal-intra.de/popupmeld_w.asp";
    private static final String NO_MESSAGE = "Es liegen keine aktuellen Meldungen für WuNDa vor!";
    private static final int SCHEDULE_INTERVAL = 5000;
    private static final transient org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(MotdRetriever.class);

    private static MotdRetriever INSTANCE;

    //~ Instance fields --------------------------------------------------------

    private final SimpleHttpAccessHandler httpHandler = new SimpleHttpAccessHandler();
    private final Collection<MotdRetrieverListener> listeners = new ArrayList<MotdRetrieverListener>();
    private final MotdRetrieverListenerHandler listenerHandler = new MotdRetrieverListenerHandler();
    private final Timer timer = new Timer();
    private String motd = null;
    private String totd = null;
    private boolean running;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new MotdRetriever object.
     */
    private MotdRetriever() {
        //
    }

    //~ Methods ----------------------------------------------------------------

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
     */
    public void start() {
        synchronized (timer) {
            if (!running) {
                startTimer(motd, SCHEDULE_INTERVAL);
            }
        }
    }

    /**
     * DOCUMENT ME!
     */
    private void stop() {
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
            return "Test-Title of the day"; // TODO extract title from motd
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
                inputStream = getHttpAccessHandler().doRequest(new URL(MOTD_URL),
                        new StringReader(""),
                        AccessHandler.ACCESS_METHODS.GET_REQUEST);
                return IOUtils.toString(inputStream, "ISO-8859-1");
            } catch (final Exception ex) {
                LOG.error("couldnt get the MOTD from " + MOTD_URL, ex);
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

                if (newMotd.equals(NO_MESSAGE)) {
                    setMotd(null);
                    setTotd(null);
                } else {
                    setMotd(newMotd);
                    setTotd(extractTitle(motd));
                }
            } catch (final Exception ex) {
                LOG.fatal(ex, ex);
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
