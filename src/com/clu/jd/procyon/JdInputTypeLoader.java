package com.clu.jd.procyon;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.strobel.assembler.InputTypeLoader;
import com.strobel.assembler.metadata.Buffer;
import com.strobel.assembler.metadata.ITypeLoader;
import com.strobel.core.VerifyArgument;

/**
 * 替换自带的TypeLoader
 * @author clu
 * @version 1.0
 * @date 2019-2-27下午1:48:34
 * @since 1.0.0
 */
public class JdInputTypeLoader implements ITypeLoader {
	
	private static final Logger						LOG	= Logger.getLogger(InputTypeLoader.class.getSimpleName());

	public boolean tryLoadType(final String typeNameOrPath, final Buffer buffer) {
		VerifyArgument.notNull((Object) typeNameOrPath, "typeNameOrPath");
		VerifyArgument.notNull((Object) buffer, "buffer");

		if (LOG.isLoggable(Level.FINE)) {
			LOG.fine("Attempting to load type: " + typeNameOrPath + "...");
		}
		final boolean hasExtension = typeNameOrPath.lastIndexOf(".") != -1;

		if (hasExtension && this.tryLoadFile(null, typeNameOrPath, buffer, true)) {
			return true;
		}
		return false;
	}

	private boolean tryLoadFile(final File file, final Buffer buffer) {
		if (LOG.isLoggable(Level.FINER)) {
			LOG.finer("Probing for file: " + file.getAbsolutePath() + "...");
		}
		
		if (!file.exists() || file.isDirectory()) {
			return false;
		}
		
		try (final FileInputStream in = new FileInputStream(file)) {
			int remainingBytes = in.available();

			buffer.position(0);
			buffer.reset(remainingBytes);

			while (remainingBytes > 0) {
				final int bytesRead = in.read(buffer.array(), buffer.position(), remainingBytes);

				if (bytesRead < 0) {
					break;

				}
				remainingBytes -= bytesRead;
				buffer.advance(bytesRead);

			}
			buffer.position(0);

			return true;
		} catch (IOException e) {
			return false;
		}
	}

	private boolean tryLoadFile(final String internalName, final String typeNameOrPath, final Buffer buffer, final boolean trustName) {
		final File file = new File(typeNameOrPath);

		if (!this.tryLoadFile(file, buffer)) {
			return false;
		}
		final String actualName = ProcyonDecompiler.CLASS_INFO.get().classQualifiedName;
		final String name = trustName ? ((internalName != null) ? internalName : actualName) : actualName;
		return name != null;
	}

}
