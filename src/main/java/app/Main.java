package app;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException, IllegalAccessException {
        TemplateContext ctx = new TemplateContext();
        WelcomeMessage welcome = new WelcomeMessage("hello world", "hello world2");
        ctx.put("welcome", welcome);

        Student[] students = {
                new Student(1, "Ivan"),
                new Student(2, "Maria"),
                new Student(3, "Nikola")
        };

        ctx.put("students", students);

        Template t = new Template("src/main/resources/template.tm");
        String rendered = t.render(ctx);
        System.out.println(rendered);
    }
}
