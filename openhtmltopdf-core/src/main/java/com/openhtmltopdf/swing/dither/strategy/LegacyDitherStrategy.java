package com.openhtmltopdf.swing.dither.strategy;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import org.w3c.dom.Element;
import com.openhtmltopdf.swing.dither.BaseDither;

/**
 * Legacy Floyd-Steinberg dither strategy for backward-compatibility with old templates.
 * Only recognizes dither-color attribute. Does NOT support gamma correction or multiple dither-kernel selection.
 */
public class LegacyDitherStrategy implements BaseDither {

	/**
	 * RGB pixel channel container storing red, green, blue byte channels
	 */
	public static class RGBTriple {
		public final byte[] channels;

		/**
		 * Create an empty RGBTriple instance
		 */
		public RGBTriple() {
			channels = new byte[3];
		}

		/**
		 * Construct RGBTriple with specified RGB component values
		 * @param R red channel value, range 0-255
		 * @param G green channel value, range 0-255
		 * @param B blue channel value, range 0-255
		 */
		public RGBTriple(int R, int G, int B) {
			channels = new byte[] { (byte) R, (byte) G, (byte) B };
		}
	}

	/**
	 * Palette enumeration for legacy template compatibility
	 */
	public enum ColorEnum {
		/** Black-white 2-color palette */
		BW(new RGBTriple[] { new RGBTriple(0, 0, 0), new RGBTriple(255, 255, 255) }),
		/** Black-white-red 3-color palette */
		BWR(new RGBTriple[] { new RGBTriple(0, 0, 0), new RGBTriple(255, 255, 255), new RGBTriple(255, 0, 0) }),
		/** Black-white-yellow 3-color palette */
		BWY(new RGBTriple[] { new RGBTriple(0, 0, 0), new RGBTriple(255, 255, 255), new RGBTriple(255, 255, 0) }),
	    /** Black-white-red-yellow 4-color palette */
		BWRY(new RGBTriple[]{
	    		new RGBTriple(0, 0, 0),
	    		new RGBTriple(255, 255, 255),
	    		new RGBTriple(255, 0, 0),
	    		new RGBTriple(255, 255, 0)
	    		}),
	    /** Black-white-red-yellow-green-blue-orange 7-color palette */
		BWRYGBO(new RGBTriple[]{
	    		new RGBTriple(0, 0, 0),
	    		new RGBTriple(255, 255, 255),
	    		new RGBTriple(255, 0, 0),
	    		new RGBTriple(255, 255, 0),
	            new RGBTriple(0, 250, 0),
	            new RGBTriple(0, 0, 255),
	            new RGBTriple(255, 125, 0)
	       }
	    );

	    private final RGBTriple[] rgbTriples;

	    ColorEnum(RGBTriple[] rgbTriples) {
	        this.rgbTriples = rgbTriples;
	    }

	    /**
	     * Get palette RGB triple array
	     * @return array of palette color entries
	     */
	    public RGBTriple[] getRgbTriples() {
	        return rgbTriples;
	    }
	}

    /**
     * Run complete legacy dither workflow including pixel conversion and Floyd-Steinberg processing
     * @param bufferedImage source input image
     * @param colorEnum target legacy palette
     * @return new BufferedImage after dither processing
     */
    public static BufferedImage getPerfectImageData(BufferedImage bufferedImage, ColorEnum colorEnum) {
        int height = bufferedImage.getHeight();
        int width = bufferedImage.getWidth();
        RGBTriple[][] triple = new RGBTriple[width][height];

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int rgb = bufferedImage.getRGB(j, i);
                Color color = new Color(rgb);
                triple[j][i] = new RGBTriple(color.getRed(), color.getGreen(), color.getBlue());
            }
        }

        RGBTriple[] palette = colorEnum.getRgbTriples();
        if (palette == null) {
            return null;
        }

        byte[][] floydSteinbergDither = floydSteinbergDither(triple, palette);
        BufferedImage bufferedImageNew = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int i = 0; i < floydSteinbergDither.length; i++) {
            for (int j = 0; j < floydSteinbergDither[i].length; j++) {
                byte rgb = floydSteinbergDither[i][j];
                Color color;
                if (rgb == 0) {
                    color = new Color(0, 0, 0);
                } else if (rgb == 2) {
                    if (colorEnum == ColorEnum.BWY) {
                        color = new Color(255, 255, 0);
                    } else {
                        color = new Color(255, 0, 0);
                    }
                } else if (rgb == 4) {
                    color = new Color(0, 255, 0);
                } else if (rgb == 3) {
                    color = new Color(255, 255, 0);
                } else if (rgb == 5) {
                    color = new Color(0, 0, 255);
                } else if (rgb == 6) {
                    color = new Color(255, 125, 0);
                } else {
                    color = new Color(255, 255, 255);
                }
                bufferedImageNew.setRGB(i, j, color.getRGB());
            }
        }
        return bufferedImageNew;
    }

    /**
     * Core Floyd-Steinberg error-diffusion dither algorithm
     * @param image source pixel 2D array
     * @param palette target color palette
     * @return 2D byte array storing palette index for each pixel
     */
    private static byte[][] floydSteinbergDither(RGBTriple[][] image, RGBTriple[] palette) {
        Map<Byte, Byte> map = new HashMap<>();
        byte[][] result = new byte[image.length][image[0].length];

        for (int y = 0; y < image.length; y++) {
            for (int x = 0; x < image[y].length; x++) {
                RGBTriple currentPixel = image[y][x];
                byte index = findNearestColor(currentPixel, palette);
                result[y][x] = index;
                map.put(index, index);

                for (int i = 0; i < 3; i++) {
                    int error = (currentPixel.channels[i] & 0xff) - (palette[index].channels[i] & 0xff);
                    if (x + 1 < image[0].length) {
                        image[y][x + 1].channels[i] = plusTruncateUchar(image[y][x + 1].channels[i], (error * 6) >> 4);
                    }
                    if (y + 1 < image.length) {
                        if (x - 1 > 0) {
                            image[y + 1][x - 1].channels[i] = plusTruncateUchar(image[y + 1][x - 1].channels[i], (error * 3) >> 4);
                        }
                        image[y + 1][x].channels[i] = plusTruncateUchar(image[y + 1][x].channels[i], (error * 5) >> 4);
                        if (x + 1 < image[0].length) {
                            image[y + 1][x + 1].channels[i] = plusTruncateUchar(image[y + 1][x + 1].channels[i], (error) >> 4);
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Add integer offset to unsigned byte and clamp value within 0-255
     * @param a original byte value
     * @param b offset value to add
     * @return clamped unsigned byte result
     */
    private static byte plusTruncateUchar(byte a, int b) {
        if ((a & 0xff) + b < 0) {
            return 0;
        } else if ((a & 0xff) + b > 255) {
            return (byte) 255;
        } else {
            return (byte) (a + b);
        }
    }

    /**
     * Find index of nearest palette color by squared RGB Euclidean distance
     * @param color source pixel RGB value
     * @param palette target color palette
     * @return byte index of the closest matching palette entry
     */
    private static byte findNearestColor(RGBTriple color, RGBTriple[] palette) {
        int minDistanceSquared = 255 * 255 + 255 * 255 + 255 * 255 + 1;
        byte bestIndex = 0;
        for (byte i = 0; i < palette.length; i++) {
            int Rdiff = (color.channels[0] & 0xff) - (palette[i].channels[0] & 0xff);
            int Gdiff = (color.channels[1] & 0xff) - (palette[i].channels[1] & 0xff);
            int Bdiff = (color.channels[2] & 0xff) - (palette[i].channels[2] & 0xff);
            int distanceSquared = Rdiff * Rdiff + Gdiff * Gdiff + Bdiff * Bdiff;
            if (distanceSquared < minDistanceSquared) {
                minDistanceSquared = distanceSquared;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

	/**
	 * Resize source image using bicubic interpolation
	 * @param src source BufferedImage
	 * @param targetW target pixel width
	 * @param targetH target pixel height
	 * @return resized 3BYTE_BGR BufferedImage
	 */
	private static BufferedImage scaleImage(BufferedImage src, int targetW, int targetH) {
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
	 * {@inheritDoc}
	 * <p>Only processes image when dither-color attribute exists. Ignores dither-kernel and dither-gamma attributes.</p>
	 * @param elem img DOM element
	 * @param width target output pixel width
	 * @param height target output pixel height
	 * @param newImg source input image
	 * @return processed dithered image; returns original image if required attribute is missing
	 */
	@Override
	public BufferedImage toImg(Element elem, int width, int height, BufferedImage newImg) {
		String colorAttr = elem.getAttribute(DITHER_COLOR_ATTR);
		if (colorAttr != null && !colorAttr.trim().isEmpty()) {
			if (newImg == null) {
				return null;
			}
			try {
				ColorEnum colorMode = ColorEnum.valueOf(colorAttr.trim().toUpperCase());
				BufferedImage nimg = scaleImage(newImg, width, height);
				return getPerfectImageData(nimg, colorMode);
			} catch (Exception e) {
				throw new IllegalArgumentException("Invalid palette or kernel attribute: " + colorAttr, e);
			}
		}
		return newImg;
	}
}
