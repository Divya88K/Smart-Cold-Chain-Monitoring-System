package com.coldchain.dao;
import java.util.*;
/** Generic DAO contract: implemented by in-memory and JDBC versions (polymorphism). */
public interface Dao<T> {
    List<T> findAll();
    Optional<T> findById(String id);
    void save(T t);
    void delete(String id);
}
