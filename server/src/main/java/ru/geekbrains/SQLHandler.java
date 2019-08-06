package ru.geekbrains;

import java.sql.*;

public class SQLHandler {
    private static Connection connection;
    private static Statement statement;
    private static PreparedStatement psAddHistory;
    private static PreparedStatement psGetHistory;

    public static void connect() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:server/data.db");
        statement = connection.createStatement();
        psAddHistory = connection.prepareStatement("INSERT INTO history (from_user_id, to_user_id, message) VALUES (?, ?, ?);");
        psGetHistory = connection.prepareStatement("SELECT message FROM history WHERE to_user_id = -1 OR to_user_id = ?;");
//        createBaseUsers();
    }

    public static void createBaseUsers() {
        for (int i = 0; i < 30; i++) {
            addNewUser("user" + i, "pass" + i, "nick" + i);
        }
    }

    public static boolean addNewUser(String login, String password, String nickName) {
        try {
            int passHash = password.hashCode();
            statement.executeUpdate(String.format("INSERT INTO users (login, password, nickName) VALUES ('%s', '%d', '%s');", login, passHash, nickName));
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static void disconnect() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static String getNickByLoginPass(String login, String password) {
        try {
            int passHash = password.hashCode();
            ResultSet res = statement.executeQuery(String.format("SELECT nickName FROM users WHERE login = '%s' AND password = '%d';", login, passHash));
            if (res.next()) {
                return res.getString("nickName");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static int getIdByNick(String nickName) {
        try {
            ResultSet res = statement.executeQuery(String.format("SELECT id FROM users WHERE nickName = '%s';", nickName));
            if (res.next()) {
                return res.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }


    public static void addHistory(int fromUserId, int toUserId, String msg) {
        try {
            psAddHistory.setInt(1,fromUserId);
            psAddHistory.setInt(2,toUserId);
            psAddHistory.setString(3, msg);
            psAddHistory.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static String getHistory(int userId) {
        try {
            StringBuilder sb = new StringBuilder();
            psGetHistory.setInt(1, userId);
            ResultSet rs = psGetHistory.executeQuery();
            while(rs.next()) {
                sb.append(rs.getString(1)).append(System.lineSeparator());
            }
            return sb.substring(0,sb.length() - 1);
        } catch (SQLException e) {
            e.printStackTrace();
        } return null;
    }
}
