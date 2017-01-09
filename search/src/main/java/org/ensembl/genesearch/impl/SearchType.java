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

package org.ensembl.genesearch.impl;

import org.ensembl.genesearch.Search;

/**
 * Enum of target types currently supported by search
 * 
 * @author dstaines
 *
 */
public enum SearchType {

	GENOMES("genomes", "genome"), GENES("genes", "gene"), TRANSCRIPTS("transcripts", "transcript"), TRANSLATIONS(
			"translations",
			"translation"), HOMOLOGUES("homologues", "homologue"), VARIANTS("variants", "variant"), SEQUENCES(
					"sequences", "sequence"), EXPRESSION("expression", "expression"), EXPRESSION_ANALYTICS("analytics",
							"analytics"), EXPRESSION_EXPERIMENTS("experiments", "experiment");

	private final String pluralName;
	private final String singleName;
	private final String idField;

	private SearchType(String pluralName, String singleName) {
		this(pluralName, singleName, Search.ID);
	}

	private SearchType(String pluralName, String singleName, String idField) {
		this.pluralName = pluralName;
		this.singleName = singleName;
		this.idField = idField;
	}

	public static SearchType findByName(String name) {
		SearchType type = null;
		for (SearchType t : SearchType.values()) {
			if (t.name().equalsIgnoreCase(name)) {
				type = t;
				break;
			}
		}
		return type;
	}

	@Override
	public String toString() {
		return this.pluralName;
	}

	public String getObjectName() {
		return this.singleName;
	}

	public String getId() {
		return this.idField;
	}

}
