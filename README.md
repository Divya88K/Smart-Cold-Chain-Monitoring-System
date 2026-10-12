# Smart Cold Chain Monitoring and Shipment Management System (Java)
Java Swing desktop app + JDBC/MySQL layer + Servlet placeholder. Layers: Swing UI -> Service -> DAO -> JDBC -> MySQL.

## Run (JDK 11+)
    mkdir out && javac -d out $(find src -name "*.java") && java -cp out com.coldchain.Main
Login: admin@coldchain.com / Admin@123

## Use MySQL
1. Run sql/schema.sql  2. Add mysql-connector-j to classpath
3. In ui/LoginFrame.java replace `new InMemoryShipmentDao()` with `new JdbcShipmentDao()` (set -Ddb.url -Ddb.user -Ddb.pass).

## Servlets
web/ShipmentServlet.java (needs servlet-api / Tomcat) exposes GET/DELETE /api/shipments.

## Java concepts shown
Classes/objects/constructors (model), encapsulation, inheritance (Entity), interface+polymorphism (Dao: InMemory/Jdbc), packages, custom exception, collections (ArrayList, HashMap, LinkedHashMap, Deque), lambdas/streams, multithreading (SensorSimulator), file handling + CSV (CsvUtil), JDBC CRUD, MVC.
