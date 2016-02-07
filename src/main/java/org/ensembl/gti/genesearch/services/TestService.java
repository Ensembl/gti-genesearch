package org.ensembl.gti.genesearch.services;
 
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
 
@Path("/test")
public class TestService {
 
  @GET
  @Path("/{name}")
  public String sayHello(@PathParam("name") String name) {
      return "Testing " + name;
  }
}
