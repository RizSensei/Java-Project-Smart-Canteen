package ui;

import java.awt.Color;
import java.awt.Font;

public class UITheme {

    // Primary palette
    public static final Color PRIMARY       = new Color(0xC2185B);  // pink
    public static final Color PRIMARY_DARK  = new Color(0x880E4F);
    public static final Color ACCENT        = new Color(0xAD1457);
    public static final Color DANGER        = new Color(0xC62828);  // red
    public static final Color BG            = new Color(0xF5F5F5);  // light gray
    public static final Color CARD          = Color.WHITE;
    public static final Color TEXT          = new Color(0x212121);
    public static final Color TEXT_MUTED    = new Color(0x757575);
    public static final Color BORDER        = new Color(0xE0E0E0);

    // Status colors
    public static final Color PENDING_BG    = new Color(0xFFF3CD);
    public static final Color PENDING_FG    = new Color(0x8A6D00);
    public static final Color READY_BG      = new Color(0xFCE4EC);
    public static final Color READY_FG      = new Color(0x880E4F);
    public static final Color PAID_BG      = new Color(0xE3F2FD);
    public static final Color PAID_FG      = new Color(0x0D47A1);

    // Fonts
    public static final Font H1       = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font H2       = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font BODY     = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font BODY_B   = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font SMALL    = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font MONO     = new Font("Consolas", Font.PLAIN, 13);
}