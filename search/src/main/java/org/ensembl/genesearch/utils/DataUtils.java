/*
 * Copyright [1999-2016] EMBL-European Bioinformatics Institute
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ensembl.genesearch.utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utilities for manipulating data objects
 * 
 * @author dstaines
 *
 */
public class DataUtils {

	/**
	 * Find all data for a given object
	 * 
	 * @param r
	 * @param key
	 * @return
	 */
	public static Map<String,Object> getObjsForKey(Map<String, Object> r, String key) {
		Map<String,Object> keys = new HashMap<>();
		getObjsForKey(r, key, keys);
		return keys;
	}
	protected static void getObjsForKey(Map<String, Object> r, String key, Map<String,Object> keys) {
		int i = key.indexOf('.');
		if (i != -1) {
			String stem = key.substring(0, i);
			String tail = key.substring(i + 1);
			Object subObj = r.get(stem);
			if(subObj!=null) {
				if(Map.class.isAssignableFrom(subObj.getClass())) {
					getObjsForKey((Map)subObj, tail, keys);					
				} else if(List.class.isAssignableFrom(subObj.getClass())) {
					for(Object o: (List)subObj) {
						getObjsForKey((Map)o, tail, keys);
					}
				} else {
					throw new IllegalArgumentException("Cannot find map for key "+stem);
				}
			}
		} else {
			String k = String.valueOf(r.get(key));
			keys.put(k, r);
		}
	}

	public static Map<String, Object> jsonToMap(String json) {
		try {
			return new ObjectMapper().readValue(json, new TypeReference<Map<String, Object>>() {
			});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
