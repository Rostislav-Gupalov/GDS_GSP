import javax.swing.*;

public class App {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DrillingProgram drillingProgram = new DrillingProgram();
            drillingProgram.setVisible(true);
        });
    }
}