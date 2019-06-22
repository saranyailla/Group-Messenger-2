package edu.buffalo.cse.cse486586.groupmessenger2;

import java.util.Comparator;

public class Message implements java.io.Serializable{
    public Boolean agreement;
    public String message;
    public int messageId;
    public int senderPort;
    public int proposedSequenceNumber;
    public int agreedSequenceNumber;
    public int finalAgreedPort;
    public Boolean deliverable;
    public Boolean agreementReceived;
    public Boolean sendStatus;

    public int getSenderPort() {
        return senderPort;
    }

    public void setSenderPort(int senderPort) {
        this.senderPort = senderPort;
    }

    public Boolean getSendStatus() {
        return sendStatus;
    }

    public void setSendStatus(Boolean sendStatus) {
        this.sendStatus = sendStatus;
    }

    public Boolean getAgreementReceived() {
        return agreementReceived;
    }

    public void setAgreementReceived(Boolean agreementReceived) {
        this.agreementReceived = agreementReceived;
    }


    public Boolean getDeliverable() {
        return deliverable;
    }

    public void setDeliverable(Boolean deliverable) {
        this.deliverable = deliverable;
    }

    public Boolean getAgreement() {
        return agreement;
    }

    public void setAgreement(Boolean agreement) {
        this.agreement = agreement;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }


    public int getProposedSequenceNumber() {
        return proposedSequenceNumber;
    }

    public void setProposedSequenceNumber(int proposedSequenceNumber) {
        this.proposedSequenceNumber = proposedSequenceNumber;
    }

    public int getAgreedSequenceNumber() {
        return agreedSequenceNumber;
    }

    public void setAgreedSequenceNumber(int agreedSequenceNumber) {
        this.agreedSequenceNumber = agreedSequenceNumber;
    }

    public int getFinalAgreedPort() {
        return finalAgreedPort;
    }

    public void setFinalAgreedPort(int finalAgreedPort) {
        this.finalAgreedPort = finalAgreedPort;
    }

    public static Comparator<Message> sortMessages() {
      Comparator comp=  new Comparator<Message>() {
            @Override
            public int compare(Message p, Message q) {
                if (Integer.compare(p.getAgreedSequenceNumber(), q.getAgreedSequenceNumber()) == 0) {
                    if (Integer.compare(p.getFinalAgreedPort(), q.getFinalAgreedPort()) == 0) {
                        return Integer.compare(p.messageId, q.getMessageId());
                    } else {
                        return Integer.compare(p.getFinalAgreedPort(), q.getFinalAgreedPort());
                    }
                } else {
                    return Integer.compare(p.getAgreedSequenceNumber(), q.getAgreedSequenceNumber());
                }

            }
        };
      return  comp;

    }



}

