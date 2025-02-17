package net.slipcor.pvpstats.impl;

import net.slipcor.pvpstats.api.DatabaseConnection;
import net.slipcor.pvpstats.classes.PlayerStatistic;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A partial implementation of methods that are handled the same by all SQL implementations
 */
public abstract class AbstractSQLConnection implements DatabaseConnection {

    // Database tables
    final String dbTable;
    final String dbKillTable;

    // The connection object
    Connection databaseConnection;

    boolean collectPrecise = false;

    AbstractSQLConnection(String dbTable, String dbKillTable) {
        this.dbTable = dbTable;
        this.dbKillTable = dbKillTable;
    }

    /**
     * Actually execute an SQL query
     *
     * @param query    the query to send to the SQL server.
     * @param modifies tf the Query modifies the database, set this to true, otherwise set this to false
     * @return If modifies is true, returns a valid ResultSet obtained from the query, otherwise returns null.
     * @throws SQLException if the query had an error or there was not a valid connection.
     */
    protected ResultSet executeQuery(final String query, final boolean modifies) throws SQLException {
        Statement statement = this.databaseConnection.createStatement();
        if (modifies) {
            statement.execute(query);
            return null;
        } else {
            return statement.executeQuery(query);
        }
    }

    /*
     * ----------------------
     *  TABLE ENTRY CREATION
     * ----------------------
     */

    /**
     * Create the first statistic entry for a player
     *
     * @param playerName the player's name
     * @param uuid       the player's UUID
     * @param kills      the kill amount
     * @param deaths     the death amount
     * @param elo        the ELO rating
     */
    @Override
    public void addFirstStat(String playerName, UUID uuid, int kills, int deaths, int elo) {
        long time = System.currentTimeMillis() / 1000;
        try {
            executeQuery("INSERT INTO `" + dbTable +
                    "` (`name`, `uid`, `kills`,`deaths`,`streak`,`currentstreak`,`elo`,`time`) VALUES ('"
                    + playerName + "', '" + uuid + "', " + kills + ", " + deaths + ", " +
                    kills + ", " + kills + ", " + elo + ", " + time + ")", true);
        } catch (SQLException e) {
        }
    }

    /**
     * Add a kill to the player's count
     *
     * @param playerName the player's name
     * @param uuid       the player's uuid
     * @param kill       true if they did kill, false if they were killed
     */
    @Override
    public void addKill(String playerName, UUID uuid, boolean kill) {
        if (!collectPrecise) {
            return;
        }
        long time = System.currentTimeMillis() / 1000;
        try {
            executeQuery("INSERT INTO " + dbKillTable + " (`name`,`uid`,`kill`,`time`) VALUES(" +
                    "'" + playerName + "', '" + uuid + "', '" + (kill ? 1 : 0) + "', " + time + ")", true);
        } catch (SQLException e) {
        }
    }

    /**
     * Delete ALL kill stats
     */
    @Override
    public void deleteKills() {
        if (!collectPrecise) {
            return;
        }
        try {
            executeQuery("DELETE FROM `" + dbKillTable + "` WHERE 1;", true);
        } catch (SQLException e) {
        }
    }

    /**
     * Delete kill stats of a player
     *
     * @param playerName the player's name
     */
    @Override
    public void deleteKillsByName(String playerName) {
        if (!collectPrecise) {
            return;
        }
        try {
            executeQuery("DELETE FROM `" + dbKillTable + "` WHERE `name` = '" + playerName
                    + "';", true);
        } catch (SQLException e) {
        }
    }

    /**
     * Delete kill stats older than a timestamp
     *
     * @param timestamp the timestamp to compare to
     * @throws SQLException
     */
    @Override
    public int deleteKillsOlderThan(long timestamp) throws SQLException {
        if (!collectPrecise) {
            return 0;
        }

        int count = 0;

        ResultSet result = executeQuery("SELECT `time` FROM `" + dbKillTable + "` WHERE `time` < " + timestamp + ";", false);
        while (result.next()) {
            count++;
        }
        executeQuery("DELETE FROM `" + dbKillTable + "` WHERE `time` < " + timestamp + ";", true);
        return count;
    }

    /**
     * Delete all statistics
     */
    @Override
    public void deleteStats() {
        try {
            executeQuery("DELETE FROM `" + dbTable + "` WHERE 1;", true);
        } catch (SQLException e) {
        }
    }

    /**
     * Delete statistics by player name
     *
     * @param playerName the player's name
     */
    @Override
    public void deleteStatsByName(String playerName) {
        try {
            executeQuery("DELETE FROM `" + dbTable + "` WHERE `name` = '" + playerName
                    + "';", true);
        } catch (SQLException e) {

        }
    }

    /**
     * Delete statistics older than a timestamp
     *
     * @param timestamp the timestamp to compare to
     * @throws SQLException
     */
    @Override
    public int deleteStatsOlderThan(long timestamp) throws SQLException {

        int count = 0;

        ResultSet result = executeQuery("SELECT `time` FROM `" + dbTable + "` WHERE `time` < " + timestamp + ";", false);
        while (result.next()) {
            count++;
        }
        executeQuery("DELETE FROM `" + dbTable + "` WHERE `time` < " + timestamp + ";", true);
        return count;
    }

    /**
     * Get a statistic value by exact player name
     *
     * @param stat       the statistic value
     * @param playerName the exact player's name to look for
     * @return a set of all matching entries
     * @throws SQLException
     */
    @Override
    public int getStatExact(String stat, String playerName) throws SQLException {
        ResultSet result = executeQuery("SELECT `" + stat + "` FROM `" + dbTable + "` WHERE `name` = '" + playerName + "' LIMIT 1;", false);
        return (result != null && result.next()) ? result.getInt(stat) : -1;
    }

    /**
     * Get a statistic value by matching partial player name
     *
     * @param stat       the statistic value
     * @param playerName the partial player's name to look for
     * @return a set of all matching entries
     * @throws SQLException
     */
    @Override
    public int getStatLike(String stat, String playerName) throws SQLException {
        ResultSet result = executeQuery("SELECT `" + stat + "` FROM `" + dbTable + "` WHERE `name` LIKE '%" + playerName + "%' LIMIT 1;", false);
        return (result != null && result.next()) ? result.getInt(stat) : -1;
    }

    /**
     * Get statistics by exact player name
     *
     * @param playerName the exact player's name to look for
     * @return the first matching player stat entry
     * @throws SQLException
     */
    @Override
    public PlayerStatistic getStatsExact(String playerName) throws SQLException {
        ResultSet result = executeQuery("SELECT `name`,`kills`,`deaths`,`streak`,`currentstreak`, `elo` FROM `" + dbTable + "` WHERE `name` = '" + playerName + "' LIMIT 1;", false);
        if (result.next()) {
            return new PlayerStatistic(result.getString("name"),
                    result.getInt("kills"),
                    result.getInt("deaths"),
                    result.getInt("streak"),
                    result.getInt("currentstreak"),
                    result.getInt("elo"));
        }
        return null;
    }

    /**
     * Get statistics by matching partial player name
     *
     * @param playerName the partial player's name to look for
     * @return the first matching player stat entry
     * @throws SQLException
     */
    @Override
    public PlayerStatistic getStatsLike(String playerName) throws SQLException {
        ResultSet result = executeQuery("SELECT `name`,`kills`,`deaths`,`streak`,`currentstreak`, `elo` FROM `" + dbTable + "` WHERE `name` LIKE '%" + playerName + "%' LIMIT 1;", false);
        List<PlayerStatistic> list = new ArrayList<>();
        if (result.next()) {
            return new PlayerStatistic(result.getString("name"),
                    result.getInt("kills"),
                    result.getInt("deaths"),
                    result.getInt("streak"),
                    result.getInt("currentstreak"),
                    result.getInt("elo"));
        }
        return null;
    }

    /**
     * Get all player names
     *
     * @return all player names
     * @throws SQLException
     */
    @Override
    public List<String> getStatsNames() throws SQLException {
        List<String> list = new ArrayList<>();
        ResultSet result = executeQuery("SELECT `name` FROM `" + dbTable + "` GROUP BY `name`;", false);
        while (result.next()) {
            list.add(result.getString("name"));
        }
        return list;
    }

    /**
     * Get a player's saved UUID entry
     *
     * @param player the player to look for
     * @return their UID
     * @throws SQLException
     */
    @Override
    public String getStatUIDFromPlayer(Player player) throws SQLException {
        ResultSet result = executeQuery("SELECT `uid` FROM `" + dbTable + "` WHERE `name` = '" + player.getName() + "';", false);
        if (result.next()) {
            return result.getString("uid");
        }
        return "";
    }

    /**
     * Get the top players sorted by a given column
     *
     * @param amount    the amount to return
     * @param orderBy   the column to sort by
     * @param ascending true if ascending order, false otherwise
     * @return a list of all stats from the top players
     * @throws SQLException
     */
    @Override
    public List<PlayerStatistic> getTopSorted(int amount, String orderBy, boolean ascending) throws SQLException {
        String query = "SELECT `name`,`kills`,`deaths`,`streak`,`currentstreak`,`elo` FROM `" +
                dbTable + "` WHERE 1 ORDER BY `" + orderBy + "` " + (ascending ? "ASC" : "DESC") + " LIMIT " + amount + ";";

        List<PlayerStatistic> list = new ArrayList<>();

        ResultSet result = executeQuery(query, false);

        if (result == null) {
            return null;
        }

        while (result.next()) {
            list.add(new PlayerStatistic(result.getString("name"),
                    result.getInt("kills"),
                    result.getInt("deaths"),
                    result.getInt("streak"),
                    result.getInt("currentstreak"),
                    result.getInt("elo")));
        }
        return list;
    }

    /**
     * Check whether an entry matches a player UUID
     *
     * @param uuid the UUID to find
     * @return true if found, false otherwise
     */
    @Override
    public boolean hasEntry(UUID uuid) {
        try {
            ResultSet result = executeQuery("SELECT * FROM `" + dbTable + "` WHERE `uid` = '" + uuid + "';", false);
            return result != null && result.next();
        } catch (SQLException e) {
        }
        return false;
    }

    /**
     * Increase player death count, update ELO score and reset streak
     *
     * @param uuid the player's UUID
     * @param elo  the new ELO rating
     */
    @Override
    public void increaseDeaths(UUID uuid, int elo) {
        long time = System.currentTimeMillis() / 1000;
        try {
            executeQuery("UPDATE `" + dbTable + "` SET `deaths` = `deaths`+1, `elo` = " + elo +
                    ", `currentstreak` = 0, `time` = " + time + " WHERE `uid` = '" + uuid + "'", true);
        } catch (SQLException e) {
        }
    }

    /**
     * Increase player kill count, update ELO score and the max and current streak
     *
     * @param uuid the player's UUID
     * @param elo  the new ELO rating
     */
    @Override
    public void increaseKillsAndMaxStreak(UUID uuid, int elo) {
        long time = System.currentTimeMillis() / 1000;
        try {
            executeQuery("UPDATE `" + dbTable + "` SET `kills` = `kills`+1, `elo` = '" + elo +
                    "', `streak` = `streak`+1, `currentstreak` = `currentstreak`+1, `time` = " + time +
                    " WHERE `uid` = '" + uuid + "'", true);
        } catch (SQLException e) {
        }
    }

    /**
     * Increase player kill count, update ELO score and the current streak
     *
     * @param uuid the player's UUID
     * @param elo  the new ELO rating
     */
    @Override
    public void increaseKillsAndStreak(UUID uuid, int elo) {
        long time = System.currentTimeMillis() / 1000;
        try {
            executeQuery("UPDATE `" + dbTable + "` SET `kills` = `kills`+1, `elo` = '" + elo +
                    "', `currentstreak` = `currentstreak`+1, `time` = " + time +
                    " WHERE `uid` = '" + uuid + "'", true);
        } catch (SQLException e) {
        }
    }

    /**
     * @return whether the connection was established properly
     */
    @Override
    public boolean isConnected() {
        return this.databaseConnection != null;
    }

    /**
     * Set specific statistical value of a player
     *
     * @param playerName the player to find
     * @param entry      the entry to set
     * @param value      the value to set
     */
    @Override
    public void setSpecificStat(String playerName, String entry, int value) throws SQLException {
        executeQuery("UPDATE `" + dbTable + "` SET `" + entry + "` = " + value + " WHERE `name` = '" + playerName + "';", true);
    }

    /**
     * Set the UUID of a certain player entry
     *
     * @param player the player to find and update
     * @throws SQLException
     */
    @Override
    public void setStatUIDByPlayer(Player player) throws SQLException {
        executeQuery("UPDATE `" + dbTable + "` SET `uid` = '" + player.getUniqueId() + "' WHERE `name` = '" + player.getName() + "';", true);
    }
}
