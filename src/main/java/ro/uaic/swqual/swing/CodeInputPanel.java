package ro.uaic.swqual.swing;

import ro.uaic.swqual.Parser;
import ro.uaic.swqual.model.Instruction;
import ro.uaic.swqual.proc.CentralProcessingUnit;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;

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
    private CpuOrchestrator cpuOrchestrator;

    public void setCpuOrchestrator(CpuOrchestrator orchestrator) {
        cpuOrchestrator = orchestrator;
    }

    public void run() {
        if (cpuOrchestrator == null) {
            return;
        }

        cpuOrchestrator.run();
    }

    public void stop() {
        if (cpuOrchestrator == null) {
            return;
        }

        if (cpuOrchestrator.getState() == CpuOrchestrator.State.STOPPED) {
            cpuOrchestrator.step();
        } else {
            cpuOrchestrator._break();
        }
    }

    public void load(List<Instruction> instructions) {
        cpuOrchestrator.setInstructions(instructions);
    }

    public void reset() {
        parser = new Parser();
    }

    public CodeInputPanel() {
        runButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                run();
            }
        });
        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stop();
            }
        });
        chooseFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser("/home/loghin/IdeaProjects/compu-sim/src/test/resources/checks");
                int option = fileChooser.showOpenDialog(SwingUtilities.getWindowAncestor(chooseFileButton));
                if (option == JFileChooser.APPROVE_OPTION) {
                    String selectedFilePath = fileChooser.getSelectedFile().getAbsolutePath();
                    // Read file content and display in text area
                    readFileContent(selectedFilePath);

                    reset();
                    load(parser.parse(selectedFilePath)
                            .resolveReferences(cpuOrchestrator.getCentralProcessingUnit().getRegistryReferenceMap())
                            .link().getInstructions());

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

    public void update() {

    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("CodeInputPanel");
        var codeInputPanel = new CodeInputPanel();
        frame.setContentPane(codeInputPanel.panel1);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        var orch = new CpuOrchestrator(Map.of());
        codeInputPanel.setCpuOrchestrator(orch);
        orch.addUpdateListener(codeInputPanel::update);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    orch.terminate();
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
    }
}
