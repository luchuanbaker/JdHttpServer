package com.clu.jd;

import java.io.*;
import java.net.URL;

import jd.ide.eclipse.editors.JDSourceMapper;

import com.clu.jd.http.JdHttpServer;
import com.clu.jd.procyon.ProcyonDecompiler;

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

	// private static final List<String>	SUPPORTED_FILE_SUFFIXES	= Arrays.asList(new String[]{ ".zip", ".jar" });
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
				String content = decompile(args[0]);

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

	/**
	 * 反编译
	 * @param inputFile 要反编译的class文件 C:\Temp\AA.tmp
	 * @return
	 */
	public static String decompile(String inputFile) {
		String fullPath = inputFile.replace("\\", "/");
		String classBasePath = null;

		File classFile = new File(inputFile);
		ClassInfo classInfo = getClassInfoFromClassFile(classFile);
		if (classInfo == null) {
			return null;
		}
		String classFileFullName = null;
		classFileFullName = classInfo.classQualifiedName + ".class";
		if (fullPath.endsWith(classFileFullName)) {
			// 目录结构和类名一致，一般是解压好的jar或者是编译的输出目录
			classBasePath = fullPath.substring(0, fullPath.lastIndexOf("/" + classFileFullName));
		} else {
			// 不一致，直接以class文件所在目录为根目录
			classBasePath = classFile.getParentFile().getAbsolutePath();
			classFileFullName = classFile.getName();
		}
		return doDecompile(classBasePath, classFileFullName, classInfo, JdHttpServer.ENGINE);
	}

	private static String doDecompile(String basePath, String classFileFullName, ClassInfo classInfo, int engine) {
		String source = null;
		if (engine == JdHttpServer.ENGINE_JD_CORE || engine == JdHttpServer.ENGINE_ALL) {
			source = JD_SOURCE_MAPPER.decompile(basePath, classFileFullName);
		}

		if (engine == JdHttpServer.ENGINE_PROCYON || source == null && engine == JdHttpServer.ENGINE_ALL) {
			source = ProcyonDecompiler.decompile(basePath, classFileFullName, classInfo);
		}
		return source;
	}

	public static class ClassInfo {
		public String	classQualifiedName;
		public int		jdkVersion;

		public ClassInfo(String classQualifiedName, int jdkVersion) {
			this.classQualifiedName = classQualifiedName;
			this.jdkVersion = jdkVersion;
		}
	}

	@SuppressWarnings("unused")
	private static ClassInfo getClassInfoFromClassFile(File classFile) {
		String classQualifiedName = null;
		DataInputStream in = null;
		int minorVersion = 0;
		int majorVersion = 0;
		try {
			in = new DataInputStream(new FileInputStream(classFile));
			if (in.readInt() != MAGIC) {
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
							constants[i] = new Object[]{ tag, in.readUTF() };
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
		if (classQualifiedName == null) {
			return null;
		} else {
			return new ClassInfo(classQualifiedName, majorVersion);
		}
	}
}
