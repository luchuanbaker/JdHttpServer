package com.clu.jd.procyon;

import java.io.File;
import java.io.StringWriter;

import com.clu.jd.JDMain.ClassInfo;
import com.clu.jd.http.Logger;
import com.strobel.assembler.metadata.DeobfuscationUtilities;
import com.strobel.assembler.metadata.MetadataSystem;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.decompiler.DecompilationOptions;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.PlainTextOutput;
import com.strobel.decompiler.languages.Languages;

public class ProcyonDecompiler {

	private static MetadataSystem metadataSystem;
	private static DecompilationOptions decompilationOptions;
	private static DecompilerSettings settings;
	
	
	static {
		decompilationOptions = new DecompilationOptions();

		settings = DecompilerSettings.javaDefaults();
		settings.setTypeLoader(new JdInputTypeLoader());
		settings.setForceExplicitImports(true);

		decompilationOptions.setSettings(settings);
		decompilationOptions.setFullDecompilation(true);

		metadataSystem = new MetadataSystem(settings.getTypeLoader());
		metadataSystem.setEagerMethodLoadingEnabled(false);
	}
	
	public static String decompile(String basePath, String classFileFullName, ClassInfo classInfo) {
		try {
			String classPathStr = new File(basePath, classFileFullName).getAbsolutePath();

			TypeReference type = metadataSystem.lookupType(classPathStr);
			TypeDefinition resolvedType = type.resolve();
			DeobfuscationUtilities.processType(resolvedType);

			StringWriter writer = new StringWriter();
			PlainTextOutput output = new PlainTextOutput(writer);

			/*TypeDecompilationResults results = */
			Languages.java().decompileType(resolvedType, output, decompilationOptions);

			/*List<LineNumberPosition> lineNumberPositions = results.getLineNumberPositions();*/

			return writer.toString();
		} catch (Exception e) {
			Logger.error(e.getMessage(), e);
			return null;
		}
	}

}
