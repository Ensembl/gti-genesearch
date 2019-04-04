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
package org.ensembl.gti.genesearch.services;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Path("/swagger.json")
@Service
public class SwaggerService {

    final Logger log = LoggerFactory.getLogger(this.getClass());
    final ObjectMapper mapper = new ObjectMapper();
    final TypeReference<Map<String, Object>> ref = new TypeReference<Map<String, Object>>() {
    };
    private final static String[] RES = new String[] { "/health_swagger.json", "/genes_swagger.json",
            "/transcripts_swagger.json", "/genomes_swagger.json", "/variants_swagger.json", "/expression_swagger.json",
            "/cell_lines_swagger.json" };

    @GET
    @Produces("application/json")
    public Response swagger() {
        try {

            Map<String, Object> base = mapper.readValue(this.getClass().getResource("/swagger.json").openStream(), ref);
            Map<String, Object> paths = new HashMap<>();
            for (String pathRes : RES) {
                // resources are endpoints keyed by path
                paths.putAll(mapper.readValue(this.getClass().getResource(pathRes).openStream(), ref));
            }
            base.put("paths", paths);
            return Response.ok().entity(base).type(MediaType.APPLICATION_JSON).build();
        } catch (IOException e) {
            log.error("Could not read swagger JSON file", e);
            return Response.serverError().entity("Could not read API description").build();
        }
    }
}
