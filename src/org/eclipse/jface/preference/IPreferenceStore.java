package org.eclipse.jface.preference;

import java.util.HashMap;
import java.util.Map;

public class IPreferenceStore {
	private static Map<String, Boolean>	prefs	= new HashMap<>();

	static {
		prefs.put("jd.ide.eclipse.prefs.EscapeUnicodeCharacters", Boolean.valueOf(getEnvValue("jd.ide.eclipse.prefs.EscapeUnicodeCharacters", false)));
		prefs.put("jd.ide.eclipse.prefs.OmitPrefixThis", Boolean.valueOf(getEnvValue("jd.ide.eclipse.prefs.OmitPrefixThis", false)));
		prefs.put("jd.ide.eclipse.prefs.RealignLineNumbers", Boolean.valueOf(getEnvValue("jd.ide.eclipse.prefs.RealignLineNumbers", true)));
		prefs.put("jd.ide.eclipse.prefs.ShowLineNumbers", Boolean.valueOf(getEnvValue("jd.ide.eclipse.prefs.ShowLineNumbers", true)));
		prefs.put("jd.ide.eclipse.prefs.ShowMetadata", Boolean.valueOf(getEnvValue("jd.ide.eclipse.prefs.ShowMetadata", false)));
	}

	private static boolean getEnvValue(String configName, boolean defaultVal) {
		String envVal = System.getenv(configName);
		if ("true".equalsIgnoreCase(envVal)) {
			return true;
		}
		if ("false".equalsIgnoreCase(envVal)) {
			return false;
		}
		return defaultVal;
	}

	public boolean getBoolean(String paramString) {
		Boolean result = (Boolean) prefs.get(paramString);
		if (result == null) {
			result = Boolean.valueOf(false);
		}
		return result.booleanValue();
	}

	public void setValue(String paramString, double paramDouble) {
		System.out.println(11);
	}

	public void setValue(String paramString, float paramFloat) {
		System.out.println(15);
	}

	public void setValue(String paramString, int paramInt) {
		System.out.println(19);
	}

	public void setValue(String paramString, long paramLong) {
		System.out.println(23);
	}

	public void setValue(String paramString1, String paramString2) {
		System.out.println(27);
	}

	public void setValue(String paramString, boolean paramBoolean) {
		System.out.println(31);
	}
}
