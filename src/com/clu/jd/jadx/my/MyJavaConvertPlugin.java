package com.clu.jd.jadx.my;

import jadx.api.plugins.input.data.ILoadResult;
import jadx.api.plugins.input.data.impl.EmptyLoadResult;
import jadx.plugins.input.dex.DexInputPlugin;
import jadx.plugins.input.javaconvert.ConvertResult;
import jadx.plugins.input.javaconvert.JavaConvertPlugin;

import java.nio.file.Path;
import java.util.List;

public class MyJavaConvertPlugin extends JavaConvertPlugin {

    @Override
    public ILoadResult loadFiles(List<Path> input) {
        ConvertResult result = MyJavaConvertLoader.process(input);
        if (result.isEmpty()) {
            result.close();
            return EmptyLoadResult.INSTANCE;
        }
        return DexInputPlugin.loadDexFiles(result.getConverted(), result);
    }

}
