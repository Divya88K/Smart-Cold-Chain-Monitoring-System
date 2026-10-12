package com.coldchain.model;
/** Base class (inheritance + encapsulation). */
public abstract class Entity {
    protected final String id;
    protected Entity(String id) { this.id = id; }
    public String getId() { return id; }
}
