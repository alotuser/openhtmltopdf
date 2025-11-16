package com.openhtmltopdf.jhtml;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

import javax.imageio.ImageIO;

import com.openhtmltopdf.jhtml.processor.AsJsoupProcessor;
import com.openhtmltopdf.jhtml.util.ImageCropUtil;

import cn.alotus.core.io.resource.ResourceUtil;

public class JhtmlTest {

	public static void main(String[] args) throws IOException {
		String resHtml="2.html";
		String html = ResourceUtil.readUtf8Str(resHtml);
		URL fonts= ResourceUtil.getResource("fonts");
		
		JhtmlRender htmlRender = JhtmlRender.create(BufferedImage.TYPE_INT_RGB);
		htmlRender.addFontDirectory(fonts.getPath());
		htmlRender.setPageWidth(400f);
		htmlRender.setPageHeight(300f);
		htmlRender.setScale(1f);
		htmlRender.setLoggingEnabled(true);
		
		AsJsoupProcessor ajp= htmlRender.useJsoup();
		
		// htmlRender.toImage(html, BuilderConfig.WITH_CUSTOM);

//		htmlRender.toImage(html, builder->{
//			 builder.useFont(new File("myfont"), "myfont");
//		});
//		
		htmlRender.toPng(html, "D://"+resHtml+".png");

		
		String className="original-price";

		Map<org.jsoup.nodes.Element, Rectangle> mers = ajp.getElementById("pname");
 
		System.out.println(mers);

		Rectangle f = mers.values().stream().findFirst().get();

		BufferedImage original = ImageIO.read(new File("D:\\"+resHtml+".png"));

		Rectangle rect = new Rectangle(f.x, f.y, f.width, f.height);

		BufferedImage cropped = ImageCropUtil.cropImage(original, rect);
		ImageIO.write(cropped, "png", new File("D:\\"+resHtml+"-cropped.png"));

	}

}
