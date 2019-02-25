package com.clu.jd.http;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {

	public static void info(String message) {
		Logger.info(message, (Object[]) null);
	}

	public static void error(String message) {
		Logger.error(message, (Object[]) null);
	}

	public static void info(String message, Object... params) {
		System.out.println(getMessage(message, params));
	}

	public static void error(String message, Object... params) {
		System.err.println(getMessage(message, params));
	}
	
	public static void error(String message, Throwable t) {
		Logger.error(message);
		Logger.error("\r\n" + getExceptionString(t));
	}
	
	private static String getExceptionString(Throwable t) {
		StringWriter writer = new StringWriter();
		PrintWriter printWriter = new PrintWriter(writer);
		t.printStackTrace(printWriter);
		return writer.toString();
	}
	
	private static String getMessage(String message, Object... params) {
		if (params != null && params.length > 0) {
			message = String.format(message, params);
		}
		StackTraceElement caller = getCaller();
		String callerInfo = caller == null ? "" : caller.toString();
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + " {"+ Thread.currentThread().getName() +"} " + callerInfo + " " + message;
	}
	
	private static StackTraceElement getCaller() {
		for (StackTraceElement element : new Exception().getStackTrace()) {
			if (!element.getClassName().equals(Logger.class.getName())) {
				return element;
			}
		}
		return null;
	}

}
