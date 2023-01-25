package app;

import org.junit.jupiter.api.Test;

import java.io.PrintWriter;

public class TemplateTest {

    @Test
    void test() {
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
        PrintWriter out = new PrintWriter(System.out);
        t.render(ctx, out);
    }
}
