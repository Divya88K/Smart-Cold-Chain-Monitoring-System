package com.coldchain.model;
public class Shipment extends Entity {
    private final String product, source, destination;
    private String vehicle, driver, status = "Preparing";
    private double temperature, humidity, quality = 100;
    public Shipment(String id, String product, String source, String destination, String vehicle, String driver) {
        super(id); this.product = product; this.source = source; this.destination = destination;
        this.vehicle = vehicle; this.driver = driver;
    }
    public String getProduct() { return product; }
    public String getSource() { return source; }
    public String getDestination() { return destination; }
    public String getVehicle() { return vehicle; }
    public String getDriver() { return driver; }
    public String getStatus() { return status; }
    public void setStatus(String s) { status = s; }
    public double getTemperature() { return temperature; }
    public void setTemperature(double t) { temperature = t; }
    public double getHumidity() { return humidity; }
    public void setHumidity(double h) { humidity = h; }
    public double getQuality() { return quality; }
    public void setQuality(double q) { quality = Math.max(0, Math.min(100, q)); }
}
