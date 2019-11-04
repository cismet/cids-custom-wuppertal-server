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
package de.cismet.cids.custom.utils.vermessungsunterlagen.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.Collection;

import de.cismet.cids.custom.utils.alkis.AlkisProducts;
import de.cismet.cids.custom.utils.alkis.ServerAlkisProducts;
import de.cismet.cids.custom.utils.vermessungsunterlagen.VermessungsunterlagenHelper;
import de.cismet.cids.custom.utils.vermessungsunterlagen.exceptions.VermessungsunterlagenTaskException;

import de.cismet.cids.dynamics.CidsBean;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class VermUntTaskAPList extends VermUntTaskAP {

    //~ Static fields/initializers ---------------------------------------------

    public static final String TYPE = "AP_List";

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new VermUntTaskAPList object.
     *
     * @param  jobkey       DOCUMENT ME!
     * @param  alkisPoints  DOCUMENT ME!
     */
    public VermUntTaskAPList(final String jobkey, final Collection<CidsBean> alkisPoints) {
        super(TYPE, jobkey, alkisPoints);
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public void performTask() throws VermessungsunterlagenTaskException {
        final String punktListenString = getPunktlistenStringForChosenPoints(getAlkisPoints());
        final String code = ServerAlkisProducts.getInstance().get(AlkisProducts.Type.PUNKTLISTE_TXT);
        final String filename = getPath() + "/"
                    + ServerAlkisProducts.getInstance().get(AlkisProducts.Type.PUNKTLISTE_TXT) + ".plst";
        final File fileToSaveTo = new File(filename);

        if (punktListenString.length() > 3) {
            if ((code != null) && (code.length() > 0)) {
                String url = null;
                try {
                    url = ServerAlkisProducts.getInstance().productListenNachweisUrl(punktListenString, code)
                                .toString();
                } catch (MalformedURLException ex) {
                    final String message = "Beim Generieren der URL kam es zu einem unerwarteten Fehler.";
                    throw new VermessungsunterlagenTaskException(getType(), message, ex);
                }
                if ((url != null) && (url.trim().length() > 0)) {
                    final int parameterPosition = url.indexOf('?');

                    final String parameters;
                    final URL getOrPostUrl;
                    try {
                        if (parameterPosition < 0) {
                            parameters = null;
                            getOrPostUrl = new URL(url);
                        } else {
                            parameters = url.substring(parameterPosition + 1);
                            getOrPostUrl = new URL(url.substring(0, parameterPosition));
                        }
                    } catch (final Exception ex) {
                        final String message = "Beim Erstellen der AP-Listen URL '" + url
                                    + "' kam es zu einem unerwarteten Fehler.";
                        throw new VermessungsunterlagenTaskException(getType(), message, ex);
                    }

                    try {
                        InputStream in = null;
                        OutputStream out = null;
                        try {
                            if ((parameters == null) || (parameters.trim().length() <= 0)) {
                                in = VermessungsunterlagenHelper.doGetRequest(getOrPostUrl);
                            } else {
                                in = VermessungsunterlagenHelper.doPostRequest(
                                        getOrPostUrl,
                                        new StringReader(parameters));
                            }
                            out = new FileOutputStream(fileToSaveTo);
                            VermessungsunterlagenHelper.downloadStream(in, out);
                        } finally {
                            VermessungsunterlagenHelper.closeStream(in);
                            VermessungsunterlagenHelper.closeStream(out);
                        }
                    } catch (final Exception ex) {
                        final String message = "Beim Herunterladen des Produktes unter der URL '" + url
                                    + "' kam es zu einem unerwarteten Fehler.";
                        throw new VermessungsunterlagenTaskException(getType(), message, ex);
                    }
                }
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   alkisPoints  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private static String getPunktlistenStringForChosenPoints(final Collection<CidsBean> alkisPoints) {
        final StringBuffer punktListeString = new StringBuffer();

        for (final CidsBean alkisPoint : alkisPoints) {
            if (punktListeString.length() > 0) {
                punktListeString.append(",");
            }
            punktListeString.append(ServerAlkisProducts.getInstance().getPointDataForProduct(alkisPoint));
        }

        return punktListeString.toString();
    }
}
