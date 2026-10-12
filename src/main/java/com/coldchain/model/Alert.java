package com.coldchain.model;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
public class Alert extends Entity {
    private final String shipmentId, type, time;
    private final Severity severity;
    private String status = "Active";
    public Alert(String id, String shipmentId, String type, Severity severity) {
        super(id); this.shipmentId = shipmentId; this.type = type; this.severity = severity;
        this.time = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"));
    }
    public String getShipmentId() { return shipmentId; }
    public String getType() { return type; }
    public Severity getSeverity() { return severity; }
    public String getTime() { return time; }
    public String getStatus() { return status; }
    public void setStatus(String s) { status = s; }
}
