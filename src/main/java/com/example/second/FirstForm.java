package com.example.second;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.List;

public class FirstForm extends JFrame {
    private JPanel panel1;
    private JButton button1;
    private JButton button3;
    private JButton button2;
    private JTextArea logTextArea;
    private Path sourcePath;
    private Path outputPath;


    public FirstForm() {
        super("UnZip_2.0");
        setSize(700, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(createContentPane());
        setResizable(false);
        setLocationRelativeTo(null);
        button1.addActionListener(e -> chooseSourceFolder());
        button2.addActionListener(e -> chooseOutputFolder());
        button3.addActionListener(e -> startUnpacking());
        setVisible(true);
    }

    private JPanel createContentPane() {
        JPanel contentPane = new JPanel(new BorderLayout(8, 8));
        logTextArea = new JTextArea();
        logTextArea.setEditable(false);
        contentPane.add(panel1, BorderLayout.NORTH);
        contentPane.add(new JScrollPane(logTextArea), BorderLayout.CENTER);
        return contentPane;
    }

    private void chooseSourceFolder() {
        JFileChooser jFileChooser = new JFileChooser();
        jFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = jFileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            sourcePath = jFileChooser.getSelectedFile().toPath();
            button1.setText(sourcePath.getFileName().toString());
        }
    }

    private void chooseOutputFolder() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            outputPath = fileChooser.getSelectedFile().toPath();
            button2.setText(outputPath.getFileName().toString());
        }
    }

    private void startUnpacking() {
        if (sourcePath == null) {
            JOptionPane.showMessageDialog(this, "Выбери папку ОТКУДА распаковывать");
            return;
        }
        if (outputPath == null) {
            JOptionPane.showMessageDialog(this, "Выбери папку КУДА распаковывать");
            return;
        }
        button3.setEnabled(false);
        button3.setText("Работаю...");
        logTextArea.setText("");

        SwingWorker<Void, String> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                ZipUnlock.unzipAll(sourcePath, outputPath, message -> publish(message));
                return null;
            }

            @Override
            protected void process(List<String> messages) {
                for (String message : messages) {
                    logTextArea.append(message + "\n");
                }
            }

            @Override
            protected void done() {
                button3.setEnabled(true);
                button3.setText("Распаковать");

                try {
                    get();
                    logTextArea.append("Готово\n");
                    JOptionPane.showMessageDialog(FirstForm.this, "Распаковка завершена");
                } catch (Exception e) {
                    logTextArea.append("Ошибка: " + e.getMessage() + "\n");
                    JOptionPane.showMessageDialog(FirstForm.this,
                            "Ошибка при распаковке: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }
}
