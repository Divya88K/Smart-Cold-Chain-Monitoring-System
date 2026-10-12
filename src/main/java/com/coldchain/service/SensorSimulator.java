package com.coldchain.service;
import com.coldchain.model.*;
import java.util.Random;
/** Simulated IoT feed running on its own thread (replace with MQTT consumer later). */
public class SensorSimulator implements Runnable {
    private final ShipmentService svc; private final Runnable onUpdate;
    private volatile boolean running = true; private final Random rnd = new Random();
    public SensorSimulator(ShipmentService svc, Runnable onUpdate) { this.svc = svc; this.onUpdate = onUpdate; }
    public void stop() { running = false; }
    @Override public void run() {
        while (running) {
            for (Shipment s : svc.list()) {
                if (!s.getStatus().equals("In Transit") && !s.getStatus().equals("Breached")) continue;
                Product p = svc.product(s.getProduct());
                double mid = (p.getTmin() + p.getTmax()) / 2;
                double t = s.getTemperature() + (rnd.nextDouble() - 0.5) * 2.5 + (mid - s.getTemperature()) * 0.08;
                double h = s.getHumidity() + (rnd.nextDouble() - 0.5) * 5;
                svc.applyReading(s, Math.round(t * 10) / 10.0, Math.round(h));
            }
            onUpdate.run();
            try { Thread.sleep(3000); } catch (InterruptedException e) { return; }
        }
    }
}
