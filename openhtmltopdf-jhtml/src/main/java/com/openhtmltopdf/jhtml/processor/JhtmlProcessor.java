package com.openhtmltopdf.jhtml.processor;

import com.openhtmltopdf.jhtml.api.JhtmlRenderer;

public interface JhtmlProcessor {

	void jhtmlRenderer(JhtmlRenderer jhtmlRenderer);

	String asHtml(String html);

}
