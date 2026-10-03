package billing;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import model.Order;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BillGenerator {

    private static final Pattern ITEM_PATTERN = Pattern.compile(
            "^(.+?)\\s+x(\\d+)\\s*=\\s*Rs\\.?\\s*([0-9]+(?:\\.[0-9]{1,2})?)$");

    private BillGenerator() {
    }

    public static File generate(Order order) throws IOException, DocumentException {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null.");
        }
        if (order.getStatus() == null || !"PAID".equalsIgnoreCase(order.getStatus().trim())) {
            throw new IllegalArgumentException("Only paid orders can have a bill generated.");
        }

        File output = getBillFile(order);
        Document document = new Document(PageSize.A4, 48, 48, 48, 48);
        try (FileOutputStream stream = new FileOutputStream(output)) {
            PdfWriter.getInstance(document, stream);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 20, Font.BOLD, new Color(194, 24, 91));
            Font bodyFont = new Font(Font.HELVETICA, 11, Font.NORMAL, Color.DARK_GRAY);
            Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);

            Paragraph title = new Paragraph("Smart Canteen", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph subtitle = new Paragraph("Order Invoice", bodyFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(22);
            document.add(subtitle);

            document.add(new Paragraph("Customer: " + safeText(order.getStudentName()), bodyFont));
            Paragraph orderId = new Paragraph("Order ID: " + order.getId(), bodyFont);
            orderId.setSpacingAfter(16);
            document.add(orderId);

            PdfPTable table = new PdfPTable(new float[] { 5, 2, 1.5f, 2 });
            table.setWidthPercentage(100);
            table.setSpacingAfter(16);
            addHeaderCell(table, "Food / Item", headerFont);
            addHeaderCell(table, "Unit Price", headerFont);
            addHeaderCell(table, "Quantity", headerFont);
            addHeaderCell(table, "Item Total", headerFont);

            for (InvoiceItem item : parseItems(order.getItems())) {
                table.addCell(new Phrase(item.name, bodyFont));
                table.addCell(new Phrase("Rs. " + money(item.unitPrice), bodyFont));
                table.addCell(new Phrase(Integer.toString(item.quantity), bodyFont));
                table.addCell(new Phrase("Rs. " + money(item.total), bodyFont));
            }

            PdfPCell totalLabel = new PdfPCell(new Phrase("Grand Total", headerFont));
            totalLabel.setColspan(3);
            totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalLabel.setBackgroundColor(new Color(194, 24, 91));
            totalLabel.setPadding(8);
            table.addCell(totalLabel);

            PdfPCell totalValue = new PdfPCell(new Phrase(
                    "Rs. " + money(BigDecimal.valueOf(order.getTotal())), headerFont));
            totalValue.setBackgroundColor(new Color(194, 24, 91));
            totalValue.setPadding(8);
            table.addCell(totalValue);

            document.add(table);
            document.close();
        }
        return output;
    }

    public static File getBillFile(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null.");
        }
        String customerName = safeFileNamePart(order.getStudentName());
        return new File("Bill_Order_" + order.getId() + "_" + customerName + ".pdf");
    }

    private static String safeFileNamePart(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "Customer";
        }
        String safeName = text.trim().replaceAll("[^A-Za-z0-9._-]+", "_");
        return safeName.isEmpty() ? "Customer" : safeName;
    }

    private static void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(136, 14, 79));
        cell.setPadding(8);
        table.addCell(cell);
    }

    private static java.util.List<InvoiceItem> parseItems(String items) {
        java.util.List<InvoiceItem> parsed = new java.util.ArrayList<>();
        if (items == null || items.trim().isEmpty()) {
            throw new IllegalArgumentException("The selected order has no invoice items.");
        }

        for (String entry : items.split("\\s*,\\s*")) {
            Matcher matcher = ITEM_PATTERN.matcher(entry.trim());
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Cannot read order item: " + entry);
            }
            int quantity = Integer.parseInt(matcher.group(2));
            if (quantity <= 0) {
                throw new IllegalArgumentException("Item quantity must be positive: " + entry);
            }
            BigDecimal total = new BigDecimal(matcher.group(3)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal unitPrice = total.divide(BigDecimal.valueOf(quantity), 2, RoundingMode.HALF_UP);
            parsed.add(new InvoiceItem(matcher.group(1).trim(), quantity, unitPrice, total));
        }
        return parsed;
    }

    private static String money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String safeText(String text) {
        return text == null || text.trim().isEmpty() ? "Customer" : text.trim();
    }

    private static final class InvoiceItem {
        private final String name;
        private final int quantity;
        private final BigDecimal unitPrice;
        private final BigDecimal total;

        private InvoiceItem(String name, int quantity, BigDecimal unitPrice, BigDecimal total) {
            this.name = name;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.total = total;
        }
    }
}