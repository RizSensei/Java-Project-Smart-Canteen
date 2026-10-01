package db;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility for parsing and merging order-item strings.
 * Format: "Name xQty = Rs. LINE_TOTAL, Name xQty = Rs. LINE_TOTAL"
 */
public class ItemMerger {

    /** Parse "Momo x1 = Rs. 120.00, Coke x1 = Rs. 50.00"
        into a map of name -> qty. Line totals are ignored. */
    public static Map<String, Integer> parseQuantities(String items) {
        Map<String, Integer> map = new LinkedHashMap<>();
        if (items == null || items.isEmpty()) return map;

        for (String entry : items.split("\\s*,\\s*")) {
            // Split on '=' first to strip the price part
            String left = entry.split("\\s*=\\s*")[0].trim();
            // Now left is like "Momo x1"
            int xIdx = left.lastIndexOf(" x");
            if (xIdx == -1) continue;
            String name = left.substring(0, xIdx).trim();
            try {
                int qty = Integer.parseInt(left.substring(xIdx + 2).trim());
                map.merge(name, qty, Integer::sum);
            } catch (NumberFormatException ignored) {}
        }
        return map;
    }

    /** Given a map of name -> qty and a price lookup function,
        rebuild the items string with line totals. */
    public static String rebuild(Map<String, Integer> quantities,
                                 java.util.function.ToDoubleFunction<String> priceOf) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : quantities.entrySet()) {
            String name = e.getKey();
            int qty = e.getValue();
            double lineTotal = priceOf.applyAsDouble(name) * qty;

            if (sb.length() > 0) sb.append(", ");
            sb.append(name)
              .append(" x").append(qty)
              .append(" = Rs. ").append(String.format("%.2f", lineTotal));
        }
        return sb.toString();
    }
}