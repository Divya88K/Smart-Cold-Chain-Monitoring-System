package com.coldchain.service;
import com.coldchain.model.*;
/** Business rules: SAFE inside range, WARNING within 2 units outside, CRITICAL beyond. */
public final class ColdChainRules {
    private ColdChainRules() {}
    private static Severity grade(double v, double lo, double hi) {
        double d = v < lo ? lo - v : v > hi ? v - hi : 0;
        return d == 0 ? Severity.SAFE : d <= 2 ? Severity.WARNING : Severity.CRITICAL;
    }
    public static Severity temperature(double t, Product p) { return grade(t, p.getTmin(), p.getTmax()); }
    public static Severity humidity(double h, Product p) { return grade(h, p.getHmin(), p.getHmax()); }
    public static int risk(double quality) { return (int) Math.min(100, Math.round((100 - quality) * 1.6)); }
    public static String riskClass(int r) { return r <= 30 ? "Low" : r <= 60 ? "Moderate" : r <= 80 ? "High" : "Critical"; }
}
