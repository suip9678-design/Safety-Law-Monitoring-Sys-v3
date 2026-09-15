package com.safetylaw.monitor.support;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 외부에서 받은 XML 을 다루는 공용 도구.
 *
 * <p>법령 API 응답과 뉴스 RSS 를 모두 이 경로로 읽는다. 둘 다 외부 입력이라
 * 외부 개체 참조(XXE)를 막은 설정으로만 파싱한다. 이 방어 설정이 여러
 * 곳으로 흩어지면 한쪽만 빠뜨리기 쉬워 한 곳에 모아 둔다.
 */
public final class Xml {

    private Xml() {
    }

    public static Document parse(byte[] xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setNamespaceAware(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new ByteArrayInputStream(xml));
        } catch (Exception e) {
            throw new XmlParseException(e.getMessage(), e);
        }
    }

    /** 바로 아래 자식 중 주어진 태그들을 순서대로 찾아 첫 값을 돌려준다. */
    public static String childText(Element parent, List<String> tagNames) {
        for (String tag : tagNames) {
            for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
                if (child.getNodeType() == Node.ELEMENT_NODE && tag.equals(child.getNodeName())) {
                    String text = trimmed(child.getTextContent());
                    if (text != null) {
                        return text;
                    }
                }
            }
        }
        return null;
    }

    public static String childText(Element parent, String tagName) {
        return childText(parent, List.of(tagName));
    }

    /** 트리 전체에서 주어진 태그들을 찾아 첫 값을 돌려준다. */
    public static String textAnywhere(Element root, List<String> tagNames) {
        for (String tag : tagNames) {
            for (Element el : descendantsAndSelf(root, List.of(tag))) {
                String text = trimmed(el.getTextContent());
                if (text != null) {
                    return text;
                }
            }
        }
        return null;
    }

    /** 바로 아래 자식 중 해당 태그인 요소들. */
    public static List<Element> children(Element parent, String tagName) {
        List<Element> out = new ArrayList<>();
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE && tagName.equals(child.getNodeName())) {
                out.add((Element) child);
            }
        }
        return out;
    }

    /**
     * 자기 자신을 포함해 트리 전체에서 해당 태그인 요소들을 문서 순서대로 모은다.
     *
     * <p>태그별로 따로 모아 이어붙이면 본문이 원래 순서를 잃는다.
     * 한 번의 깊이 우선 탐색으로 순서를 지킨다.
     */
    public static List<Element> descendantsAndSelf(Element root, Collection<String> tagNames) {
        List<Element> out = new ArrayList<>();
        collect(root, tagNames, out);
        return out;
    }

    private static void collect(Element element, Collection<String> tagNames, List<Element> out) {
        if (tagNames.contains(element.getNodeName())) {
            out.add(element);
        }
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                collect((Element) child, tagNames, out);
            }
        }
    }

    public static String trimmed(String raw) {
        if (raw == null) {
            return null;
        }
        String text = raw.trim();
        return text.isEmpty() ? null : text;
    }
}
