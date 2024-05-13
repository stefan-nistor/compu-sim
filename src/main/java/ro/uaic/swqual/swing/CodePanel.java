package ro.uaic.swqual.swing;

import javax.swing.*;
import java.awt.*;

public class CodePanel extends JPanel {

    private final JTextArea codeArea = new JTextArea();


    public CodePanel() {
        setLayout(new BorderLayout());
        codeArea.setEditable(true);
        codeArea.setLineWrap(false);
        codeArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        var scrollPane = new JScrollPane(codeArea);
        add(scrollPane, BorderLayout.WEST);

        scrollPane.setPreferredSize(new Dimension(500, 500));
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
    }

    public String getCode() {
        return codeArea.getText();
    }

    public void setCode(String code) {
        codeArea.setText(code);
    }

}
