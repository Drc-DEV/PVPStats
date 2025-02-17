package net.slipcor.pvpstats.impl;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Based on MySQLConnection by Jesika(Kaitlyn) Tremaine aka JesiKat
 */
public class MySQLConnection extends AbstractSQLConnection {
    // SQL connection details
    private final String dbUrl, dbUsername, dbPassword, dbOptions;

    public MySQLConnection(String host, int port,
                           String database, String username, String password, String dbOptions,
                           String dbTable, String dbKillTable)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        super(dbTable, dbKillTable);
        this.dbUrl = host + ":" + port + "/" + database;
        this.dbUsername = username;
        this.dbPassword = password;
        this.dbOptions = dbOptions;

        Class.forName("com.mysql.jdbc.Driver").newInstance();
    }

    /**
     * Try to connect to the database
     *
     * @param printError should we print errors that we encounter?
     * @return true if the connection was made successfully, false otherwise.
     */
    public boolean connect(boolean printError) {
        try {
            this.databaseConnection = DriverManager.getConnection("jdbc:mysql://" + this.dbUrl + this.dbOptions, this.dbUsername, this.dbPassword);

            collectPrecise = dbKillTable != null && !"".equals(dbKillTable);

            return this.databaseConnection != null;
        } catch (SQLException e) {
            if (printError) e.printStackTrace();
            return false;
        }
    }

    /**
     * Check whether a table exists
     *
     * @param database The database to check for the table in.
     * @param table    The table to check for existence.
     * @return true if the table exists, false if there was an error or the database doesn't exist.
     */
    public boolean tableExists(String database, String table) {
        String format = "SELECT * FROM `information_schema`.`TABLES` WHERE TABLE_SCHEMA = '$DB' && TABLE_NAME = '$TABLE';";
        try {
            return this.databaseConnection.createStatement().executeQuery(format.replace("$DB", database).replace("$TABLE", table)).first();
        } catch (SQLException e) {
            return false;
        }
    }

    /*
     * ----------------------
     *  TABLE ENTRY CREATION
     * ----------------------
     */

    /**
     * Create the kill stat table
     *
     * @param printError should we print errors that we encounter?
     */
    @Override
    public void createKillStatsTable(boolean printError) {
        final String query2 = "CREATE TABLE `" + dbKillTable + "` ( " +
                "`id` int(16) NOT NULL AUTO_INCREMENT, " +
                "`name` varchar(42) NOT NULL, " +
                "`uid` varchar(42), " +
                "`kill` int(1) not null default 0, " +
                "`time` int(16) not null default 0, " +
                "PRIMARY KEY (`id`) ) AUTO_INCREMENT=1 ;";
        try {
            executeQuery(query2, true);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Create the statistics table
     *
     * @param printError should we print errors that we encounter?
     */
    @Override
    public void createStatsTable(boolean printError) {
        final String query = "CREATE TABLE `" + dbTable + "` ( " +
                "`id` int(5) NOT NULL AUTO_INCREMENT, " +
                "`name` varchar(42) NOT NULL, " +
                "`uid` varchar(42), " +
                "`kills` int(8) not null default 0, " +
                "`deaths` int(8) not null default 0, " +
                "`streak` int(8) not null default 0, " +
                "`currentstreak` int(8) not null default 0, " +
                "`elo` int(8) not null default 0, " +
                "`time` int(16) not null default 0, " +
                "PRIMARY KEY (`id`) ) AUTO_INCREMENT=1 ;";
        try {
            executeQuery(query, true);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Delete all statistics by ID
     *
     * @param list the list of IDs to delete
     * @throws SQLException
     */
    @Override
    public void deleteStatsByIDs(List<Integer> list) throws SQLException {

        StringBuilder buff = new StringBuilder("DELETE FROM `");
        buff.append(dbTable);
        buff.append("` WHERE `id` IN (");

        boolean first = true;

        for (Integer i : list) {
            if (!first) {
                buff.append(',');
            }
            first = false;
            buff.append(i);
        }

        buff.append(");");

        executeQuery(buff.toString(), true);
    }

    /**
     * Get ALL statistics player names and entry IDs
     *
     * @return a map of all entry IDs and player names
     * @throws SQLException
     */
    @Override
    public Map<Integer, String> getStatsIDsAndNames() throws SQLException {
        Map<Integer, String> map = new LinkedHashMap<>();
        ResultSet result = executeQuery("SELECT `id`, `name` FROM `" + dbTable + "` WHERE 1 ORDER BY `kills` DESC;", false);
        while (result.next()) {
            map.put(result.getInt("id"), result.getString("name"));
        }
        return map;
    }
}
