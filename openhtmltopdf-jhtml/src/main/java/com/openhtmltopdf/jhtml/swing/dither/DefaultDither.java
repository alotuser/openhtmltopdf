package com.openhtmltopdf.jhtml.swing.dither;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

import org.w3c.dom.Element;

public interface DefaultDither {

	
    String DITHER_COLOR_ATTR  = "dither-color";
    String DITHER_GAMMA_ATTR  = "dither-gamma";
    String DITHER_KERNEL_ATTR = "dither-kernel";
    String DITHER_CACHE_ATTR  = "dither-cache";

    /** Default gamma correction factor; 0.7-0.9 recommended for e-ink displays; use 1.0f to disable gamma correction */
    final float DEFAULT_GAMMA = 0.85f;
    
    
	default boolean hasCache(Element elem) {

		String cacheAttr = elem.getAttribute(DITHER_CACHE_ATTR);
		return null==cacheAttr ||Boolean.parseBoolean(cacheAttr);
		
	};
    

    /**
	 * High-quality bicubic image scaling, outputs TYPE_3BYTE_BGR
	 * @param src source image
	 * @param targetW target pixel width
	 * @param targetH target pixel height
	 * @return scaled BufferedImage
	 */
    default BufferedImage scaleImage(BufferedImage src, int targetW, int targetH) {
		if (src.getWidth() == targetW && src.getHeight() == targetH) {
			return src;
		}
		BufferedImage dst = new BufferedImage(targetW, targetH, BufferedImage.TYPE_3BYTE_BGR);
		Graphics2D g2d = dst.createGraphics();
		try {
			g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2d.drawImage(src, 0, 0, targetW, targetH, null);
		} finally {
			g2d.dispose();
		}
		return dst;
	}
    
	/**
	 * Apply gamma correction transformation on source image
	 * @param src source BufferedImage
	 * @param gamma gamma factor; 1.0f disables correction
	 * @return gamma-corrected output image
	 */
    default BufferedImage applyGamma(BufferedImage src, float gamma) {
		if (Math.abs(gamma - 1.0f) < 1e-5f) {
			return src;
		}
		int w = src.getWidth();
		int h = src.getHeight();
		BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
		WritableRaster inRaster = src.getRaster();
		WritableRaster outRaster = out.getRaster();
		double invGamma = 1.0 / gamma;
		int[] pix = new int[3];
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				inRaster.getPixel(x, y, pix);
				pix[0] = (int) Math.min(255, Math.max(0, Math.pow(pix[0] / 255.0, invGamma) * 255));
				pix[1] = (int) Math.min(255, Math.max(0, Math.pow(pix[1] / 255.0, invGamma) * 255));
				pix[2] = (int) Math.min(255, Math.max(0, Math.pow(pix[2] / 255.0, invGamma) * 255));
				outRaster.setPixel(x, y, pix);
			}
		}
		return out;
	}
	
    /**
	 * Convert image with alpha channel to opaque image with white background
	 * @param src source BufferedImage
	 * @return opaque BufferedImage with white background
	 */
	default BufferedImage flatToWhite(BufferedImage src) {
	    if (src.getTransparency() == BufferedImage.OPAQUE) {
	        return src;
	    }

	    int width = src.getWidth();
	    int height = src.getHeight();
	    BufferedImage whiteBg = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);

	    Graphics2D g2d = whiteBg.createGraphics();
	    try {
	        g2d.setColor(Color.WHITE);
	        g2d.fillRect(0, 0, width, height);

	        g2d.drawImage(src, 0, 0, null);
	    } finally {
	        g2d.dispose();
	    }

	    return whiteBg;
	}
    
    
    
    
    
}
