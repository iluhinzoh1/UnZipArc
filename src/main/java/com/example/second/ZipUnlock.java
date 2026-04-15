package com.example.second;

import com.github.junrar.Junrar;
import com.github.junrar.exception.RarException;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.springframework.util.StopWatch;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class ZipUnlock {
    private final static List<String> rightPasswords = new CopyOnWriteArrayList<>();
    private final static Path OUTPUT = Paths.get("C:/Users/MyComputer/Desktop/UnPacks");
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

    public static void main(String[] args) throws Exception {
        Path source = Paths.get("C:/Users/MyComputer/Desktop/Packs");
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        System.out.println("Начинаю распаковывать...");

        try (ExecutorService executor = Executors.newFixedThreadPool(4)) {

            try (Stream<Path> file = Files.list(source)) {
                file
                        .filter(Files::isRegularFile)
                        .forEach(c -> {
                            executor.submit(() -> {
                                try {
                                    unzip(c);
                                } catch (Exception e) {
                                    System.out.println("Ошибка при распаковке архива: " + c.getFileName());
                                    e.printStackTrace();
                                }
                            });
                        });
            }
            executor.shutdown();
            boolean finished = executor.awaitTermination(1, TimeUnit.HOURS);
            if (!finished) {
                System.out.println("Не все архивы успели обработаться");
                executor.shutdownNow();
            }
        }
        System.out.printf("правильные пароли: %s\n", rightPasswords);
        System.out.println("Все архивы обработаны");
        stopWatch.stop();
        System.out.println(stopWatch.getTotalTimeSeconds());
    }

    private static void unzip(Path paths) throws ZipException {
        String name = paths.getFileName().toString().toLowerCase();
        if (name.endsWith(".zip")) {
            unzipZip(paths);
        } else if (name.endsWith(".rar")) {
            unZipRar(paths);
        } else if (name.endsWith(".7z")) {
            unzip7z(paths);
        } else {
            System.out.printf("данный формат не поддерживается: %s", name);
        }
    }

    public static void unzipZip(Path paths) throws ZipException {
        ZipFile zipFile = new ZipFile(paths.toFile());
        zipFile.setCharset(Charset.forName("IBM866"));
        if (zipFile.isEncrypted()) {
            for (int i = 0; i < PASSWORDS.size(); i++) {
                try {
                    zipFile = new ZipFile(paths.toFile(), PASSWORDS.get(i).toCharArray());
                    zipFile.setCharset(Charset.forName("IBM866"));
                    zipFile.extractAll(String.valueOf(OUTPUT));
                    rightPasswords.add(PASSWORDS.get(i));
                    System.out.println("zip распакован: " + paths.getFileName() + ", пароль: " + PASSWORDS.get(i));
                    return;
                } catch (ZipException e) {
                    // System.out.printf("не получилось обработать данный пароль %s ", PASSWORDS.get(i));
                }
            }
        }
        zipFile.extractAll(OUTPUT.toString());
        System.out.println("папка создана, файлы перенесены и распакованы");
    }

    private static void unzip7z(Path archive) {
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
                        Path outputPath = OUTPUT.resolve(entry.getName());
                        if (entry.isDirectory()) {
                            Files.createDirectories(outputPath);
                        } else {
                            Files.createDirectories(outputPath.getParent());
                            try (InputStream inputStream = sevenZFile.getInputStream(entry)) {
                                Files.copy(inputStream, outputPath, StandardCopyOption.REPLACE_EXISTING);
                            }
                        }
                    }
                }
                rightPasswords.add(password);
                System.out.println("7Z распакован: " + archive.getFileName() + ", пароль: " + password);
                return;
            } catch (Exception e) {
                // System.out.println("7Z пароль не подошел: " + password + " (" + e.getClass().getSimpleName() + ": " + e.getMessage() + ")");
            }
        }
        System.out.println("7Z не распакован: " + archive.getFileName());
    }
    private static void unZipRar(Path paths) {
        for (String password : PASSWORDS) {
            try {
                Files.createDirectories(OUTPUT);
                if (password.isEmpty()) {
                    Junrar.extract(paths.toString(), OUTPUT.toString());
                } else {
                    Junrar.extract(paths.toString(), OUTPUT.toString(), password);
                }

                rightPasswords.add(password);
                System.out.println("RAR распакован: " + paths.getFileName() + ", пароль: " + password);
                return;
            } catch (IOException | RarException e) {
                // System.out.println("RAR пароль не подошел: " + password + " (" + e.getClass().getSimpleName() + ": " + e.getMessage() + ")");
            }
        }
        System.out.println("RAR не распакован: " + paths.getFileName());
    }
}
