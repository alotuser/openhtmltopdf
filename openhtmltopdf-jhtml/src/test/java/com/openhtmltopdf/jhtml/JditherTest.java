package com.openhtmltopdf.jhtml;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.openhtmltopdf.jhtml.swing.dither.strategy.SimpleDitherStrategy;
import com.openhtmltopdf.jhtml.swing.dither.strategy.SimpleDitherStrategy.ColorMode;
import com.openhtmltopdf.jhtml.swing.dither.strategy.SimpleDitherStrategy.DitherKernel;

public class JditherTest {

	
	public static void main(String[] args) throws IOException {
		ColorMode colorMode = ColorMode.BWRY;
		DitherKernel ditherKernel = DitherKernel.SIERRA_LITE;
		float useGamma = 1;
		 
		BufferedImage newImgs=ImageIO.read(new File("D:\\777.png"));

		BufferedImage newImg3=SimpleDitherStrategy.Builder.create().src(newImgs).targetSize(800, 480).colorMode(colorMode).kernel(ditherKernel).gamma(useGamma).dither();
		
		
		ImageIO.write(newImg3, "png", new File("D:\\777.html-dither.png"));
	}
}
