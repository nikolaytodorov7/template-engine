package app;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

public class Template {
    private final String T_EACH = "t:each";
    private final String T_TEXT = "t:text";
    private final String T_IF = "t:if";

    private String templatePath;

    public Template(String templatePath) {
        this.templatePath = templatePath;
    }

    public String render(TemplateContext context) throws IOException, IllegalAccessException {
        File input = new File(templatePath);
        Document doc = Jsoup.parse(input);
        Element body = doc.body();
        List<Node> nodes = body.childNodes();
        processNodes(nodes, context);
        return doc.toString();
    }

    private void processNodes(List<Node> nodes, TemplateContext context) throws IllegalAccessException {
        for (Node node : nodes) {
            if (node.toString().equals(""))
                continue;

            if (node.hasAttr(T_IF))
                processIf(context, node);

            if (node.hasAttr(T_EACH))
                processLoop(context, node);

            if (node.hasAttr(T_TEXT))
                processText(context, node);

            if (node.childNodes().size() != 0)
                processNodes(node.childNodes(), context);
        }
    }

    private void processIf(TemplateContext context, Node node) {
        String ifAttribute = node.attr(T_IF);
        node.removeAttr(T_IF);
        if (!ifAttribute.equals(""))
            ifAttribute = trimAttribute(ifAttribute);

        String[] condition = ifAttribute.split(" == ");
        if (condition.length != 2)
            throw new IllegalStateException("Corrupt condition!");

        Element e = (Element) node;
        if (e.text().contains(T_TEXT))
            processIfText(context, e.text(), node);
    }

    private void processLoop(TemplateContext context, Node node) throws IllegalAccessException {
        String attr = node.attr(T_EACH);
        String[] loopSplit = attr.split(": ");
        String loopAttribute = trimAttribute(loopSplit[1]);
        Element element = (Element) node.parent();
        node.remove();
        node.removeAttr(T_EACH);

        List<Node> nodes = node.childNodes();
        Set<String> textAttributes = getTextAttributes(nodes);

        Object template = context.get(loopAttribute);
        List<?> list = convertObjectToList(template);
        addAttributes(node, element, textAttributes, list);
    }

    private List<?> convertObjectToList(Object obj) {
        if (obj.getClass().isArray())
            return Arrays.asList((Object[]) obj);
        else if (obj instanceof Collection)
            return new ArrayList<>((Collection<?>) obj);

        throw new IllegalStateException("Must be list or array!");
    }

    private void addAttributes(Node node, Element element, Set<String> textAttributes, List<?> list) throws IllegalAccessException {
        for (Object obj : list) {
            Field[] fields = obj.getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                String fieldName = field.getName();
                if (!textAttributes.contains(fieldName))
                    continue;

                String fieldVal = field.get(obj).toString();
                addTextToNode(node, fieldVal);
            }

            element.append(String.valueOf(node));
            clearNodesText(node);
        }
    }

    private void addTextToNode(Node node, String fieldVal) {
        for (Node n : node.childNodes()) {
            if (!(n instanceof Element))
                continue;

            if (((Element) n).text().equals("")) {
                Element e = (Element) n;
                e.appendText(fieldVal);
                break;
            }
        }
    }

    private Set<String> getTextAttributes(List<Node> nodes) {
        Set<String> textAttributes = new HashSet<>();
        for (Node n : nodes) {
            String attr1 = n.attr(T_TEXT);
            if (!attr1.equals("")) {
                attr1 = trimAttribute(attr1);
                textAttributes.add(attr1.split("\\.")[1]);
            }

            n.removeAttr(T_TEXT);
        }

        return textAttributes;
    }

    private void clearNodesText(Node node) {
        for (Node n : node.childNodes()) {
            if (!(n instanceof Element e))
                continue;

            List<TextNode> textNodes = e.textNodes();
            for (TextNode textNode : textNodes) {
                textNode.text("");
            }
        }
    }

    private void processText(TemplateContext context, Node node) {
        String textAttribute = node.attr(T_TEXT);
        node.removeAttr(T_TEXT);
        textAttribute = trimAttribute(textAttribute);
        Element e = (Element) node;
        String text = context.get(textAttribute).toString();
        e.appendText(text);
    }

    private void processIfText(TemplateContext context, String textAttribute, Node node) {
        if (!textAttribute.startsWith(T_TEXT))
            throw new IllegalStateException("Corrupt T:text provided!");

        textAttribute = textAttribute.substring(10, textAttribute.length() - 2);
        Element e = (Element) node;
        String text = context.get(textAttribute).toString();
        TextNode textNode = e.textNodes().get(0);
        textNode.text(text);
    }

    private String trimAttribute(String attribute) {
        return attribute.substring(2, attribute.length() - 1);
    }
}
