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
package org.ensembl.gti.genesearch.services.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Utility class to write maps as XML
 * 
 * @author dstaines
 *
 */
public class MapXmlWriter {

	final Logger log = LoggerFactory.getLogger(MapXmlWriter.class);

	/**
	 * Transform a map into XML
	 * @param name
	 * @param map
	 * @return String in XML format
	 * @throws XMLStreamException
	 * @throws FactoryConfigurationError
	 * @throws UnsupportedEncodingException
	 */
	public static String mapToXml(String name, Map<String,Object> map) throws XMLStreamException, FactoryConfigurationError, UnsupportedEncodingException {
		ByteArrayOutputStream os = new ByteArrayOutputStream(); 
		XMLStreamWriter xsw = XMLOutputFactory.newInstance().createXMLStreamWriter(os);
		MapXmlWriter writer = new MapXmlWriter(xsw);
		xsw.writeStartDocument();
		writer.writeMap(name, map);
		xsw.writeEndDocument();
		xsw.flush();
		xsw.close();
		return os.toString("UTF-8");
	}

	private final XMLStreamWriter writer;

	public MapXmlWriter(XMLStreamWriter writer) {
		this.writer = writer;
	}

	@SuppressWarnings("unchecked")
	public void writeObject(String name, Object data) throws XMLStreamException {
		Class<?> clazz = data.getClass();
		log.debug("Data received:" + data.toString());
		if (Map.class.isAssignableFrom(clazz)) {
			Map<String, Object> dataMap = (Map<String, Object>) data;
			writeMap(name, dataMap);
		} else if (Collection.class.isAssignableFrom(clazz)) {
			Collection<Object> dataCollection = (Collection<Object>) data;
			writeCollection(name, dataCollection);
		} else {
			log.debug("Default behavior ");
			if (data != null) {
				writer.writeAttribute(name, String.valueOf(data));
			}
		}
	}

	public void writeCollection(String name, Collection<Object> dataCollection) throws XMLStreamException {
		if (dataCollection != null && !dataCollection.isEmpty()) {
			log.debug("Write Collection");
			String elemName = null;
			if (name.endsWith("s")) {
				elemName = name.substring(0, name.length() - 1);
			} else {
				elemName = name + "_elem";
			}
			writer.writeStartElement(name);
			for (Object obj : dataCollection) {
				Class<?> oClazz = obj.getClass();
				if (Map.class.isAssignableFrom(oClazz) || List.class.isAssignableFrom(oClazz)) {
					writeObject(elemName, obj);
				} else {
					writer.writeStartElement(elemName);
					writer.writeCharacters(String.valueOf(obj));
					writer.writeEndElement();
				}
			}
			writer.writeEndElement();
		}
	}

	public void writeMap(String name, Map<String, Object> dataMap) throws XMLStreamException {
		if (dataMap != null && !dataMap.isEmpty()) {
			log.debug("write Map ");
			writer.writeStartElement(name);
			// write attributes first
			for (Entry<String, Object> e : dataMap.entrySet()) {
				if (e.getValue() != null) {
					Class<?> eClazz = e.getValue().getClass();
					if (!Map.class.isAssignableFrom(eClazz) && !Collection.class.isAssignableFrom(eClazz)) {
						writer.writeAttribute(e.getKey(), String.valueOf(e.getValue()));
					}
				}
			}
			// write child elements last
			for (Entry<String, Object> e : dataMap.entrySet()) {
				if (e.getValue() != null) {
					Class<?> eClazz = e.getValue().getClass();
					if (Map.class.isAssignableFrom(eClazz) || Collection.class.isAssignableFrom(eClazz)) {
						writeObject(e.getKey(), e.getValue());
					}
				}
			}
			writer.writeEndElement();
		}
	}

}
