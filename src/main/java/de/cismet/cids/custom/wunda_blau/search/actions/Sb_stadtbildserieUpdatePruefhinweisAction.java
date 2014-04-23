/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.wunda_blau.search.actions;

import Sirius.server.middleware.impls.domainserver.DomainServerImpl;
import Sirius.server.newuser.User;

import org.apache.log4j.Logger;

import org.openide.util.Exceptions;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;
import de.cismet.cids.server.actions.UserAwareServerAction;

import static de.cismet.cids.custom.wunda_blau.search.actions.Sb_stadtbildserieUpdatePruefhinweisAction.ParameterType.*;

/**
 * DOCUMENT ME!
 *
 * @author   Gilles Baatz
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class Sb_stadtbildserieUpdatePruefhinweisAction implements UserAwareServerAction {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(Sb_stadtbildserieUpdatePruefhinweisAction.class);
    public static final String TASK_NAME = "Sb_stadtbildserieUpdatePruefhinweisAction";
    public static final String UPDATE_STADTBILDSERIE = "UPDATE sb_stadtbildserie"
                + " SET pruefen_kommentar = ?, "
                + " pruefen = TRUE, pruefhinweis_von = ? "
                + " WHERE id = ?";

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum ParameterType {

        //~ Enum constants -----------------------------------------------------

        COMMENT, STADTBILDSERIE_ID
    }

    //~ Instance fields --------------------------------------------------------

    private User user;

    //~ Methods ----------------------------------------------------------------

    @Override
    public Object execute(final Object body, final ServerActionParameter... params) {
        String comment = null;
        Integer stadtbildserieId = -1;
        for (final ServerActionParameter param : params) {
            final String key = param.getKey();
            if (COMMENT.toString().equals(key)) {
                comment = (String)param.getValue();
            } else if (STADTBILDSERIE_ID.toString().equals(key)) {
                stadtbildserieId = (Integer)param.getValue();
            }
        }

        if (stadtbildserieId > -1) {
            PreparedStatement s = null;
            try {
                s = DomainServerImpl.getServerInstance().getConnectionPool().getDBConnection().getConnection()
                            .prepareStatement(UPDATE_STADTBILDSERIE);

                s.setString(1, comment);

                final String username = getUser().getName();
                s.setString(2, username);

                s.setInt(3, stadtbildserieId);

                s.executeUpdate();
            } catch (SQLException ex) {
                if (s != null) {
                    LOG.error("Error while updating the Stadtbildserie " + stadtbildserieId + ". Query: "
                                + s.toString(),
                        ex);
                } else {
                    LOG.error("Error while updating the Stadtbildserie " + stadtbildserieId, ex);
                }
            }
        }

        return null;
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
