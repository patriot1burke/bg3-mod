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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.logging.Log;

public class RootTemplateArchive {
    
    public static NodeList getGameObjectsFromXml(Path xmlPath) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(xmlPath.toFile());
        
        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xPath = xPathFactory.newXPath();
        
        String xpathExpression = "//save/region[@id=\"Templates\"]/node[@id=\"Templates\"]/children/node[@id=\"GameObjects\"]";
        return (NodeList) xPath.evaluate(xpathExpression, document, XPathConstants.NODESET);
    }
    
    public static List<Element> getGameObjectsAsElements(Path xmlPath) throws Exception {
        NodeList nodeList = getGameObjectsFromXml(xmlPath);
        List<Element> elements = new ArrayList<>();
        
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                elements.add((Element) node);
            }
        }
        
        return elements;
    }
    
    public static List<Element> getAttributeElements(Element element) {
        List<Element> attributeElements = new ArrayList<>();
        NodeList children = element.getChildNodes();
        
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && "attribute".equals(child.getNodeName())) {
                attributeElements.add((Element) child);
            }
        }
        
        return attributeElements;
    }


    public Map<String, RootTemplate> templates = new HashMap<>();

    public RootTemplateArchive scan(Path xmlPath) throws Exception {
        List<Element> gameObjects = getGameObjectsAsElements(xmlPath);
        int sum = 0;
        for (Element gameObject : gameObjects) {
            String stats = null;
            String mapKey = null;
            String displayName = null;
            String description = null;
            String parentTemplateId = null;
            String icon = null;
            List<Element> attributeElements = getAttributeElements(gameObject);
            for (Element attributeElement : attributeElements) {
                String attribute = attributeElement.getAttribute("id");
                if (attribute.equals("Stats")) {
                    stats = attributeElement.getAttribute("value");
                } else if (attribute.equals("MapKey")) {
                    mapKey = attributeElement.getAttribute("value");
                } else if (attribute.equals("DisplayName")) {
                    displayName = attributeElement.getAttribute("handle");
                } else if (attribute.equals("Description")) {
                    description = attributeElement.getAttribute("handle");
                } else if (attribute.equals("ParentTemplateId")) {
                    parentTemplateId = attributeElement.getAttribute("value");
                } else if (attribute.equals("Icon")) {
                    icon = attributeElement.getAttribute("value");
                }
            }
            templates.put(mapKey, new RootTemplate(stats, mapKey, displayName, description, parentTemplateId, icon, this));
            sum++;
        }
        Log.info("Scanned " + sum + " root templates");
        return this;
    }

    public void save(Path dest) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writeValue(dest.toFile(), templates);
    }

    public void load(Path dest) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        templates = objectMapper.readValue(dest.toFile(), new TypeReference<Map<String, RootTemplate>>() {});
        for (RootTemplate template : templates.values()) {
            template.archive = this;
        }
        Log.info("Loaded " + templates.size() + " root templates");
    }

 }
