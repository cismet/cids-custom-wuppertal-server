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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

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

    private static FormSolutionsMySqlHelper INSTANCE;

    //~ Instance fields --------------------------------------------------------

    private final PreparedStatement preparedSelectStatement;
    private final PreparedStatement preparedInsertStatement;
    private final PreparedStatement preparedUpdateProduktStatement;
    private final PreparedStatement preparedUpdateInfoStatement;
    private final PreparedStatement preparedUpdateStatusStatement;

    private Connection connect = null;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new FormSolutionsMySqlHelper object.
     *
     * @throws  Exception  DOCUMENT ME!
     */
    private FormSolutionsMySqlHelper() throws Exception {
        Class.forName("com.mysql.jdbc.Driver");
        this.connect = DriverManager.getConnection(FormSolutionsConstants.MYSQL_JDBC);

        this.preparedSelectStatement = connect.prepareStatement(
                "SELECT id FROM bestellung WHERE transid = ?;");
        this.preparedInsertStatement = connect.prepareStatement(
                "INSERT INTO bestellung VALUES (default, ?, ?, null, null, null, null, null, null, ?);");
        this.preparedUpdateProduktStatement = connect.prepareStatement(
                "UPDATE bestellung SET status = ?, last_update = ?, dokument_dateipfad = ?, dokument_dateiname = ? WHERE transid = ?;");
        this.preparedUpdateInfoStatement = connect.prepareStatement(
                "UPDATE bestellung SET status = ?, last_update = ?, flurstueck = ?, produkt = ?, nur_download = ?, email = ? WHERE transid = ?;");
        this.preparedUpdateStatusStatement = connect.prepareStatement(
                "UPDATE bestellung SET status = ?, last_update = ? WHERE transid = ?;");
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static FormSolutionsMySqlHelper getInstance() {
        if (INSTANCE == null) {
            try {
                INSTANCE = new FormSolutionsMySqlHelper();
            } catch (final Exception ex) {
                LOG.error("error while intiliazing FormSolutionsMySqlHelper", ex);
            }
        }
        return INSTANCE;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   transid  DOCUMENT ME!
     * @param   status   DOCUMENT ME!
     *
     * @throws  SQLException  DOCUMENT ME!
     */
    public void insertMySql(final String transid, final int status) throws SQLException {
        int index = 1;
        preparedInsertStatement.setString(index++, transid);
        preparedInsertStatement.setInt(index++, status);
        preparedInsertStatement.setTimestamp(index++, new Timestamp(new java.util.Date().getTime()));
        preparedInsertStatement.executeUpdate();
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
        int index = 1;
        preparedUpdateStatusStatement.setInt(index++, status);
        preparedUpdateStatusStatement.setTimestamp(index++, new Timestamp(new java.util.Date().getTime()));
        preparedUpdateStatusStatement.setString(index++, transid);
        preparedUpdateStatusStatement.executeUpdate();
    }

    /**
     * DOCUMENT ME!
     *
     * @param   transid         DOCUMENT ME!
     * @param   status          DOCUMENT ME!
     * @param   landparcelcode  DOCUMENT ME!
     * @param   produkt         DOCUMENT ME!
     * @param   downloadOnly    DOCUMENT ME!
     * @param   email           DOCUMENT ME!
     *
     * @throws  SQLException  DOCUMENT ME!
     */
    public void updateEmail(final String transid,
            final int status,
            final String landparcelcode,
            final String produkt,
            final boolean downloadOnly,
            final String email) throws SQLException {
        int index = 1;
        preparedUpdateInfoStatement.setInt(index++, status);
        preparedUpdateInfoStatement.setTimestamp(index++, new Timestamp(new java.util.Date().getTime()));
        preparedUpdateInfoStatement.setString(index++, landparcelcode);
        preparedUpdateInfoStatement.setString(index++, produkt);
        preparedUpdateInfoStatement.setBoolean(index++, downloadOnly);
        preparedUpdateInfoStatement.setString(index++, email);
        preparedUpdateInfoStatement.setString(index++, transid);
        preparedUpdateInfoStatement.executeUpdate();
    }

    /**
     * DOCUMENT ME!
     *
     * @param   transid   DOCUMENT ME!
     * @param   status    DOCUMENT ME!
     * @param   filePath  DOCUMENT ME!
     * @param   origName  DOCUMENT ME!
     *
     * @throws  SQLException  DOCUMENT ME!
     */
    public void updateProdukt(final String transid, final int status, final String filePath, final String origName)
            throws SQLException {
        int index = 1;
        preparedUpdateProduktStatement.setInt(index++, status);
        preparedUpdateProduktStatement.setTimestamp(index++, new Timestamp(new java.util.Date().getTime()));
        preparedUpdateProduktStatement.setString(index++, filePath);
        preparedUpdateProduktStatement.setString(index++, origName);
        preparedUpdateProduktStatement.setString(index++, transid);
        preparedUpdateProduktStatement.executeUpdate();
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
    public ResultSet select(final String transid) throws SQLException {
        preparedSelectStatement.setString(1, transid);
        final ResultSet resultSet = preparedSelectStatement.executeQuery();
        return resultSet;
    }
}
