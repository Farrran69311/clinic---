package clinic.ui.common;

import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

/**
 * Shared UI helpers for aligning panel padding and spacing between toolbars and content areas.
 */
public final class UIUtils {
    private static final Border DEFAULT_PAGE_BORDER = new EmptyBorder(12, 12, 12, 12);
    private static final Border DEFAULT_HEADER_GAP = new EmptyBorder(0, 0, 10, 0);

    private UIUtils() {
    }

    public static void applyPagePadding(JPanel panel) {
        if (panel.getBorder() == null) {
            panel.setBorder(DEFAULT_PAGE_BORDER);
        }
    }

    public static void applyHeaderSpacing(JPanel header) {
        header.setBorder(DEFAULT_HEADER_GAP);
    }
}
