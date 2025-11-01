package clinic.ui;

/**
 * Panels that can refresh their UI data and should be notified by global refresh actions.
 */
public interface Refreshable {
    void refreshData();
}
