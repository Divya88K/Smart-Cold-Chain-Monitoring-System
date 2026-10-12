package com.coldchain.util;
import com.coldchain.model.*;
import java.util.*;
public final class MockData {
    public static final String[] CITIES = {"Bengaluru","Chennai","Hyderabad","Mumbai","Pune","Mysuru","Coimbatore"};
    private MockData() {}
    public static Map<String, Product> products() {
        Map<String, Product> m = new LinkedHashMap<>();
        Object[][] d = {{"Insulin","Biologics",2,8,35,60},{"Vaccines","Biologics",2,8,35,65},{"Blood Plasma","Blood Products",-30,-18,30,60},
            {"Antibiotics","Pharma",2,8,35,65},{"Frozen Food","Perishable",-25,-18,30,70}};
        int i = 101;
        for (Object[] r : d) m.put((String) r[0], new Product("PRD" + i++, (String) r[0], (String) r[1],
            (int) r[2], (int) r[3], (int) r[4], (int) r[5]));
        return m;
    }
    public static List<Shipment> shipments(Map<String, Product> p) {
        List<Shipment> l = new ArrayList<>();
        String[] names = p.keySet().toArray(new String[0]);
        for (int i = 0; i < 8; i++) {
            Product pr = p.get(names[i % names.length]);
            Shipment s = new Shipment("SHP" + (1010 + i), pr.getName(), CITIES[i % 7], CITIES[(i + 3) % 7], "VEH0" + (i + 1), "Driver " + (i + 1));
            s.setStatus(i == 5 ? "Preparing" : i == 6 ? "Delivered" : "In Transit");
            s.setTemperature((pr.getTmin() + pr.getTmax()) / 2); s.setHumidity((pr.getHmin() + pr.getHmax()) / 2);
            l.add(s);
        }
        return l;
    }
}
