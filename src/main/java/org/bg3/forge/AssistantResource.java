package org.bg3.forge;

import org.bg3.forge.agents.ForgeAgent;
import org.bg3.forge.agents.MetadataFinderAgent;
import org.bg3.forge.model.EquipmentFilter;
import org.bg3.forge.model.EquipmentFilters;
import org.bg3.forge.toolbox.Bg3DB;
import org.hibernate.Session;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Path("/assistant")
public class AssistantResource {
	private static final Logger LOG = Logger.getLogger( AssistantResource.class);

	@Inject
    ForgeAgent forgeAgent;


	@Inject
	ItemService itemService;

	@Inject
	MetadataFinderAgent metadataFinderAgent;

	@Inject
	Bg3DB bg3DB;

	/**
	 * Executes a natural language query and returns data in JSON format.
	 */
	@GET
	@Path("/json")
	@Produces(MediaType.TEXT_PLAIN)
	public Response queryToJson(@QueryParam("query") String query) throws Exception {
		/* 
		String json = EquipmentFilters.toJson(metadataFinderAgent.answer(query));
		LOG.info("Equipment filters: " + json);
		return Response.ok(json).build();
		*/

		/* 
		List<String> boosts = bg3DB.getAllBoostFunctionSignatures();
		ObjectMapper objectMapper = new ObjectMapper();
		String json = objectMapper.writeValueAsString(boosts);
		*/
		//LOG.info("Boosts: " + json);

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
				List<String> statAttributeValues = bg3DB.getStatAttributeValues(unquotedParam);
				return objectMapper.writeValueAsString(statAttributeValues);
			} else if (function.equals("getAllBoostFunctionSignatures")) {
				return objectMapper.writeValueAsString(bg3DB.getAllBoostFunctionSignatures());
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
