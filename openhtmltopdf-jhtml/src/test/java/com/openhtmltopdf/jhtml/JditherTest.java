package com.openhtmltopdf.jhtml;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.openhtmltopdf.jhtml.swing.dither.strategy.SimpleDitherStrategy;
import com.openhtmltopdf.jhtml.swing.dither.strategy.SimpleDitherStrategy.ColorMode;
import com.openhtmltopdf.jhtml.swing.dither.strategy.SimpleDitherStrategy.DitherKernel;

import cn.alotus.core.io.FileUtil;

public class JditherTest {

	public static void main(String[] args) throws IOException {
		
		
		File file = new File("D:\\test\\333.png");

		ColorMode colorMode = ColorMode.BWRY;

		DitherKernel ditherKernel = DitherKernel.SHIAU_FAN;
		

		String prefix = FileUtil.getPrefix(file);
        String suffix = FileUtil.getSuffix(file);
        String newName = prefix + "_" + colorMode.name() + "_" + ditherKernel.name() + "." + suffix;
        File newFile = new File(file.getParent(), newName);
		
		float useGamma = 1;

		BufferedImage newImgs = ImageIO.read(file);

		BufferedImage newImg3 = SimpleDitherStrategy.Builder.create().src(newImgs).colorMode(colorMode).kernel(ditherKernel).gamma(useGamma).dither();

		ImageIO.write(newImg3, "png", newFile);

		System.out.println(newFile.getAbsolutePath());
		
	}
}
