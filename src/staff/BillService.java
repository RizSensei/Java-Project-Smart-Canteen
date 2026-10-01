package staff;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import model.Order;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BillService {

    /** Directory where bills are saved. */
    private static final String OUTPUT_DIR = "D:\\Java\\JavaProjects\\SmartCanteen\\bills";

    /**
     * Generates a PDF bill for the order and returns the file.
     * Returns null on failure.
     */
    public static File generateBill(Order order) {
        try {
            // Ensure output directory exists
            File dir = new File(OUTPUT_DIR);
            if (!dir.exists())
                dir.mkdirs();

            String filename = "Bill_Order" + order.getId() + "_" +
                    System.currentTimeMillis() + ".pdf";
            File file = new File(dir, filename);

            Document doc = new Document();
            PdfWriter.getInstance(doc, new FileOutputStream(file));
            doc.open();

            // ---- Header ----
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
            Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
            Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);

            Paragraph title = new Paragraph("Smart Canteen", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);

            Paragraph sub = new Paragraph(
                    "Kathmandu, Nepal  •  +977-0000000000",
                    subFont);
            sub.setAlignment(Element.ALIGN_CENTER);
            doc.add(sub);

            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("----------------------------------------",
                    subFont));
            doc.add(new Paragraph(" "));

            // ---- Order meta ----
            doc.add(new Paragraph("Order #" + order.getId(), headFont));
            doc.add(new Paragraph("Student: " + order.getStudentName(), bodyFont));
            doc.add(new Paragraph("Status:  " + order.getStatus(), bodyFont));
            doc.add(new Paragraph("Date:    " +
                    LocalDateTime.now().format(
                            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")),
                    bodyFont));
            doc.add(new Paragraph(" "));

            // ---- Items table ----
            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.setWidths(new float[] { 4, 1, 2 });

            addHeaderCell(table, "Item");
            addHeaderCell(table, "Qty");
            addHeaderCell(table, "Amount (Rs.)");

            double computedTotal = 0.0;

            // Parse "Name xQty = Rs. Amount, ..." into rows
            String[] entries = order.getItems().split("\\s*,\\s*");
            for (String entry : entries) {
                String[] halves = entry.split("\\s*=\\s*");
                String left = halves.length > 0 ? halves[0].trim() : entry;
                String right = halves.length > 1 ? halves[1].trim() : "0";

                int xIdx = left.lastIndexOf(" x");
                String name = xIdx == -1 ? left : left.substring(0, xIdx).trim();
                String qty = xIdx == -1 ? "1" : left.substring(xIdx + 2).trim();

                // Strip "Rs." prefix from right side
                String amount = right.replaceFirst("(?i)rs\\.?\\s*", "");

                table.addCell(new Phrase(name, bodyFont));
                table.addCell(new Phrase(qty, bodyFont));
                PdfPCell amtCell = new PdfPCell(new Phrase(amount, bodyFont));
                amtCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(amtCell);

                try {
                    computedTotal += Double.parseDouble(amount);
                } catch (NumberFormatException ignored) {
                }
            }

            // Grand total row
            PdfPCell empty1 = new PdfPCell(new Phrase(""));
            PdfPCell empty2 = new PdfPCell(new Phrase(""));
            PdfPCell totalCell = new PdfPCell(new Phrase(
                    String.format("Total: Rs. %.2f", order.getTotal()),
                    headFont));
            totalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(empty1);
            table.addCell(empty2);
            table.addCell(totalCell);

            doc.add(table);

            // ---- Footer ----
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph(" "));
            Paragraph thanks = new Paragraph(
                    "Thank you for dining with us!",
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 11));
            thanks.setAlignment(Element.ALIGN_CENTER);
            doc.add(thanks);

            doc.close();
            return file;

        } catch (Exception e) {
            System.out.println("generateBill error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static void addHeaderCell(PdfPTable table, String text) {
        Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12,
                new Color(0x1B, 0x5E, 0x20));
        PdfPCell cell = new PdfPCell(new Phrase(text, headFont));
        cell.setBackgroundColor(new Color(0xE8, 0xF5, 0xE9));
        cell.setPadding(6);
        table.addCell(cell);
    }
}