package org.bg3.forge;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bg3.forge.agents.ForgeAgent;
import org.bg3.forge.agents.MetadataAgent;
import org.bg3.forge.command.DataCommandService;
import org.bg3.forge.model.Equipment;
import org.bg3.forge.toolbox.EquipmentDB;
import org.bg3.forge.toolbox.LibraryService;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
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
	ItemService itemService;

	@Inject
	MetadataAgent metadataFinderAgent;

	@Inject
	LibraryService library;

	@Inject
	EquipmentDB equipmentDB;

	@Inject
	DataCommandService assistantCommandService;

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
		List<Equipment> items = equipmentDB.query(query);

        String response = "";
        if (items.isEmpty()) {
            response = "I couldn't find any items that match your query.";
        } else {
			/*
           response = EquipmentModel.toJson(items);
		   response = forgeAgent.list(query, response);
		   */
		  

        }
		return Response.ok(response).build();
	}

	record EquipmentModel(String name,String type, String slot, String rarity, String boostDescription, String description) {
		public static String toJson(List<Equipment> equipments) throws Exception {
			return new ObjectMapper().writeValueAsString(equipments.stream().map(EquipmentModel::fromEquipment).toList());
		}
		public static EquipmentModel fromEquipment(Equipment equipment) {
			return new EquipmentModel(
				equipment.name(),
				equipment.type().name(),
				equipment.slot().name(),
				equipment.rarity().name(),
				equipment.boostDescription(),
				equipment.description()
			);
		}
	}



}
