package com.coldchain.service;
import com.coldchain.dao.Dao;
import com.coldchain.exception.ColdChainException;
import com.coldchain.model.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
public class ShipmentService {
    private final Dao<Shipment> dao;
    private final Map<String, Product> products;
    private final AlertService alerts;
    private final Map<String, Severity> lastSeverity = new HashMap<>();
    public ShipmentService(Dao<Shipment> dao, Map<String, Product> products, AlertService alerts) {
        this.dao = dao; this.products = products; this.alerts = alerts;
    }
    public List<Shipment> list() { return dao.findAll(); }
    public List<Shipment> filter(Predicate<Shipment> p) { return dao.findAll().stream().filter(p).collect(Collectors.toList()); }
    public Set<String> productNames() { return products.keySet(); }
    public Product product(String name) { return products.get(name); }
    public void add(Shipment s) {
        if (s.getId() == null || s.getId().isBlank()) throw new ColdChainException("Shipment ID is required");
        if (dao.findById(s.getId()).isPresent()) throw new ColdChainException("Shipment " + s.getId() + " already exists");
        if (s.getSource().equals(s.getDestination())) throw new ColdChainException("Source and destination must differ");
        Product p = products.get(s.getProduct());
        s.setTemperature((p.getTmin() + p.getTmax()) / 2); s.setHumidity((p.getHmin() + p.getHmax()) / 2);
        dao.save(s);
    }
    public void delete(String id) {
        dao.findById(id).orElseThrow(() -> new ColdChainException("Shipment not found: " + id));
        dao.delete(id);
    }
    public void setStatus(String id, String status) {
        Shipment s = dao.findById(id).orElseThrow(() -> new ColdChainException("Shipment not found: " + id));
        s.setStatus(status); dao.save(s);
    }
    /** Called by the sensor thread: applies rules, raises alerts, updates quality. */
    public synchronized void applyReading(Shipment s, double temp, double hum) {
        Product p = products.get(s.getProduct());
        s.setTemperature(temp); s.setHumidity(hum);
        Severity t = ColdChainRules.temperature(temp, p), h = ColdChainRules.humidity(hum, p);
        Severity worst = t.compareTo(h) >= 0 ? t : h;
        Severity prev = lastSeverity.getOrDefault(s.getId(), Severity.SAFE);
        if (worst.compareTo(prev) > 0) {
            String type = t.compareTo(h) >= 0 ? (temp > p.getTmax() ? "High Temperature" : "Low Temperature") : "High Humidity";
            alerts.raise(s.getId(), type, worst);
        }
        lastSeverity.put(s.getId(), worst);
        if (worst == Severity.CRITICAL) s.setStatus("Breached");
        else if (s.getStatus().equals("Breached") && worst == Severity.SAFE) s.setStatus("In Transit");
        s.setQuality(s.getQuality() + (worst == Severity.CRITICAL ? -2.5 : worst == Severity.WARNING ? -0.8 : 0.3));
        dao.save(s);
    }
}
