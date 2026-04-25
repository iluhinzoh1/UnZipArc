package com.example.second;

import com.aspose.zip.RarArchive;
import com.aspose.zip.RarArchiveEntry;
import com.aspose.zip.RarArchiveLoadOptions;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.springframework.util.StopWatch;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class ZipUnlock {
    public static final String ERROR_PREFIX = "[ERROR] ";
    private final static AtomicBoolean hasErrors = new AtomicBoolean(false);
    private final static List<String> rightPasswords = new CopyOnWriteArrayList<>();
    private final static List<String> PASSWORDS = List.of("", "Lfdbkmyz124", "Lfdyjcnm124", "Lfrnbkbn124", "Lfhubytw124", "Lfhbntkm124",
            "Lfnxfyby124", "Ldjhbirj124", "Ldjhjdsq124", "Ldjhzyrf124", "Ldekbxbt124", "Ldthjxrf124",
            "Ltdbiybr124", "Ldejrbcm124", "Ltlerwbz124", "Ltpthnbh124", "Ltunzhyz124", "Ltrfltyn124",
            "Ltrflfyc124", "Ltrflybr124", "Ltrfkbnh124", "Ltrjkmnt124", "Ltvjuhfa124", "Ltvtywbz124",
            "Ltvjrhfn124", "Lthpfybt124", "Lthpjcnm124", "Ltcznybr124", "Ltntrnjh124", "Ltnfynth124",
            "Ltwbvtnh124", "Lbfuyjcn124", "Lbfcgjhf124", "Fduecn32025", "Fllbrn32025", "Fljybc32025",
            "Fpfcey32025", "Fpehbn32025", "Fqlfyf32025", "Fqrblj32025", "Fqvfhf32025", "Fqcfhb32025",
            "Fqnfkf32025", "Frfyif32025", "Frptht32025", "Frvjkf32025", "Frcfbn32025", "Frcbjv32025",
            "Fkfpjy32025", "Fkdthf32025", "Fktqrf32025", "Fkktkm32025", "Fvfynf32025", "Fvbfyn32025",
            "Fvvbfr32025", "Fvajhf32025", "Fyfkbp32025", "Fyfybv32025", "Fyujhf32025", "Fyyfns32025",
            "Fynbaf32025", "Fgjabp32025", "Fhlfbn32025", "Fnkfyn32025", "Fhabcn32025", "Fafnbr32025",
            "Fhntkm32025", "Fhvfnf32025", "Fqceke32025", "Fqdjhb32025", "Fqlfyf32025");

    public static void unzipAll(Path sourcePath, Path outputPath) {
        unzipAll(sourcePath, outputPath, System.out::println);
    }

    public static void unzipAll(Path sourcePath, Path outputPath, Consumer<String> log) {
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
                    try (Stream<Path> file = Files.list(sourcePath)) {
                        file
                                .filter(Files::isRegularFile)
                                .filter(ZipUnlock::isSupportedArchive)
                                .forEach(c -> executor.submit(() -> {
                                    try {
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
            trackedLog.accept("Все папки распакованы");
        }

        stopWatch.stop();
        trackedLog.accept("Время: " + stopWatch.getTotalTimeSeconds() + " сек.");
    }

    public static boolean isSupportedArchive(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".zip")
                || name.endsWith(".rar")
                || name.endsWith(".7z");
    }

    private static void unzip(Path paths, Path outputPath, Consumer<String> trackedLog
    ) throws ZipException {
        String name = paths.getFileName().toString().toLowerCase();
        if (name.endsWith(".zip")) {
            unzipZip(paths, outputPath, trackedLog
            );
        } else if (name.endsWith(".rar")) {
            unZipRar(paths, outputPath, trackedLog
            );
        } else if (name.endsWith(".7z")) {
            unzip7z(paths, outputPath, trackedLog
            );
        }
    }

    public static void unzipZip(Path paths, Path outputPath, Consumer<String> trackedLog) {
        for (String password : PASSWORDS) {
            try {
                ZipFile zipFile;

                if (password.isEmpty()) {
                    zipFile = new ZipFile(paths.toFile());
                } else {
                    zipFile = new ZipFile(paths.toFile(), password.toCharArray());
                }

                zipFile.setCharset(Charset.forName("IBM866"));

                if (zipFile.isEncrypted() && password.isEmpty()) {
                    continue;
                }

                for (FileHeader fileHeader : zipFile.getFileHeaders()) {
                    Path fileOutputPath = outputPath.resolve(fileHeader.getFileName());

                    if (fileHeader.isDirectory()) {
                        Files.createDirectories(fileOutputPath);
                    } else {
                        Files.createDirectories(fileOutputPath.getParent());
                        try (InputStream inputStream = zipFile.getInputStream(fileHeader)) {
                            Files.copy(inputStream, fileOutputPath, StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                }

                rightPasswords.add(password);
                trackedLog.accept("ZIP распакован: " + paths.getFileName() + ", пароль: " + password);
                return;

            } catch (Exception e) {
                // пароль не подошел, пробуем следующий
            }
        }

        trackedLog.accept("[ERROR] ZIP не распакован, пароль не найден: " + paths.getFileName());
        hasErrors.set(true);
    }

    private static void unzip7z(Path archive, Path outputPath, Consumer<String> trackedLog
    ) {
        for (String password : PASSWORDS) {
            try {
                SevenZFile.Builder builder = SevenZFile.builder()
                        .setPath(archive);
                if (!password.isEmpty()) {
                    builder.setPassword(password);
                }
                try (SevenZFile sevenZFile = builder.get()) {
                    SevenZArchiveEntry entry;
                    while ((entry = sevenZFile.getNextEntry()) != null) {
                        Path fileOutputPath = outputPath.resolve(entry.getName());
                        if (entry.isDirectory()) {
                            Files.createDirectories(fileOutputPath);
                        } else {
                            Files.createDirectories(fileOutputPath.getParent());
                            try (InputStream inputStream = sevenZFile.getInputStream(entry)) {
                                Files.copy(inputStream, fileOutputPath, StandardCopyOption.REPLACE_EXISTING);
                            }
                        }
                    }
                }

                rightPasswords.add(password);
                trackedLog
                        .accept("7Z распакован: " + archive.getFileName() + ", пароль: " + password);
                return;
            } catch (Exception e) {
                // пароль не подошел, пробуем следующий
            }
        }
        trackedLog
                .accept("[ERROR] 7Z не распакован, пароль не найден: " + archive.getFileName());
        hasErrors.set(true);
    }

    private static void unZipRar(Path paths, Path outputPath, Consumer<String> trackedLog) {
        for (String password : PASSWORDS) {
            try {
                RarArchiveLoadOptions options = new RarArchiveLoadOptions();
                if (!password.isEmpty()) {
                    options.setDecryptionPassword(password);
                }

                try (RarArchive archive = password.isEmpty()
                        ? new RarArchive(paths.toString())
                        : new RarArchive(paths.toString(), options)) {

                    for (RarArchiveEntry entry : archive.getEntries()) {
                        Path fileOutputPath = outputPath.resolve(entry.getName());

                        if (entry.isDirectory()) {
                            Files.createDirectories(fileOutputPath);
                        } else {
                            Files.createDirectories(fileOutputPath.getParent());
                            try (InputStream inputStream = password.isEmpty() ? entry.open() : entry.open(password)) {
                                Files.copy(inputStream, fileOutputPath, StandardCopyOption.REPLACE_EXISTING);
                            }
                        }
                    }
                }

                rightPasswords.add(password);
                trackedLog.accept("RAR распакован: " + paths.getFileName() + ", пароль: " + password);
                return;

            } catch (Exception e) {
                // пароль не подошел, пробуем следующий
            }
        }

        trackedLog.accept("[ERROR] RAR не распакован, пароль не найден: " + paths.getFileName());
        hasErrors.set(true);
    }
}
