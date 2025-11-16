package com.openhtmltopdf.jhtml.processor;

import java.awt.Rectangle;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.text.html.HTML;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Document.OutputSettings.Syntax;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.openhtmltopdf.jhtml.renderer.AsRenderer;
import com.openhtmltopdf.render.BlockBox;
import com.openhtmltopdf.render.Box;

/**
 * An AsProcessor implementation that uses Jsoup to parse and manipulate HTML, adding unique IDs to elements and allowing for element retrieval based on various criteria.
 */
public class AsJsoupProcessor implements AsProcessor {

	private AsRenderer asRenderer;
	private Document _doc;

	private Document processHtmlWithIds(String html, String idAttribute) {
		Document doc = Jsoup.parse(html);
		doc.outputSettings().syntax(Syntax.xml);
		doc.outputSettings().prettyPrint(false);

		AtomicInteger counter = new AtomicInteger(1);

		doc.traverse(new org.jsoup.select.NodeVisitor() {
			@Override
			public void head(org.jsoup.nodes.Node node, int depth) {
				if (node instanceof Element) {
					Element element = (Element) node;
					if (!element.tagName().equals("#root")) {
						element.attr(idAttribute, "id-" + counter.getAndIncrement());
					}
				}
			}

			@Override
			public void tail(org.jsoup.nodes.Node node, int depth) {
				// 不需要实现
			}
		});

		return doc;
	}

	@Override
	public String asHtml(String html) {

		_doc = processHtmlWithIds(html, BlockBox.JHTML_BOX_ID);

		return _doc.html();
	}

	@Override
	public void asRenderer(AsRenderer asRenderer) {

		this.asRenderer = asRenderer;

	}

	/**
	 * Find elements by ID and return their content area rectangles.
	 *
	 * Requires that a previous render has initialized the internal AsRenderer.
	 *
	 * @param id The ID of the element to find.
	 * @return A map of DOM Element to its content Rectangle
	 */
	public Map<Element, Rectangle> getElementById(String id) {
		Element element = _doc.getElementById(id);
		if (element == null)
			return null;
		return new java.util.HashMap<Element, Rectangle>() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			{
				put(element, getContentAreaEdge(element));
			}
		};
	}

	/**
	 * Find elements by name attribute and return their content area rectangles.
	 *
	 * @param name The value of the name attribute to match.
	 * @return Map of Element to Rectangle for matched elements
	 */
	public Map<Element, Rectangle> getElementsByName(String name) {
		Map<Element, Rectangle> result = new java.util.HashMap<>();
		Elements elements = _doc.getElementsByAttributeValue(HTML.Attribute.NAME.toString(), name);
		if (elements == null)
			return null;
		elements.forEach(element -> {
			result.put(element, getContentAreaEdge(element));

		});
		return result;
	}

	/**
	 * Find elements by CSS class (class attribute) and return their rectangles.
	 *
	 * @param cssClass CSS class string to match
	 * @return Map of Element to Rectangle for matched elements
	 */
	public Map<Element, Rectangle> getElementsByClass(String cssClass) {
		Map<Element, Rectangle> result = new java.util.HashMap<>();
		Elements elements = _doc.getElementsByClass(cssClass);
		if (elements == null)
			return null;
		elements.forEach(element -> {
			result.put(element, getContentAreaEdge(element));
		});
		return result;
	}

	/**
	 * Find elements by tag name and return their rectangles.
	 *
	 * Matches element tag names (case-sensitive as provided by DOM).
	 *
	 * @param tagName element tag name to match
	 * @return Map of Element to Rectangle for matched elements
	 */
	public Map<Element, Rectangle> getElementsByTag(String tagName) {
		Map<Element, Rectangle> result = new java.util.HashMap<>();
		Elements elements = _doc.getElementsByTag(tagName);
		if (elements == null)
			return null;
		elements.forEach(element -> {
			result.put(element, getContentAreaEdge(element));
		});
		return result;
	}

	/**
	 * Find first element by arbitrary CSS query and return its rectangle.
	 *
	 * @param cssQuery CSS query string to match
	 * @return Map of Element to Rectangle for matched element
	 */
	public Map<Element, Rectangle> selectFirst(String cssQuery) {
		
		Element element = _doc.selectFirst(cssQuery);
		if (element == null)
			return null;
		String jbi = element.attr(BlockBox.JHTML_BOX_ID);
		if (jbi == null)
			return null;
		return new java.util.HashMap<Element, Rectangle>() {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			{
				put(element, getContentAreaEdge(element));
			}
		};
	}

	/**
	 * Find elements by arbitrary CSS query and return their rectangles.
	 *
	 * @param cssQuery CSS query string to match
	 * @return Map of Element to Rectangle for matched elements
	 */
	public Map<Element, Rectangle> select(String cssQuery) {
		
		Map<Element, Rectangle> result = new java.util.HashMap<>();
		Elements elements = _doc.select(cssQuery);
		if (elements == null)
			return null;
		elements.forEach(element -> {
			result.put(element, getContentAreaEdge(element));

		});
		return result;
	}
	
	/**
	 * Find elements by XPath expression and return their rectangles.
	 *
	 * @param xpath XPath expression to match
	 * @return Map of Element to Rectangle for matched elements
	 */
	public Map<Element, Rectangle> selectXpath(String xpath) {
		
		Map<Element, Rectangle> result = new java.util.HashMap<>();
		Elements elements = _doc.selectXpath(xpath);
		if (elements == null)
			return null;
		elements.forEach(element -> {
			result.put(element, getContentAreaEdge(element));
		});
		return result;
	}

	/**
	 * Find first element by arbitrary CSS query and return its rectangle.
	 *
	 * @param cssQuery CSS query string to match
	 * @return Rectangle of matched element
	 */
	public Rectangle findFirst(String cssQuery) {
		Element element = _doc.selectFirst(cssQuery);
		return getContentAreaEdge(element);
	}

	/**
	 * Find element by ID and return its rectangle.
	 *
	 * @param id The ID of the element to find.
	 * @return Rectangle of matched element
	 */
	public Rectangle findById(String id) {
		Element element = _doc.getElementById(id);
		return getContentAreaEdge(element);
	}

	/**
	 * Get content area rectangle for a given Element.
	 *
	 * @param element The Jsoup Element
	 * @return Rectangle of the element's content area
	 */
	public Rectangle getContentAreaEdge(Element element) {
		if (element == null)
			return null;
		String jbi = element.attr(BlockBox.JHTML_BOX_ID);
		if (jbi == null)
			return null;
		return getContentAreaEdge(asRenderer.getRenderingContext().getBoxById(jbi));
	}

	/**
	 * Get content area rectangle for a given Box.
	 *
	 * @param box The Box object
	 * @return Rectangle of the box's content area
	 */
	public Rectangle getContentAreaEdge(Box box) {
		if (box == null)
			return null;
		return asRenderer.getContentAreaEdge(box);
	}

	/**
	 * Get the processed Jsoup Document.
	 *
	 * @return The Jsoup Document
	 */
	public Document getJsoupDoc() {
		return _doc;
	}
	
}
