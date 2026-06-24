/**
 * Main entry point for the First Bank Uganda — New Account Opening Application.
 * Sets the system look and feel for a native appearance and launches the GUI.
 */
public class Main {

    public static void main(String[] args) {
        // Set system look and feel for native OS appearance
        try {
            javax.swing.UIManager.setLookAndFeel(
                    javax.swing.UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Fall back to default look and feel
            System.err.println("Could not set system look and feel: " + e.getMessage());
        }

        // Launch the GUI on the Event Dispatch Thread (thread-safety)
        java.awt.EventQueue.invokeLater(() -> {
            new view.BankAccountForm();
        });
    }
}    