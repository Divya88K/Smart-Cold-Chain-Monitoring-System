package com.coldchain.dao;
import com.coldchain.exception.ColdChainException;
import com.coldchain.model.Shipment;
import com.coldchain.util.DBConnection;
import java.sql.*;
import java.util.*;
/** JDBC implementation against the Shipments table (see sql/schema.sql). */
public class JdbcShipmentDao implements Dao<Shipment> {
    private static final String COLS = "id,product,source,destination,vehicle,driver,status,temperature,humidity,quality";
    private Shipment map(ResultSet r) throws SQLException {
        Shipment s = new Shipment(r.getString(1), r.getString(2), r.getString(3), r.getString(4), r.getString(5), r.getString(6));
        s.setStatus(r.getString(7)); s.setTemperature(r.getDouble(8)); s.setHumidity(r.getDouble(9)); s.setQuality(r.getDouble(10));
        return s;
    }
    public List<Shipment> findAll() {
        List<Shipment> out = new ArrayList<>();
        try (Connection c = DBConnection.getConnection(); Statement st = c.createStatement(); ResultSet r = st.executeQuery("SELECT " + COLS + " FROM Shipments")) {
            while (r.next()) out.add(map(r));
        } catch (SQLException e) { throw new ColdChainException("Failed to load shipments", e); }
        return out;
    }
    public Optional<Shipment> findById(String id) {
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement("SELECT " + COLS + " FROM Shipments WHERE id=?")) {
            p.setString(1, id); ResultSet r = p.executeQuery();
            return r.next() ? Optional.of(map(r)) : Optional.empty();
        } catch (SQLException e) { throw new ColdChainException("Failed to load shipment " + id, e); }
    }
    public void save(Shipment s) {
        String sql = "INSERT INTO Shipments(" + COLS + ") VALUES(?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE status=VALUES(status),temperature=VALUES(temperature),humidity=VALUES(humidity),quality=VALUES(quality)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, s.getId()); p.setString(2, s.getProduct()); p.setString(3, s.getSource()); p.setString(4, s.getDestination());
            p.setString(5, s.getVehicle()); p.setString(6, s.getDriver()); p.setString(7, s.getStatus());
            p.setDouble(8, s.getTemperature()); p.setDouble(9, s.getHumidity()); p.setDouble(10, s.getQuality());
            p.executeUpdate();
        } catch (SQLException e) { throw new ColdChainException("Failed to save shipment", e); }
    }
    public void delete(String id) {
        try (Connection c = DBConnection.getConnection(); PreparedStatement p = c.prepareStatement("DELETE FROM Shipments WHERE id=?")) {
            p.setString(1, id); p.executeUpdate();
        } catch (SQLException e) { throw new ColdChainException("Failed to delete shipment", e); }
    }
}
