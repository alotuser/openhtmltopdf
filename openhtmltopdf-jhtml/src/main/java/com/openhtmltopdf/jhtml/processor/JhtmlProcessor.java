package com.openhtmltopdf.jhtml.processor;

import com.openhtmltopdf.jhtml.JhtmlRenderer;

public interface JhtmlProcessor {

	void jhtmlRenderer(JhtmlRenderer jhtmlRenderer);

	String asHtml(String html);

}
