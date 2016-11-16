package org.ensembl.gti.genesearch.services.converter;

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
		if (Map.class.isAssignableFrom(clazz)) {
			Map<String, Object> dataMap = (Map<String, Object>) data;
			writeMap(name, dataMap);
		} else if (Collection.class.isAssignableFrom(clazz)) {
			Collection<Object> dataCollection = (Collection<Object>) data;
			writeCollection(name, dataCollection);
		} else {
			if (data != null)
				writer.writeAttribute(name, String.valueOf(data));
		}
	}

	public void writeCollection(String name, Collection<Object> dataCollection) throws XMLStreamException {
		if (dataCollection != null && !dataCollection.isEmpty()) {
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
