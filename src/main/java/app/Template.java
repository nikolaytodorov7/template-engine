package app;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Template {
    private final String FOREACH_ATTRIBUTE = "t:each";
    private final String TEXT_ATTRIBUTE = "t:text";
    private final String IF_ATTRIBUTE = "t:if";
    private final Pattern TEXT_SPLIT_PATTERN = Pattern.compile("[#$][{]([a-zA-Z0-9.= ]+)[}]");
    private final Pattern CONDITION_SPLIT_PATTERN = Pattern.compile("t:text=\"[#$][{]([a-zA-Z0-9.= ]+)[}]\"");
    private final StringBuilder parsedDoc = new StringBuilder();

    private Document doc;
    private TemplateContext context;
    private int tabSpaces = 0;

    public Template(String templatePath) {
        File input = new File(templatePath);
        try {
            doc = Jsoup.parse(input);
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid file!");
        }
    }

    public String render(TemplateContext context) {
        this.context = context;
        Element root = doc.root();
        render(root);
        String result = parsedDoc.toString();
        parsedDoc.setLength(0);
        return result;
    }

    public void render(Element element) {
        List<Node> nodes = element.childNodes();
        if (nodes.size() == 0)
            return;

        parsedDoc.append("\n");
        for (Node node : nodes) {
            if (node instanceof Element)
                processNode(node);
        }
    }

    private void processNode(Node node) {
        String tagName = ((Element) node).tagName();
        tabSpaces += 2;
        parsedDoc.append(" ".repeat(tabSpaces));
        addTag(tagName, false);
        processAttributes(node);
        if (node.childNodes().size() != 0)
            parsedDoc.append(" ".repeat(tabSpaces));

        addTag(tagName, true);
        tabSpaces -= 2;
    }

    private void processAttributes(Node node) {
        if (node.hasAttr(IF_ATTRIBUTE))
            processIf(node);

        boolean foreachAttr = node.hasAttr(FOREACH_ATTRIBUTE);
        if (foreachAttr)
            processForEach(node);

        if (node.hasAttr(TEXT_ATTRIBUTE))
            processText(node);

        if (!foreachAttr)
            render((Element) node);
    }

    private void processIf(Node node) {
        String attribute = node.attr(IF_ATTRIBUTE);
        Matcher textMatcher = TEXT_SPLIT_PATTERN.matcher(attribute);
        if (!textMatcher.matches())
            throw new IllegalStateException("Can't render corrupt attribute: " + attribute);

        attribute = textMatcher.group(1);
        String[] conditionParts = attribute.split(" == ");
        if (conditionParts.length != 2)
            throw new IllegalStateException("Can't render corrupt attribute: " + attribute);

        String firstConditionPart = conditionParts[0];
        firstConditionPart = context.get(firstConditionPart).toString();
        String secondConditionPart = conditionParts[1];
        if (!firstConditionPart.equals(secondConditionPart))
            return;

        Element element = (Element) node;
        String conditionResult = element.text();
        if (!conditionResult.startsWith(TEXT_ATTRIBUTE)) {
            parsedDoc.append(conditionResult);
            return;
        }

        Matcher conditionMatcher = CONDITION_SPLIT_PATTERN.matcher(conditionResult);
        if (!conditionMatcher.matches())
            throw new IllegalStateException("Can't render corrupt attribute: " + attribute);

        conditionResult = conditionMatcher.group(1);
        parsedDoc.append(context.get(conditionResult));
    }

    private void processForEach(Node node) {
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

                render((Element) node);
                break;
            }
        }
    }

    private void processText(Node node) {
        String attribute = node.attr(TEXT_ATTRIBUTE);
        Matcher matcher = TEXT_SPLIT_PATTERN.matcher(attribute);
        if (!matcher.matches())
            throw new IllegalStateException("Can't render corrupt attribute: " + attribute);

        attribute = matcher.group(1);
        Object o = context.get(attribute);
        parsedDoc.append(o);
    }

    private List<?> convertObjectToList(Object obj) {
        if (obj.getClass().isArray())
            return Arrays.asList((Object[]) obj);
        else if (obj instanceof Collection)
            return new ArrayList<>((Collection<?>) obj);

        throw new IllegalStateException("Must be collection or array!");
    }

    private void addTag(String tag, boolean closing) {
        String correctTag = closing ? String.format("</%s>%n", tag) : String.format("<%s>", tag);
        parsedDoc.append(correctTag);
    }
}