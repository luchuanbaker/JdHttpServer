package com.clu.jd.jadx;

import com.clu.jd.JDMain.ClassInfo;
import com.clu.jd.jadx.my.MyJavaConvertPlugin;
import jadx.api.*;
import jadx.api.plugins.JadxPluginManager;
import jadx.api.plugins.input.data.ILoadResult;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.Utils;
import jadx.plugins.input.javaconvert.JavaConvertPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MyJadxDecompiler {

    private static final Logger logger = LoggerFactory.getLogger(MyJadxDecompiler.class);

    private static final JavaConvertPlugin MY_JAVA_CONVERT_PLUGIN = new MyJavaConvertPlugin();

    private static final Field fieldPluginManager;

    private static final Field fieldAllPlugins;

    static {
        try {
            fieldPluginManager = JadxDecompiler.class.getDeclaredField("pluginManager");
            fieldPluginManager.setAccessible(true);

            fieldAllPlugins = JadxPluginManager.class.getDeclaredField("allPlugins");
            fieldAllPlugins.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static String decompile(String basePath, String classFileFullName, ClassInfo classInfo) {
        File classFile = new File(basePath, classFileFullName);
        File originalFile = classFile;

        List<ILoadResult> loadedInputs = new ArrayList<>();
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

            RootNode rootNode = load(jadxArgs, loadedInputs);
            List<JavaClass> classes = getClasses(rootNode);
            if (classes.size() > 0) {
                success = true;
                return classes.get(0).getCode();
            }
            return null;
        } finally {
            // 关闭资源，自动删除临时文件
            // 原始版本是JadxDecompiler实现了Closeable接口，在close方法中调用了其reset()方法，
            // 内部掉用了其closeInputs()方法，closeInputs方法同下。而JadxDecompiler用了try/auto-close语句
            for (ILoadResult loadedInput : loadedInputs) {
                try {
                    loadedInput.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (classFile != originalFile) {
                if (!classFile.renameTo(originalFile)) {
                    if (!success) {
                        logger.error("移动文件[{}]到[{}]失败", classFile.getAbsolutePath(), originalFile.getAbsolutePath());
                    }
                }
            }
        }

    }

    private static RootNode load(JadxArgs args, List<ILoadResult> loadedInputs) {
        JadxArgsValidator.validate(args);

        RootNode root = new RootNode(args);

        loadInputFiles(args, loadedInputs);
        root.loadClasses(loadedInputs);
        root.initClassPath();

        List<File> inputFiles = args.getInputFiles();
        List<ResourceFile> resourceFiles = new ArrayList<>(inputFiles.size());
        for (File file : inputFiles) {
            String name = file.getAbsolutePath();
            ResourceFile rf;
            rf = newResourceFile(name);
            resourceFiles.add(rf);
        }

        root.loadResources(resourceFiles);
        root.runPreDecompileStage();
        root.initPasses();

        return root;
    }


    private static final Constructor<ResourceFile> RESOURCE_FILE_CONSTRUCTOR;

    static {
        try {
            RESOURCE_FILE_CONSTRUCTOR = ResourceFile.class.getDeclaredConstructor(JadxDecompiler.class, String.class, ResourceType.class);
            RESOURCE_FILE_CONSTRUCTOR.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private static ResourceFile newResourceFile(String name) {
        ResourceFile rf;
        try {
            rf = RESOURCE_FILE_CONSTRUCTOR.newInstance(null, name, ResourceType.CODE);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return rf;
    }

    private static void loadInputFiles(JadxArgs jadxArgs, List<ILoadResult> loadedInputs) {
        List<Path> inputPaths = Utils.collectionMap(jadxArgs.getInputFiles(), File::toPath);
        ILoadResult loadResult = MY_JAVA_CONVERT_PLUGIN.loadFiles(inputPaths);
        if (loadResult != null && !loadResult.isEmpty()) {
            loadedInputs.add(loadResult);
        }
    }

    private static final Method METHOD_RESET;

    static {
        try {
            METHOD_RESET = JadxDecompiler.class.getDeclaredMethod("reset");
            METHOD_RESET.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private static final Constructor<JavaClass> JAVA_CLASS_CONSTRUCTOR;
    static {
        try {
            JAVA_CLASS_CONSTRUCTOR = JavaClass.class.getDeclaredConstructor(ClassNode.class, JadxDecompiler.class);
            JAVA_CLASS_CONSTRUCTOR.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private static List<JavaClass> getClasses(RootNode root) {
        if (root == null) {
            return Collections.emptyList();
        }
        List<ClassNode> classNodeList = root.getClasses(false);
        List<JavaClass> clsList = new ArrayList<>(classNodeList.size());
        List<JavaClass> classes = new ArrayList<>();
        for (ClassNode classNode : classNodeList) {
            if (!classNode.contains(AFlag.DONT_GENERATE)) {
                JavaClass javaClass = null;
                try {
                    javaClass = JAVA_CLASS_CONSTRUCTOR.newInstance(classNode, null);
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
                clsList.add(javaClass);
            }
        }
        return clsList;
    }

//    private static void reset(JadxDecompiler jadx) {
//        try {
//            METHOD_RESET.invoke(jadx);
//        } catch (Exception e) {
//            throw new RuntimeException(e.getMessage(), e);
//        }
//    }

    public static void main(String[] args) {
        System.out.println(decompile("C:\\Users\\clu\\Desktop", "k1$a.class", null));
    }

}
