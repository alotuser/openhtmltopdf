package com.openhtmltopdf.svgsupport;

import com.openhtmltopdf.util.LogMessageId;
import org.apache.batik.bridge.ExternalResourceSecurity;
import org.apache.batik.bridge.FontFamilyResolver;
import org.apache.batik.bridge.ScriptSecurity;
import org.apache.batik.bridge.UserAgentAdapter;
import org.apache.batik.util.ParsedURL;

import com.openhtmltopdf.svgsupport.PDFTranscoder.OpenHtmlFontResolver;
import com.openhtmltopdf.util.XRLog;

import java.util.Set;
import java.util.logging.Level;

public class OpenHtmlUserAgent extends UserAgentAdapter {

	private final OpenHtmlFontResolver resolver;
    private final boolean allowScripts;
    private final boolean allowExternalResources;
    private final Set<String> allowedProtocols;

    public OpenHtmlUserAgent(OpenHtmlFontResolver resolver, boolean allowScripts, boolean allowExternalResources, Set<String> allowedProtocols) {
		this.resolver = resolver;
        this.allowScripts = allowScripts;
        this.allowExternalResources = allowExternalResources;
        this.allowedProtocols = allowedProtocols;
	}

	@Override
	public FontFamilyResolver getFontFamilyResolver() {
		return this.resolver;
	}

    @Override
    public void displayError(Exception e) {
        XRLog.log(Level.SEVERE, LogMessageId.LogMessageId1Param.GENERAL_MESSAGE, e.getLocalizedMessage(), e);
    }

    @Override
    public void displayMessage(String message) {
        XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.GENERAL_MESSAGE, message);
    }

    @Override
    public void checkLoadScript(String scriptType, ParsedURL scriptURL, ParsedURL docURL) throws SecurityException {
        if (!this.allowScripts) {
            XRLog.log(Level.WARNING, LogMessageId.LogMessageId3Param.EXCEPTION_SVG_SCRIPT_NOT_ALLOWED, scriptType, scriptURL, docURL);
            throw new SecurityException("Tried to run script inside SVG!");
        }
    }
    
    @Override
    public void checkLoadExternalResource(ParsedURL resourceURL, ParsedURL docURL) throws SecurityException {
        if (!this.allowExternalResources && (allowedProtocols == null || !allowedProtocols.contains(resourceURL.getProtocol()))) {
            XRLog.log(Level.WARNING, LogMessageId.LogMessageId2Param.EXCEPTION_SVG_EXTERNAL_RESOURCE_NOT_ALLOWED,resourceURL, docURL);
            throw new SecurityException("Tried to fetch external resource (" + resourceURL + ") from SVG. Refused!");
        }
    }
}
