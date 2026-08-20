package com.openhtmltopdf.util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.Arrays;
import java.util.List;

/**
 * 多色误差扩散抖动工具类
 * 参考：dither-dream(kgjenkins/dither-dream MIT)、Floyd-Steinberg 1976
 * 特性：定点short整数运算，无float；直接操作Raster
 * 生产环境Linux/Docker必须JVM启动参数：‑Djava.awt.headless=true
 */
public class ColorDitherUtil {

	public static final String PALETTE_MODE_ATTR = "data-palette-mode";
	public static final String DITHER_KERNEL_ATTR ="data-dither-kernel";
	
	/**
	 * 调色板
	 * 参考各厂彩色墨水屏硬件色定义
	 */
	public enum PaletteMode {

		/** 黑白 2色 */
		BW(Arrays.asList(
				new Color(255, 255, 255), //白
				new Color(0, 0, 0)        //黑
		)),
		/** 黑白红 3色 */
		BWR(Arrays.asList(
				new Color(255, 255, 255),//白
				new Color(0, 0, 0), 	 //黑
				new Color(255, 0, 0)     //红
		)),
		/** 黑白黄 3色 */
		BWY(Arrays.asList(
				new Color(255, 255, 255),//白
				new Color(0, 0, 0),	     //黑
				new Color(255, 255, 0)   //黄
		)),
		/** 黑白红黄 4色 */
		BWRY(Arrays.asList(
				new Color(255, 255, 255),//白
				new Color(0, 0, 0),	     //黑
				new Color(255, 0, 0),    //红
				new Color(255, 255, 0)   //黄
		)),
		/** 白黑红黄蓝绿橙 7色 */
		BWRYGBO(Arrays.asList(
				new Color(255, 255, 255), //白
				new Color(0, 0, 0),       //黑
				new Color(255, 0, 0),     //红
				new Color(255, 255, 0),   //黄
				new Color(0, 255, 0),     //绿
				new Color(0, 0, 255),     //蓝
				new Color(255, 165, 0)    //橙
		));

		private List<Color> palette;

		PaletteMode(List<Color> palette) {
			this.palette = palette;
		}

		public List<Color> getPalette() {
			return palette;
		}

	}

	/**
	 * 误差扩散内核，参考 dither-dream kgjenkins/dither-dream MIT
	 */
	public enum DitherKernel {

		/**
		 * Floyd-Steinberg
		 */
		FLOYD_STEINBERG(new int[][]{{1, 0, 7}, {-1, 1, 3}, {0, 1, 5}, {1, 1, 1}}, 16),

		/**
		 * Sierra-Lite 噪点少
		 */
		SIERRA_LITE(new int[][]{{1, 0, 2}, {2, 0, 1}, {-1, 1, 1}, {0, 1, 2}, {1, 1, 1}}, 4),

		/**
		 * Atkinson，文字优选
		 */
		ATKINSON(new int[][]{{1, 0, 1}, {2, 0, 1}, {-1, 1, 1}, {0, 1, 1}, {1, 1, 1}, {0, 2, 1}}, 8);

		private final int[][] offsets;
		private final int divisor;

		DitherKernel(int[][] offsets, int divisor) {
			this.offsets = offsets;
			this.divisor = divisor;
		}

		public int[][] getOffsets() {
			return offsets;
		}

		public int getDivisor() {
			return divisor;
		}
	}

	/**
	 * 调色板预缓存结构
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
	 * 高性能定点误差扩散抖动，使用原图尺寸
	 * @param src 输入原图
	 * @param paletteMode 调色板模式
	 * @param kernel 抖动内核
	 * @return 抖动完成后的图像 BufferedImage(TYPE_3BYTE_BGR)
	 */
	public static BufferedImage dither(BufferedImage src, PaletteMode paletteMode, DitherKernel kernel) {
		return dither(src, src.getWidth(), src.getHeight(), paletteMode, kernel, null);
	}

	/**
	 * 高性能定点误差扩散抖动，先缩放到目标宽高，再抖动
	 * @param src 输入原图
	 * @param targetWidth 目标宽度
	 * @param targetHeight 目标高度
	 * @param paletteMode 调色板模式
	 * @param kernel 抖动内核
	 * @return 抖动完成后的图像 BufferedImage(TYPE_3BYTE_BGR)，尺寸=targetWidth × targetHeight
	 */
	public static BufferedImage dither(BufferedImage src, int targetWidth, int targetHeight, PaletteMode paletteMode, DitherKernel kernel) {
		return dither(src, targetWidth, targetHeight, paletteMode, kernel, null);
	}

	/**
	 * 高性能定点误差扩散抖动，缩放+抖动，同时输出硬件索引字节数组
	 * @param src 原图
	 * @param targetWidth 目标宽
	 * @param targetHeight 目标高
	 * @param paletteMode 调色板
	 * @param kernel 抖动核
	 * @param outHardwareIndexBytes 输出索引数组，长度必须 >= targetWidth * targetHeight，可以传null
	 * @return 抖动后图片 targetWidth × targetHeight
	 */
	public static BufferedImage dither(BufferedImage src, int targetWidth, int targetHeight, PaletteMode paletteMode, DitherKernel kernel,  byte[] outHardwareIndexBytes) {
		BufferedImage scaled = scaleImage(src, targetWidth, targetHeight);
		return doDitherCore(scaled, paletteMode, kernel, outHardwareIndexBytes);
	}

	/**
	 * 重载：原图尺寸抖动，同时输出硬件索引字节数组
	 * @param src 输入原图
	 * @param paletteMode 调色板模式
	 * @param kernel 抖动内核
	 * @param outHardwareIndexBytes 预先分配好的数组，长度>=width*height，方法内部填充，可以传null
	 * @return 抖动后BufferedImage
	 */
	public static BufferedImage dither(BufferedImage src, PaletteMode paletteMode, DitherKernel kernel, byte[] outHardwareIndexBytes) {
		return dither(src, src.getWidth(), src.getHeight(), paletteMode, kernel, outHardwareIndexBytes);
	}

	/**
	 * 核心抖动逻辑，私有抽取
	 */
	private static BufferedImage doDitherCore(BufferedImage src, PaletteMode paletteMode, DitherKernel kernel, byte[] outHardwareIndexBytes) {
		int width = src.getWidth();
		int height = src.getHeight();
		List<Color> palette = paletteMode.getPalette();
		PaletteCache pc = new PaletteCache(palette);

		// 定点缓冲区 short[y][x][R/G/B]；放大系数16，存放 原值*16+误差
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

		// 输出索引缓存
		int[][] indexOut = new int[height][width];

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				// 还原真实0-255值 >>4
				int r = buf[y][x][0] >> 4;
				int g = buf[y][x][1] >> 4;
				int b = buf[y][x][2] >> 4;

				int nearestIdx = findNearestIndex(pc, r, g, b);
				int nr = pc.rArr[nearestIdx];
				int ng = pc.gArr[nearestIdx];
				int nb = pc.bArr[nearestIdx];

				indexOut[y][x] = nearestIdx;

				// 量化误差（定点空间）
				int errR = buf[y][x][0] - (nr << 4);
				int errG = buf[y][x][1] - (ng << 4);
				int errB = buf[y][x][2] - (nb << 4);

				buf[y][x][0] = (short) (nr << 4);
				buf[y][x][1] = (short) (ng << 4);
				buf[y][x][2] = (short) (nb << 4);

				// 误差扩散
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

		// 填充外部传入的硬件索引byte数组
		if (outHardwareIndexBytes != null && outHardwareIndexBytes.length >= width * height) {
			int pos = 0;
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					outHardwareIndexBytes[pos++] = (byte) indexOut[y][x];
				}
			}
		}

		// 生成预览输出图
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
	 * 高质量图像缩放，价签预处理；输出TYPE_3BYTE_BGR
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

	private static short clampShort(short val, int min, int max) {
		if (val < min) {
			return (short) min;
		}
		if (val > max) {
			return (short) max;
		}
		return val;
	}

}
