package com.dmx.caro.common.protocol;

import com.dmx.caro.common.util.XmlSupport;
import java.util.LinkedHashMap;
import java.util.Map;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public final class ProtocolCodec {
    private ProtocolCodec() {
    }

    public static String encode(NetworkMessage message) {
        Document document = XmlSupport.newDocument();
        Element root = document.createElement("message");
        root.setAttribute("type", message.type().name());
        document.appendChild(root);

        for (Map.Entry<String, String> entry : message.fields().entrySet()) {
            Element field = document.createElement("field");
            field.setAttribute("name", entry.getKey());
            field.setTextContent(entry.getValue());
            root.appendChild(field);
        }

        return XmlSupport.toString(document);
    }

    public static NetworkMessage decode(String xml) throws ProtocolException {
        try {
            Document document = XmlSupport.parse(xml);
            Element root = document.getDocumentElement();
            if (!"message".equals(root.getTagName())) {
                throw new ProtocolException("Unexpected root element: " + root.getTagName());
            }

            String rawType = root.getAttribute("type");
            if (rawType == null || rawType.isBlank()) {
                throw new ProtocolException("Protocol message is missing its type.");
            }

            Map<String, String> fields = new LinkedHashMap<>();
            for (Element field : XmlSupport.children(root, "field")) {
                String name = field.getAttribute("name");
                fields.put(name, field.getTextContent());
            }

            NetworkMessage.Builder builder = NetworkMessage.type(MessageType.valueOf(rawType));
            for (Map.Entry<String, String> entry : fields.entrySet()) {
                builder.field(entry.getKey(), entry.getValue());
            }
            return builder.build();
        } catch (ProtocolException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ProtocolException("Unable to decode protocol message: " + exception.getMessage(), exception);
        }
    }
}
