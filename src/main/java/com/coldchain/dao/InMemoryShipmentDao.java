package com.coldchain.dao;
import com.coldchain.model.Shipment;
import java.util.*;
public class InMemoryShipmentDao implements Dao<Shipment> {
    private final Map<String, Shipment> store = new LinkedHashMap<>();
    public synchronized List<Shipment> findAll() { return new ArrayList<>(store.values()); }
    public synchronized Optional<Shipment> findById(String id) { return Optional.ofNullable(store.get(id)); }
    public synchronized void save(Shipment s) { store.put(s.getId(), s); }
    public synchronized void delete(String id) { store.remove(id); }
}
