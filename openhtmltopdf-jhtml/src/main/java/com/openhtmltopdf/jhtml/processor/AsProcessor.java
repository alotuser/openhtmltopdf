package com.openhtmltopdf.jhtml.processor;

import com.openhtmltopdf.jhtml.renderer.AsRenderer;

public interface AsProcessor {

	void asRenderer(AsRenderer asRenderer);

	String asHtml(String html);

}
