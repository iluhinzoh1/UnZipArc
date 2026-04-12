package com.example.second;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;


public class ZipUnlock {
    public static void main(String[] args) throws Exception {
        Path source = Paths.get("C:/Users/MyComputer/Desktop/Packs");
        System.out.println("Начинаю распаковывать...");
        try (Stream<Path> file = Files.list(source)) {
            file
                    .filter(c -> c.getFileName().toString().toLowerCase().endsWith(".zip"))
                    .filter(Files::isRegularFile)
                    .forEach(c -> {
                        try {
                            unzip(c);
                        } catch (ZipException e) {
                            e.printStackTrace();
                            throw new RuntimeException();
                        }
                    });
        }

    }

    public static void unzip(Path paths) throws ZipException {
        ZipFile zipFile = new ZipFile(paths.toFile());
        List<String> passwords = List.of("", "Lfdbkmyz124", "Lfdyjcnm124", "Lfrnbkbn124", "Lfhubytw124", "Lfhbntkm124",
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
        zipFile.setCharset(Charset.forName("IBM866"));
        if (zipFile.isEncrypted()) {
            for (int i = 0; i < passwords.size(); i++) {
                try {
                    zipFile = new ZipFile(paths.toFile(), passwords.get(i).toCharArray());
                    System.out.println(passwords.get(i));
                    zipFile.setCharset(Charset.forName("IBM866"));
                    zipFile.extractAll("C:/Users/MyComputer/Desktop/UnPacks");
                    System.out.printf("пароль подобран: %s ", passwords.get(i));
                    break;
                } catch (ZipException e) {
                    System.out.printf("не получилось обработать данный пароль %s ", passwords.get(i));
                }
            }
        }
        zipFile.extractAll("C:/Users/MyComputer/Desktop/UnPacks");
        System.out.println("папка создана, файлы перенесены и распакованы");
    }
}
