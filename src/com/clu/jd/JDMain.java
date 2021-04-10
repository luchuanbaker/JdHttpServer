package com.clu.jd;

import com.clu.jd.http.JdHttpServer;
import com.clu.jd.jadx.MyJadxDecompiler;
import com.clu.jd.procyon.ProcyonDecompiler;
import jd.ide.eclipse.editors.JDSourceMapper;

import java.io.*;
import java.net.URL;

public class JDMain {
    private static Throwable t = null;
    private static String dllFilePath = null;

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
    private static final JDSourceMapper JD_SOURCE_MAPPER = new JDSourceMapper();
    public static final int MAGIC = 0xCAFEBABE;

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please input at least 1 param!");
            return;
        }
        if (args.length == 1) {
            args = new String[]{args[0], args[0]};
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
        return doDecompile(classBasePath, classFileFullName, classInfo, JdHttpServer.ENGINE_ALL);
    }

    private static String doDecompile(String basePath, String classFileFullName, ClassInfo classInfo, int engine) {
        String source = null;
        PrintStream out = System.out;
        try {
            // 重定向，屏蔽中间控制台输出
            System.setOut(new PrintStream(new OutputStream() {
                @Override
                public void write(int b) throws IOException {
                    // ignore
                }
            }));
            if ((engine == JdHttpServer.ENGINE_JD_CORE || (engine == JdHttpServer.ENGINE_ALL && !classInfo.isKotlin))) {
                source = JD_SOURCE_MAPPER.decompile(basePath, classFileFullName);
                if (source != null && source.contains("/* Error */")) {
                    // 反编译包含错误，舍弃
                    source = null;
                }
                if (source != null) {
                    source += "\n// by jd-core";
                }
            }

            if (source == null /*&& engine == JdHttpServer.ENGINE_PROCYON*/) {
                source = ProcyonDecompiler.decompile(basePath, classFileFullName, classInfo);
                if (source != null && source.contains("could not be decompiled")) {
                    // 反编译包含错误，舍弃
                    source = null;
                }
                if (source != null) {
                    source += "\n// by procyon";
                }
            }

            if (source == null) {
                source = MyJadxDecompiler.decompile(basePath, classFileFullName, classInfo);
                if (source != null) {
                    source += "\n// by jadx";
                }
            }

            if (source != null) {
                source += "\n// class version: " + classInfo.jdkVersion;
            }
        } finally {
            System.setOut(out);
        }


        // 修复Kotlin的@Metadata导致Beyond Compare异常的问题
		/*if (classInfo.isKotlin) {
			try {
				StringBuilder newSource = new StringBuilder();
				BufferedReader reader = new BufferedReader(new StringReader(source));
				String line = null;
				boolean processed = false;
				while ((line = reader.readLine()) != null) {
					if (!processed && line.startsWith("@Metadata")) {
						final String startString =  "d1={\"";
						int startIndex = line.indexOf(startString);
						int endIndex = -1;
						if (startIndex != -1) {
							String leftString = line.substring(startIndex);
							endIndex = leftString.indexOf("}");
							if (endIndex != -1) {
								line = line.substring(0, startIndex + startString.length()) + leftString.substring(endIndex - 1);
							}
						}
						processed = true;
					}
					newSource.append(line).append("\r\n");
				}
				source = newSource.toString();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}*/

        return source;
    }

    public static class ClassInfo {
        public String classQualifiedName;
        public int jdkVersion;
        public boolean isKotlin;
        public int minorVersion;

        public ClassInfo(String classQualifiedName, int jdkVersion, int minorVersion, boolean isKotlin) {
            this.classQualifiedName = classQualifiedName;
            this.jdkVersion = jdkVersion;
            this.minorVersion = minorVersion;
            this.isKotlin = isKotlin;
        }
    }

    private static ClassInfo getClassInfoFromClassFile(File classFile) {
        try {
            return getClassInfoFromInputStream(new FileInputStream(classFile));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static ClassInfo getClassInfoFromInputStream(InputStream inputStream) {
        String classQualifiedName = null;
        DataInputStream in = null;
        int minorVersion = 0;
        int majorVersion = 0;
        boolean isKotlin = false;
        try {
            in = new DataInputStream(inputStream);
            if (in.readInt() != MAGIC) {
                throw new IOException("Not class file!");
            }
            minorVersion = in.readUnsignedShort();
            majorVersion = in.readUnsignedShort();

            int count = in.readUnsignedShort();
            Object[] constants = (Object[]) null;
            if (count > 0) {
                constants = new Object[count];
                for (int i = 1; i < count; i++) {
                    byte tag = in.readByte();
                    switch (tag) {
                        case 1: // UTF8 2
                            String s = in.readUTF();
                            constants[i] = new Object[]{tag, s};
                            if (!isKotlin && "Lkotlin/Metadata;".equals(s)) {
                                isKotlin = true;
                            }
                            break;
                        case 3: // Integer 4
                            constants[i] = new Object[]{tag, in.readInt()};
                            break;
                        case 4: // Float 4
                            constants[i] = new Object[]{tag, in.readFloat()};
                            break;
                        case 5: // Long 8
                            constants[(i++)] = new Object[]{tag, in.readLong()};
                            break;
                        case 6: // Double
                            constants[(i++)] = new Object[]{tag, in.readDouble()};
                            break;
                        case 7: // Class 2-byte UTF8 index
                            constants[i] = new Object[]{tag, in.readUnsignedShort()};
                            break;
                        case 8: // String 2-byte UTF8 index
                            constants[i] = new Object[]{tag, in.readUnsignedShort()};
                            break;
                        case 9: // Fieldref 2-byte Class index,
                            // 			2-byte NameAndType index
                            constants[i] = new Object[]{tag, in.readUnsignedShort(), in.readUnsignedShort()};
                            break;
                        case 10:    // Methodref 2-byte Class index,
                            // 			 2-byte NameAndType index
                            constants[i] = new Object[]{tag, in.readUnsignedShort(), in.readUnsignedShort()};
                            break;
                        case 11:    // Interface Methodref 2-byte class index
                            //					   2-byte NameAndType index
                            constants[i] = new Object[]{tag, in.readUnsignedShort(), in.readUnsignedShort()};
                            break;
                        case 12:    // NameAndType	2-byte UTF8 index
                            //				2-byte UTF8 index
                            constants[i] = new Object[]{tag, in.readUnsignedShort(), in.readUnsignedShort()};
                            break;
                        case 15:    // MethodHandler 方法句柄
                            constants[i] = new Object[]{tag, in.readByte(), in.readUnsignedShort()};
                            break;
                        case 16:    // MethodType	方法类型
                            constants[i] = new Object[]{tag, in.readUnsignedShort()};
                            break;
                        case 18:    // InvokeDynamic 动态方法调用点
                            constants[i] = new Object[]{tag, in.readUnsignedShort(), in.readUnsignedShort()};
                            break;
                        case 19:    // Module		53版本支持(jdk9)
                            constants[i] = new Object[]{tag, in.readUnsignedShort()};
                            break;
                        case 20:    // Package		53版本支持(jdk9)
                            constants[i] = new Object[]{tag, in.readUnsignedShort()};
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

			/*int interfaces_count = in.readUnsignedShort();
			int[] interfaces = new int[interfaces_count];
			for (int i = 0; i < interfaces_count; i++) {
				interfaces[i] = in.readUnsignedShort();
			}
			int fields_count = in.readUnsignedShort();
			FieldInfo[] fields = new FieldInfo[fields_count];
			for (int i = 0; i < fields_count; i++) {
				fields[i] = FieldInfo.read(in);
			}

			int methods_count = in.readUnsignedShort();
			MethodInfo[] methods = new MethodInfo[methods_count];
			for (int i = 0; i < methods_count; i++) {
				methods[i] = MethodInfo.read(in);
			}

			int attributes_count = in.readUnsignedShort();
			AttributeInfo[] attributes = new AttributeInfo[attributes_count];
			for (int i = 0; i < attributes_count; i++) {
				attributes[i] = AttributeInfo.read(in);
			}*/

            // find RuntimeVisibleAnnotations
			/*for (int i = 0; i < attributes_count; i++) {
				AttributeInfo info = attributes[i];
				int nameIdex = info.attribute_name_index;
				String name = (String) ((Object[]) constants[nameIdex])[1];
				if ("RuntimeVisibleAnnotations".equals(name)) {
					DataInputStream dataIn = new DataInputStream(new ByteArrayInputStream(info.info));
					AttributeInfo attributeInfo = AttributeInfo.read(dataIn);
					System.out.println(attributeInfo);
				}
			}*/

            int nameIndex = ((Integer) ((Object[]) constants[(this_class)])[1]).intValue();
            classQualifiedName = (String) ((Object[]) constants[(nameIndex)])[1];
        } catch (Exception localException) {
            localException.printStackTrace();
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
            return new ClassInfo(classQualifiedName, majorVersion, minorVersion, isKotlin);
        }

    }
	
	/*static class FieldInfo {
		public int				access_flags;
		public int				name_index;
		public int				descriptor_index;
		public int				attributes_count;
		public AttributeInfo	attributes[];

		public static FieldInfo read(DataInput in) throws IOException {
			FieldInfo info = new FieldInfo();
			info.access_flags = in.readUnsignedShort();
			info.name_index = in.readUnsignedShort();
			info.descriptor_index = in.readUnsignedShort();
			info.attributes_count = in.readUnsignedShort();
			info.attributes = new AttributeInfo[info.attributes_count];
			for (int i = 0; i < info.attributes_count; i++) {
				info.attributes[i] = AttributeInfo.read(in);
			}
			return info;
		}
	}

	static class MethodInfo {
		public int				access_flags;
		public int				name_index;
		public int				descriptor_index;
		public int				attributes_count;
		public AttributeInfo	attributes[];

		public static MethodInfo read(DataInput in) throws IOException {
			MethodInfo info = new MethodInfo();
			info.access_flags = in.readUnsignedShort();
			info.name_index = in.readUnsignedShort();
			info.descriptor_index = in.readUnsignedShort();
			info.attributes_count = in.readUnsignedShort();
			info.attributes = new AttributeInfo[info.attributes_count];
			for (int i = 0; i < info.attributes_count; i++) {
				info.attributes[i] = AttributeInfo.read(in);
			}
			return info;
		}
	}

	static class AttributeInfo {
		public int	attribute_name_index;
		public int	attribute_length;
		public byte	info[];

		public static AttributeInfo read(DataInput in) throws IOException {
			AttributeInfo info = new AttributeInfo();
			info.attribute_name_index = in.readUnsignedShort();
			info.attribute_length = in.readInt();
			info.info = new byte[info.attribute_length];
			for (int i = 0; i < info.attribute_length; i++) {
				info.info[i] = (byte) in.readUnsignedByte();
			}
			return info;
		}
	}

	static class RuntimeVisibleAnnotationsAttribute {
		public int			attribute_name_index;
		public int			attribute_length;
		public int			num_annotations;
		public Annotation	annotations[];
		public static RuntimeVisibleAnnotationsAttribute read(DataInput in) throws IOException {
			RuntimeVisibleAnnotationsAttribute info = new RuntimeVisibleAnnotationsAttribute();
			info.attribute_name_index = in.readUnsignedShort();
			info.attribute_length = in.readInt();
			
			return info;
		}
	}

	static class Annotation {
		public int					type_index;
		public int					num_element_value_pairs;
		public ElementValuePair[]	element_value_pairs;
		public static Annotation read(DataInput in) throws IOException {
			Annotation info = new Annotation();
			info.type_index = in.readUnsignedShort();
			info.num_element_value_pairs = in.readUnsignedShort();
			info.element_value_pairs = new ElementValuePair[info.num_element_value_pairs];
			for (int i = 0; i < info.num_element_value_pairs; i++) {
				info.element_value_pairs[i] = ElementValuePair.read(in);
			}
			return info;
		}
	}

	static class ElementValuePair {
		public int			element_name_index;
		public ElementValue	value;
		public static ElementValuePair read(DataInput in) {
			return null;
		}
	}

	static class ElementValue {
		public int		tag;
		*//**
     * union {
     u2 const_value_index;

     {   u2 type_name_index;
     u2 const_name_index;
     } enum_const_value;

     u2 class_info_index;

     annotation annotation_value;

     {   u2            num_values;
     element_value values[num_values];
     } array_value;
     } value;
     tag Item	Type	value Item	Constant Type
     B	byte	const_value_index	CONSTANT_Integer
     C	char	const_value_index	CONSTANT_Integer
     D	double	const_value_index	CONSTANT_Double
     F	float	const_value_index	CONSTANT_Float
     I	int	const_value_index	CONSTANT_Integer
     J	long	const_value_index	CONSTANT_Long
     S	short	const_value_index	CONSTANT_Integer
     Z	boolean	const_value_index	CONSTANT_Integer
     s	String	const_value_index	CONSTANT_Utf8
     e	Enum type	enum_const_value	Not applicable
     c	Class	class_info_index	Not applicable
     @	Annotation type	annotation_value	Not applicable
     [	Array type	array_value	Not applicable
     */
	/*
	public Object	value;
	}*/

}
