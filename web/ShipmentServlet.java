package com.coldchain.controller;

import com.coldchain.dao.JdbcShipmentDao;
import com.coldchain.model.Shipment;
import com.coldchain.service.AlertService;
import com.coldchain.service.ShipmentService;
import com.coldchain.util.MockData;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class ShipmentServlet extends HttpServlet {

    private final JdbcShipmentDao dao = new JdbcShipmentDao();

    private final ShipmentService svc =
            new ShipmentService(
                    dao,
                    MockData.products(),
                    new AlertService()
            );

    private boolean isAuthorized(HttpServletRequest req) {
        HttpSession session = req.getSession(false);

        if (session == null) {
            return false;
        }

        String userId = (String) session.getAttribute("userId");
        String role = (String) session.getAttribute("userRole");

        return userId != null
                && ("ADMIN".equalsIgnoreCase(role)
                || "VIEWER".equalsIgnoreCase(role));
    }

    private boolean isAdmin(HttpServletRequest req) {
        HttpSession session = req.getSession(false);

        return session != null
                && session.getAttribute("userId") != null
                && "ADMIN".equalsIgnoreCase(
                        (String) session.getAttribute("userRole")
                );
    }

    private String jsonEscape(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String shipmentToJson(Shipment s) {
        return "{"
                + "\"id\":\"" + jsonEscape(s.getId()) + "\","
                + "\"product\":\"" + jsonEscape(s.getProduct()) + "\","
                + "\"source\":\"" + jsonEscape(s.getSource()) + "\","
                + "\"destination\":\"" + jsonEscape(s.getDestination()) + "\","
                + "\"vehicle\":\"" + jsonEscape(s.getVehicle()) + "\","
                + "\"driver\":\"" + jsonEscape(s.getDriver()) + "\","
                + "\"status\":\"" + jsonEscape(s.getStatus()) + "\","
                + "\"temperature\":" + s.getTemperature() + ","
                + "\"humidity\":" + s.getHumidity() + ","
                + "\"quality\":" + s.getQuality()
                + "}";
    }

    private void sendJson(
            HttpServletResponse res,
            int status,
            String json
    ) throws IOException {

        res.setStatus(status);
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write(json);
    }

    @Override
    protected void doGet(
            HttpServletRequest req,
            HttpServletResponse res
    ) throws IOException {

        if (!isAuthorized(req)) {
            sendJson(res, 401,
                    "{\"success\":false,\"message\":\"Please log in first.\"}");
            return;
        }

        try {
            List<Shipment> shipments = svc.list();

            String json = shipments.stream()
                    .map(this::shipmentToJson)
                    .collect(Collectors.joining(",", "[", "]"));

            sendJson(res, 200, json);

        } catch (Exception e) {
            sendJson(res, 500,
                    "{\"success\":false,\"message\":\"Unable to load shipments.\"}");
        }
    }

    @Override
    protected void doPost(
            HttpServletRequest req,
            HttpServletResponse res
    ) throws IOException {

        if (!isAdmin(req)) {
            sendJson(res, 403,
                    "{\"success\":false,\"message\":\"Administrator permission required.\"}");
            return;
        }

        req.setCharacterEncoding("UTF-8");

        try {
            StringBuilder body = new StringBuilder();

            try (BufferedReader reader = req.getReader()) {
                String line;

                while ((line = reader.readLine()) != null) {
                    body.append(line);
                }
            }

            String json = body.toString();

            String id = getString(json, "id");
            String product = getString(json, "product");
            String source = getString(json, "source");
            String destination = getString(json, "destination");
            String vehicle = getString(json, "vehicle");
            String driver = getString(json, "driver");
            String status = getString(json, "status");

            double temperature = getNumber(json, "temperature");
            double humidity = getNumber(json, "humidity");
            double quality = getNumber(json, "quality");

            if (id.isEmpty()
                    || product.isEmpty()
                    || source.isEmpty()
                    || destination.isEmpty()
                    || vehicle.isEmpty()
                    || driver.isEmpty()
                    || status.isEmpty()) {

                sendJson(res, 400,
                        "{\"success\":false,\"message\":\"Please fill in all required fields.\"}");
                return;
            }

            if (!id.matches("[A-Za-z0-9_-]{1,10}")) {
                sendJson(res, 400,
                        "{\"success\":false,\"message\":\"Shipment ID must contain at most 10 letters, numbers, underscores, or hyphens.\"}");
                return;
            }

            if (!Double.isFinite(temperature)
                    || !Double.isFinite(humidity)
                    || !Double.isFinite(quality)
                    || humidity < 0
                    || humidity > 100
                    || quality < 0
                    || quality > 100) {

                sendJson(res, 400,
                        "{\"success\":false,\"message\":\"Enter valid temperature, humidity, and quality values.\"}");
                return;
            }

            if (dao.findById(id).isPresent()) {
                sendJson(res, 409,
                        "{\"success\":false,\"message\":\"That shipment ID already exists.\"}");
                return;
            }

            Shipment shipment = new Shipment(
                    id,
                    product,
                    source,
                    destination,
                    vehicle,
                    driver
            );

            shipment.setStatus(status);
            shipment.setTemperature(temperature);
            shipment.setHumidity(humidity);
            shipment.setQuality(quality);

            dao.save(shipment);

            sendJson(res, 201,
                    "{\"success\":true,\"message\":\"Shipment added successfully.\"}");

        } catch (Exception e) {
            sendJson(res, 500,
                    "{\"success\":false,\"message\":\"Unable to save shipment. Check the database connection and server logs.\"}");
        }
    }

    private String getString(String json, String key) {
        String marker = "\"" + key + "\"";
        int start = json.indexOf(marker);

        if (start < 0) {
            return "";
        }

        start = json.indexOf(':', start);

        if (start < 0) {
            return "";
        }

        start = json.indexOf('"', start);

        if (start < 0) {
            return "";
        }

        int end = start + 1;
        boolean escaped = false;

        while (end < json.length()) {
            char c = json.charAt(end);

            if (escaped) {
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                break;
            }

            end++;
        }

        if (end >= json.length()) {
            return "";
        }

        return json.substring(start + 1, end)
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .trim();
    }

    private double getNumber(String json, String key) {
        String marker = "\"" + key + "\"";
        int start = json.indexOf(marker);

        if (start < 0) {
            return Double.NaN;
        }

        start = json.indexOf(':', start);

        if (start < 0) {
            return Double.NaN;
        }

        start++;

        while (start < json.length()
                && Character.isWhitespace(json.charAt(start))) {
            start++;
        }

        int end = start;

        while (end < json.length()
                && "-+.0123456789eE".indexOf(json.charAt(end)) >= 0) {
            end++;
        }

        try {
            return Double.parseDouble(json.substring(start, end));
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    @Override
    protected void doDelete(
            HttpServletRequest req,
            HttpServletResponse res
    ) throws IOException {

        if (!isAdmin(req)) {
            sendJson(res, 403,
                    "{\"success\":false,\"message\":\"Administrator permission required.\"}");
            return;
        }

        String path = req.getPathInfo();

        if (path == null || path.length() < 2) {
            sendJson(res, 400,
                    "{\"success\":false,\"message\":\"Shipment ID is required.\"}");
            return;
        }

        String id = path.substring(1);

        try {
            if (!dao.findById(id).isPresent()) {
                sendJson(res, 404,
                        "{\"success\":false,\"message\":\"Shipment not found.\"}");
                return;
            }

            svc.delete(id);

            sendJson(res, 200,
                    "{\"success\":true,\"message\":\"Shipment deleted successfully.\"}");

        } catch (Exception e) {
            sendJson(res, 500,
                    "{\"success\":false,\"message\":\"Unable to delete shipment. Related records may exist.\"}");
        }
    }
}