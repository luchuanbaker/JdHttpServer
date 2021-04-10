package com.clu.jd.jadx;

import com.clu.jd.JDMain.ClassInfo;
import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;

import java.io.File;
import java.util.List;

public class MyJadxDecompiler {

//    static {
//        ((ch.qos.logback.classic.Logger) LoggerFactory.getILoggerFactory().getLogger("jadx")).setAdditive(false);
//    }

    public static String decompile(String basePath, String classFileFullName, ClassInfo classInfo) {
        File classFile = new File(basePath, classFileFullName);
        File originalFile = classFile;
        boolean success = false;
        try {
            String extName = ".class";
            String fileName = classFile.getName();
            if (!fileName.endsWith(extName)) {
                classFile = new File(classFile.getParentFile(), fileName + ".class");
                if (!originalFile.renameTo(classFile)) {
                    return String.format("移动文件[%s]到[%s]失败", originalFile.getAbsolutePath(), classFile.getAbsolutePath());
                }
            }

            JadxArgs jadxArgs = new JadxArgs();
            jadxArgs.setShowInconsistentCode(true);
            jadxArgs.getInputFiles().add(classFile);
            try (JadxDecompiler jadx = new JadxDecompiler(jadxArgs)) {
                jadx.load();
                List<JavaClass> classes = jadx.getClasses();
                if (classes.size() > 0) {
                    success = true;
                    return classes.get(0).getCode();
                }
                return null;
            }
        } finally {
            if (classFile != originalFile) {
                if (!classFile.renameTo(originalFile)) {
                    if (!success) {
                        System.out.println(String.format("移动文件[%s]到[%s]失败", classFile.getAbsolutePath(), originalFile.getAbsolutePath()));
                    }
                }
            }
        }

    }

    public static void main(String[] args) {
        System.out.println(decompile("C:\\Users\\clu\\Desktop", "k1$a.class", null));
    }

}
