package org.bg3.forge;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bg3.forge.agents.ForgeAgent;
import org.bg3.forge.agents.MetadataAgent;
import org.bg3.forge.model.Equipment;
import org.bg3.forge.toolbox.EquipmentDB;
import org.bg3.forge.toolbox.LibraryService;
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
	ItemService itemService;

	@Inject
	MetadataAgent metadataFinderAgent;

	@Inject
	LibraryService library;

	@Inject
	EquipmentDB equipmentDB;

	/**
	 * Executes a natural language query and returns data in JSON format.
	 */
	@GET
	@Path("/json")
	@Produces(MediaType.TEXT_PLAIN)
	public Response queryToJson(@QueryParam("query") String query) throws Exception {

		String commands = forgeAgent.jsonCommands(query);
		LOG.info("Commands: " + commands);
		if (commands.isEmpty()) {
			return Response.ok("[]").build();
		}
		String[] commandArray = commands.split(";");
		String json = "";
		if (commandArray.length > 1) {
			json += "[";

		}
		boolean first = true;
		for (String command : commandArray) {
			if (first) {
				first = false;
			} else {
				json += ",";
			}
			json += executeCommand(command.trim());
		}
		if (commandArray.length > 1) json += "]";
		return Response.ok(json).build();
	}
	ObjectMapper objectMapper = new ObjectMapper();

	static Pattern functionPattern = Pattern.compile("^(\\w+)\\((.*)\\)$");

	private String executeCommand(String command) throws Exception{
		Matcher matcher = functionPattern.matcher(command);
		if (matcher.matches()) {
			
			String function = matcher.group(1);
			String param = matcher.group(2);
			LOG.info("Function: " + function + " Param: " + param);
			if (function.equals("getStatAttributeValues")) {
				String unquotedParam = param.replaceAll("^\"|\"$", "");
				LOG.info("Unquoted param: " + unquotedParam);
				List<String> statAttributeValues = library.getStatAttributeValues(unquotedParam);
				return objectMapper.writeValueAsString(statAttributeValues);
			} else if (function.equals("getAllBoostFunctionSignatures")) {
				return objectMapper.writeValueAsString(library.getAllBoostFunctionSignatures());
			}
		}
		return "";
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
           response = EquipmentModel.toJson(items);
		   response = forgeAgent.list(query, response);

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
