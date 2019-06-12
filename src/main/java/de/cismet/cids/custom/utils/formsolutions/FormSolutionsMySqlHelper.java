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
    private final PreparedStatement preparedInsertCompleteStatement;
    private final PreparedStatement preparedUpdateProductStatement;
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
        this.connect = DriverManager.getConnection(FormSolutionsProperties.getInstance().getMysqlJdbc());

        this.preparedSelectStatement = connect.prepareStatement(
                "SELECT id FROM bestellung WHERE transid = ?;");
        this.preparedInsertStatement = connect.prepareStatement(
                "INSERT INTO bestellung (id, transid, status, flurstueck, produkt, nur_download, email, dokument_dateipfad, dokument_dateiname, last_update) VALUES (default, ?, ?, null, null, null, null, null, null, now());");
        this.preparedInsertCompleteStatement = connect.prepareStatement(
                "INSERT INTO bestellung (id, transid, status, flurstueck, produkt, nur_download, email, dokument_dateipfad, dokument_dateiname, last_update) VALUES (default, ?, ?, ?, ?, ?, ?, ?, ?, now());");
        this.preparedUpdateProductStatement = connect.prepareStatement(
                "UPDATE bestellung SET status = ?, last_update = now(), dokument_dateipfad = ?, dokument_dateiname = ? WHERE transid = ?;");
        this.preparedUpdateInfoStatement = connect.prepareStatement(
                "UPDATE bestellung SET status = ?, last_update = now(), flurstueck = ?, produkt = ?, nur_download = ?, email = ? WHERE transid = ?;");
        this.preparedUpdateStatusStatement = connect.prepareStatement(
                "UPDATE bestellung SET status = ?, last_update = now() WHERE transid = ?;");
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
        preparedInsertStatement.executeUpdate();
    }

    /**
     * DOCUMENT ME!
     *
     * @param   transid         DOCUMENT ME!
     * @param   status          DOCUMENT ME!
     * @param   landparcelcode  DOCUMENT ME!
     * @param   product         DOCUMENT ME!
     * @param   downloadOnly    DOCUMENT ME!
     * @param   email           DOCUMENT ME!
     * @param   filePath        DOCUMENT ME!
     * @param   origName        DOCUMENT ME!
     *
     * @throws  SQLException  DOCUMENT ME!
     */
    public void insertProductMySql(final String transid,
            final int status,
            final String landparcelcode,
            final String product,
            final boolean downloadOnly,
            final String email,
            final String filePath,
            final String origName) throws SQLException {
        int index = 1;
        preparedInsertCompleteStatement.setString(index++, transid);
        preparedInsertCompleteStatement.setInt(index++, status);
        preparedInsertCompleteStatement.setString(index++, landparcelcode);
        preparedInsertCompleteStatement.setString(index++, product);
        preparedInsertCompleteStatement.setBoolean(index++, downloadOnly);
        preparedInsertCompleteStatement.setString(index++, email);
        preparedInsertCompleteStatement.setString(index++, filePath);
        preparedInsertCompleteStatement.setString(index++, origName);
        preparedInsertCompleteStatement.executeUpdate();
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
        preparedUpdateStatusStatement.setString(index++, transid);
        preparedUpdateStatusStatement.executeUpdate();
    }

    /**
     * DOCUMENT ME!
     *
     * @param   transid         DOCUMENT ME!
     * @param   status          DOCUMENT ME!
     * @param   landparcelcode  DOCUMENT ME!
     * @param   product         DOCUMENT ME!
     * @param   downloadOnly    DOCUMENT ME!
     * @param   email           DOCUMENT ME!
     *
     * @throws  SQLException  DOCUMENT ME!
     */
    public void updateRequest(final String transid,
            final int status,
            final String landparcelcode,
            final String product,
            final boolean downloadOnly,
            final String email) throws SQLException {
        int index = 1;
        preparedUpdateInfoStatement.setInt(index++, status);
        preparedUpdateInfoStatement.setString(index++, landparcelcode);
        preparedUpdateInfoStatement.setString(index++, product);
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
    public void updateProduct(final String transid, final int status, final String filePath, final String origName)
            throws SQLException {
        int index = 1;
        preparedUpdateProductStatement.setInt(index++, status);
        preparedUpdateProductStatement.setString(index++, filePath);
        preparedUpdateProductStatement.setString(index++, origName);
        preparedUpdateProductStatement.setString(index++, transid);
        preparedUpdateProductStatement.executeUpdate();
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
