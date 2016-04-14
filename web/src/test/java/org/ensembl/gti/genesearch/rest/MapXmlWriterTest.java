package org.ensembl.gti.genesearch.rest;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.ensembl.gti.genesearch.services.converter.MapXmlWriter;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class MapXmlWriterTest {

	@Test
	public void testSimple() throws XMLStreamException, FactoryConfigurationError, UnsupportedEncodingException {
		Map<String, Object> map = new HashMap<>();
		map.put("1", "one");
		map.put("2", "two");
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		XMLStreamWriter xsw = XMLOutputFactory.newInstance().createXMLStreamWriter(os);
		MapXmlWriter writer = new MapXmlWriter(xsw);
		writer.writeObject("test", map);
		String xml = new String(os.toByteArray(),"UTF-8");
		assertEquals("Correct XML", "<test 1=\"one\" 2=\"two\"></test>",  xml);
	}

	@Test
	public void testSimpleMap() throws XMLStreamException, FactoryConfigurationError, UnsupportedEncodingException {
		Map<String, Object> map = new HashMap<>();
		map.put("1", "one");
		map.put("2", "two");
		Map<String, Object> map2 = new HashMap<>();
		map2.put("3", "three");
		map2.put("4", "four");
		map.put("test2", map2);
		map.put("5", "five");
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		XMLStreamWriter xsw = XMLOutputFactory.newInstance().createXMLStreamWriter(os);
		MapXmlWriter writer = new MapXmlWriter(xsw);
		writer.writeObject("test", map);
		String xml = new String(os.toByteArray(),"UTF-8");
		System.out.println(xml);
		assertEquals("Correct XML", "<test 1=\"one\" 2=\"two\" 5=\"five\"><test2 3=\"three\" 4=\"four\"></test2></test>",  xml);
	}

	@Test
	public void testList() throws XMLStreamException, FactoryConfigurationError, UnsupportedEncodingException {
		List<String> list = new ArrayList<>();
		list.add("three");
		list.add("four");
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		XMLStreamWriter xsw = XMLOutputFactory.newInstance().createXMLStreamWriter(os);
		MapXmlWriter writer = new MapXmlWriter(xsw);
		writer.writeObject("tests", list);
		String xml = new String(os.toByteArray(),"UTF-8");
		System.out.println(xml);
		assertEquals("Correct XML", "<tests><test>three</test><test>four</test></tests>", xml);
	}

	
	@Test
	public void testMapList() throws XMLStreamException, FactoryConfigurationError, UnsupportedEncodingException {
		Map<String, Object> map = new HashMap<>();
		map.put("1", "one");
		map.put("2", "two");
		List<String> list = new ArrayList<>();
		list.add("three");
		list.add("four");
		map.put("test2", list);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		XMLStreamWriter xsw = XMLOutputFactory.newInstance().createXMLStreamWriter(os);
		MapXmlWriter writer = new MapXmlWriter(xsw);
		writer.writeObject("test", map);
		String xml = new String(os.toByteArray(),"UTF-8");
		System.out.println(xml);
		assertEquals("Correct XML", "<test 1=\"one\" 2=\"two\"><test2><test2_elem>three</test2_elem><test2_elem>four</test2_elem></test2></test>",  xml);
	}
 	
}
