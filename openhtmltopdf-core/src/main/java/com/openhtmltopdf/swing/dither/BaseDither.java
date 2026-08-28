package com.openhtmltopdf.swing.dither;

import java.awt.image.BufferedImage;
import org.w3c.dom.Element;

/**
 * Image dither business interface.
 * Exposed business method: process image dither by parsing custom attributes from Element.
 */
public interface BaseDither {

    String DITHER_COLOR_ATTR  = "dither-color";
    String DITHER_GAMMA_ATTR  = "dither-gamma";
    String DITHER_KERNEL_ATTR = "dither-kernel";

    /** Default gamma correction factor; 0.7-0.9 recommended for e‑ink displays; use 1.0f to disable gamma correction */
    final float DEFAULT_GAMMA = 0.85f;

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
