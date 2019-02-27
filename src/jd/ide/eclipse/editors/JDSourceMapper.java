package jd.ide.eclipse.editors;

public class JDSourceMapper {
	private static final String	JAVA_CLASS_SUFFIX			= ".class";
	private static final String	JAVA_SOURCE_SUFFIX			= ".java";
	private static final int	JAVA_SOURCE_SUFFIX_LENGTH	= 5;
	private static boolean		loaded						= true;

	public native String decompile(String paramString1, String paramString2);

	public native String getVersion();
}