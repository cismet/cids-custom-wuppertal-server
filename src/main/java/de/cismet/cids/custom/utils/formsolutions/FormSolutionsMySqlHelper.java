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
package de.cismet.cids.custom.utils.formsolutions;

import Sirius.server.middleware.impls.domainserver.DomainServerImpl;
import Sirius.server.property.ServerProperties;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * DOCUMENT ME!
 *
 * @author   jruiz
 * @version  $Revision$, $Date$
 */
public class FormSolutionsMySqlHelper {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            FormSolutionsMySqlHelper.class);

    //~ Instance fields --------------------------------------------------------

    private PreparedStatement preparedSelectStatement;
    private PreparedStatement preparedInsertStatement;
    private PreparedStatement preparedInsertCompleteStatement;
    private PreparedStatement preparedUpdateProductStatement;
    private PreparedStatement preparedUpdateInfoStatement;
    private PreparedStatement preparedUpdateStatusStatement;
    private PreparedStatement preparedUpdatePruefungFreigabeStatement;
    private PreparedStatement preparedUpdatePruefungAblehnungStatement;
    private Connection connection = null;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new FormSolutionsMySqlHelper object.
     */
    private FormSolutionsMySqlHelper() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            LOG.error("com.mysql.jdbc.Driver not found, FormSolutionsMySqlHelper will not work !", ex);
        }
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @throws  SQLException  DOCUMENT ME!
     */
    private void connect() throws SQLException {
        if (this.connection == null) {
            this.connection = DriverManager.getConnection(FormSolutionsProperties.getInstance().getMysqlJdbc());

            this.preparedSelectStatement = connection.prepareStatement(
                    "SELECT id FROM bestellung WHERE transid = ?;");
            this.preparedInsertStatement = connection.prepareStatement(
                    "INSERT INTO bestellung (id, transid, status, flurstueck, buchungsblatt, produkt, nur_download, email, dokument_dateipfad, dokument_dateiname, last_update) VALUES (default, ?, ?, null, null, null, null, null, null, null, now());");
            this.preparedInsertCompleteStatement = connection.prepareStatement(
                    "INSERT INTO bestellung (id, transid, bpruefnr, status, flurstueck, buchungsblatt, produkt, nur_download, email, dokument_dateipfad, dokument_dateiname, last_update) VALUES (default, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now());");
            this.preparedUpdateProductStatement = connection.prepareStatement(
                    "UPDATE bestellung SET bpruefnr = ?, status = ?, last_update = now(), dokument_dateipfad = ?, dokument_dateiname = ? WHERE transid = ?;");
            this.preparedUpdateInfoStatement = connection.prepareStatement(
                    "UPDATE bestellung SET status = ?, last_update = now(), flurstueck = ?, buchungsblatt = ?, produkt = ?, nur_download = ?, email = ? WHERE transid = ?;");
            this.preparedUpdateStatusStatement = connection.prepareStatement(
                    "UPDATE bestellung SET status = ?, last_update = now() WHERE transid = ?;");
            this.preparedUpdatePruefungFreigabeStatement = connection.prepareStatement(
                    "UPDATE bestellung SET bpruefnr = ?, status = ?, last_update = now(), abschlussformular = ? WHERE transid = ?;");
            this.preparedUpdatePruefungAblehnungStatement = connection.prepareStatement(
                    "UPDATE bestellung SET bpruefnr = ?, status = ?, last_update = now(), ablehnungsgrund = ? WHERE transid = ?;");
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   transid  DOCUMENT ME!
     * @param   status   DOCUMENT ME!
     *
     * @throws  SQLException  DOCUMENT ME!
     */
    public void insertOrUpdateStatus(final String transid, final int status) throws SQLException {
        if (isEnabled()) {
            if (checkMysqlEntry(transid)) {
                updateStatus(transid, status);
            } else {
                insertStatus(transid, status);
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   transid  DOCUMENT ME!
     * @param   status   DOCUMENT ME!
     *
     * @throws  SQLException  DOCUMENT ME!
     */
    public void insertStatus(final String transid, final int status) throws SQLException {
        if (isEnabled()) {
            connect();

            int index = 1;
            preparedInsertStatement.setString(index++, transid);
            preparedInsertStatement.setInt(index++, status);
            preparedInsertStatement.executeUpdate();
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   transid  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  SQLException  DOCUMENT ME!
     */
    private boolean checkMysqlEntry(final String transid) throws SQLException {
        boolean mysqlEntryAlreadyExists = false;
        try(final ResultSet resultSet = select(transid)) {
            mysqlEntryAlreadyExists = (resultSet != null) && resultSet.next();
        } catch (final SQLException ex) {
            LOG.error("check nach bereits vorhandenen transids fehlgeschlagen.", ex);
        }

        return mysqlEntryAlreadyExists;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   transid         DOCUMENT ME!
     * @param   status          DOCUMENT ME!
     * @param   landparcelcode  DOCUMENT ME!
     * @param   buchungsblatt   DOCUMENT ME!
     * @param   product         DOCUMENT ME!
     * @param   downloadOnly    DOCUMENT ME!
     * @param   email           DOCUMENT ME!
     * @param   schluessel      DOCUMENT ME!
     * @param   filePath        DOCUMENT ME!
     * @param   origName        DOCUMENT ME!
     *
     * @throws  SQLException  DOCUMENT ME!
     */
    public void insertOrUpdateProduct(final String transid,
            final int status,
            final String landparcelcode,
            final String buchungsblatt,
            final String product,
            final Boolean downloadOnly,
            final String email,
            final String schluessel,
            final String filePath,
            final String origName) throws SQLException {
        if (isEnabled()) {
            if (checkMysqlEntry(transid)) {
                updateProduct(
                    transid,
                    schluessel,
                    status,
                    filePath,
                    origName);
            } else {
                insertProduct(
                    transid,
                    schluessel,
                    status,
                    landparcelcode,
                    buchungsblatt,
                    product,
                    downloadOnly,
                    email,
                    filePath,
                    origName);
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   transid         DOCUMENT ME!
     * @param   schluessel      DOCUMENT ME!
     * @param   status          DOCUMENT ME!
     * @param   landparcelcode  DOCUMENT ME!
     * @param   buchungsblatt   DOCUMENT ME!
     * @param   product         DOCUMENT ME!
     * @param   downloadOnly    DOCUMENT ME!
     * @param   email           DOCUMENT ME!
     * @param   filePath        DOCUMENT ME!
     * @param   origName        DOCUMENT ME!
     *
     * @throws  SQLException  DOCUMENT ME!
     */
    public void insertProduct(final String transid,
            final String schluessel,
            final int status,
            final String landparcelcode,
            final String buchungsblatt,
            final String product,
            final Boolean downloadOnly,
            final String email,
            final String filePath,
            final String origName) throws SQLException {
        if (isEnabled()) {
            connect();

            int index = 1;
            preparedInsertCompleteStatement.setString(index++, transid);
            preparedInsertCompleteStatement.setString(index++, schluessel);
            preparedInsertCompleteStatement.setInt(index++, status);
            preparedInsertCompleteStatement.setString(index++, landparcelcode);
            preparedInsertCompleteStatement.setString(index++, buchungsblatt);
            preparedInsertCompleteStatement.setString(index++, product);
            preparedInsertCompleteStatement.setBoolean(index++, downloadOnly);
            preparedInsertCompleteStatement.setString(index++, email);
            preparedInsertCompleteStatement.setString(index++, filePath);
            preparedInsertCompleteStatement.setString(index++, origName);
            preparedInsertCompleteStatement.executeUpdate();
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   transid  DOCUMENT ME!
     * @param   status   DOCUMENT ME!
     *
     * @throws  SQLException  DOCUMENT ME!
     */
    public void updateStatus(final String transid, final int status) throws SQLException {
        if (isEnabled()) {
            FormSolutionBestellungSpecialLogger.getInstance().log("updating mysql entry for: " + transid);
            connect();

            int index = 1;
            preparedUpdateStatusStatement.setInt(index++, status);
            preparedUpdateStatusStatement.setString(index++, transid);
            preparedUpdateStatusStatement.executeUpdate();
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   schluessel         DOCUMENT ME!
     * @param   transid            DOCUMENT ME!
     * @param   status             DOCUMENT ME!
     * @param   abschlussformular  DOCUMENT ME!
     *
     * @throws  SQLException  DOCUMENT ME!
     */
    public void updatePruefungFreigabe(final String schluessel,
            final String transid,
            final int status,
            final String abschlussformular) throws SQLException {
        if (isEnabled()) {
            connect();

            int index = 1;
            preparedUpdatePruefungFreigabeStatement.setString(index++, schluessel);
            preparedUpdatePruefungFreigabeStatement.setInt(index++, status);
            preparedUpdatePruefungFreigabeStatement.setString(index++, abschlussformular);
            preparedUpdatePruefungFreigabeStatement.setString(index++, transid);
            preparedUpdatePruefungFreigabeStatement.executeUpdate();
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   schluessel       DOCUMENT ME!
     * @param   transid          DOCUMENT ME!
     * @param   status           DOCUMENT ME!
     * @param   ablehnungsgrund  DOCUMENT ME!
     *
     * @throws  SQLException  DOCUMENT ME!
     */
    public void updatePruefungAblehnung(final String schluessel,
            final String transid,
            final int status,
            final String ablehnungsgrund) throws SQLException {
        if (isEnabled()) {
            connect();

            int index = 1;
            preparedUpdatePruefungAblehnungStatement.setString(index++, schluessel);
            preparedUpdatePruefungAblehnungStatement.setInt(index++, status);
            preparedUpdatePruefungAblehnungStatement.setString(index++, ablehnungsgrund);
            preparedUpdatePruefungAblehnungStatement.setString(index++, transid);
            preparedUpdatePruefungAblehnungStatement.executeUpdate();
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   transid            DOCUMENT ME!
     * @param   status             DOCUMENT ME!
     * @param   landparcelcode     DOCUMENT ME!
     * @param   buchungsblattcode  DOCUMENT ME!
     * @param   product            DOCUMENT ME!
     * @param   downloadOnly       DOCUMENT ME!
     * @param   email              DOCUMENT ME!
     *
     * @throws  SQLException  DOCUMENT ME!
     */
    public void updateRequest(final String transid,
            final int status,
            final String landparcelcode,
            final String buchungsblattcode,
            final String product,
            final Boolean downloadOnly,
            final String email) throws SQLException {
        if (isEnabled()) {
            connect();

            int index = 1;
            preparedUpdateInfoStatement.setInt(index++, status);
            preparedUpdateInfoStatement.setString(index++, landparcelcode);
            preparedUpdateInfoStatement.setString(index++, buchungsblattcode);
            preparedUpdateInfoStatement.setString(index++, product);
            preparedUpdateInfoStatement.setBoolean(index++, downloadOnly);
            preparedUpdateInfoStatement.setString(index++, email);
            preparedUpdateInfoStatement.setString(index++, transid);
            preparedUpdateInfoStatement.executeUpdate();
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   transid     DOCUMENT ME!
     * @param   schluessel  DOCUMENT ME!
     * @param   status      DOCUMENT ME!
     * @param   filePath    DOCUMENT ME!
     * @param   origName    DOCUMENT ME!
     *
     * @throws  SQLException  DOCUMENT ME!
     */
    public void updateProduct(final String transid,
            final String schluessel,
            final int status,
            final String filePath,
            final String origName) throws SQLException {
        if (isEnabled()) {
            connect();

            int index = 1;
            preparedUpdateProductStatement.setString(index++, schluessel);
            preparedUpdateProductStatement.setInt(index++, status);
            preparedUpdateProductStatement.setString(index++, filePath);
            preparedUpdateProductStatement.setString(index++, origName);
            preparedUpdateProductStatement.setString(index++, transid);
            preparedUpdateProductStatement.executeUpdate();
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   transid  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  SQLException  DOCUMENT ME!
     */
    private ResultSet select(final String transid) throws SQLException {
        connect();

        preparedSelectStatement.setString(1, transid);
        final ResultSet resultSet = preparedSelectStatement.executeQuery();
        return resultSet;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static FormSolutionsMySqlHelper getInstance() {
        return LazyInitialiser.INSTANCE;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public boolean isEnabled() {
        return ServerProperties.DEPLOY_ENV__PRODUCTION.equalsIgnoreCase(DomainServerImpl.getServerProperties()
                        .getDeployEnv()) && !FormSolutionsProperties.getInstance().isMysqlDisabled();
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    private static final class LazyInitialiser {

        //~ Static fields/initializers -----------------------------------------

        private static final FormSolutionsMySqlHelper INSTANCE = new FormSolutionsMySqlHelper();

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new LazyInitialiser object.
         */
        private LazyInitialiser() {
        }
    }
}
