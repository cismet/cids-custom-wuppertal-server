/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
/*
 *  Copyright (C) 2010 thorsten
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.cismet.cids.custom.extensionfactory.wunda_blau;

import Sirius.server.middleware.types.MetaClass;
import Sirius.server.middleware.types.MetaObject;

import de.cismet.cids.dynamics.CidsBean;

import de.cismet.cids.objectextension.ObjectExtensionFactory;

import de.cismet.connectioncontext.AbstractConnectionContext;
import de.cismet.connectioncontext.ConnectionContext;
import de.cismet.connectioncontext.ConnectionContextProvider;

/**
 * DOCUMENT ME!
 *
 * @version  $Revision$, $Date$
 */
public class VzkatStandortExtensionFactory extends ObjectExtensionFactory implements ConnectionContextProvider {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            VzkatStandortExtensionFactory.class);

    //~ Instance fields --------------------------------------------------------

    private final ConnectionContext connectionContext = ConnectionContext.create(
            AbstractConnectionContext.Category.STATIC,
            VzkatStandortExtensionFactory.class.getSimpleName());

    //~ Methods ----------------------------------------------------------------

    @Override
    public void extend(final CidsBean standortBean) {
        try {
            final String queryTemplate = ""
                        + "SELECT %d, %s "
                        + "FROM %s "
                        + "WHERE fk_standort = %d "
                        + " AND ( "
                        + "  now() BETWEEN gueltig_von AND gueltig_bis "
                        + "  OR (gueltig_bis IS NULL AND now() >= gueltig_von)"
                        + "  OR (gueltig_von IS NULL AND now() <= gueltig_bis)"
                        + " )"
                        + ";";

            final MetaClass mcBestellung = CidsBean.getMetaClassFromTableName(
                    "WUNDA_BLAU",
                    "vzkat_standort",
                    getConnectionContext());
            final String query = String.format(
                    queryTemplate,
                    mcBestellung.getID(),
                    mcBestellung.getTableName()
                            + "."
                            + mcBestellung.getPrimaryKey(),
                    mcBestellung.getTableName(),
                    standortBean.getProperty("id"));

            final MetaObject[] mos = getDomainServer().getMetaObject(getUser(), query, getConnectionContext());
            final Integer[] schilderIds = (mos != null) ? new Integer[mos.length] : null;
            if (schilderIds != null) {
                for (int i = 0; i < schilderIds.length; i++) {
                    schilderIds[i] = mos[i].getId();
                }
            }
            standortBean.setProperty("ext_aktive_schilder", schilderIds);

            /*
             * try { String val = "kein Wert gefunden"; Class.forName("org.postgresql.Driver").newInstance(); final
             * String url = "jdbc:postgresql://localhost:5432/wunda_demo"; final Connection conn =
             * DriverManager.getConnection(url, "postgres", "x"); final Statement stmnt = conn.createStatement(); final
             * ResultSet rs = stmnt.executeQuery("select leiter from kigaeinrichtung where id="                 +
             * bean.getProperty("number").toString()); rs.next(); final String s = rs.getString(1); if (s != null) { val
             * = s; } conn.close();
             *
             * bean.setProperty("firstextensiontest", val + " (" + System.currentTimeMillis() + ")"); } catch (Exception
             * ex) { log.error("Error during extension", ex); }
             */
        } catch (final Exception ex) {
            LOG.error(ex, ex);
        }
    }

    @Override
    public ConnectionContext getConnectionContext() {
        return connectionContext;
    }
}
