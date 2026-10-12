package com.coldchain.controller;
/**
 * REST contract planned for the Servlet layer (see web/ShipmentServlet.java):
 *  GET/POST /api/shipments   GET/PUT/DELETE /api/shipments/{id}
 *  GET/POST /api/products    GET /api/sensors/{shipmentId}
 *  GET/POST /api/alerts      GET /api/reports
 * Flow: Servlet -> ShipmentService -> Dao -> JDBC -> MySQL
 */
public final class ApiPlaceholder { private ApiPlaceholder() {} }
