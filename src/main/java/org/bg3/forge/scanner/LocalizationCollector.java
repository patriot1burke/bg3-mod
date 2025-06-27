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

public class LocalizationCollector {
    public static NodeList getLocalizationFromXml(Path xmlPath) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(xmlPath.toFile());
        
        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xPath = xPathFactory.newXPath();
        
        String xpathExpression = "//contentList/context";
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

    public Map<String, String> localization = new HashMap<>();

    public void scan(Path xmlPath) throws Exception {
        List<Element> localizationElements = getLocalizationAsElements(xmlPath);
        for (Element localizationElement : localizationElements) {
            String contentuid = localizationElement.getAttribute("contentuid");
            String content = localizationElement.getTextContent();
            localization.put(contentuid, content);
        }
    }
    
}
