package ro.uaic.swqual.swing;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class KeyboardPanel extends JPanel {
    private List<Consumer<Character>> onPress = new ArrayList<>();

    public void addOnPressListener(Consumer<Character> listener) {
        onPress.add(listener);
    }

    public KeyboardPanel() {
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
                var val  = src.getText().charAt(0);
                onPress.forEach(listener -> listener.accept(val));
            });
            add(button);
        }

    }
}
