package com.coldchain.controller;

import com.coldchain.util.DBConnection;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;


public class SignupServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String password = request.getParameter("password");

        if (name == null || email == null || password == null
                || name.isBlank() || email.isBlank() || password.isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"All fields are required\"}");
            return;
        }

        name = name.trim();
        email = email.trim().toLowerCase();

        if (name.length() > 80 || email.length() > 100
                || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
                || password.length() < 8) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"Enter valid details and a password of at least 8 characters\"}");
            return;
        }

        String checkSql = "SELECT id FROM Users WHERE email = ?";
        String insertSql = """
                INSERT INTO Users
                    (id, name, email, role, status, password_hash)
                VALUES (?, ?, ?, 'VIEWER', 'PENDING', ?)
                """;

        try (Connection connection = DBConnection.getConnection()) {

            try (PreparedStatement check =
                    connection.prepareStatement(checkSql)) {

                check.setString(1, email);

                try (ResultSet result = check.executeQuery()) {
                    if (result.next()) {
                        response.setStatus(
                                HttpServletResponse.SC_CONFLICT);
                        response.getWriter().write(
                                "{\"success\":false,\"message\":\"This email is already registered\"}");
                        return;
                    }
                }
            }

            String id;

            try (Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery(
                         "SELECT COALESCE(MAX(CAST(SUBSTRING(id, 4) AS UNSIGNED)), 0) + 1 AS next_id FROM Users WHERE id LIKE 'USR%'")) {

                result.next();
                id = String.format("USR%07d", result.getLong("next_id"));
            }

            try (PreparedStatement insert =
                    connection.prepareStatement(insertSql)) {

                insert.setString(1, id);
                insert.setString(2, name);
                insert.setString(3, email);
                insert.setString(4, sha256(password));

                insert.executeUpdate();
            }

            response.setStatus(HttpServletResponse.SC_CREATED);
            response.getWriter().write(
                    "{\"success\":true,"
                    + "\"message\":\"Registration successful. Please wait for administrator approval.\"}");

        } catch (Exception exception) {
            getServletContext().log("Signup failed", exception);

            response.setStatus(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"Registration could not be completed\"}");
        }
    }

    private String sha256(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(
                value.getBytes(StandardCharsets.UTF_8));

        StringBuilder hex = new StringBuilder();

        for (byte b : hash) {
            hex.append(String.format("%02x", b & 0xff));
        }

        return hex.toString();
    }
}