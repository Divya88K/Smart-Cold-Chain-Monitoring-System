package com.coldchain.model;
public class Product extends Entity {
    private final String name, category;
    private final double tmin, tmax, hmin, hmax;
    public Product(String id, String name, String category, double tmin, double tmax, double hmin, double hmax) {
        super(id); this.name = name; this.category = category;
        this.tmin = tmin; this.tmax = tmax; this.hmin = hmin; this.hmax = hmax;
    }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public double getTmin() { return tmin; }
    public double getTmax() { return tmax; }
    public double getHmin() { return hmin; }
    public double getHmax() { return hmax; }
}
