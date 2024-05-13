package ro.uaic.swqual.swing;

import ro.uaic.swqual.model.peripheral.Keyboard;

import javax.swing.*;
import java.awt.*;

public class KeyboardPanel extends JPanel {

    public KeyboardPanel(Keyboard kb) {
        setLayout(new GridLayout(4, 3, 5, 5));
        var buttonLabels = new String[]{
                "1", "2", "3",
                "4", "5", "6",
                "7", "8", "9",
                "#", "0", "*"
        };

        for (String buttonLabel : buttonLabels) {
            var button = new JButton(buttonLabel);
            button.setFont(new Font("Monospaced", Font.PLAIN, 12));
            button.addActionListener(e -> {
                var src = (JButton) e.getSource();
                var val  = src.getText();
            });
            add(button);
        }

    }
}
