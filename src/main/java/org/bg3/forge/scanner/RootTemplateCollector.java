package org.bg3.forge.scanner;

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

public class RootTemplateCollector {
    
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

    public static class RootTemplate {
        public String Name;
        public String MapKey;
        public Map<String, String> Attributes = new HashMap<>();

        public String toString() {
            return "RootTemplate{" +
                "Name='" + Name + '\'' +
                ", MapKey='" + MapKey + '\'' +
                ", Attributes=" + Attributes +
                '}';
        }
        
    }

    public Map<String, RootTemplate> templates = new HashMap<>();

    public void scan(Path xmlPath) throws Exception {
        List<Element> gameObjects = getGameObjectsAsElements(xmlPath);
        for (Element gameObject : gameObjects) {
            RootTemplate template = new RootTemplate();
            List<Element> attributeElements = getAttributeElements(gameObject);
            for (Element attributeElement : attributeElements) {
                template.Attributes.put(attributeElement.getAttribute("id"), attributeElement.getAttribute("value"));
            }
            template.Name = template.Attributes.get("Name");
            template.MapKey = template.Attributes.get("MapKey");
            templates.put(template.MapKey, template);
        }
    }


}
