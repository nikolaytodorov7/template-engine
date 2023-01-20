package app;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

public class Template {
    String templatePath;

    public Template(String templatePath) {
        this.templatePath = templatePath;
    }

    public String render(TemplateContext context) throws IOException, IllegalAccessException {
        File input = new File(templatePath);
        Document doc = Jsoup.parse(input);
        Element body = doc.body();
        List<Node> nodes = body.childNodes();
        doNodes(nodes, context);
        return doc.toString();
    }

    public static List<?> convertObjectToList(Object obj) {
        if (obj.getClass().isArray())
            return Arrays.asList((Object[]) obj);
        else if (obj instanceof Collection)
            return new ArrayList<>((Collection<?>) obj);

        return null;
    }

    private static void doNodes(List<Node> nodes, TemplateContext context) throws IllegalAccessException {
        for (Node node : nodes) {
            if (node.toString().equals(""))
                continue;

            if (node.hasAttr("t:each"))
                processLoop(context, node);

            if (node.hasAttr("t:text"))
                processText(context, node);

            if (node.childNodes().size() != 0)
                doNodes(node.childNodes(), context);
        }
    }

    private static void processLoop(TemplateContext context, Node node) throws IllegalAccessException {
        String attr = node.attr("t:each");
        String[] loopSplit = attr.split(": ");
        String loopAttribute = trimAttribute(loopSplit[1]);
        Element element = (Element) node.parent();
        node.remove();
        node.removeAttr("t:each");

        List<Node> nodes = node.childNodes();
        Set<String> textAttributes = new HashSet<>();
        fillTextAttributes(nodes, textAttributes);

        Object template = context.getTemplate(loopAttribute);
        List<?> list = convertObjectToList(template);
        if (list == null)
            return;

        addAttributes(node, element, textAttributes, list);
    }

    private static void addAttributes(Node node, Element element, Set<String> textAttributes, List<?> list) throws IllegalAccessException {
        for (Object obj : list) {
            int count = 0;
            Field[] fields = obj.getClass().getDeclaredFields();
            for (Field field : fields) {
                if (count == textAttributes.size())
                    break;

                field.setAccessible(true);
                String fieldName = field.getName();
                if (!textAttributes.contains(fieldName))
                    continue;

                String fieldVal = field.get(obj).toString();
                addTextToNode(node, fieldVal);
                count++;
            }

            element.append(String.valueOf(node));
            clearNodesText(node);
        }
    }

    private static void addTextToNode(Node node, String fieldVal) {
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

    private static void fillTextAttributes(List<Node> nodes, Set<String> textAttributes) {
        for (Node n : nodes) {
            String attr1 = n.attr("t:text");
            if (!attr1.equals("")) {
                attr1 = trimAttribute(attr1);
                textAttributes.add(attr1.split("\\.")[1]);
            }

            n.removeAttr("t:text");
        }
    }

    private static void clearNodesText(Node node) {
        for (Node n : node.childNodes()) {
            if (!(n instanceof Element e))
                continue;

            List<TextNode> textNodes = e.textNodes();
            for (TextNode textNode : textNodes) {
                textNode.text("");
            }
        }
    }

    private static void processText(TemplateContext context, Node node) {
        String textAttribute = node.attr("t:text");
        node.removeAttr("t:text");
        textAttribute = trimAttribute(textAttribute);
        Element e = (Element) node;
        e.appendText(context.getTemplate(textAttribute).toString());
    }

    private static String trimAttribute(String attribute) {
        return attribute.substring(2, attribute.length() - 1);
    }
}
