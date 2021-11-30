package ru.gb.file.manager.server;

import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class DbAuthProvider implements AuthProvider{

    private final String dbUrl = "jdbc:postgresql://localhost:5432/postgres";
    private final String dbUser = "postgres";
    private final String dbPassword = "Pass2020!";

    private Connection conn;
    private Statement stmt;

    @Override
    public void start() {
        try {
            conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
            stmt = conn.createStatement();
            stmt.executeUpdate(
                    "create table if not exists users (" +
                            "id serial primary key," +
                            "login text," +
                            "password text)"
            );
        } catch (SQLException e) {
            log.error("", e);
        }
    }

    @Override
    public void stop() {
        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (SQLException e) {
            log.error("", e);
        }

        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            log.error("", e);
        }
    }

    @Override
    public String[] getUsers(String login) {
        try (PreparedStatement ps =
                     conn.prepareStatement(
                             "SELECT * " +
                                     "FROM users " +
                                     "WHERE login" + " = ?"
                     );
        ) {
            ps.setString(1, login);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String[] results = new String[2];
                results[0] = rs.getString("login");
                results[1] = rs.getString("password");
                return results;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<String> getAllUsers() {
        try {
            String query = "SELECT login FROM users;";
            ResultSet rs = stmt.executeQuery(query);
            List<String> results = new ArrayList<>();
            while (rs.next()) {
                results.add(rs.getString("login"));
            }
            return results;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean addUserRecord(String login, String password) {
        try (PreparedStatement ps =
                     conn.prepareStatement(
                             "insert into users (login, password) " +
                                     "values (?, ?)"
                     );
        ) {
            ps.setString(1, login);
            ps.setString(2, password);
            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }
}
