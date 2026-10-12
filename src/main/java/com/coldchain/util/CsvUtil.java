package com.coldchain.util;
import com.coldchain.exception.ColdChainException;
import com.coldchain.model.Shipment;
import java.io.*;
import java.util.List;
/** File handling + CSV processing. */
public final class CsvUtil {
    private CsvUtil() {}
    public static void exportShipments(List<Shipment> list, File f) {
        try (PrintWriter w = new PrintWriter(new FileWriter(f))) {
            w.println("id,product,source,destination,vehicle,driver,status,temperature,humidity,quality");
            list.forEach(s -> w.printf("%s,%s,%s,%s,%s,%s,%s,%.1f,%.0f,%.1f%n", s.getId(), s.getProduct(), s.getSource(),
                s.getDestination(), s.getVehicle(), s.getDriver(), s.getStatus(), s.getTemperature(), s.getHumidity(), s.getQuality()));
        } catch (IOException e) { throw new ColdChainException("CSV export failed", e); }
    }
}
