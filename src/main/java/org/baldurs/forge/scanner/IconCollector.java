package org.baldurs.forge.scanner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import io.quarkus.logging.Log;

public class IconCollector {
    public static void extractIcons(String srcXml, String imagePath, String destdir) throws Exception {
        Path imagePathPath = Path.of(imagePath);
        Path destdirPath = Path.of(destdir);

        if (!Files.exists(imagePathPath)) {
            throw new IllegalArgumentException("Image path does not exist: " + imagePathPath);
        }
        if (!Files.exists(destdirPath)) {
            Files.createDirectories(destdirPath);
        }

        Icons icons = extractXml(srcXml);

        String command = "/home/bburke/bin/squashfs-root/magick";
        for (Icon icon : icons.icons()) {
            String cmd = String.format("%s %s -crop %dx%d+%d+%d %s", command, imagePath, icons.iconSize().width(), icons.iconSize().height(), icon.x(), icon.y(), destdirPath.resolve(icon.name() + ".png").toString());
            System.out.println(cmd);
            Process process = new ProcessBuilder(cmd.split(" "))
            //.redirectOutput(ProcessBuilder.Redirect.INHERIT)
            //.redirectError(ProcessBuilder.Redirect.INHERIT)
            .start();
            process.waitFor();
            System.out.println(icon.name + " " + icon.x + " " + icon.y);
        }

        

        
    }
    private static record Icons(Size iconSize, Size textureSize, List<Icon> icons) {}

    private static record Size(int height, int width) {}

    private static record Icon(String name, int x, int y) {}

    public static Icons extractXml(String srcXml) throws Exception {
        Path srcXmlPath = Path.of(srcXml);
        if (!Files.exists(srcXmlPath)) {
            throw new IllegalArgumentException("Source XML file does not exist: " + srcXmlPath);
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(srcXmlPath.toFile());

        String xpathExpression = "//save/region[@id=\"TextureAtlasInfo\"]/node[@id=\"root\"]/children/node[@id=\"TextureAtlasIconSize\"]/attribute";
        Size iconSize = getDimensions(document, xpathExpression);

        xpathExpression = "//save/region[@id=\"TextureAtlasInfo\"]/node[@id=\"root\"]/children/node[@id=\"TextureAtlasTextureSize\"]/attribute";
        Size textureSize = getDimensions(document, xpathExpression);

        List<Icon> icons = getIcons(document, iconSize, textureSize);
        return new Icons(iconSize, textureSize, icons);

    }

    private static List<Element> getAttributeElements(Element element) {
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


    private static List<Icon> getIcons(Document document, Size iconSize, Size textureSize) throws Exception {
        String xpathExpression = "//save/region[@id=\"IconUVList\"]/node[@id=\"root\"]/children/node[@id=\"IconUV\"]";        
        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xPath = xPathFactory.newXPath();

        NodeList result = (NodeList)xPath.evaluate(xpathExpression, document, XPathConstants.NODESET);

        List<Icon> icons = new ArrayList<>();

        for (int i = 0; i < result.getLength(); i++) {
            Node node = result.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                List<Element> attributeElements = getAttributeElements(element);
                String name = "";
                double u1 = 0;
                double u2 = 0;
                double v1 = 0;
                double v2 = 0;
                for (Element attributeElement : attributeElements) {
                    String id = attributeElement.getAttribute("id");
                    String value = attributeElement.getAttribute("value");
                    if (id.equals("MapKey")) {
                        name = value;
                    } else if (id.equals("U1")) {
                        u1 = Double.parseDouble(value);
                    } else if (id.equals("U2")) {
                        u2 = Double.parseDouble(value);
                    } else if (id.equals("V1")) {
                        v1 = Double.parseDouble(value);
                    } else if (id.equals("V2")) {
                        v2 = Double.parseDouble(value);
                    }

 
                }
                int x = (int) (u1 * textureSize.width);
                int y = (int) (v1 * textureSize.height);

                icons.add(new Icon(name, x, y));
            }
        }
        icons.sort(Comparator.comparing(Icon::y).thenComparing(Icon::x));
        return icons;
 }

    private static Size getDimensions(Document document, String xpathExpression) throws XPathExpressionException {
        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xPath = xPathFactory.newXPath();

        NodeList result = (NodeList)xPath.evaluate(xpathExpression, document, XPathConstants.NODESET);

        String Height = "";
        String Width = "";
        for (int i = 0; i < result.getLength(); i++) {
            Node node = result.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                if (element.getAttribute("id").equals("Height")) {
                    Height = element.getAttribute("value");
                }
                if (element.getAttribute("id").equals("Width")) {
                    Width = element.getAttribute("value");
                }
            }
        }

        Size iconSize = new Size(Integer.parseInt(Height), Integer.parseInt(Width));
        return iconSize;
    }

}
