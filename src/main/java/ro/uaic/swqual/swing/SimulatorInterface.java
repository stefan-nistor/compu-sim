package ro.uaic.swqual.swing;

import javax.swing.*;
import java.awt.*;

public class SimulatorInterface extends JFrame {

    public SimulatorInterface() {
        setTitle("Simulator Interface");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 600);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        add(mainPanel);

        JLabel headerLabel = new JLabel("Simulator Interface", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 20));
        GridBagConstraints gbcHeader = new GridBagConstraints();
        gbcHeader.gridx = 0;
        gbcHeader.gridy = 0;
        gbcHeader.gridwidth = 2;
        gbcHeader.fill = GridBagConstraints.HORIZONTAL;
        gbcHeader.insets = new Insets(10, 0, 20, 0);
        mainPanel.add(headerLabel, gbcHeader);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 0.5;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(10, 20, 20, 0);

        CodePanel codePanel = new CodePanel();
        codePanel.setPreferredSize(new Dimension(200, 700));
        mainPanel.add(codePanel, gbc);

        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 0.5;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(10, 20, 10, 10); // Spațiere între componente

        JPanel processorPanel = new JPanel();
        processorPanel.setBorder(BorderFactory.createTitledBorder("Processor Section"));
        processorPanel.setPreferredSize(new Dimension(400, 150));
        mainPanel.add(processorPanel, gbc);

        gbc.gridy++;
        JPanel flags = new JPanel();
        flags.setBorder(BorderFactory.createTitledBorder("Flags"));
        flags.setPreferredSize(new Dimension(200, 100));
        mainPanel.add(flags, gbc);

        gbc.gridy++;
        JPanel memoryPanel = new JPanel();
        memoryPanel.setBorder(BorderFactory.createTitledBorder("Memory Section"));
        memoryPanel.setPreferredSize(new Dimension(200, 150));
        mainPanel.add(memoryPanel, gbc);
    }
}
