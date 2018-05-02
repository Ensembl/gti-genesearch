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

package org.ensembl.genesearch.query;

import java.util.List;
import java.util.Map;

import org.ensembl.genesearch.Query;

/**
 * Interface to transform supplied JSON string or nested map structure into a
 * list of {@link Query} objects.
 * 
 * @author dstaines
 *
 */
public interface QueryHandler {

    public List<Query> parseQuery(String json);

    public List<Query> parseQuery(Map<String, Object> query);

}
