package com.caxerx.mc.interconomy.sql;

import com.caxerx.mc.interconomy.InterConomy;
import com.caxerx.mc.interconomy.InterConomyConfig;
import com.caxerx.mc.interconomy.UpdateResult;
import com.hypernite.mc.hnmc.core.main.HyperNiteMC;
import com.hypernite.mc.hnmc.core.managers.SQLDataSource;
import org.bukkit.OfflinePlayer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

import static com.caxerx.mc.interconomy.UpdateResult.*;

/**
 * Created by caxerx on 2016/6/28.
 */
public class MYSQLController {
    private InterConomyConfig config;

    private final String getAccountStatement;
    private final String getBalanceStatement;
    private final String createAccountStatement;
    private final String updateBalanceStatement;
    private final String setBalanceStatement;
    private final String createUserTableStmt;
    private final double defaultBalance = InterConomyConfig.getInstance().defaultBalance;
    private static MYSQLController instance;

    private final SQLDataSource sql;

    public MYSQLController(InterConomyConfig config) {
        this.sql = HyperNiteMC.getAPI().getSQLDataSource();
        this.config = config;
        String userdataTable = config.mysqlUserdataTable;
        createUserTableStmt = "CREATE TABLE IF NOT EXISTS `" + userdataTable + "` ( `uuid` TEXT NOT NULL , `name` TINYTEXT, `money` DOUBLE NOT NULL DEFAULT '0' , PRIMARY KEY (`uuid`(36)))";
        getAccountStatement = "SELECT * FROM `" + userdataTable + "` WHERE `uuid`=?";
        getBalanceStatement = "SELECT `money` FROM `" + userdataTable + "` WHERE `uuid` = ?";
        createAccountStatement = "INSERT IGNORE INTO `" + userdataTable + "` VALUES (?,?,?)";
        updateBalanceStatement = "INSERT INTO " + userdataTable + " VALUES(?,?,?) ON DUPLICATE KEY UPDATE name = ?, money = money + ?";
        setBalanceStatement = "INSERT INTO " + userdataTable + " VALUES(?,?,?) ON DUPLICATE KEY UPDATE name = ?, money = ?";
        instance = this;
        this.createTable();
    }

    public static MYSQLController getInstance() {
        return instance;
    }

    private void createTable(){
        try(Connection connection = sql.getConnection();
            PreparedStatement createTable = connection.prepareStatement(createUserTableStmt);
        ) {
            createTable.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public double getBalance(OfflinePlayer player) {
        double result = defaultBalance;
        String uuid = player.getUniqueId().toString();
        try (Connection connection = sql.getConnection();
             PreparedStatement statement = connection.prepareStatement(getBalanceStatement)) {
            statement.setString(1, uuid);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                result = resultSet.getDouble("money");
            } else {
                createAccount(player);
            }
        } catch (SQLException e) {
            InterConomy.getInstance().getLogger().log(Level.SEVERE, e.getSQLState(), e);
        }
        return result;
    }


    public boolean hasAccount(OfflinePlayer player) {
        String uuid = player.getUniqueId().toString();
        try (Connection connection = sql.getConnection();
             PreparedStatement statement = connection.prepareStatement(getAccountStatement);) {
            statement.setString(1, uuid);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            InterConomy.getInstance().getLogger().log(Level.SEVERE, e.getSQLState(), e);
        }
        return false;
    }

    public void createAccount(OfflinePlayer player) {
        String uuid = player.getUniqueId().toString();
        try (Connection connection = sql.getConnection();
             PreparedStatement statement = connection.prepareStatement(createAccountStatement)
        ) {
            statement.setString(1, uuid);
            statement.setString(2, player.getName());
            statement.setDouble(3, defaultBalance);
            statement.execute();
        } catch (SQLException e) {
            InterConomy.getInstance().getLogger().log(Level.SEVERE, e.getSQLState(), e);
        }
    }


    public UpdateResult updatePlayer(OfflinePlayer player, double value, boolean set) {
        String uuid = player.getUniqueId().toString();
        UpdateResult result = UNKNOWN;
        try (Connection connection = sql.getConnection();
             PreparedStatement statement = connection.prepareStatement(getBalanceStatement);
             PreparedStatement statement2 = connection.prepareStatement(set ? setBalanceStatement : updateBalanceStatement)) {
            if (!set && value < 0) {
                statement.setString(1, uuid);
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    if (resultSet.getDouble("money") < Math.abs(value)) {
                        return BALANCE_INSUFFICIENT;
                    }
                }
            }
            statement2.setString(1, uuid);
            statement2.setString(2, player.getName());
            statement2.setDouble(3, set ? value : config.defaultBalance + value);
            statement2.setString(4, player.getName());
            statement2.setDouble(5, value);
            if (statement2.executeUpdate() > 0) {
                result = SUCCESS;
            }
        } catch (SQLException e) {
            InterConomy.getInstance().getLogger().log(Level.SEVERE, e.getSQLState(), e);
        }
        return result;
    }



    /*
    public boolean logTransition(String uuid, String operator, String action, double value, long time) {
        return logTransition(uuid, operator, action, value, time, getConnection(), true);
    }

    public boolean logTransition(String uuid, String operator, String action, double value, long time, Connection connection) {
        return logTransition(uuid, operator, action, value, time, connection, false);
    }

    public boolean logTransition(String uuid, String operator, String action, double value, long time, Connection connection, boolean closeConnection) {
        PreparedStatement statement = null;
        boolean result = false;
        try {
            statement = connection.prepareStatement(logStatement);
            statement.setString(1, uuid);
            statement.setString(2, operator);
            statement.setString(3, action);
            statement.setDouble(4, value);
            statement.setLong(5, time);
            if (statement.executeUpdate() > 0) {
                result = true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (closeConnection) {
                closeConnection(connection);
            }
            closeStatement(statement);
            return result;
        }
    }
*/
}
