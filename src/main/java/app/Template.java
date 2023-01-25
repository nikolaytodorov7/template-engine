package app;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Template {
    private final String FOREACH_ATTRIBUTE = "t:each";
    private final String TEXT_ATTRIBUTE = "t:text";
    private final String IF_ATTRIBUTE = "t:if";
    private final Pattern TEXT_SPLIT_PATTERN = Pattern.compile("[#$][{]([a-zA-Z0-9.= ]+)[}]");
    private final Pattern CONDITION_SPLIT_PATTERN = Pattern.compile("t:text=\"[#$][{]([a-zA-Z0-9.= ]+)[}]\"");
    private Document doc;
    private int tabSpaces = 0;

    public Template(String templatePath) {
        File input = new File(templatePath);
        try {
            doc = Jsoup.parse(input);
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid file!");
        }
    }

    public void render(TemplateContext context, PrintWriter out) {
        Element root = doc.root();
        render(root, context, out);
        out.flush();
    }

    private void render(Element element, TemplateContext context, PrintWriter out) {
        List<Node> nodes = element.childNodes();
        if (nodes.size() == 0)
            return;

        out.append("\n");
        for (Node node : nodes) {
            if (node instanceof Element)
                processNode(node, context, out);
        }
    }

    private void processNode(Node node, TemplateContext context, PrintWriter out) {
        String tagName = ((Element) node).tagName();
        tabSpaces += 2;
        out.append(" ".repeat(tabSpaces));
        addTag(tagName, out, false);
        processAttributes(node, context, out);
        if (node.childNodes().size() != 0)
            out.append(" ".repeat(tabSpaces));

        addTag(tagName, out, true);
        tabSpaces -= 2;
    }

    private void processAttributes(Node node, TemplateContext context, PrintWriter out) {
        if (node.hasAttr(IF_ATTRIBUTE))
            processIf(node, context, out);

        boolean foreachAttr = node.hasAttr(FOREACH_ATTRIBUTE);
        if (foreachAttr)
            processForEach(node, context, out);

        if (node.hasAttr(TEXT_ATTRIBUTE))
            processText(node, context, out);

        if (!foreachAttr)
            render((Element) node, context, out);
    }

    private void processIf(Node node, TemplateContext context, PrintWriter out) {
        String attribute = node.attr(IF_ATTRIBUTE);
        Matcher textMatcher = TEXT_SPLIT_PATTERN.matcher(attribute);
        if (!textMatcher.matches())
            throw new IllegalStateException("Can't render corrupt attribute: " + attribute);

        attribute = textMatcher.group(1);
        if (attribute == null || attribute.equals("0") || attribute.isBlank())
            return;

        Element element = (Element) node;
        String conditionResult = element.text();
        if (!conditionResult.startsWith(TEXT_ATTRIBUTE)) {
            out.append(conditionResult);
            return;
        }

        Matcher conditionMatcher = CONDITION_SPLIT_PATTERN.matcher(conditionResult);
        if (!conditionMatcher.matches())
            throw new IllegalStateException("Can't render corrupt attribute: " + attribute);

        conditionResult = conditionMatcher.group(1);
        out.append(context.get(conditionResult).toString());
    }

    private void processForEach(Node node, TemplateContext context, PrintWriter out) {
        String attribute = node.attr(FOREACH_ATTRIBUTE);
        String[] split = attribute.split(": ");
        Matcher matcher = TEXT_SPLIT_PATTERN.matcher(split[1]);
        if (!matcher.matches())
            throw new IllegalStateException("Can't render corrupt attribute: " + attribute);

        attribute = matcher.group(1);
        Object o = context.get(attribute);
        List<?> objects = convertObjectToList(o);
        Element node1 = (Element) node;
        for (Object obj : objects) {
            context.put(split[0], obj);
            List<Node> nodes1 = node1.childNodes();
            for (Node node2 : nodes1) {
                if (!(node2 instanceof Element))
                    continue;

                render((Element) node, context, out);
                break;
            }
        }
    }

    private void processText(Node node, TemplateContext context, PrintWriter out) {
        String attribute = node.attr(TEXT_ATTRIBUTE);
        Matcher matcher = TEXT_SPLIT_PATTERN.matcher(attribute);
        if (!matcher.matches())
            throw new IllegalStateException("Can't render corrupt attribute: " + attribute);

        attribute = matcher.group(1);
        Object o = context.get(attribute);
        out.append(o.toString());
    }

    private List<?> convertObjectToList(Object obj) {
        if (obj.getClass().isArray())
            return Arrays.asList((Object[]) obj);
        else if (obj instanceof Collection)
            return new ArrayList<>((Collection<?>) obj);

        throw new IllegalStateException("Must be collection or array!");
    }

    private void addTag(String tag, PrintWriter out, boolean closing) {
        String correctTag = closing ? String.format("</%s>%n", tag) : String.format("<%s>", tag);
        out.append(correctTag);
    }
}