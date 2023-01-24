package app;

public class WelcomeMessage {
    private String message;
    private String secondMessage;

    public WelcomeMessage(String message) {
        this.message = message;
    }

    public WelcomeMessage(String message, String secondMessage) {
        this.message = message;
        this.secondMessage = secondMessage;
    }
}
