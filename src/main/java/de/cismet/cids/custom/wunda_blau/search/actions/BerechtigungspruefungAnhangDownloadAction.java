/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.wunda_blau.search.actions;

import de.cismet.cids.custom.utils.berechtigungspruefung.BerechtigungspruefungProperties;

import de.cismet.cids.server.actions.DownloadFileAction;
import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class BerechtigungspruefungAnhangDownloadAction extends DownloadFileAction {

    //~ Static fields/initializers ---------------------------------------------

    public static final String TASK_NAME = "berechtigungspruefungAnhangDownload";

    //~ Methods ----------------------------------------------------------------

    @Override
    public String getTaskName() {
        return TASK_NAME;
    }

    @Override
    public Object execute(final Object body, final ServerActionParameter... params) {
        if (body == null) {
            throw new RuntimeException("The body is missing.");
        } else if (!(body instanceof String)) {
            throw new RuntimeException("Wrong type for body, have to be as String.");
        } else {
            final String dateiname = (String)body;

            final String filePath = BerechtigungspruefungProperties.ANHANG_PFAD + "/" + dateiname.replaceAll("../", "");
            final Object ret = super.execute(filePath);
            if (ret == null) {
                throw new RuntimeException("File not found.");
            }
            return ret;
        }
    }
}
