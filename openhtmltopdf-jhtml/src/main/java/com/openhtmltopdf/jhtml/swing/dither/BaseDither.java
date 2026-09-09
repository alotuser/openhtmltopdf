package com.openhtmltopdf.jhtml.swing.dither;

import java.awt.image.BufferedImage;

import org.w3c.dom.Element;

/**
 * Image dither business interface.
 * Exposed business method: process image dither by parsing custom attributes from Element.
 */
public interface BaseDither extends DefaultDither{

    /**
     * Perform image dither according to attributes on Element tag.
     * @param elem html img element
     * @param width target output pixel width
     * @param height target output pixel height
     * @param newImg source input image
     * @return dithered image; return original image if dither conditions are not satisfied
     */
    BufferedImage toImg(Element elem, int width, int height, BufferedImage newImg);

	
    
    
    
    
    
	
}
