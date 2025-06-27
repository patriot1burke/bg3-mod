package org.bg3.forge;

import org.bg3.forge.agents.ForgeAgent;
import org.bg3.forge.agents.MetadataFinderAgent;
import org.bg3.forge.model.MetadataFilter;
import org.hibernate.Session;

import org.jboss.logging.Logger;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Objects;

@Path("/assistant")
public class AssistantResource {
	private static final Logger LOG = Logger.getLogger( AssistantResource.class);

	@Inject
    ForgeAgent forgeAgent;

	@Inject
	ItemService itemService;

	@Inject
	MetadataFinderAgent metadataFinderAgent;

	/**
	 * Executes a natural language query and returns data in JSON format.
	 */
	@GET
	@Path("/json")
	@Produces(MediaType.TEXT_PLAIN)
	public Response queryToJson(@QueryParam("query") String query) {
		String json = metadataFinderAgent.answer(query);
		List<MetadataFilter> metadataFilters = MetadataFilter.fromJson(json);
		json = MetadataFilter.toJson(metadataFilters);
		return Response.ok(json).build();
	}

	/**
	 * Executes a natural language query and feeds back data into the LLM to provide a natural language response.
	 */
	@GET
	@Path("/ask")
	@Produces(MediaType.TEXT_PLAIN)
	public Response naturalLanguage(@QueryParam("query") String query) {
		List<Item> items = itemService.query(query);
        String response = "";
        if (items.isEmpty()) {
            response = "I couldn't find any items that match your query.";
        } else {
            List<ForgeItem> forgeItems = items.stream().map(ForgeItem::toForgeItem).toList();
            String json = ForgeItem.toJson(forgeItems);
            response = forgeAgent.answer(query, json);
        }
		return Response.ok(response).build();
	}



}
