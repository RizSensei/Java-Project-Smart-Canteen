════════════════════════════════════════════════════════════════════════
        SMART CANTEEN ORDERING SYSTEM — COMPLETE BUILD GUIDE
              From Zero to a Working Application
════════════════════════════════════════════════════════════════════════

  Table of Contents
  ─────────────────
  PART 0   — What You Will Build
  PART 1   — Software Requirements
  PART 2   — Install JDK 21
  PART 3   — Install MySQL Server + Workbench
  PART 4   — Configure Environment Variables
  PART 5   — Download MySQL Connector/J
  PART 6   — Install VS Code + Java Extension Pack
  PART 7   — Create Project Folder Structure
  PART 8   — Open Project in VS Code
  PART 9   — Create the Database
  PART 10  — Write DBConnection.java
  PART 11  — Write MenuItem.java
  PART 12  — Write MenuDAO.java
  PART 13  — Write Order.java
  PART 14  — Write OrderDAO.java
  PART 15  — Write UITheme.java
  PART 16  — Write StudentClient.java (basic GUI)
  PART 17  — Write StaffDashboard.java (basic GUI)
  PART 18  — Write CanteenServer.java (socket server)
  PART 19  — Test Server with PowerShell
  PART 20  — Modify StudentClient to use Sockets
  PART 21  — Write StaffConnection.java
  PART 22  — Rewrite StaffDashboard for Live Updates
  PART 23  — Fix the Refresh Freeze (Single-Reader Rule)
  PART 24  — Final Testing Checklist
  PART 25  — Demo Script for Viva


════════════════════════════════════════════════════════════════════════
PART 0 — WHAT YOU WILL BUILD
════════════════════════════════════════════════════════════════════════

A three-part Java desktop system:

  • StudentClient   (Swing GUI) — Browse menu, add to cart, place order
  • StaffDashboard  (Swing GUI) — See live orders, mark them ready
  • CanteenServer   (Headless)  — Central socket server + database access

Only the server talks to MySQL. Clients communicate with the server
over TCP sockets. Staff receives real-time push updates.

Technologies: Java 21, Swing, MySQL 8, JDBC, TCP Sockets, Threads.


════════════════════════════════════════════════════════════════════════
PART 1 — SOFTWARE REQUIREMENTS
════════════════════════════════════════════════════════════════════════

Essential software (must install):

  1. JDK 21 (Oracle or OpenJDK)          — Compiles and runs Java
  2. MySQL Server 8.0.x                  — Database
  3. MySQL Connector/J 26.7.0 (JAR)      — JDBC driver
  4. VS Code                             — Code editor / IDE
  5. Extension Pack for Java (Microsoft) — Java support in VS Code

Highly recommended:

  6. MySQL Workbench                     — Visual DB tool (bundled)
  7. Git                                 — Version control (optional)

Hardware: any modern Windows / macOS / Linux machine.
Disk: ~1 GB free.

Verify after install (in PowerShell / Terminal):

  java -version      →  prints 21.x
  mysql --version    →  prints 8.0.x


════════════════════════════════════════════════════════════════════════
PART 2 — INSTALL JDK 21
════════════════════════════════════════════════════════════════════════

STEP 1. Download from:
  https://www.oracle.com/java/technologies/downloads/#java21

STEP 2. Pick the Windows x64 Installer (.exe) — ~167 MB.

STEP 3. Run the installer:
  • Click Next through the wizard.
  • Keep "Set JAVA_HOME variable" enabled.
  • Keep "Add to PATH" enabled.
  • Finish.

STEP 4. Open a NEW PowerShell and verify:

    java -version

  Expected:  java version "21.0.x" ...

If it fails, see PART 4 for PATH setup.

════════════════════════════════════════════════════════════════════════
PART 3 — INSTALL MYSQL SERVER + WORKBENCH
════════════════════════════════════════════════════════════════════════

STEP 1. Download the MySQL Installer (full, ~565 MB):
  https://dev.mysql.com/downloads/installer/

  Choose:  mysql-installer-community-8.0.46.0.msi  (the larger file)

STEP 2. Run the installer → choose Setup Type: CUSTOM → Next.

STEP 3. In the product tree, check ONLY:

    ✅ MySQL Server 8.0.46
    ✅ MySQL Workbench 8.0.x

  Skip: MySQL Shell, Router, all Connectors, Samples, Docs.

STEP 4. Next → Execute → wait for installation to finish.

STEP 5. The MySQL Configurator launches automatically. Configure:

    • Type and Networking:  Development Computer, Port 3306
    • Authentication:       Strong Password Encryption (default)
    • Root Password:        SET A PASSWORD — write it down!
                            (example: root123)
    • Windows Service:      MySQL80, Start at System Startup
    • Apply Configuration → Execute → Finish

STEP 6. Verify the service:

    Get-Service -Name "MySQL*"

  MySQL80 should show Status = Running.


════════════════════════════════════════════════════════════════════════
PART 4 — CONFIGURE ENVIRONMENT VARIABLES
════════════════════════════════════════════════════════════════════════

STEP 1. Press Windows key → type "environment variables" →
        click "Edit the system environment variables".

STEP 2. Click "Environment Variables..." (bottom right).

STEP 3. Under System variables, select Path → Edit.

STEP 4. Add TWO new entries (one per line):

    D:\java\jdk-21.0.12.1\bin
    D:\MySQL\MySQL Server 8.0\bin

  (Adjust paths if JDK/MySQL installed elsewhere.)

STEP 5. Still in System variables, click New:

    Variable name:   JAVA_HOME
    Variable value:  D:\java\jdk-21.0.12.1
                     (JDK ROOT, NOT the \bin subfolder!)

STEP 6. OK → OK → OK. Close and reopen PowerShell.

STEP 7. Verify both:

    java -version
    mysql --version

  Expected:
    java version "21.0.x"
    mysql  Ver 8.0.46 ...


════════════════════════════════════════════════════════════════════════
PART 5 — DOWNLOAD MYSQL CONNECTOR/J
════════════════════════════════════════════════════════════════════════

STEP 1. Go to:  https://dev.mysql.com/downloads/connector/j/

STEP 2. Select:
    Version:          26.7.0
    Operating System: Platform Independent

STEP 3. Download:  mysql-connector-j-26.7.0.zip  (5.2 MB)

STEP 4. Right-click → Extract All.

STEP 5. Inside the extracted folder, locate:

    mysql-connector-j-26.7.0.jar

  (NOT -sources.jar, NOT -javadoc.jar.)

STEP 6. Save this JAR somewhere easy — you'll move it into your
        project's lib/ folder in PART 7.

════════════════════════════════════════════════════════════════════════
PART 6 — INSTALL VS CODE + JAVA EXTENSION PACK
════════════════════════════════════════════════════════════════════════

STEP 1. Download and install VS Code:
  https://code.visualstudio.com/

STEP 2. Open VS Code → press Ctrl+Shift+X (Extensions).

STEP 3. Search: Extension Pack for Java

STEP 4. Install the one by Microsoft (~46M downloads).

STEP 5. Reload VS Code when prompted.

  This pack bundles:
    • Language Support for Java
    • Debugger for Java
    • Test Runner for Java
    • Maven for Java
    • Project Manager for Java
    • IntelliCode


════════════════════════════════════════════════════════════════════════
PART 7 — CREATE PROJECT FOLDER STRUCTURE
════════════════════════════════════════════════════════════════════════

STEP 1. In File Explorer create:

  D:\JavaProjects\SmartCanteen\
    ├── lib\
    └── src\
        ├── ui\
        ├── model\
        ├── db\
        ├── client\
        ├── staff\
        └── server\

STEP 2. Move the mysql-connector-j-26.7.0.jar into:
    D:\JavaProjects\SmartCanteen\lib\

STEP 3. (Optional) Create helper scripts in SmartCanteen\:

  compile.bat:
    @echo off
    cd src
    javac -cp ".;../lib/mysql-connector-j-26.7.0.jar" %*

  run.bat:
    @echo off
    cd src
    java -cp ".;../lib/mysql-connector-j-26.7.0.jar" %*

  These save you typing the long classpath each time.


════════════════════════════════════════════════════════════════════════
PART 8 — OPEN PROJECT IN VS CODE
════════════════════════════════════════════════════════════════════════

STEP 1. VS Code → File → Open Folder → select SmartCanteen.

STEP 2. Click "Yes, I trust the authors".

STEP 3. In the Explorer, scroll to "Java Projects" → expand it.

STEP 4. Under Java Projects find "Referenced Libraries" → click +.

STEP 5. Select: lib\mysql-connector-j-26.7.0.jar → Open.

  The JAR now appears under Referenced Libraries. JDBC will compile.

STEP 6. Sanity test:

  Create a temp file src/Test.java:

    public class Test {
        public static void main(String[] args) {
            System.out.println("Java is working!");
        }
    }

  Open a terminal in VS Code (Ctrl + `), then:

    cd src
    javac Test.java
    java Test

  Expected output:  Java is working!

  Delete Test.java afterwards.


════════════════════════════════════════════════════════════════════════
PART 9 — CREATE THE DATABASE
════════════════════════════════════════════════════════════════════════

STEP 1. Open PowerShell and run:

    mysql -u root -p

  Enter your MySQL root password.

STEP 2. Paste the following SQL (all at once is fine):

    CREATE DATABASE canteen_db;
    USE canteen_db;

    CREATE TABLE menu (
        id INT PRIMARY KEY AUTO_INCREMENT,
        item_name VARCHAR(50),
        price DECIMAL(6,2),
        available BOOLEAN DEFAULT TRUE
    );

    CREATE TABLE orders (
        id INT PRIMARY KEY AUTO_INCREMENT,
        student_name VARCHAR(50),
        total DECIMAL(8,2),
        status VARCHAR(20) DEFAULT 'PENDING',
        order_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

    CREATE TABLE order_items (
        id INT PRIMARY KEY AUTO_INCREMENT,
        order_id INT,
        item_name VARCHAR(50),
        quantity INT,
        FOREIGN KEY (order_id) REFERENCES orders(id)
    );

    INSERT INTO menu (item_name, price) VALUES
    ('Burger', 150.00),
    ('Coke', 50.00),
    ('Momo', 120.00),
    ('Chowmein', 100.00),
    ('Tea', 30.00);

STEP 3. Verify:

    SELECT * FROM menu;

  Expected: 5 rows.

STEP 4. Exit:

    exit;


════════════════════════════════════════════════════════════════════════
PART 10 — DBConnection.java
════════════════════════════════════════════════════════════════════════

File:  src/db/DBConnection.java

package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final String URL = "jdbc:mysql://localhost:3306/canteen_db";
    private static final String USER = "root";
    private static final String PASSWORD = "root123";   // ← your password

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void main(String[] args) {
        try (Connection conn = getConnection()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("Connected to canteen_db successfully!");
            }
        } catch (SQLException e) {
            System.out.println("Connection failed: " + e.getMessage());
        }
    }
}

Compile & test from SmartCanteen\:

    .\compile.bat db/DBConnection.java
    .\run.bat db.DBConnection

Expected:  Connected to canteen_db successfully!


════════════════════════════════════════════════════════════════════════
PART 11 — MenuItem.java
════════════════════════════════════════════════════════════════════════

File:  src/model/MenuItem.java

package model;

public class MenuItem {
    private int id;
    private String name;
    private double price;

    public MenuItem(int id, String name, double price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    public int getId()       { return id; }
    public String getName()  { return name; }
    public double getPrice() { return price; }

    @Override
    public String toString() {
        return name + " (Rs. " + price + ")";
    }
}


════════════════════════════════════════════════════════════════════════
PART 12 — MenuDAO.java
════════════════════════════════════════════════════════════════════════

File:  src/db/MenuDAO.java

package db;

import model.MenuItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MenuDAO {

    public static List<MenuItem> getAllItems() {
        List<MenuItem> items = new ArrayList<>();
        String sql = "SELECT id, item_name, price FROM menu WHERE available = TRUE";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                items.add(new MenuItem(
                    rs.getInt("id"),
                    rs.getString("item_name"),
                    rs.getDouble("price")
                ));
            }
        } catch (SQLException e) {
            System.out.println("MenuDAO error: " + e.getMessage());
        }
        return items;
    }

    public static void main(String[] args) {
        for (MenuItem m : getAllItems()) {
            System.out.println(m);
        }
    }
}

Test:  .\compile.bat db/MenuDAO.java
       .\run.bat db.MenuDAO

Expected:  Burger (Rs. 150.0) ... etc.


════════════════════════════════════════════════════════════════════════
PART 13 — Order.java
════════════════════════════════════════════════════════════════════════

File:  src/model/Order.java

package model;

public class Order {
    private int id;
    private String studentName;
    private double total;
    private String status;
    private String items;

    public Order(int id, String studentName, String items,
                 double total, String status) {
        this.id = id;
        this.studentName = studentName;
        this.items = items;
        this.total = total;
        this.status = status;
    }

    public int getId()             { return id; }
    public String getStudentName() { return studentName; }
    public String getItems()       { return items; }
    public double getTotal()       { return total; }
    public String getStatus()      { return status; }

    @Override
    public String toString() {
        return "Order #" + id + " | " + studentName + " | Rs." + total +
               " | " + status;
    }
}


════════════════════════════════════════════════════════════════════════
PART 14 — OrderDAO.java
════════════════════════════════════════════════════════════════════════

File:  src/db/OrderDAO.java

package db;

import model.Order;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderDAO {

    public static int placeOrder(String studentName, String items, double total) {
        String sql = "INSERT INTO orders (student_name, total, status) " +
                     "VALUES (?, ?, 'PENDING')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql,
                     Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, studentName);
            ps.setDouble(2, total);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);

        } catch (SQLException e) {
            System.out.println("placeOrder error: " + e.getMessage());
        }
        return -1;
    }

    public static List<Order> getAllOrders() {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT id, student_name, total, status " +
                     "FROM orders ORDER BY id DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                orders.add(new Order(
                    rs.getInt("id"),
                    rs.getString("student_name"),
                    "",
                    rs.getDouble("total"),
                    rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            System.out.println("getAllOrders error: " + e.getMessage());
        }
        return orders;
    }

    public static boolean markReady(int orderId) {
        String sql = "UPDATE orders SET status = 'READY' WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, orderId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("markReady error: " + e.getMessage());
        }
        return false;
    }
}

Test:  .\compile.bat db/OrderDAO.java  (no main; just to compile)


════════════════════════════════════════════════════════════════════════
PART 15 — UITheme.java
════════════════════════════════════════════════════════════════════════

File:  src/ui/UITheme.java

package ui;

import java.awt.Color;
import java.awt.Font;

public class UITheme {
    public static final Color PRIMARY       = new Color(0x2E7D32);
    public static final Color PRIMARY_DARK  = new Color(0x1B5E20);
    public static final Color ACCENT        = new Color(0xFF6F00);
    public static final Color DANGER        = new Color(0xC62828);
    public static final Color BG            = new Color(0xF5F5F5);
    public static final Color CARD          = Color.WHITE;
    public static final Color TEXT          = new Color(0x212121);
    public static final Color TEXT_MUTED    = new Color(0x757575);
    public static final Color BORDER        = new Color(0xE0E0E0);

    public static final Color PENDING_BG    = new Color(0xFFF3CD);
    public static final Color PENDING_FG    = new Color(0x8A6D00);
    public static final Color READY_BG      = new Color(0xD4EDDA);
    public static final Color READY_FG      = new Color(0x1B5E20);

    public static final Font H1       = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font H2       = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font BODY     = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font BODY_B   = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font SMALL    = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font MONO     = new Font("Consolas", Font.PLAIN, 13);
}


════════════════════════════════════════════════════════════════════════
PART 16 — StudentClient.java (BASIC GUI — direct JDBC version)
════════════════════════════════════════════════════════════════════════

This is the FIRST version (before sockets). Later we swap the DAO
calls for socket calls in PART 20.

File:  src/client/StudentClient.java

[Use the enhanced StudentClient from the UI-polish step. It imports
 db.MenuDAO and db.OrderDAO initially. Full source is in the code
 blocks delivered earlier.]

Compile & test:
    .\compile.bat client/StudentClient.java
    .\run.bat client.StudentClient

Verify: menu loads, add to cart works, place order saves to DB.
MySQL check:  SELECT * FROM orders;

════════════════════════════════════════════════════════════════════════
PART 17 — StaffDashboard.java (BASIC GUI — refresh-based)
════════════════════════════════════════════════════════════════════════

First version uses OrderDAO directly and has a Refresh button.
Full source is the enhanced UI version delivered earlier (before sockets).

Compile & test:
    .\compile.bat staff/StaffDashboard.java
    .\run.bat staff.StaffDashboard

Verify: orders appear when Refresh is clicked.
MySQL check:  SELECT * FROM orders;


════════════════════════════════════════════════════════════════════════
PART 18 — CanteenServer.java (SOCKET SERVER)
════════════════════════════════════════════════════════════════════════

File:  src/server/CanteenServer.java

[Insert full source from the "Socket server" step — the version with
 ServerSocket(6000), thread-per-client, staffClients list, and
 commands REGISTER_STAFF, GET_MENU, ORDER, GET_ORDERS, MARK_READY, QUIT.]

Compile & run:
    .\compile.bat server/CanteenServer.java
    .\run.bat server.CanteenServer

Expected banner:
    🍔 Canteen Server started on port 6000
       Waiting for clients...


════════════════════════════════════════════════════════════════════════
PART 19 — TEST SERVER WITH POWERSHELL
════════════════════════════════════════════════════════════════════════

Open a second PowerShell and run:

  $c = New-Object System.Net.Sockets.TcpClient("localhost",6000)
  $s = $c.GetStream()
  $w = New-Object System.IO.StreamWriter($s); $w.AutoFlush = $true
  $r = New-Object System.IO.StreamReader($s)

  $w.WriteLine("GET_MENU");  $r.ReadLine()
  $w.WriteLine("ORDER:Test:Burger x1:150.0");  $r.ReadLine()
  $w.WriteLine("QUIT");  $r.ReadLine()
  $c.Close()

Expected:
  MENU:1~Burger~150.0;2~Coke~50.0;...
  ORDER_OK:1
  BYE

Server console should show matching logs.


════════════════════════════════════════════════════════════════════════
PART 20 — MODIFY StudentClient TO USE SOCKETS
════════════════════════════════════════════════════════════════════════

STEP 1. Create src/client/ServerConnection.java:

package client;

import java.io.*;
import java.net.Socket;

public class ServerConnection {
    public static final String HOST = "localhost";
    public static final int PORT = 6000;

    public static String send(String command) throws IOException {
        try (Socket socket = new Socket(HOST, PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()))) {
            out.println(command);
            return in.readLine();
        }
    }
}

STEP 2. In StudentClient.java:
  • Remove:  import db.MenuDAO;  import db.OrderDAO;
  • Add:     import model.MenuItem;
  • Rewrite loadMenu() to send "GET_MENU" and parse "MENU:..."
  • Rewrite placeOrder() to send "ORDER:name:items:total"
  • Add a private parseMenu(String) helper.

Full code was delivered in the "Modify StudentClient" step.

Compile & test:
    .\compile.bat client/ServerConnection.java
    .\compile.bat client/StudentClient.java
    .\run.bat client.StudentClient

Verify: menu loads from server; placing an order logs ORDER: on
the server console and returns an ORDER_OK:id popup.


════════════════════════════════════════════════════════════════════════
PART 21 — StaffConnection.java (PERSISTENT SOCKET)
════════════════════════════════════════════════════════════════════════

File:  src/staff/StaffConnection.java

package staff;

import java.io.*;
import java.net.Socket;

public class StaffConnection {

    public static final String HOST = "localhost";
    public static final int PORT = 6000;

    public interface MessageListener {
        void onMessage(String message);
    }

    private final MessageListener listener;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private volatile boolean running = true;

    public StaffConnection(MessageListener listener) {
        this.listener = listener;
    }

    public void connect() throws IOException {
        socket = new Socket(HOST, PORT);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out.println("REGISTER_STAFF");

        Thread reader = new Thread(this::readLoop, "staff-listener");
        reader.setDaemon(true);
        reader.start();
    }

    private void readLoop() {
        try {
            String line;
            while (running && (line = in.readLine()) != null) {
                listener.onMessage(line);
            }
        } catch (IOException e) {
            if (running) listener.onMessage("ERROR:" + e.getMessage());
        }
    }

    /** Non-blocking send. Responses arrive asynchronously. */
    public synchronized void send(String command) {
        if (out != null) out.println(command);
    }

    public void close() {
        running = false;
        try {
            if (out != null) out.println("QUIT");
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
    }
}


════════════════════════════════════════════════════════════════════════
PART 22 — REWRITE StaffDashboard FOR LIVE UPDATES
════════════════════════════════════════════════════════════════════════

STEP 1. StaffDashboard implements StaffConnection.MessageListener.

STEP 2. On startup, call connection.connect().

STEP 3. Implement onMessage(String) → SwingUtilities.invokeLater(...)

STEP 4. Handle these prefixes in handleServerMessage:
    • NEW_ORDER:id:name:total  → add row, beep
    • ORDER_UPDATED:id:STATUS  → update row
    • ORDERS:...               → replace entire list
    • ERROR:...                → status bar

STEP 5. Buttons call connection.send(...) — never read.

Full source was delivered in the "persistent staff client" step.

Compile & test:
    .\compile.bat staff/StaffConnection.java
    .\compile.bat staff/StaffDashboard.java
    .\run.bat staff.StaffDashboard

Verify: header shows ● live; placing an order from StudentClient
causes a new row to appear automatically with a beep.


════════════════════════════════════════════════════════════════════════
PART 23 — FIX THE REFRESH FREEZE (SINGLE-READER RULE)
════════════════════════════════════════════════════════════════════════

Symptom:  Clicking Refresh hangs the UI.

Cause:  Both the background reader thread AND the EDT call
        in.readLine() on the same socket. Whichever wins consumes
        the bytes; the other blocks forever.

Fix:
  1. StaffConnection exposes ONLY send(String) — never reads.
  2. refreshOrders() and markReady() call connection.send(...).
  3. All responses (including ORDERS: and OK:READY) arrive via
     onMessage(...) on the background reader thread, then are
     marshaled to the EDT via SwingUtilities.invokeLater.

Rule:  Only ONE thread may read from a socket. UI never blocks on I/O.


════════════════════════════════════════════════════════════════════════
PART 24 — FINAL TESTING CHECKLIST
════════════════════════════════════════════════════════════════════════

Run through this every time you demo:

  [  ] MySQL service Running (Get-Service MySQL*)
  [  ] CanteenServer started — banner visible
  [  ] StaffDashboard opened — ● live in green header
  [  ] StudentClient opened — menu loads from server
  [  ] Add Burger x2 + Coke x1 — total shows Rs. 350.00
  [  ] Place Order — popup shows Order ID
  [  ] StaffDashboard updates automatically (no refresh)
  [  ] Beep plays on new order
  [  ] Mark Ready — status badge turns green
  [  ] Second StaffDashboard also updates (broadcast proof)
  [  ] MySQL: SELECT * FROM orders; shows the new rows
  [  ] Server console logs every command and broadcast


════════════════════════════════════════════════════════════════════════
PART 25 — DEMO SCRIPT FOR VIVA
════════════════════════════════════════════════════════════════════════

  1. Show the running MySQL service.

  2. Start CanteenServer — "this is the headless backend. It
     listens on port 6000 and is the only component that talks
     to MySQL."

  3. Start StaffDashboard — "this is the kitchen view. Note the
     ● live indicator in the top-right corner — the socket is
     open and listening for broadcasts."

  4. Start StudentClient — "this is the student view. Notice the
     menu items — they came from MySQL, delivered over a socket."

  5. Type "Ram", add Burger x2 and Coke x1, point out the
     running total.

  6. Click Place Order — popup shows the order ID.

  7. Switch to StaffDashboard — "no Refresh was clicked. The
     server broadcast NEW_ORDER to all registered staff."

  8. Click Mark Ready — status turns green. "This triggered an
     ORDER_UPDATED broadcast to every staff client."

  9. Open a second StaffDashboard — place another order — both
     dashboards update simultaneously. "This is the broadcast
     pattern at work."

 10. Show MySQL:
       SELECT * FROM orders;
     "Every order is persisted. If the server restarts, nothing
      is lost."

 11. Show server console — every command logged with timestamps.

 12. Wrap up: "The project demonstrates Swing GUI, MySQL + JDBC,
     TCP sockets, multi-threading, thread-safe collections, and
     a custom text protocol — with a clean separation between
     UI, transport, and data."


════════════════════════════════════════════════════════════════════════
                            END OF BUILD GUIDE
════════════════════════════════════════════════════════════════════════