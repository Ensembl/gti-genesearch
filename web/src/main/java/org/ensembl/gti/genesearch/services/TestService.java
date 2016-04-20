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

package org.ensembl.gti.genesearch.services;
 
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
 
@Path("/test")
public class TestService {
 
  @GET
  @Path("/string/{name}")
  public String sayHello(@PathParam("name") String name) {
      return "Testing " + name;
  }
  
  @GET
  @Path("/obj/{name}")
  @Produces(MediaType.APPLICATION_JSON)
  public Map<String,Object> sayHelloJson(@PathParam("name") String name) {
	  Map<String,Object> output = new HashMap<>();
	  output.put("name", name);
      return output;
  }

  @GET
  @Path("/echo")
  @Produces(MediaType.APPLICATION_JSON)
  public Map<String,Object> echoJson(@QueryParam("name") String name, @QueryParam("age") int age, @QueryParam("address") String address) {
	  Map<String,Object> output = new HashMap<>();
	  output.put("name", name);
	  output.put("age", age);
	  output.put("address", address);
      return output;
  }

  
}
