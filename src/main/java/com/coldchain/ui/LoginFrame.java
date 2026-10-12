package com.coldchain.ui;
import com.coldchain.dao.*;
import com.coldchain.model.*;
import com.coldchain.service.*;
import com.coldchain.util.MockData;
import javax.swing.*;
import java.awt.*;
import java.util.Map;
public class LoginFrame extends JFrame {
    public LoginFrame() {
        super("Smart Cold Chain - Login");
        JTextField u = new JTextField("admin@coldchain.com", 20);
        JPasswordField p = new JPasswordField("Admin@123", 20);
        JLabel err = new JLabel(" "); err.setForeground(Color.RED);
        JButton go = new JButton("Login");
        JPanel f = new JPanel(new GridLayout(0, 1, 4, 4)); f.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        JLabel t = new JLabel("Smart Cold Chain Monitoring System"); t.setFont(t.getFont().deriveFont(Font.BOLD, 16f));
        f.add(t); f.add(new JLabel("Email / Username")); f.add(u); f.add(new JLabel("Password")); f.add(p); f.add(err); f.add(go);
        add(f);
        go.addActionListener(e -> {
            if (u.getText().trim().equalsIgnoreCase("admin@coldchain.com") && new String(p.getPassword()).equals("Admin@123")) {
                Map<String, Product> products = MockData.products();
                Dao<Shipment> dao = new InMemoryShipmentDao();     // swap for new JdbcShipmentDao() when MySQL is ready
                MockData.shipments(products).forEach(dao::save);
                AlertService alerts = new AlertService();
                new MainFrame(new ShipmentService(dao, products, alerts), alerts).setVisible(true);
                dispose();
            } else err.setText("Invalid credentials");
        });
        getRootPane().setDefaultButton(go);
        setDefaultCloseOperation(EXIT_ON_CLOSE); pack(); setLocationRelativeTo(null);
    }
}
