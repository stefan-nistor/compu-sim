package ro.uaic.swqual.swing;

import ro.uaic.swqual.Parser;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class CodeInputPanel extends JPanel {
    private JPanel panel1;
    private JButton stopButton;
    private JTextArea codeArea;
    private JButton runButton;
    private JButton chooseFileButton;
    private JTextArea registryView;
    private JTextArea textArea3;
    private JTextPane textPane1;
    private JTextArea textArea4;

    private static Parser parser = new Parser();


    public void reset() {
        parser = new Parser();
    }

    public CodeInputPanel() {
        runButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });
        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

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

                    reset();
                    parser.parse(selectedFilePath);

                    System.out.println("Selected file: " + selectedFilePath);
                    System.out.println("Read code: " + parser.getInstructions().toString());
                }
            }
        });

    }

    private void readFileContent(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            codeArea.setText(content.toString());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error reading file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public Parser getParser(){
        return parser;
    }

    public void setParser(Parser p){
        parser = p;
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("CodeInputPanel");
        frame.setContentPane(new CodeInputPanel().panel1);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
}
