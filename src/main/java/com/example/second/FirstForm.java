package com.example.second;

import javax.sound.sampled.*;
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
    private JButton button1;
    private JButton button3;
    private JButton button2;
    private JTextPane logTextArea;
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
        installDropHandlers();
        setVisible(true);
    }

    private void installDropHandlers() {
        button1.setTransferHandler(createSourceDropHandler());
        button2.setTransferHandler(createOutputDropHandler());
    }

    private JPanel createContentPane() {
        JPanel contentPane = new JPanel(new BorderLayout(8, 8));

        panel1 = new JPanel(new GridLayout(1, 3, 8, 8));
        button1 = new JButton("Откуда");
        button2 = new JButton("Куда");
        button3 = new JButton("Распаковать");

        panel1.add(button1);
        panel1.add(button2);
        panel1.add(button3);
        logTextArea = new JTextPane();
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
            Path selectedPath = jFileChooser.getSelectedFile().toPath();

            if (!isValidSourcePath(selectedPath)) {
                JOptionPane.showMessageDialog(this, "Выбери архив .zip/.rar/.7z или папку");
                return;
            }
            sourcePath = selectedPath;
            updateButtonPath(button1, sourcePath);
        }
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

                    sourcePath = droppedPath;
                    updateButtonPath(button1, sourcePath);
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
