package org.ensembl.gti.genesearch.services.info;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Provider that reads field information from /field_info.json
 * 
 * @author dstaines
 *
 */
@Component
public class JsonDataTypeInfoProvider implements DataTypeInfoProvider {

	private final List<DataTypeInfo> dataTypes;
	private List<String> dataNames;

	public JsonDataTypeInfoProvider() throws IOException {
		this(IOUtils.toString(JsonDataTypeInfoProvider.class.getResource("/datatype_info.json"),
				StandardCharsets.UTF_8));
	}

	public JsonDataTypeInfoProvider(String json) throws IOException {
		this.dataTypes = new ObjectMapper().readValue(json, new TypeReference<List<DataTypeInfo>>() {
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ensembl.gti.genesearch.services.info.DataTypeInfoProvider#getAllNames
	 * ()
	 */
	@Override
	public List<String> getAllNames() {
		if (dataNames == null) {
			dataNames = dataTypes.stream().map(d -> d.getName()).collect(Collectors.toList());
		}
		return dataNames;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ensembl.gti.genesearch.services.info.DataTypeInfoProvider#getAll()
	 */
	@Override
	public List<DataTypeInfo> getAll() {
		return dataTypes;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ensembl.gti.genesearch.services.info.DataTypeInfoProvider#getByName(
	 * java.lang.String)
	 */
	@Override
	public DataTypeInfo getByName(String name) {
		return getAll().stream().filter(i -> name.equals(i.getName())).findFirst().orElse(null);
	}

}
