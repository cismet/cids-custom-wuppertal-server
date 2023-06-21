/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.wunda_blau.search.actions;

import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.middleware.interfaces.domainserver.MetaServiceStore;

import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.io.IOException;

import de.cismet.cids.custom.utils.berechtigungspruefung.BerechtigungspruefungProperties;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class BerechtigungspruefungAnhangDownloadAction implements ServerAction, MetaServiceStore {

    //~ Static fields/initializers ---------------------------------------------

    public static final String TASK_NAME = "berechtigungspruefungAnhangDownload";

    //~ Instance fields --------------------------------------------------------

    private MetaService metaService;

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

            final String filePath = BerechtigungspruefungProperties.getInstance().getAnhangAbsPath() + "/"
                        + dateiname.replaceAll("../", "");
            try {
                return IOUtils.toByteArray(new FileInputStream(filePath));
            } catch (final IOException ex) {
                throw new RuntimeException("File not found.", ex);
            }
        }
    }

    @Override
    public MetaService getMetaService() {
        return metaService;
    }

    @Override
    public void setMetaService(final MetaService metaService) {
        this.metaService = metaService;
    }
}
