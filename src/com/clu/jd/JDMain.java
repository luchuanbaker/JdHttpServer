package com.clu.jd;

import java.io.*;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import jd.ide.eclipse.editors.JDSourceMapper;

public class JDMain {
	private static Throwable			t						= null;
	private static String				dllFilePath				= null;

	static {
		try {
			String tmpPath = System.getenv("tmp").split(";")[0];
			String libName = null;
			String vmName = System.getProperty("java.vm.name");
			if (vmName != null && vmName.contains("64-")) {
				// Java HotSpot(TM) 64-Bit Server VM
				libName = "jd-x64.dll";
			} else {
				libName = "jd-x32.dll";
			}
			
			File destFile = new File(tmpPath, libName);
			if (!destFile.exists()) {
				URL url = JDMain.class.getResource("/lib/" + libName);
				InputStream inputStream = url.openStream();
				FileOutputStream outputStream = new FileOutputStream(destFile);
				byte[] buff = new byte[1024];
				int len = -1;
				while ((len = inputStream.read(buff)) != -1) {
					outputStream.write(buff, 0, len);
				}
				inputStream.close();
				outputStream.close();
			}
			dllFilePath = destFile.getAbsolutePath();
			System.load(dllFilePath);
		} catch (Throwable e) {
			t = e;
		}
	}

	private static final List<String>	SUPPORTED_FILE_SUFFIXES	= Arrays.asList(new String[]{ ".zip", ".jar" });
	private static final JDSourceMapper	JD_SOURCE_MAPPER		= new JDSourceMapper();
	private static final int			MAGIC					= 0xCAFEBABE;

	public static void main(String[] args) {
		if (args.length == 0) {
			System.out.println("Please input at least 1 param!");
			return;
		}
		if (args.length == 1) {
			args = new String[]{ args[0], args[0] };
		}
		if (t != null) {
			System.out.println(getException(t));
		} else {
			try {
				String content = decompile(args[0], args[1]);

				System.out.println(content);
			} catch (Exception e) {
				System.out.println(getException(e));
			}
		}
	}

	private static String getException(Throwable t) {
		StringWriter writer = new StringWriter();
		t.printStackTrace(new PrintWriter(writer));
		return writer.toString();
	}

	public static String decompile(String inputFile, String fullPath) throws Exception {
		fullPath = fullPath.replace("\\", "/");
		String basePath = null;

		String className = getClassNameFromClassFile(new File(inputFile));
		String classFileFullName = null;
		if (className != null) {
			classFileFullName = className + ".class";
			if (fullPath.endsWith(classFileFullName)) {
				basePath = fullPath.substring(0, fullPath.lastIndexOf("/" + classFileFullName));
			}
		}
		if (basePath == null) {
			boolean isFromJar = false;
			String fileSuffix = null;
			String fullPathLower = fullPath.toLowerCase();
			for (String suffix : SUPPORTED_FILE_SUFFIXES) {
				if (fullPathLower.contains(suffix)) {
					isFromJar = true;
					fileSuffix = suffix;
					break;
				}
			}
			if (isFromJar) {
				int index = fullPathLower.lastIndexOf(fileSuffix);
				basePath = fullPath.substring(0, index + fileSuffix.length());
				classFileFullName = fullPath.substring(index + fileSuffix.length() + 1);
			} else {
				File classFile = new File(fullPath);
				if (classFile.exists()) {
					File baseFile = classFile;
					String[] segments = fullPath.split("/");
					int count = 0;
					while (baseFile.getParentFile() != null) {
						baseFile = baseFile.getParentFile();
						count++;
						StringBuilder classFullNameBuilder = new StringBuilder();
						for (int i = segments.length - count; i < segments.length; i++) {
							classFullNameBuilder.append(segments[i]).append("/");
						}
						classFullNameBuilder.deleteCharAt(classFullNameBuilder.length() - 1);

						String content = JD_SOURCE_MAPPER.decompile(baseFile.getAbsolutePath(), classFullNameBuilder.toString());
						if (content != null) {
							return content;
						}
					}
				} else {
					return "File Not Found：" + fullPath;
				}
			}
		}
		if (basePath != null) {
			String content = null;
			if (new File(basePath).exists()) {
				content = JD_SOURCE_MAPPER.decompile(basePath, classFileFullName);
			} else {
				File tmpClassFile = new File(inputFile);
				content = JD_SOURCE_MAPPER.decompile(tmpClassFile.getParent(), tmpClassFile.getName());
			}
			return content;
		}
		return null;
	}

	@SuppressWarnings("unused")
	private static String getClassNameFromClassFile(File classFile) {
		String className = null;
		DataInputStream in = null;
		try {
			in = new DataInputStream(new FileInputStream(classFile));
			if (in.readInt() != MAGIC) {
				throw new IOException("Not class file!");
			}
			int minor_version = in.readUnsignedShort();

			int major_version = in.readUnsignedShort();

			int count = in.readUnsignedShort();
			Object[] constants = (Object[]) null;
			if (count > 0) {
				constants = new Object[count - 1];
				for (int i = 0; i < count - 1; i++) {
					byte tag = in.readByte();
					switch (tag) {
						case 7:
							constants[i] = new Object[]{ Byte.valueOf(tag), Integer.valueOf(in.readUnsignedShort()) };
							break;
						case 9:
							constants[i] = new Object[]{ Byte.valueOf(tag), Integer.valueOf(in.readUnsignedShort()), Integer.valueOf(in.readUnsignedShort()) };
							break;
						case 10:
							constants[i] = new Object[]{ Byte.valueOf(tag), Integer.valueOf(in.readUnsignedShort()), Integer.valueOf(in.readUnsignedShort()) };
							break;
						case 11:
							constants[i] = new Object[]{ Byte.valueOf(tag), Integer.valueOf(in.readUnsignedShort()), Integer.valueOf(in.readUnsignedShort()) };
							break;
						case 8:
							constants[i] = new Object[]{ Byte.valueOf(tag), Integer.valueOf(in.readUnsignedShort()) };
							break;
						case 3:
							constants[i] = new Object[]{ Byte.valueOf(tag), Integer.valueOf(in.readInt()) };
							break;
						case 4:
							constants[i] = new Object[]{ Byte.valueOf(tag), Float.valueOf(in.readFloat()) };
							break;
						case 5:
							constants[(i++)] = new Object[]{ Byte.valueOf(tag), Long.valueOf(in.readLong()) };
							break;
						case 6:
							constants[(i++)] = new Object[]{ Byte.valueOf(tag), Double.valueOf(in.readDouble()) };
							break;
						case 12:
							constants[i] = new Object[]{ Byte.valueOf(tag), Integer.valueOf(in.readUnsignedShort()), Integer.valueOf(in.readUnsignedShort()) };
							break;
						case 1:
							constants[i] = new Object[]{ Byte.valueOf(tag), in.readUTF() };
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
			className = (String) ((Object[]) constants[(nameIndex - 1)])[1];
		} catch (Exception localException) {
			if (in != null) {
				try {
					in.close();
				} catch (Exception localException1) {
				}
			}
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (Exception localException2) {
				}
			}
		}
		return className;
	}
}
