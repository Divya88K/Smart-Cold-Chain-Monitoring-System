package com.coldchain.ui;
import com.coldchain.exception.ColdChainException;
import com.coldchain.model.*;
import com.coldchain.service.*;
import com.coldchain.util.*;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.List;
public class MainFrame extends JFrame {
    private final ShipmentService svc; private final AlertService alerts;
    private final DefaultTableModel sm = model("ID","Product","Route","Vehicle","Driver","Status","Temp C","Humidity %","Quality","Risk");
    private final DefaultTableModel am = model("Alert ID","Shipment","Type","Severity","Time","Status");
    private final JTable st = new JTable(sm), at = new JTable(am);
    private final JTextField search = new JTextField(16);
    private final JLabel kpi = new JLabel();
    private final SensorSimulator sim;
    private static DefaultTableModel model(String... c) {
        return new DefaultTableModel(c, 0) { public boolean isCellEditable(int r, int k) { return false; } };
    }
    public MainFrame(ShipmentService svc, AlertService alerts) {
        super("Smart Cold Chain Monitoring and Shipment Management System");
        this.svc = svc; this.alerts = alerts;
        kpi.setFont(kpi.getFont().deriveFont(Font.BOLD, 15f)); kpi.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        at.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                Component x = super.getTableCellRendererComponent(t, v, s, f, r, c);
                String sev = (String) t.getValueAt(r, 3);
                if (!s) x.setForeground(sev.equals("CRITICAL") ? new Color(0xDC2626) : sev.equals("WARNING") ? new Color(0xD97706) : new Color(0x16A34A));
                return x;
            }
        });
        JPanel sp = new JPanel(new BorderLayout());
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton add = new JButton("Register Shipment"), del = new JButton("Delete"), stat = new JButton("Change Status"), csv = new JButton("Export CSV");
        bar.add(new JLabel("Search:")); bar.add(search); bar.add(add); bar.add(stat); bar.add(del); bar.add(csv);
        sp.add(bar, BorderLayout.NORTH); sp.add(new JScrollPane(st));
        JPanel ap = new JPanel(new BorderLayout()); JButton res = new JButton("Resolve Selected");
        ap.add(res, BorderLayout.NORTH); ap.add(new JScrollPane(at));
        JTabbedPane tabs = new JTabbedPane(); tabs.add("Shipments / Live Monitoring", sp); tabs.add("Alerts", ap);
        add(kpi, BorderLayout.NORTH); add(tabs);
        search.addKeyListener(new KeyAdapter() { public void keyReleased(KeyEvent e) { refresh(); } });
        add.addActionListener(e -> register());
        del.addActionListener(e -> withSelected(id -> {
            if (JOptionPane.showConfirmDialog(this, "Are you sure you want to delete Shipment " + id + "?", "Confirm", JOptionPane.YES_NO_OPTION) == 0) svc.delete(id);
        }));
        stat.addActionListener(e -> withSelected(id -> {
            Object s = JOptionPane.showInputDialog(this, "New status for " + id, "Status", JOptionPane.PLAIN_MESSAGE, null,
                new String[]{"Preparing","In Transit","Delivered","Delayed","Breached","Cancelled"}, "In Transit");
            if (s != null) svc.setStatus(id, (String) s);
        }));
        csv.addActionListener(e -> run(() -> { File f = new File("shipments.csv"); CsvUtil.exportShipments(svc.list(), f);
            JOptionPane.showMessageDialog(this, "Exported to " + f.getAbsolutePath()); }));
        res.addActionListener(e -> { int r = at.getSelectedRow(); if (r >= 0) { alerts.resolve((String) am.getValueAt(r, 0)); refresh(); } });
        refresh();
        sim = new SensorSimulator(svc, () -> SwingUtilities.invokeLater(this::refresh));
        Thread th = new Thread(sim, "sensor-sim"); th.setDaemon(true); th.start();
        addWindowListener(new WindowAdapter() { public void windowClosing(WindowEvent e) { sim.stop(); } });
        setDefaultCloseOperation(EXIT_ON_CLOSE); setSize(1100, 600); setLocationRelativeTo(null);
    }
    private void run(Runnable r) {
        try { r.run(); refresh(); } catch (ColdChainException ex) { JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); }
    }
    private void withSelected(java.util.function.Consumer<String> c) {
        int r = st.getSelectedRow();
        if (r < 0) { JOptionPane.showMessageDialog(this, "Select a shipment first"); return; }
        String id = (String) sm.getValueAt(r, 0); run(() -> c.accept(id));
    }
    private void register() {
        JTextField id = new JTextField(); JComboBox<String> pr = new JComboBox<>(svc.productNames().toArray(new String[0]));
        JComboBox<String> src = new JComboBox<>(MockData.CITIES), dst = new JComboBox<>(MockData.CITIES);
        dst.setSelectedIndex(1); JTextField veh = new JTextField("VEH01"), drv = new JTextField("Driver 1");
        Object[] f = {"Shipment ID *", id, "Product", pr, "Source", src, "Destination", dst, "Vehicle", veh, "Driver", drv};
        if (JOptionPane.showConfirmDialog(this, f, "Register Shipment", JOptionPane.OK_CANCEL_OPTION) == 0)
            run(() -> { Shipment s = new Shipment(id.getText().trim(), (String) pr.getSelectedItem(), (String) src.getSelectedItem(),
                (String) dst.getSelectedItem(), veh.getText(), drv.getText()); svc.add(s); });
    }
    private void refresh() {
        String id = st.getSelectedRow() >= 0 ? (String) sm.getValueAt(st.getSelectedRow(), 0) : null;
        String q = search.getText().toLowerCase();
        List<Shipment> all = svc.list();
        sm.setRowCount(0);
        svc.filter(s -> (s.getId() + s.getProduct() + s.getVehicle() + s.getDriver() + s.getStatus()).toLowerCase().contains(q)).forEach(s -> {
            int r = ColdChainRules.risk(s.getQuality());
            sm.addRow(new Object[]{s.getId(), s.getProduct(), s.getSource() + " -> " + s.getDestination(), s.getVehicle(), s.getDriver(),
                s.getStatus(), s.getTemperature(), s.getHumidity(), String.format("%.1f", s.getQuality()), r + "% " + ColdChainRules.riskClass(r)});
        });
        if (id != null) for (int i = 0; i < sm.getRowCount(); i++) if (sm.getValueAt(i, 0).equals(id)) st.setRowSelectionInterval(i, i);
        am.setRowCount(0);
        alerts.all().forEach(a -> am.addRow(new Object[]{a.getId(), a.getShipmentId(), a.getType(), a.getSeverity().name(), a.getTime(), a.getStatus()}));
        long act = all.stream().filter(s -> s.getStatus().equals("In Transit") || s.getStatus().equals("Breached")).count();
        long br = all.stream().filter(s -> s.getStatus().equals("Breached")).count();
        double avg = all.stream().mapToDouble(Shipment::getQuality).average().orElse(0);
        kpi.setText(String.format("Total: %d   |   Active: %d   |   Breached: %d   |   Active Alerts: %d   |   Avg Quality: %.1f%%", all.size(), act, br, alerts.active(), avg));
    }
}
