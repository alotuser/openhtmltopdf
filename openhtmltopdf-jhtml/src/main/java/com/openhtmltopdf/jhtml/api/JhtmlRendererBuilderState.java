package com.openhtmltopdf.jhtml.api;

import java.awt.Graphics2D;

import com.openhtmltopdf.java2d.api.FSPageProcessor;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;

/**
 * This class is an internal implementation detail. 
 */
public class JhtmlRendererBuilderState extends BaseRendererBuilder.BaseRendererBuilderState {
	 
	public JhtmlRendererBuilderState() {
	}

	public Graphics2D _layoutGraphics;
	public FSPageProcessor _pageProcessor;
	public boolean _useEnvironmentFonts = false;
	public boolean _usePixelDimensions  = false;
	public boolean _cacheFonts = true;
}
