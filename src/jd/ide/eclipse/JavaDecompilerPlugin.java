package jd.ide.eclipse;

import org.eclipse.jface.preference.IPreferenceStore;

public class JavaDecompilerPlugin {
	public static final String			PLUGIN_ID						= "jd.ide.eclipse";
	public static final String			VERSION_JD_ECLIPSE				= "0.1.5";
	public static final String			PREF_ESCAPE_UNICODE_CHARACTERS	= "jd.ide.eclipse.prefs.EscapeUnicodeCharacters";
	public static final String			PREF_OMIT_PREFIX_THIS			= "jd.ide.eclipse.prefs.OmitPrefixThis";
	public static final String			PREF_REALIGN_LINE_NUMBERS		= "jd.ide.eclipse.prefs.RealignLineNumbers";
	public static final String			PREF_SHOW_LINE_NUMBERS			= "jd.ide.eclipse.prefs.ShowLineNumbers";
	public static final String			PREF_SHOW_METADATA				= "jd.ide.eclipse.prefs.ShowMetadata";
	public static final String			URL_JDECLIPSE					= "http://en.wikipedia.org/wiki/Java_Decompiler";
	private static JavaDecompilerPlugin	plugin							= new JavaDecompilerPlugin();

	public static JavaDecompilerPlugin getDefault() {
		return plugin;
	}

	private IPreferenceStore	prefs	= new IPreferenceStore();

	public IPreferenceStore getPreferenceStore() {
		return this.prefs;
	}

	public final void savePluginPreferences() {
	}
}
