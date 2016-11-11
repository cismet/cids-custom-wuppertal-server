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

import java.io.ByteArrayOutputStream;

import de.cismet.cids.custom.utils.vermessungsunterlagen.VermessungsunterlagenHelper;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;

import static de.cismet.cids.custom.utils.vermessungsunterlagen.VermessungsunterlagenHelper.FTP_PATH;

/**
 * DOCUMENT ME!
 *
 * @author   thorsten
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class VermessungsUnterlagenPortalDownloadAction implements ServerAction {

    //~ Static fields/initializers ---------------------------------------------

    public static final String TASK_NAME = "VUPDownloadAction";

    //~ Methods ----------------------------------------------------------------

    @Override
    public Object execute(final Object body, final ServerActionParameter... params) {
        final String schluessel = new String((byte[])body);
        try {
            final String ftpZipPath = (FTP_PATH.isEmpty() ? "" : ("/" + FTP_PATH)) + "/" + schluessel + ".zip";
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            VermessungsunterlagenHelper.getInstance().downloadFromFTP(baos, ftpZipPath);
            return baos.toByteArray();
        } catch (final Exception ex) {
            return new Exception("Fehler beim Herunterladen der Zip-Datei.", ex);
        }
    }

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }
}
