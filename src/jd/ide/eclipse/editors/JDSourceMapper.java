/*  1:   */ package jd.ide.eclipse.editors;
/*  2:   */ 
/*  3:   */ public class JDSourceMapper
/*  4:   */ {
/*  5:   */   private static final String JAVA_CLASS_SUFFIX = ".class";
/*  6:   */   private static final String JAVA_SOURCE_SUFFIX = ".java";
/*  7:   */   private static final int JAVA_SOURCE_SUFFIX_LENGTH = 5;
/*  8:16 */   private static boolean loaded = true;
/*  9:   */   
/* 10:   */   public native String decompile(String paramString1, String paramString2);
/* 11:   */   
/* 12:   */   public native String getVersion();
/* 13:   */ }


/* Location:           D:\Program Files (x86)\Beyond Compare 3\Helpers\Java\jd-1.0.2.jar
 * Qualified Name:     jd.ide.eclipse.editors.JDSourceMapper
 * JD-Core Version:    0.7.0.1
 */