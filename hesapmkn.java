import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Main {
    private static double result = 0;
    private static String currentOperator = "";
    private static boolean startNewNumber = true;
    private static JTextField display;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> createAndShowGui());
    }

    private static void createAndShowGui() {
        JFrame frame = new JFrame("Hesap Makinesi");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(320, 420);
        frame.setLayout(new BorderLayout(6,6));
        frame.setResizable(false);

        display = new JTextField("0");
        display.setEditable(false);
        display.setHorizontalAlignment(SwingConstants.RIGHT);
        display.setFont(new Font("SansSerif", Font.BOLD, 28));
        display.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        frame.add(display, BorderLayout.NORTH);

        JPanel buttons = new JPanel(new GridLayout(5, 4, 6, 6));
        String[] labels = {
                "C", "←", "/", "*",
                "7", "8", "9", "-",
                "4", "5", "6", "+",
                "1", "2", "3", "=",
                "0", "0", ".", "="
        };

        addButton(buttons, "C", e -> clear());
        addButton(buttons, "←", e -> backspace());
        addButton(buttons, "/", e -> operatorPressed("/"));
        addButton(buttons, "*", e -> operatorPressed("*"));

        addButton(buttons, "7", e -> numberPressed("7"));
        addButton(buttons, "8", e -> numberPressed("8"));
        addButton(buttons, "9", e -> numberPressed("9"));
        addButton(buttons, "-", e -> operatorPressed("-"));

        addButton(buttons, "4", e -> numberPressed("4"));
        addButton(buttons, "5", e -> numberPressed("5"));
        addButton(buttons, "6", e -> numberPressed("6"));
        addButton(buttons, "+", e -> operatorPressed("+"));

        addButton(buttons, "1", e -> numberPressed("1"));
        addButton(buttons, "2", e -> numberPressed("2"));
        addButton(buttons, "3", e -> numberPressed("3"));
        addButton(buttons, "=", e -> equalsPressed());

        JButton zero = new JButton("0");
        zero.addActionListener(e -> numberPressed("0"));
        zero.setFont(new Font("SansSerif", Font.BOLD, 20));
        buttons.add(zero);

        addButton(buttons, "00", e -> numberPressed("00"));
        addButton(buttons, ".", e -> decimalPressed());

        frame.add(buttons, BorderLayout.CENTER);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void addButton(JPanel panel, String text, ActionListener al) {
        JButton b = new JButton(text);
        b.setFont(new Font("SansSerif", Font.BOLD, 20));
        b.addActionListener(al);
        panel.add(b);
    }

    private static void numberPressed(String number) {
        if (startNewNumber) {
            display.setText(number.equals("00") ? "0" : number);
            startNewNumber = false;
        } else {
            if (display.getText().equals("0") && number.equals("0")) return;
            display.setText(display.getText() + number);
        }
    }

    private static void decimalPressed() {
        if (startNewNumber) {
            display.setText("0.");
            startNewNumber = false;
            return;
        }
        if (!display.getText().contains(".")) {
            display.setText(display.getText() + ".");
        }
    }

    private static void operatorPressed(String op) {
        try {
            double displayValue = Double.parseDouble(display.getText());
            if (currentOperator.isEmpty()) {
                result = displayValue;
            } else {
                result = calculate(result, displayValue, currentOperator);
                display.setText(trimDouble(result));
            }
            currentOperator = op;
            startNewNumber = true;
        } catch (NumberFormatException ex) {
            display.setText("Error");
            startNewNumber = true;
            currentOperator = "";
        }
    }

    private static void equalsPressed() {
        try {
            if (currentOperator.isEmpty()) return;
            double displayValue = Double.parseDouble(display.getText());
            result = calculate(result, displayValue, currentOperator);
            display.setText(trimDouble(result));
            currentOperator = "";
            startNewNumber = true;
        } catch (NumberFormatException ex) {
            display.setText("Error");
            startNewNumber = true;
            currentOperator = "";
        } catch (ArithmeticException ae) {
            display.setText("Error");
            startNewNumber = true;
            currentOperator = "";
        }
    }

    private static double calculate(double a, double b, String op) {
        switch (op) {
            case "+":
                return a + b;
            case "-":
                return a - b;
            case "*":
                return a * b;
            case "/":
                if (b == 0) throw new ArithmeticException("Division by zero");
                return a / b;
            default:
                return b;
        }
    }

    private static void clear() {
        display.setText("0");
        result = 0;
        currentOperator = "";
        startNewNumber = true;
    }

    private static void backspace() {
        String s = display.getText();
        if (s.length() <= 1) {
            display.setText("0");
            startNewNumber = true;
        } else {
            display.setText(s.substring(0, s.length() - 1));
        }
    }
    private static String trimDouble(double d) {
        if (d == (long) d) return String.format("%d", (long) d);
        else return String.format("%s", d);
    }
}
