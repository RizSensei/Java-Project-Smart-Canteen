@echo off
echo Compiling Smart Canteen...
echo.

cd src

javac -cp ".;../lib/mysql-connector-j-26.7.0.jar" ^
    net/ClientConnection.java ^
    model/MenuItem.java ^
    model/Order.java ^
    db/DBConnection.java ^
    db/MenuDAO.java ^
    db/OrderDAO.java ^
    db/UserDAO.java ^
    client/ServerConnection.java ^
    client/LoginScreen.java ^
    client/StudentClient.java ^
    staff/StaffDashboard.java ^
    server/CanteenServer.java

if %errorlevel% neq 0 (
    echo.
    echo BUILD FAILED.
) else (
    echo.
    echo BUILD SUCCESSFUL.
)

cd ..