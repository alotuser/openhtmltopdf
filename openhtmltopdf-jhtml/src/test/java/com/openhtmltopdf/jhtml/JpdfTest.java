package com.openhtmltopdf.jhtml;

import java.io.IOException;
import java.net.URL;

import cn.alotus.core.io.resource.ResourceUtil;

public class JpdfTest {

	public static void main(String[] args) throws IOException {
		String resHtml="7.html";
		String html = ResourceUtil.readUtf8Str(resHtml);
		URL fonts= ResourceUtil.getResource("fonts");
		 
		
		JhtmlKit htmlRender = JhtmlKit.create();
		htmlRender.addFontDirectory(fonts.getPath());
		htmlRender.setLoggingEnabled(true);
		
		
		// htmlRender.toImage(html, BuilderConfig.WITH_CUSTOM);

//		htmlRender.toImage(html, builder->{
//			 builder.useFont(new File("myfont"), "myfont");
//		});
//		
		
		htmlRender.toPdf(html, "D://"+resHtml+".pdf");
		
		 

		System.out.println("log:"+htmlRender.getLogString());
		 

 

	}
	
}
