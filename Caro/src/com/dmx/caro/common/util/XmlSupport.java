package com.dmx.caro.common.util;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public final class XmlSupport {
    private XmlSupport() {
    }

    public static Document newDocument() {
        try {
            return newBuilder().newDocument();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create XML document.", exception);
        }
    }

    public static Document parse(String xml) throws Exception {
        return newBuilder().parse(new InputSource(new StringReader(xml)));
    }

    public static Document parse(Path path) throws Exception {
        return newBuilder().parse(path.toFile());
    }

    public static String toString(Document document) {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(writer));
            return writer.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to serialize XML document.", exception);
        }
    }

    public static void write(Document document, Path path) throws IOException {
        try {
            Path parent = path.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, toString(document));
        } catch (IOException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IOException("Unable to write XML document to " + path + ".", exception);
        }
    }

    public static Element requiredChild(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        for (int index = 0; index < nodeList.getLength(); index++) {
            Node node = nodeList.item(index);
            if (node.getParentNode() == parent && node instanceof Element element) {
                return element;
            }
        }
        throw new IllegalArgumentException("Missing XML element <" + tagName + ">.");
    }

    public static String childText(Element parent, String tagName, String defaultValue) {
        for (Element child : children(parent, tagName)) {
            return child.getTextContent().trim();
        }
        return defaultValue;
    }

    public static int childInt(Element parent, String tagName, int defaultValue) {
        String text = childText(parent, tagName, String.valueOf(defaultValue));
        return Integer.parseInt(text);
    }

    public static List<Element> children(Element parent, String tagName) {
        List<Element> result = new ArrayList<>();
        NodeList nodeList = parent.getChildNodes();
        for (int index = 0; index < nodeList.getLength(); index++) {
            Node node = nodeList.item(index);
            if (node instanceof Element element && tagName.equals(element.getTagName())) {
                result.add(element);
            }
        }
        return result;
    }

    private static DocumentBuilder newBuilder() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setNamespaceAware(false);
        return factory.newDocumentBuilder();
    }
}
