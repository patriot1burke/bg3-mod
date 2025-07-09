package org.baldurs.forge;

import java.util.List;

import org.baldurs.forge.agents.ForgeAgent;
import org.baldurs.forge.agents.MetadataAgent;
import org.baldurs.forge.model.Equipment;
import org.baldurs.forge.model.EquipmentModel;
import org.baldurs.forge.nli.ToolBoxNLI;
import org.baldurs.forge.nli.ToolBoxNLIInvoker;
import org.baldurs.forge.toolbox.BoostService;
import org.baldurs.forge.toolbox.EquipmentDB;
import org.baldurs.forge.toolbox.LibraryService;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/assistant")
public class AssistantResource {
	private static final Logger LOG = Logger.getLogger( AssistantResource.class);

	@Inject
    ForgeAgent forgeAgent;


	@Inject
	MetadataAgent metadataFinderAgent;

	@Inject
	LibraryService library;

	@Inject
	EquipmentDB equipmentDB;

	@Inject
	@ToolBoxNLI({EquipmentDB.class, LibraryService.class, BoostService.class})
	ToolBoxNLIInvoker assistantCommandService;

	/**
	 * Executes a natural language query and returns data in JSON format.
	 */
	@GET
	@Path("/json")
	@Produces(MediaType.TEXT_PLAIN)
	public String queryToJson(@QueryParam("query") String query) throws Exception {
        LOG.info("QUERY: " + query);
		return assistantCommandService.execute(query);
	}


	/**
	 * Executes a natural language query and feeds back data into the LLM to provide a natural language response.
	 */
	@GET
	@Path("/ask")
	@Produces(MediaType.TEXT_PLAIN)
	public Response naturalLanguage(@QueryParam("query") String query) throws Exception {
		List<EquipmentModel> items = equipmentDB.search(query);

        String response = "";
        if (items.isEmpty()) {
            response = "I couldn't find any items that match your query.";
        } else {
           response = forgeAgent.queryEquipment(query, EquipmentModel.toJson(items));
		   LOG.info("RESPONSE:\n " + response + "\n");
        }
		return Response.ok(response).build();
	}

}
