package ro.uaic.swqual.swing;

import javax.swing.*;
import java.awt.*;

public class CPUEmulator {

    private static final int WINDOW_HEIGHT = 800;
    private static final int WINDOW_WIDTH = 800;

    public static void main(String[] args) {
        var frame = new JFrame("CPU Emulator");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout());
        var codePanel = new CodePanel();

        frame.getContentPane().add(codePanel);
        frame.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        frame.setVisible(true);
    }
}
