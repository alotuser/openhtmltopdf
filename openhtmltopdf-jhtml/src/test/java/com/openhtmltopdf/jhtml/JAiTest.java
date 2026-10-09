package com.openhtmltopdf.jhtml;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

import com.openhtmltopdf.extend.SVGDrawer;
import com.openhtmltopdf.jhtml.config.BuilderConfig;
import com.openhtmltopdf.jhtml.processor.JhtmlJsoupProcessor;
import com.openhtmltopdf.mathmlsupport.MathMLDrawer;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;

import cn.alotus.core.img.ImgUtil;
import cn.alotus.core.io.resource.ResourceUtil;

public class JAiTest {

	
	
	public static void main(String[] args) throws IOException {
		
		
		
		
		
		
		String resHtml="box-shadow.html";
		String html = ResourceUtil.readUtf8Str(resHtml);
		URL fonts= ResourceUtil.getResource("fonts");
		
		JhtmlKit htmlRender = JhtmlKit.create(BufferedImage.TYPE_INT_RGB);
		htmlRender.addFontDirectory(fonts.getPath());
		htmlRender.setPageWidth(1800f);
		htmlRender.setPageHeight(300f);
		htmlRender.setScale(1f);
		htmlRender.setLoggingEnabled(true);
		
		
		
		
		BufferedImage image = htmlRender.toImage(html, BuilderConfig.WITH_BASE, builder->{
			SVGDrawer svg = new BatikSVGDrawer();
			SVGDrawer mathMl = new MathMLDrawer();

			builder.useSVGDrawer(svg);
			builder.useMathMLDrawer(mathMl);
	       
		});
       
       
       
		ImgUtil.write(image, new File("D://AI//"+resHtml+".png"));
		
		

		System.out.println("log:"+htmlRender.getLogString());
		
		
		
		
		
		
		
       
       
       
	}
}
