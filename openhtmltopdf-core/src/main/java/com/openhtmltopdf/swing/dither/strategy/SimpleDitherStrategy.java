package com.openhtmltopdf.swing.dither.strategy;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Element;
import com.openhtmltopdf.swing.dither.BaseDither;

/**
 * Multi-color error diffusion dither strategy
 * Reference: dither-dream(kgjenkins/dither-dream MIT), Floyd-Steinberg 1976
 * Features: fixed-point short integer arithmetic (no float), direct Raster manipulation
 * For Linux/Docker production: JVM arg -Djava.awt.headless=true is required
 */
public class SimpleDitherStrategy implements BaseDither {

	/**
	 * Color palette modes for e-ink hardware
	 */
	public enum ColorMode {
		/** Black-white 2-color */
		BW(Arrays.asList(
				new Color(255, 255, 255), // White
				new Color(0, 0, 0)        // Black
		)),
		/** Black-white-red 3-color */
		BWR(Arrays.asList(
				new Color(255, 255, 255),// White
				new Color(0, 0, 0), 	 // Black
				new Color(255, 0, 0)     // Red
		)),
		/** Black-white-yellow 3-color */
		BWY(Arrays.asList(
				new Color(255, 255, 255),// White
				new Color(0, 0, 0),	     // Black
				new Color(255, 255, 0)   // Yellow
		)),
		/** Black-white-red-yellow 4-color */
		BWRY(Arrays.asList(
				new Color(255, 255, 255),// White
				new Color(0, 0, 0),	     // Black
				new Color(255, 0, 0),    // Red
				new Color(255, 255, 0)   // Yellow
		)),
		/** White-black-red-yellow-green-blue-orange 7-color */
		BWRYGBO(Arrays.asList(
				new Color(255, 255, 255), // White
				new Color(0, 0, 0),       // Black
				new Color(255, 0, 0),     // Red
				new Color(255, 255, 0),   // Yellow
				new Color(0, 255, 0),     // Green
				new Color(0, 0, 255),     // Blue
				new Color(255, 165, 0)    // Orange
		));

		private final List<Color> palette;

		ColorMode(List<Color> palette) {
			this.palette = palette;
		}

		public List<Color> getPalette() {
			return palette;
		}
	}

	/**
	 * Dither kernel enumeration for error-diffusion and ordered dither algorithms.
	 * Reference algorithm kernel weights: kgjenkins/dither-dream(MIT License).
	 * <p>Bibliography of original papers / references:</p>
	 * <ul>
	 * <li>Floyd-Steinberg(1976): R.W.Floyd, L.Steinberg, <i>An Adaptive Algorithm for Spatial Gray Scale</i>, SIGGRAPH 1976</li>
	 * <li>Jarvis-Judice-Ninke(1976): <i>A Survey of Techniques for the Image Display of Continuous Tone Pictures on Bilevel Displays</i></li>
	 * <li>Stucki(1981): MECCA-A Multiple-Error Correcting Computation Algorithm for Bilevel Image Hardcopy Reproduction</li>
	 * <li>Burkes(1988): simplified variant of Stucki algorithm</li>
	 * <li>Atkinson(circa 1985): Bill Atkinson(Apple Macintosh firmware), no formal published paper; discards ~25% quantization error</li>
	 * <li>Sierra family(1989): Frank Sierra, Usenet graphics community public-domain weights(Sierra-Lite, Sierra-2-4A, Sierra-3)</li>
	 * <li>Knox: texture-optimized error-diffusion for printed halftone</li>
	 * <li>Stevenson-Arce: blue-noise oriented error-diffusion halftone</li>
	 * <li>Bayer ordered dither(1969): B.E.Bayer, US Patent US3531793, dispersed-dot ordered dither matrix</li>
	 * <li>Book reference: Robert Ulichney, <i>Digital Halftoning</i>, 1987</li>
	 * </ul>
	 *
	 * <p>Implementation note for error-diffusion kernels: maximum vertical offset dy = +2 for all entries in this enum.
	 * The sliding-window ring-buffer implementation in {@code doDitherCore} requires at least 3 physical lines of buffer memory.
	 * <br>CAUTION: Adding new error-diffusion kernel with vertical offset {@code dy > +2} requires increasing ring-buffer line count,
	 * otherwise error propagation will be incomplete and produce corrupted output image.</p>
	 *
	 * <p>Algorithm categories:</p>
	 * <ul>
	 * <li>Error-diffusion: FLOYD_STEINBERG / SIERRA_LITE / SIERRA_2_4A / SIERRA_3 / STUCKI / BURKES / KNOX / STEVENSON_ARCE / ATKINSON / JARVIS_JUDICE_NINKE</li>
	 * <li>Ordered dither: BAYER_8X8, offsets and divisor are unused, handled in separate code branch</li>
	 * </ul>
	 *
	 * <p>E-ink device kernel selection reference:</p>
	 * <table border="1" cellpadding="4" cellspacing="0">
	 * <tr>
	 * <th>Enum Constant</th>
	 * <th>Algorithm Type</th>
	 * <th>Visual Characteristic</th>
	 * <th>Side-effects</th>
	 * <th>Application Scenario</th>
	 * <th>HTML Attribute Example</th>
	 * </tr>
	 * <tr>
	 * <td>{@code FLOYD_STEINBERG}</td>
	 * <td>Error-diffusion</td>
	 * <td>Classic balanced, natural gradation</td>
	 * <td>Medium grain noise, potential serpentine artifacts</td>
	 * <td>Legacy template compatibility, general product images</td>
	 * <td>{@code dither-kernel="FLOYD_STEINBERG"}</td>
	 * </tr>
	 * <tr>
	 * <td>{@code SIERRA_LITE}</td>
	 * <td>Error-diffusion</td>
	 * <td>Clean and soft appearance, stable lines</td>
	 * <td>Very low noise, moderate quality loss compared to full Sierra variants</td>
	 * <td><b>Recommended default: mixed templates with product images and small logos</b></td>
	 * <td>{@code dither-kernel="SIERRA_LITE"}</td>
	 * </tr>
	 * <tr>
	 * <td>{@code SIERRA_2_4A}</td>
	 * <td>Error-diffusion</td>
	 * <td>High-quality smooth output, fine gradation</td>
	 * <td>Higher computation cost</td>
	 * <td>Photographs of real-world merchandise, still-life shots</td>
	 * <td>{@code dither-kernel="SIERRA_2_4A"}</td>
	 * </tr>
	 * <tr>
	 * <td>{@code SIERRA_3}</td>
	 * <td>Error-diffusion</td>
	 * <td>Full-version Sierra, extremely smooth gradation</td>
	 * <td>Highest CPU overhead among Sierra family</td>
	 * <td>Static high-quality product still-life photos, performance-unconstrained scenario</td>
	 * <td>{@code dither-kernel="SIERRA_3"}</td>
	 * </tr>
	 * <tr>
	 * <td>{@code STUCKI}</td>
	 * <td>Error-diffusion</td>
	 * <td>Smooth gradients, good detail preservation</td>
	 * <td>Moderately high CPU overhead</td>
	 * <td>High-quality product and landscape photographs</td>
	 * <td>{@code dither-kernel="STUCKI"}</td>
	 * </tr>
	 * <tr>
	 * <td>{@code BURKES}</td>
	 * <td>Error-diffusion</td>
	 * <td>Balanced smoothness and computation speed</td>
	 * <td>Slightly reduced quality compared to Stucki</td>
	 * <td>Photo scenarios where performance matters</td>
	 * <td>{@code dither-kernel="BURKES"}</td>
	 * </tr>
	 * <tr>
	 * <td>{@code KNOX}</td>
	 * <td>Error-diffusion</td>
	 * <td>Texture-optimized halftone pattern</td>
	 * <td>Moderate compute overhead</td>
	 * <td>Print-oriented texture images, not recommended for mixed text-graphic price-tag</td>
	 * <td>{@code dither-kernel="KNOX"}</td>
	 * </tr>
	 * <tr>
	 * <td>{@code STEVENSON_ARCE}</td>
	 * <td>Error-diffusion</td>
	 * <td>Blue-noise characteristic, uniform grain distribution</td>
	 * <td>High computation cost</td>
	 * <td>High-quality continuous-tone photos, text-free image only</td>
	 * <td>{@code dither-kernel="STEVENSON_ARCE"}</td>
	 * </tr>
	 * <tr>
	 * <td>{@code ATKINSON}</td>
	 * <td>Error-diffusion</td>
	 * <td>Film-like appearance, high contrast</td>
	 * <td>Dark-area clipping, small text tends to blur; discards ~25% quantization error</td>
	 * <td>Use with caution; for text-free landscape images</td>
	 * <td>{@code dither-kernel="ATKINSON"}</td>
	 * </tr>
	 * <tr>
	 * <td>{@code JARVIS_JUDICE_NINKE}</td>
	 * <td>Error-diffusion</td>
	 * <td>Wide diffusion range, extremely soft transitions</td>
	 * <td>Loss of fine details, small text blurring; highest computational cost among built-in kernels</td>
	 * <td>Use with caution; large-size images without small text or logos</td>
	 * <td>{@code dither-kernel="JARVIS_JUDICE_NINKE"}</td>
	 * </tr>
	 * <tr>
	 * <td>{@code BAYER_8X8}</td>
	 * <td>Ordered dither</td>
	 * <td>Fastest execution speed, deterministic output</td>
	 * <td>Grid-like artifacts appear on smooth color gradients</td>
	 * <td>Icons and simple graphics, performance-prioritized scenarios</td>
	 * <td>{@code dither-kernel="BAYER_8X8"}</td>
	 * </tr>
	 * </table>
	 *
	 * <p>Note: HTML attribute value must exactly match uppercase enum name.
	 * BAYER_8X8 uses separate logic branch instead of error-diffusion processing loop.</p>
	 */
	public enum DitherKernel {
		/**
		 * Floyd-Steinberg classic error-diffusion, balanced output, legacy compatibility.
		 * Reference: SIGGRAPH 1976 paper.
		 */
		FLOYD_STEINBERG(new int[][]{{1, 0, 7}, {-1, 1, 3}, {0, 1, 5}, {1, 1, 1}}, 16),

		/**
		 * Sierra-Lite [Recommended Default] low noise, clean lines, good performance for product + small-logo content.
		 * Light-weight simplified Sierra variant from Usenet graphics community.
		 */
		SIERRA_LITE(new int[][]{{1, 0, 2}, {-1, 1, 1}, {0, 1, 1}, {1, 1, 1}}, 5),

		/**
		 * Sierra-2-4A high-quality smooth error-diffusion with fine gradation; for real-world product photos, higher CPU overhead.
		 * Two-row Sierra variant from Usenet graphics community.
		 */
		SIERRA_2_4A(new int[][]{{1, 0, 4}, {2, 0, 3}, {-2, 1, 1}, {-1, 1, 2}, {0, 1, 3}, {1, 1, 2}, {2, 1, 1}}, 16),

		/**
		 * Sierra-3 full 3-row error-diffusion algorithm; higher computational cost, very smooth gradient output.
		 * Full weight set of Sierra family, best gradation quality but expensive to compute.
		 */
		SIERRA_3(new int[][] { { 1, 0, 5 }, { 2, 0, 3 }, { -2, 1, 2 }, { -1, 1, 4 }, { 0, 1, 5 }, { 1, 1, 4 }, { 2, 1, 2 }, { -2, 2, 1 }, { -1, 2, 2 }, { 0, 2, 3 }, { 1, 2, 2 }, { 2, 2, 1 } }, 32),

		/**
		 * Knox error-diffusion, texture-oriented halftone, optimized for printed textures; rarely used for e-ink price-tag templates.
		 * Tuned for texture reproduction, not friendly to small text or thin lines.
		 */
		KNOX(new int[][] { { 1, 0, 4 }, { 2, 0, 3 }, { -2, 1, 1 }, { -1, 1, 2 }, { 0, 1, 3 }, { 1, 1, 2 }, { 2, 1, 1 }, { -1, 2, 1 }, { 0, 2, 2 }, { 1, 2, 1 } }, 20),

		/**
		 * Stevenson-Arce error-diffusion, blue-noise property, more uniform noise distribution; good for continuous-tone images.
		 * Produces visually pleasant grain pattern, high CPU consumption.
		 */
		STEVENSON_ARCE(new int[][] { { 1, 0, 8 }, { 2, 0, 4 }, { -2, 1, 2 }, { -1, 1, 4 }, { 0, 1, 8 }, { 1, 1, 4 }, { 2, 1, 2 }, { -2, 2, 1 }, { -1, 2, 2 }, { 0, 2, 4 }, { 1, 2, 2 }, { 2, 2, 1 } }, 48),

		/**
		 * Stucki error-diffusion, smooth gradient, better detail preservation than Floyd-Steinberg, higher CPU cost.
		 * Reference: MECCA algorithm paper 1981.
		 */
		STUCKI(new int[][] { { 1, 0, 8 }, { 2, 0, 4 }, { -2, 1, 2 }, { -1, 1, 4 }, { 0, 1, 8 }, { 1, 1, 4 }, { 2, 1, 2 }, { -2, 2, 1 }, { -1, 2, 2 }, { 0, 2, 4 }, { 1, 2, 2 }, { 2, 2, 1 } }, 42),

		/**
		 * Burkes error-diffusion, balance of speed and image smoothness.
		 * Simplified performance-optimized variant derived from Stucki.
		 */
		BURKES(new int[][] { { 1, 0, 8 }, { 2, 0, 4 }, { -2, 1, 2 }, { -1, 1, 4 }, { 0, 1, 8 }, { 1, 1, 4 }, { 2, 1, 2 } }, 32),

		/**
		 * Atkinson high-contrast film-style; ⚠️ dark clipping risk, small text may blur, avoid heavy text templates.
		 * Origin: Apple Macintosh firmware, discards approximately 25% quantization error.
		 */
		ATKINSON(new int[][] { { 1, 0, 1 }, { 2, 0, 1 }, { -1, 1, 1 }, { 0, 1, 1 }, { 1, 1, 1 }, { 0, 2, 1 } }, 8),

		/**
		 * Jarvis-Judice-Ninke wide-range diffusion for very soft gradients; ⚠️ fine-detail loss risk, not suitable for small text or logos.
		 * Reference: 1976 bilevel display survey paper; highest computation cost among built-in kernels.
		 */
		JARVIS_JUDICE_NINKE(new int[][] { { 1, 0, 7 }, { 2, 0, 5 }, { -2, 1, 3 }, { -1, 1, 5 }, { 0, 1, 7 }, { 1, 1, 5 }, { 2, 1, 3 }, { -2, 2, 1 }, { -1, 2, 3 }, { 0, 2, 5 }, { 1, 2, 3 }, { 2, 2, 1 } }, 48),

		/**
		 * Bayer 8×8 ordered dither, no error diffusion; fastest speed for icons/simple graphics; gradients produce visible grid patterns.
		 * Reference: US Patent US3531793 (1969).
		 */
		BAYER_8X8(null, 0);

		/**
		 * Error-diffusion offset table: each entry format {dx, dy, weight}.
		 * Unused for ordered-dither ({@code BAYER_8X8}).
		 */
		private final int[][] offsets;

		/**
		 * Sum of diffusion weights, divisor for error distribution calculation.
		 * Unused for ordered-dither ({@code BAYER_8X8}).
		 */
		private final int divisor;

		DitherKernel(int[][] offsets, int divisor) {
			this.offsets = offsets;
			this.divisor = divisor;
		}

		/**
		 * Get error-diffusion offset weight table.
		 * @return offset table, null for ordered-dither kernel.
		 */
		public int[][] getOffsets() {
			return offsets;
		}

		/**
		 * Get divisor for error weight normalization.
		 * @return divisor value, undefined for ordered-dither kernel.
		 */
		public int getDivisor() {
			return divisor;
		}

		/**
		 * Judge whether this kernel belongs to ordered-dither (non-error-diffusion).
		 * @return true if ordered dither, false for error-diffusion kernel.
		 */
		public boolean isOrderedDither() {
			return this == BAYER_8X8;
		}
    }


	private static final int[][] BAYER_8X8_MATRIX = {
			{0, 32, 8, 40, 2, 34, 10, 42},
			{48, 16, 56, 24, 50, 18, 58, 26},
			{12, 44, 4, 36, 14, 46, 6, 38},
			{60, 28, 52, 20, 62, 30, 54, 22},
			{3, 35, 11, 43, 1, 33, 9, 41},
			{51, 19, 59, 27, 49, 17, 57, 25},
			{15, 47, 7, 39, 13, 45, 5, 37},
			{63, 31, 55, 23, 61, 29, 53, 21}
	};

	/**
	 * Builder for dither parameter configuration
	 */
	public static class Builder {
		private BufferedImage src;
		private int targetWidth;
		private int targetHeight;
		private ColorMode colorMode;
		private DitherKernel kernel;
		private byte[] outHardwareIndexBytes;
		private float gamma = DEFAULT_GAMMA;

		private Builder() {
		}

		public static Builder create() {
			return new Builder();
		}

		/**
		 * Set source input image
		 * @param src source BufferedImage
		 * @return builder instance
		 */
		public Builder src(BufferedImage src) {
			this.src = src;
			return this;
		}

		/**
		 * Set output target width and height
		 * @param width target pixel width
		 * @param height target pixel height
		 * @return builder instance
		 */
		public Builder targetSize(int width, int height) {
			this.targetWidth = width;
			this.targetHeight = height;
			return this;
		}

		/**
		 * Set hardware palette color mode
		 * @param colorMode target ColorMode enum
		 * @return builder instance
		 */
		public Builder colorMode(ColorMode colorMode) {
			this.colorMode = colorMode;
			return this;
		}

		/**
		 * Set dither diffusion kernel
		 * @param kernel target DitherKernel enum
		 * @return builder instance
		 */
		public Builder kernel(DitherKernel kernel) {
			this.kernel = kernel;
			return this;
		}

		/**
		 * Optional output hardware palette index byte array, may be null
		 * @param outHardwareIndexBytes output index buffer
		 * @return builder instance
		 */
		public Builder outHardwareIndexBytes(byte[] outHardwareIndexBytes) {
			this.outHardwareIndexBytes = outHardwareIndexBytes;
			return this;
		}

		/**
		 * Set gamma correction factor; use 1.0f to disable correction; 0.7-0.9 recommended for e-ink displays
		 * @param gamma gamma factor
		 * @return builder instance
		 */
		public Builder gamma(float gamma) {
			this.gamma = gamma;
			return this;
		}

		/**
		 * Execute dither pipeline and return processed output image
		 * @return dithered BufferedImage
		 */
		public BufferedImage dither() {
			// Validate required input parameters
			if (src == null) {
				throw new IllegalArgumentException("src image must not be null");
			}
			if (targetWidth <= 0 || targetHeight <= 0) {
				throw new IllegalArgumentException("targetWidth and targetHeight must > 0");
			}
			if (colorMode == null) {
				throw new IllegalArgumentException("colorMode must not be null");
			}
			if (kernel == null) {
				throw new IllegalArgumentException("kernel must not be null");
			}
			if (outHardwareIndexBytes != null && outHardwareIndexBytes.length < targetWidth * targetHeight) {
				throw new IllegalArgumentException("outHardwareIndexBytes buffer length too small");
			}

			BufferedImage scaled = scaleImage(src, targetWidth, targetHeight);
			BufferedImage gammaImg = applyGamma(scaled, gamma);
			return doDitherCore(gammaImg, colorMode, kernel, outHardwareIndexBytes);
		}
	}

	/**
	 * Palette lookup cache for fast nearest-color calculation
	 */
	private static class PaletteCache {
		int size;
		int[] rArr;
		int[] gArr;
		int[] bArr;

		public PaletteCache(List<Color> palette) {
			size = palette.size();
			rArr = new int[size];
			gArr = new int[size];
			bArr = new int[size];
			for (int i = 0; i < size; i++) {
				Color c = palette.get(i);
				rArr[i] = c.getRed();
				gArr[i] = c.getGreen();
				bArr[i] = c.getBlue();
			}
		}
	}

	/**
	 * Core dither processing routine
	 * @param src input pre-processed BufferedImage
	 * @param colorMode target palette mode
	 * @param kernel selected dither kernel
	 * @param outHardwareIndexBytes optional hardware index output buffer, nullable
	 * @return final dither image
	 */
	private static BufferedImage doDitherCore(BufferedImage src, ColorMode colorMode, DitherKernel kernel, byte[] outHardwareIndexBytes) {
		int width = src.getWidth();
		int height = src.getHeight();

		if (kernel.isOrderedDither()) {
			return runBayer8x8(src, colorMode, outHardwareIndexBytes);
		}

		List<Color> palette = colorMode.getPalette();
		PaletteCache pc = new PaletteCache(palette);

		short[][][] buf = new short[height][width][3];

		Raster srcRaster = src.getRaster();
		int[] pixelBuf = new int[3];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				srcRaster.getPixel(x, y, pixelBuf);
				buf[y][x][0] = (short) (pixelBuf[0] << 4);
				buf[y][x][1] = (short) (pixelBuf[1] << 4);
				buf[y][x][2] = (short) (pixelBuf[2] << 4);
			}
		}

		int[][] offsets = kernel.getOffsets();
		int divisor = kernel.getDivisor();
		int[][] indexOut = new int[height][width];

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int r = buf[y][x][0] >> 4;
				int g = buf[y][x][1] >> 4;
				int b = buf[y][x][2] >> 4;

				int nearestIdx = findNearestIndex(pc, r, g, b);
				int nr = pc.rArr[nearestIdx];
				int ng = pc.gArr[nearestIdx];
				int nb = pc.bArr[nearestIdx];

				indexOut[y][x] = nearestIdx;

				int errR = buf[y][x][0] - (nr << 4);
				int errG = buf[y][x][1] - (ng << 4);
				int errB = buf[y][x][2] - (nb << 4);

				buf[y][x][0] = (short) (nr << 4);
				buf[y][x][1] = (short) (ng << 4);
				buf[y][x][2] = (short) (nb << 4);

				for (int[] off : offsets) {
					int nx = x + off[0];
					int ny = y + off[1];
					int w = off[2];
					if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
						buf[ny][nx][0] += (errR * w) / divisor;
						buf[ny][nx][1] += (errG * w) / divisor;
						buf[ny][nx][2] += (errB * w) / divisor;

						buf[ny][nx][0] = clampShort(buf[ny][nx][0], 0 << 4, 255 << 4);
						buf[ny][nx][1] = clampShort(buf[ny][nx][1], 0 << 4, 255 << 4);
						buf[ny][nx][2] = clampShort(buf[ny][nx][2], 0 << 4, 255 << 4);
					}
				}
			}
		}

		if (outHardwareIndexBytes != null) {
			int pos = 0;
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					outHardwareIndexBytes[pos++] = (byte) indexOut[y][x];
				}
			}
		}

		BufferedImage outImg = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		WritableRaster outRaster = outImg.getRaster();
		int[] outPix = new int[3];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int idx = indexOut[y][x];
				outPix[0] = pc.rArr[idx];
				outPix[1] = pc.gArr[idx];
				outPix[2] = pc.bArr[idx];
				outRaster.setPixel(x, y, outPix);
			}
		}
		return outImg;
	}

	/**
	 * Bayer-8x8 ordered dither implementation, optimized for icons and simple vector graphics
	 * @param src source image
	 * @param colorMode palette mode
	 * @param outHardwareIndexBytes optional hardware index output buffer
	 * @return ordered-dithered output image
	 */
	private static BufferedImage runBayer8x8(BufferedImage src, ColorMode colorMode, byte[] outHardwareIndexBytes) {
		int width = src.getWidth();
		int height = src.getHeight();
		PaletteCache pc = new PaletteCache(colorMode.getPalette());

		Raster srcRaster = src.getRaster();
		BufferedImage outImg = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		WritableRaster outRaster = outImg.getRaster();

		int[] pixelBuf = new int[3];
		int pos = 0;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				srcRaster.getPixel(x, y, pixelBuf);
				int r = pixelBuf[0];
				int g = pixelBuf[1];
				int b = pixelBuf[2];

				int threshold = BAYER_8X8_MATRIX[y & 7][x & 7] * 4 - 128;
				int nr = Math.max(0, Math.min(255, r + threshold));
				int ng = Math.max(0, Math.min(255, g + threshold));
				int nb = Math.max(0, Math.min(255, b + threshold));

				int idx = findNearestIndex(pc, nr, ng, nb);
				pixelBuf[0] = pc.rArr[idx];
				pixelBuf[1] = pc.gArr[idx];
				pixelBuf[2] = pc.bArr[idx];
				outRaster.setPixel(x, y, pixelBuf);

				if (outHardwareIndexBytes != null) {
					outHardwareIndexBytes[pos++] = (byte) idx;
				}
			}
		}
		return outImg;
	}

	/**
	 * Apply gamma correction transformation on source image
	 * @param src source BufferedImage
	 * @param gamma gamma factor; 1.0f disables correction
	 * @return gamma-corrected output image
	 */
	private static BufferedImage applyGamma(BufferedImage src, float gamma) {
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
	 * High-quality bicubic image scaling, outputs TYPE_3BYTE_BGR
	 * @param src source image
	 * @param targetW target pixel width
	 * @param targetH target pixel height
	 * @return scaled BufferedImage
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
	 * Find nearest palette color index by Euclidean squared RGB distance
	 * @param pc palette cache instance
	 * @param r input red channel 0-255
	 * @param g input green channel 0-255
	 * @param b input blue channel 0-255
	 * @return best-match palette index
	 */
	private static int findNearestIndex(PaletteCache pc, int r, int g, int b) {
		int bestIdx = 0;
		long minDist = Long.MAX_VALUE;
		for (int i = 0; i < pc.size; i++) {
			int dr = r - pc.rArr[i];
			int dg = g - pc.gArr[i];
			int db = b - pc.bArr[i];
			long dist = (long) dr * dr + (long) dg * dg + (long) db * db;
			if (dist < minDist) {
				minDist = dist;
				bestIdx = i;
			}
		}
		return bestIdx;
	}

	/**
	 * Clamp short integer value to given inclusive min-max bounds
	 * @param val input short value
	 * @param min lower bound
	 * @param max upper bound
	 * @return clamped short value
	 */
	private static short clampShort(short val, int min, int max) {
		if (val < min) {
			return (short) min;
		}
		if (val > max) {
			return (short) max;
		}
		return val;
	}

	/**
	 * Execute dither processing by reading HTML element custom attributes
	 * @param elem img DOM element
	 * @param width target output width
	 * @param height target output height
	 * @param newImg source input image
	 * @return processed dithered image; returns original image if required attributes are missing
	 */
	@Override
	public BufferedImage toImg(Element elem, int width, int height, BufferedImage newImg) {

		String colorAttr = elem.getAttribute(DITHER_COLOR_ATTR);
		String gammaAttr = elem.getAttribute(DITHER_GAMMA_ATTR);
		String kernelAttr = elem.getAttribute(DITHER_KERNEL_ATTR);

		if (colorAttr != null && !colorAttr.trim().isEmpty() && kernelAttr != null && !kernelAttr.trim().isEmpty()) {
			if (newImg == null) {
				return null;
			}
			try {
				ColorMode colorMode = ColorMode.valueOf(colorAttr.trim().toUpperCase());
				DitherKernel ditherKernel = DitherKernel.valueOf(kernelAttr.trim().toUpperCase());
				float useGamma = DEFAULT_GAMMA;
				if (gammaAttr != null && !gammaAttr.trim().isEmpty()) {
					try {
						useGamma = Float.parseFloat(gammaAttr.trim());
					} catch (NumberFormatException nfe) {
						throw new IllegalArgumentException("Invalid gamma value: " + gammaAttr, nfe);
					}
				}

				return Builder.create().src(newImg).targetSize(width, height).colorMode(colorMode).kernel(ditherKernel).gamma(useGamma).dither();
			} catch (Exception e) {
				throw new IllegalArgumentException("Invalid palette or kernel attribute: " + colorAttr + ", " + kernelAttr, e);
			}
		}
		return newImg;
	}
}
