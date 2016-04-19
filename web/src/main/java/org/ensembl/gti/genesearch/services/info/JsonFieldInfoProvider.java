package org.ensembl.gti.genesearch.services.info;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.ensembl.gti.genesearch.services.info.FieldInfo.FieldType;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Provider that reads field information from /field_info.json
 * 
 * @author dstaines
 *
 */
@Component
public class JsonFieldInfoProvider implements FieldInfoProvider {

	private final List<FieldInfo> fields;

	public JsonFieldInfoProvider() throws IOException {
		this(IOUtils.toString(JsonFieldInfoProvider.class.getResource("/field_info.json"), StandardCharsets.UTF_8));
	}

	public JsonFieldInfoProvider(File jsonFile) throws IOException {
		this(FileUtils.readFileToString(jsonFile));
	}

	public JsonFieldInfoProvider(String json) throws JsonParseException, JsonMappingException, IOException {
		this.fields = new ObjectMapper().readValue(json, new TypeReference<List<FieldInfo>>() {
		});
	}

	@Override
	public List<FieldInfo> getAll() {
		return fields;
	}

	@Override
	public List<FieldInfo> getDisplayable() {
		return fields.stream().filter(i -> i.isDisplay()).collect(Collectors.toList());
	}

	@Override
	public List<FieldInfo> getSearchable() {
		return fields.stream().filter(i -> i.isSearch()).collect(Collectors.toList());
	}

	@Override
	public List<FieldInfo> getFacetable() {
		return fields.stream().filter(i -> i.isFacet()).collect(Collectors.toList());
	}

	@Override
	public FieldInfo getByName(String name) {
		List<FieldInfo> fs = fields.stream()
				.filter(i -> i.getName().equalsIgnoreCase(name) || i.getDisplayName().equalsIgnoreCase(name))
				.collect(Collectors.toList());
		if (fs.isEmpty()) {
			return null;
		} else {
			return fs.get(0);
		}
	}

	@Override
	public List<FieldInfo> getByType(FieldType type) {
		return fields.stream().filter(i -> i.getType().equals(type)).collect(Collectors.toList());
	}

}
