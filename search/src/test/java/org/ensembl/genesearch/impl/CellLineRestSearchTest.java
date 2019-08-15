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
package org.ensembl.genesearch.impl;

import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.QueryOutput;
import org.ensembl.genesearch.QueryResult;
import org.ensembl.genesearch.info.DataTypeInfo;
import org.ensembl.genesearch.info.FieldType;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

public class CellLineRestSearchTest {

	static Logger log = LoggerFactory.getLogger(CellLineRestSearchTest.class);
	static CellLineSearch search;

	@BeforeClass
	public static void setUp() throws IOException {
		search = new CellLineSearch(DataTypeInfo.fromResource("/datatypes/celllines_datatype_info.json"),
				"https://cells.ebisc.org/api/v0/cell-lines", "ebi-allele-query",
				"e126f3b2-8d04-3fb5-3ed7-73445d7ef7cc");
	}

	@Test
	public void testQueryById() {
		QueryResult res = search.query(
				Arrays.asList(new Query(FieldType.TERM, "biosamples_id", "SAMEA104493310")),
				QueryOutput.build("[\"biosamples_id\"]"), Collections.emptyList(), 0, 50, Collections.emptyList());
		Assert.assertEquals("1 result found", 1, res.getResults().size());
	}

}
