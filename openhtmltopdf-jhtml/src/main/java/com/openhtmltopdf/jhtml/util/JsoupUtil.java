package com.openhtmltopdf.jhtml.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Document.OutputSettings.Syntax;

/**
 * Utility class for parsing HTML strings into Jsoup Document objects with specific output settings.
 */
public class JsoupUtil {

	/**
	 * Parses HTML string into a Jsoup Document with specific output settings.
	 * 
	 * @param html
	 * @return
	 */
	public static Document parse(String html) {
		Document doc = Jsoup.parse(html);
		doc.outputSettings().syntax(Syntax.xml);
		doc.outputSettings().prettyPrint(false);
		return doc;
	}

}