package com.example.second;

import net.lingala.zip4j.exception.ZipException;
import net.sf.sevenzipjbinding.*;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import net.sf.sevenzipjbinding.impl.RandomAccessFileOutStream;
import org.springframework.util.StopWatch;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class ZipUnlock {
    public static final String ERROR_PREFIX = "[ERROR] ";
    private static final AtomicBoolean hasErrors = new AtomicBoolean(false);
    private static final AtomicInteger count = new AtomicInteger(0);
    private final static List<String> rightPasswords = new CopyOnWriteArrayList<>();
    private final static List<String> PASSWORDS = List.of("", "Fvvbfr32025", "Fduecn32025", "Fllbrn32025", "Fljybc32025", "Fpfcey32025", "Fpehbn32025", "Fqlfyf32025", "Fqrblj32025", "Fqvfhf32025", "Fqcfhb32025", "Fqnfkf32025", "Frfyif32025", "Frptht32025", "Frvjkf32025", "Frcfbn32025", "Frcbjv32025", "Fkfpjy32025", "Fkdthf32025", "Fktqrf32025", "Fkktkm32025", "Fvfynf32025", "Fvbfyn32025", "Fvajhf32025", "Fyfkbp32025", "Fyfybv32025", "Fyujhf32025", "Fyyfns32025", "Fynbaf32025", "Fgjabp32025", "Fhlfbn32025", "Fnkfyn32025", "Fhabcn32025", "Fafnbr32025", "Fhntkm32025", "Fhvfnf32025", "Fqceke32025", "Fqdjhb32025", "Fqlfyf32025");

    public static void unzipAll(Path sourcePath, Path outputPath, Consumer<String> log) {
        hasErrors.set(false);
        Consumer<String> trackedLog = message -> {
            if (message.startsWith(ERROR_PREFIX)) {
                hasErrors.set(true);
            }
            log.accept(message);
        };
        rightPasswords.clear();
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        trackedLog.accept("Начинаю распаковывать...");
        try {
            Files.createDirectories(outputPath);
            if (Files.isRegularFile(sourcePath)) {
                if (isSupportedArchive(sourcePath)) {
                    try {
                        unzip(sourcePath, outputPath, trackedLog);
                    } catch (Exception e) {
                        trackedLog.accept(ERROR_PREFIX + "Ошибка при распаковке архива: " + sourcePath.getFileName());
                        trackedLog.accept(ERROR_PREFIX + e.getClass().getSimpleName() + ": " + e.getMessage());
                    }
                } else {
                    trackedLog.accept(ERROR_PREFIX + "Неподдерживаемый файл: " + sourcePath.getFileName());
                }
            } else {
                try (ExecutorService executor = Executors.newFixedThreadPool(4)) {
                    try (Stream<Path> file = Files.walk(sourcePath)) {
                        file.filter(Files::isRegularFile).filter(ZipUnlock::isSupportedArchive).forEach(c -> executor.submit(() -> {
                            try {
                                count.incrementAndGet();
                                unzip(c, outputPath, trackedLog);
                            } catch (Exception e) {
                                trackedLog.accept(ERROR_PREFIX + "Ошибка при распаковке архива: " + c.getFileName());
                                trackedLog.accept(ERROR_PREFIX + e.getClass().getSimpleName() + ": " + e.getMessage());
                            }
                        }));
                    }
                    executor.shutdown();
                    boolean finished = executor.awaitTermination(1, TimeUnit.HOURS);
                    if (!finished) {
                        trackedLog.accept(ERROR_PREFIX + "Не все архивы успели обработаться");
                        executor.shutdownNow();
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            trackedLog.accept(ERROR_PREFIX + "Распаковка была прервана");
            throw new RuntimeException(e);
        } catch (IOException e) {
            trackedLog.accept(ERROR_PREFIX + "Ошибка файловой системы: " + e.getMessage());
            throw new RuntimeException(e);
        }
        if (hasErrors.get()) {
            FirstForm.playWindowsSound("Windows Critical Stop.wav");
            trackedLog.accept(ERROR_PREFIX + "Все папки распакованы, но есть ошибки");
        } else {
            FirstForm.playWindowsSound("Windows Unlock.wav");
            trackedLog.accept("\n" + "Все папки распакованы");
        }
        stopWatch.stop();
        trackedLog.accept("Время: " + stopWatch.getTotalTimeSeconds() + " сек." + "\n" +
                "Распаковано архивов: " + count);
    }

    public static boolean isSupportedArchive(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".zip") || name.endsWith(".rar")
                || name.endsWith(".7z") || name.endsWith(" .zip")
                || name.endsWith(" .rar") || name.endsWith(" .7z");
    }

    private static void unzip(Path paths, Path outputPath, Consumer<String> trackedLog) throws ZipException {
        extractWithSevenZip(paths, outputPath, trackedLog);
    }

    private static void extractWithSevenZip(Path archivePath, Path outputPath, Consumer<String> trackedLog) {
        String archiveName = archivePath.getFileName().toString();
        try {
            if (!SevenZip.isInitializedSuccessfully()) SevenZip.initSevenZipFromPlatformJAR();
        } catch (Exception ignored) {
        }

        for (String password : PASSWORDS) {
            try (RandomAccessFile raf = new RandomAccessFile(archivePath.toFile(), "r");
                 RandomAccessFileInStream rafStream = new RandomAccessFileInStream(raf);
                 // null означает: "Движок, сам пойми что это за формат (zip/rar/7z) и распарси его"
                 IInArchive inArchive = SevenZip.openInArchive(null, rafStream, password)) {

                Path archiveOutputDir = createArchiveOutputDir(archivePath, outputPath);
                RarExtractCallback callback = new RarExtractCallback(inArchive, archiveOutputDir.toString(), password);

                inArchive.extract(null, false, callback);

                if (!callback.hasError) {
                    trackedLog.accept(archiveName + ", пароль: " + (password.isEmpty() ? "(без пароля)" : password));
                    return;
                }
            } catch (Exception ignored) {
            }
        }
        trackedLog.accept(ERROR_PREFIX + "Архив не распакован, пароль не найден или поврежден: " + archiveName);
        hasErrors.set(true);
    }

    private static Path createArchiveOutputDir(Path archivePath, Path outputPath) throws IOException {
        String archiveName = archivePath.getFileName().toString();
        String folderName = removeArchiveExtension(archiveName).trim();
        Path archiveOutputDir = outputPath.resolve(folderName);
        Files.createDirectories(archiveOutputDir);

        return archiveOutputDir;
    }

    private static String removeArchiveExtension(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".zip")) {
            return fileName.substring(0, fileName.length() - 4);
        }
        if (lower.endsWith(".rar")) {
            return fileName.substring(0, fileName.length() - 4);
        }
        if (lower.endsWith(".7z")) {
            return fileName.substring(0, fileName.length() - 3);
        }
        return fileName;
    }

    private static class RarExtractCallback implements IArchiveExtractCallback, ICryptoGetTextPassword {
        private final IInArchive inArchive;
        private final String destinationDirectory;
        private final String password;
        public boolean hasError = false; // Сюда мы ловим ошибку неверного пароля
        private RandomAccessFile currentOutStream;

        public RarExtractCallback(IInArchive inArchive, String destDir, String pass) {
            this.inArchive = inArchive;
            this.destinationDirectory = destDir;
            this.password = pass;
        }

        @Override
        public ISequentialOutStream getStream(int index, ExtractAskMode mode) throws SevenZipException {
            if (mode != ExtractAskMode.EXTRACT) return null;
            Boolean isFolder = (Boolean) inArchive.getProperty(index, PropID.IS_FOLDER);
            String path = (String) inArchive.getProperty(index, PropID.PATH);
            // Подстраховка на случай, если 7-Zip наткнется на скрытый системный файл без имени
            if (path == null) {
                path = "Файл_без_имени_" + index;
            }
            // МАГИЧЕСКАЯ ОЧИСТКА ПУТИ:
            // 1. Убираем виндовые запрещенные символы
            path = path.replaceAll("[:*?\"<>|]", "_");
            // 2. Убираем ЛЮБОЕ количество пробелов ПЕРЕД любыми слешами (спасает от "папка  /файл")
            path = path.replaceAll(" +(?=[/\\\\])", "");
            // 3. Убираем пробел в самом конце имени файла, если он там есть
            path = path.trim();

            java.io.File outFile = java.nio.file.Paths.get(destinationDirectory, path).toFile();

            if (isFolder != null && isFolder) return null;
            outFile.getParentFile().mkdirs();

            try {
                currentOutStream = new RandomAccessFile(outFile, "rw");
                return new RandomAccessFileOutStream(currentOutStream);
            } catch (Exception e) {
                // Если файл всё равно не создался, пробрасываем ошибку, чтобы движок понял, что файл битый
                throw new SevenZipException(e);
            }
        }

        @Override
        public void setOperationResult(ExtractOperationResult result) {
            // Магия здесь: если пароль не подошел, результат будет DATA_ERROR, и мы ставим флаг ошибки
            if (result != ExtractOperationResult.OK) hasError = true;
            if (currentOutStream != null) {
                try {
                    currentOutStream.close();
                } catch (IOException e) {
                }
                currentOutStream = null;
            }
        }

        @Override
        public String cryptoGetTextPassword() {
            return password;
        }

        @Override
        public void prepareOperation(ExtractAskMode mode) {
        }

        @Override
        public void setTotal(long total) {
        }

        @Override
        public void setCompleted(long complete) {
        }
    }
}