/*
 *  See the NOTICE file distributed with this work for additional information
 *  regarding copyright ownership.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.ensembl.gti.genesearch.rest;

import static org.junit.Assert.assertEquals;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapXmlWriterTest {

	private static Logger log = LoggerFactory.getLogger(MapXmlWriterTest.class);

	@Test
	public void testSimple() throws XMLStreamException, FactoryConfigurationError, UnsupportedEncodingException {
		Map<String, Object> map = new HashMap<>();
		map.put("1", "one");
		map.put("2", "two");
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		XMLStreamWriter xsw = XMLOutputFactory.newInstance().createXMLStreamWriter(os);
		MapXmlWriter writer = new MapXmlWriter(xsw);
		writer.writeObject("test", map);
		String xml = new String(os.toByteArray(), "UTF-8");
		log.debug("xml" + os.toString());
		assertEquals("Correct XML", "<test 1=\"one\" 2=\"two\"></test>", xml);
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
		log.debug("outputStream:" + os.toString());
		XMLStreamWriter xsw = XMLOutputFactory.newInstance().createXMLStreamWriter(os);
		MapXmlWriter writer = new MapXmlWriter(xsw);
		writer.writeObject("test", map);
		String xml = new String(os.toByteArray(), "UTF-8");
		log.debug(xml);
		assertEquals("Correct XML",
				"<test 1=\"one\" 2=\"two\" 5=\"five\"><test2 3=\"three\" 4=\"four\"></test2></test>", xml);
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
		String xml = new String(os.toByteArray(), "UTF-8");
		log.debug(xml);
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
		String xml = new String(os.toByteArray(), "UTF-8");
		log.debug(xml);
		assertEquals("Correct XML",
				"<test 1=\"one\" 2=\"two\"><test2><test2_elem>three</test2_elem><test2_elem>four</test2_elem></test2></test>",
				xml);
	}

}
