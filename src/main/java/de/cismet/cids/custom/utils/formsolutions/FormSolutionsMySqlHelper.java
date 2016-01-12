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

import org.openide.util.Exceptions;

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
    private final PreparedStatement preparedUpdateStatement;

    private Connection connect = null;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new FormSolutionsMySqlHelper object.
     */
    private FormSolutionsMySqlHelper() {
        PreparedStatement preparedSelectStatement = null;
        PreparedStatement preparedInsertStatement = null;
        PreparedStatement preparedUpdateStatement = null;

        try {
            Class.forName("com.mysql.jdbc.Driver");
            connect = DriverManager.getConnection(FormSolutionsConstants.MYSQL_JDBC);
            try {
                preparedSelectStatement = connect.prepareStatement("SELECT id FROM bestellung where transid = ?;");
            } catch (final SQLException ex) {
                LOG.error(ex, ex);
            }
            try {
                preparedInsertStatement = connect.prepareStatement(
                        "INSERT INTO bestellung VALUES (default, ?, ?, null, null, ?);");
            } catch (final SQLException ex) {
                LOG.error(ex, ex);
            }
            try {
                preparedUpdateStatement = connect.prepareStatement(
                        "UPDATE bestellung SET status = ?, dokument_dateipfad = ?, dokument_dateiname = ?, last_update = ? WHERE transid = ?;");
            } catch (final SQLException ex) {
                LOG.error(ex, ex);
            }
        } catch (final Exception ex) {
            Exceptions.printStackTrace(ex);
        }
        this.preparedSelectStatement = preparedSelectStatement;
        this.preparedInsertStatement = preparedInsertStatement;
        this.preparedUpdateStatement = preparedUpdateStatement;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static FormSolutionsMySqlHelper getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new FormSolutionsMySqlHelper();
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
    public void insertMySQL(final String transid, final int status) throws SQLException {
        preparedInsertStatement.setString(1, transid);
        preparedInsertStatement.setInt(2, status);
        preparedInsertStatement.setTimestamp(3, new Timestamp(new java.util.Date().getTime()));
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
    public void updateMySQL(final String transid, final int status) throws SQLException {
        updateMySQL(transid, status, null, null);
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
    public void updateMySQL(final String transid, final int status, final String filePath, final String origName)
            throws SQLException {
        preparedUpdateStatement.setInt(1, status);
        preparedUpdateStatement.setString(2, filePath);
        preparedUpdateStatement.setString(3, origName);
        preparedUpdateStatement.setTimestamp(4, new Timestamp(new java.util.Date().getTime()));
        preparedUpdateStatement.setString(5, transid);
        preparedUpdateStatement.executeUpdate();
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
    public ResultSet selectMySQL(final String transid) throws SQLException {
        ResultSet resultSet = null;
        try {
            preparedSelectStatement.setString(1, transid);
            resultSet = preparedSelectStatement.executeQuery();
        } finally {
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException ex) {
                }
            }
        }
        return resultSet;
    }
}
