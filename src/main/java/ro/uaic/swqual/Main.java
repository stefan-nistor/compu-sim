package ro.uaic.swqual;

import ro.uaic.swqual.swing.SimulatorInterface;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SimulatorInterface simulatorInterface = new SimulatorInterface();
            simulatorInterface.setVisible(true);
        });
    }
}
