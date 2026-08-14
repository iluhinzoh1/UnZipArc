package com.example.second;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class FirstForm extends JFrame {
    private JPanel panel1;
    private JPanel panel2;
    private JButton button1;
    private JButton button2;
    private JButton button3;
    private JButton button4;
    private JTextPane logTextArea;
    private Path sourcePath;
    private Path outputPath;

    public FirstForm() {
        super("UnZip_FINAL");
        setSize(700, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(createContentPane());
        setResizable(false);
        setLocationRelativeTo(null);
        button1.addActionListener(e -> chooseSourceFolder());
        button2.addActionListener(e -> chooseOutputFolder());
        button3.addActionListener(e -> startUnpacking());
        button4.addActionListener(e -> JOptionPane.showMessageDialog(null,
                message, "От Новикова, 11.12.2026 ДМБ", JOptionPane.INFORMATION_MESSAGE));
        installDropHandlers();
        setVisible(true);
    }

    private final String message = "Я очень долго делал эту залупу" +
            ", очень много вложено пота" +
            ", слез и других жидкостей, " +
            "\n" + "цените эту хрень, во время призыва много времени сэкономит :)"
            +
            "\n" +
            "\n" + "P.S Если вылетает какая либо ошибка, то пробуйте всегда "
            +
            "\n" + "распаковать архив вручную, через Сборного Шибанутого."
            +
            "\n" +
            "\n" + "Вы со всем справитесь и все у вас будет хорошо" +
            "\n" + "P.S.S Не ешьте бершбаршмаки © Гросул";


    private void installDropHandlers() {
        button1.setTransferHandler(createSourceDropHandler());
        button2.setTransferHandler(createOutputDropHandler());
    }

    private JPanel createContentPane() {
        JPanel contentPane = new JPanel(new BorderLayout(8, 8));
        panel1 = new JPanel(new GridLayout(1, 3, 8, 8));
        panel2 = new JPanel(new GridLayout(0, 7, 8, 8));
        button1 = new JButton("Откуда");
        button2 = new JButton("Куда");
        button3 = new JButton("Распаковать");
        button4 = new JButton("От Деда");
        panel1.add(button1);
        panel1.add(button2);
        panel1.add(button3);
        panel2.add(button4);
        logTextArea = new JTextPane();
        logTextArea.setEditable(false);
        contentPane.add(panel1, BorderLayout.NORTH);
        contentPane.add(panel2, BorderLayout.SOUTH);
        contentPane.add(new JScrollPane(logTextArea), BorderLayout.CENTER);
        return contentPane;
    }

    private void chooseSourceFolder() {
        JFileChooser jFileChooser = new JFileChooser();
        jFileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        int result = jFileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            Path selectedPath = jFileChooser.getSelectedFile().toPath();

            if (!isValidSourcePath(selectedPath)) {
                JOptionPane.showMessageDialog(this, "Выбери архив .zip/.rar/.7z или папку");
                return;
            }

            setSourcePathAndAutoOutput(selectedPath);
        }
    }

    private void setSourcePathAndAutoOutput(Path selectedPath) {
        sourcePath = selectedPath;
        updateButtonPath(button1, sourcePath);

        if (Files.isDirectory(selectedPath)) {
            outputPath = selectedPath;
        } else {
            outputPath = selectedPath.getParent();
        }

        updateButtonPath(button2, outputPath);
    }

    private TransferHandler createSourceDropHandler() {
        return new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @Override
            public boolean importData(TransferSupport support) {
                if (!canImport(support)) {
                    return false;
                }

                try {
                    Path droppedPath = extractDroppedPath(support);

                    if (droppedPath == null || !isValidSourcePath(droppedPath)) {
                        JOptionPane.showMessageDialog(
                                FirstForm.this,
                                "На кнопку 'Откуда' можно бросать только архив или папку"
                        );
                        return false;
                    }

                    setSourcePathAndAutoOutput(droppedPath);
                    return true;
                } catch (UnsupportedFlavorException | IOException e) {
                    JOptionPane.showMessageDialog(
                            FirstForm.this,
                            "Ошибка перетаскивания: " + e.getMessage()
                    );
                    return false;
                }
            }
        };
    }

    private TransferHandler createOutputDropHandler() {
        return new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @Override
            public boolean importData(TransferSupport support) {
                if (!canImport(support)) {
                    return false;
                }

                try {
                    Path droppedPath = extractDroppedPath(support);

                    if (droppedPath == null || !Files.isDirectory(droppedPath)) {
                        JOptionPane.showMessageDialog(
                                FirstForm.this,
                                "На кнопку 'Куда' можно бросать только папку"
                        );
                        return false;
                    }

                    outputPath = droppedPath;
                    updateButtonPath(button2, outputPath);
                    return true;
                } catch (UnsupportedFlavorException | IOException e) {
                    JOptionPane.showMessageDialog(
                            FirstForm.this,
                            "Ошибка перетаскивания: " + e.getMessage()
                    );
                    return false;
                }
            }
        };
    }

    @SuppressWarnings("unchecked")
    private Path extractDroppedPath(TransferHandler.TransferSupport support)
            throws UnsupportedFlavorException, IOException {
        List<File> files = (List<File>) support.getTransferable()
                .getTransferData(DataFlavor.javaFileListFlavor);

        if (files == null || files.isEmpty()) {
            return null;
        }

        return files.get(0).toPath();
    }

    private boolean isValidSourcePath(Path path) {
        return Files.isDirectory(path)
                || (Files.isRegularFile(path) && ZipUnlock.isSupportedArchive(path));
    }

    private void updateButtonPath(JButton button, Path path) {
        button.setText(path.getFileName().toString());
        button.setToolTipText(path.toString());
    }

    private void appendLog(String message) {
        SimpleAttributeSet attributeSet = new SimpleAttributeSet();
        if (message.startsWith("[ERROR]")) {
            StyleConstants.setForeground(attributeSet, Color.RED);
            message = message.substring("[ERROR]".length()).trim();
        } else {
            StyleConstants.setForeground(attributeSet, Color.BLACK);
        }
        StyledDocument doc = logTextArea.getStyledDocument();
        try {
            doc.insertString(doc.getLength(), message + "\n", attributeSet);
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }

    public static void playWindowsSound(String fileName) {
        File soundFile = new File("C:/Windows/Media/" + fileName);
        try {
            Clip clip = AudioSystem.getClip();
            clip.open(AudioSystem.getAudioInputStream(soundFile));
            clip.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void chooseOutputFolder() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            outputPath = fileChooser.getSelectedFile().toPath();
            updateButtonPath(button2, outputPath);
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
                    appendLog(message);
                }
            }

            @Override
            protected void done() {
                button3.setEnabled(true);
                button3.setText("Распаковать");

                try {
                    get();
                    JOptionPane.showMessageDialog(FirstForm.this, "Распаковка завершена");
                } catch (Exception e) {
                    appendLog("[ERROR] Ошибка: " + e.getMessage() + "\n");
                    JOptionPane.showMessageDialog(FirstForm.this,
                            "Ошибка при распаковке: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }
}