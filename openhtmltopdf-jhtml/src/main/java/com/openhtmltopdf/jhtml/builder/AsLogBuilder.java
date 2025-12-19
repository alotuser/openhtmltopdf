package com.openhtmltopdf.jhtml.builder;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;

import com.openhtmltopdf.util.Diagnostic;
import com.openhtmltopdf.util.JDKXRLogger;
import com.openhtmltopdf.util.XRLog;
import com.openhtmltopdf.util.XRLogger;
import com.openhtmltopdf.util.XRSimpleLogFormatter;

public class AsLogBuilder {

	public static StringBuilder newStringBuilder() {
		final XRLogger delegate = new JDKXRLogger(false, Level.WARNING, new ConsoleHandler(), new XRSimpleLogFormatter());
		final StringBuilder sb = new StringBuilder();
		XRLog.setLoggerImpl(new StringBuilderLogger(sb, delegate));
		return sb;
	}

	public static class StringBuilderLogger implements XRLogger {
		private final StringBuilder sb;
		private final XRLogger delegate;

		public StringBuilderLogger(StringBuilder sb, XRLogger delegate) {
			this.delegate = delegate;
			this.sb = sb;
		}

		@Override
		public boolean isLogLevelEnabled(Diagnostic diagnostic) {
			return true;
		}

		@Override
		public void setLevel(String logger, Level level) {
		}

		@Override
		public void log(String where, Level level, String msg, Throwable th) {
			if (th == null) {
				log(where, level, msg);
				return;
			}
			StringWriter sw = new StringWriter();
			th.printStackTrace(new PrintWriter(sw, true));
			sb.append(where + ": " + level + ":\n" + msg + sw.toString() + "\n");
			delegate.log(where, level, msg, th);
		}

		@Override
		public void log(String where, Level level, String msg) {
			if (!level.equals(Level.FINEST) && !level.equals(Level.FINER) && !level.equals(Level.FINE)) {
				sb.append(where + ": " + level + ": " + msg + "\n");
			}
			delegate.log(where, level, msg);
		}
	}
}
