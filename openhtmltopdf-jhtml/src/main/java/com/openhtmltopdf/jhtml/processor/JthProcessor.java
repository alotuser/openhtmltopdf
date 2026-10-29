package com.openhtmltopdf.jhtml.processor;

import java.util.Locale;
import java.util.Map;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

public class JthProcessor {
	private final TemplateEngine _thymeleaf = createTemplateEngine();

    private static TemplateEngine createTemplateEngine() {
        TemplateEngine te = new TemplateEngine();

        StringTemplateResolver sr = new StringTemplateResolver();
        sr.setTemplateMode(TemplateMode.HTML);
        sr.setCacheable(false);

        te.setTemplateResolver(sr);
        te.addDialect(new Java8TimeDialect());

        return te;
    }

    public String process(String html, Map<String, Object> args) {
        Context ctx = new Context(Locale.US, args);
        return _thymeleaf.process(html, ctx);
    }
}
