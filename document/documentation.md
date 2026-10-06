SMART CANTEEN ORDERING SYSTEM
Project Documentation

BIT255CO — Programming in Java
Year II, Semester II


TABLE OF CONTENTS

1. Project Overview
2. Objectives
3. Technology Stack
4. Architecture
5. Database Design
6. Project Structure
7. Component Details
8. Socket Protocol
9. Key Workflows
10. Concurrency Model
11. Setup Instructions
12. Running the Application
13. Demonstration Script
14. Error Handling
15. Syllabus Coverage Map
16. Known Limitations & Future Enhancements
17. Troubleshooting
18. Conclusion


1. PROJECT OVERVIEW

Smart Canteen Ordering System is a Java desktop application that digitizes
a canteen's order-taking process. Students browse the menu, add items to a
cart, and place orders. Canteen staff see incoming orders in real time on a
kitchen dashboard and can mark them ready.

The project demonstrates the three pillars of modern Java application
development:
  • GUI with Java Swing
  • Persistence with MySQL via JDBC
  • Real-time communication with TCP sockets + multi-threading


2. OBJECTIVES

  • Replace manual canteen ordering with a digital system
  • Provide a live, real-time view for kitchen staff (no manual refreshing)
  • Demonstrate client–server architecture with a custom socket protocol
  • Show clean separation of concerns: UI ↔ Server ↔ Database
  • Cover core Java concepts: OOP, collections, exceptions, threads, I/O,
    networking, GUI, JDBC


3. TECHNOLOGY STACK

  Layer         Technology                     Purpose
  -----------------------------------------------------------------------
  Language      Java 21 (JDK 21)               Application logic
  GUI           Java Swing                     Student & Staff windows
  Database      MySQL 8.0                      Persistent storage
  DB Driver     MySQL Connector/J 26.7.0       JDBC bridge
  Networking    TCP Sockets (java.net)         Real-time transport
  Concurrency   Java Threads                   Per-client handling
  IDE           VS Code + Java Extension Pack  Development
  Build         javac + batch scripts          Compilation


4. ARCHITECTURE

  [Student Client (Swing)]  ←→  [Canteen Server]  ←→  [MySQL DB]
                                       ↑
                                       ↓
                              [Staff Dashboard (Swing)]

  Key design principles:
   1. Only the server touches the database. Clients never open a JDBC
      connection.
   2. Single-reader rule. Each socket has exactly one thread reading it.
   3. UI stays on the EDT. All Swing updates happen via
      SwingUtilities.invokeLater(...), never from socket/DB threads.
   4. Fire-and-forget writes. Clients send commands without waiting;
      replies arrive asynchronously through the reader thread.


5. DATABASE DESIGN

  Database: canteen_db

  Table: users
    id          INT PK AUTO_INCREMENT
    name        VARCHAR(50) UNIQUE (case-insensitive)
    email       VARCHAR(254) UNIQUE (normalized Gmail address; NULL for legacy accounts)
    password    VARCHAR(255) (PBKDF2 hash for new accounts)
    name_normalized / email_normalized
                Generated lowercase columns with unique indexes, installed by UserDAO
                to enforce case-insensitive uniqueness for concurrent registrations.

  Table: student_login_history (created automatically on first successful login)
    id          BIGINT PK AUTO_INCREMENT
    username    VARCHAR(50)
    login_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP

  Table: menu
    id          INT PK AUTO_INCREMENT
    item_name   VARCHAR(50) UNIQUE
    price       DECIMAL(6,2)
    available   BOOLEAN DEFAULT TRUE

  Table: orders
    id             INT PK AUTO_INCREMENT
    student_name   VARCHAR(50)
    total          DECIMAL(8,2)
    status         VARCHAR(20) DEFAULT 'PENDING'
    order_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP

  Table: order_items (reserved for future use)
    id          INT PK AUTO_INCREMENT
    order_id    INT FK → orders(id)
    item_name   VARCHAR(50)
    quantity    INT


6. PROJECT STRUCTURE

  SmartCanteen/
    compile.bat
    run.bat
    lib/mysql-connector-j-26.7.0.jar
    src/
      ui/UITheme.java
      model/MenuItem.java
      model/Order.java
      db/DBConnection.java
      db/MenuDAO.java
      db/OrderDAO.java
      client/ServerConnection.java
      client/StudentClient.java
      staff/StaffConnection.java
      staff/StaffDashboard.java
      server/CanteenServer.java


7. COMPONENT DETAILS

  UITheme          - Centralized colors, fonts, shared style.
  MenuItem, Order  - Immutable POJOs.
  DBConnection     - Static factory returning a JDBC Connection.
  MenuDAO          - getAllItems(), addItem(), removeItem()
  UserDAO          - Legacy login validation and secure student account creation
  OrderDAO         - placeOrder(), getAllOrders(), markReady()
  ServerConnection - One-shot socket helper (student side).
  StudentClient    - Swing JFrame: menu table, cart, name field, buttons.
  StaffConnection  - Persistent socket helper with callback listener.
  StaffDashboard   - Swing JFrame: live order table with color badges.
  CanteenServer    - Multi-threaded socket server + command dispatcher.

  All DAO methods use PreparedStatement with ? placeholders.


8. SOCKET PROTOCOL

  All messages are single-line UTF-8 text. Fields separated by ':'.
  Lists use ';' between entries, '~' between fields.

  Client → Server:
    REGISTER_STAFF
    CREATE_STUDENT:username:base64url-gmail:base64url-password
    LOGIN:username:B64:base64url-password
    GET_MENU
    ADD_ITEM:base64url-name:price
    REMOVE_ITEM:id
    ORDER:name:items:total
    GET_ORDERS
    MARK_READY:id
    QUIT

  Server → Client:
    OK:REGISTERED
    ACCOUNT_CREATED / ACCOUNT_EXISTS
    MENU:id~name~price;...
    ITEM_ADDED / ITEM_REMOVED / ITEM_EXISTS
    MENU_UPDATED (broadcast to connected students after a menu change)
    ORDER_OK:id
    ORDERS:id~name~total~status;...
    OK:READY
    ERROR:message
    BYE

  New student passwords are stored as PBKDF2-HMAC-SHA256 hashes with random
  salts. New accounts require a unique Gmail address and a password of 8-128
  characters containing a letter and a number. Existing accounts without email
  continue to authenticate. On the first account creation the server expands
  users.password as needed, adds users.email if missing, and adds unique indexes
  on users.name and users.email. Existing duplicate usernames must be resolved
  before account creation can add the username index. The first menu item
  addition adds a unique index on menu.item_name.
  Each successful login is recorded in student_login_history. If history
  recording fails, the server logs the database error but still allows a
  valid student to log in.

  Menu changes are accepted only from a connection registered as staff. The
  existing staff registration flow does not have a separate credential check.
  Connected student clients reload the menu from the server when they receive
  MENU_UPDATED, so additions and removals appear without signing in again.

  Server broadcasts (to all staff):
    NEW_ORDER:id:name:total
    ORDER_UPDATED:id:STATUS


9. KEY WORKFLOWS

  9.1 Place an order
    1. StudentClient sends GET_MENU.
    2. Server calls MenuDAO.getAllItems() → replies MENU:...
    3. Student picks items, clicks Place Order.
    4. Client sends ORDER:Ram:Burger x2, Coke x1:350.0
    5. Server calls OrderDAO.placeOrder() → returns new ID.
    6. Server replies ORDER_OK:12 to student.
    7. Server broadcasts NEW_ORDER:12:Ram:350.00 to all staff.
    8. Staff dashboard adds a row, plays a beep.

  9.2 Mark ready
    1. Staff clicks Mark Ready on a selected row.
    2. StaffDashboard sends MARK_READY:12 (non-blocking).
    3. Server calls OrderDAO.markReady(12).
    4. Server broadcasts ORDER_UPDATED:12:READY to all staff.
    5. All staff dashboards update the badge to green.

  9.3 Manual refresh
    1. Staff clicks Refresh.
    2. Sends GET_ORDERS (non-blocking).
    3. Server replies ORDERS:... only to that socket.
    4. Dashboard replaces its list and rebuilds the table.


10. CONCURRENCY MODEL

  Threads:
    - Swing EDT              (UI drawing, event handling)
    - Server main thread     (accept() loop)
    - Server per-client      (read command, execute, respond)
    - Staff listener thread  (blocking readLine on persistent socket)

  Rules:
    1. One reader per socket.
    2. UI touched only from EDT via SwingUtilities.invokeLater.
    3. Server shared state uses CopyOnWriteArrayList.
    4. DAO methods are stateless — each opens and closes its own connection.


11. SETUP INSTRUCTIONS

  Prerequisites:
    JDK 17 or 21
    MySQL Server 8.0.x
    MySQL Connector/J 26.7.0
    VS Code + Extension Pack for Java

  Environment:
    Set JAVA_HOME to the JDK root.
    Add %JAVA_HOME%\bin and MySQL's bin folder to Path.
    Verify:  java -version   and   mysql --version

  Database:
    Run the SQL in Section 5 to create canteen_db, the three tables,
    and insert the five seed menu items.

  Project:
    1. Create the folder structure from Section 6.
    2. Place the Connector/J JAR in lib/.
    3. Open the folder in VS Code.
    4. Add the JAR to Referenced Libraries.

  Password:
    Edit src/db/DBConnection.java and set PASSWORD to your MySQL root
    password.


12. RUNNING THE APPLICATION

  Three terminals. Order matters — server first.

  Terminal 1 — Server:
    .\compile.bat server/CanteenServer.java
    .\run.bat server.CanteenServer

  Terminal 2 — Staff Dashboard:
    .\compile.bat staff/StaffConnection.java
    .\compile.bat staff/StaffDashboard.java
    .\run.bat staff.StaffDashboard

  Terminal 3 — Student Client:
    .\compile.bat client/ServerConnection.java
    .\compile.bat client/StudentClient.java
    .\run.bat client.StudentClient


13. DEMONSTRATION SCRIPT

   1. Start MySQL — confirm Running.
   2. Start CanteenServer — show the banner.
   3. Start StaffDashboard — point out the ● live indicator.
   4. Start StudentClient — enter name "Ram".
   5. Show the menu — "these items came from MySQL over a socket".
   6. Add Burger x2 and Coke x1 — total shows Rs. 350.00.
   7. Click Place Order — popup shows order ID.
   8. Switch to Staff Dashboard — the order is already there.
      No Refresh was clicked. Beep plays.
   9. Click Mark Ready — status badge turns green.
  10. Open a second StaffDashboard — place another order —
      both dashboards update simultaneously (proves broadcast).
  11. Show MySQL: SELECT * FROM orders; (proves persistence).
  12. Show server console — every command and broadcast logged.


14. ERROR HANDLING

  Layer          Approach
  -----------------------------------------------------------------------
  DB             try-with-resources, catch SQLException,
                 return safe default (-1, empty list)
  Socket         try-with-resources, IOException caught and logged
  Server         Per-client thread — one failure doesn't kill others
  UI             JOptionPane popups for user errors,
                 status bar for silent failures
  Protocol       Unknown commands → ERROR:unknown command
  Null           All socket reads checked for null before parsing


15. SYLLABUS COVERAGE MAP

  Topic                              Where It Appears
  -----------------------------------------------------------------------
  1.1  OOP in Java                   Every class
  1.2  JVM, Java env, tools          javac, java, JDK, JAR
  1.3  Features of Java              Portability, threads, GC
  1.4  Control statements            Loops, conditionals everywhere
  1.5  Looping                       Server while(true), for on lists
  1.6  Array                         (implicit in lists / varargs)
  1.7  String / StringBuffer         Protocol parsing, StringBuilder
  1.8  Vector                        Replaced by ArrayList /
                                     CopyOnWriteArrayList
  1.9  Class and Objects             MenuItem, Order, StudentClient
  1.10 Inheritance                   JFrame, DefaultTableCellRenderer
  1.11 Polymorphism                  MessageListener interface
  1.12 Collections                   ArrayList, List, CopyOnWriteArrayList
  1.13 Interface & Packages          MessageListener, package declarations
  1.14 Exceptions                    SQLException, IOException,
                                     try-with-resources
  1.15 Multi-threading               Per-client threads, EDT safety
  3.1  AWT vs Swing                  Swing chosen for modern look
  3.2  Swing components              JFrame, JPanel, JButton, JTable,
                                     JTextArea, JSpinner
  3.3  Atomic components             JLabel, JButton, JTextField
  3.4  Frame, Panel, Table           Used extensively
  3.5  Event handling                ActionListener, custom cell renderer
  5.x  JDBC                          DBConnection, MenuDAO, OrderDAO
  6.1–6.5 Socket programming         ServerSocket, Socket,
                                     BufferedReader / PrintWriter


16. KNOWN LIMITATIONS & FUTURE ENHANCEMENTS

  Current limitations:
    - Items stored inline as a string; order_items table unused
    - No authentication — anyone can be "staff"
    - Plaintext protocol — no encryption
    - Server and clients assumed to be on localhost

  Possible extensions:
    - Order history GUI
    - Daily sales report (SUM of today's orders)
    - Item availability toggle
    - Multiple canteens with routing
    - SSL-encrypted sockets
    - Replace custom sockets with RMI or web services


17. TROUBLESHOOTING

  Symptom                              Fix
  -----------------------------------------------------------------------
  Communications link failure          Start-Service MySQL80
  Access denied for user 'root'        Fix password in DBConnection
  Unknown database 'canteen_db'        Run the schema SQL
  No suitable driver found             Add JAR to classpath
  Address already in use: 6000         Kill old server or change port
  ● offline in staff header            Start server first
  Staff UI freezes on Refresh          Use non-blocking send()
  NumberFormatException                Strip ':' from items string
  Emoji render as ?                    Cosmetic only


18. CONCLUSION

  The Smart Canteen Ordering System is a complete, working demonstration
  of:

    - Desktop GUI development with Java Swing
    - Relational persistence with MySQL + JDBC
    - Real-time networking with TCP sockets
    - Concurrent programming with threads and thread-safe collections
    - Clean architecture — UI separated from transport, transport
      separated from data
    - Custom application protocol designed and implemented from scratch

  It is intentionally scoped to be beginner-friendly yet rich enough to
  exhibit every major topic from the Java syllabus. The result is a fully
  functional, demo-ready application that showcases practical software
  engineering in a real-world scenario.


End of documentation.