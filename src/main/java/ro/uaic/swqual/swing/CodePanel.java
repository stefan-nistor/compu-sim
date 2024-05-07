package ro.uaic.swqual.swing;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

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
        JButton chooseFileButton = new JButton("Choose File..");

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

        chooseFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                int option = fileChooser.showOpenDialog(SwingUtilities.getWindowAncestor(chooseFileButton));
                if (option == JFileChooser.APPROVE_OPTION) {
                    String selectedFilePath = fileChooser.getSelectedFile().getAbsolutePath();
                    // Read file content and display in text area
                    readFileContent(selectedFilePath);
                    System.out.println("Selected file: " + selectedFilePath);
                }
            }
        });

        buttonPanel.add(compileButton);
        buttonPanel.add(runButton);
        buttonPanel.add(stopButton);
        buttonPanel.add(chooseFileButton);

        add(buttonPanel, BorderLayout.NORTH);
    }

    private void readFileContent(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            codeTextArea.setText(content.toString());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error reading file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
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
