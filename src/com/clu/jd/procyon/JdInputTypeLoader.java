package com.clu.jd.procyon;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.clu.jd.JDMain;
import com.clu.jd.JDMain.ClassInfo;
import com.strobel.assembler.InputTypeLoader;
import com.strobel.assembler.metadata.Buffer;
import com.strobel.assembler.metadata.ClasspathTypeLoader;
import com.strobel.assembler.metadata.ITypeLoader;
import com.strobel.core.StringComparison;
import com.strobel.core.StringUtilities;
import com.strobel.core.VerifyArgument;
import com.strobel.io.PathHelper;

/**
 * 替换自带的TypeLoader
 * @author clu
 * @version 1.0
 * @date 2019-2-27下午1:48:34
 * @since 1.0.0
 */
public class JdInputTypeLoader implements ITypeLoader {
	private static final Logger						LOG	= Logger.getLogger(InputTypeLoader.class.getSimpleName());
	private final ITypeLoader						_defaultTypeLoader;
	private final Map<String, LinkedHashSet<File>>	_packageLocations;
	private final Map<String, File>					_knownFiles;

	public JdInputTypeLoader() {
		this((ITypeLoader) new ClasspathTypeLoader());
	}

	public JdInputTypeLoader(final ITypeLoader defaultTypeLoader) {
		this._defaultTypeLoader = (ITypeLoader) VerifyArgument.notNull((Object) defaultTypeLoader, "defaultTypeLoader");
		this._packageLocations = new LinkedHashMap<String, LinkedHashSet<File>>();
		this._knownFiles = new LinkedHashMap<String, File>();
	}

	public boolean tryLoadType(final String typeNameOrPath, final Buffer buffer) {
		VerifyArgument.notNull((Object) typeNameOrPath, "typeNameOrPath");
		VerifyArgument.notNull((Object) buffer, "buffer");

		if (LOG.isLoggable(Level.FINE)) {
			LOG.fine("Attempting to load type: " + typeNameOrPath + "...");
		}
		// edit by clu on 2019-2-27 14:04:10 修复只能对.class文件进行反编译的问题
		int index = typeNameOrPath.lastIndexOf(".");
		final boolean hasExtension = index != -1; // StringUtilities.endsWithIgnoreCase((CharSequence) typeNameOrPath, ".class");

		if (hasExtension && this.tryLoadFile(null, typeNameOrPath, buffer, true)) {
			return true;
		}

		if (PathHelper.isPathRooted(typeNameOrPath)) {
			if (LOG.isLoggable(Level.FINER)) {
				LOG.finer("Failed to load type: " + typeNameOrPath + ".");
			}
			return false;
		}

		String internalName = hasExtension ? typeNameOrPath.substring(0, typeNameOrPath.length() - 6) : typeNameOrPath.replace('.', '/');

		if (this.tryLoadTypeFromName(internalName, buffer)) {
			return true;

		}
		if (hasExtension) {
			if (LOG.isLoggable(Level.FINER)) {
				LOG.finer("Failed to load type: " + typeNameOrPath + ".");
			}
			return false;
		}

		for (int lastDelimiter = internalName.lastIndexOf(47); lastDelimiter != -1; lastDelimiter = internalName.lastIndexOf(47)) {

			internalName = internalName.substring(0, lastDelimiter) + "$" + internalName.substring(lastDelimiter + 1);

			if (this.tryLoadTypeFromName(internalName, buffer)) {
				return true;

			}
		}
		if (LOG.isLoggable(Level.FINER)) {
			LOG.finer("Failed to load type: " + typeNameOrPath + ".");

		}
		return false;
	}

	private boolean tryLoadTypeFromName(final String internalName, final Buffer buffer) {
		if (this.tryLoadFromKnownLocation(internalName, buffer)) {
			return true;

		}
		if (this._defaultTypeLoader.tryLoadType(internalName, buffer)) {
			return true;

		}
		final String filePath = internalName.replace('/', File.separatorChar) + ".class";

		if (this.tryLoadFile(internalName, filePath, buffer, false)) {
			return true;

		}
		final int lastSeparatorIndex = filePath.lastIndexOf(File.separatorChar);

		return lastSeparatorIndex >= 0 && this.tryLoadFile(internalName, filePath.substring(lastSeparatorIndex + 1), buffer, true);

	}

	private boolean tryLoadFromKnownLocation(final String internalName, final Buffer buffer) {
		final File knownFile = this._knownFiles.get(internalName);

		if (knownFile != null && this.tryLoadFile(knownFile, buffer)) {
			if (LOG.isLoggable(Level.FINE)) {
				LOG.fine("Type loaded from " + knownFile.getAbsolutePath() + ".");
			}
			return true;

		}
		final int packageEnd = internalName.lastIndexOf(47);

		String head;

		String tail;
		if (packageEnd < 0 || packageEnd >= internalName.length()) {
			head = "";
			tail = internalName;
		} else {
			head = internalName.substring(0, packageEnd);
			tail = internalName.substring(packageEnd + 1);

		}
		while (true) {
			final LinkedHashSet<File> directories = this._packageLocations.get(head);

			if (directories != null) {
				for (final File directory : directories) {
					if (this.tryLoadFile(internalName, new File(directory, tail + ".class").getAbsolutePath(), buffer, true)) {
						return true;

					}
				}
			}
			final int split = head.lastIndexOf(47);

			if (split <= 0) {

				return false;
			}
			tail = head.substring(split + 1) + '/' + tail;
			head = head.substring(0, split);
		}
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
		final String actualName = getInternalNameFromClassFile(buffer);

		final String name = trustName ? ((internalName != null) ? internalName : actualName) : actualName;

		if (name == null) {
			return false;
		}
		final boolean nameMatches = StringUtilities.equals(actualName, internalName);
		final boolean pathMatchesName = typeNameOrPath.endsWith(name.replace('/', File.separatorChar) + ".class");

		final boolean result = internalName == null || pathMatchesName || nameMatches;

		if (result) {
			final int packageEnd = name.lastIndexOf(47);

			String packageName;
			if (packageEnd < 0 || packageEnd >= name.length()) {
				packageName = "";
			} else {
				packageName = name.substring(0, packageEnd);

			}
			this.registerKnownPath(packageName, file.getParentFile(), pathMatchesName);

			this._knownFiles.put(actualName, file);

			if (LOG.isLoggable(Level.FINE)) {
				LOG.fine("Type loaded from " + file.getAbsolutePath() + ".");
			}
		} else {
			buffer.reset(0);
		}
		return result;
	}

	private void registerKnownPath(final String packageName, final File directory, final boolean recursive) {
		if (directory == null || !directory.exists()) {
			return;

		}
		LinkedHashSet<File> directories = this._packageLocations.get(packageName);

		if (directories == null) {
			this._packageLocations.put(packageName, directories = new LinkedHashSet<File>());

		}
		if (!directories.add(directory) || !recursive) {
			return;

		}
		try {
			final String directoryPath = StringUtilities.removeRight(directory.getCanonicalPath(),
					new char[]{ PathHelper.DirectorySeparator, PathHelper.AlternateDirectorySeparator }).replace('\\', '/');

			String currentPackage = packageName;
			File currentDirectory = new File(directoryPath);

			int delimiterIndex;
			while ((delimiterIndex = currentPackage.lastIndexOf(47)) >= 0 && currentDirectory.exists() && delimiterIndex < currentPackage.length() - 1) {

				final String segmentName = currentPackage.substring(delimiterIndex + 1);

				if (!StringUtilities.equals(currentDirectory.getName(), segmentName, StringComparison.OrdinalIgnoreCase)) {
					break;

				}
				currentPackage = currentPackage.substring(0, delimiterIndex);
				currentDirectory = currentDirectory.getParentFile();

				directories = this._packageLocations.get(currentPackage);

				if (directories == null) {
					this._packageLocations.put(currentPackage, directories = new LinkedHashSet<File>());

				}
				if (!directories.add(currentDirectory)) {
					break;

				}
			}
		} catch (IOException ex) {
		}
	}

	private static String getInternalNameFromClassFile(final Buffer b) {
		ClassInfo classInfo = getClassInfoFromClassFile(b);
		b.position(0);
		if (classInfo == null) {
			return null;
		} else {
			return classInfo.classQualifiedName;
		}
		
		/*final long magic = b.readInt() & 0xFFFFFFFFL;

		if (magic != 0xCAFEBABEL) {
			return null;

		}
		b.readUnsignedShort();
		b.readUnsignedShort();

		final ConstantPool constantPool = ConstantPool.read(b);

		b.readUnsignedShort();

		final ConstantPool.TypeInfoEntry thisClass = (ConstantPool.TypeInfoEntry) constantPool.getEntry(b.readUnsignedShort());
		b.position(0);
		return thisClass.getName();*/
	}
	
	@SuppressWarnings("unused")
	private static ClassInfo getClassInfoFromClassFile(final Buffer b) {
		String classQualifiedName = null;
		Buffer in = null;
		int minorVersion = 0;
		int majorVersion = 0;
		try {
			in = b;
			if (in.readInt() != JDMain.MAGIC) {
				throw new IOException("Not class file!");
			}
			minorVersion = in.readUnsignedShort();
			majorVersion = in.readUnsignedShort();

			int count = in.readUnsignedShort();
			Object[] constants = (Object[]) null;
			if (count > 0) {
				constants = new Object[count - 1];
				for (int i = 0; i < count - 1; i++) {
					byte tag = in.readByte();
					switch (tag) {
						case 7:
							constants[i] = new Object[]{ tag, in.readUnsignedShort() };
							break;
						case 9:
							constants[i] = new Object[]{ tag, in.readUnsignedShort(), in.readUnsignedShort() };
							break;
						case 10:
							constants[i] = new Object[]{ tag, in.readUnsignedShort(), in.readUnsignedShort() };
							break;
						case 11:
							constants[i] = new Object[]{ tag, in.readUnsignedShort(), in.readUnsignedShort() };
							break;
						case 8:
							constants[i] = new Object[]{ tag, in.readUnsignedShort() };
							break;
						case 3:
							constants[i] = new Object[]{ tag, in.readInt() };
							break;
						case 4:
							constants[i] = new Object[]{ tag, in.readFloat() };
							break;
						case 5:
							constants[(i++)] = new Object[]{ tag, in.readLong() };
							break;
						case 6:
							constants[(i++)] = new Object[]{ tag, in.readDouble() };
							break;
						case 12:
							constants[i] = new Object[]{ tag, in.readUnsignedShort(), in.readUnsignedShort() };
							break;
						case 1:
							constants[i] = new Object[]{ tag, in.readUtf8() };
							break;
						case 15:
							constants[i] = new Object[]{ tag, in.readByte(), in.readUnsignedShort() };
							break;
						case 16:
							constants[i] = new Object[]{ tag, in.readUnsignedShort() };
							break;
						case 18:
							constants[i] = new Object[]{ tag, in.readUnsignedShort(), in.readUnsignedShort() };
							break;
						case 19:
							constants[i] = new Object[]{ tag, in.readUnsignedShort() };
							break;
						case 20:
							constants[i] = new Object[]{ tag, in.readUnsignedShort() };
							break;
						case 2:
						default:
							throw new RuntimeException("Invalid constant pool entry");
					}
				}
			}
			int access_flags = in.readUnsignedShort();
			int this_class = in.readUnsignedShort();

			int super_class = in.readUnsignedShort();

			int nameIndex = ((Integer) ((Object[]) constants[(this_class - 1)])[1]).intValue();
			classQualifiedName = (String) ((Object[]) constants[(nameIndex - 1)])[1];
		} catch (Exception exception) {
		} finally {
		}
		if (classQualifiedName == null) {
			return null;
		} else {
			return new ClassInfo(classQualifiedName, majorVersion);
		}
	}
	
}
