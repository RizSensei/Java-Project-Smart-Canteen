package model;

public class Order {
    private int id;
    private String studentName;
    private double total;
    private String status;
    private String items;   // e.g. "Burger x2, Coke x1"

    public Order(int id, String studentName, String items, double total, String status) {
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
        return "Order #" + id + " | " + studentName + " | " + items +
               " | Rs." + total + " | " + status;
    }
}