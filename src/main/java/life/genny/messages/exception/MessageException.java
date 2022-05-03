package life.genny.messages.exception;


public class MessageException extends Exception {
    
    public MessageException(String message) {
        super("MessageException: " + message);
    }
}
