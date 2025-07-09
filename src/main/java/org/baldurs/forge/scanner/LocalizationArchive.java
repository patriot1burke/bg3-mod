package org.baldurs.forge.scanner;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;

import io.quarkus.logging.Log;

public class LocalizationArchive {
    public static NodeList getLocalizationFromXml(Path xmlPath) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(xmlPath.toFile());
        
        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xPath = xPathFactory.newXPath();
        
        String xpathExpression = "//contentList/content";
        return (NodeList) xPath.evaluate(xpathExpression, document, XPathConstants.NODESET);
    }
    
    public static List<Element> getLocalizationAsElements(Path xmlPath) throws Exception {
        NodeList nodeList = getLocalizationFromXml(xmlPath);
        List<Element> elements = new ArrayList<>();
        
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                elements.add((Element) node);
            }
        }
        return elements;
    }

    public Map<String, Map<Integer, String>> localization = new HashMap<>();

    public String getLocalization(String handle) {
        return getLocalization(Handle.fromString(handle));
    }
    public String getLocalization(Handle handle) {
        //Log.info("Getting localization for " + handle.id() + " " + handle.version());
        Map<Integer, String> versions = localization.get(handle.id());
        if (versions == null) {
            //Log.info("No versions for " + handle.id());
            return null;
        }
        if (handle.version().equals("*")) {
            int highestVersion = versions.keySet().stream().max(Integer::compareTo).orElse(0);
            return versions.get(highestVersion);
        } else {
            return versions.get(Integer.parseInt(handle.version()));
        }
    }


    public void scan(Path xmlPath) throws Exception {
        List<Element> localizationElements = getLocalizationAsElements(xmlPath);
        int sum = 0;
        for (Element localizationElement : localizationElements) {
            String contentuid = localizationElement.getAttribute("contentuid");
            int version = Integer.parseInt(localizationElement.getAttribute("version"));
            String content = localizationElement.getTextContent();
            localization.computeIfAbsent(contentuid, k -> new HashMap<>()).put(version, content);
            sum++;
        }
        Log.info("Scanned " + sum + " localizations");
    }

    public void save(Path dest) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writeValue(dest.toFile(), localization);
    }

    public void load(Path dest) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        localization = objectMapper.readValue(dest.toFile(), 
            new TypeReference<Map<String, Map<Integer, String>>>() {});
        int totalLocalizations = localization.values().stream()
            .mapToInt(Map::size)
            .sum();
        Log.info("Loaded " + totalLocalizations + " localizations");
    }
    
}
