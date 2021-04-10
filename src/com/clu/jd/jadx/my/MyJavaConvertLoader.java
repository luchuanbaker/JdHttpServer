package com.clu.jd.jadx.my;

import jadx.api.plugins.utils.ZipSecurity;
import jadx.plugins.input.javaconvert.ConvertResult;
import jadx.plugins.input.javaconvert.DxConverter;
import jadx.plugins.input.javaconvert.JavaConvertLoader;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MyJavaConvertLoader extends JavaConvertLoader {

    public static ConvertResult process(List<Path> input) {
        ConvertResult result = new ConvertResult();
        try {
            processClassFiles(input, result);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return result;
    }

    private static void processClassFiles(List<Path> input, ConvertResult result) throws IOException {
        Path jarFile = Files.createTempFile("jadx-", ".jar");
        List<Path> clsFiles = input.stream()
            .filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
            .collect(Collectors.toList());
        // class文件打包成jar
        try (JarOutputStream jo = new JarOutputStream(Files.newOutputStream(jarFile))) {
            for (Path file : clsFiles) {
                String clsName = getNameFromClassFile(file);
                if (clsName == null || !ZipSecurity.isValidZipEntryName(clsName)) {
                    throw new IOException("Can't read class name from file: " + file);
                }
                addFileToJar(jo, file, clsName + ".class");
            }
        }
        result.addTempPath(jarFile);
        // jar文件转dex
        convertJar(result, jarFile);

        // 删除中间产生的临时文件
//        if (!jarFile.toFile().delete()) {
//            System.out.println("删除临时文件失败：" + jarFile.toAbsolutePath());
//        }
    }

    //
    private static void convertJar(ConvertResult result, Path path) throws IOException {
        Path tempDirectory = Files.createTempDirectory("jadx-");
        result.addTempPath(tempDirectory);

        DxConverter.run(path, tempDirectory);

        result.addConvertedFiles(collectFilesInDir(tempDirectory));
    }

    private static List<Path> collectFilesInDir(Path tempDirectory) throws IOException {
        PathMatcher dexMatcher = FileSystems.getDefault().getPathMatcher("glob:**.dex");
        try (Stream<Path> pathStream = Files.walk(tempDirectory, 1)) {
            return pathStream
                .filter(p -> Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS))
                .filter(dexMatcher::matches)
                .collect(Collectors.toList());
        }
    }

}
