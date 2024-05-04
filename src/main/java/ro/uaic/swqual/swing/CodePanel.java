package ro.uaic.swqual.swing;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class CodePanel extends JPanel {

    private JTextArea codeTextArea;

    public CodePanel() {
        setLayout(new BorderLayout());

        codeTextArea = new JTextArea();
        JScrollPane scrollPane = new JScrollPane(codeTextArea);
        add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton compileButton = new JButton("Compile");
        JButton runButton = new JButton("Run");
        JButton stopButton = new JButton("Stop");

        compileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Compiling...");
            }
        });

        runButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Running...");
            }
        });

        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Stopping...");
            }
        });

        buttonPanel.add(compileButton);
        buttonPanel.add(runButton);
        buttonPanel.add(stopButton);

        add(buttonPanel, BorderLayout.NORTH);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Code Panel");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(600, 400);

            CodePanel codePanel = new CodePanel();
            frame.add(codePanel);

            frame.setVisible(true);
        });
    }
}
