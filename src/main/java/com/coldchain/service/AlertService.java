package com.coldchain.service;
import com.coldchain.model.*;
import java.util.*;
public class AlertService {
    private final Deque<Alert> alerts = new ArrayDeque<>();   // newest first
    private int seq = 0;
    public synchronized Alert raise(String shipmentId, String type, Severity sev) {
        Alert a = new Alert(String.format("ALT%03d", ++seq), shipmentId, type, sev);
        alerts.addFirst(a); return a;
    }
    public synchronized List<Alert> all() { return new ArrayList<>(alerts); }
    public synchronized void resolve(String id) { alerts.stream().filter(a -> a.getId().equals(id)).forEach(a -> a.setStatus("Resolved")); }
    public synchronized long active() { return alerts.stream().filter(a -> !a.getStatus().equals("Resolved")).count(); }
}
