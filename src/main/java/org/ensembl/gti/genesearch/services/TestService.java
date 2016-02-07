package org.ensembl.gti.genesearch.services;
 
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
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

  
}
