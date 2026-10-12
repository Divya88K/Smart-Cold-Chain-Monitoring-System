package com.coldchain.controller;

import com.coldchain.util.DBConnection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;


public class AuthServlet extends HttpServlet {

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String email = request.getParameter("email");
        String password = request.getParameter("password");

        if (email == null || password == null
                || email.isBlank() || password.isBlank()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"Email and password are required\"}");
            return;
        }

        String sql = """
                SELECT id, name, email, role, status, password_hash
                FROM Users
                WHERE email = ?
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, email.trim());

            try (ResultSet result = statement.executeQuery()) {

                if (!result.next()) {
                    sendUnauthorized(response);
                    return;
                }

                String id = result.getString("id");
                String name = result.getString("name");
                String role = result.getString("role");
                String status = result.getString("status");
                String storedHash = result.getString("password_hash");

                if (!"ACTIVE".equalsIgnoreCase(status)
                        || storedHash == null
                        || !storedHash.equals(sha256(password))) {
                    sendUnauthorized(response);
                    return;
                }

                HttpSession oldSession = request.getSession(false);
                if (oldSession != null) {
                    oldSession.invalidate();
                }

                HttpSession session = request.getSession(true);
                session.setAttribute("userId", id);
                session.setAttribute("userName", name);
                session.setAttribute("userEmail", email.trim());
                session.setAttribute("userRole", role);
                session.setMaxInactiveInterval(30 * 60);

                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write(
                        "{\"success\":true,"
                        + "\"message\":\"Login successful\","
                        + "\"name\":\"" + escapeJson(name) + "\","
                        + "\"role\":\"" + escapeJson(role) + "\"}");
            }

        } catch (Exception exception) {
            getServletContext().log("Login request failed", exception);
            response.setStatus(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"Unable to process login\"}");
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

    private void sendUnauthorized(HttpServletResponse response)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write(
                "{\"success\":false,\"message\":\"Invalid email or password, or inactive account\"}");
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
    }
}