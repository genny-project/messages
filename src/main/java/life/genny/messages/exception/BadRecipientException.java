package life.genny.messages.exception;

import life.genny.qwandaq.entity.BaseEntity;

public class BadRecipientException extends MessageException {
    
    public BadRecipientException(String recipient) {
        super("Bad Recipient: " + recipient);
    }

    public BadRecipientException(BaseEntity recipientBe) {
        super("Bad Recipient " + (recipientBe != null ? "with BE Code: " + recipientBe.getCode() : ". Null BE Code for Recipient!"));
    }
}
