package com.coldchain.controller;

import com.coldchain.util.DBConnection;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Locale;
import java.util.UUID;

public class UsersServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private boolean isAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        return session != null
                && "ADMIN".equals(session.getAttribute("userRole"));
    }

    private void sendJson(
            HttpServletResponse response,
            int status,
            String message) throws IOException {

        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        response.getWriter().write(
                "{\"success\":" + (status >= 200 && status < 300)
                + ",\"message\":\"" + escape(message) + "\"}"
        );
    }

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        if (!isAdmin(request)) {
            sendJson(response, HttpServletResponse.SC_FORBIDDEN,
                    "Administrator permission required");
            return;
        }

        String sql =
                "SELECT id, name, email, role, status FROM Users ORDER BY id";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            StringBuilder json = new StringBuilder("[");
            boolean first = true;

            while (result.next()) {
                if (!first) {
                    json.append(",");
                }

                json.append("{")
                    .append("\"id\":\"")
                    .append(escape(result.getString("id"))).append("\",")
                    .append("\"name\":\"")
                    .append(escape(result.getString("name"))).append("\",")
                    .append("\"email\":\"")
                    .append(escape(result.getString("email"))).append("\",")
                    .append("\"role\":\"")
                    .append(escape(result.getString("role"))).append("\",")
                    .append("\"status\":\"")
                    .append(escape(result.getString("status"))).append("\"")
                    .append("}");

                first = false;
            }

            json.append("]");
            response.getWriter().write(json.toString());

        } catch (Exception e) {
            getServletContext().log("Unable to retrieve users", e);
            sendJson(response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unable to retrieve users");
        }
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        request.setCharacterEncoding("UTF-8");

        if (!isAdmin(request)) {
            sendJson(response, HttpServletResponse.SC_FORBIDDEN,
                    "Administrator permission required");
            return;
        }

        String path = request.getPathInfo();
        String action = request.getParameter("action");
        String userId = request.getParameter("userId");

        if (path == null) {
            sendJson(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Missing operation");
            return;
        }

        try {
            switch (path.toLowerCase(Locale.ROOT)) {

                case "/approve":
                    updateUserStatus(userId, "ACTIVE", response);
                    break;

                case "/reject":
                    updateUserStatus(userId, "REJECTED", response);
                    break;

                case "/deactivate":
                    updateUserStatus(userId, "INACTIVE", response);
                    break;

                case "/role":
                    updateUserRole(userId,
                            request.getParameter("role"), response);
                    break;

                case "/admin":
                    createAdmin(request, response);
                    break;

                default:
                    sendJson(response, HttpServletResponse.SC_NOT_FOUND,
                            "Unknown user operation");
            }

        } catch (Exception e) {
            getServletContext().log("User management operation failed", e);
            sendJson(response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "User management operation failed");
        }
    }

    private void updateUserStatus(
            String userId,
            String status,
            HttpServletResponse response) throws Exception {

        if (userId == null || userId.isBlank()) {
            sendJson(response, HttpServletResponse.SC_BAD_REQUEST,
                    "User ID is required");
            return;
        }

        String sql = "UPDATE Users SET status = ? WHERE id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, status);
            statement.setString(2, userId);

            if (statement.executeUpdate() == 0) {
                sendJson(response, HttpServletResponse.SC_NOT_FOUND,
                        "User not found");
                return;
            }
        }

        sendJson(response, HttpServletResponse.SC_OK,
                "User status updated to " + status);
    }

    private void updateUserRole(
            String userId,
            String role,
            HttpServletResponse response) throws Exception {

        if (userId == null || userId.isBlank()
                || role == null
                || !(role.equals("ADMIN") || role.equals("VIEWER"))) {

            sendJson(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Provide a user ID and role ADMIN or VIEWER");
            return;
        }

        String sql = "UPDATE Users SET role = ? WHERE id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, role);
            statement.setString(2, userId);

            if (statement.executeUpdate() == 0) {
                sendJson(response, HttpServletResponse.SC_NOT_FOUND,
                        "User not found");
                return;
            }
        }

        sendJson(response, HttpServletResponse.SC_OK,
                "User role updated to " + role);
    }

    private void createAdmin(
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String password = request.getParameter("password");

        if (name == null || name.isBlank()
                || email == null || email.isBlank()
                || password == null || password.length() < 8) {

            sendJson(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Name, email, and a password of at least 8 characters are required");
            return;
        }

        String id = "USR" + UUID.randomUUID()
                .toString().replace("-", "")
                .substring(0, 7).toUpperCase(Locale.ROOT);

        String sql = """
                INSERT INTO Users (id, name, email, role, status, password_hash)
                VALUES (?, ?, ?, 'ADMIN', 'ACTIVE', ?)
                """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, id);
            statement.setString(2, name.trim());
            statement.setString(3, email.trim().toLowerCase(Locale.ROOT));
            statement.setString(4, sha256(password));

            statement.executeUpdate();

        } catch (java.sql.SQLIntegrityConstraintViolationException e) {
            sendJson(response, HttpServletResponse.SC_CONFLICT,
                    "This email or user ID already exists");
            return;
        }

        sendJson(response, HttpServletResponse.SC_OK,
                "Administrator account created successfully");
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

    private String escape(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}