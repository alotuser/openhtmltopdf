package com.openhtmltopdf.jhtml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.openhtmltopdf.jhtml.processor.JthProcessor;

import cn.alotus.core.io.resource.ResourceUtil;

public class JthTest {

	public static Map<String, Object> build() {

		Map<String, Object> menu = new LinkedHashMap<>();

		// ================= company =================
		Map<String, Object> address = new LinkedHashMap<>();
		address.put("line1", "1 Victoria Street");
		address.put("line2", "Richmond");
		address.put("city", "Melbourne");

		Map<String, Object> company = new LinkedHashMap<>();
		company.put("name", "My Place");
		company.put("address", address);
		company.put("email", "myplace@example.com");
		menu.put("company", company);

		// ================= sections =================
		List<Map<String, Object>> sections = new ArrayList<>();

		// ---- Breakfast ----
		Map<String, Object> breakfast = new LinkedHashMap<>();
		breakfast.put("title", "Breakfast");
		List<Map<String, Object>> breakfastItems = new ArrayList<>();

		Map<String, Object> it = new LinkedHashMap<>();
		it.put("title", "Toast with jam");
		it.put("description", "Ask about our delicious array of fruit jams");
		it.put("price", "$10.00");
		breakfastItems.add(it);

		it = new LinkedHashMap<>();
		it.put("title", "Vegemite on sourdough");
		it.put("description", "Who can resist our vegemite laden bread");
		it.put("price", "$9.20");
		breakfastItems.add(it);

		it = new LinkedHashMap<>();
		it.put("title", "Eggs your way");
		it.put("description", "Choose from fried, poached or scrambled");
		it.put("price", "$15.00");
		breakfastItems.add(it);

		it = new LinkedHashMap<>();
		it.put("title", "Porridge");
		it.put("description", "Simply irresistible with berries");
		it.put("price", "$5.80");
		breakfastItems.add(it);

		it = new LinkedHashMap<>();
		it.put("title", "Big breakfast");
		it.put("description", "Sausages, eggs, toast and mushroom goodness");
		it.put("price", "$18.00");
		breakfastItems.add(it);

		it = new LinkedHashMap<>();
		it.put("title", "Pancakes");
		it.put("description", "Served with ice-cream on top");
		it.put("price", "$14.00");
		breakfastItems.add(it);

		it = new LinkedHashMap<>();
		it.put("title", "French toast");
		it.put("description", "With the very best maple syrup");
		it.put("price", "$12.00");
		breakfastItems.add(it);

		breakfast.put("items", breakfastItems);
		sections.add(breakfast);

		// ---- Lunch ----
		Map<String, Object> lunch = new LinkedHashMap<>();
		lunch.put("title", "Lunch");
		List<Map<String, Object>> lunchItems = new ArrayList<>();

		it = new LinkedHashMap<>();
		it.put("title", "Chicken burger");
		it.put("description", "Yummy chicken piece on a bun");
		it.put("price", "$19.00");
		lunchItems.add(it);

		it = new LinkedHashMap<>();
		it.put("title", "Sandwich");
		it.put("description", "Choice of fillings including cucumber");
		it.put("price", "$3.99");
		lunchItems.add(it);

		it = new LinkedHashMap<>();
		it.put("title", "Vegetarian curry");
		it.put("description", "Not too hot, not too mild");
		it.put("price", "$15.00");
		lunchItems.add(it);

		it = new LinkedHashMap<>();
		it.put("title", "Chips");
		it.put("description", "Get healthy with our sweet-potato fries");
		it.put("price", "$9.00");
		lunchItems.add(it);

		lunch.put("items", lunchItems);
		sections.add(lunch);

		menu.put("sections", sections);
		return Collections.singletonMap("menu", menu);

	}

	public static void main(String[] args) throws IOException {
		JthProcessor processor = new JthProcessor();
		String tempHtml = ResourceUtil.readUtf8Str("th/cafe-menu.html");
		String html = processor.process(tempHtml, build());

		System.out.println(html);

	}
}
