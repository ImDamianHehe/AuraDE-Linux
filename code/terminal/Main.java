import javax.swing.*;
import java.awt.*;
import java.io.*;

public class Main {

    public static void main(String[] args) {
        JFrame frame = new JFrame("AuraMass");

        frame.setSize(650, 450);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.DARK_GRAY);

        JTextArea terminal = new JTextArea();
        terminal.setBackground(Color.DARK_GRAY);
        terminal.setForeground(Color.WHITE);
        terminal.setCaretColor(Color.WHITE);
        terminal.setEditable(false);

        JTextField runCommandField = new JTextField();

        panel.add(new JScrollPane(terminal), BorderLayout.CENTER);
        panel.add(runCommandField, BorderLayout.SOUTH);

        frame.add(panel);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        try {
            // Start Bash
            ProcessBuilder processBuilder = new ProcessBuilder("/bin/bash");
            Process process = processBuilder.start();

            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(process.getOutputStream())
            );

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );

            // Thread that constantly reads Bash output
            Thread outputThread = new Thread(() -> {
                try {
                    String line;

                    while ((line = reader.readLine()) != null) {
                        String output = line;

                        SwingUtilities.invokeLater(() -> {
                            terminal.append(output + "\n");
                        });
                    }

                } catch (IOException e) {
                    SwingUtilities.invokeLater(() -> {
                        terminal.append("Error reading output: "
                                + e.getMessage() + "\n");
                    });
                }
            });

            outputThread.start();

            // Run command when Enter is pressed
            runCommandField.addActionListener(e -> {
                try {
                    String command = runCommandField.getText();

                    if (!command.isEmpty()) {
                        writer.write(command);
                        writer.newLine();
                        writer.flush();

                        runCommandField.setText("");
                    }

                } catch (IOException ex) {
                    terminal.append("Error: "
                            + ex.getMessage() + "\n");
                }
            });

        } catch (IOException e) {
            terminal.append(
                    "Failed to start Bash: " + e.getMessage() + "\n"
            );
        }
    }
}
